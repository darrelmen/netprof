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

package mitll.langtest.server.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AMASReaderTest {
  private static final Logger logger = LogManager.getLogger(AMASReaderTest.class);
  // public static final String AM_LB_002 = "AM-LB-002";
 /* private static DatabaseImpl database;

  @BeforeClass
  public static void setup() {
    getDatabase("amasMSA");
  }

  private static void getDatabase(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "amas.properties");
    String parent = file.getParent();
    logger.debug("config dir " + parent);
    logger.debug("config     " + file.getName());
    ServerProperties serverProps = new ServerProperties(parent, file.getName());
    String dbName = serverProps.getH2Database();
    database = new DatabaseImpl(parent, file.getName(), dbName, serverProps, new PathHelper("war", serverProps), false, null, true);
    logger.debug("made " + database);
    database.setInstallPath("war", parent + File.separator + database.getServerProps().getLessonPlan());
  }

  @Test
  public void testReport() {
    database.doReportForYear(new PathHelper("war", null), "", 2016);
  }

  @Test
  public void testMe() {
    //database.getAMASSectionHelper().report();
    Collection<AmasExerciseImpl> exercises = database.getAMASExercises();
    Stream<AmasExerciseImpl> amasExerciseStream = exercises.stream().filter(ex -> ex.getOldID().equals("AM-LA-004"));
    Optional<AmasExerciseImpl> first = amasExerciseStream.findFirst();
    AmasExerciseImpl amasExercise = first.get();
    logger.info("first " + first + " audio '" + amasExercise.getAudioURL() + "'");
    QAPair next1 = amasExercise.getForeignLanguageQuestions().iterator().next();
    logger.info("q " + next1);
    AmasExerciseImpl next = exercises.iterator().next();
    logger.info("e.g. " + next);
    logger.info("\n\ngot " + exercises.size());

    ServerProperties serverProps = database.getServerProps();
    AudioFileHelper audioFileHelper = new AudioFileHelper(new PathHelper("war", serverProps), serverProps, database, null, null);

    audioFileHelper.makeAutoCRT(".");

    database.getAMASSectionHelper().report();
  }*/
}
