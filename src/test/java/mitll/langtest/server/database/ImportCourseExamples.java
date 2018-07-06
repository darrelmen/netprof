/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ImportCourseExamples {
  private static final Logger logger = LogManager.getLogger(ImportCourseExamples.class);

/*  protected static void importCourseExamplesRussian() {
    String configDir = "war/config/russian";
    String importH2 = "russianCourseExamples_04_16";
    String destinationH2 = "npfRussian";
    String destAudioDir = "war/config/russian/bestAudio";
    String candidateAudioDir = "war/config/russian/candidateAudio";

    importExamples(configDir, importH2, destinationH2,destAudioDir,candidateAudioDir);
  }

  protected static void importCourseExamplesJapanese() {
    String language = "japanese";
    String destinationH2 = "npfJapanese";
    importExamples(language, destinationH2);
  }

  protected static void importCourseExamplesKorean() {
    String language = "korean";
    String destinationH2 = "npfKorean";
    importExamples(language, destinationH2);
  }*/

  /*protected static void importCourseExamplesLevantine() {
    String language = "levantine";
    String destinationH2 = "npfLevantine";
    importExamples(language, destinationH2);
  }
*/
/*  private static void importExamples(String language, String destinationH2) {
    String configDir = "war/config/" + language;
    String importH2 = language;
    String destAudioDir = "war/config/" +language+ "/bestAudio";
    String candidateAudioDir = "war/config/" +language+ "/candidateAudio";

    importExamples(configDir, importH2, destinationH2,destAudioDir,candidateAudioDir);
  }

  private static void importExamples(String configDir, String importH2, String destinationH2, String destAudioDir, String candidateAudioDir) {
    DatabaseImpl courseExamples = makeDatabaseImpl(importH2, configDir);
    IResultDAO resultDAO1 = courseExamples.getResultDAO();
    System.out.println("got num results " + resultDAO1.getNumResults());
    Map<Integer, Map<String, Result>> userToResultsRegular = resultDAO1.getUserToResults(true, courseExamples.getUserDAO());
  //  Map<Integer, Map<String, Result>> userToResultsRegular = resultDAO1.getUserToResults(Result.AUDIO_TYPE_FAST_AND_SLOW, courseExamples.getUserDAO());
    System.out.println("regular speed got users " + userToResultsRegular.size() + " keys " + userToResultsRegular.keySet());

    //  System.out.println("got " + result);

    Map<Integer, Map<String, Result>> userToResultsSlow = resultDAO1.getUserToResults(false, courseExamples.getUserDAO());
    System.out.println("slow speed    got users " + userToResultsSlow.size() + " keys " + userToResultsSlow.keySet());

    // so now we have the latest audio
    // write to id/regular_or_slow/user_id

    // copy users to real database

    DatabaseImpl npfRussian = makeDatabaseImpl(destinationH2, configDir);
    Map<Integer, User> userMap = courseExamples.getUserDAO().getUserMap();
    Map<Integer, Integer> oldToNew = new HashMap<Integer, Integer>();

    for (int userid : userToResultsRegular.keySet()) {
      copyUser(npfRussian, userMap, oldToNew, userid);
    }

    for (int userid : userToResultsSlow.keySet()) {
      copyUser(npfRussian, userMap, oldToNew, userid);
    }

    // so now we have the users in the database

    // addExerciseToList a audio reference to the audio ref table for each recording
    IAudioDAO audioDAO = npfRussian.getAudioDAO();
    //audioDAO.drop();

    // TODO : put these back if we ever need this again
    //  copyAudio(userToResultsRegular, oldToNew, audioDAO, destAudioDir, candidateAudioDir);
  //  copyAudio(userToResultsSlow, oldToNew, audioDAO, destAudioDir, candidateAudioDir);
  }*/

/*  private static DatabaseImpl makeDatabaseImpl(String h2DatabaseFile, String configDir) {
    ServerProperties serverProps = new ServerProperties(configDir, "quizlet.properties");
    return new DatabaseImpl(serverProps, null, null, servletContext);
  }*/

/*
  private static void copyUser(DatabaseImpl npfRussian, Map<Integer, User> userMap, Map<Integer, Integer> oldToNew, int userid) {
    User user = userMap.get(userid);
    int i = npfRussian.userExists(user.getUserID());
    if (i > 0) logger.debug("found duplicate " + user);
    int l = i != -1 ? i : npfRussian.addUser(user);
    oldToNew.put(user.getID(), l);
  }
*/

  /*
   * TODO : deal with the user ids being the same after toLowerCase
   * @param userToResultsRegular
   * @param oldToNew
   * @param audioDAO
   */
/*
  private static void copyAudio(Map<Long, Map<String, Result>> userToResultsRegular, Map<Long, Long> oldToNew, AudioDAO audioDAO) {
    String destAudioDir = "war/config/russian/bestAudio";
    String candidateAudioDir = "war/config/russian/candidateAudio";

    copyAudio(userToResultsRegular, oldToNew, audioDAO, destAudioDir, candidateAudioDir);
  }
*/

/*  private static void copyAudio(Map<Integer, Map<String, Result>> userToResultsRegular, Map<Integer, Integer> oldToNew,
                                IAudioDAO audioDAO,
                                String destAudioDir, String candidateAudioDir) {
    int childCount = 0;
    int bad = 0;
    for (Map.Entry<Integer, Map<String, Result>> userToExIdToResult : userToResultsRegular.entrySet()) {
      //for (Long userid : userToExIdToResult.getKey())
      logger.debug("User " +userToExIdToResult.getKey());
      Map<String, Result> exIdToResult = userToExIdToResult.getValue();
      logger.debug("num = " + exIdToResult.size() + " exercises->results ");
      for (Result r : exIdToResult.values()) {
        if (childCount %  100 == 0) {
          logger.debug("\tchildCount " + childCount +
            " result = " + r.getUniqueID() + " for " + r.getCompoundID() + " type " + r.getAudioType() + " path " + r.getAnswer());
        }

        audioDAO.addExerciseToList(r, oldToNew.get(r.getUserid()).intValue(), "bestAudio/" + r.getAnswer());

        try {
          File destFile = new File(destAudioDir, r.getAnswer());
          destFile.getParentFile().mkdirs();
          File srcFile = new File(candidateAudioDir, r.getAnswer());
          if (!srcFile.exists() && bad++ < 20) {
            logger.error("can't find " + srcFile.getAbsolutePath());
          } else {
            FileUtils.copyFile(srcFile, destFile);

            childCount++;
            if (childCount % 100 == 0) {
              logger.debug("\tchildCount " + childCount + " copied to " + destFile.getAbsolutePath());
            }
          }
        } catch (IOException e) {
          logger.error("got " + e, e);
        }
      }
    }
    logger.debug("copied " + childCount + " files, found " + bad + " bad src audio paths");
  }*/

/*  public static void main(String []arg){
    //importCourseExamplesJapanese();
   // importCourseExamplesKorean();
    importCourseExamplesLevantine();
  }*/
}
