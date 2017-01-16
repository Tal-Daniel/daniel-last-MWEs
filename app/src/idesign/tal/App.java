/**
 * 
 */
package idesign.tal;

import idesign.tal.db.Database;
import idesign.tal.db.Statistics;
import idesign.tal.util.Histogram.Histogram;
import idesign.tal.util.Histogram.Trend;
import idesign.tal.corpus.*;
import idesign.tal.corpus.google.ngrams.GoogleSyntacticNgrams;
import idesign.tal.corpus.google.ngrams.GoogleSyntacticNgramsAnalyzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.derby.tools.sysinfo;
import org.xml.sax.SAXException;
import edu.mit.jmwe.data.IMWE;
import edu.mit.jmwe.data.IToken;
import edu.mit.jmwe.data.Token;
import edu.mit.jmwe.detect.Consecutive;
import edu.mit.jmwe.detect.IMWEDetector;
import edu.mit.jmwe.detect.LMLR;
import edu.mit.jmwe.index.*;

/**
 * @author tal
 * 
 */
public class App {

	/**
	 * @param args
	 */
	// NOTE: app running path is ./bin
	public final static boolean DEBUG = false;
	final static boolean STARTOVER = false;
	final static int startingTextFile = 0; // from which textFile to start (++
											// when app starts)

	private static enum OS {
		PC, OSX
	};

	/*
	 * final static class FUNCTION { public final static String
	 * READ_CORPUS_TO_DB_AND_FIND_MWE = "read"; public final static String STATS
	 * = "statistics"; };
	 */

	private static enum FUNCTION {
		READ_CORPUS_TO_DB_AND_FIND_MWE, STATS, LATER_FIX_ADD_WORD_COUNTS, GOOGLE_SYNTACTIC_NGRAMS_READ
	};

	final static OS currentOS = OS.PC;
	final static FUNCTION function = FUNCTION.GOOGLE_SYNTACTIC_NGRAMS_READ;

	private static final int SEGMENT_YEARS = 1;
	final static String MWE_INDEX_FILE_PATH = (currentOS == OS.OSX) ? "../../Resources/Libraries/edu.mit.jmwe_1.0.1_all/mweindex_wordnet3.0_semcor1.6.data"
			: "D:/Resources/jMWE/edu.mit.jmwe_1.0.1_all/mweindex_wordnet3.0_semcor1.6.data";
	final static String DATA_CORPUS_PATH = "./data/clmet3_0/sentence_per_line/";
	final static String dbConnectionURL = (currentOS == OS.OSX) ? "jdbc:postgresql://localhost:4242/mwe"
			: "jdbc:postgresql://localhost:5432/mwe";
	final static String dbUser = "postgres";
	final static String dbPassword = "tttt2pg";
	public final static String OUTPUT_PATH = "D:/Documents/My Personal Projects/Thesis/assets"; //"./output";

	private static final String GOOGLE_SYTACTIC_NGRAMS_CORPUS_PATH = (currentOS == OS.OSX) ? "../../Resources/NOTSETPATH"
			: "D:/Resources/Datasets/GoogleSyntacticNgrams";
	private static final String GOOGLE_NGRAMS_DATASET_TOTAL_COUNTS_PATH = "googlebooks-eng-1M-totalcounts-20090715.txt";
	private static final long MB = 10241024; // bytes
	private static final boolean GOOGLE_SEGMENT = true; // apply histogram segmentation?
	private static final boolean GOOGLE_SMOOTH = true; // apply histogram smoothing?
	private static final boolean GOOGLE_PARSE_FILES = true; // parse dataset files? (very long time). set true only on first run. afterwards, there's no need (DB stores matching MWEs in list).
	public static final int SMOOTHING_WINDOW_SIZE = 5;
	private static final boolean GOOGLE_LOAD_LAST_STATE = true; // to load last state from DB? used when you parse dataset in batches, not all, and you want to continue from last point. complements next const.
	private static final boolean GOOGLE_SAVE_STATE_TO_DB = true; // used when you parse dataset in batches, and you want to save the current findings, stop the application. complements previous const.

	static Analyzer analyzer;

	App() {
		// this.analyzer = new Analyzer();
		/*
		 * System.out.println(analyzer.lemmatize("this is my touch.")); //
		 * doc.getSentence(10) System.out.println(analyzer
		 * .lemmatize("Frozen inside without our touches two?\n")); //
		 * doc.getSentence(10)
		 */
		// TEST sentence analyzer:
		/*
		 * List<List<IToken>> sentences = analyzer .buildSentenceJMWE(
		 * "They stood up and raise their voice, so they could be heard.");
		 */

	}

	public static void main(String[] args) throws ParserConfigurationException,
			SAXException {
		// TODO Auto-generated method stub
		System.out.println("App started");

		Database db = new Database(dbConnectionURL, dbUser, dbPassword);
		if (STARTOVER) {
			db.deleteAllData();
		}

		CLMET corpusCMLET;
		DocumentAnalyzer docAnalyzer;

		String fileName; // current filename processed

		// String function = FUNCTION.STATS;
		switch (function) {
		case READ_CORPUS_TO_DB_AND_FIND_MWE:

			corpusCMLET = new CLMET(startingTextFile); // cstr. + starting file
														// number

			docAnalyzer = new DocumentAnalyzer(db);

			// load MWE index:
			File idxData = new File(MWE_INDEX_FILE_PATH);
			IMWEIndex index = new MWEIndex(idxData);
			// index.open();

			try {
				index.open();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// make a basic detector
			IMWEDetector detector = new Consecutive(index);
			LMLR longestLTRfilter = new LMLR(detector);

			/*
			 * 1. READ Corpus documents
			 */

			// if (DEBUG) fileName = DATA_CORPUS_PATH + "1710-1780/" +
			// "CLMET3_0_test.txt";
			// * else...
			while ((fileName = DATA_CORPUS_PATH + corpusCMLET.getNextDocPath()) != null
					&& fileName != null)

			{
				// TODO: filename contains full path, so isn't NULL, ever. FIX.

				// parse file and save MWEs into DB:
				// parse file:
				Document doc = new Document(fileName);
				// For each sentence in doc, find expressions:
				int countMWEs = 0;
				countMWEs += docAnalyzer.findMWEs(detector, longestLTRfilter,
						doc);
				docAnalyzer.countTokens(doc);
				/*
				 * for (int i = 0, len = doc.getSentencesSize(); i < len; i++) {
				 * String currentSentence = doc.getSentence(i); if (DEBUG)
				 * System.out.println("Sentence " + i +":\n" +currentSentence);
				 * List<List<IToken>> sentences = analyzer
				 * .buildSentenceJMWE(currentSentence);
				 * 
				 * for (List<IToken> sentence : sentences) { List<IMWE<IToken>>
				 * mwes = longestLTRfilter.detect(sentence); //was more simple:
				 * detector.detect(sentence); countMWEs += mwes.size(); //
				 * counts MWEs in document. for (IMWE<IToken> mwe : mwes) { if
				 * (DEBUG) System.out.println("\t"+mwe);
				 * 
				 * // TODO Attach to document // TODO save all MWE in one place?
				 * -> HashMap MWE -> {location, year, ...} } } }
				 */
				System.out.println("Total of " + countMWEs
						+ " MWEs found in \"" + doc.getTitle() + "\" ("
						+ doc.getYear() + ")");

				// NOTES:
				// there are 2-3 sentences sometimes in a single sentence
				// because of "." chars.

			}

			break;
		case STATS:
			System.out.println("----- Statistics mode ----------");
			// Calculate descriptives, and trends.
			Statistics stats = new Statistics(db);

			// TODO find trends, and save their [Kendall's tau] statistic
			ArrayList<Trend> trends = stats.findTrends();

			// export found trends to file (call function in Statistics).
			// TODO: improve... (trends)
			// stats.exportTrends2CSV();

			// for {... trend.toCSV()}...

			// filter, with max sparsity threshold
			ArrayList<Trend> selectTrends = stats.filterBySparsity(trends, .34);

			// find correlations
			HashMap<Trend, ArrayList<Trend>> correlatedTrendsHashMap = stats
					.findNegativeCorrelatingTrends(selectTrends, -0.6);

			App.exportCorrelatingTrends2CSV(correlatedTrendsHashMap,
					"Correlating Trends.csv");

			System.out.println("\n DONE.");
			break;

		case LATER_FIX_ADD_WORD_COUNTS:
			// pass all documents, and update word counts.
			// this step was added after all docs were already inserted into DB.
			corpusCMLET = new CLMET(startingTextFile); // cstr. + starting file
														// number
			docAnalyzer = new DocumentAnalyzer(db);

			// TODO: filename contains full path, so isn't NULL, ever. FIX.
			while ((fileName = DATA_CORPUS_PATH + corpusCMLET.getNextDocPath()) != null
					&& fileName != null) {
				// parse file:
				Document doc = new Document(fileName);
				// For each doc, find number of words:
				docAnalyzer.countTokens(doc);
			}
			break;

		case GOOGLE_SYNTACTIC_NGRAMS_READ:

			GoogleSyntacticNgrams googleCorpus = new GoogleSyntacticNgrams(
					GOOGLE_SYTACTIC_NGRAMS_CORPUS_PATH,
					GOOGLE_NGRAMS_DATASET_TOTAL_COUNTS_PATH);
			GoogleSyntacticNgramsAnalyzer googleAnalyzer = new GoogleSyntacticNgramsAnalyzer(
					MWE_INDEX_FILE_PATH, googleCorpus, true, db);

			// 0. load histograms from DB, if exist.
			if (GOOGLE_LOAD_LAST_STATE) googleAnalyzer.loadLastState();

			if (GOOGLE_PARSE_FILES) {
				// UNCOMMENT to parse files again.
				String googleFile = null;
				while ((googleFile = googleCorpus.getNextFilename(DEBUG)) != null
						&& googleFile != null) {
					// check its phrases, insert to DB new histograms, ...

					if (DEBUG)
						reportMemoryStatus();
					// If found phrase in phrase jMWE index,
					// OR internally classified as MWE, saves it in foundMWEs
					// HashMap.
					System.out.println("Analyzing " + googleFile + "...");
					googleAnalyzer.analyze(googleFile);
				}
			}

			if (DEBUG)
				reportMemoryStatus();

			// convert histograms to relative Histograms (normalize; different
			// HashMap)

			googleAnalyzer.normalizeHistograms();

			if (GOOGLE_SEGMENT) {
				googleAnalyzer.segmentHistograms();
			}

			if (GOOGLE_SMOOTH) {
				 googleAnalyzer.smoothHistograms(SMOOTHING_WINDOW_SIZE);
			}

			// find overall trend (Statistics.sumHistograms)
			googleAnalyzer.analyzeOverallTrend();

			if (DEBUG) reportMemoryStatus();
			// filter only trending Trends (& convert to Trends)
			System.out.println("finding trends...");
			googleAnalyzer.findTrends(); // note:
																		// saves
																		// to
																		// same
																		// object:
																		// foundExpressionTrends.
			if (DEBUG) reportMemoryStatus();
			// (optional) copy statistic into raw histograms, before saving to
			// DB.
			googleAnalyzer.syncStatistics();

			// Save to DB raw histograms found (hist. sum till now)
			// UNCOMMENT TO SAVE TO DB:
			if (GOOGLE_SAVE_STATE_TO_DB) googleAnalyzer.saveState();

			// export to CSV
			googleAnalyzer.exportTrends();
			googleAnalyzer.exportNoTrends(); // to check if synonym expressions are there, (even with no statistically significant trend)

			// DONE
			System.out.println("Done :)");
			break;

		default:
			System.out.println("Nothing to do; I quit.");
			System.exit(0);
		}

		// TODO 4. calculate in db word counts for each doc.

		// TODO (5) add some common words, as reference (the, of, ...).

		// TODO read expressions, and export to csv/JSON.

	}

	private static void reportMemoryStatus() {
		System.out.println("Left " + Runtime.getRuntime().freeMemory() / MB
				+ "/" + Runtime.getRuntime().totalMemory() / MB);
	}

	public static void exportCorrelatingTrends2CSV(
			HashMap<Trend, ArrayList<Trend>> correlatingTrends, String filename) {
		System.out
				.println("exporting trendCorrelations to CSV (7 year segments; Smoother: Moving Average 5, equal weights):");

		File outputFolder = new File(App.OUTPUT_PATH);
		File foundTrendsFile = new File(App.OUTPUT_PATH + "/" + filename);
		FileWriter exportFileWriter;
		if (foundTrendsFile.exists())
			foundTrendsFile.delete();
		try {
			if (!outputFolder.exists()) {
				outputFolder.mkdir();
			}
			foundTrendsFile.createNewFile();
			exportFileWriter = new FileWriter(foundTrendsFile);
			exportFileWriter
					.write("expression, z-score, correlation, z-score, correlated expression, 1711-1717, 1718-1724, -1731, -1738, ...\n");

			KendallsCorrelation kendallsCorrelation = new KendallsCorrelation();

			// For each trend, list correlating trends found.
			String line;

			for (Trend trend : correlatingTrends.keySet()) {

				line = ",,," + trend.toCSV(true);

				System.out.println(line);
				exportFileWriter.write(line + "\n");

				for (Trend correlatingTrend : correlatingTrends.get(trend)) {

					line = trend.getName()
							+ ", "
							+ trend.getStatistic()
							+ ", "
							+ kendallsCorrelation.correlation(
									trend.getHistogramValues(),
									correlatingTrend.getHistogramValues())
							+ ", " + correlatingTrend.toCSV(true);

					System.out.println(line);

					exportFileWriter.write(line + "\n");

					// expression z score,
					// Example: trend - e1 (z), e2 (z)
					// e0 ...
					// 01
				}

			}

			exportFileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
