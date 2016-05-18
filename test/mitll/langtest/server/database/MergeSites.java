package mitll.langtest.server.database;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.MonitorResult;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import mitll.langtest.shared.exercise.CommonExercise;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/6/2014.
 */
public class MergeSites extends BaseTest {
  private static final Logger logger = Logger.getLogger(MergeSites.class);

  /**
   * Take the users from the current site and add them to the candidate site
   * Add Result table entries from current site to new site - fixing the user id references for the result entries.
   * <p>
   * Map old exercise id 900 to new 12887
   */
  @Test
  public void testMerge() {
    DatabaseImpl egyptianCurrent = getDatabase("egyptian");
    DatabaseImpl<CommonExercise> candidate = getDatabase("egyptianCandidate");


    List<User> oldUsers = egyptianCurrent.getUsers();

    Map<String, User> chosenToUser = new HashMap<>();
    for (User u : oldUsers) chosenToUser.put(u.getUserID(), u);

    List<User> candidateUsers = candidate.getUsers();
    Map<Long, Long> oldToNew = new HashMap<>();
    Set<String> candidateIDs = new HashSet<>();
    for (User newUser : candidateUsers) {
      String userID = newUser.getUserID();
      candidateIDs.add(userID);
      if (!userID.isEmpty()) {
        if (chosenToUser.containsKey(userID)) {
          User oldUser = chosenToUser.get(userID);
          if (oldUser.getId() != newUser.getId()) {
            oldToNew.put(oldUser.getId(), newUser.getId());
            logger.info("got existing user new\n\t" + newUser + "old\n\t" + oldUser);
          }
        }
      }
    }

    int c = 0;
    for (User old : oldUsers) {
      if (!candidateIDs.contains(old.getUserID())) {
        long l1 = candidate.addUser(old);
        if (l1 < 0) logger.warn("huh - couldn't add " + old);
        else {
          logger.info("Adding " + old);
          oldToNew.put(old.getId(), l1);
          c++;
        }
      }
    }
    logger.info("added " + c + " old users to candidate");

    List<MonitorResult> oldResults = egyptianCurrent.getResultDAO().getMonitorResults();
    List<Result> newResults = candidate.getResultDAO().getResults();

    Set<String> knownFiles = new HashSet<>();
    List<Result> toAdd = new ArrayList<>();
    for (Result r : newResults) {
      knownFiles.add(r.getAnswer());
    }

    int n = 0;
    int e = 0;
    int before = candidate.getResultDAO().getNumResults();
    Set<Long> unk = new HashSet<>();

    Map<Long, User> userMap = candidate.getUserDAO().getUserMap();
    for (MonitorResult oldR : oldResults) {
      if (!knownFiles.contains(oldR.getAnswer())) {
        n++;
        if (n < 10) logger.info("add " + oldR.getAnswer());
        long oldUserID = oldR.getUserid();
        Long newUserID = oldToNew.get(oldUserID);

        if (newUserID == null) {
          if (userMap.containsKey(oldUserID)) {
            try {
              candidate.getAnswerDAO().addResultToTable(oldR);
            } catch (SQLException e1) {
              e1.printStackTrace();
            }
          }
          else {
            e++;
            logger.error("huh? can't find user id " + oldUserID);
            unk.add(oldUserID);
          }
        } else {
        //  oldR.setUserID(newUserID);
          //if (n < 10)
            logger.warn("adding with fixed id " + oldR);

          try {
            candidate.getAnswerDAO().addResultToTable(oldR);
          } catch (SQLException e1) {
            e1.printStackTrace();
          }

        }
      }
    }
    int after = candidate.getResultDAO().getNumResults();
    logger.info("before " + before + " after " + after);
    logger.info("unknown " + new TreeSet<Long>(unk));
    logger.info("added " + n);
  }

  private static void importExamples(String configDir, String importH2, String destinationH2, String destAudioDir, String candidateAudioDir) {
    DatabaseImpl courseExamples = makeDatabaseImpl(importH2, configDir);
    ResultDAO resultDAO1 = courseExamples.getResultDAO();
    System.out.println("got num results " + resultDAO1.getNumResults());
    Map<Long, Map<String, Result>> userToResultsRegular = resultDAO1.getUserToResults(true, courseExamples.getUserDAO());
    //  Map<Long, Map<String, Result>> userToResultsRegular = resultDAO1.getUserToResults(Result.AUDIO_TYPE_FAST_AND_SLOW, courseExamples.getUserDAO());
    System.out.println("regular speed got users " + userToResultsRegular.size() + " keys " + userToResultsRegular.keySet());

    //  System.out.println("got " + result);

    Map<Long, Map<String, Result>> userToResultsSlow = resultDAO1.getUserToResults(false, courseExamples.getUserDAO());
    System.out.println("slow speed    got users " + userToResultsSlow.size() + " keys " + userToResultsSlow.keySet());

    // so now we have the latest audio
    // write to id/regular_or_slow/user_id

    // copy users to real database

    DatabaseImpl npfRussian = makeDatabaseImpl(destinationH2, configDir);
    Map<Long, User> userMap = courseExamples.getUserDAO().getUserMap();
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
    //audioDAO.drop();
    copyAudio(userToResultsRegular, oldToNew, audioDAO, destAudioDir, candidateAudioDir);
    copyAudio(userToResultsSlow, oldToNew, audioDAO, destAudioDir, candidateAudioDir);
  }

  private static DatabaseImpl makeDatabaseImpl(String h2DatabaseFile, String configDir) {
    ServerProperties serverProps = new ServerProperties(configDir, "quizlet.properties");
    return new DatabaseImpl(configDir, configDir, h2DatabaseFile, serverProps, null, true, null);
  }

  private static void copyUser(DatabaseImpl npfRussian, Map<Long, User> userMap, Map<Long, Long> oldToNew, long userid) {
    User user = userMap.get(userid);
    int i = npfRussian.userExists(user.getUserID());
    if (i > 0) logger.debug("found duplicate " + user);
    long l = i != -1 ? i : npfRussian.addUser(user);
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

  private static void copyAudio(Map<Long, Map<String, Result>> userToResultsRegular, Map<Long, Long> oldToNew,
                                AudioDAO audioDAO,
                                String destAudioDir, String candidateAudioDir) {
    int count = 0;
    int bad = 0;
    for (Map.Entry<Long, Map<String, Result>> userToExIdToResult : userToResultsRegular.entrySet()) {
      //for (Long userid : userToExIdToResult.getKey())
      logger.debug("User " + userToExIdToResult.getKey());
      Map<String, Result> exIdToResult = userToExIdToResult.getValue();
      logger.debug("num = " + exIdToResult.size() + " exercises->results ");
      for (Result r : exIdToResult.values()) {
        if (count % 100 == 0) {
          logger.debug("\tcount " + count +
              " result = " + r.getUniqueID() + " for " + r.getID() + " type " + r.getAudioType() + " path " + r.getAnswer());
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
  }
}
