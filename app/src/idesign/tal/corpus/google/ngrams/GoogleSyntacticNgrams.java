package idesign.tal.corpus.google.ngrams;

import idesign.tal.App;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import edu.stanford.nlp.io.EncodingFileReader;

/*
 * Google Syntactic Ngrams Corpus, based on 1M English books (randomly selected).
 */
public class GoogleSyntacticNgrams {

	public static final int YEAR_START = 1520;
	public static final int YEAR_END = 2008;
	private double[] totalCounts;
	private String corpusPath = null;
	// TODO fix start/end indexes of Google corpus:
	// UNCOMMENT, to Test:
//	private int startIndex = 30, endIndex = 30;
	// real
	private int startIndex = 00, endIndex = 05;//98 is final
	private int currentFile;

	public GoogleSyntacticNgrams(String corpusPath, String totalCountsFile) {
		this.corpusPath = corpusPath;
		this.currentFile = startIndex;
		this.loadTotalCounts(this.corpusPath + "/" + totalCountsFile);
	}

	public double getTotalCounts(int year) {
		return this.totalCounts[year];
	}

	public double[] getTotalCounts() {
		return this.getTotalCounts(YEAR_START, YEAR_END + 1); // ...till end.
	}

	public double[] getTotalCounts(int yearStart, int yearEnd) {
		// TODO Auto-generated method stub
		return Arrays.copyOfRange(this.totalCounts, yearStart, yearEnd + 1);
	}

	private void loadTotalCounts(String totalCountsFile) {
		this.totalCounts = new double[YEAR_END + 1];

		try {
			EncodingFileReader fileReader = new EncodingFileReader(
					totalCountsFile);

			// load totalCounts into double[] (index represents year)
			BufferedReader reader = new BufferedReader(fileReader);
			String line = reader.readLine(); // skip first line (comments)
			while ((line = reader.readLine()) != null) {
				String[] lineParts = line.split("\t");
				int year = Integer.parseInt(lineParts[0]);
				int year1GramCount = Integer.parseInt(lineParts[1]);
				// put in array:
				this.totalCounts[year] = year1GramCount;
			}
			if (reader != null)
				reader.close();

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

	public String getNextFilename(boolean isTest) {
		String filename;
		if (currentFile <= endIndex) {
			if (isTest) {
				filename = this.corpusPath + "/arcs." + String.format("%02d", currentFile)
						+ "-of-99.250K.test";
			} else {
				filename = this.corpusPath + "/arcs." + String.format("%02d", currentFile)
						+ "-of-99";
			}
			currentFile++;
			return filename;
		} else
			return null;

	}

}
