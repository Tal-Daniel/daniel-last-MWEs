package idesign.tal.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import edu.mit.jmwe.data.IMWE;
import edu.mit.jmwe.data.IToken;
import edu.mit.jmwe.data.Token;
import edu.mit.jmwe.detect.Consecutive;
import edu.mit.jmwe.detect.IMWEDetector;
import edu.mit.jmwe.detect.LMLR;
import edu.mit.jmwe.index.IMWEIndex;
import edu.mit.jmwe.index.MWEIndex;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class analyzeTest {

	private static enum OS {
		PC, OSX
	};

	final static OS currentOS = OS.OSX;
	final static String MWE_INDEX_FILE_PATH = (currentOS == OS.OSX) ? "../../Resources/Libraries/edu.mit.jmwe_1.0.1_all/mweindex_wordnet3.0_semcor1.6.data"
			: "D:/Resources/jMWE/edu.mit.jmwe_1.0.1_all/mweindex_wordnet3.0_semcor1.6.data";

	protected static StanfordCoreNLP pipeline;


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		
		Properties props;
		props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");

		// StanfordCoreNLP loads a lot of models, so you probably
		// only want to do this once per execution
		pipeline = new StanfordCoreNLP(props);
		
		
		// load MWE index:
		File idxData = new File(MWE_INDEX_FILE_PATH);
		IMWEIndex index = new MWEIndex(idxData);

		try {
			index.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		IMWEDetector detector = new Consecutive(index);
		LMLR detector2 = new LMLR(detector);

		String textTest = "_NOAH_ Come, ------'s child (__not really__) _CLARA_. join my humble arc, au gustus. A public sale of favor and injustice was instituted, both in the court and in the provinces, by the worthless delegates of his power, whose merit it was made _sacrilege_ to question.";

		List<List<IToken>> sentences = buildSentenceJMWE(textTest);

		if (detector2 != null) {
			for (List<IToken> sentence : sentences) {
				// detect MWEs
				List<IMWE<IToken>> mwes = detector2.detect(sentence);
				
				
				for (IMWE<IToken> mwe : mwes) {
						System.out.println("\t detector2: " 
								+ mwe.getEntry().getID() + " " 
								+ mwe.getForm().toString() + " " 
								+ mwe.getTokens().toString());
				}
			}
		}
	}

	/**
	 * Parse the input sentence(s) for the jMWE's library detector. Each
	 * sentence is parsed for POS & Lemmas, and returned as List of ITokens
	 * Note: returns list of sentences, to cope with single/multiple sentences.
	 * 
	 * @param text
	 *            Sentence, or sentences.
	 * @return List of Sentences, in jMWE format, ready for MWE detection.
	 * @author Tal Daniel
	 */
	private static List<List<IToken>> buildSentenceJMWE(String text) {

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(text);

		// run all Annotators on this text

		pipeline.annotate(document);

		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		List<List<IToken>> sentences_JMWE = new LinkedList<List<IToken>>();
		for (CoreMap sentence : sentences) {

			List<IToken> sentence_JMWE = new ArrayList<IToken>();

			// Iterate over all tokens in a sentence, and build sentence in jMWE
			// format
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// TODO strip sentence from illegal arguments/characters
				if (!token.toString().equalsIgnoreCase("_")
						&& !token.toString().equalsIgnoreCase("__")
						&& !token.toString().equalsIgnoreCase("___")
						&& !token.toString().equalsIgnoreCase("____")) {
					sentence_JMWE.add(new Token(token.toString(), token.tag(),
							token.lemma()));
				}
			}

			sentences_JMWE.add(sentence_JMWE);
		}

		return sentences_JMWE;
	}

}
