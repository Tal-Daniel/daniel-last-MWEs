package idesign.tal.db;

//import java.io.*;
import java.sql.*;

public class Util {

	/***      Check for  WISH_LIST table    ****/
	   public static boolean Chk4Table (Connection conTst, String checkStatement ) throws SQLException {
//	      boolean chk = true;
//	      boolean doCreate = false;
	      try {
	         Statement s = conTst.createStatement();
	         s.execute(checkStatement);
	      }  catch (SQLException sqle) {
	         String theError = (sqle).getSQLState();
	         //System.out.println("  Utils GOT:  " + theError);
	         /** If table exists will get -  WARNING 02000: No row was found **/
	         if (theError.equals("42X05"))   // Table does not exist
	         {  return false;
	          }  else if (theError.equals("42X14") || theError.equals("42821"))  {
	             System.out.println("@Chk4Table: Incorrect table definition.");
	             throw sqle;   
	          } else { 
	             System.out.println("@Chk4Table: Unhandled SQLException" );
	             throw sqle; 
	          }
	      }
	      //  System.out.println("Just got the warning - table exists OK ");
	      return true;
	   }  /*** END wwdInitTable  **/

}

