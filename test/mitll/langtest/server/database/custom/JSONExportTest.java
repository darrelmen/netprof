package mitll.langtest.server.database.custom;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.shared.exercise.CommonExercise;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Collection;

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
    logger.info("got " +exercisesAsJson);

    JSONObject jsonObject = new JSONObject();
    jsonExport.addJSONExerciseExport(jsonObject,database.getExercises());

    logger.info("got " +jsonObject);
  }

  @Test
  public void testExport2() {
    JsonExport jsonExport = new JsonExport(null, database.getSectionHelper(), null);
    JSONObject jsonObject = new JSONObject();
    jsonExport.addJSONExerciseExport(jsonObject,database.getExercises());
    logger.info("got " +jsonObject);

  }
  @Test
  public void testExport3() {
    JsonExport jsonExport = new JsonExport(null, database.getSectionHelper(), null);
    JSONObject jsonObject = new JSONObject();
    jsonExport.addJSONExerciseExport(jsonObject,database.getExercises());
    logger.info("got " +jsonObject);
    String s = jsonObject.toString();
    Collection<CommonExercise> exercises = jsonExport.getExercises(s);
  }
}
