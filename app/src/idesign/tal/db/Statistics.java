package idesign.tal.db;

import idesign.tal.App;
import idesign.tal.mwe.ExpressionHistogram;
import idesign.tal.mwe.Occurrence;
import idesign.tal.util.Histogram.Histogram;
import idesign.tal.util.Histogram.Trend;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.naming.spi.DirectoryManager;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.math3.exception.MathIllegalNumberException;
import org.apache.commons.math3.special.Erf;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

public class Statistics {

	private static final boolean DEBUG = true;
	private static final double ALPHA = .05;
	public static final double ZSCORE_THRESHOLD = 8; // for alpha=.01, set zscore to 3;
	public static final double[] DANIELS_TEST_WP = {0, 0, 0, 0, 0, .9429, .8929, .8571, .8167, .7818, .7455, .7273, .6978, .6747, .6536, .6342, .6152, .5975, .5825, .5684, .5545, .5426, .5306, .5200, .5100, .5002, .4915, .4828, .4744, .4665 , 2.5758}; 
	private Database database;
	private int segmentSize = 1;
	private ArrayList<String> expressions;
	private HashMap<String, ArrayList<Occurrence>> expressionsHistograms; // expression,
																			// histogram
	private HashMap<String, ArrayList<Occurrence>> expressionsHistogramsRelative;
	private ArrayList<ExpressionHistogram> expressionHistogramsFromDB;
	// after reading from DB, and processing "raw" histograms... I use
	private ArrayList<Histogram> histogramsSegmented7; // holds segmented (7
														// years) histograms of
														// expressions.
	private ArrayList<Histogram> histogramsSmoothedMA5; // holds segmented &
														// smoothed (moving avg.
														// 5) histograms of
														// expressions.
	private HashMap<Integer, Double> yearlyWordCountsHM;
	private ArrayList<Occurrence> yearlyWordCountsList;
	private int totalMWEfound;
	private int yearStart; // year corpus starts.
	private int yearEnd; // year corpus ends.
	private int minYearGap = Integer.MAX_VALUE;
	private int maxYearGap = Integer.MIN_VALUE;

	public Statistics(Database db) {

		this.database = db;
		// assert(segmentYears >= 1);
		// this.segmentSize = segmentYears;

		// 3.1 get number of expressions found
		this.totalMWEfound = database.getTotalMWEsFound();
		System.out.println("Total MWEs Found: " + this.totalMWEfound);

		// look at years in corpus, year gap, etc.
		this.yearsStats(); // finds maxYearGap
		this.segmentSize = this.maxYearGap;

		// TODO calculate average words per 1 year, and per segmentSize years
		// (7)
		// TODO calculate std words per 1 year, and per segmentSize

		this.loadExpressionHistograms();

		// 3.3 for each expression, count occurrences in each period
		// create a moving average for specific period (segmentSize; e.g.
		// 7/10/70 years)

		// TODO test this:
		this.histogramsSegmented7 = new ArrayList<Histogram>();
		this.segmentHistograms(this.expressionHistogramsFromDB,
				this.segmentSize, this.histogramsSegmented7);

		// TODO do the same as above, here:
		int movingAverageWindowSize = 5;
		this.histogramsSmoothedMA5 = new ArrayList<Histogram>();
		this.smoothHistograms(this.histogramsSegmented7,
				movingAverageWindowSize, this.histogramsSmoothedMA5);

		// TODO find pairs?

		// test conversion from z-score to p-value:
		/*
		 * double testp = 0.0; try { testp = Erf.erf(2.81); } catch
		 * (MathIllegalNumberException e) { e.printStackTrace(); }
		 * System.out.println(testp + " vs. .9920 in z table");
		 */
		// trend test for each expression

		// TODO complete aggregation code, for Corpus Trend:
		// Aggregate relative histograms, into a single one, in order to find
		// "corpus trend".
		// sum histograms into a baseline, or general behavior time-series.
		Histogram baseline = Statistics.sumHistograms(this.histogramsSmoothedMA5);
		double z = Statistics.trendTestKendall(baseline,0);

		if (Math.abs(z) > 3) {
			System.out.println("Found corpus trend (Kendall's tau z-score: " + z + ").");
			System.out.println("Aggregated Trend:" + baseline.toCSV(true));
			// TODO: convert to Trend.
		} else {
			System.out.println("Aggregated Trend not found.");
		}

		// TODO
		// this.exportHistogramsToCSV(this.histogramsSegmented7);
		// this.exportToCSV(this.histogramsSmoothedMA5);

		/*
		 * //Write Field names in row 1: Histogram tempHistogram = new
		 * Histogram(
		 * this.expressionHistogramsFromDB.get(0).getHistogramRelative()); int
		 * startingYear = this.yearStart +
		 * Histogram.getSegmentedHistogramLeftOver(tempHistogram,
		 * this.segmentSize);
		 * 
		 * System.out.print("Expression, segmentSize, histogramType"); // ... //
		 * add year column names for(int year=startingYear; year < this.yearEnd;
		 * year+=7) { System.out.print(", " + year + "_" + (year+6) ); }
		 * 
		 * System.out.print("\n");//(System.lineSeparator()); int
		 * movingAverageWindowSize = 5; for (ExpressionHistogram exp :
		 * this.expressionHistogramsFromDB) { Histogram originalHistogram = new
		 * Histogram(exp.getHistogramRelative()); Histogram segmentedHistogram =
		 * Histogram.segmentHistogram(originalHistogram, this.segmentSize);
		 * 
		 * Histogram smoothedSegmentedHistogram =
		 * Histogram.smoothMovingAverage(segmentedHistogram,
		 * movingAverageWindowSize);
		 * 
		 * if (DEBUG) { System.out.println(exp.getExpression() +
		 * ", "+this.segmentSize + ", \"segmented\", " +
		 * segmentedHistogram.toCSV()); System.out.println(exp.getExpression() +
		 * ", "+this.segmentSize + ", \"segmented-ma-" + movingAverageWindowSize
		 * + "\", " + smoothedSegmentedHistogram.toCSV()); } }
		 */
		// ...
		// 3.4 export (raw format?)
		// TODO (4) add some common words, as reference (the, of, ...).
	}

	/**
	 * String outputFilename
	 * String[] yearTableCellsArray
	 * ArrayList/HashMap expressions
	 */
	public void exportTrends2CSV() {
		System.out.println(
			"Found Trends in the following MWEs (7 year segments; Smoother: Moving Average 5, equal weights):");

		File outputFolder = new File(App.OUTPUT_PATH);
		File foundTrendsFile = new File(App.OUTPUT_PATH
				+ "/Expressions-with-trend.csv");
		FileWriter foundTrendsFileWriter;
		if (foundTrendsFile.exists())
			foundTrendsFile.delete();
		try {
			if (!outputFolder.exists()) {
				outputFolder.mkdir();
			}
			foundTrendsFile.createNewFile();
			foundTrendsFileWriter = new FileWriter(foundTrendsFile);
			foundTrendsFileWriter
					.write("z score, expression id, 1711-1717, 1718-1724, -1731, -1738, ...\n");

			for (Histogram expression : this.histogramsSmoothedMA5) {
				double z = Statistics.trendTestKendall(expression, 0);

				// didn't convert well:
				/*
				 * double p = 0.0; try { p = Erf.erf(z); } catch
				 * (MathIllegalNumberException e) { e.printStackTrace(); }
				 */
				// return p;

				// 2-tailed statistical test for Kendall's tau:
				if (Math.abs(z) > ZSCORE_THRESHOLD) { // eq. to p .000 p-value <.001 .999 //
										// .975 2.81) { // corresponds to p >
										// (1-ALPHA/2) ) {
					System.out.println(z + ", " + expression.toCSV(true));
					foundTrendsFileWriter.write(z + ", "
							+ expression.toCSV(true) + "\n");
				}
			}

			foundTrendsFileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Tests for Kendall's tau correlation coefficient, and returns z statistic.
	 * Based on Gray's doctorate description of Kendall's tau statistic.
	 * Test supports testing partial histograms, from a certain index.
	 * DEPRECATED: Uses Apache commons Math Erf Gamma Error function to convert
	 * z to p.
	 * 
	 * @param histogram
	 * @param testFromIndex 
	 * @return p-value
	 */
	public static double trendTestKendall(Histogram histogram, int testFromIndex) {

		int sumOfDifferences = 0;
		int n = histogram.size()-testFromIndex, nNotNaN=0;
		int finalIndex = histogram.size()-1;
		for (int i = testFromIndex; i < finalIndex; i++) {
			int greater = 0, smaller = 0; // other
			double valueI = histogram.getHistogramValue(i);
			if (valueI >= 0) { // a valid number; not NaN 
				nNotNaN += 1; 
				
				for (int j = i + 1; j <= finalIndex; j++) {
					double valueJ = histogram.getHistogramValue(j); // histogram value(s) after i
					if (valueJ >= 0 ) { // NOT NaN
						if ( valueJ > valueI ) greater++;
						else if (valueJ < valueI) smaller++;
						// NOTE: equal values are ignored.
					}
				}
				sumOfDifferences += greater - smaller;
			}
		}
		// Note: NOT SURE THIS IS RIGHT
		// n = nNotNaN;
		// NOTE: taking into account only valid values in histogram:
		// Kendall's tau
//		double tau = 2 * sumOfDifferences / n * (n - 1);
		double tau = 2.0 * sumOfDifferences / (n*(n - 1));
		// test statistic
		double z = tau / Math.sqrt( (2.0*(2*n+5)) / (9*n*(n-1)) );
		return z;
	}
	
	
	
	public static double getSpearmanRho(Histogram histogram, int testFromIndex) {
		int n = histogram.size()-testFromIndex;
		double[] ordering = new double[n];
		double[] values = new double[n];
		for (int i=0; i<n; i++) {
			ordering[i] = i;
			values[i] = histogram.getHistogramValue(i);
		}
		
		return new SpearmansCorrelation().correlation(values, ordering);
	}
	
	/*public static double trendTestDaniel(Histogram histogram, int testFromIndex) {
		// Daniel's Test for Trend, based on Spearman's rank coefficient
		// if |rho| > Wp, then a trend is declared significant - at the a=2p significance level.
		double rho = Statistics.getSpearmanRho(histogram, testFromIndex);
		
		// > 30 data points?
		// no: 
		// use Wp from table 2, in Guideline for Air Quality... 1974, pg. 22
		// e.g. Wp = 4.665 for n=30, and p=.995 
		// yes:
		// if Spearman's rho > Wp 
		// (Wp = Xp / sqrt(n-1), where Xp is the p quantile of a standard normal random variable obtained from
		// Table 3.  
		// e.g., Wp = -3.0902 for p=.001; (alpha < .05)
		// Wp = 2.7578 for p=.995;
		
		return false;
	}*/

	/*
	 * private void exportHistogramsToCSV( ArrayList<Histogram> histograms, File
	 * file) { // TODO: save CSV to file for (Histogram histogram : histograms)
	 * { System.out.println(histogram.toCSV(true)); }
	 * 
	 * }
	 */

	private void smoothHistograms(ArrayList<Histogram> histograms,
			int movingAverageWindowSize, ArrayList<Histogram> output) {
		for (Histogram histogram : histograms) {
			Histogram smoothedHistogram = Histogram.smoothMovingAverage(
					histogram, movingAverageWindowSize);
			output.add(smoothedHistogram);
		}

	}

	private void segmentHistograms(ArrayList<ExpressionHistogram> expressions,
			int segmentSize, ArrayList<Histogram> output) {

		for (ExpressionHistogram exp : expressions) {
			Histogram originalHistogram = new Histogram(exp.getExpression(),
					exp.getHistogramRelative());
			Histogram segmentedHistogram = Histogram.segmentHistogram(
					originalHistogram, segmentSize);
			output.add(segmentedHistogram);
		}

	}

	/**
	 * Sums all expression histgrams into an overall [average] Histogram.
	 * 
	 * @param expressions
	 * @return aggregated Histogram
	 */
	public static Histogram sumHistograms(ArrayList<Histogram> expressions) {

		double[] histogramSum = new double[expressions.get(0).size()];

		// for every histogram
		for (Histogram histogram : expressions) {

			// pass on all values
			for (int i = 0, len = histogram.size(); i < len; i++) {
				if (histogramSum[i] == 0) {// Was (Double) == null) {
					histogramSum[i] = histogram.getHistogramValue(i);
				} else {
					// sum into histogramSum
					histogramSum[i] += histogram.getHistogramValue(i);
				}
			}

		}
		return new Histogram(histogramSum);
	}

	private void yearsStats() {
		this.yearlyWordCountsHM = new HashMap<Integer, Double>();
		this.yearlyWordCountsList = database.getYearlyWordCounts();
		this.yearStart = this.yearlyWordCountsList.get(0).getYearFrom();
		this.yearEnd = this.yearlyWordCountsList.get(
				this.yearlyWordCountsList.size() - 1).getYearFrom();

		// convert to instance HashMap structure, for faster access, by year.
		for (int i = 0; i < this.yearlyWordCountsList.size(); i++) {
			this.yearlyWordCountsHM.put(this.yearlyWordCountsList.get(i)
					.getYearFrom(), this.yearlyWordCountsList.get(i)
					.getFrequency());
		}

		System.out.println("\n" + "Year Statistics:");
		System.out.println("Years in corpus: "
				+ (this.yearEnd - this.yearStart + 1) + " (" + this.yearStart
				+ "-" + this.yearEnd + ")");

		ArrayList<Integer> yearGaps = new ArrayList<Integer>();
		int yearGapsSum = 0;

		for (int i = 1, length = yearlyWordCountsList.size(); i < length; i++) {
			int gap = yearlyWordCountsList.get(i).getYearFrom()
					- yearlyWordCountsList.get(i - 1).getYearFrom();
			minYearGap = Math.min(minYearGap, gap);
			maxYearGap = Math.max(maxYearGap, gap);
			yearGapsSum += gap;
			yearGaps.add(gap);
		}

		System.out.println("Average year Gap: "
				+ ((double) yearGapsSum / yearGaps.size()));
		System.out.println("Greatest year Gap: " + maxYearGap);

	}

	private void loadExpressionHistograms() {

		this.expressionsHistograms = new HashMap<String, ArrayList<Occurrence>>();
		this.expressionsHistogramsRelative = new HashMap<String, ArrayList<Occurrence>>(); // relative
																							// -
																							// expression-frequency
																							// /
																							// word-frequency-in-that-year

		// if statistics expression histogram table doesn't contain all
		// expressions, repopulate it
		if (database.getTotalExpressionHistograms() < this.totalMWEfound) {
			database.clearStatsExpressionHistogramData();
			this.populateExpressionHistograms();
		}

		this.loadExpressionHistogramsFromDB();

		// TODO (May 2014:) If statistics_expression_histogram table exists, AND
		// has all (total) expressions, load them into appropriate arrays
		// ELSE, delete all expressions from table, and repopulate all over
		// again.

	}

	private void loadExpressionHistogramsFromDB() {
		// TODO Auto-generated method stub
		this.expressionHistogramsFromDB = database.getExpressionHistograms();
	}

	private void populateExpressionHistograms() {
		// 3.2 Get Expressions list
		expressions = new ArrayList<String>(database.getExpressionsFound());
		System.out.println("Listing " + expressions.size()
				+ " Expressions -----------");

		boolean zeroFill = true;

		// 3.3 for each expression, count occurrences in each period (year, but
		// can be decade, or 70 years)
		// 3.4 export to DB table statistics_expression_histogram (raw format?)
		// TODO? insert to DB if not exists already?
		for (int i = 0, len = expressions.size(); i < len; i++) {
			System.out.println(i + ". " + expressions.get(i));
			ArrayList<Occurrence> expressionHistogramRaw = this
					.getExpressionHistogram(expressions.get(i), zeroFill);
			ArrayList<Occurrence> expressionHistogramRelative = this
					.getExpressionHistogramRelative(expressions.get(i),
							zeroFill);

			// 3.3 for each expression, count occurrences in each period
			// (year/decade/70)

			// convert list of years, to CVS, zero-filled
			String expressionHistogramRelativeCSV = this
					.histogram2CSV(expressionHistogramRelative);
			String expressionHistogramRawCSV = this
					.histogram2CSV(expressionHistogramRaw);

			// TODO open new text file, write 1st line (expression,1710,1711,
			// ...), write line for each expression
			System.out.println(expressions.get(i) + ","
					+ expressionHistogramRelativeCSV);
			System.out.println("verying CSV size: "
					+ expressionHistogramRelative.size());

			// INSERT into DB Table (this will help retrieve the list, if
			// already exists... in the future, for further segmentation)
			// ... expression - relative occurrence per year
			// TABLE structure: String, Array as string "{1, 0 ,0, ...}"
			database.insertExpressionHistogram(expressions.get(i), "{"
					+ expressionHistogramRelativeCSV + "}", "{"
					+ expressionHistogramRawCSV + "}");

			// normalize counts 1/million ( interpolate?)
			// years with 0 words? - interpolation?
		}
	}

	/**
	 * converts an Occurrence array into a comma-separated string of frequencies
	 * 
	 * @param histogram
	 * @return comma-separated String containing the frequencies alone
	 */
	private String histogram2CSV(ArrayList<Occurrence> histogram) {
		StringBuffer buffer = new StringBuffer();

		// export frequencies only
		for (Occurrence o : histogram) {
			buffer.append(",").append(o.getFrequency());
		}
		// Convert expressionHistogramRelative to HM...
		/*
		 * HashMap<Integer, Double> expressionHistogramRelativeHM = new
		 * HashMap<Integer, Double>(); for (int i=0;
		 * i<expressionHistogramRelative.size(); i++) {
		 * expressionHistogramRelativeHM.put(
		 * expressionHistogramRelative.get(i).getYearFrom(),
		 * expressionHistogramRelative.get(i).getFrequency()); } for (int i=0;
		 * i< this.yearlyWordCountsList.size()-this.segmentYears+1;
		 * i=i+this.segmentYears) {
		 * 
		 * int yearFrom = this.yearlyWordCountsList.get(0).getYearFrom(); int
		 * yearTo = yearFrom + this.segmentYears -1; // = 1 for 1 year segment.
		 * 
		 * if (expressionHistogramRelativeHM.containsKey(yearFrom)) {
		 * buffer.append
		 * (",").append(expressionHistogramRelativeHM.get(yearFrom)); } else {
		 * buffer.append(",").append(0); } }
		 */

		// TODO: add 0 for all years not in yearlyWordXountsList

		return buffer.substring(1).toString(); // without the 1st ","
	}

	private ArrayList<Occurrence> histogramFromCSV() {
		return new ArrayList<Occurrence>();
	}

	/**
	 * get expression occurrence for each year it exists in the DB. saves the
	 * histogram to Statics instance, for future retrieval.
	 * 
	 * @param expression
	 * @return
	 */
	public ArrayList<Occurrence> getExpressionHistogram(String expression,
			boolean zeroFill) {

		// return expression's historgram of occurrences for each year in corpus
		// if not exits, retrieve from DB

		// return expression's historgram of occurrences for each year in corpus
		if (!this.expressionsHistograms.containsKey(expression)) {
			// retrieve from DB:
			ArrayList<Occurrence> expressionHistogram = database
					.getExpressionOccurrences(expression);
			// and add to Statistics instance
			this.expressionsHistograms.put(expression, expressionHistogram);
			if (App.DEBUG)
				System.out.println("Occurrences (Raw) of " + expression + ": "
						+ expressionHistogram);
		}

		if (zeroFill) {
			return this
					.histogramZeroFill(expressionsHistograms.get(expression));
		} else {
			return expressionsHistograms.get(expression);
		}

	}

	public ArrayList<Occurrence> getExpressionHistogramRelative(
			String expression, boolean zeroFill) {

		// create data structure, if not exists
		if (!this.expressionsHistogramsRelative.containsKey(expression)) {
			ArrayList<Occurrence> expressionHistogramRAW = this
					.getExpressionHistogram(expression, false);
			ArrayList<Occurrence> expressionHistogramRelative = new ArrayList<Occurrence>();

			for (int e = 0; e < expressionHistogramRAW.size(); e++) {
				int year = expressionHistogramRAW.get(e).getYearFrom();
				int words = this.getPeriodWordCounts(year, year); // word in
																	// curpus in
																	// this year
				// WARNING: words = 0 if corpus doesn't contain words for that
				// year.
				double relativeFrequency = expressionHistogramRAW.get(e)
						.getFrequency() / words;
				expressionHistogramRelative.add(new Occurrence(
						relativeFrequency, year, year));
			}

			// TODO put zero-filled array, instead?
			this.expressionsHistogramsRelative.put(expression,
					expressionHistogramRelative);

			if (App.DEBUG)
				System.out.println("Occurrences (Relative) of " + expression
						+ ": " + expressionHistogramRelative);
		}

		if (zeroFill) {
			return this.histogramZeroFill(expressionsHistogramsRelative
					.get(expression));
		} else {
			return expressionsHistogramsRelative.get(expression);
		}
	}

	/**
	 * Zero-fill years with no-occurrence, for simpler handling. Zero-fill takes
	 * into account the 1st and last year of the corpus, so histogram returned
	 * is full.
	 * 
	 * @param histogram
	 * @return ArrayList<Occurrence>
	 */
	private ArrayList<Occurrence> histogramZeroFill(
			ArrayList<Occurrence> histogram) {
		ArrayList<Occurrence> histogramZeroFilled = new ArrayList<Occurrence>();

		// Convert expressionHistogramRelative to HM...
		HashMap<Integer, Double> histogramHashMap = new HashMap<Integer, Double>();
		for (int i = 0; i < histogram.size(); i++) {
			histogramHashMap.put(histogram.get(i).getYearFrom(),
					histogram.get(i).getFrequency());
		}

		for (int yearIndex = this.yearStart, // histogram.get(0).getYearFrom(),
		yearFinal = this.yearEnd; // histogram.get(histogram.size()-1).getYearFrom();
		yearIndex <= yearFinal; yearIndex++) {
			// if HM contains this year, return the calculated frequency, else,
			// return NULL:
			if (histogramHashMap.containsKey(yearIndex)) {
				// use existing year frequency
				histogramZeroFilled.add(new Occurrence(histogramHashMap
						.get(yearIndex), yearIndex, yearIndex));
			} else {
				// zero-fill this year
				histogramZeroFilled
						.add(new Occurrence(0, yearIndex, yearIndex));
			}
		}
		return histogramZeroFilled;

	}

	/**
	 * Retrieve yearly words count (ONCE)
	 * 
	 * @param yearFrom
	 *            - period starts at this year
	 * @param yearTo
	 *            - period end (same as start will result in a single year
	 *            period)
	 * @return periodWordCount
	 */
	private int getPeriodWordCounts(int yearFrom, int yearTo) {

		int periodWordCount = 0;
		// and add to Statistics instance
		assert (yearFrom <= yearTo);

		for (int year = yearFrom; year <= yearTo; year++) {
			if (this.yearlyWordCountsHM.containsKey(year)) {
				// add words of this year, to period sum of words.
				periodWordCount += this.yearlyWordCountsHM.get(year); // HashMap
			}
			// else, no words in the corpus, for that year.
		}
		return periodWordCount;

	}

	public ArrayList<Trend> findTrends() {
		ArrayList<Trend> trends = new ArrayList<Trend>();

		for (Histogram histogram : this.histogramsSmoothedMA5) {
			double z = Statistics.trendTestKendall(histogram, 0);

			// 2-tailed statistical test for Kendall's tau:
			if (Math.abs(z) > 3) { // eq. to p .000 p-value <.001 .999 //
									// .975 2.81) { // corresponds to p >
									// (1-ALPHA/2) ) {
				trends.add(new Trend(histogram, z));
			}
		}
		return trends;
	}

	public ArrayList<Trend> filterBySparsity(ArrayList<Trend> trends,
			double maxSparsity) {
		ArrayList<Trend> selectTrends = new ArrayList<Trend>();

		for (Trend trend : trends) {
			double sparsity = trend.getSparsity();

			// 2-tailed statistical test for Kendall's tau:
			if (sparsity <= maxSparsity) {
				selectTrends.add(trend);
			}
		}
		return selectTrends;
	}

	public HashMap<Trend, ArrayList<Trend>> findNegativeCorrelatingTrends(
			ArrayList<Trend> selectTrends, 
			double correlationThreshold) {
		// Split into positive & negative trends:
		ArrayList<Trend> positiveTrends = new ArrayList<Trend>();
		ArrayList<Trend> negativeTrends = new ArrayList<Trend>();
		for (Trend trend : selectTrends) {
			if (trend.getStatistic() > 0)positiveTrends.add(trend);
			else negativeTrends.add(trend);
		}
		
		System.out.println("--------- Finding negative correlations ------- ");
		System.out.println(negativeTrends.size() + " Negative trends, " + positiveTrends.size() + " Positive trends");
		
		KendallsCorrelation kendallsCorrelation = new KendallsCorrelation();
		
		
		HashMap<Trend, ArrayList<Trend>> correlatedTrendsHashMap = new HashMap<Trend, ArrayList<Trend>>();
		int largestMostCorrelatedTrends = 0;
		int smallestMostCorrelatedTrends = Integer.MAX_VALUE;
		double largestCorrelation = 1; // should aspire to -1, ideally.
		
		for (Trend negativeTrend : negativeTrends) {
			ArrayList<Trend> mostCorrelatedTrends = new ArrayList<Trend>();
			
			System.out.println("\n");
			//System.out.println(negativeTrend.toCSV(true));
			
			for (Trend positiveTrend : positiveTrends) {
				double correlation = kendallsCorrelation.correlation(
						negativeTrend.getHistogramValues(),
						positiveTrend.getHistogramValues());
				
				if (correlation <= correlationThreshold) {
					System.out.println(
							"Kendall's Correlation between " +
							negativeTrend.getName() + " & " +
							positiveTrend.getName() + ": " +
							correlation
							);
					mostCorrelatedTrends.add(positiveTrend);
					
					largestCorrelation = Math.min(largestCorrelation, correlation);
				}
				
				
			}
			
			largestMostCorrelatedTrends = Math.max(largestMostCorrelatedTrends, mostCorrelatedTrends.size());
			smallestMostCorrelatedTrends = Math.min(smallestMostCorrelatedTrends, mostCorrelatedTrends.size());
			// add all positiveTrends that passed threshold:
			correlatedTrendsHashMap.put(negativeTrend, mostCorrelatedTrends);
		}
		// Find correlation between each negative trend, to every possible positive trend. 
		// then, select only the most negative correlating
		// for each negative trend
		
		System.out.println("The expression with the least correlated trends found had " + smallestMostCorrelatedTrends + " correlating trends.");
		System.out.println("The expression with the most correlated trends found had " + largestMostCorrelatedTrends + " correlating trends.");
		System.out.println("Smallest Correlation found: " + largestCorrelation);
		
		return correlatedTrendsHashMap;
	}
	
	
}
