package idesign.tal;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import edu.mit.jmwe.data.IToken;
import edu.mit.jmwe.data.Token;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class Analyzer {

	protected StanfordCoreNLP pipeline;

	public Analyzer() {
		// Create StanfordCoreNLP object properties, with POS tagging
		// (required for lemmatization), and lemmatization
		Properties props;
		props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");

		// StanfordCoreNLP loads a lot of models, so you probably
		// only want to do this once per execution
		this.pipeline = new StanfordCoreNLP(props);
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
	public List<List<IToken>> buildSentenceJMWE(String text) {

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(this.fixPecularities(text));

		// run all Annotators on this text
		
		this.pipeline.annotate(document);

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

	/**
	 * DEPRACATED. Returns a list of lemmas for that sentence.
	 */
	public List<String> lemmatize(String documentText) {
		List<String> lemmas = new LinkedList<String>();
		List<String> pos = new LinkedList<String>();
		List<String> tokens = new LinkedList<String>();

		// create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);

		// run all Annotators on this text
		this.pipeline.annotate(document);

		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {

			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// Retrieve and add the lemma for each word into the
				// list of lemmas
				// lemmas.add(token.get(LemmaAnnotation.class));
				// pos.add(token.get(PartOfSpeechAnnotation.class));
				lemmas.add(token.lemma());
				pos.add(token.tag());
				// tokens.add(token.get(sAnnotation.class));
				tokens.add(token.toString());

			}
		}
		System.out.println(lemmas + "\n" + pos + "\n" + tokens);
		return lemmas;
	}

	/**
	 * Finds the number of tokens in a sentence/string.
	 * @param text - sentence, or any string.
	 * @return tokensNumber
	 */
	public int getTokensNumber (String text) {
		int tokensNumber = 0;
		Annotation annotatedText = new Annotation(this.fixPecularities(text));
		// run all Annotators on this text
		this.pipeline.annotate(annotatedText);
		List<CoreMap> sentences = annotatedText.get(SentencesAnnotation.class);
		
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence and add num of tokens (words)
			tokensNumber += sentence.get(TokensAnnotation.class).size();
		}
		return tokensNumber;
		
	}
	private String fixPecularities(String text) {
		text = text.replaceAll(";", "; ");
		text = text.replaceAll(":", ": ");
		text = text.replaceAll("=", "= ");
		text = text.replaceAll("_", " ");
		text = text.replaceAll("—", "Ð");
		text = text.replaceAll("à", "‡");
		text = text.replaceAll("------", "XXXXXX");
		text = text.replaceAll("----", "XXXX");
		return text;
	}
}