package mitll.langtest.server.database.custom;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.JSONExerciseDAO;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.shared.exercise.CommonExercise;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by go22670 on 2/9/16.
 */
public class JSONExportTest {
  private static final String ENGLISH = "english";
  private static final Logger logger = Logger.getLogger(JSONExportTest.class);
  private static DatabaseImpl database;
  //private static String dbName;

  @BeforeClass
  public static void setup() {
    logger.debug("setup called");

    String config = "spanish";//"mandarin";
    // dbName = "npfSpanish";//"mandarin";// "mandarin";

    getDatabase(config, "npfSpanish");
  }

  private static void getDatabase(String config, String dbName) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    logger.debug("config dir " + parent);
    logger.debug("config     " + file.getName());
    //  dbName = "npfEnglish";//"mandarin";// "mandarin";
    database = new DatabaseImpl(parent, file.getName(), dbName, new ServerProperties(parent, file.getName()), new PathHelper("war"), false, null);
    logger.debug("made " + database);
    String media = parent + File.separator + "media";
    logger.debug("media " + media);
    database.setInstallPath(".", parent + File.separator + database.getServerProps().getLessonPlan(), "media");
    Collection<CommonExercise> exercises = database.getExercises();
  }

  @Test
  public void testExport() {
    JsonExport jsonExport = new JsonExport(null, database.getSectionHelper(), null);
    JSONArray exercisesAsJson = jsonExport.getExercisesAsJson(database.getExercises());
    logger.info("got " + exercisesAsJson);

    JSONObject jsonObject = new JSONObject();
    jsonExport.addJSONExerciseExport(jsonObject, database.getExercises());

    logger.info("got " + jsonObject);
  }

  @Test
  public void testExport2() {
    JsonExport jsonExport = new JsonExport(null, database.getSectionHelper(), null);
    JSONObject jsonObject = new JSONObject();
    jsonExport.addJSONExerciseExport(jsonObject, database.getExercises());
    logger.info("got " + jsonObject);

  }

  @Test
  public void testExport3() {
    JsonExport jsonExport = new JsonExport(null, database.getSectionHelper(), null);
    JSONObject jsonObject = new JSONObject();
    jsonExport.addJSONExerciseExport(jsonObject, database.getExercises());
    logger.info("got " + jsonObject);
    String s = jsonObject.toString();


    Collection<CommonExercise> exercises = jsonExport.getExercises(s);
  }

  @Test
  public void testExport4() {
    JsonExport jsonExport = new JsonExport(null, database.getSectionHelper(), null);
    Collection<CommonExercise> exercises = database.getExercises();
    List<CommonExercise> copy = new ArrayList<>(exercises);
    List<CommonExercise> exercises1 = copy.subList(0, 10);

    JSONObject jsonObject = new JSONObject();
    jsonExport.addJSONExerciseExport(jsonObject, exercises1);
    logger.info("got " + jsonObject);
    String text = jsonObject.toString();

    try {
      String pathname = "spanishSmall.json";
      File file = new File(pathname);
      logger.debug("writing to  " + file.getAbsolutePath());
      try (PrintWriter out = new PrintWriter(file)) {
        out.println(text);
        out.flush();
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }


  @Test
  public void testExportSmall() {
    JsonExport jsonExport = new JsonExport(null, database.getSectionHelper(), null);
    Collection<CommonExercise> exercises = database.getExercises();
    List<CommonExercise> copy = new ArrayList<>(exercises);
    List<CommonExercise> exercises1 = copy;//.subList(0, 4000);

    JSONObject jsonObject = new JSONObject();
    jsonExport.addJSONExerciseExport(jsonObject, exercises1);
    logger.info("got " + jsonObject);
    String text = jsonObject.toString();

    String pathname = "spanish.json";
    File file = new File(pathname);
    logger.debug("writing to  " + file.getAbsolutePath());

    writeToFile(text, file);
  }

  void writeToFile(String text, File file) {
    try {

      String[] split = text.split("\\},\\{");
      try (PrintWriter out = new PrintWriter(file)) {
        for (int i = 0; i < split.length;i++) {
          String s = split[i];
          out.print(s);

          if (i < split.length-1) {
            out.println("},");
            out.print("{");
          }
        }
        out.flush();
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testExport6() {
    JsonExport jsonExport = new JsonExport(null, database.getSectionHelper(), null);
    Collection<CommonExercise> exercises = database.getExercises();
    //List<CommonExercise> copy = new ArrayList<>(exercises);
    //List<CommonExercise> exercises1 = copy.subList(0, 10);

    JSONObject jsonObject = new JSONObject();
    jsonExport.addJSONExerciseExport(jsonObject, exercises);
    logger.info("got " + jsonObject);
    String text = jsonObject.toString();
    String[] split = text.split("\\},\\{");

    try {
      String pathname = "spanish2.json";
      File file = new File(pathname);
      logger.debug("writing to  " + file.getAbsolutePath());
      try (PrintWriter out = new PrintWriter(file)) {
        out.println(text);
        out.flush();
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
  @Test
  public void testExport5() {

    Collection<CommonExercise> exercises = database.getExercises();
    logger.info("Got " + exercises.size());
  }

  @Test
  public void testImport() {
    String file = "spanish.json";
    JSONExerciseDAO exerciseDAO = new JSONExerciseDAO(file, database.getServerProps(), database.getUserListManager(), true);

    database.setDependencies("bestAudio", ".", exerciseDAO);

    List<CommonExercise> rawExercises = exerciseDAO.getRawExercises();

    logger.info("Got " + rawExercises.size());

    //for (CommonExercise exercise : rawExercises) logger.debug(exercise);
  }
}
