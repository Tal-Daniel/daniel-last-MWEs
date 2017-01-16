package idesign.tal.mwe;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class ExpressionHistogram {
	private String expression; // freq. of occurance within corpus in that year.
	private double[] histogramRaw;
	private double[] histogramRelative;
	private int size;
	
	public ExpressionHistogram (String expression, Array histogramRaw, Array histogramRelative) {
		this.setExpression(expression);
		
		try {
//			Double [] hr = (Double[]) histogramRaw.getArray();
			Double[] tempHistogramRaw = (Double[]) histogramRaw.getArray();
			this.histogramRaw = new double[tempHistogramRaw.length];
			for (int i=0, len= tempHistogramRaw.length; i<len; i++) {
				this.histogramRaw[i] = tempHistogramRaw[i];
			}
			
			Double[] tempHistogramRelative = (Double[]) histogramRelative.getArray();
			this.histogramRelative = new double[tempHistogramRelative.length];
			for (int i=0, len= tempHistogramRelative.length; i<len; i++) {
				this.histogramRelative[i] = tempHistogramRelative[i];
			}
			
//			this.histogramRaw = (double[]) histogramRaw.getArray();
//			this.histogramRelative = (double[]) histogramRelative.getArray();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("Can't convert histogram SQL Array to double[].");
			e.printStackTrace();
		} 
		
		this.size = this.histogramRaw.length;
	}
	
	public ExpressionHistogram (String expression, double[] histogramRaw, double[] histogramRelative) {
		this.setExpression(expression);
		this.histogramRaw = histogramRaw.clone();
		this.histogramRelative = histogramRelative.clone();
		this.size = this.histogramRaw.length;
	}
	
	public int size() {
		return size;
	}

	public String getExpression() {
		return expression;
	}
	
	public double[] getHistogramRaw() {
		return histogramRaw.clone();
	}

	public double[] getHistogramRelative() {
		return histogramRelative.clone();
	}

	private void setExpression(String expression) {
		this.expression = expression;
	}
	
	@Override
	public String toString(){
		StringBuilder buffer = new StringBuilder();
		buffer.append(expression)
			.append("\n\t Hist. (Raw): {")
			.append(this.histogramRaw.toString())
			.append("}")
			.append("\n\t Hist. (Relative): {")
			.append(this.histogramRelative.toString())
			.append("}");
		return buffer.toString();
	}
}
