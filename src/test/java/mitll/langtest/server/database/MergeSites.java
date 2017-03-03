package mitll.langtest.server.database;

import org.apache.logging.log4j.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/6/2014.
 */
public class MergeSites extends BaseTest {
  private static final Logger logger = LogManager.getLogger(MergeSites.class);

  /**
   * Take the users from the current site and addExerciseToList them to the candidate site
   * Add Result table entries from current site to new site - fixing the user id references for the result entries.
   * <p>
   * Map old exercise id 900 to new 12887
   */
/*  @Test
  public void testMerge() {
    DatabaseImpl egyptianCurrent = getDatabase("egyptian");
    DatabaseImpl candidate = getDatabase("egyptianCandidate");


    List<User> oldUsers = egyptianCurrent.getUsers();

    Map<String, User> chosenToUser = new HashMap<>();
    for (User u : oldUsers) chosenToUser.put(u.getUserID(), u);

    List<User> candidateUsers = candidate.getUsers();
    Map<Integer, Integer> oldToNew = new HashMap<>();
    Set<String> candidateIDs = new HashSet<>();
    for (User newUser : candidateUsers) {
      String userID = newUser.getUserID();
      candidateIDs.addExerciseToList(userID);
      if (!userID.isEmpty()) {
        if (chosenToUser.containsKey(userID)) {
          User oldUser = chosenToUser.get(userID);
          if (oldUser.getID() != newUser.getID()) {
            oldToNew.put(oldUser.getID(), newUser.getID());
            logger.info("got existing user new\n\t" + newUser + "old\n\t" + oldUser);
          }
        }
      }
    }

    int c = 0;
    for (User old : oldUsers) {
      if (!candidateIDs.contains(old.getUserID())) {
        int l1 = candidate.addUser(old);
        if (l1 < 0) logger.warn("huh - couldn't addExerciseToList " + old);
        else {
          logger.info("Adding " + old);
          oldToNew.put(old.getID(), l1);
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
      knownFiles.addExerciseToList(r.getAnswer());
    }

    int n = 0;
    int e = 0;
    int before = candidate.getResultDAO().getNumResults();
    Set<Integer> unk = new HashSet<>();

    Map<Integer, User> userMap = candidate.getUserDAO().getUserMap();
    for (MonitorResult oldR : oldResults) {
      if (!knownFiles.contains(oldR.getAnswer())) {
        n++;
        if (n < 10) logger.info("addExerciseToList " + oldR.getAnswer());
        Integer oldUserID = oldR.getUserid();
        Integer newUserID = oldToNew.get(oldUserID);

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
            unk.addExerciseToList(oldUserID);
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
    logger.info("unknown " + new TreeSet<>(unk));
    logger.info("added " + n);
  }*/

/*  private static void importExamples(String configDir, String importH2, String destinationH2, String destAudioDir, String candidateAudioDir) {
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
//    AudioDAO audioDAO = npfRussian.getAudioDAO();
//    //audioDAO.drop();
//    copyAudio(userToResultsRegular, oldToNew, audioDAO, destAudioDir, candidateAudioDir);
//    copyAudio(userToResultsSlow, oldToNew, audioDAO, destAudioDir, candidateAudioDir);
  }*/

/*  private static DatabaseImpl makeDatabaseImpl(String h2DatabaseFile, String configDir) {
    ServerProperties serverProps = new ServerProperties(configDir, "quizlet.properties");
    return new DatabaseImpl(configDir, configDir, h2DatabaseFile, serverProps, null, true, null, true);
  }

  private static void copyUser(DatabaseImpl npfRussian, Map<Integer, User> userMap, Map<Integer, Integer> oldToNew, int userid) {
    User user = userMap.get(userid);
    int i = npfRussian.userExists(user.getUserID());
    if (i > 0) logger.debug("found duplicate " + user);
    int l = i != -1 ? i : npfRussian.addUser(user);
    oldToNew.put(user.getID(), l);
  }*/

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
/*
  private static void copyAudio(Map<Integer, Map<String, Result>> userToResultsRegular, Map<Integer, Integer> oldToNew,
                                AudioDAO audioDAO,
                                String destAudioDir, String candidateAudioDir) {
    int count = 0;
    int bad = 0;
    for (Map.Entry<Integer, Map<String, Result>> userToExIdToResult : userToResultsRegular.entrySet()) {
      //for (Long userid : userToExIdToResult.getKey())
      logger.debug("User " + userToExIdToResult.getKey());
      Map<String, Result> exIdToResult = userToExIdToResult.getValue();
      logger.debug("num = " + exIdToResult.size() + " exercises->results ");
      for (Result r : exIdToResult.values()) {
        if (count % 100 == 0) {
          logger.debug("\tcount " + count +
              " result = " + r.getUniqueID() + " type " + r.getAudioType() + " path " + r.getAnswer());
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
}
