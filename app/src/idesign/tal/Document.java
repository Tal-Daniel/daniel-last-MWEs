package idesign.tal;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import edu.mit.jmwe.data.IMWE;
import edu.mit.jmwe.data.IToken;
import edu.stanford.nlp.io.EncodingFileReader;
import edu.stanford.nlp.process.WhitespaceTokenizer;
import edu.stanford.nlp.process.WhitespaceTokenizer.WhitespaceTokenizerFactory;

public class Document {
	/**
	 * Stands for CMLET document. If other documet types, in the future, make
	 * this an interface, and create new Document classes.
	 * 
	 * @author Tal Daniel
	 */
	private static final String ID_MARKER_START = "<id>";
	private static final String ID_MARKER_END = "</id>";
	
	private static final String PERIOD_MARKER_START = "<period>";
	private static final String PERIOD_MARKER_END = "</period>";
	
	private static final String TITLE_MARKER_START = "<title>";
	private static final String TITLE_MARKER_END = "</title>";
	
	private static final String AUTHOR_MARKER_START = "<author>";
	private static final String AUTHOR_MARKER_END = "</author>";
	
	private static final String GENRE_MARKER_START = "<genre>";
	private static final String GENRE_MARKER_END = "</genre>";
	
	private static final String SUBGENRE_MARKER_START = "<sub-genre>";
	private static final String SUBGENRE_MARKER_END = "<sub-genre>";
	
	private static final String AUTHOR_GENDER_MARKER_START = "<author's gender>";
	private static final String AUTHOR_GENDER_MARKER_END = "</author's gender>";
	
	private static final String AUTHOR_BIRTH_MARKER_START = "<author's year of birth>";
	private static final String AUTHOR_BIRTH_MARKER_END = "</author's year of birth>";
	
	private static final String TEXT_DATE_MARKER_START = "<date of text>";
	private static final String TEXT_DATE_MARKER_END = "</date of text>";
	
	private static final String TEXT_MARKER_START = "<text>";
	private static final String TEXT_MARKER_END = "</text>";
	
	private static final String PAGE_MARKER = "[Page]";
	
	private int id, words=0;
	private String period, title, genre, sub_genre, text_date, text;
	private String author, author_gender, author_birth;
	private ArrayList<String> sentences;
	private List<List<IMWE<IToken>>> expressions;

	public Document(String fileName) {
		this.sentences = new ArrayList<String>();
		
		EncodingFileReader fileReader = null;
		try {
			fileReader = new EncodingFileReader(fileName); 
			this.parseDocument(fileReader);
			fileReader.close();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void parseDocument(EncodingFileReader fileReader) {

		BufferedReader reader = new BufferedReader(fileReader);

		String line = null;
		int lineNumber = 0;
		try {
			while ((line = reader.readLine()) != null) {
				this.parseLine(line, ++lineNumber);
			}
			if (reader != null)
				reader.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void parseLine(String line, int lineNumber) {
		
		if (lineNumber < 13) {
			// parse meta-data:
		
			if (line.contains(ID_MARKER_START)) {
				// set ID
				line = line.substring(
						line.indexOf(ID_MARKER_START) + ID_MARKER_START.length(), 
						line.indexOf(ID_MARKER_END)
					);
				this.id = Integer.parseInt(line);
			} 
			else if (line.contains(TITLE_MARKER_START)) {
				line = line.substring(
						line.indexOf(TITLE_MARKER_START) + TITLE_MARKER_START.length(), 
						line.indexOf(TITLE_MARKER_END)
					);
				this.setTitle(line);	
			}
			
			else if (line.contains(TEXT_DATE_MARKER_START)) {
				line = line.substring(
						line.indexOf(TEXT_DATE_MARKER_START) + TEXT_DATE_MARKER_START.length(), 
						line.indexOf(TEXT_DATE_MARKER_END)
					);
				this.text_date = line;
			}
			
			
			else if (line.contains(GENRE_MARKER_START)) {
				line = line.substring(
						line.indexOf(GENRE_MARKER_START) + GENRE_MARKER_START.length(), 
						line.indexOf(GENRE_MARKER_END)
					);
				this.genre = line;	
			}
			
			else if (line.contains(SUBGENRE_MARKER_START)) {
				line = line.substring(
						line.indexOf(SUBGENRE_MARKER_START) + SUBGENRE_MARKER_START.length(), 
						line.lastIndexOf(SUBGENRE_MARKER_END)
					);
				this.sub_genre = line;	
			}
			
			else if (line.contains(AUTHOR_MARKER_START)) {
				line = line.substring(
						line.indexOf(AUTHOR_MARKER_START) + AUTHOR_MARKER_START.length(), 
						line.indexOf(AUTHOR_MARKER_END)
					);
				this.setAuthor(line);	
			}
			
			else if (line.contains(AUTHOR_GENDER_MARKER_START)) {
				line = line.substring(
						line.indexOf(AUTHOR_GENDER_MARKER_START) + AUTHOR_GENDER_MARKER_START.length(), 
						line.indexOf(AUTHOR_GENDER_MARKER_END)
					);
				this.author_gender = line;	
			}
			
			else if (line.contains(AUTHOR_BIRTH_MARKER_START)) {
				line = line.substring(
						line.indexOf(AUTHOR_BIRTH_MARKER_START) + AUTHOR_BIRTH_MARKER_START.length(), 
						line.indexOf(AUTHOR_BIRTH_MARKER_END)
					);
				this.author_birth = line;	
			}
			
			// TODO: continue parse META-DATA.
			
		} 
		else {
			// parse text (each line has a single sentence):
			
			// TODO: use regex, when possible:

			// remove Text start marker
			if (line.contains(TEXT_MARKER_START))
				line = line.substring(line.indexOf(TEXT_MARKER_START)
						+ TEXT_MARKER_START.length());

			// remove Text end marker
			if (line.contains(TEXT_MARKER_END))
				line = line.substring(0, line.indexOf(TEXT_MARKER_END));

			// remove Page markers
			while (line.contains(PAGE_MARKER)) {
				int foundIndex = line.indexOf(PAGE_MARKER);
				line = line.substring(0, foundIndex)
						+ line.substring(foundIndex
								+ PAGE_MARKER.length());
				// TODO improve and remove "[page *]" instances, too.
			}
		
			// TODO: split into sentences (file isn't perfect)
			// add to Document class
			this.addSentence(line.trim());
			System.out.println("parsed line: " + line);
		}
		
	}

	/**
	 * @param sentence
	 *            the sentence to set
	 */
	public void addSentence(String sentence) {
		this.sentences.add(sentence);
	}

	/**
	 * @return the sentence
	 */
	public String getSentence(int index) {
		return this.sentences.get(index);
	}

	public int getSentencesSize() {
		// TODO Auto-generated method stub
		return this.sentences.size();
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param author the author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}
	
	/**
	 * Extracts the year of text, from date of text was published. 
	 * NOTE: for simplicity, date ranges will be parsed as the earliest year (e.g. 1777-9 will be returned as 1777)
	 * @return the year 
	 */
	public int getYear() {
		String yearString = this.text_date.substring(0, 4); 
		return Integer.parseInt(yearString);
	}

	/**
	 * @param words the words to set
	 */
	public void setWords(int words) {
		this.words = words;
	}

	/**
	 * @return the words
	 */
	public int getWords() {
		return words;
	}
	

}
