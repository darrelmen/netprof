package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
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
@SuppressWarnings("ALL")
public class MergeSites extends BaseTest {
  private static final Logger logger = Logger.getLogger(MergeSites.class);

  /**
   * Take the users from the current site and add them to the candidate site
   * Add Result table entries from current site to new site - fixing the user id references for the result entries.
   * <p>
   */
  @Test
  public void testMerge() {
    String current = "egyptian";
    String destination = "egyptianCandidate";

    doMerge(current, destination);
  }

  @Test
  public void testMergeSudanese() {
    String current = "sudanese";
    String destination = "sudaneseEval";
    doMerge(current, destination);
  }

  @Test
  public void testOldData() {
    String current = "sudaneseEval";

    DatabaseImpl<CommonExercise> currentSite = getDatabase(current);
    List<MonitorResult> monitorResults = currentSite.getMonitorResults();

    int i = 10;
    List<MonitorResult> monitorResults1 = monitorResults.subList(monitorResults.size() - i, monitorResults.size());
    for (MonitorResult result : monitorResults) {
      if (result.getExID().equals("1")) {
        logger.info("res " + result.getUniqueID() + " " + result.getExID() + " " + result.getForeignText());
      }
    }
  }

  private void doMerge(String current, String destination) {
    DatabaseImpl<CommonExercise> currentSite = getDatabase(current);
    DatabaseImpl<CommonExercise> destinationSite = getDatabase(destination);

    Map<Long, Long> oldToNewUserIDs = mergeUsers(currentSite, destinationSite);

    try {
      mergeResults(currentSite, destinationSite, oldToNewUserIDs);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void mergeResults(DatabaseImpl<CommonExercise> currentSite, DatabaseImpl<CommonExercise> destinationSite,
                            Map<Long, Long> oldToNewUserIDs) throws SQLException {
    List<MonitorResult> oldResults = currentSite.getResultDAO().getMonitorResults();
    Set<String> knownFiles = getKnownAnswerFiles(destinationSite);
    addOldResultsToDest(currentSite, destinationSite, oldToNewUserIDs, oldResults, knownFiles);
  }

  private Map<String, String> getFLToID(DatabaseImpl<CommonExercise> destinationSite) {
    Map<String, String> newFLToID = new HashMap<>();
    for (CommonExercise exercise : destinationSite.getExercises()) {
      String foreignLanguage = exercise.getForeignLanguage().trim();
      String newID = newFLToID.get(foreignLanguage);
      if (newID == null) {
        newFLToID.put(foreignLanguage, exercise.getID());
      }
    }

    return newFLToID;
  }

  private Map<String, String> getOldIDToFL(DatabaseImpl<CommonExercise> destinationSite) {
    Map<String, String> idToFL = new HashMap<>();
    for (CommonExercise exercise : destinationSite.getExercises()) {
      String foreignLanguage = exercise.getForeignLanguage().trim();
      String id = exercise.getID();
      String newID = idToFL.get(id);
      if (newID == null) {
        idToFL.put(id, foreignLanguage);
      }
    }

    return idToFL;
  }

  private Map<Long, Long> mergeUsers(DatabaseImpl<CommonExercise> currentSite, DatabaseImpl<CommonExercise> destinationSite) {
    Collection<User> oldUsers = currentSite.getUsers();
    Collection<String> candidateIDs = new HashSet<>();

    Map<Long, Long> oldToNewUserIDs = getOldToNewUserIDs(destinationSite, oldUsers, candidateIDs);

    addUsersToDest(destinationSite, oldUsers, candidateIDs, oldToNewUserIDs);
    return oldToNewUserIDs;
  }

  private void addOldResultsToDest(DatabaseImpl<CommonExercise> original,
                                   DatabaseImpl<CommonExercise> destinationSite,
                                   Map<Long, Long> oldToNewUserIDs,
                                   Collection<MonitorResult> oldResults,
                                   Collection<String> knownFiles) throws SQLException {
    int n = 0;
    int e = 0;
    int before = destinationSite.getResultDAO().getNumResults();
    Set<Long> unk = new HashSet<>();

    Map<Long, User> userMap = destinationSite.getUserDAO().getUserMap();

    int exidCount = 0;
    int noExIDMatch = 0;
    Map<String, String> newFLToID = getFLToID(destinationSite);
    Map<String, String> oldIDToFL = getOldIDToFL(original);

    logger.info("fl --> id     " + newFLToID.size() + " e.g. " + newFLToID.entrySet().iterator().next());
    logger.info("old id --> fl " + oldIDToFL.size() + " e.g. " + oldIDToFL.entrySet().iterator().next());
    int fix = 0;
    int nofix = 0;
    for (MonitorResult oldR : oldResults) {
      if (!knownFiles.contains(oldR.getAnswer())) {
        n++;
        if (n < 5) logger.info(n + " add " + oldR.getAnswer());
        long oldUserID = oldR.getUserid();
        Long newUserID = oldToNewUserIDs.get(oldUserID);

        if (newUserID == null) {
          if (userMap.containsKey(oldUserID)) {
            try {
              String foreignText = oldIDToFL.get(oldR.getExID());

              if (foreignText == null || foreignText.isEmpty()) logger.warn("no transcript on " + oldR);
              else {
                foreignText = foreignText.trim();
                String exID = newFLToID.get(foreignText);
                if (exID != null) {
                  oldR.setExID(exID);
                  exidCount++;
                } else {
                  noExIDMatch++;
                }
              }
              if (oldR.getForeignText().isEmpty()) {
                oldR.setForeignText(foreignText);
                fix++;
                if (oldR.getExID().equals("1"))
                  logger.info("fixed " + oldR.getUniqueID() + " : " + oldR.getForeignText());
              } else {
                nofix++;
              }
              destinationSite.getAnswerDAO().addResultToTable(oldR);
            } catch (SQLException e1) {
              e1.printStackTrace();
            }
          } else {
            e++;
            logger.error("huh? can't find user id " + oldUserID);
            unk.add(oldUserID);
          }
        } else {

          String foreignText = oldIDToFL.get(oldR.getExID());

          if (foreignText == null || foreignText.isEmpty()) logger.warn("no transcript on " + oldR);
          else {
            foreignText = foreignText.trim();
            String exID = newFLToID.get(foreignText);
            if (exID != null) {
              oldR.setExID(exID);
              exidCount++;
            } else {
              noExIDMatch++;
            }
          }
          if (oldR.getForeignText().isEmpty()) {
            oldR.setForeignText(foreignText);
            fix++;
            if (oldR.getExID().equals("1"))
              logger.info("fixed " + oldR.getUniqueID() + " : " + oldR.getForeignText());
          } else {
            nofix++;
          }

          destinationSite.getAnswerDAO().addResultToTable(oldR);
        }
      }
    }
    int after = destinationSite.getResultDAO().getNumResults();
    logger.info("dest before " + before + " after " + after);
    if (!unk.isEmpty()) {
      logger.info("unknown users " + new TreeSet<>(unk));
    }
    logger.info("added to dest " + n);
    logger.info("exids copied " + exidCount + " no match " + noExIDMatch);
    logger.info("fix " + fix + " no fix " + nofix);
  }

  private Set<String> getKnownAnswerFiles(DatabaseImpl<CommonExercise> destinationSite) {
    List<Result> newResults = destinationSite.getResultDAO().getResults();

    Set<String> knownFiles = new HashSet<>();
    for (Result r : newResults) {
      knownFiles.add(r.getAnswer());
    }
    return knownFiles;
  }

  private void addUsersToDest(DatabaseImpl<CommonExercise> destinationSite,
                              Collection<User> oldUsers,
                              Collection<String> candidateIDs,
                              Map<Long, Long> oldToNew) {
    int c = 0;
    for (User old : oldUsers) {
      if (!candidateIDs.contains(old.getUserID())) {
        long l1 = destinationSite.addUser(old);
        if (l1 < 0) logger.warn("huh - couldn't add " + old);
        else {
          //  logger.info("Adding " + old);
          oldToNew.put(old.getId(), l1);
          c++;
        }
      }
    }
    logger.info("added " + c + " old users to destinationSite");
    if (false) {
      for (User current : destinationSite.getUserDAO().getUsers()) {
        logger.info("destinationSite user " + current.getUserID() + "\t: " + new Date(current.getTimestampMillis()) + "\t: " + current.getPasswordHash());
      }
    }
  }

  private Map<Long, Long> getOldToNewUserIDs(DatabaseImpl<CommonExercise> destinationSite, Collection<User> oldUsers,
                                             Collection<String> candidateIDs) {
    Map<String, User> chosenToUser = new HashMap<>();
    for (User u : oldUsers) chosenToUser.put(u.getUserID(), u);

    List<User> candidateUsers = destinationSite.getUsers();
    Map<Long, Long> oldToNew = new HashMap<>();
    for (User newUser : candidateUsers) {
      String userID = newUser.getUserID();
      candidateIDs.add(userID);
      if (!userID.isEmpty()) {
        if (chosenToUser.containsKey(userID)) {
          User oldUser = chosenToUser.get(userID);
          if (oldUser.getId() != newUser.getId()) {
            oldToNew.put(oldUser.getId(), newUser.getId());
            //logger.info("got existing user new\n\t" + newUser + "old\n\t" + oldUser);
          }
        }
      }
    }
    return oldToNew;
  }

  private static void importExamples(String configDir, String importH2, String destinationH2, String destAudioDir,
                                     String candidateAudioDir) {
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
    Map<Long, Long> oldToNew = new HashMap<>();

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
    copyAudio(userToResultsRegular, oldToNew, audioDAO, destAudioDir, candidateAudioDir, npfRussian.getPathHelper());
    copyAudio(userToResultsSlow, oldToNew, audioDAO, destAudioDir, candidateAudioDir, npfRussian.getPathHelper());
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
                                String destAudioDir, String candidateAudioDir, PathHelper pathHelper) {
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

        audioDAO.add(r, oldToNew.get(r.getUserid()).intValue(), "bestAudio/" + r.getAnswer(), pathHelper);

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
