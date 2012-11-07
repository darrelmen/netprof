package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SQLExerciseDAO implements ExerciseDAO {
  private static Logger logger = Logger.getLogger(SQLExerciseDAO.class);

  private final Database database;
  private static final String ENCODING = "UTF8";

  /**
   * @see DatabaseImpl
   * @param database
   */
  public SQLExerciseDAO(Database database) {
    this.database = database;
  }

  /**
   * Hit the database for the exercises
   * <p/>
   * Mainly what is important is the json exercise content (column 5).
   *
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getExercises
   */
  public List<Exercise> getRawExercises() {
    List<Exercise> exercises = new ArrayList<Exercise>();
    Connection connection = null;
  //  SortedSet<String> ids = new TreeSet<String>();
    try {
      connection = database.getConnection();
      String sql = "SELECT * FROM exercises";
      PreparedStatement statement = connection.prepareStatement(sql);
      // logger.info("doing " + sql + " on " + connection);
      ResultSet rs = statement.executeQuery();
      while (rs.next()) {
        String plan = rs.getString(1);
        String exid = rs.getString(2);
        String type = rs.getString(3);
        boolean onlyfa = rs.getBoolean(4);
        String content = getStringFromClob(rs.getClob(5));

        if (content.startsWith("{")) {
          JSONObject obj = JSONObject.fromObject(content);
          Exercise e = getExercise(plan, exid, obj);
          if (e == null) {
            logger.warn("couldn't find exercise for plan '" + plan + "'");
            continue;
          }
          if (e.getID() == null) {
            logger.warn("no valid exid for " + e);
            continue;
          }
      //    ids.add(e.getID());
          exercises.add(e);
        } else {
          logger.warn("expecting a { (marking json data), so skipping " + content);
        }
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      logger.warn("got " + e,e);
    } finally {
      if (connection != null) {
        /*   try {
          //connection.close();
        } catch (SQLException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }*/
      }
    }
    if (exercises.isEmpty()) {
      logger.warn("no exercises found in database?");
    } else {
      logger.info("getRawExercises : found " + exercises.size() + " exercises.");
    }
    return exercises;
  }



  /**
   * Parse the json that represents the exercise.  Created during ingest process (see ingest.scala).
   *
   * @param obj
   * @return
   */
  private Exercise getExercise(String plan, String exid, JSONObject obj) {
    boolean promptInEnglish = false;
    boolean recordAudio = false;
    String content = (String) obj.get("content");
    if (content == null) {
      logger.warn("no content key in " + obj.keySet());
    }
    Exercise exercise = new Exercise(plan, exid, content, promptInEnglish, recordAudio);
    Object qa1 = obj.get("qa");
    if (qa1 == null) {
      logger.warn("no qa key in " + obj.keySet());
    }
    Collection<JSONObject> qa = JSONArray.toCollection((JSONArray) qa1, JSONObject.class);
    for (JSONObject o : qa) {
      Set<String> keys = o.keySet();
      for (String k : keys) {
        JSONObject qaForLang = (JSONObject) o.get(k);
        exercise.addQuestion(k, (String) qaForLang.get("question"), (String) qaForLang.get("answerKey"));
      }
    }
    return exercise;
  }


  private String getStringFromClob(Clob clob) throws SQLException, IOException {
    InputStreamReader utf8 = new InputStreamReader(clob.getAsciiStream(), ENCODING);
    BufferedReader br = new BufferedReader(utf8);
    int c;
    char cbuf[] = new char[1024];
    StringBuilder b = new StringBuilder();
    while ((c = br.read(cbuf)) != -1) {
      b.append(cbuf, 0, c);
    }
    return b.toString();
  }
}