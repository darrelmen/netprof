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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SQLExerciseDAO implements ExerciseDAO {
  private static Logger logger = Logger.getLogger(SQLExerciseDAO.class);

  private static final String ENCODING = "UTF8";

  private final Database database;
  private final String mediaDir;
  private static final boolean DEBUG = false;
  /**
   * @see DatabaseImpl#makeExerciseDAO(boolean)
   * @param database
   * @param mediaDir
   */
  public SQLExerciseDAO(Database database, String mediaDir) {
    this.database = database;
    this.mediaDir = mediaDir;
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
    try {
      Connection connection = database.getConnection();
      String sql = "SELECT * FROM exercises";
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      // int count = 0;
      while (rs.next()) {
      //  if (count++ > 10) break;
        String plan = rs.getString(1);
        String exid = rs.getString(2);
        // String type = rs.getString(3);
        // boolean onlyfa = rs.getBoolean(4);
        String content = getStringFromClob(rs.getClob(5));

        if (content.startsWith("{")) {
          JSONObject obj = JSONObject.fromObject(content);
          Exercise e = getExercise(plan, exid, obj);
          if (e == null) {
            logger.warn("couldn't find exercise for plan '" + plan + "'");
          } else if (e.getID() == null) {
            logger.warn("no valid exid for " + e);
          } else {
            exercises.add(e);
          }
        } else {
          logger.warn("expecting a { (marking json data), so skipping " + content);
        }
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);
    } catch (Exception e) {
      logger.warn("got " + e,e);
    }

    if (exercises.isEmpty()) {
      logger.warn("no exercises found in database?");
    } else {
      if (DEBUG) logger.debug("getRawExercises : found " + exercises.size() + " exercises.");
    }
    return exercises;
  }

  /**
   * Parse the json that represents the exercise.  Created during ingest process (see ingest.scala).
   * Remember to prefix any media references (audio or images) with the location of the media directory,
   *
   * e.g. config/pilot/media
   *
   * @see #getRawExercises()
   * @param plan that this exercise is a part of
   * @param exid id for the exercise
   * @param obj json to get content and questions from
   * @return Exercise from the json
   */
  private Exercise getExercise(String plan, String exid, JSONObject obj) {
    String content = getContent(obj);

    String tip = "Item #"+exid; // TODO : have more informative tooltip
    Exercise exercise = new Exercise(plan, exid, content, false, false, tip);
    Object qa1 = obj.get("qa");
    if (qa1 == null) {
      logger.warn("no qa key in " + obj.keySet());
    }
    Collection<JSONObject> qa = JSONArray.toCollection((JSONArray) qa1, JSONObject.class);
    addQuestions(exercise, qa);
    return exercise;
  }

  /**
   * Remember to prefix any media references (audio or images) with the location of the media directory,
   *
   * e.g. config/pilot/media
   *
   * @param obj json to get content from
   * @return content with media paths set
   */
  private String getContent(JSONObject obj) {
    String content = (String) obj.get("content");
    if (content == null) {
      logger.warn("no content key in " + obj.keySet());
    } else {
      // prefix the media dir
      String srcPattern = " src\\s*=\\s*\"";
      content = content.replaceAll(srcPattern, " src=\"" + mediaDir.replaceAll("\\\\", "/") + "/");
    }
    return content;
  }

  private void addQuestions(Exercise exercise, Collection<JSONObject> qa) {
    for (JSONObject o : qa) {
      Set<String> keys = o.keySet();
      for (String lang : keys) {
        JSONObject qaForLang = (JSONObject) o.get(lang);
        String answerKey = (String) qaForLang.get("answerKey");
        List<String> alternateAnswers = Arrays.asList(answerKey.split("\\|\\|"));
        exercise.addQuestion(lang, (String) qaForLang.get("question"), answerKey, alternateAnswers);
      }
    }
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