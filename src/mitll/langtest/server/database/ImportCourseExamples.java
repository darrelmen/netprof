package mitll.langtest.server.database;

/*import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;*/

/**
 * Created by GO22670 on 5/6/2014.
 */
public class ImportCourseExamples {
/*  private static final Logger logger = Logger.getLogger(ImportCourseExamples.class);

  private static DatabaseImpl makeDatabaseImpl(String h2DatabaseFile, String configDir) {
    ServerProperties serverProps = new ServerProperties(configDir,"quizlet.properties");
    return new DatabaseImpl(configDir, configDir, h2DatabaseFile, serverProps, null, true);
  }

  protected static void importCourseExamples() {
    DatabaseImpl russianCourseExamples = makeDatabaseImpl("russianCourseExamples_04_16", "war/config/russian");
    ResultDAO resultDAO1 = russianCourseExamples.getResultDAO();
    System.out.println("got " + resultDAO1.getNumResults());
    Map<Long, Map<String, Result>> userToResultsRegular = resultDAO1.getUserToResults(true, russianCourseExamples.getUserDAO());
    System.out.println("got " + userToResultsRegular.size() + " keys " + userToResultsRegular.keySet());

    //  System.out.println("got " + result);

    Map<Long, Map<String, Result>> userToResultsSlow = resultDAO1.getUserToResults(false, russianCourseExamples.getUserDAO());
    System.out.println("got " + userToResultsSlow.size() + " keys " + userToResultsSlow.keySet());

    // so now we have the latest audio
    // write to id/regular_or_slow/user_id

    // copy users to real database

    DatabaseImpl npfRussian = makeDatabaseImpl("npfRussian", "war/config/russian");
    Map<Long, User> userMap = russianCourseExamples.getUserDAO().getUserMap();
    Map<Long, Long> oldToNew = new HashMap<Long, Long>();

    for (long userid : userToResultsRegular.keySet()) {
      copyUser(npfRussian, userMap, oldToNew, userid);
    }

    for (long userid : userToResultsSlow.keySet()) {
      copyUser(npfRussian, userMap, oldToNew, userid);
    }

    // so now we have the users in the database

    // add a audio reference to the audio ref table for each recording
    AudioDAO audioDAO = npfRussian.getAudioDAO();
    // audioDAO.drop();
    copyAudio(userToResultsRegular, oldToNew, audioDAO);
    copyAudio(userToResultsSlow, oldToNew, audioDAO);
  }

  private static void copyUser(DatabaseImpl npfRussian, Map<Long, User> userMap, Map<Long, Long> oldToNew, long userid) {
    User user = userMap.get(userid);
    int i = npfRussian.userExists(user.getUserID());
    if (i > 0) logger.debug("found duplicate " + user);
    long l = i != -1 ? i : npfRussian.addUser(user);
    oldToNew.put(user.getId(), l);
  }

  *//**
   * TODO : deal with the user ids being the same after toLowerCase
   * @param userToResultsRegular
   * @param oldToNew
   * @param audioDAO
   *//*
  protected static void copyAudio(Map<Long, Map<String, Result>> userToResultsRegular, Map<Long, Long> oldToNew, AudioDAO audioDAO) {
    int count = 0;
    for (Map.Entry<Long, Map<String, Result>> userToExIdToResult : userToResultsRegular.entrySet()) {
      //for (Long userid : userToExIdToResult.getKey())
      logger.debug("User " +userToExIdToResult.getKey());
      Map<String, Result> exIdToResult = userToExIdToResult.getValue();
      logger.debug("num = " + exIdToResult.size() + " exercises->results ");
      for (Result r : exIdToResult.values()) {
        if (count %  100 == 0) {
          logger.debug("\tcount " + count +
            " result = " + r.uniqueID + " for " + r.getID() + " type " + r.getAudioType() + " path " + r.answer);
        }

        audioDAO.add(r, oldToNew.get(r.userid).intValue(), "bestAudio/" + r.answer);

        try {
          File destFile = new File("war/config/russian/bestAudio", r.answer);
          destFile.getParentFile().mkdirs();
          FileUtils.copyFile(new File("war/config/russian/candidateAudio", r.answer), destFile);
          count++;
          if (count % 100 == 0)  {
            logger.debug("\tcount " + count + " copied to " + destFile.getAbsolutePath());
          }
        } catch (IOException e) {
          logger.error("got " + e, e);
        }
      }
    }
    logger.debug("copied " + count + " files.");
  }*/
}
