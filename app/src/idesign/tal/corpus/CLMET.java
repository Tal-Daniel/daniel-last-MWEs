package idesign.tal.corpus;

public class CLMET {
	private int currentPeriod = 0, currentFile = 0;

	private final String fileNamePrefix = "CLMET3_0_";
	private final String fileNameSuffix = ".txt.txt.txt";

	static String[] PERIODS = { "1710-1780", "1780-1850", "1850-1920" };
	static int[] PERIOD_START_INDEX = { 1, 89, 188 };
	static int[] PERIOD_END_INDEX = { 88, 187, 333 };

	public CLMET(int i) {
		currentFile=i;
	}
	
	public String getNextDocPath() {
		currentFile++;
		if (currentFile > PERIOD_END_INDEX[0]) {
			// start period 2 files
			currentPeriod = 1;
			if (currentFile > PERIOD_END_INDEX[1]) {
				// start period 3 files
				currentPeriod = 2;
			}
		}

		if (currentFile <= PERIOD_END_INDEX[2]) {
			return PERIODS[currentPeriod] + "/" + fileNamePrefix
					+ (currentPeriod + 1) + "_" + currentFile + fileNameSuffix;
		} else
			return null;
	}
}