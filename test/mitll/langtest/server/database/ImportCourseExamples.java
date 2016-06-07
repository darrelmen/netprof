package mitll.langtest.server.database;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/6/2014.
 */
public class ImportCourseExamples {
  private static final Logger logger = Logger.getLogger(ImportCourseExamples.class);

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

    // add a audio reference to the audio ref table for each recording
    IAudioDAO audioDAO = npfRussian.getAudioDAO();
    //audioDAO.drop();

    // TODO : put these back if we ever need this again
    //  copyAudio(userToResultsRegular, oldToNew, audioDAO, destAudioDir, candidateAudioDir);
  //  copyAudio(userToResultsSlow, oldToNew, audioDAO, destAudioDir, candidateAudioDir);
  }*/

  private static DatabaseImpl makeDatabaseImpl(String h2DatabaseFile, String configDir) {
    ServerProperties serverProps = new ServerProperties(configDir, "quizlet.properties");
    return new DatabaseImpl(configDir, configDir, h2DatabaseFile, serverProps, null, true, null);
  }

  private static void copyUser(DatabaseImpl npfRussian, Map<Integer, User> userMap, Map<Integer, Integer> oldToNew, int userid) {
    User user = userMap.get(userid);
    int i = npfRussian.userExists(user.getUserID());
    if (i > 0) logger.debug("found duplicate " + user);
    int l = i != -1 ? i : npfRussian.addUser(user);
    oldToNew.put(user.getId(), l);
  }

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
    int count = 0;
    int bad = 0;
    for (Map.Entry<Integer, Map<String, Result>> userToExIdToResult : userToResultsRegular.entrySet()) {
      //for (Long userid : userToExIdToResult.getKey())
      logger.debug("User " +userToExIdToResult.getKey());
      Map<String, Result> exIdToResult = userToExIdToResult.getValue();
      logger.debug("num = " + exIdToResult.size() + " exercises->results ");
      for (Result r : exIdToResult.values()) {
        if (count %  100 == 0) {
          logger.debug("\tcount " + count +
            " result = " + r.getUniqueID() + " for " + r.getCompoundID() + " type " + r.getAudioType() + " path " + r.getAnswer());
        }

        audioDAO.add(r, oldToNew.get(r.getUserid()).intValue(), "bestAudio/" + r.getAnswer());

        try {
          File destFile = new File(destAudioDir, r.getAnswer());
          destFile.getParentFile().mkdirs();
          File srcFile = new File(candidateAudioDir, r.getAnswer());
          if (!srcFile.exists() && bad++ < 20) {
            logger.error("can't find " + srcFile.getAbsolutePath());
          } else {
            FileUtils.copyFile(srcFile, destFile);

            count++;
            if (count % 100 == 0) {
              logger.debug("\tcount " + count + " copied to " + destFile.getAbsolutePath());
            }
          }
        } catch (IOException e) {
          logger.error("got " + e, e);
        }
      }
    }
    logger.debug("copied " + count + " files, found " + bad + " bad src audio paths");
  }*/

/*  public static void main(String []arg){
    //importCourseExamplesJapanese();
   // importCourseExamplesKorean();
    importCourseExamplesLevantine();
  }*/
}
