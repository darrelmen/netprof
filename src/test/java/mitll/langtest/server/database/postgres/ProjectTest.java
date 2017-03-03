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

package mitll.langtest.server.database.postgres;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.analysis.IAnalysis;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.project.ProjectType;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.userexercise.ExercisePhoneInfo;
import mitll.langtest.server.database.userexercise.ExerciseToPhone;
import mitll.langtest.server.scoring.PrecalcScores;
import mitll.langtest.shared.analysis.UserInfo;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickProject;
import mitll.npdata.dao.SlickProjectProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.*;

public class ProjectTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(ProjectTest.class);
  public static final int MAX = 200;

  @Test
  public void testProject() {
    DatabaseImpl spanish = getDatabase("spanish");
    IProjectDAO projectDAO = spanish.getProjectDAO();
    User gvidaver = spanish.getUserDAO().getUserByID("gvidaver");

    Iterator<String> iterator = spanish.getTypeOrder(-1).iterator();
    projectDAO.add(
        gvidaver.getID(),
        System.currentTimeMillis(),
        "my Spanish",
        spanish.getLanguage(),
        "ALL",
        ProjectType.NP,
        ProjectStatus.PRODUCTION, iterator.next(), iterator.next(), "es", 0);
  }

  @Test
  public void testListProjects() {
    DatabaseImpl spanish = getDatabase("spanish");

    IProjectDAO projectDAO = spanish.getProjectDAO();
    Collection<SlickProject> all = projectDAO.getAll();
    for (SlickProject project : all) {
      logger.info("Got " + project);
      for (SlickProjectProperty prop : project.getProps()) {
        logger.info("\t prop " + prop);
      }
    }
  }

  @Test
  public void testAddProperty() {
    DatabaseImpl spanish = getDatabaseVeryLight("spanish", "quizlet.properties", false);

    IProjectDAO projectDAO = spanish.getProjectDAO();
    SlickProject next = projectDAO.getAll().iterator().next();

    projectDAO.addProperty(next.id(), "key", "value");

    testListProjects();
//    next.addProp(new SlickProjectProperty(-1, new Timestamp(System.currentTimeMillis()), next.id(), "test", "test"));
  }

  @Test
  public void testByName() {
    DatabaseImpl spanish = getDatabaseVeryLight("netProf", "config.properties", false);
    IProjectDAO projectDAO = spanish.getProjectDAO();

    String english1 = "english";
    int english = projectDAO.getByName(english1);

    logger.info("found " + english + " for " + english1);
  }

  @Test
  public void testPhones() {
    DatabaseImpl spanish = getDatabaseVeryLight("netProf", "config.properties", false);
    IProjectDAO projectDAO = spanish.getProjectDAO();

    String english1 = "english";
    int english = projectDAO.getByName(english1);

    logger.info("found " + english + " for " + english1);
  }

  @Test
  public void testOneEditNeighbors() {
    DatabaseImpl spanish = getDatabaseVeryLight("netProf", "config.properties", false);
    IUserDAO userDAO = spanish.getUserDAO();
    logger.info("counts " + userDAO + " " + userDAO.getUsers().size());

    spanish.populateProjects();
    spanish.setInstallPath("", "");

    Project project = spanish.getProject(2);

    List<CommonExercise> rawExercises = project.getRawExercises();

    int i = 0;

    // TODO : get the exercisePhoneInfo somehow...

    for (CommonExercise exercise : rawExercises) {
      ExercisePhoneInfo exercisePhoneInfo = null;//project.getExToPhone().get(exercise.getID());
      if (exercisePhoneInfo != null) {
        Map<String, ExerciseToPhone.Info> wordToInfo = exercisePhoneInfo.getWordToInfo();
        logger.info("for " + exercise.getID() + " : " + exercise.getForeignLanguage() + " " + wordToInfo);

        if (wordToInfo != null) {
          for (Map.Entry<String, ExerciseToPhone.Info> pair : wordToInfo.entrySet()) {
            String pron = pair.getKey();
            ExerciseToPhone.Info value = pair.getValue();
            Map<String, Integer> pronToCount = value.getPronToCount();
            logger.info("\t" + pron + " = " + value.getPronToInfo());// + " (" +pronToCount.get(pron)+ ")");
            logger.info("\t" + pron + " = " + pronToCount);
          }
        }

        if (i++ > MAX) break;
      }
    }

/*    List<String> tests = Arrays.asList("l", "la", "las", "s", "se", "seg", "ak");
    for (String test : tests) {
      Collection<CommonExercise> matches = phoneTrie.getMatches(test);
      logger.info("for " + test + " got " + matches.size());
      int i = 0;
      for (CommonExercise exercise : matches) {
        logger.info("found " + test + " : " + exercise.getForeignLanguage());
        if (i++ > 10) break;
      }
    }*/
  }

  @Test
  public void testAgain() {
    DatabaseImpl spanish = getDatabaseVeryLight("netProf", "config.properties", false);
    IProjectDAO projectDAO = spanish.getProjectDAO();

    String english1 = "mandarin";
    int english = projectDAO.getByName(english1);

    logger.info("found " + english + " for " + english1);

    spanish.populateProjects();

    spanish.setInstallPath("", "");

    Project project = spanish.getProject(english);

    logger.info("project " + project);

    project.getSectionHelper().report();
  }

  @Test
  public void testListProjects2() {
    IProjectDAO projectDAO = getDatabaseVeryLight("netProf", "config.properties", false).getProjectDAO();
    projectDAO.getAll().stream().forEach(p -> logger.info("projec " + p));
    //  projectDAO.delete(14);
  }

  @Test
  public void testDrop() {
    IProjectDAO projectDAO = getDatabaseVeryLight("netProf", "config.properties", false).getProjectDAO();
    projectDAO.delete(3);
  }

  @Test
  public void testMaleFemale() {
    DatabaseImpl database = getAndPopulate();
    Map<String, Float> maleFemaleProgress = database.getMaleFemaleProgress(3);
    logger.info(maleFemaleProgress.toString());
  }
  @Test
  public void testMaleFemaleFilterBySameGender() {
    DatabaseImpl database = getAndPopulate();
    IAudioDAO audioDAO = database.getAudioDAO();
    Collection<Integer> recordedBy = audioDAO.getRecordedBySameGender(2, Collections.emptyMap(),3);
    logger.info("found english " + recordedBy.size());

    Collection<Integer> frecordedBy = audioDAO.getRecordedBySameGender(133, Collections.emptyMap(),3);
    logger.info("found female english " + frecordedBy.size());

    Collection<Integer> recordedBy2 = audioDAO.getRecordedBySameGender(2, Collections.emptyMap(),2);
    logger.info("found hindi   " + recordedBy2.size());

    Collection<Integer> frecordedBy2 = audioDAO.getRecordedBySameGender(133, Collections.emptyMap(),2);
    logger.info("found female hindi   " + frecordedBy2.size());

    Map<String, Float> maleFemaleProgress = database.getMaleFemaleProgress(3);
    logger.info(maleFemaleProgress.toString());
  }

  @Test
  public void testAnalysis() {
    DatabaseImpl database = getAndPopulate();
    int projectid = 3;
    IAnalysis analysis = database.getAnalysis(projectid);
    List<UserInfo> userInfo = analysis.getUserInfo(database.getUserDAO(), 5);

    List<WordScore> wordScoresForUser = analysis.getWordScoresForUser(6, 5);

    for (WordScore wordScore:wordScoresForUser) logger.info("ws " +wordScore);

    for (UserInfo userInfo1 : userInfo) logger.info(userInfo1);
  }

  @Test
  public void testPhonesLookup() {
    getAndPopulate();
  }

  @Test
  public void testDropCroatian() {  doDrop( "croatian");  }

  @Test
  public void testDropSpanish() {  doDrop( "spanish");  }

  private void doDrop(String croatian) {
    DatabaseImpl andPopulate = getAndPopulate();
    IProjectDAO projectDAO = andPopulate.getProjectDAO();
    projectDAO.delete(projectDAO.getByName(croatian));
    andPopulate.close();
  }

  @Test
  public void testDropSpanishRef() {  doDropRef( "spanish");  }

  private void doDropRef(String croatian) {
    DatabaseImpl andPopulate = getAndPopulate();

    IProjectDAO projectDAO = andPopulate.getProjectDAO();
    int projid = projectDAO.getByName(croatian);
    andPopulate.getRefResultDAO().deleteForProject(projid);

    andPopulate.close();
  }

  @Test
  public void testEnglishHydra() {
    DatabaseImpl andPopulate = getAndPopulate();
    int english = andPopulate.getProjectDAO().getByName("english");
    Project project = andPopulate.getProject(english);
    logger.info("project " +project.getAudioFileHelper().isHydraAvailableCheckNow());
    andPopulate.close();
  }

  @Test
  public void testParse() {
    ServerProperties serverProperties = new ServerProperties();

    String json ="{\"words\":[{\"id\":\"0\",\"w\":\"<s>\",\"s\":\"0.977\",\"str\":\"0.0\",\"end\":\"0.71\",\"phones\":[]},{\"id\":\"1\",\"w\":\"abbreviation\",\"s\":\"0.995\",\"str\":\"0.71\",\"end\":\"1.75\",\"phones\":[{\"id\":\"0\",\"p\":\"ah\",\"s\":\"1.0\",\"str\":\"0.71\",\"end\":\"0.81\"},{\"id\":\"1\",\"p\":\"b\",\"s\":\"1.0\",\"str\":\"0.81\",\"end\":\"0.89\"},{\"id\":\"2\",\"p\":\"r\",\"s\":\"1.0\",\"str\":\"0.89\",\"end\":\"0.97\"},{\"id\":\"3\",\"p\":\"iy\",\"s\":\"0.943\",\"str\":\"0.97\",\"end\":\"1.03\"},{\"id\":\"4\",\"p\":\"v\",\"s\":\"1.0\",\"str\":\"1.03\",\"end\":\"1.1\"},{\"id\":\"5\",\"p\":\"iy\",\"s\":\"1.0\",\"str\":\"1.1\",\"end\":\"1.21\"},{\"id\":\"6\",\"p\":\"ey\",\"s\":\"1.0\",\"str\":\"1.21\",\"end\":\"1.36\"},{\"id\":\"7\",\"p\":\"sh\",\"s\":\"1.0\",\"str\":\"1.36\",\"end\":\"1.49\"},{\"id\":\"8\",\"p\":\"ah\",\"s\":\"1.0\",\"str\":\"1.49\",\"end\":\"1.58\"},{\"id\":\"9\",\"p\":\"n\",\"s\":\"0.987\",\"str\":\"1.58\",\"end\":\"1.75\"}]},{\"id\":\"2\",\"w\":\"<\\/s>\",\"s\":\"0.932\",\"str\":\"1.75\",\"end\":\"2.1\",\"phones\":[]}],\"score\":0.9984356,\"exid\":9255,\"valid\":\"OK\",\"reqid\":\"1\"}\n";
//    String json = "{\"words\":[{\"id\":\"0\",\"w\":\"<s>\",\"s\":\"0.977\",\"str\":\"0.0\",\"end\":\"0.71\",\"phones\":[]},{\"id\":\"1\",\"w\":\"abbreviation\",\"s\":\"0.995\",\"str\":\"0.71\",\"end\":\"1.75\",\"phones\":[{\"id\":\"0\",\"p\":\"ah\",\"s\":\"1.0\",\"str\":\"0.71\",\"end\":\"0.81\"},{\"id\":\"1\",\"p\":\"b\",\"s\":\"1.0\",\"str\":\"0.81\",\"end\":\"0.89\"},{\"id\":\"2\",\"p\":\"r\",\"s\":\"1.0\",\"str\":\"0.89\",\"end\":\"0.97\"},{\"id\":\"3\",\"p\":\"iy\",\"s\":\"0.943\",\"str\":\"0.97\",\"end\":\"1.03\"},{\"id\":\"4\",\"p\":\"v\",\"s\":\"1.0\",\"str\":\"1.03\",\"end\":\"1.1\"},{\"id\":\"5\",\"p\":\"iy\",\"s\":\"1.0\",\"str\":\"1.1\",\"end\":\"1.21\"},{\"id\":\"6\",\"p\":\"ey\",\"s\":\"1.0\",\"str\":\"1.21\",\"end\":\"1.36\"},{\"id\":\"7\",\"p\":\"sh\",\"s\":\"1.0\",\"str\":\"1.36\",\"end\":\"1.49\"},{\"id\":\"8\",\"p\":\"ah\",\"s\":\"1.0\",\"str\":\"1.49\",\"end\":\"1.58\"},{\"id\":\"9\",\"p\":\"n\",\"s\":\"0.987\",\"str\":\"1.58\",\"end\":\"1.75\"}]},{\"id\":\"2\",\"w\":\"<\\/s>\",\"s\":\"0.932\",\"str\":\"1.75\",\"end\":\"2.1\",\"phones\":[]}],\"exid\":9255,\"valid\":\"OK\",\"reqid\":\"1\"}\n";
//    String json = "{\"score\":0.7233214,\"WORD_TRANSCRIPT\":[{\"event\":\"<s>\",\"start\":0,\"end\":0.51,\"score\":0.9104534},{\"event\":\"abbreviation\",\"start\":0.51,\"end\":1.34,\"score\":0.7858934},{\"event\":\"<\\/s>\",\"start\":1.34,\"end\":1.35,\"score\":0.88590395}],\"PHONE_TRANSCRIPT\":[{\"event\":\"sil\",\"start\":0,\"end\":0.51,\"score\":0.9104534},{\"event\":\"ah\",\"start\":0.51,\"end\":0.62,\"score\":0.690349},{\"event\":\"b\",\"start\":0.62,\"end\":0.7,\"score\":0.94709295},{\"event\":\"r\",\"start\":0.7,\"end\":0.77,\"score\":1},{\"event\":\"iy\",\"start\":0.77,\"end\":0.84,\"score\":1},{\"event\":\"v\",\"start\":0.84,\"end\":0.9,\"score\":0.5870326},{\"event\":\"iy\",\"start\":0.9,\"end\":1.02,\"score\":0.96210086},{\"event\":\"ey\",\"start\":1.02,\"end\":1.1,\"score\":0.94535315},{\"event\":\"sh\",\"start\":1.1,\"end\":1.27,\"score\":0.7260531},{\"event\":\"ah\",\"start\":1.27,\"end\":1.31,\"score\":0.25677478},{\"event\":\"n\",\"start\":1.31,\"end\":1.34,\"score\":0.019446973},{\"event\":\"sil\",\"start\":1.34,\"end\":1.35,\"score\":0.8859039}],\"exid\":9255,\"valid\":\"OK\",\"reqid\":\"1\"}";
    PrecalcScores precalcScores = new PrecalcScores(serverProperties, json);

    logger.info("Got " + precalcScores);
  }

  private DatabaseImpl getAndPopulate() { return getDatabase().setInstallPath("war", "").populateProjects();  }
}
