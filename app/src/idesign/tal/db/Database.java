package idesign.tal.db;

import idesign.tal.mwe.ExpressionHistogram;
import idesign.tal.mwe.Occurrence;
import idesign.tal.util.Histogram.Histogram;
import idesign.tal.util.Histogram.Trend;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import edu.mit.jmwe.data.IToken;

/**
 * @author taldan
 * 
 */

public class Database {
	// define the driver to use
	// String driver = "org.apache.derby.jdbc.EmbeddedDriver";
	final static boolean DEBUG = false;
	private static int EXPRESSIONS_NUMBER = 10;

	String driver = "org.postgresql.Driver";
	// define the Derby connection URL to use
	String connectionURL = "jdbc:derby:DB;create=true"; // (Database name = DB)

	String pgConnectionURL = "jdbc:postgresql://localhost:5432/mwe";
	String pgUser = "postgres";
	String pgPassword = "tttt2pg";

	private Connection conn = null;

	public Database(String connectionURL, String user, String password) {
		this.pgConnectionURL = connectionURL;
		this.pgUser = user;
		this.pgPassword = password;

		// init DB by creating tables, if not exist.
		// Connection conn = null;
		Statement s;

		String checkDocumentsTable = "update DOCUMENTS set TITLE='TEST' where 1=2";
		// TODO !! create DB only if not exists !!
		// create Table syntax:
		// [ id, year, title, Author]
		String createDocumentsTable = "CREATE TABLE IF NOT EXISTS Documents ("
				+ "id SERIAL PRIMARY KEY, " + "title varchar(500) NOT NULL, "
				+ "author varchar(500) NOT NULL, " + "year int, "
				+ "words int);";

		// "CREATE TABLE DOCUMENTS "
		// + "(ID INT NOT NULL GENERATED ALWAYS AS IDENTITY "
		// + " CONSTRAINT DOCUMENTS_PK PRIMARY KEY, "
		// + " YEAR_PUBLISHED INT NOT NULL, "
		// + " TITLE VARCHAR(1000) NOT NULL, "
		// + " AUTHOR VARCHAR(1000) NOT NULL)";

		String checkSentencesTable = "update SENTENCES set SENTENCE='TEST' where 1=2";
		// TODO !! create DB only if not exists !!
		// create Table syntax:
		// [ id, year, title, Author]
		String createSentencesTable = "CREATE TABLE SENTENCES "
				+ "(ID INT NOT NULL GENERATED ALWAYS AS IDENTITY "
				+ " CONSTRAINT SENTENCES_PK PRIMARY KEY, "
				+ " DOCUMENT INT NOT NULL "
				+ " CONSTRAINT DOCUMENTS_FK ...?...,"
				+ " SENTENCE VARCHAR(5000) NOT NULL)";

		String checkMWETable = "update EXPRESSIONS set EXPRESSION_ID='TEST' where 1=2";
		// TODO !! create DB only if not exists !!
		// create Table syntax:
		// [ id, year, title, Author]
		String createMWETable = "CREATE TABLE EXPRESSIONS "
				+ "(ID INT NOT NULL GENERATED ALWAYS AS IDENTITY "
				+ " CONSTRAINT EXPRESSIONS_PK PRIMARY KEY, "
				+ " TOKENS VARCHAR (1000) NOT NULL "
				+ " TAGS VARCHAR(1000) NOT NULL)";

		String checkExpressionsOccurrencesSummaryTable = "SELECT expression_id FROM expression_occurances_summary "
				+ "WHERE expression_id = 'non-existing-expression'";
		String createExpressionsOccurrencesSummaryTable = "CREATE MATERIALIZED VIEW EXPRESSION_OCCURANCES_summary AS "
				+ "(SELECT expression_id, s.sentence_id, doc.year, doc.document_id "
				+ "FROM EXPRESSION_OCCURANCES AS eo "
				+ "LEFT JOIN sentences AS s ON eo.sentence_id = s.sentence_id "
				+ "LEFT JOIN documents AS doc ON s.document_id = doc.document_id "
				+ "ORDER BY expression_id, doc.year) " + "WITH DATA;";

		// TODO ? Create table expression_occurances
		// TODO create table EXPRESSION_OCCURANCES_summary
		/*
		 * CREATE MATERIALIZED VIEW EXPRESSION_OCCURANCES_summary AS (SELECT
		 * expression_id, s.sentence_id, doc.year, doc.document_id FROM
		 * EXPRESSION_OCCURANCES AS eo LEFT JOIN sentences AS s ON
		 * eo.sentence_id = s.sentence_id LEFT JOIN documents AS doc ON
		 * s.document_id = doc.document_id ORDER BY expression_id, doc.year)
		 * WITH DATA;
		 */
		// TODO CREATE MATERIALIZED VIEW DOCUMENTS_SUMMARY AS
		/*
		 * CREATE MATERIALIZED VIEW DOCUMENTS_SUMMARY AS (SELECT year,
		 * sum(words) AS words, count(document_id) AS documents FROM documents
		 * GROUP BY year ORDER BY year) WITH DATA;
		 */

		// TODO create it... Statistics uses it to store preliminary frequencies
		String createStatisticsTables = "CREATE TABLE STATISTICS_EXPRESSION_HISTOGRAM "
				+ "(	expression_id varchar(1000) PRIMARY KEY REFERENCES Expressions,	"
				+ "histogram float ARRAY);";
		// (expression.id, sentence.id)

		String createGoogleSNTables = "CREATE TABLE GoogleSN_EXPRESSION_HISTOGRAMS ("
				+ "expression_id varchar(1000) PRIMARY KEY, counts int, sparsity float, "
				+ "histogram_raw float ARRAY,	histogram_relative float ARRAY,	kendallTauZ float);";
		// ## LOAD DRIVER SECTION ##
		try {
			/*
			 * * Load the Derby driver.* When the embedded Driver is used this
			 * action start the Derby engine.* Catch an error and suggest a
			 * CLASSPATH problem
			 */
			Class.forName(driver);
			System.out.println(driver + " loaded. ");

		} catch (java.lang.ClassNotFoundException e) {
			System.err.print("ClassNotFoundException: ");
			System.err.println(e.getMessage());
			System.out
					.println("\n >>> Please check your CLASSPATH variable   <<<\n");
		}

		// Beginning of Primary DB access section
		// ## BOOT DATABASE SECTION ##
		try {
			// Create (if needed) and connect to the database
			// conn = DriverManager.getConnection(connectionURL);
			Class.forName(driver);
			conn = DriverManager.getConnection(pgConnectionURL, pgUser,
					pgPassword);
			System.out.println("OPENED Connection");

			// ## INITIAL SQL SECTION ##
			// Create a statement to issue simple commands.
			s = conn.createStatement();
			// Call utility method to check if table exists.
			// Create the table if needed
			if (!Util.Chk4Table(conn, checkDocumentsTable)) {
				System.out.println("Creating Documents table.");
				s.execute(createDocumentsTable);
			}

			s.close();

			// TODO check & create other tables.

			conn.close();
			System.out.println("CLOSED connection");
			// Beginning of the primary catch block: uses errorPrint method
		} catch (Throwable e) {
			// Catch all exceptions and pass them to* the exception reporting
			// method
			System.out.println(" . . . exception thrown:");
			errorPrint(e);
		}
	}

	public int insertDocument(String title, String author, int year) {
		PreparedStatement ps;
		ResultSet result = null;
		int documentId = 0;

		try {
			conn = establishConnection();
			// Create SQL statement
			// s = conn.createStatement();

			ps = conn
					.prepareStatement("INSERT INTO documents(title, author, year) VALUES (?, ?, ?) RETURNING document_id");
			ps.setString(1, title);
			ps.setString(2, author);
			ps.setInt(3, year);

			result = ps.executeQuery(); // ps.executeUpdate();
			if (result.next()) {
				documentId = result.getInt("document_id");
				System.out.println("DB: Inserted doc (document_id:"
						+ documentId + ")");
			}

			// Release the resources (clean up )

			result.close();
			ps.close();
			// s.close();
			// conn.getAutoCommit();
			// conn.commmit();
			conn.close();

		} catch (Throwable e) {
			System.out.println("... Exception thrown:");
			errorPrint(e);
		}

		return documentId;
	}

	/**
	 * update the words number in document. Note: this function was added later,
	 * after all documents were inserted into DB.
	 * 
	 * @param title
	 *            - title of doc (title, author) are a Primary Key
	 * @param author
	 *            - author of doc (title, author) are a Primary Key.
	 * @param words
	 *            - words number in doc.
	 * @return affectedRows - after DB UPDATE
	 */
	public int updateDocumentWords(String title, String author, int words) {
		PreparedStatement ps;
		int affectedRows = 0;

		try {
			conn = establishConnection();
			// Create SQL statement
			// s = conn.createStatement();

			ps = conn.prepareStatement("UPDATE documents SET words=(?) "
					+ "WHERE title=(?) AND author=(?)");
			ps.setInt(1, words);
			ps.setString(2, title);
			ps.setString(3, author);

			affectedRows = ps.executeUpdate(); // ps.executeUpdate();

			// Release the resources (clean up )

			ps.close();
			// s.close();
			// conn.getAutoCommit();
			// conn.commmit();
			conn.close();

		} catch (Throwable e) {
			System.out.println("... Exception thrown:");
			errorPrint(e);
		}

		return affectedRows;
	}

	public int insertSentence(int dbDocumentId, String sentence) {
		PreparedStatement ps;
		ResultSet result = null;
		int sentenceId = 0;

		try {
			conn = establishConnection();
			// Create SQL statement
			// s = conn.createStatement();

			ps = conn
					.prepareStatement("INSERT INTO sentences (sentence, document_id) VALUES (?, ?) RETURNING sentence_id");
			ps.setString(1, sentence);
			ps.setInt(2, dbDocumentId);

			result = ps.executeQuery(); // ps.executeUpdate();
			if (result.next()) {
				sentenceId = result.getInt("sentence_id");
				System.out.println("DB: Inserted sentence (id:" + sentenceId
						+ ")");
			}

			// Release the resources (clean up )

			result.close();
			ps.close();
			// s.close();
			// conn.getAutoCommit();
			// conn.commmit();
			conn.close();

		} catch (Throwable e) {
			System.out.println("... Exception thrown:");
			errorPrint(e);
		}

		return sentenceId;
	}

	/**
	 * Inserts an expression to DB, only if it's new (unique).
	 * 
	 * @param expressionId
	 * @param tokens
	 * @param tags
	 * @return
	 */
	public boolean insertExpression(String expressionId, String tokens,
			String tags) {
		PreparedStatement ps;
		// ResultSet result = null;
		int result = 0;

		try {
			conn = establishConnection();
			// Create SQL statement
			// s = conn.createStatement();

			// insert expression, only if it's unique.
			ps = conn
					.prepareStatement("INSERT INTO expressions (expression_id, tokens, tags) "
							+ "SELECT (?), (?), (?)	WHERE NOT EXISTS "
							+ "( SELECT 1 FROM expressions WHERE expression_id = (?) )");
			ps.setString(1, expressionId);
			ps.setString(2, tokens);
			ps.setString(3, tags);
			ps.setString(4, expressionId);

			result = ps.executeUpdate();
			/*
			 * if (result.next()) { sentenceId = result.getInt("sentence_id");
			 * System.out.println("DB: Inserted sentence (id:" + sentenceId +
			 * ")"); }
			 */
			if (result > 0) {
				System.out.println("DB: Inserted [new] expression \""
						+ expressionId + "\" (" + result + ")");
			}

			// Release the resources (clean up )
			ps.close();
			conn.close();

		} catch (Throwable e) {
			System.out.println("... Exception thrown:");
			System.exit(1);
			errorPrint(e);
		}

		if (result > 0)
			return true;
		else
			return false;
	}

	/**
	 * Inserts expression occurrence to DB, so we can later count how many times
	 * the expression occurred at a certain document.
	 * 
	 * @param expressionId
	 * @param sentenceId
	 * @return
	 */
	public boolean inserExpressionOccurrance(String expressionId, int sentenceId) {
		PreparedStatement ps;
		int result = 0;

		try {
			conn = establishConnection();
			ps = conn
					.prepareStatement("INSERT INTO expression_occurances(expression_id, sentence_id) VALUES (?, ?)");
			ps.setString(1, expressionId);
			ps.setInt(2, sentenceId);

			result = ps.executeUpdate();

			if (result > 0) {
				System.out.println("DB: Inserted expression occurrance ("
						+ result + ")");
			}

			// Release resources (clean up)
			ps.close();
			conn.close();

		} catch (Throwable e) {
			System.out.println("... Exception thrown:");
			errorPrint(e);
		}

		if (result > 0)
			return true;
		else
			return false;

	}

	/**
	 * Saves expression frequencies over the years into the DB. Used by
	 * Statistics.
	 * 
	 * @param expressionId
	 * @param histogramRelativeArray
	 * @return result - integer
	 */
	public boolean insertExpressionHistogram(String expressionId,
			String histogramRelativeArray, String histogramRawArray) {
		PreparedStatement ps;
		int result = 0;

		try {
			conn = establishConnection();
			ps = conn
					.prepareStatement("INSERT INTO statistics_expression_histogram(expression_id, histogram_raw, histogram_relative) "
							+ " VALUES (?,'"
							+ histogramRawArray
							+ "','"
							+ histogramRelativeArray + "');");
			ps.setString(1, expressionId);
			// ps =
			// conn.prepareStatement("INSERT INTO statistics_expression_histogram(expression_id, histogram) VALUES (?, ?);");
			// ps.setString(1, expressionId);
			// ps.setArray(2, histogramArray);

			result = ps.executeUpdate();

			if (result > 0) {
				System.out.println("DB: Inserted expression histogram ("
						+ result + ")");
			}

			// Release resources (clean up)
			ps.close();
			conn.close();

		} catch (Throwable e) {
			System.out.println("... Exception thrown:");
			errorPrint(e);
		}

		if (result > 0)
			return true;
		else
			return false;

	}

	private boolean insertGoogleExpressionHistograms(HashMap<String, Trend> histograms, String tableName) {
		PreparedStatement ps;
		int result = 0;

		try {
			conn = establishConnection();
			conn.setAutoCommit(false); // makes a transaction, to ease DB
										// procedures.

			for (Entry<String, Trend> entry : histograms.entrySet()) {
				Trend histogram = entry.getValue();

				ps = conn
						.prepareStatement("INSERT INTO "+tableName+" (expression_id, counts, sparsity, histogram_raw, kendalltauz) "
								+ " VALUES (?,?,?,'{"
								+ histogram.histogramValuesToString()
								+ "}',?);");
				ps.setString(1, histogram.getName());
				ps.setDouble(2, histogram.getHistogramSum());
				ps.setDouble(3, histogram.getSparsity());
				ps.setDouble(4, histogram.getStatistic());

				result = ps.executeUpdate();

				if (result > 0 && DEBUG) {
					System.out.print(histogram.getName() + ", "); // B: Inserted Google
															// Syntactic Ngram
															// expression
															// histogram
															// ("+result+")");
				}
				// Release resources (clean up)
				ps.close();

			}
			
			// all statements sent for commit, as a single transaction.
			
			conn.commit(); // COMMIT transaction. see above comment.
			conn.close();

		} catch (Throwable e) {
			System.out.println("... Exception thrown:");
			errorPrint(e);
		}

		if (result > 0)
			return true;
		else
			return false;
	}
	
	public boolean insertGoogleSNexpressionHistograms(
			HashMap<String, Trend> histograms) {
		return this.insertGoogleExpressionHistograms(histograms, "googlesn_expression_histograms");
	}

	public boolean insertGoogleSNSuspectExpressions(
			HashMap<String, Trend> histograms) {
		return this.insertGoogleExpressionHistograms(histograms, "googlesn_suspect_expression_histograms");
		
	}

	/**
	 * Saves expression frequencies over the years into the DB. Used by
	 * Statistics. Note: inserts only raw counts. normalize at application
	 * level.
	 * 
	 * @param expressionId
	 * @param histogramRelativeArray
	 * @return result - integer
	 */
	@Deprecated 
	// used too many DB resources, caused DB to crash sometimes, when exceeding 30,000 row inserts. 
	public boolean insertGoogleSNexpressionHistogram(String expressionId,
			double counts, double sparsity, String histogramRawArray,
			double kendallTauZ) {
		PreparedStatement ps;
		int result = 0;

		try {
			conn = establishConnection();
			ps = conn
					.prepareStatement("INSERT INTO googlesn_expression_histograms(expression_id, counts, sparsity, histogram_raw, kendalltauz) "
							+ " VALUES (?,?,?,'{"
							+ histogramRawArray
							+ "}',?);");
			ps.setString(1, expressionId);
			ps.setDouble(2, counts);
			ps.setDouble(3, sparsity);
			ps.setDouble(4, kendallTauZ);

			result = ps.executeUpdate();

			if (result > 0 && DEBUG) {
				System.out.print(expressionId + ", "); // B: Inserted Google
														// Syntactic Ngram
														// expression histogram
														// ("+result+")");
			}
			// Release resources (clean up)
			ps.close();
			conn.close();

		} catch (Throwable e) {
			System.out.println("... Exception thrown:");
			errorPrint(e);
		}

		if (result > 0)
			return true;
		else
			return false;

	}

	public int getTotalMWEsFound() {

		if (DEBUG)
			return EXPRESSIONS_NUMBER;

		PreparedStatement ps;
		ResultSet result = null;
		int expressionsFound = 0;
		try {
			conn = establishConnection();
			// Create SQL statement
			// s = conn.createStatement();

			ps = conn
					.prepareStatement("SELECT COUNT (expression_id) as Expressions_found FROM expressions");

			result = ps.executeQuery(); // ps.executeUpdate();
			if (result.next()) {
				expressionsFound = result.getInt("Expressions_found");
			}

			// Release the resources (clean up )

			result.close();
			ps.close();
			// s.close();
			// conn.getAutoCommit();
			// conn.commmit();
			conn.close();

		} catch (Throwable e) {
			System.out.println("... Exception thrown:");
			errorPrint(e);
		}

		return expressionsFound;
	}

	public ArrayList<String> getExpressionsFound() {
		PreparedStatement ps;
		ResultSet result = null;
		ArrayList<String> expressions = new ArrayList<String>();

		try {
			conn = establishConnection();
			// Create SQL statement
			// s = conn.createStatement();

			if (!DEBUG)
				ps = conn
						.prepareStatement("SELECT expression_id, tokens, tags from expressions");// that's...
																									// 12,000
																									// expressions!
			else
				ps = conn
						.prepareStatement("SELECT expression_id, tokens, tags from expressions LIMIT "
								+ EXPRESSIONS_NUMBER);// that's... 12,000
														// expressions!
			// ps =
			// conn.prepareStatement("SELECT expression_id, tokens, tags from expressions	WHERE expression_id LIKE '%''%';");
			// ... to limit, for debugging, add "LIMIT 10 OFFSET 90");

			result = ps.executeQuery(); // ps.executeUpdate();
			while (result.next()) {
				expressions.add(result.getString("expression_id"));
			}

			// Release the resources (clean up )

			result.close();
			ps.close();
			// s.close();
			// conn.getAutoCommit();
			// conn.commmit();
			conn.close();

		} catch (Throwable e) {
			System.out.println("... Exception thrown:");
			errorPrint(e);
		}

		return expressions;
	}

	/**
	 * Finds expression occurrences (frequency) in each year available within
	 * the corpus.
	 * 
	 * @param expression
	 * @return List of Occurrences (year, occurrences)
	 */
	public ArrayList<Occurrence> getExpressionOccurrences(String expression) {
		PreparedStatement ps;
		ResultSet result = null;
		ArrayList<Occurrence> expressionOccurrences = new ArrayList<Occurrence>();

		try {
			conn = establishConnection();
			// Create SQL statement
			// s = conn.createStatement();

			// TODO: SELECT all occurrences of a single expression
			ps = conn
					.prepareStatement(" SELECT expression_id, count(sentence_id), year "
							+ " FROM expression_occurances_summary "
							+ " WHERE expression_id = ? "
							+ " GROUP BY expression_id, year ORDER BY year;");
			ps.setString(1, expression); // e.g. "speak_of_V"

			result = ps.executeQuery(); // ps.executeUpdate();
			while (result.next()) {
				expressionOccurrences.add(new Occurrence(
						result.getInt("count"), result.getInt("year"), result
								.getInt("year") // NOTE: from & to are equal
						));
			}

			// Release the resources (clean up )

			result.close();
			ps.close();
			// s.close();
			// conn.getAutoCommit();
			// conn.commmit();
			conn.close();

		} catch (Throwable e) {
			System.out.println("... Exception thrown:");
			errorPrint(e);
		}

		return expressionOccurrences;
	}

	/**
	 * Finds words frequency in corpus, in each year, available within the
	 * corpus (partial list, not all years are returned)
	 * 
	 * @return List of Occurrences (year, occurrences)
	 */
	public ArrayList<Occurrence> getYearlyWordCounts() {
		PreparedStatement ps;
		ResultSet result = null;
		ArrayList<Occurrence> yearlyWordCounts = new ArrayList<Occurrence>();

		try {
			conn = establishConnection();
			// Create SQL statement
			// SELECT words in each year, from DB
			ps = conn
					.prepareStatement("SELECT year, documents, words FROM documents_summary ORDER BY year;");
			result = ps.executeQuery();
			while (result.next()) {
				yearlyWordCounts.add(new Occurrence(result.getInt("words"),
						result.getInt("year"), result.getInt("year") // NOTE:
																		// from
																		// & to
																		// are
																		// equal
						));
			}

			// Release the resources (clean up )

			result.close();
			ps.close();
			// conn.getAutoCommit();
			// conn.commmit();
			conn.close();

		} catch (Throwable e) {
			System.out.println("... Exception thrown:");
			errorPrint(e);
		}

		return yearlyWordCounts;
	}

	private Connection establishConnection() {
		try {
			Class.forName(driver);
			return DriverManager.getConnection(pgConnectionURL, pgUser,
					pgPassword);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // DriverManager.registerDriver();
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void deleteAllData() {
		PreparedStatement ps;
		boolean result;
		// int result = -1;
		// structure of a record: [ id | User | Title | Tags | People |
		// Timeframe ]
		// Prepare the insert statement to use

		conn = establishConnection();
		// Create SQL statement
		try {
			ps = conn
					.prepareStatement("DELETE FROM statistics_expression_histogram; DELETE FROM expression_occurances; DELETE FROM expressions; DELETE FROM sentences; DELETE FROM documents;");
			result = ps.execute(); // ps.executeUpdate();
			// Release resources
			ps.close();
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("DB: Deleted Documents TABLE.");
	}

	public void clearStatsExpressionHistogramData() {
		this.clearTableData("statistics_expression_histogram");
	}

	private void clearTableData(String table) {
		PreparedStatement ps;
		int result = -1;
		// structure of a record: [ id | User | Title | Tags | People |
		// Timeframe ]
		// Prepare the insert statement to use

		conn = establishConnection();
		// Create SQL statement
		try {
			ps = conn.prepareStatement("DELETE FROM " + table);
			result = ps.executeUpdate();
			// Release resources
			ps.close();
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("DB: Deleted data from TABLE " + table);
	}

	/*
	 * public int deleteReport(int id) { // connect to DB, and get all reports
	 * this user has: Connection conn = null; Statement s; PreparedStatement ps;
	 * int result = -1; // structure of a record: [ id | User | Title | Tags |
	 * People | // Timeframe ] // Prepare the insert statement to use try { conn
	 * = DriverManager.getConnection(connectionURL);
	 * System.out.println("OPENED Connection");
	 * 
	 * // Create SQL statement s = conn.createStatement(); ps =
	 * conn.prepareStatement("DELETE FROM REPORTS WHERE id=(?)"); ps.setInt(1,
	 * id); result = ps.executeUpdate();
	 * System.out.println("Deleted from Reports, returned:" + result);
	 * 
	 * // Release the resources (clean up ) ps.close(); s.close(); conn.close();
	 * System.out.println("CLOSED Connection"); } catch (Throwable e) {
	 * System.out.println("... Exception thrown:"); errorPrint(e); } return
	 * result; }
	 */

	public void shutdown() {
		try {
			// ## DATABASE SHUTDOWN SECTION ##
			/***
			 * In embedded mode, an application should shut down Derby. Shutdown
			 * throws the XJ015 exception to confirm success.
			 ***/
			if (driver.equals("org.apache.derby.jdbc.EmbeddedDriver")) {
				boolean gotSQLExc = false;
				try {
					DriverManager.getConnection("jdbc:derby:;shutdown=true");
				} catch (SQLException se) {
					if (se.getSQLState().equals("XJ015")) {
						gotSQLExc = true;
					}
				}

				if (!gotSQLExc) {
					System.out.println("Database did not shut down normally");
				} else {
					System.out.println("Database shut down normally");
				}
			}

			// Beginning of the primary catch block: uses errorPrint method
		} catch (Throwable e) {
			/*
			 * Catch all exceptions and pass them to* the exception reporting
			 * method
			 */
			System.out.println(" . . . exception thrown:");
			errorPrint(e);
		}
	}

	/*
	 * public String getDocuments(String user) { // connect to DB, and get all
	 * reports this user has: Connection conn = null; Statement s;
	 * PreparedStatement ps; ResultSet documents; // structure of a record: [ id
	 * | User | Title | Tags // | People | Timeframe ] String answer = ""; //
	 * Prepare the insert statement to use
	 * 
	 * try { conn = DriverManager.getConnection(connectionURL);
	 * System.out.println("OPENED Connection");
	 * 
	 * // Create SQL statement s = conn.createStatement(); // Select all records
	 * in the WISH_LIST table ps =
	 * conn.prepareStatement("SELECT * FROM documents" +
	 * "WHERE year < (?) ORDER BY year"); // TODO: use ps, preparedStatement,
	 * instead of reports // Insert the text entered into the WISH_ITEM table
	 * ps.setInt(1, user); documents = ps.executeQuery();//.executeUpdate();
	 * 
	 * 
	 * reports =
	 * s.executeQuery("select ID, TITLE, TAGS, PEOPLE, TIMEFRAME from REPORTS "
	 * + "WHERE USER=(?) order by TITLE");
	 * 
	 * // Loop through the ResultSet and print the data
	 * 
	 * //List<Report> arr = new ArrayList<Report>();// = new List<Object>();
	 * 
	 * while (documents.next()) { System.out.println(documents.getInt(1) +
	 * documents.getString(2) + documents.getString(3) + documents.getString(4)
	 * + documents.getString(5)); }
	 * 
	 * 
	 * while (ps.getMoreResults()) System.out.println("DB Report: " +
	 * ps.getResultSet().getString(1)); // Close the resultSet
	 * documents.close(); // Check if it is time to EXIT, if so end the loop //
	 * } while (! answer.equals("exit")) ; // End of do-while loop
	 * 
	 * // Release the resources (clean up ) ps.close(); s.close(); conn.close();
	 * System.out.println("CLOSED Connection"); } catch (Throwable e) {
	 * System.out.println("... Exception thrown:"); errorPrint(e); } return
	 * answer; // TODO: return JSON? }
	 */

	// ## DERBY EXCEPTION REPORTING CLASSES ##
	/***
	 * Exception reporting methods with special handling of SQLExceptions
	 ***/
	static void errorPrint(Throwable e) {
		if (e instanceof SQLException)
			SQLExceptionPrint((SQLException) e);
		else {
			System.out.println("A non SQL error occured.");
			e.printStackTrace();
		}
	} // END errorPrint

	// Iterates through a stack of SQLExceptions
	static void SQLExceptionPrint(SQLException sqle) {
		while (sqle != null) {
			System.out.println("\n---SQLException Caught---\n");
			System.out.println("SQLState:   " + (sqle).getSQLState());
			System.out.println("Severity: " + (sqle).getErrorCode());
			System.out.println("Message:  " + (sqle).getMessage());
			sqle.printStackTrace();
			sqle = sqle.getNextException();
		}
	} // END SQLExceptionPrint

	/**
	 * Loads expression histograms (raw & relative), from DB table
	 * statistics_expression_histogram.
	 * 
	 * @return ArrayList<ExpressionHistogram> - a list of all expressions, along
	 *         with their histograms.
	 */
	public ArrayList<ExpressionHistogram> getExpressionHistograms() {
		PreparedStatement ps;
		ResultSet result = null;
		ArrayList<ExpressionHistogram> expressionHistograms = new ArrayList<ExpressionHistogram>();

		try {
			conn = establishConnection();
			// Create SQL statement
			// s = conn.createStatement();

			// TODO: cancel LIMIT in SELECT expressions
			if (DEBUG) {
				ps = conn
						.prepareStatement("SELECT expression_id, histogram_raw, histogram_relative FROM statistics_expression_histogram LIMIT 10;");
			} else {
				ps = conn
						.prepareStatement("SELECT expression_id, histogram_raw, histogram_relative FROM statistics_expression_histogram;");

			}

			result = ps.executeQuery(); // ps.executeUpdate();
			while (result.next()) {
				expressionHistograms.add(new ExpressionHistogram(result
						.getString("expression_id"), result
						.getArray("histogram_raw"), result
						.getArray("histogram_relative")));
			}

			// Release the resources (clean up )

			result.close();
			ps.close();
			// s.close();
			// conn.getAutoCommit();
			// conn.commmit();
			conn.close();

		} catch (Throwable e) {
			System.out.println("... Exception thrown:");
			errorPrint(e);
		}

		return expressionHistograms;
	}

	/**
	 * Get the number of expressions found in the
	 * statistics_expression_histogram table
	 * 
	 * @return int
	 */
	public int getTotalExpressionHistograms() {
		PreparedStatement ps;
		ResultSet result = null;
		int expressionsFound = 0;
		try {
			conn = establishConnection();
			// Create SQL statement
			// s = conn.createStatement();

			ps = conn
					.prepareStatement("SELECT COUNT (expression_id) as Expressions_found FROM statistics_expression_histogram");

			result = ps.executeQuery(); // ps.executeUpdate();
			if (result.next()) {
				expressionsFound = result.getInt("Expressions_found");
			}

			// Release the resources (clean up )

			result.close();
			ps.close();
			// s.close();
			// conn.getAutoCommit();
			// conn.commmit();
			conn.close();

		} catch (Throwable e) {
			System.out.println("... Exception thrown:");
			errorPrint(e);
		}

		return expressionsFound;
	}

	public HashMap<String, Trend> getGoogleSuspectExpressions(
			boolean rawCounts) {
		return this.getGoogleExpressionHistograms(rawCounts, "googlesn_suspect_expression_histograms");
	}

	/**
	 * Loads found MWEs in Google Syntactic Ngrams corpus. Returns only raw
	 * counts. normalization by app, is possible.
	 * 
	 * @return ArrayList<ExpressionHistogram> - a list of all expressions, along
	 *         with their histograms.
	 */
	public HashMap<String, Trend> getGoogleExpressions(
			boolean rawCounts) {
		return this.getGoogleExpressionHistograms(rawCounts, "googlesn_expression_histograms");
	}
	
	
	public HashMap<String, Trend> getGoogleExpressionHistograms(
			boolean rawCounts, String tableName) {
		PreparedStatement ps;
		ResultSet result = null;
		HashMap<String, Trend> expressionHistograms = new HashMap<String, Trend>();

		try {
			conn = establishConnection();
			// Create SQL statement
			// s = conn.createStatement();

			ps = conn
					.prepareStatement("SELECT expression_id, histogram_raw, kendalltauz FROM "+tableName+";");

			result = ps.executeQuery(); // ps.executeUpdate();
			while (result.next()) {
				if (rawCounts) {
					expressionHistograms.put(
							result.getString("expression_id"),
							new Trend(result.getString("expression_id"), result
									.getArray("histogram_raw"), result
									.getDouble("kendalltauz")));
				} else {
					// TODO:...
				}

			}

			// Release the resources (clean up )

			result.close();
			ps.close();
			// s.close();
			// conn.getAutoCommit();
			// conn.commmit();
			conn.close();

		} catch (Throwable e) {
			System.out.println("... Exception thrown:");
			errorPrint(e);
		}

		return expressionHistograms;
	}

	public void deleteGoogleSNexpressionHistograms() {
		PreparedStatement ps;
		boolean result;
		conn = establishConnection();
		try {
			ps = conn
					.prepareStatement("DELETE FROM googlesn_expression_histograms");
			result = ps.execute();
			
			ps = conn
					.prepareStatement("DELETE FROM googlesn_suspect_expression_histograms");
			result = ps.execute();
			// Release resources
			ps.close();
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("DB: Deleted TABLE googlesn_expression_histograms.");
	}

}
