package mitll.langtest.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.LangTestDatabase;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/7/12
 * Time: 5:49 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("serial")
public class LangTestDatabaseImpl extends RemoteServiceServlet implements LangTestDatabase {
/*  private String url = "jdbc:mysql://localhost:3306/",
    dbOptions = "?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull",
    driver = "com.mysql.jdbc.Driver";*/

  private String url = "jdbc:h2:latest",
    dbOptions = "",//"?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull",
    driver = "org.h2.Driver";

  public LangTestDatabaseImpl() {
    try {
//      checkDB();
    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  private Connection dbLogin() throws Exception {
    try {
      Class.forName(driver).newInstance();
      Connection connection = DriverManager.getConnection(url
        + /*getInitParameter(PRETEST_DATABASE) +*/ dbOptions,
        "sa",
        "");
      connection.setAutoCommit(false);
      return connection;
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  public void checkDB() throws Exception {
    System.out.println("checkDB called");
    Connection connection = this.dbLogin();
    PreparedStatement statement = connection.prepareStatement("SELECT * FROM exercises");



 //   statement.setString(1, exerciseTitle);
    ResultSet rs = statement.executeQuery();

           while(rs.next()) {
             Clob clob = rs.getClob(4);

             Reader characterStream = clob.getCharacterStream();
             BufferedReader br = new BufferedReader(characterStream);
             int c= 0;
             char cbuf[] = new char[1024];
             StringBuilder b = new StringBuilder();
             while((c = br.read(cbuf)) != -1) {
                                                   b.append(cbuf,0,c);
             };
             characterStream.read();
             net.sf.json.JSONObject obj = net.sf.json.JSONObject.fromObject(b.toString());
            // clob.getSubString(1, clob.length.toInt).replaceAll("\\s+", " ");
               System.err.println("got " + rs.getString(1) +"/" + rs.getString(2) + "/"+ rs.getString(3) + " : " +obj);
           }

    connection.commit();
    connection.close();
    System.out.println("checkDB returning ");
    //return exercise_id;
  }

public void test() {
	  try {
	      checkDB();
	    } catch (Exception e) {
	      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
	    }}
}
