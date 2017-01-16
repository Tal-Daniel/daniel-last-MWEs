package idesign.tal.util.Histogram;

import idesign.tal.App;
import idesign.tal.db.Statistics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class Util {

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
	
	
	/**
	 * Sums all expression histgrams into an overall [average] Histogram.
	 * 
	 * @param histograms
	 * @return Histogram
	 */
	public static Histogram sumHistograms(HashMap<String, Histogram> histograms) {

		// initialize empty histogram
//		double[] histogramSum = new double[histograms.size()];
		Histogram histogram = null;

		// sum every histogram
		for (Entry<String,Histogram> entry : histograms.entrySet()) {
			if (histogram == null) histogram = new Histogram(entry.getValue().getHistogramValues());
			else histogram.add(entry.getValue().getHistogramValues());
		}
		return histogram;
	}
	
	
	public static Trend sumTrends(
			HashMap<String, Trend> trends) {
		// TODO Auto-generated method stub
		Trend trend = null;

		// sum every histogram
		for (Entry<String,Trend> entry : trends.entrySet()) {
			if (trend == null) trend = new Trend(entry.getValue().getHistogramValues());
			else trend.add(entry.getValue().getHistogramValues());
		}
		return trend;
	}
	
	/**
	 * Exports found trends to CSV file (output folder).
	 * @param trends
	 * @param filename
	 * @param periodTableCells
	 */
	public static void exportTrends(ArrayList<Trend> trends, String filename, String periodTableCells) {
		//  export for CSV file
		File outputFolder = new File(App.OUTPUT_PATH);
		String exportFilePath = App.OUTPUT_PATH + "/" + filename;
		File foundTrendsFile = new File(exportFilePath);
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
					.write("z score, rho, sparsity, countsSum (normalized), expression id, " + periodTableCells + "\n");

			for (Trend trend : trends) {
					foundTrendsFileWriter.write(trend.toCSV(true) + "\n");
			}

			foundTrendsFileWriter.close();
			
			System.out.println("\n\nExported " + trends.size() + " Trends to " + exportFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
