/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.custom;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ScoreServlet;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;

public class JSONExportTest extends JsonExport {
  private static final Logger logger = LogManager.getLogger(JSONExportTest.class);
  private static DatabaseImpl database;

  /**
   * @paramx phoneToCount
   * @paramx sectionHelper
   * @paramx preferredVoices
   * @see ScoreServlet#getJsonNestedChapters(boolean)
   */
  public JSONExportTest() {
    super(null, null, false, null);
  }
  //private static String dbName;

  @BeforeClass
  public static void setup() {
    getDatabase("mandarin");
  }

  private static void getDatabase(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    logger.debug("config dir " + parent);
    logger.debug("config     " + file.getName());
    //  dbName = "npfEnglish";//"mandarin";// "mandarin";
    ServerProperties serverProps = new ServerProperties(parent, file.getName());
    database = new DatabaseImpl(serverProps, new PathHelper("war", serverProps), null, null);
    logger.debug("made " + database);
    String media = parent + File.separator + "media";
    logger.debug("media " + media);
    database.setInstallPath(parent + File.separator + database.getServerProps().getLessonPlan(), null, true);
    database.getExercises(-1, false);
  }

  @Test
  public void testRead() {
    database.getSectionHelper();

    CommonExercise exercise = database.getExercise(1,724);
    logger.warn("\n\ntestRead got " + exercise);
  }
/*


  @Test
  public void testExport() {
    JsonExport jsonExport = new JsonExport(null, database.getSectionHelper(), null, false);
    JSONArray exercisesAsJson = jsonExport.getExercisesAsJson(database.getExercises(-1));
    logger.info("got " + exercisesAsJson);

    JSONObject jsonObject = new JSONObject();
    jsonExport.addJSONExerciseExport(jsonObject, database.getExercises(-1));

    logger.info("got " + jsonObject);
  }
*/

/*  @Test
  public void testExport2() {
    JsonExport jsonExport = new JsonExport(null, database.getSectionHelper(), null, false);
    JSONObject jsonObject = new JSONObject();
    jsonExport.addJSONExerciseExport(jsonObject, database.getExercises(-1));
    logger.info("got " + jsonObject);

  }*/

 /* @Test
  public void testExport3() {
    JsonExport jsonExport = new JsonExport(null, database.getSectionHelper(), null, false);
    JSONObject jsonObject = new JSONObject();
    jsonExport.addJSONExerciseExport(jsonObject, database.getExercises(-1));
    //logger.info("got " + jsonObject);
    String s = jsonObject.toString();


    Collection<CommonExercise> exercises = jsonExport.getExercises(s);
  }*/

/*  @Test
  public void testExport4() {
    JsonExport jsonExport = new JsonExport(null, database.getSectionHelper(), null, false);
    Collection<CommonExercise> exercises = database.getExercises(-1);
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
  }*/

/*  @Test
  public void testExportSpanish() {
    exportTo("spanishNew.json");
  }

  @Test
  public void testExportMandarin() {
    exportTo("mandarin.json");
  }*/

/*  void exportTo(String pathname) {
    JsonExport jsonExport = new JsonExport(null, database.getSectionHelper(), null, false);
    Collection<CommonExercise> exercises = database.getExercises(-1);
    List<CommonExercise> copy = new ArrayList<>(exercises);
    List<CommonExercise> exercises1 = copy;//.subList(0, 4000);excel

    JSONObject jsonObject = new JSONObject();
    jsonExport.addJSONExerciseExport(jsonObject, exercises1);
    //   logger.info("got " + jsonObject);
    String text = jsonObject.toString();

    File file = new File(pathname);
    logger.debug("writing to  " + file.getAbsolutePath());

    writeToFile(text, file);
  }*/

  void writeToFile(String text, File file) {
    try {

      String[] split = text.split("\\},\\{");
      try (PrintWriter out = new PrintWriter(file)) {
        for (int i = 0; i < split.length; i++) {
          String s = split[i];
          out.print(s);

          if (i < split.length - 1) {
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
/*
  @Test
  public void testExport6() {
    JsonExport jsonExport = new JsonExport(null, database.getSectionHelper(), null, false);
    Collection<CommonExercise> exercises = database.getExercises(-1);
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
  }*/

  @Test
  public void testExport5() {
    Collection<CommonExercise> exercises = database.getExercises(-1, false);
    logger.info("Got " + exercises.size());
  }
//
//  @Test
//  public void testImport() {
//    String file = "spanish.json";
//    JSONExerciseDAO exerciseDAO = new JSONExerciseDAO(file, database.getServerProps(), database.getUserListManager(), true);
//
//    database.setDependencies("bestAudio", ".", exerciseDAO, projid);
//
//    List<CommonExercise> rawExercises = exerciseDAO.getRawExercises();
//
//    logger.info("Got " + rawExercises.size());
//    for (SectionNode node : exerciseDAO.getSectionHelper().getSectionNodesForTypes()) {
//      Collection<CommonExercise> exercisesForSelectionState = exerciseDAO.getSectionHelper().getExercisesForSelectionState(node.getProperty(), node.getName());
//      logger.info("for " + node + " got " + exercisesForSelectionState.size());
//    }
//    //for (CommonExercise exercise : rawExercises) logger.debug(exercise);
//  }
}
