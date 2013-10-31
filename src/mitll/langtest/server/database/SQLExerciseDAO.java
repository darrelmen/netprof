package mitll.langtest.server.database;

import mitll.langtest.server.database.testing.SmallDatabaseImpl;
import mitll.langtest.shared.Exercise;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SQLExerciseDAO implements ExerciseDAO {
  private static Logger logger = Logger.getLogger(SQLExerciseDAO.class);

  private static final String ENCODING = "UTF8";
  private static final boolean DEBUG = false;

  private final Database database;
  private String mediaDir;
  private List<Exercise> exercises;
  private Map<String,Exercise> idToExercise = new HashMap<String,Exercise>();
  private SectionHelper sectionHelper = new SectionHelper();

  /**
   * @see DatabaseImpl#makeExerciseDAO(boolean)
   * @param database
   * @param mediaDir
   */
  public SQLExerciseDAO(Database database, String mediaDir) {
    this.database = database;
    this.mediaDir = mediaDir;
    logger.debug("database " + database + " media dir " + mediaDir);
  }

  @Override
  public SectionHelper getSectionHelper() { return sectionHelper; }

  @Override
  public Exercise getExercise(String id) {
    if (idToExercise.isEmpty()) logger.warn("huh? couldn't find any exercises..?");
    if (!idToExercise.containsKey(id)) {
      logger.warn("couldn't find " +id + " in " +idToExercise.size() + " exercises...");
    }
    return idToExercise.get(id);
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
    if (exercises == null) {
      String sql = "SELECT * FROM exercises";
      exercises = getExercises(sql);
      populateIDToExercise();
    }
    return exercises;
  }

  private void populateIDToExercise() {
    for (Exercise e : exercises) idToExercise.put(e.getID(),e);
  }

  private List<Exercise> getExercises(String sql) {
    List<Exercise> exercises = new ArrayList<Exercise>();
    try {
      Connection connection = database.getConnection();
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
            recordUnitChapterWeek(e);
          }
        } else {
          logger.warn("expecting a { (marking json data), so skipping " + content);
        }
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);

   //   logger.debug("reporting for " +database);
   //   sectionHelper.report();

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

 // private boolean debug = true;
  private boolean recordUnitChapterWeek(Exercise imported) {
    String[] split = imported.getID().split("-");
    String unit = split[0];//getCell(next, unitIndex);
    String chapter = split.length > 1 ? split[1] : "";//getCell(next, chapterIndex);
    String week = "";//getCell(next, weekIndex);
    List<SectionHelper.Pair> pairs = new ArrayList<SectionHelper.Pair>();

    if (unit.length() == 0 &&
      chapter.length() == 0 &&
      week.length() == 0
      ) {
      unit = "Blank";
    }

    // hack to trim off leading tics
    if (unit.startsWith("'")) unit = unit.substring(1);
    if (unit.equals("intro")) unit = "Intro"; // hack
    if (chapter.startsWith("'")) chapter = chapter.substring(1);
    if (week.startsWith("'")) week = week.substring(1);

    //if (debug) logger.debug("unit " + unitIndex +"/"+unit + " chapter " + chapterIndex+"/"+chapter + " week " + week);

    if (unit.length() > 0) {
        pairs.add(sectionHelper.addUnitToLesson(imported,unit));
    }
    if (chapter.length() > 0) {
        pairs.add(sectionHelper.addChapterToLesson(imported,chapter));
    }
    if (week.length() > 0) {
        pairs.add(sectionHelper.addWeekToLesson(imported,week));
    }
    sectionHelper.addAssociations(pairs);

    return false;
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

    //String tip = "Item #"+exid; // TODO : have more informative tooltip
    String tip = exid; // TODO : have more informative tooltip
    Exercise exercise = new Exercise(plan, exid, "", false, false, tip);

    String content = getContent(obj,exercise);

    content = convertTableMarkup(content);
    exercise.setContent(content);

    Object qa1 = obj.get("qa");
    if (qa1 == null) {
      logger.warn("no qa key in " + obj.keySet());
    }
    Collection<JSONObject> qa = JSONArray.toCollection((JSONArray) qa1, JSONObject.class);
    addQuestions(exercise, qa);
    return exercise;
  }

  private String convertTableMarkup(String content) {
    if (content.contains("<td dir=\"rtl\">")) {
      content = content.replaceAll("Orientation :","Question Scenario");
      content = content.replaceAll("Orientation:","Question Scenario");
      content = content.replaceAll("<td width=\"20%\"> &nbsp; </td>","");
      content = content.replaceAll("td","h3");
      content = content.replaceAll("br", "h3");
      if (content.contains(":")) {
        content = content.replaceAll(":", " ");
      } else {
//        logger.debug("no colon in " + content);
      }
      content += "</h3>";
    }
    if (content.contains("<p")) {
      content = content.replaceAll("<p>\\s+</p>","");
      content = content.replaceAll("<p> &nbsp; </p>","");
      content = content.replaceAll("<p","<h3").replaceAll("p>","h3>");
    }
    return content;
  }

  /**
   * Remember to prefix any media references (audio or images) with the location of the media directory,
   *
   * e.g. config/pilot/media
   *
   * @param obj json to get content from
   * @return content with media paths set
   */
  private String getContent(JSONObject obj, Exercise exercise) {
    String content = (String) obj.get("content");
    if (content == null) {
      logger.warn("no content key in " + obj.keySet());
    } else {
      content = getSrcHTML5WithMediaDir(content,mediaDir);
      if (content.contains("<audio")) {
        String[] split = content.split("<audio");
        String before = split[0];
        String after = split[1];

       // logger.debug("before " + before);
     //   logger.debug("after " + after);
        String[] split1 = after.split("</audio>");
        String audioTag = split1[0];
        String afterContent = split1.length > 1 ? split1[1] : "";

      //  logger.debug("audioTag " + audioTag);

        String[] split2 = audioTag.split("src=\"");

        String audioPathOrig = split2[1];
     //   logger.debug("audioPathOrig " + audioPathOrig);

        String audioPath = audioPathOrig.split("mp3")[0];
        exercise.setRefAudio(audioPath + "mp3");


        content = before + /*newStuff + */afterContent;
/*        System.out.println("Content " + content);
        System.out.println("ref audio " + exercise.getRefAudio());*/
      }
    }

    return content;
  }

  public String getSrcHTML5WithMediaDir(String content, String mediaDir) {
    // prefix the media dir
    String srcPattern = " src\\s*=\\s*\"";
    content = content.replaceAll(srcPattern, " src=\"" + mediaDir.replaceAll("\\\\", "/") + "/");
    return content;
  }

  private void addQuestions(Exercise exercise, Collection<JSONObject> qa) {
    for (JSONObject o : qa) {
      Set keys = o.keySet();
      for (Object lang : keys) {
        JSONObject qaForLang = (JSONObject) o.get(lang);
        String answerKey = (String) qaForLang.get("answerKey");
        List<String> alternateAnswers = Arrays.asList(answerKey.split("\\|\\|"));
        exercise.addQuestion(lang.toString(), (String) qaForLang.get("question"), answerKey, alternateAnswers);
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

/*  private static String getConfigDir(String language) {
    String installPath = ".";
    String dariConfig = File.separator +
      "war" +
      File.separator +
      "config" +
      File.separator +
      language +
      File.separator;
    return installPath + dariConfig;
  }*/

  private static void dumpQuestionsAndAnswers(SQLExerciseDAO sqlExerciseDAO) {
    List<Exercise> rawExercises = sqlExerciseDAO.getRawExercises();
    //Exercise next = rawExercises.iterator().next();
    try {
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("L0PQuestionAndAnswer.tsv"), FileExerciseDAO.ENCODING));
      BufferedWriter writer2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("L0PQuestionAndAnswerEnglish.tsv"), FileExerciseDAO.ENCODING));
      //  System.out.println("First is " +next + " content " + next.getContent());
      writer.write("ID\tQuestion\tAnswer\n");
      writer2.write("ID\tQuestion\tAnswer\n");

      for (Exercise e : rawExercises) {
        if (e.getID().contains("L0P")) {
          for (Exercise.QAPair qaPair : e.getForeignLanguageQuestions()) {
            String x = e.getID() + "\t" + qaPair.getQuestion() + "\t" + qaPair.getAnswer();
            writer.write(x+"\n");
          }
          for (Exercise.QAPair qaPair : e.getEnglishQuestions()) {
            String x = e.getID() + "\t" + qaPair.getQuestion() + "\t" + qaPair.getAnswer();
            writer2.write(x+"\n");
          }
        }
      }
      writer.close();
      writer2.close();
    } catch (Exception e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public static void main(String [] arg) {


   // final String configDir = getConfigDir("pilot");

/*    DatabaseImpl unitAndChapter = new DatabaseImpl(
      configDir,
      "arabicText",
      configDir +
        spreadsheet);*/

    SQLExerciseDAO sqlExerciseDAO = new SQLExerciseDAO(new SmallDatabaseImpl("war/config/pilot/avpDemo"), "config" +
      File.separator +
      "pilot");
    dumpQuestionsAndAnswers(sqlExerciseDAO);
  }
}