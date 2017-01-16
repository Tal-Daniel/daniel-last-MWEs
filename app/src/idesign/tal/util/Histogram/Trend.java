/**
 * 
 */
package idesign.tal.util.Histogram;

import java.sql.Array;
import java.util.Arrays;

/**
 * @author tal
 *
 */
public class Trend extends Histogram {

	/**
	 * @param histogram
	 */
	private double statistic = 0.0; // trend statistic.
	private double[] statistics;
	

	public Trend(double[] histogram) {
		super(histogram);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param title
	 * @param histogram
	 */
	public Trend(String title, double[] histogram) {
		super(title, histogram);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a trend, which is a Histogram that has a (statistically significant) trend.
	 * Hence, Positive/Negative statistic should be significant, otherwise, don't create a trend.
	 * @param title
	 * @param histogram
	 * @param statistic
	 */
	public Trend(String title, double[] histogram, double statistic) {
		super(title, histogram);
		// TODO Auto-generated constructor stub
		this.statistic = statistic;
	}
	
	/**
	 * @return the statistic
	 */
	public double getStatistic() {
		return statistic;
	}

	/**
	 * @param statistic the statistic to set
	 */
	public void setStatistic(double statistic) {
		this.statistic = statistic;
	}

	public double[] getStatistics() {
		return statistics.clone();
	}
	
	public void setStatistics(double[] statistics) {
		this.statistics = statistics.clone();
	}

	public Trend(Histogram histogram, double statistic) {
		super(histogram.getName(), histogram.getHistogramValues());
		this.statistic = statistic;
	}
	
	public Trend(Histogram histogram, double[] statistics) {
		super(histogram.getName(), histogram.getHistogramValues());
		assert(statistics != null);
		this.statistics = statistics.clone();
		// maintain backward compatibility:
		this.statistic = this.statistics[0];
	}
	
	public Trend(String title, Array histogram, double statistic) {
		super(title,histogram);
		this.setStatistic(statistic);
	}

	public Trend(Histogram histogram) {
		super(histogram.getName(), histogram.getHistogramValues());	}

	@Override
	public String toCSV(boolean withName) {
		String statsString = "";  
		
		if (this.statistics != null) { 
			statsString = Arrays.toString(this.getStatistics());
			statsString = statsString.substring(1, statsString.length()-1);
		} else {
			statsString = Double.toString(this.getStatistic());
		}
			
		return statsString + ", " 
				+ super.getSparsity() + ", "
				+ super.getHistogramSum() + ", "
				+ super.toCSV(withName);
	}	

}
