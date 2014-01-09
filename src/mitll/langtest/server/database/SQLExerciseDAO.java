package mitll.langtest.server.database;

import mitll.langtest.server.database.custom.UserExerciseDAO;
import mitll.langtest.server.database.testing.SmallDatabaseImpl;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.custom.UserExercise;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SQLExerciseDAO implements ExerciseDAO {
  public static final String HEADER_TAG = "h4";
  private static Logger logger = Logger.getLogger(SQLExerciseDAO.class);

  private static final String ENCODING = "UTF8";
  private static final boolean DEBUG = false;

  private final Database database;
  private String mediaDir;
  private List<Exercise> exercises;
  private Map<String,Exercise> idToExercise = new HashMap<String,Exercise>();
  private SectionHelper sectionHelper = new SectionHelper();
  private Map<String,List<String>> levelToExercises = new HashMap<String,List<String>>();
  private Map<String,String> exerciseToLevel = new HashMap<String,String>();

  /**
   * @see DatabaseImpl#makeExerciseDAO(boolean)
   * @param database
   * @param mediaDir
   * @param configDir
   */
  public SQLExerciseDAO(Database database, String mediaDir, String configDir) {
    this.database = database;
    this.mediaDir = mediaDir;
    logger.debug("database " + database + " media dir " + mediaDir);
  //  File ilrMapping = new File(configDir,"autocrt-docids.txt");
    File ilrMapping = new File(configDir,"vlr-parle-pilot-items.txt");

    if (ilrMapping.exists()) {
      readILRMapping2(ilrMapping);
    }
    else logger.debug("can't find " + ilrMapping.getAbsolutePath());
    getRawExercises();
    int size = idToExercise.keySet().size();
    int size1 = getMappedExercises().size();
    if (size != size1) {
      logger.error("huh? there are " + size + " ids from reading the database, but " + size1 + " from reading the mapping file" );
      Set<String> strings = new HashSet<String>(idToExercise.keySet());
      /*boolean b =*/ strings.removeAll(getMappedExercises());
      logger.error("unmapped are " + strings);
    }
  }

/*  private void readILRMapping(File ilrMapping) {
    try {
      BufferedReader reader = getReader(ilrMapping.getAbsolutePath());
      String line;
      reader.readLine(); //read header
      while ((line = reader.readLine()) != null) {
        String[] split = line.split("\\|");
     //   logger.debug("line " + line + " split size " + split.length);
        if (split.length < 6) continue;
        String ilr = split[5].trim();
        String id = split[6].trim();
        List<String> ids = levelToExercises.get(ilr);
        if (ids == null) {
          levelToExercises.put(ilr, ids = new ArrayList<String>());
        }
        ids.add(id);
      }
      logger.debug("map has size " + levelToExercises.size() + " keys " + levelToExercises.keySet());// + " : " + levelToExercises);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }*/

  private void readILRMapping2(File ilrMapping) {
    try {
      BufferedReader reader = getReader(ilrMapping.getAbsolutePath());
      String line;
      while ((line = reader.readLine()) != null) {
        String[] split = line.split("\t");
        if (split.length < 2) continue;
        String ilr = split[1].trim();
        ilr = ilr.split("/")[0];
        String id = split[0].trim();
        List<String> ids = levelToExercises.get(ilr);
        if (ids == null) {
          levelToExercises.put(ilr, ids = new ArrayList<String>());
        }
        exerciseToLevel.put(id,ilr);
        ids.add(id);
      }
      logger.debug("map has size " + levelToExercises.size() + " keys " + levelToExercises.keySet());// + " : " + levelToExercises);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

/*  public List<String> getExercisesForLevel(String level) {
    return levelToExercises.get(level);
  }*/

  public Set<String> getMappedExercises() {
    Set<String> strings = new HashSet<String>();
    for (List<String> ids : levelToExercises.values()) { strings.addAll(ids); }
    return strings;
  }

  private BufferedReader getReader(String lessonPlanFile) throws FileNotFoundException, UnsupportedEncodingException {
    FileInputStream resourceAsStream = new FileInputStream(lessonPlanFile);
    return new BufferedReader(new InputStreamReader(resourceAsStream,ENCODING));
  }

  @Override
  public SectionHelper getSectionHelper() { return sectionHelper; }

  @Override
  public void addOverlay(UserExercise userExercise) {

  }

  @Override
  public void setUserExerciseDAO(UserExerciseDAO userExerciseDAO) {

  }

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
    //int count = 0;
    try {
      Connection connection = database.getConnection();
      PreparedStatement statement = connection.prepareStatement(sql);
      ResultSet rs = statement.executeQuery();
      boolean useMapping = !exerciseToLevel.isEmpty();
      while (rs.next()) {
      //  if (count++ > 5) break;
        String plan = rs.getString(1);
        String exid = rs.getString(2);
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

            if (useMapping) {
              sectionHelper.addAssociations(Collections.singleton(sectionHelper.addExerciseToLesson(e, "ILR_Level", exerciseToLevel.get(exid))));
            }
            else {
              recordUnitChapterWeek(e);
            }
          }
        } else {
          logger.warn("expecting a { (marking json data), so skipping " + content);
        }
      }
      if (useMapping) {
        sectionHelper.setPredefinedTypeOrder(Arrays.asList("ILR_Level"));
      }
      rs.close();
      statement.close();
      database.closeConnection(connection);

      //logger.debug("reporting for " +database);
      //sectionHelper.report();

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

  /**
   * <table>\n<tr><td> <i>Orientation: <\/i><\/td>\n<td width=\"20%\"> &nbsp; <\/td>\n<td dir=\"rtl\"> \u0631\u062d\u0644\u0629 \u0627\u0644\u0649 \u0645\u0635\u0631: <\/td><\/tr>\n<\/table>\n<p> &nbsp; <\/p>\n<h2>
   * @param content
   * @return
   */
  private String convertTableMarkup(String content) {
    if (content.contains("<td dir=\"rtl\">")) {
      content = content.replaceAll("Orientation :","Question Scenario");
      content = content.replaceAll("Orientation:","Question Scenario");
      content = content.replaceAll("<td width=\"20%\"> &nbsp; </td>","");
      content = content.replaceAll("td", HEADER_TAG);
      content = content.replaceAll("br", HEADER_TAG);
      if (content.contains(":")) {
        content = content.replaceAll(":", " ");
      }
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
   * @see mitll.langtest.client.flashcard.AudioExerciseContent#getAudioWidget
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
      File.separator;  cd w
    return installPath + dariConfig;
  }*/

/*  private static void dumpQuestionsAndAnswers(SQLExerciseDAO sqlExerciseDAO) {
    List<Exercise> rawExercises = sqlExerciseDAO.getRawExercises();
    //Exercise next = rawExercises.iterator().next();
    try {
      String filename = "QuestionAndAnswer";
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename +
        ".tsv"), FileExerciseDAO.ENCODING));
      BufferedWriter writer2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename +
        "English.tsv"), FileExerciseDAO.ENCODING));
      //  System.out.println("First is " +next + " content " + next.getContent());
      writer.write ("ID\tQ #\tQuestion\t" +
        "Answer" +
        "\n");

      writer.write("ID\t"+ "Audio\t" +"Scenario\t"+
        "Q #\tQuestion\t");// +
   //   writer.write("Content\t");
      for (int i = 1; i < 7; i++) writer.write("Answer #" +i+ "\t");
      writer.write("\n");

      writer2.write("ID\t"+ "Audio\t" +"Scenario\t"+
        "Q #\tQuestion\t");// +
    //  writer2.write("Content\t");

      for (int i = 1; i < 7; i++) writer2.write("Answer #" +i+ "\t");
      writer2.write("\n");

      for (Exercise e : rawExercises) {
        int q = 0, qq = 0;
     //   if (e.getID().contains("L0P")) {
          for (Exercise.QAPair qaPair : e.getForeignLanguageQuestions()) {
            q++;
            writeQAPair(writer, e,q, qaPair);
          }
      //  writer2.write(e.getEnglishSentence()+"\t");

        for (Exercise.QAPair qaPair : e.getEnglishQuestions()) {
            String x = e.getID() + "\t" + qaPair.getQuestion() + "\t" + qaPair.getAnswer();
            writer2.write(x+"\n");
                     qq++;
            writeQAPair(writer2, e, qq,qaPair);

          }
        //}
      }
      writer.close();
      writer2.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }*/

/*  private static void writeQAPair(BufferedWriter writer, Exercise e, int q,Exercise.QAPair qaPair) throws IOException {
    writer.write(e.getID() + "\t");
    String content = e.getContent();
    boolean isAudio = content.contains("Listen to this");

    writer.write(isAudio ? "Yes\t" :"No\t");

    String[] split = content.split("Question Scenario");
    if (split.length > 1) {
      String s = split[1];
      String[] split1 = s.split("<" +
        HEADER_TAG +
        " dir=\"rtl\">");

      String s1 = split1[1];
      String s2 = s1.split("</" +
        HEADER_TAG +
        ">")[0];
      //logger.warn("s2 " +s2);
      writer.write(s2 + "\t");
    }

    // String x = e.getID() + "\t" + qaPair.getQuestion() + "\t" + qaPair.getAnswer();
    writer.write(q+ "\t");
    writer.write(qaPair.getQuestion()+ "\t");
    List<String> alternateAnswers = qaPair.getAlternateAnswers();
    ListIterator<String> answerIterator = alternateAnswers.listIterator();
    while (answerIterator.hasNext()) {
      writer.write(answerIterator.next());
      if (answerIterator.hasNext()) writer.write("\t");
    }
    writer.write("\n");
  }*/

  public static void main(String [] arg) {


   // final String configDir = getConfigDir("pilot");

/*    DatabaseImpl unitAndChapter = new DatabaseImpl(
      configDir,
      "arabicText",
      configDir +
        spreadsheet);*/

    String configDir = "config" +
      File.separator +
      "pilot";
    SQLExerciseDAO sqlExerciseDAO = new SQLExerciseDAO(new SmallDatabaseImpl("war/config/pilot/avpDemo"), configDir, "war"+File.separator+configDir);
    sqlExerciseDAO.getSectionHelper().getSectionNodes();
  //  dumpQuestionsAndAnswers(sqlExerciseDAO);
  }
}