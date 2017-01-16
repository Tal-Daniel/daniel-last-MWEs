package idesign.tal.mwe;

public class Occurrence {
	private double frequency; // freq. of occurance within corpus in that year.
	private int yearFrom;
	private int yearTo;
	
	public Occurrence (double frequency, int yearFrom, int yearTo) {
		this.setFrequency(frequency);
		this.setYearFrom(yearFrom);
		this.setYearTo(yearTo);
	}
	
	public double getFrequency() {
		return frequency;
	}
	private void setFrequency(double frequency) {
		this.frequency = frequency;
	}
	public int getYearFrom() {
		return yearFrom;
	}
	private void setYearFrom(int yearFrom) {
		this.yearFrom = yearFrom;
	}
	public int getYearTo() {
		return yearTo;
	}
	private void setYearTo(int yearTo) {
		this.yearTo = yearTo;
	}
	
	@Override
	public String toString(){
		StringBuilder buffer = new StringBuilder();
		buffer.append("{")
			.append(this.getYearFrom()).append(": ")
			.append(this.getFrequency())
			.append("}");
		return buffer.toString();
	}
}
