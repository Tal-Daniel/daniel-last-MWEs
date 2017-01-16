package idesign.tal.corpus.google.ngrams;

import idesign.tal.Analyzer;
import idesign.tal.App;
import idesign.tal.db.Database;
import idesign.tal.db.Statistics;
import idesign.tal.mwe.ExpressionHistogram;
import idesign.tal.mwe.Occurrence;
import idesign.tal.util.Histogram.Histogram;
import idesign.tal.util.Histogram.Trend;
import idesign.tal.util.Histogram.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import edu.mit.jmwe.data.IMWE;
import edu.mit.jmwe.data.IToken;
import edu.mit.jmwe.data.Token;
import edu.mit.jmwe.detect.Consecutive;
import edu.mit.jmwe.detect.IMWEDetector;
import edu.mit.jmwe.detect.LMLR;
import edu.mit.jmwe.index.IMWEIndex;
import edu.mit.jmwe.index.MWEIndex;
import edu.stanford.nlp.io.EncodingFileReader;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.WordLemmaTag;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class GoogleSyntacticNgramsAnalyzer {

	private static final String FIELD_SEPARATOR = "\t";
	private static final String EXPRESSION_SEPARATOR = " ";
	private static final int SUSPECT_EXPRESSIONS_FROM_INDEX = 29; // year 1904,
																	// calculated
																	// as
																	// yearStart
																	// (1701) +
																	// period(7)
																	// * 29 bins
																	// on
																	// histogram.
	/** Currently using no spartisy & count thresholds. 
	 *  Change them to other values, when necessary.
	 */
	private static final double THRESHOLD_SPARSITY = .267; // 1 = 100% sparsity. //default .267; // 4 periods * 7 years
															// per segment = 28 years. 
															// That's 0.93 sparsity for whole histogram,
															// and 4/15 for the partial histogram we check, starting at index 29 (year 1904).
	private static final double THRESHOLD_COUNTS = 1360; //1360;
	// raw count threshold calculated from (rawCount/1.11432E+11)=1.22E-08 [the
	// normal freq.]
	// where 1.22E-08 is the minimum count of found MWEs, in indexed MWEs
	// (1701-2008)
	private static final boolean VERBOSE = false;
	private static final int SEGMENT_SIZE = 7;
	private Database database;
	private LMLR mweDetector;
	private String fileName = null;
	private EncodingFileReader fileReader = null;

	HashMap<String, Trend> foundExpressionsHM = new HashMap<>(), suspectExpressionsHM = new HashMap<>();
	private HashMap<String, Histogram> normalizedFoundExpressionsHM, normalizedSuspectExpressionsHM,
			normalizedFoundExpressionsHMSegmented = new HashMap<>();
	private Stats foundExpressionsStats, suspectExpressionsStats;
	private GoogleSyntacticNgrams googleCorpus;
	private Histogram totalCountsHistogram;
	private ArrayList<Trend> foundExpressionTrends, suspectExpressionsTrends;
	private int HISTOGRAM_PARSE_FROM_YEAR = 1701;
	private boolean trimHistograms;
	private int yearStart;
	private boolean FIND_SUSPECT_EXPRESSIONS = true;
	private boolean isSegmentedHistograms = false,
			isSmooothedHistograms = false;
	private ArrayList<Trend> normalizedExpressionsNoTrend = null,
			suspectExpressionsNoTrend = null;

	// private ArrayList<String> sentences;
	// private List<List<IMWE<IToken>>> expressions;

	public GoogleSyntacticNgramsAnalyzer(String mweIndexFile,
			GoogleSyntacticNgrams googleCorpus, boolean trimHistograms,
			Database db) {

		this.googleCorpus = googleCorpus;
		this.database = db;
		this.trimHistograms = trimHistograms;
		this.yearStart = (this.trimHistograms) ? HISTOGRAM_PARSE_FROM_YEAR
				: GoogleSyntacticNgrams.YEAR_START;
		this.totalCountsHistogram = new Histogram("Corpus totalCounts",
				googleCorpus.getTotalCounts(this.yearStart,
						GoogleSyntacticNgrams.YEAR_END));

		this.normalizedExpressionsNoTrend = new ArrayList<Trend>();
		this.suspectExpressionsNoTrend = new ArrayList<Trend>();
		this.foundExpressionsStats = new Stats();
		this.suspectExpressionsStats = new Stats();

		// this.database = db;

		// load MWE index:
		File idxData = new File(mweIndexFile);
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
		this.mweDetector = new LMLR(detector);

	}

	public boolean analyze(String fileName) {
		this.fileName = fileName;

		try {
			fileReader = new EncodingFileReader(this.fileName);
			this.parseDocument(fileReader);
			this.printStats();

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
		return false;
	}

	public void printStats() {
		this.foundExpressionsStats.syncTrendsStats(this.foundExpressionsHM);
		System.out
				.println("\nWordNet expressions found till now in Google corpus:");
		this.foundExpressionsStats.printStats();

		if (FIND_SUSPECT_EXPRESSIONS && (this.suspectExpressionsHM != null)) {
			this.suspectExpressionsStats
					.syncTrendsStats(this.suspectExpressionsHM);
			System.out
					.println("\nSuspect expressions found till now in Google corpus: ");
			this.suspectExpressionsStats.printStats();
		}
	}

	private void parseDocument(EncodingFileReader fileReader) {

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

	/**
	 * tries to find MWE in WordNet index, and files it in foundMWEs HashMap.
	 * 
	 * @param line
	 * @param lineNumber
	 */
	private void parseLine(String line, int lineNumber) {

		// TODO new Trend...

		// If found phrase in phrase jMWE index,
		// OR internally classified as MWE... file it in this.foundMWEs HashMap.
		String[] lineParts = line.split(FIELD_SEPARATOR);
		String[] expressionParts = lineParts[1].split(EXPRESSION_SEPARATOR);
		int countSum = Integer.parseInt(lineParts[2]);
		int histogramStartIndex = 3; // after counts.

		String expressionId = "", expressionExample = "";
		List<IToken> sentence_JMWE = new ArrayList<IToken>(); // "sentence" that
																// will be
																// checked
																// against the
																// Semcor/WordNet
																// list
		for (String token : expressionParts) {
			// each token format is “word/pos-­tag/dep-­label/head-­index”.
			// pos-­tag is a Penn-­Treebank part-­of-­speech tag.
			// dep-­label is a stanford-­basic-­dependencies label.
			// head-­index is an integer, pointing to the head of the current
			// token. “1” refers to the first token in
			// the list, 2 the second, and 0 indicates that the head is the root
			// of the fragment.
			String[] tokenParts = token.split("/");

			if (tokenParts[0].length() > 0) {
				tokenParts[0] = this.fixPecularities(tokenParts[0]);

				if (tokenParts[0].length() == 0) {
					if (GoogleSyntacticNgramsAnalyzer.VERBOSE) {
						System.out.println("SKIPPED line " + line);
					}
					return;
				}
				// get lemma, using StanfordCoreNLP:
				WordLemmaTag wordLemmaTag = new WordLemmaTag(tokenParts[0],
						tokenParts[1]);

				// convert to jMWE sentence (each token need the original word,
				// the tag, and lemma):
				sentence_JMWE.add(new Token(wordLemmaTag.word(), wordLemmaTag
						.tag(), wordLemmaTag.lemma()));

				expressionExample += wordLemmaTag.word() + " "; // word_word_...word_
				expressionId += wordLemmaTag.lemma() + "_";

			}
		}

		// remove last "_" to get expressionTitle (NOTE: no POS, just words).
		if (expressionExample.length() > 0) {
			expressionExample = expressionExample.substring(0,
					expressionExample.length() - 1);
			expressionId = expressionId.substring(0, expressionId.length() - 1);
		} else {
			System.out.print("empty expression, ");
		}

		// done creating jMWE sentence from parts.
		// check for matching MWE in the WordNet expressions index (jMWE)
		// Detect if MWE in WordNet/Semcor 3.0/1.6:
		List<IMWE<IToken>> mwes = this.mweDetector.detect(sentence_JMWE);

		if (mwes.size() > 0) {
			for (IMWE<IToken> mwe : mwes) {
				if (App.DEBUG && GoogleSyntacticNgramsAnalyzer.VERBOSE) {
					System.out.println("\t mweDetector found "
							+ mwe.getEntry().getID() + " "
							+ mwe.getForm().toString() + " "
							+ mwe.getTokens().toString());
					System.out.println("\t line " + lineNumber + ": " + mwe
							+ " -- (count: " + countSum + ") in line: " + line);
				}
				// parse histogram
				// NOTE: histograms in line are written in compact mode (as
				// list; years with no books are missing)
				ArrayList<Occurrence> histogramList = this.parseHistogram(
						lineParts, histogramStartIndex, this.trimHistograms);

				Histogram countsHistogram = new Histogram(mwe.getEntry()
						.getID().toString(), histogramList, yearStart,
						GoogleSyntacticNgrams.YEAR_END);
				// expression -> total count, histogram
				// Add expression to internal MWEs index,
				// NOTE: not unique, so combines identical expressions to same
				// hashMap entry.
				// (ignores dependencies; takes into account only POS)
				String mweId = mwe.getEntry().getID().toString();
				
				// Add new MWE found, or update existing one in HashMap.
				if (!foundExpressionsHM.containsKey(mweId)) {
					// add new HM entry.
					foundExpressionsHM.put(mwe.getEntry().getID().toString(),
							new Trend(countsHistogram));
					if (GoogleSyntacticNgramsAnalyzer.VERBOSE) {
						System.out.println("+ MWE: "
								+ countsHistogram.toString());
						System.out
								.println(foundExpressionsHM.size() + " mwes.");
					}

				} else {
					// update HM entry
					Histogram foundHistogram = foundExpressionsHM.get(mweId);
					Histogram combinedHistogram = foundHistogram
							.add(countsHistogram.getHistogramValues());
					foundExpressionsHM.put(mweId, new Trend(combinedHistogram));

					if (GoogleSyntacticNgramsAnalyzer.VERBOSE) {
						System.out.println("~ MWE: "
								+ combinedHistogram.toString());
					}
				}
			}

			// if no matching MWE was found... 
		} else if (FIND_SUSPECT_EXPRESSIONS) {
			// collocation wasn't found in Index,
			// so I save it in a different place, if it passes some thresholds.
			// TODO may not be correct to filter counts here, try to filter
			// counts at the end, instead... because sum of identical
			// expressions may pass threshold.
			// 
			if (countSum > THRESHOLD_COUNTS) {
				// parse histogram, and test sparsity:
				ArrayList<Occurrence> histogramList = this.parseHistogram(
						lineParts, histogramStartIndex, this.trimHistograms);

				Histogram countsHistogram = new Histogram(expressionId,
						histogramList, yearStart,
						GoogleSyntacticNgrams.YEAR_END);

				// check if it's rather a full histogram (default sparsity
				// THRESHOLD: 0.3).
				// note, we're interested in expressions that appear since 1904,
				// and not in really old expressions (0 counts, prior to 1904)
				// note: sparsity = 1 equals to all 0 counts in histogram.

				// FIXME histograms are not segmented, nor smoothed(!) so all thresholds need to be updated again. 
				// need to segment & smooth before sparsity & counts could be used.
				Histogram processedHistogram = Histogram.segmentHistogram(countsHistogram, SEGMENT_SIZE);
				processedHistogram = Histogram.smoothMovingAverage(processedHistogram, App.SMOOTHING_WINDOW_SIZE);
				boolean isEmptyBefore1904 = (processedHistogram.getSparsity(0, SUSPECT_EXPRESSIONS_FROM_INDEX-1) == 1);
				boolean isMinimum4Period = processedHistogram.getSparsity(SUSPECT_EXPRESSIONS_FROM_INDEX,
						processedHistogram.size() - 1) <= THRESHOLD_SPARSITY; 
				if (isMinimum4Period && isEmptyBefore1904) {

					if (!suspectExpressionsHM.containsKey(expressionId)) {
						// add new HM entry
						suspectExpressionsHM.put(expressionId, new Trend(countsHistogram));
						if (App.DEBUG && GoogleSyntacticNgramsAnalyzer.VERBOSE) {
							System.out.println("+ MWE Suspect: "
									+ countsHistogram.toString());
							System.out.println(suspectExpressionsHM.size()
									+ " suspect MWEs in memory.");
						}
					} else {
						// combine histogram with previous HM entry
						Histogram foundHistogram = suspectExpressionsHM
								.get(expressionId);
						Histogram combinedHistogram = foundHistogram
								.add(countsHistogram.getHistogramValues());
						suspectExpressionsHM.put(expressionId,
								new Trend(countsHistogram));
						if (GoogleSyntacticNgramsAnalyzer.VERBOSE) {
							System.out.println("~ MWE Suspect: "
									+ combinedHistogram.toString());
						}
					}
				}
			}

		}

	}

	
	private String fixPecularities(String text) {
		// replace "_" with "-" (_ is used as a word/pos separator, in jMWE lib)
		String fixedText = text.replaceAll("_", "-");
		// remove "-"(s), if last char
		while (fixedText.length() >= 1
				&& (fixedText.lastIndexOf("-") == fixedText.length() - 1)) {
			fixedText = fixedText.substring(0, fixedText.length() - 1);
		}
		return fixedText;
	}

	/*
	 * private Histogram parseHistogram(String[] unparsedHistogram, int
	 * histogramStartIndex ) { int histogramLength = unparsedHistogram.length -
	 * histogramStartIndex; int[] years = new int[histogramLength]; double[]
	 * counts = new double[histogramLength]; //, counts; for (int i=0; i <
	 * histogramLength; i++) { String[] parts =
	 * unparsedHistogram[i+histogramStartIndex].split(","); years[i] =
	 * Integer.parseInt(parts[0]); counts[i] = Double.parseDouble(parts[1]); }
	 * 
	 * return new Histogram(counts); }
	 */

	/**
	 * Parse <year,count> String pairs, into a list of Occurrences. Which can be
	 * converted later as a Histogram (a zero-filled double[]), based on this
	 * list.
	 * 
	 * @param unparsedHistogram
	 * @param histogramStartIndex
	 * @return
	 */
	private ArrayList<Occurrence> parseHistogram(String[] unparsedHistogram,
			int histogramStartIndex, boolean trimHistogram) {
		int histogramLength = unparsedHistogram.length - histogramStartIndex;

		ArrayList<Occurrence> occurences = new ArrayList<Occurrence>();

		for (int i = 0; i < histogramLength; i++) {
			String[] parts = unparsedHistogram[i + histogramStartIndex]
					.split(",");
			int year = Integer.parseInt(parts[0]);
			double count = Double.parseDouble(parts[1]);

			// get partial histogram
			if (trimHistogram && year >= HISTOGRAM_PARSE_FROM_YEAR)
				occurences.add(new Occurrence(count, year, year));

		}

		return occurences;
	}

	// no need, Histogram contains calculated histogramTotal
	/*
	 * private class GoogleHistogram { public Histogram countsHistogram; public
	 * int countsTotal;
	 * 
	 * }
	 */

	private class Stats {
		public int mwesFound = 0;
		public int minHistogramCount = Integer.MAX_VALUE,
				maxHistogramCount = Integer.MIN_VALUE;
		public double minHistogramSparsity = 1, maxHistogramSparsity = 0;
		private int sumHistogramCount;
		private double sumHistogramSparsity;

		public Stats() {
		}

		public void syncHistogramsStats(
				HashMap<String, Histogram> foundExpressionsHM) {
			mwesFound = 0;
			for (Entry<String, Histogram> h : foundExpressionsHM.entrySet()) {
				mwesFound += 1;
				int histogramCount = (int) h.getValue().getHistogramSum();
				this.sumHistogramCount += histogramCount;
				this.maxHistogramCount = (int) Math.max(this.maxHistogramCount,
						histogramCount);
				this.minHistogramCount = (int) Math.min(this.minHistogramCount,
						histogramCount);

				double histogramSparsity = h.getValue().getSparsity();
				this.sumHistogramSparsity += histogramSparsity;
				this.maxHistogramSparsity = Math.max(this.maxHistogramSparsity,
						histogramSparsity);
				this.minHistogramSparsity = Math.min(this.minHistogramSparsity,
						histogramSparsity);
			}

		}

		public void syncTrendsStats(HashMap<String, Trend> foundExpressionsHM) {
			mwesFound = 0;
			for (Entry<String, Trend> h : foundExpressionsHM.entrySet()) {
				mwesFound += 1;
				int histogramCount = (int) h.getValue().getHistogramSum();
				this.sumHistogramCount += histogramCount;
				this.maxHistogramCount = (int) Math.max(this.maxHistogramCount,
						histogramCount);
				this.minHistogramCount = (int) Math.min(this.minHistogramCount,
						histogramCount);

				double histogramSparsity = h.getValue().getSparsity();
				this.sumHistogramSparsity += histogramSparsity;
				this.maxHistogramSparsity = Math.max(this.maxHistogramSparsity,
						histogramSparsity);
				this.minHistogramSparsity = Math.min(this.minHistogramSparsity,
						histogramSparsity);
			}

		}

		public double getAverageHistogramCount() {
			return (double) this.sumHistogramCount / this.mwesFound;
		}

		public double getAverageHistogramSparsity() {
			return this.sumHistogramSparsity / mwesFound;
		}

		/**
		 * printStats.
		 */
		public void printStats() {
			System.out.println(this.mwesFound + " expressions;");
			System.out.println("Counts: min " + this.minHistogramCount
					+ ", max " + this.maxHistogramCount + ", avg "
					+ this.getAverageHistogramCount());
			System.out.println("Sparsity: min " + this.minHistogramSparsity
					+ ", max " + this.maxHistogramSparsity + ", avg "
					+ this.getAverageHistogramSparsity());
		}

	}

	public void analyzeOverallTrend() {
		Histogram normalizedOverallHistogram = null;
		if (!this.isSegmentedHistograms && !this.isSmooothedHistograms) {
			Histogram foundExpressionsSumHistogram = Util
					.sumTrends(this.foundExpressionsHM);
			normalizedOverallHistogram = foundExpressionsSumHistogram
					.normalize(this.totalCountsHistogram);
		} else { // segmented/smoothed; use normalized histogram, instead:
			normalizedOverallHistogram = Util
					.sumHistograms(this.normalizedFoundExpressionsHM);
		}

		double z = Statistics.trendTestKendall(normalizedOverallHistogram, 0);

		if (Math.abs(z) > 3) {
			System.out
					.println("Found trend in Google corpus found MWEs (Kendall's tau z-score: "
							+ z + ").");
		} else {
			System.out.println("No trend in Google corpus found MWEs.");
		}
		System.out.println("Overall normalized histogram, from "
				+ this.yearStart + "-:"
				+ normalizedOverallHistogram.toCSV(true));
	}

	public void normalizeHistograms() {
		if (this.normalizedFoundExpressionsHM != null) {
			return;
		}
		// create a new hash map
		this.normalizedFoundExpressionsHM = new HashMap<String, Histogram>();
		// for every Entry in hashmap, save a normalized version in new HashMap:
		for (Entry<String, Trend> entry : this.foundExpressionsHM.entrySet()) {
			this.normalizedFoundExpressionsHM.put(entry.getKey(), entry
					.getValue().normalize(this.totalCountsHistogram));
		}

		System.out.println("Normalized found MWEs in Google Corpus.");

		if (FIND_SUSPECT_EXPRESSIONS) {
			if (this.normalizedSuspectExpressionsHM != null) {
				return;
			}
			// create a new hash map
			this.normalizedSuspectExpressionsHM = new HashMap<String, Histogram>();
			// for every Entry in hashmap, save a normalized version in new
			// HashMap:
			for (Entry<String, Trend> entry : this.suspectExpressionsHM
					.entrySet()) {
				this.normalizedSuspectExpressionsHM.put(entry.getKey(), entry
						.getValue().normalize(this.totalCountsHistogram));
			}

			System.out
					.println("Normalized suspect MWEs in Google Corpus. (count & sparsity threshold).");
		}
	}

	/**
	 * Segments normalized expressions HashMap to period of 7 years. Note: when
	 * done, deletes original, non-segmented hashMap.
	 */
	public void segmentHistograms() {
		this.isSegmentedHistograms = true;
		System.out.println("segmenting histograms");
		HashMap<String, Histogram> segmentedHistograms = this
				.segmentHistograms(this.normalizedFoundExpressionsHM,
						SEGMENT_SIZE);
		this.normalizedFoundExpressionsHM.clear();
		this.normalizedFoundExpressionsHM = null;
		this.normalizedFoundExpressionsHM = segmentedHistograms;

		if (this.normalizedSuspectExpressionsHM != null) {
			HashMap<String, Histogram> segmentedSuspectHistograms = this
					.segmentHistograms(this.normalizedSuspectExpressionsHM,
							SEGMENT_SIZE);
			this.normalizedSuspectExpressionsHM.clear();
			this.normalizedSuspectExpressionsHM = null;
			this.normalizedSuspectExpressionsHM = segmentedSuspectHistograms;

		}
	}

	private HashMap<String, Histogram> segmentHistograms(
			HashMap<String, Histogram> expressions, int segmentSize) {

		HashMap<String, Histogram> segmentedExpressionsHM = new HashMap<String, Histogram>();

		for (Entry<String, Histogram> entry : expressions.entrySet()) {
			String key = entry.getKey();
			segmentedExpressionsHM.put(key,
					Histogram.segmentHistogram(entry.getValue(), segmentSize));
		}

		return segmentedExpressionsHM;
		// replace old HashMap
		// expressions.clear(); //this.normalizedFoundExpressionsHM.clear();
		// expressions = null; //this.normalizedFoundExpressionsHM = null;
		// expressions = segmentedExpressionsHM;
		// //this.normalizedFoundExpressionsHM = segmentedExpressionsHM;
	}

	public void smoothHistograms(int smoothingWindowSize) {
		this.isSmooothedHistograms = true;
		System.out.println("smoothing histograms");
		HashMap<String, Histogram> smoothedHistograms = null;

		if (this.normalizedFoundExpressionsHM != null
				&& this.normalizedFoundExpressionsHM.size() > 0) {
			smoothedHistograms = this.smoothHistograms(
					this.normalizedFoundExpressionsHM, smoothingWindowSize);

		} else if (this.normalizedFoundExpressionsHMSegmented != null
				&& this.normalizedFoundExpressionsHMSegmented.size() > 0) {
			// FIXME remove this. not used HashMap(?)
			smoothedHistograms = this.smoothHistograms(
					this.normalizedFoundExpressionsHMSegmented,
					smoothingWindowSize);
		}

		this.normalizedFoundExpressionsHM.clear();
		this.normalizedFoundExpressionsHM = null;
		this.normalizedFoundExpressionsHM = smoothedHistograms;

		// smooth suspect expressions, if flag was true
		if (this.normalizedSuspectExpressionsHM != null
				&& this.normalizedSuspectExpressionsHM.size() > 0) {
			HashMap<String, Histogram> smoothedSuspectHistograms = this
					.smoothHistograms(this.normalizedSuspectExpressionsHM,
							smoothingWindowSize);
			this.normalizedSuspectExpressionsHM.clear();
			this.normalizedSuspectExpressionsHM = null;
			this.normalizedSuspectExpressionsHM = smoothedSuspectHistograms;
		}
	}

	/**
	 * Smoothes histograms in HashMap (updates current HashMap)
	 * 
	 * @param expressionsHM
	 * @param smoothingWindowSize
	 * @return
	 */
	private HashMap<String, Histogram> smoothHistograms(
			HashMap<String, Histogram> expressionsHM, int smoothingWindowSize) {
		HashMap<String, Histogram> smoothedExpressionsHM = new HashMap<String, Histogram>();

		for (Entry<String, Histogram> entry : expressionsHM.entrySet()) {
			String key = entry.getKey();
			smoothedExpressionsHM.put(key, Histogram.smoothMovingAverage(
					entry.getValue(), smoothingWindowSize));
		}

		return smoothedExpressionsHM;
		// finally, replace old HashMap
		// expressionsHM.clear();
		// //this.normalizedFoundExpressionsHMSegmented.clear();
		// expressionsHM = null; // this.normalizedFoundExpressionsHMSegmented =
		// null;
		// expressionsHM = smoothedExpressionsHM;
		// //this.normalizedFoundExpressionsHM = smoothedExpressionsHM;

	}

	public void findTrends() {
		if (!this.isSegmentedHistograms && !this.isSmooothedHistograms) {
			this.findTrends(this.normalizedFoundExpressionsHM);
		} else if (this.isSegmentedHistograms && !this.isSmooothedHistograms) {
			// TODO verify this structure is still used. I think not...
			this.findTrends(this.normalizedFoundExpressionsHMSegmented);
		} else if (this.isSegmentedHistograms && this.isSmooothedHistograms) {
			this.findTrends(this.normalizedFoundExpressionsHM);
		}
		
		
		// find trends in suspect expressions (no guarantee they are MWEs!)
		if (FIND_SUSPECT_EXPRESSIONS
				&& this.normalizedSuspectExpressionsHM != null) {
			// if suspect expressions are too numerous, select only trending  
			if (THRESHOLD_COUNTS == 0 && THRESHOLD_SPARSITY==1) {
				this.suspectExpressionsTrends = this.selectTrends(
						this.normalizedSuspectExpressionsHM,
						null, SUSPECT_EXPRESSIONS_FROM_INDEX); // all histograms will return trends array, and move all else to ...noTrend array.
			} else {
				// split suspect expressions into trend / no trend:
				this.suspectExpressionsTrends = this.selectTrends(
						this.normalizedSuspectExpressionsHM,
						this.suspectExpressionsNoTrend, SUSPECT_EXPRESSIONS_FROM_INDEX); // all histograms will return trends array, and move all else to ...noTrend array.
			}
			
			System.out.println("Found " + this.suspectExpressionsTrends.size()
					+ " Trends (suspect MWEs, count threshold: "
					+ THRESHOLD_COUNTS + ", sparsity threshold: "
					+ THRESHOLD_SPARSITY + ") out of "
					+ this.normalizedSuspectExpressionsHM.size()
					+ " suspect(!) expressions.");
		}
	}

	/**
	 * Filters expressions found, to select only expressions with a trend. Uses
	 * Kendall's tau statistical test, and z>3, for .99 confidence level.
	 */
	public void findTrends(HashMap<String, Histogram> histograms) {
		this.foundExpressionTrends = this.selectTrends(histograms,
				this.normalizedExpressionsNoTrend, 0); // all histograms will return trends array, and move all else to ...noTrend array.
		System.out.println("Found " + this.foundExpressionTrends.size()
				+ " Trends (MWEs found in WordNet), out of "
				+ histograms.size() + " MWEs.");
	}

	/**
	 * Selects trends from a HashMap and return an ArrayList, as a prior step
	 * for exporting to CSV, or DB.
	 * 
	 * @param histograms
	 * @return
	 */
	private ArrayList<Trend> selectTrends(
			HashMap<String, Histogram> histograms,
			ArrayList<Trend> noTrendsArray, int testFromIndex) {
		ArrayList<Trend> trends = new ArrayList<Trend>();
		for (Entry<String, Histogram> entry : histograms.entrySet()) {
			Histogram histogram = entry.getValue();
			double z = Statistics.trendTestKendall(histogram, testFromIndex);
			double rho = Statistics.getSpearmanRho(histogram, testFromIndex);
			int n = histogram.size()-testFromIndex;
			double danielsTestCriticalValue = (n>=30)? (Statistics.DANIELS_TEST_WP[30]/Math.sqrt(n-1)) : Statistics.DANIELS_TEST_WP[n]; 
			double []statistics = {z, rho};
			// If either stat. significant in Kendall's tau z score, or Daniels Test for trend (based on Spearman's rho), 
			// add to trends:
			if (Math.abs(z) > Statistics.ZSCORE_THRESHOLD 
					|| Math.abs(rho) > danielsTestCriticalValue) { // eq. to p .000
//			if (Math.abs(z) > Statistics.ZSCORE_THRESHOLD) { // eq. to p .000
																// p-value <.001
																// .999 //
				// .975 2.81) { // corresponds to p >
				// (1-ALPHA/2) )
				trends.add(new Trend(histogram, statistics));
//				trends.add(new Trend(histogram, z));
			} else {
				// maintain separate list for no trends.
				// later, export to CSV
				if (noTrendsArray != null) {
					noTrendsArray.add(new Trend(histogram, statistics));
				}
			}
		}
		return trends;
	}

	/**
	 * Copies Kendall's tau statistic from normalized trends, to raw counts
	 * trends.
	 */
	public void syncStatistics() {
		this.copyStatistics(this.foundExpressionTrends, this.foundExpressionsHM);
	}

	/**
	 * Copies Kendall's tau statistic from array to hash-map. Note that not all
	 * keys in hash-map may change their statistic value.
	 * 
	 * @param from
	 * @param to
	 */
	public void copyStatistics(ArrayList<Trend> from, HashMap<String, Trend> to) {
		for (Trend trend : from) {
			to.get(trend.getName()).setStatistic(trend.getStatistic());
		}
	}

	public void exportTrends() {

		if (!this.isSegmentedHistograms && !this.isSmooothedHistograms) {
			String periodTableCells = "";
			for (int i = this.yearStart; i <= GoogleSyntacticNgrams.YEAR_END; i++) {
				periodTableCells += i + ", ";
			}
			this.exportTrends(this.foundExpressionTrends,
					"Expressions-with-trend-in-GSNgramsCorpus.csv",
					periodTableCells);
		} else if (this.isSegmentedHistograms && !this.isSmooothedHistograms) {
			this.exportTrends(this.foundExpressionTrends,
					"Expressions-with-trend-in-GSNgramsCorpus, segmented.csv",
					String.valueOf(this.yearStart));
		} else if (this.isSegmentedHistograms && this.isSmooothedHistograms) {
			this.exportTrends(
					this.foundExpressionTrends,
					"Expressions-with-trend-in-GSNgramsCorpus, segmented-n-smoothed.csv",
					String.valueOf(this.yearStart));
		}

		// Export suspect expressions (no trending test was performed on them)
		if (FIND_SUSPECT_EXPRESSIONS && this.suspectExpressionsTrends != null) {
			this.exportTrends(this.suspectExpressionsTrends,
					"Suspect-Expressions-with-trend-in-GSNgrams, NO thresholds.csv", "n/a");
		}
	}

	public void exportNoTrends() {
		if (this.normalizedExpressionsNoTrend != null
				&& this.normalizedExpressionsNoTrend.size() > 0) {
			this.exportTrends(this.normalizedExpressionsNoTrend,
					"Expressions-without-trend-in-GSNgramsCorpus.csv", String.valueOf(this.yearStart));
		}

		if (FIND_SUSPECT_EXPRESSIONS && this.suspectExpressionsNoTrend != null) {
			this.exportTrends(this.suspectExpressionsNoTrend,
					"Suspect-Expressions-without-trend-in-GSNgrams, NO thresholds.csv", String.valueOf(this.yearStart));
		}
	}

	/**
	 * Exports ArrayList to CSV file.
	 * 
	 * @param trends
	 * @param filename
	 * @param periodTableCells
	 */
	private void exportTrends(ArrayList<Trend> trends, String filename,
			String periodTableCells) {
		periodTableCells = periodTableCells.substring(0,
				periodTableCells.length() - 2);
		Util.exportTrends(trends, filename, periodTableCells);
	}

	/**
	 * Saves raw counts histograms of found MWEs in google Syntactic Ngrams
	 * dataset (checked against WordNet MWEs index) NOTE: raw counts only.
	 * relative/normalization will occur later in app, after loading from DB.
	 */
	public void saveState() {
		// save to DB table googlesn_expression_histograms
		// first, delete * from table
		this.database.deleteGoogleSNexpressionHistograms();
		// NOTE: saving raw counts only, and hopefully, statistic is updated
		// (otherwise, it would be saved as 0.0)
		// for normalized trends, use exportTrends() or exportNoTrends()
		// functions.
		this.database
				.insertGoogleSNexpressionHistograms(this.foundExpressionsHM);
		
		this.database
			.insertGoogleSNSuspectExpressions(this.suspectExpressionsHM);
		// Deprecated:
		/*
		 * for (Entry<String, Trend> entry : this.foundExpressionsHM.entrySet())
		 * { Trend histogram = entry.getValue();
		 * this.database.insertGoogleSNexpressionHistogram( histogram.getName(),
		 * histogram.getHistogramSum(), histogram.getSparsity() // note: counts
		 * can be double, if histogram // is normalized/relative. ,
		 * histogram.histogramValuesToString(), histogram.getStatistic()); }
		 */
	}

	public void loadLastState() {
		System.out.print("\nLoading expressions from DB...");
		// HashMap<String, Trend>lastFoundExpressionsHM =
		this.foundExpressionsHM = this.database
				.getGoogleExpressions(true);
		System.out.println("\tLoaded " + this.foundExpressionsHM.size()
				+ " expressions from DB.");
		
		if (FIND_SUSPECT_EXPRESSIONS) {
			System.out.print("\nLoading suspect expressions from DB...");
			// HashMap<String, Trend>lastFoundExpressionsHM =
			this.suspectExpressionsHM = this.database
					.getGoogleSuspectExpressions(true);
			System.out.println("\tLoaded " + this.suspectExpressionsHM.size()
					+ " suspect expressions from DB.");
		}

	}

}
