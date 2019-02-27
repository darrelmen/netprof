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
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;

import java.io.File;

public class AnalysisAgainTest {
  private static final Logger logger = LogManager.getLogger(AnalysisAgainTest.class);
  private static DatabaseImpl database;

  @BeforeClass
  public static void setup() {
    getDatabase("sudanese");
  }

  private static void getDatabase(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParent();
    logger.debug("config dir " + parent);
    logger.debug("config     " + file.getName());
    //  dbName = "npfEnglish";//"mandarin";// "mandarin";
    ServerProperties serverProps = new ServerProperties(parent, file.getName());
    String dbName = serverProps.getH2Database();
    database = new DatabaseImpl(serverProps, new PathHelper("war", serverProps), null, null);
    logger.debug("made " + database);
    database.setInstallPath(parent + File.separator + database.getServerProps().getLessonPlan());
    database.getExercises(-1, false);
  }

/*
  @Test
  public void testMe() {
    database.getSectionHelper(-1);

    UserPerformance performance = database.getAnalysis(-1).getPerformanceForUser(1, 1);
    logger.info("got " + performance);

    List<WordScore> wordScoresForUser = database.getAnalysis(-1).getWordScoresForUser(1, 1);
    for (WordScore ws : wordScoresForUser) {
      //if (ws.getExID().equals("50246"))
      logger.info("ws " +ws.getId() + " " + ws.getNativeAudio() + " " + ws.getFileRef());
    }

  }
*/

}
