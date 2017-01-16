package idesign.tal.util.Histogram;

import idesign.tal.mwe.Occurrence;

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class Histogram {

	private final double[] histogramValues;
	private String name = "untitled";
	private int size;
	private double histogramSum = 0;

	public Histogram(double[] histogram) {
		this.histogramValues = histogram.clone();
		this.setFinals();
	}

	public Histogram(String title, double[] histogram) {
		this.setName(title);
		this.histogramValues = histogram.clone();
		this.setFinals();
	}

	public Histogram(String title, Array histogram) {
		Double[] tempHistogram = null;
		try {
			tempHistogram = (Double[]) histogram.getArray();
		} catch (SQLException e) {
			System.out
					.println("Can't convert histogram SQL Array to double[].");
			e.printStackTrace();
		}

		this.histogramValues = new double[tempHistogram.length];
		for (int i = 0, len = tempHistogram.length; i < len; i++) {
			this.histogramValues[i] = tempHistogram[i];
		}

		this.setName(title);
		this.setFinals();

	}

	public Histogram(String title, ArrayList<Occurrence> histogramList,
			int yearStart, int yearEnd) {
		this.setName(title);

		int histogramLength = yearEnd - yearStart + 1;
		double[] histogramZeroFilled = new double[histogramLength];

		// Convert occurrences into a Hashmap, first
		HashMap<Integer, Double> histogramHashMap = new HashMap<Integer, Double>();
		for (int i = 0; i < histogramList.size(); i++) {
			histogramHashMap.put(histogramList.get(i).getYearFrom(),
					histogramList.get(i).getFrequency());
		}

		// create a zero filled array, where no value present in HashMap.
		for (int i = 0; i < histogramLength; i++) {
			// if HM contains this year, return the calculated frequency, else,
			// return NULL:
			int year = yearStart + i;
			if (histogramHashMap.containsKey(year)) {
				// use existing year frequency
				histogramZeroFilled[i] = histogramHashMap.get(year);
			} else {
				// zero-fill this year
				histogramZeroFilled[i] = 0;
			}
		}

		// move histogram to instance:
		this.histogramValues = histogramZeroFilled.clone();
		this.setFinals();

	}

	private void setFinals() {
		this.size = this.histogramValues.length;
		double sum = 0.0;
		for (int i = 0; i < this.size; i++) {
			sum += this.getHistogramValue(i);
		}
		this.setHistogramSum(sum);
	}

	public int size() {
		return size;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the histogramSum
	 */
	public double getHistogramSum() {
		return histogramSum;
	}

	public Histogram add(double[] histogram) {
		if (histogram.length != this.size)
			throw new Error("Histogram lengths don't match.");

		for (int i = 0, len = Math.min(histogram.length, this.size); i < len; i++) {
			this.histogramValues[i] += histogram[i];
		}

		this.setFinals(); // size, new counts total.
		return this;
	}

	public Histogram normalize(Histogram totalCountsHistogram) {
		double[] normalizesValuesArray = new double[this.size()];
		for (int i = 0; i < this.size(); i++) {
			normalizesValuesArray[i] = (double) this.getHistogramValue(i)
					/ totalCountsHistogram.getHistogramValue(i);
		}
		Histogram normalizedHistogram = new Histogram(this.getName(),
				normalizesValuesArray);
		return normalizedHistogram;
	}

	/**
	 * @param histogramSum
	 *            the histogramSum to set
	 */
	public void setHistogramSum(double histogramSum) {
		this.histogramSum = histogramSum;
	}

	/**
	 * Get sparsity for histogram (=% of empty bins in histogram).
	 * @return
	 */
	public double getSparsity() {
		int noValueBins = 0;
		for (int i = 0, len = histogramValues.length; i < len; i++) {
			if (0.0 == this.histogramValues[i]) {
				noValueBins++;
			}
		}

		return (double) noValueBins / histogramValues.length;
	}

	/**
	 * Get sparsity for partial segment of the Histogram.
	 * 
	 * @param indexFrom
	 * @param indexTo
	 * @return
	 */
	public double getSparsity(int indexFrom, int indexTo) {
		int noValueBins = 0;
		assert (indexFrom < indexTo);
		for (int i = indexFrom; i <= indexTo; i++) {
			if (0.0 == this.histogramValues[i]) {
				noValueBins++;
			}
		}

		return (double) noValueBins / (indexTo - indexFrom + 1);
	}

	public static Histogram segmentHistogram(Histogram histogram,
			int segmentSize) {
		// NOTE: number of buckets could not be equal
		int numberOfBuckets = Histogram.getSegmentedBucketsNumber(histogram,
				segmentSize); // getBucket(this.yearEnd, segmentSize);
		int startIndex = Histogram.getSegmentedHistogramLeftOver(histogram,
				segmentSize);
		// TODO abandon a few data points from the start of histogram?
		double[] segmentedHistogram = new double[numberOfBuckets];
		double[] histogramValues = histogram.getHistogramValues();

		int currentBucket = 0;
		double bucketFrequencySum = 0;
		for (int i = startIndex, len = histogramValues.length; i < len; i++) {
			// find bucket for this frequency
			int appropriateBucket = (i - startIndex) / segmentSize; // e.g. 5/7
																	// = 0; 12/7
																	// = 1; ...
																	// (8-1)/7
																	// is still
																	// 1

			if (appropriateBucket <= currentBucket) { // len-1 is for filling
														// last bucket before
														// exiting for.
				// update sum
				bucketFrequencySum += histogramValues[i];
				// force value set on last item, before exiting for loop.
				if (i == len - 1) {
					segmentedHistogram[currentBucket] = bucketFrequencySum;
				}
			} else {
				// set sum upto now into bucket,
				segmentedHistogram[currentBucket] = bucketFrequencySum;
				// and start a new sum & bucket
				bucketFrequencySum = histogramValues[i];
				currentBucket = appropriateBucket;
			}

		}

		// TODO calculate raw histogram, finally, (3rd arg -->)
		return new Histogram(histogram.getName(), segmentedHistogram);

	}

	/*
	 * public static ExpressionHistogram segmentHistogram(ExpressionHistogram
	 * expressionHistogram, int segmentSize) { // NOTE: number of buckets could
	 * not be equal int numberOfBuckets = (int) Math.floor(
	 * expressionHistogram.size() / segmentSize); //getBucket(this.yearEnd,
	 * segmentSize); int startIndex = expressionHistogram.size() % segmentSize;
	 * // TODO abandon a few data points from the start of histogram? Double[]
	 * segmentedHistogram = new Double[numberOfBuckets]; Double[]
	 * histogramValues = expressionHistogram.getHistogramRaw();
	 * 
	 * 
	 * int currentBucket = 0; double bucketFrequencySum = 0; for(int
	 * i=startIndex, len = histogramValues.length; i<len; i++) { // find bucket
	 * for this frequency int appropriateBucket = (i-startIndex) / segmentSize;
	 * // e.g. 5/7 = 0; 12/7 = 1; ... (8-1)/7 is still 1
	 * 
	 * if (appropriateBucket <= currentBucket) { // len-1 is for filling last
	 * bucket before exiting for. //update sum bucketFrequencySum +=
	 * histogramValues[i]; // force value set on last item, before exiting for
	 * loop. if (i==len-1) { segmentedHistogram[currentBucket] =
	 * bucketFrequencySum; } } else { // set sum upto now into bucket,
	 * segmentedHistogram[currentBucket] = bucketFrequencySum; // and start a
	 * new sum & bucket bucketFrequencySum = histogramValues[i]; currentBucket =
	 * appropriateBucket; }
	 * 
	 * 
	 * }
	 * 
	 * // TODO calculate raw histogram, finally, (3rd arg -->) return new
	 * ExpressionHistogram(expressionHistogram.getExpression(),
	 * segmentedHistogram, segmentedHistogram);
	 * 
	 * }
	 */
	/*
	 * private ArrayList<Occurrence> segmentHistogram(ArrayList<Occurrence>
	 * histogram, int segmentSize) { // NOTE: number of buckets could not be
	 * equal int numberOfBuckets = (int) Math.floor(
	 * (this.yearEnd-this.yearStart) / segmentSize); //getBucket(this.yearEnd,
	 * segmentSize); double leftOver = (this.yearEnd-this.yearStart) %
	 * segmentSize; // TODO abandon a few data points from the start of
	 * histogram? int startingYear = this.yearStart; ArrayList<Occurrence>
	 * segmentedHistogram = new ArrayList<Occurrence>(numberOfBuckets);
	 * 
	 * int currentBucket = 0; double bucketFrequencySum = 0; for(int i=0, len =
	 * histogram.size(); i<len; i++) { // find bucket for this frequency int
	 * appropriateBucket = this.getBucket(histogram.get(i).getYearFrom(),
	 * segmentSize);
	 * 
	 * if (appropriateBucket <= currentBucket) { //update sum bucketFrequencySum
	 * += histogram.get(i).getFrequency(); // promote previousBucket } else { //
	 * set sum upto now into bucket, segmentedHistogram.add(currentBucket, new
	 * Occurrence(bucketFrequencySum, startingYear +
	 * (currentBucket*segmentSize), startingYear +
	 * ((currentBucket+1)*segmentSize) )); // and start a new sum & bucket
	 * bucketFrequencySum = histogram.get(i).getFrequency(); currentBucket =
	 * appropriateBucket; }
	 * 
	 * 
	 * } return histogram;
	 * 
	 * 
	 * }
	 */

	public static int getSegmentedBucketsNumber(Histogram histogram,
			int segmentSize) {
		// TODO Auto-generated method stub
		return (int) Math.floor(histogram.size() / segmentSize);
	}

	public static int getSegmentedHistogramLeftOver(Histogram histogram,
			int segmentSize) {
		return histogram.size() % segmentSize;
	}

	public static Histogram smoothMovingAverage(Histogram histogram,
			int sampleSize) {
		// create a moving average of values
		double[] smoothedValues = new double[histogram.size()];

		for (int i = 0, len = histogram.size(); i < len; i++) {

			int lowerBoundry = Math.max(i - sampleSize / 2, 0);
			int upperBoundry = Math.min(histogram.size() - 1, i + sampleSize
					/ 2);
			int n = 0;
			double sum = 0;

			for (int mi = lowerBoundry; mi <= upperBoundry; mi++, n++) {
				sum += histogram.histogramValues[mi];
			}

			smoothedValues[i] = sum / n;

		}
		return new Histogram(histogram.getName(), smoothedValues);

	}

	public double[] getHistogramValues() {
		return histogramValues.clone();
	}

	public double getHistogramValue(int index) {
		return histogramValues[index];
	}

	public static Histogram substractHistogram(Histogram a, Histogram b) {
		double[] result = new double[a.size()];
		for (int i = 0, len = result.length; i < len; i++) {
			// sum into histogramSum
			result[i] = a.getHistogramValue(i) - b.getHistogramValue(i);
		}
		return new Histogram(a.getName(), result);
	}

	@Override
	public String toString() {
		return this.getName() + " [" + this.toCSV(false) + "]";
	}

	public String histogramValuesToString() {
		StringBuilder str = new StringBuilder();
		// write values
		for (int i = 0, len = this.size(); i < len; i++) {
			if (i == len - 1) {
				str.append(this.getHistogramValue(i));
			} else {
				str.append(this.getHistogramValue(i)).append(", ");
			}
		}
		return str.toString();
	}

	public String toCSV(boolean withName) {
		StringBuilder str = new StringBuilder();

		// write name
		if (withName) {
			str.append(this.getName()).append(", ");
		}

		// write values
		str.append(this.histogramValuesToString());

		return str.toString();
	}

}
