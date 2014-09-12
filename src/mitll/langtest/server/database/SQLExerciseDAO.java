package mitll.langtest.server.database;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.custom.UserExerciseDAO;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonUserExercise;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SQLExerciseDAO implements ExerciseDAO {
  private static final Logger logger = Logger.getLogger(SQLExerciseDAO.class);

  private static final String HEADER_TAG = "h4";
  public static final String ENCODING = "UTF8";
  private static final boolean DEBUG = false;

  private final Database database;
  private final String mediaDir;
  private List<CommonExercise> exercises;

  private final Map<String,CommonExercise> idToExercise = new HashMap<String,CommonExercise>();
  private final SectionHelper sectionHelper = new SectionHelper();
  private final ILRMapping ilrMapping;

  /**
   * @see DatabaseImpl#makeExerciseDAO(boolean)
   * @param database
   * @param mediaDir
   * @param configDir
   * @param properties
   */
  public SQLExerciseDAO(Database database, String mediaDir, String configDir, ServerProperties properties) {
    this.database = database;
    this.mediaDir = mediaDir;
    logger.debug("database " + database + " media dir " + mediaDir);

    ilrMapping = new ILRMapping(configDir, sectionHelper, properties.getMappingFile());
    getRawExercises();
    ilrMapping.report(idToExercise);
  }

  @Override
  public void setAudioDAO(AudioDAO audioDAO) {

  }

  @Override
  public SectionHelper getSectionHelper() { return sectionHelper; }

  @Override
  public CommonExercise addOverlay(CommonUserExercise userExercise) { return null; }

  @Override
  public void setAddRemoveDAO(AddRemoveDAO addRemoveDAO) {}

  @Override
  public boolean remove(String id) {
    return false;
  }

  @Override
  public void add(CommonUserExercise userExercise) {}

  @Override
  public void setUserExerciseDAO(UserExerciseDAO userExerciseDAO) {}

  @Override
  public CommonExercise getExercise(String id) {
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
  public List<mitll.langtest.shared.CommonExercise> getRawExercises() {
    if (exercises == null) {
      String sql = "SELECT * FROM exercises";
      exercises = getExercises(sql);
      populateIDToExercise();
    }
    return exercises;
  }

  private void populateIDToExercise() {
    for (CommonExercise e : exercises) idToExercise.put(e.getID(),e);
  }

  private List<CommonExercise> getExercises(String sql) {
    List<CommonExercise> exercises = new ArrayList<CommonExercise>();
    try {
      Connection connection = database.getConnection(this.getClass().toString());
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      boolean useMapping = ilrMapping.useMapping();
      while (rs.next()) {
        String plan = rs.getString(1);
        String exid = rs.getString(2);
        String content = getStringFromClob(rs.getClob(5));

        if (content.startsWith("{")) {
          JSONObject obj = JSONObject.fromObject(content);
          CommonExercise e = getExercise(plan, exid, obj);
          if (e == null) {
            logger.warn("couldn't find exercise for plan '" + plan + "'");
          } else if (e.getID() == null) {
            logger.warn("no valid exid for " + e);
          } else {
            exercises.add(e);

            if (useMapping) {
              ilrMapping.addMappingAssoc(exid, e);
            }
            else {
              recordUnitChapterWeek(e);
            }
          }
        } else {
          logger.warn("expecting a { (marking json data), so skipping " + content);
        }
      }

      ilrMapping.finalStep();
      rs.close();
      statement.close();
      database.closeConnection(connection);

      logger.debug("reporting for " +database);
      sectionHelper.report();

    } catch (Exception e) {
      logger.error("got " + e, e);
    }

    if (exercises.isEmpty()) {
      logger.warn("no exercises found in database?");
    } else {
      if (DEBUG) logger.debug("getRawExercises : found " + exercises.size() + " exercises.");
    }
    return exercises;
  }

  /**
   * @see #getExercises(String)
   * @param imported
   * @return
   */
  private boolean recordUnitChapterWeek(CommonExercise imported) {
    String[] split = imported.getID().split("-");
    String unit = split[0];
    String chapter = split.length > 1 ? split[1] : "";
    String week = "";
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

    if (DEBUG) logger.debug("unit " +unit + " chapter " + chapter + " week " + week + " for " + imported.getID());

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
   * @return CommonExercise from the json
   */
  private CommonExercise getExercise(String plan, String exid, JSONObject obj) {
    //String tip = "Item #"+exid; // TODO : have more informative tooltip
    String tip = exid; // TODO : have more informative tooltip
    Exercise exercise = new Exercise(plan, exid, "", false, false, tip, "");

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

  /**
   * <table>\n<tr><td> <i>Orientation: <\/i><\/td>\n<td width=\"20%\"> &nbsp; <\/td>\n<td dir=\"rtl\"> \u0631\u062d\u0644\u0629 \u0627\u0644\u0649 \u0645\u0635\u0631: <\/td><\/tr>\n<\/table>\n<p> &nbsp; <\/p>\n<h2>
   * @param content
   * @return
   */
  private String convertTableMarkup(String content) {
    content = content.replaceAll("<td width=\"20%\"> &nbsp; </td>","");
    if (content.contains("<td dir=\"rtl\">")) {
      content = content.replaceAll("Orientation :","Question Scenario");
      content = content.replaceAll("Orientation:","Question Scenario");
      content = content.replaceAll("td", HEADER_TAG);
      content = content.replaceAll("br", HEADER_TAG);
      //if (content.contains(":")) {
      //  content = content.replaceAll(":", " ");
      //}
      content += "</h3>";
    }
    content = content.replaceAll("dir=\"rtl\"","dir=\"rtl\" style=\"text-align:right\"");
    content = content.replaceAll("h3","h4");
    if (content.contains("<p")) {
      content = content.replaceAll("<p>\\s+</p>","");
      content = content.replaceAll("<p> &nbsp; </p>","");
      content = content.replaceAll("<p","<h4").replaceAll("p>","h4>");
    }
    return content;
  }

  /**
   * Remember to prefix any media references (audio or images) with the location of the media directory,
   *
   * e.g. config/pilot/media
   *
   * If there's an audio tag in the content, make that the ref audio for the exercise.
   * @see Exercise#setRefAudio(String)
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
        String[] split1 = after.split("</audio>");
        String audioTag = split1[0];
        String afterContent = split1.length > 1 ? split1[1] : "";
        String[] split2 = audioTag.split("src=\"");
        String audioPathOrig = split2[1];
        String audioPath = audioPathOrig.split("mp3")[0];
        exercise.setRefAudio(audioPath + "mp3");

        content = before + afterContent;
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
      Set<?> keys = o.keySet();
      for (Object lang : keys) {
        JSONObject qaForLang = (JSONObject) o.get(lang);
        String answerKey = (String) qaForLang.get("answerKey");
        List<String> alternateAnswers = Arrays.asList(answerKey.split("\\|\\|"));
        String lang1 = lang.toString();
        String question = (String) qaForLang.get("question");
     //   logger.debug("\tFor " + exercise.getID() + " " + lang1 + " : " + question);
        exercise.addQuestion(lang1, question, answerKey, alternateAnswers);
      }
    }
   // logger.debug("For " + exercise.getID() + " "+exercise.getQuestions());
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

/*  @Override
  public List<String> getErrors() {
    return errors;
  }*/
}
