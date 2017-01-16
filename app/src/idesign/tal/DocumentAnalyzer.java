package idesign.tal;

import idesign.tal.db.Database;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.mit.jmwe.data.IMWE;
import edu.mit.jmwe.data.IToken;
import edu.mit.jmwe.detect.IMWEDetector;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class DocumentAnalyzer {

	private Analyzer analyzer;
	private Database database;

	DocumentAnalyzer(Database db) {
		this.analyzer = new Analyzer();
		this.database = db;
		// NOTE: needs large memory:
		// set VM arguments to at
		// least -Xms1024M -Xmx1024M

	}

	/**
	 * Find Expressions in doc, using the specified detector.
	 * Notes: only sentences with MWEs are added to DB.
	 * @param detector
	 * @param doc
	 * @return
	 */
	public int findMWEs(IMWEDetector detector, IMWEDetector detector2,
			idesign.tal.Document doc) {
		int countMWEs = 0;
		int dbDocumentId = 0; // document identifier in DB.

		// save document to DB, if not exists.
		// get document_id from DB
		dbDocumentId = database.insertDocument(doc.getTitle(), doc.getAuthor(), doc.getYear());
		
		if (dbDocumentId == 0) {
			System.out.println("Couldn't insert document to DB. Exiting.");
			System.exit(1);
		}
		
		
		// for each sentence... annotate it (tags, tokens)
		for (int i = 0, len = doc.getSentencesSize(); i < len; i++) {
			String currentSentence = doc.getSentence(i);
			if (App.DEBUG)
				System.out.println("Sentence " + i + ":\n" + currentSentence);
			List<List<IToken>> sentences = this.analyzer.buildSentenceJMWE(currentSentence);
			
			/*for (List<IToken> sentence : sentences) {
				List<IMWE<IToken>> mwes = detector.detect(sentence); 
				countMWEs += mwes.size(); // counts MWEs in document.
				for (IMWE<IToken> mwe : mwes) {
					if (App.DEBUG)
						System.out.println("\t detector1: " + mwe);
				}
			}*/
			
			if (detector2 !=null) {
				for (List<IToken> sentence : sentences) {
					int dbSentenceId = 0;
					List<IMWE<IToken>> mwes = detector2.detect(sentence);
					for (IMWE<IToken> mwe : mwes) {
						
						// add sentence if not exists, if it contains expressions:
						if (dbSentenceId == 0) {
							dbSentenceId = database.insertSentence(dbDocumentId, currentSentence);
							
							if (dbSentenceId == 0) {
								System.out.println("Couldn't insert sentence to DB. Exiting.");
								System.exit(1);
							}
						}
						
						if (App.DEBUG)
							System.out.println("\t detector2: " 
									+ mwe.getEntry().getID() + " " 
									+ mwe.getForm().toString() + " " 
									+ mwe.getTokens().toString());
						//.get(0).getForm(); // text AS-IS. e.g. "the"
						//mwe.getTokens().get(0).getTag(); // (e.g. "NNP")
						//mwe.getEntry().getPOS(); // R, V, N, ...
						//mwe.getForm(); // text quotation
						String expressionId = mwe.getEntry().getID().toString();
						String tokens ="";
						String tags="";
						for(IToken token : mwe.getTokens()) {
							tokens += token.getForm() + " "; // a word
							tags += token.getTag() + " "; // IN, TO, VB, NNP, ...
						}
						
						// check if expression wasn't encountered
						// save expression (mwe), if not exists in DB expressions table (mwe_id, tokens, tags)
						database.insertExpression(expressionId, tokens.trim(), tags.trim()); 
						
							
						// TODO add expression occurrance to DB (mwe_id, dbSentenceId)
						boolean dbSuccess = database.inserExpressionOccurrance(expressionId, dbSentenceId);
					
						if (!dbSuccess) {
							System.out.println("Couldn't insert expression occurrance to DB. Exiting.");
							System.exit(1);
						}
					}
				}
			}
		}
		// TODO save to DB Documents table, how many MWEs were found in this doc.
		return countMWEs;
	}
	
	/** Find the number of tokens in a text document and updates the DB document with this number.
	 * Note: function was added after documents were already inserted into DB, without the words field.
	 * @param doc
	 * @return tokensNumber
	 * @author Tal Daniel
	 */
	public int countTokens(Document doc) {
		int tokensNumber = 0;
		for (int i = 0, len = doc.getSentencesSize(); i < len; i++) {
			String currentSentence = doc.getSentence(i);
			//TEST: currentSentence = "a bb  ccc   ddd,   one-way .  ";
			currentSentence = currentSentence.replaceAll("\\s+|-", " "); 
			tokensNumber += currentSentence.split(" ").length; // NOTE: won't isn't split into 2 words
			//tokensNumber += this.analyzer.getTokensNumber(currentSentence);
		}
		
		doc.setWords(tokensNumber);
		
		// Update DB Document with word number in doc:
		int affectedRows = database.updateDocumentWords(doc.getTitle(), doc.getAuthor(), doc.getWords());
		if (App.DEBUG) {
			System.out.println("DB: Updated " + affectedRows + " doc words: " + tokensNumber);
		}
		return tokensNumber;
	}
	
}
