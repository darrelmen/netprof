package mitll.langtest.server;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import mitll.langtest.client.LangTestDatabase;
import mitll.langtest.shared.Exercise;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 5/7/12
 * Time: 5:49 PM
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("serial")
public class LangTestDatabaseImpl extends RemoteServiceServlet implements LangTestDatabase {
  // mysql config info
/*  private String url = "jdbc:mysql://localhost:3306/",
    dbOptions = "?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull",
    driver = "com.mysql.jdbc.Driver";*/

  // h2 config info
  private String url = "jdbc:h2:new",
    dbOptions = "",//"?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull",
    driver = "org.h2.Driver";

  public LangTestDatabaseImpl() {}

  /**
   * Not necessary if we use the h2 DBStarter service -- see web.xml reference
   * @return
   * @throws Exception
   */
  private Connection dbLogin() throws Exception {
    try {
      Class.forName(driver).newInstance();
      System.out.println("connecting to " + url);

      GWT.log("connecting to " + url);
      File f = new java.io.File("new.h2.db");
      if (!f.exists()) {
    	  String s = "huh? no file at " + f.getAbsolutePath();
          System.err.println(s);

    	  GWT.log(s);
      }
      Connection connection = DriverManager.getConnection(url
        + /*getInitParameter(PRETEST_DATABASE) +*/ dbOptions/*,
        "",
        ""*/);
      connection.setAutoCommit(false);
      boolean closed = connection.isClosed();
      System.out.println("closed " + closed);
      return connection;
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  public void test() {
    try {
  //    checkDB();
    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public List<Exercise> getExercises() {
    List<Exercise> exercises = new ArrayList<Exercise>();
    Connection connection = null;

    try {
     // connection = this.dbLogin();
      connection = (Connection)getServletContext().getAttribute("connection");
      PreparedStatement statement = connection.prepareStatement("SELECT * FROM exercises");

      ResultSet rs = statement.executeQuery();
      while(rs.next()) {
        Clob clob = rs.getClob(4);

        InputStreamReader utf8 = new InputStreamReader(clob.getAsciiStream(), "UTF8");
        BufferedReader br = new BufferedReader(utf8);
        int c= 0;
        char cbuf[] = new char[1024];
        StringBuilder b = new StringBuilder();
        while((c = br.read(cbuf)) != -1) {
          b.append(cbuf,0,c);
        }

        if (b.toString().startsWith("{")) {
          net.sf.json.JSONObject obj = net.sf.json.JSONObject.fromObject(b.toString());
          Exercise e = getExercise(obj);
          exercises.add(e);
        }
      }
       rs.close();
       statement.close();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (connection != null) {
     /*   try {
          //connection.close();
        } catch (SQLException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }*/
      }
    }
    return exercises;
  }

  public void addAnswer(String id, int questionID, String answer) {
    System.out.println("Got " +id + " and " + questionID + " and " + answer);
  }

  private Exercise getExercise(JSONObject obj) {
    Exercise exercise = new Exercise((String) obj.get("exid"), (String) obj.get("content"));
    Collection<JSONObject> qa = JSONArray.toCollection((JSONArray) obj.get("qa"), JSONObject.class);
    for (JSONObject o : qa) {
      Set<String> keys = o.keySet();
      for (String k : keys) {
        JSONObject qaForLang = (JSONObject)o.get(k);
        exercise.addQuestion(k, (String) qaForLang.get("question"), (String) qaForLang.get("answerKey"));
      }
    }
    return exercise;
  }

  public static void main(String[] arg) {
    LangTestDatabaseImpl langTestDatabase = new LangTestDatabaseImpl();
    for (Exercise e : langTestDatabase.getExercises()) System.err.println("e " + e);
  }
}
