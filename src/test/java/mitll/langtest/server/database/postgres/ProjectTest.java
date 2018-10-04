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

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.analysis.IAnalysis;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.userexercise.ExercisePhoneInfo;
import mitll.langtest.server.database.userexercise.ExerciseToPhone;
import mitll.langtest.server.scoring.PrecalcScores;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.trie.ExerciseTrie;
import mitll.langtest.server.trie.SearchHelper;
import mitll.langtest.shared.analysis.UserInfo;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.langtest.shared.project.ProjectType;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.SlickProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.text.Normalizer;
import java.util.*;

public class ProjectTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(ProjectTest.class);
  public static final int MAX = 200;
  public static final int PROJECTID = 9;

/*  @Test
  public void testProjectInfo() {
    DatabaseImpl spanish = getDatabase();
    spanish.setInstallPath("");
    spanish.getProjectManagement().getVocabProjects();
    List<ImportDoc> docs = spanish.getProjectManagement().getDocs(707, System.currentTimeMillis());
    docs.forEach(doc->logger.info("got "+doc));
  }*/
//
//  @Test
//  public void testImport() {
//    DatabaseImpl spanish = getDatabase();
//    spanish.setInstallPath("");
//    IProjectManagement projectManagement = spanish.getProjectManagement();
//    ImportInfo importFromDomino = projectManagement.getImportFromDomino(5, 707, "now");
//    FileUploadHelper fileUploadHelper = projectManagement.getFileUploadHelper();
//    fileUploadHelper.rememberExercises(5,importFromDomino);
//    ImportInfo exercises = fileUploadHelper.getExercises(5);
//    logger.info("Got " + exercises);
//  }


  @Test
  public void testFrench2() {
    SmallVocabDecoder svd = new SmallVocabDecoder();
    {
      String ff = "\uFB00";
      String normalized = Normalizer.normalize(ff, Normalizer.Form.NFKC);
      System.out.println(ff + " = " + normalized);
    }
    {
      String oe = "\u0152";
      String normalized2 = Normalizer.normalize(oe, Normalizer.Form.NFKC);
      System.out.println(oe + " = " + normalized2 + " " + svd.getTrimmed(oe));
    }
    {
      String oe = "\u0153";
      String normalized2 = Normalizer.normalize(oe, Normalizer.Form.NFKC);
      System.out.println(oe + " = " + normalized2 + " " + svd.getTrimmed(oe));
    }
  }

  @Test
  public void testFrench() {

    {
      String ff = "\uFB00";
      String normalized = Normalizer.normalize(ff, Normalizer.Form.NFKC);
      System.out.println(ff + " = " + normalized);
    }
    {
      String oe = "\u0152";
      String normalized2 = Normalizer.normalize(oe, Normalizer.Form.NFKC);
      System.out.println(oe + " = " + normalized2);
    }
    {
      String oe = "\u0153";
      String normalized2 = Normalizer.normalize(oe, Normalizer.Form.NFKC);
      System.out.println(oe + " = " + normalized2);
    }

    DatabaseImpl french = getDatabase();
    Project project = french.getProject(22);
    project.getRawExercises().forEach(exercise -> {

      boolean ef = true;//exercise.getForeignLanguage().contains("b");
      if (ef) {
        AudioFileHelper audioFileHelper = project.getAudioFileHelper();
        String foreignLanguage = exercise.getForeignLanguage();
        String pronunciationsFromDictOrLTS =
            audioFileHelper.getPronunciationsFromDictOrLTSFull(foreignLanguage, "");

        logger.warn("For " +
            "\n\ten " + exercise.getEnglish() +
            "\n\tfl " + exercise.getForeignLanguage() +
            "\n\t   " + pronunciationsFromDictOrLTS+
            "\n\tLM " + audioFileHelper.getLM(foreignLanguage,false)+
            "\n\tTR " + audioFileHelper.getHydraTranscript(foreignLanguage)
        );
      }
    });
//    project.getAudioFileHelper().getPronunciationsFromDictOrLTS()
  }


  @Test
  public void testSegmentation() {
    DatabaseImpl spanish = getDatabase();
    Project project = spanish.getProject(3);

    String fl = "そして, 何を飲みましたか";


    String test = "海で泳いで、喫茶店で昼ごはんをたべました。";


    AudioFileHelper audioFileHelper = project.getAudioFileHelper();


    String segmented = audioFileHelper.getSegmented(test);

    logger.info("got " + segmented);
    logger.info("1 got '" + test + "' = '" + segmented + "'");

    String pronunciationsFromDictOrLTS = audioFileHelper.getASR().getHydraDict(segmented, "",
        new ArrayList<>()).getDict();
    logger.info("1 pronunciationsFromDictOrLTS '" + pronunciationsFromDictOrLTS + "' = '" + segmented + "'");


/*    for (CommonExercise ex : project.getRawExercises()) {
      String foreignLanguage = ex.getForeignLanguage();
      String segmented = audioFileHelper.getSegmented(foreignLanguage);
      logger.info("1 got '" + foreignLanguage + "' = '" + segmented + "'");

      String pronunciationsFromDictOrLTS = audioFileHelper.getASR().getHydraDict(segmented, ex.getTransliteration());


      //audioFileHelper.getPronunciationsFromDictOrLTS(foreignLanguage, ex.getTransliteration());
      logger.info("1 got " + pronunciationsFromDictOrLTS);
      for (CommonExercise ex2 : ex.getDirectlyRelated()) {
        String foreignLanguage1 = ex2.getForeignLanguage();
        String segmented1 = audioFileHelper.getSegmented(foreignLanguage1);
        logger.info("2 got " + foreignLanguage1 + " = " + segmented1);

        // String pronunciationsFromDictOrLTS2 = audioFileHelper.getPronunciationsFromDictOrLTS(foreignLanguage1, ex2.getTransliteration());
        String pronunciationsFromDictOrLTS2 = audioFileHelper.getASR().getHydraDict(segmented1, ex2.getTransliteration());
        logger.info("2 got " + pronunciationsFromDictOrLTS2);
      }
    }*/


//    String s = removePunct(fl);
//    List<String> tokensAllLanguages = project.getAudioFileHelper().getSmallVocabDecoder().getTokensAllLanguages(true, fl);
//
//    tokensAllLanguages.forEach(token->logger.info("got " + token));
//
//   tokensAllLanguages = project.getAudioFileHelper().getSmallVocabDecoder().getTokensAllLanguages(true, s);
//
//    tokensAllLanguages.forEach(token->logger.info("got " + token));

  }

  private String removePunct(String t) {
    return t
        .replaceAll("\\.\\.\\.", " ")
        .replaceAll("/", " ")
        .replaceAll(",", " ")
        .replaceAll("\\p{P}", "");
  }


  @Test
  public void testProject() {
    DatabaseImpl spanish = getDatabase();
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
        ProjectStatus.PRODUCTION, iterator.next(), iterator.next(), "es", 0, -1);
  }

  @Test
  public void testListProjects() {
    DatabaseImpl spanish = getDatabase();

    IProjectDAO projectDAO = spanish.getProjectDAO();
    Collection<SlickProject> all = projectDAO.getAll();
    for (SlickProject project : all) {
      logger.info("Got " + project);
//      for (SlickProjectProperty prop : project.getProps()) {
//        logger.info("\t prop " + prop);
//      }
    }
  }


//  @Test
//  public void testReadDominoJSON() {
//    DatabaseImpl spanish = getDatabase("spanish");
//
//    IProjectDAO projectDAO = spanish.getProjectDAO();
//    Collection<SlickProject> all = projectDAO.getAll();
//    for (SlickProject project : all) {
//      logger.info("Got " + project);
//      for (SlickProjectProperty prop : project.getProps()) {
//        logger.info("\t prop " + prop);
//      }
//    }
//  }

  @Test
  public void testAddProperty() {
    DatabaseImpl spanish = getDatabaseVeryLight("spanish", "quizlet.properties", false);

    IProjectDAO projectDAO = spanish.getProjectDAO();
    SlickProject next = projectDAO.getAll().iterator().next();

    projectDAO.addProperty(next.id(), "key", "value", "", "");

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

    spanish.populateProjects(-1);
    spanish.setInstallPath("");

    Project project = spanish.getProject(2);

    List<CommonExercise> rawExercises = project.getRawExercises();

    int i = 0;

    // TODO : get the exercisePhoneInfo somehow...

    for (CommonExercise exercise : rawExercises) {
      ExercisePhoneInfo exercisePhoneInfo = null;//project.getExToPhone().get(exercise.getID());
      if (exercisePhoneInfo != null) {
        Map<String, ExerciseToPhone.Info> wordToInfo = Collections.emptyMap();//exercisePhoneInfo.getWordToInfo();
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
  public void testOneEditNeighbors2() {
    DatabaseImpl spanish = getDatabase();
    IUserDAO userDAO = spanish.getUserDAO();
    logger.info("counts " + userDAO + " " + userDAO.getUsers().size());

    spanish.populateProjects(-1);
    spanish.setInstallPath("");

    Project project = spanish.getProject(2);

    List<CommonExercise> rawExercises = project.getRawExercises();

    int i = 0;

    // TODO : get the exercisePhoneInfo somehow...

    for (CommonExercise exercise : rawExercises) {
      ExercisePhoneInfo exercisePhoneInfo = null;//project.getExToPhone().get(exercise.getID());
      if (exercisePhoneInfo != null) {
        Map<String, ExerciseToPhone.Info> wordToInfo = Collections.emptyMap();//exercisePhoneInfo.getWordToInfo();
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

    spanish.populateProjects(-1);

    spanish.setInstallPath("");

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
    Project project = database.getProject(14);
    CommonExercise exerciseByID = project.getExerciseByID(41301);

    logger.info("Got " + exerciseByID);

    CommonExercise customOrPredefExercise = database.getCustomOrPredefExercise(14, 41301);

    logger.info("Got " + customOrPredefExercise);
    Map<String, Float> maleFemaleProgress = database.getMaleFemaleProgress(14);
    logger.info(maleFemaleProgress.toString());
  }

/*
  @Test
  public void testMaleFemaleFilterBySameGender() {
    DatabaseImpl database = getAndPopulate();
    IAudioDAO audioDAO = database.getAudioDAO();
    Collection<Integer> recordedBy = audioDAO.getRecordedBySameGender(2, 3, exToTranscript);
    logger.info("found english " + recordedBy.size());

    Collection<Integer> frecordedBy = audioDAO.getRecordedBySameGender(133, 3, exToTranscript);
    logger.info("found female english " + frecordedBy.size());

    Collection<Integer> recordedBy2 = audioDAO.getRecordedBySameGender(2, 2, exToTranscript);
    logger.info("found hindi   " + recordedBy2.size());

    Collection<Integer> frecordedBy2 = audioDAO.getRecordedBySameGender(133, 2, exToTranscript);
    logger.info("found female hindi   " + frecordedBy2.size());

    Map<String, Float> maleFemaleProgress = database.getMaleFemaleProgress(3);
    logger.info(maleFemaleProgress.toString());
  }
*/

  @Test
  public void testSearch() {
    DatabaseImpl database = getAndPopulate();
    // IAudioDAO audioDAO = database.getAudioDAO();

    Project project = database.getProject(2);

    ExerciseTrie<CommonExercise> fullTrie = project.getFullTrie();

    List<String> supuesto = Arrays.asList("definitiva", "en def", "la de", "la def", "ll ", "ll t", "ll th", "ll thi", "all ", "at ", "at a", "at al", "at a ", "all thing", "all c", "all co", "thi", "thin", "thing", "things", "things ", "things c", "things con", " to a", "to allow");
    // List<String> supuesto = Arrays.asList("at a", "at al");

    for (String test : supuesto) {
      try {
        List<CommonExercise> exercises = fullTrie.getExercises(test);
        // logger.info(test + " : "  + exercises);

        if (exercises.isEmpty()) logger.error("no match for " + test);
        for (CommonExercise exercise : exercises) {
          logger.info(test + " : '" + exercise.getForeignLanguage() + "'\t'" + exercise.getEnglish() + "'");
        }
      } catch (Exception e) {
        logger.error("got " + e, e);
      }
    }
  }

  @Test
  public void testFrenchSearch() {
    DatabaseImpl database = getAndPopulate();
    // IAudioDAO audioDAO = database.getAudioDAO();

    Project project = database.getProject(PROJECTID);

    List<String> supuesto = Arrays.asList("ecrire", "Écrire", "Écrire".toLowerCase(), "savez-vous");
    {
      ExerciseTrie<CommonExercise> fullTrie = project.getFullTrie();
      for (String test : supuesto) {
        try {
          List<CommonExercise> exercises = fullTrie.getExercises(test);
          // logger.info(test + " : "  + exercises);

          if (exercises.isEmpty()) logger.error("no match for " + test);
          for (CommonExercise exercise : exercises) {
            logger.info(test + " : " + exercise.getID() + " '" + exercise.getForeignLanguage() + "'\t'" + exercise.getEnglish() + "'");
          }
        } catch (Exception e) {
          logger.error("got " + e, e);
        }
      }
    }

    {
      UserList<CommonExercise> userListByID = database.getUserListByIDExercises(3924, PROJECTID);
      List<CommonExercise> exercises = userListByID.getExercises();
      for (CommonExercise exercise : exercises) {
        Collection<ClientExercise> directlyRelated = exercise.getDirectlyRelated();

        logger.info("User list " + userListByID + " : " + exercise.getID() + " '" + exercise.getForeignLanguage() + "'\t'" + exercise.getEnglish() + "'");
        for (ClientExercise de : directlyRelated) {
          logger.info("\tUser list " + userListByID + " : " + de.getID() + " '" + de.getForeignLanguage() + "'\t'" + de.getEnglish() + "'");
        }
      }
    /*  for (String test : supuesto) {
        Collection<CommonExercise> searchMatches = new SearchHelper().getSearchMatches(exercises, test, "french", project.getAudioFileHelper().getSmallVocabDecoder());

        if (searchMatches.isEmpty()) logger.error("2 no match for " + test);
        else {
          for (CommonExercise exercise : searchMatches) {
            logger.info(test + " : " + exercise.getID() + " '" + exercise.getForeignLanguage() + "'\t'" + exercise.getEnglish() + "'");
          }
        }
      }*/
    }
  }

  @Test
  public void testAnalysis() {
    DatabaseImpl database = getAndPopulate();
    int projectid = 3;
    IAnalysis analysis = database.getAnalysis(projectid);
    List<UserInfo> userInfo = analysis.getUserInfo(database.getUserDAO(), 5);
//
//    List<WordScore> wordScoresForUser = analysis.getWordScoresForUser(6, 5, -1);
//
//    for (WordScore wordScore : wordScoresForUser) logger.info("ws " + wordScore);
//
//    for (UserInfo userInfo1 : userInfo) logger.info(userInfo1);
  }


/*  @Test
  public void testReadDominoJSON() {
    DominoExerciseDAO dominoExerciseDAO = getAndPopulate().getDominoExerciseDAO();
    ImportInfo info = dominoExerciseDAO.readExercises("SAMPLE-NO-EXAM.json", null,
        -1, 1);

    logger.info("Got " + info);
  }*/

  @Test
  public void testPhonesLookup() {
    getAndPopulate();
  }

  @Test
  public void testDropCroatian() {
    doDrop("croatian");
  }

  @Test
  public void testDropSpanish() {
    doDrop("spanish");
  }

  private void doDrop(String croatian) {
    DatabaseImpl andPopulate = getAndPopulate();
    IProjectDAO projectDAO = andPopulate.getProjectDAO();
    int byLanguage = projectDAO.getByLanguage(croatian);
    projectDAO.delete(byLanguage);
    // projectDAO.delete(projectDAO.getByLanguage("sorani"));
    andPopulate.close();
  }

  @Test
  public void testDropSpanishRef() {
    doDropRef("spanish");
  }

  @Test
  public void testDropPashto() {
    doDrop("pashto");
  }

  @Test
  public void testDropPashto1() {
    doDrop("Pashto Elementary");
  }

  private void doDropRef(String croatian) {
    DatabaseImpl andPopulate = getDatabase().setInstallPath("");

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
    logger.info("project " + project.getAudioFileHelper().isHydraAvailableCheckNow());
    andPopulate.close();
  }

  @Test
  public void testReport() {
    DatabaseImpl andPopulate = getAndPopulate();
    int english = andPopulate.getProjectDAO().getByName("portuguese");
    Project project = andPopulate.getProject(english);
    project.getSectionHelper().report();
    ;
    // logger.info("project " + project.getAudioFileHelper().isHydraAvailableCheckNow());
    andPopulate.close();
  }

  @Test
  public void testCheckOwner() {
    DatabaseImpl andPopulate = getAndPopulate();
    IProjectManagement projectManagement = andPopulate.getProjectManagement();
    int userForFile = projectManagement.getUserForFile("answers/spanish/answers/plan/1945/0/subject-83/answer_1474213851511.mp3");
    logger.info("got " + userForFile);

    userForFile = projectManagement.getUserForFile("answers/spanish/answers/plan/1945/0/subject-83/answer_1474213851511.wav");
    logger.info("got " + userForFile);

    userForFile = projectManagement.getUserForFile("answers/plan/1945/0/subject-83/answer_1474213851511.wav");
    logger.info("got " + userForFile);

    andPopulate.close();
  }

  @Test
  public void testDecode() {
    DatabaseImpl andPopulate = getAndPopulate();
    int english = andPopulate.getProjectDAO().getByName("spanish");
    Project project = andPopulate.getProject(english);
    AudioFileHelper audioFileHelper = project.getAudioFileHelper();

    audioFileHelper.runHydra("De acuerdo " +
        "al " +
        "decano " +
        "tenemos " +
        "59 " +
        "minutos.");

    andPopulate.close();
  }


  AudioFileHelper getAudioFileHelper(DatabaseImpl russian) {
    ServerProperties serverProps = russian.getServerProps();
    return new AudioFileHelper(new PathHelper("war", null), serverProps, russian, null, null);
  }

  @Test
  public void testParse() {
    ServerProperties serverProperties = new ServerProperties();

    String json = "{\"words\":[{\"id\":\"0\",\"w\":\"<s>\",\"s\":\"0.977\",\"str\":\"0.0\",\"end\":\"0.71\",\"phones\":[]},{\"id\":\"1\",\"w\":\"abbreviation\",\"s\":\"0.995\",\"str\":\"0.71\",\"end\":\"1.75\",\"phones\":[{\"id\":\"0\",\"p\":\"ah\",\"s\":\"1.0\",\"str\":\"0.71\",\"end\":\"0.81\"},{\"id\":\"1\",\"p\":\"b\",\"s\":\"1.0\",\"str\":\"0.81\",\"end\":\"0.89\"},{\"id\":\"2\",\"p\":\"r\",\"s\":\"1.0\",\"str\":\"0.89\",\"end\":\"0.97\"},{\"id\":\"3\",\"p\":\"iy\",\"s\":\"0.943\",\"str\":\"0.97\",\"end\":\"1.03\"},{\"id\":\"4\",\"p\":\"v\",\"s\":\"1.0\",\"str\":\"1.03\",\"end\":\"1.1\"},{\"id\":\"5\",\"p\":\"iy\",\"s\":\"1.0\",\"str\":\"1.1\",\"end\":\"1.21\"},{\"id\":\"6\",\"p\":\"ey\",\"s\":\"1.0\",\"str\":\"1.21\",\"end\":\"1.36\"},{\"id\":\"7\",\"p\":\"sh\",\"s\":\"1.0\",\"str\":\"1.36\",\"end\":\"1.49\"},{\"id\":\"8\",\"p\":\"ah\",\"s\":\"1.0\",\"str\":\"1.49\",\"end\":\"1.58\"},{\"id\":\"9\",\"p\":\"n\",\"s\":\"0.987\",\"str\":\"1.58\",\"end\":\"1.75\"}]},{\"id\":\"2\",\"w\":\"<\\/s>\",\"s\":\"0.932\",\"str\":\"1.75\",\"end\":\"2.1\",\"phones\":[]}],\"score\":0.9984356,\"exid\":9255,\"valid\":\"OK\",\"reqid\":\"1\"}\n";
//    String json = "{\"words\":[{\"id\":\"0\",\"w\":\"<s>\",\"s\":\"0.977\",\"str\":\"0.0\",\"end\":\"0.71\",\"phones\":[]},{\"id\":\"1\",\"w\":\"abbreviation\",\"s\":\"0.995\",\"str\":\"0.71\",\"end\":\"1.75\",\"phones\":[{\"id\":\"0\",\"p\":\"ah\",\"s\":\"1.0\",\"str\":\"0.71\",\"end\":\"0.81\"},{\"id\":\"1\",\"p\":\"b\",\"s\":\"1.0\",\"str\":\"0.81\",\"end\":\"0.89\"},{\"id\":\"2\",\"p\":\"r\",\"s\":\"1.0\",\"str\":\"0.89\",\"end\":\"0.97\"},{\"id\":\"3\",\"p\":\"iy\",\"s\":\"0.943\",\"str\":\"0.97\",\"end\":\"1.03\"},{\"id\":\"4\",\"p\":\"v\",\"s\":\"1.0\",\"str\":\"1.03\",\"end\":\"1.1\"},{\"id\":\"5\",\"p\":\"iy\",\"s\":\"1.0\",\"str\":\"1.1\",\"end\":\"1.21\"},{\"id\":\"6\",\"p\":\"ey\",\"s\":\"1.0\",\"str\":\"1.21\",\"end\":\"1.36\"},{\"id\":\"7\",\"p\":\"sh\",\"s\":\"1.0\",\"str\":\"1.36\",\"end\":\"1.49\"},{\"id\":\"8\",\"p\":\"ah\",\"s\":\"1.0\",\"str\":\"1.49\",\"end\":\"1.58\"},{\"id\":\"9\",\"p\":\"n\",\"s\":\"0.987\",\"str\":\"1.58\",\"end\":\"1.75\"}]},{\"id\":\"2\",\"w\":\"<\\/s>\",\"s\":\"0.932\",\"str\":\"1.75\",\"end\":\"2.1\",\"phones\":[]}],\"exid\":9255,\"valid\":\"OK\",\"reqid\":\"1\"}\n";
//    String json = "{\"score\":0.7233214,\"WORD_TRANSCRIPT\":[{\"event\":\"<s>\",\"start\":0,\"end\":0.51,\"score\":0.9104534},{\"event\":\"abbreviation\",\"start\":0.51,\"end\":1.34,\"score\":0.7858934},{\"event\":\"<\\/s>\",\"start\":1.34,\"end\":1.35,\"score\":0.88590395}],\"PHONE_TRANSCRIPT\":[{\"event\":\"sil\",\"start\":0,\"end\":0.51,\"score\":0.9104534},{\"event\":\"ah\",\"start\":0.51,\"end\":0.62,\"score\":0.690349},{\"event\":\"b\",\"start\":0.62,\"end\":0.7,\"score\":0.94709295},{\"event\":\"r\",\"start\":0.7,\"end\":0.77,\"score\":1},{\"event\":\"iy\",\"start\":0.77,\"end\":0.84,\"score\":1},{\"event\":\"v\",\"start\":0.84,\"end\":0.9,\"score\":0.5870326},{\"event\":\"iy\",\"start\":0.9,\"end\":1.02,\"score\":0.96210086},{\"event\":\"ey\",\"start\":1.02,\"end\":1.1,\"score\":0.94535315},{\"event\":\"sh\",\"start\":1.1,\"end\":1.27,\"score\":0.7260531},{\"event\":\"ah\",\"start\":1.27,\"end\":1.31,\"score\":0.25677478},{\"event\":\"n\",\"start\":1.31,\"end\":1.34,\"score\":0.019446973},{\"event\":\"sil\",\"start\":1.34,\"end\":1.35,\"score\":0.8859039}],\"exid\":9255,\"valid\":\"OK\",\"reqid\":\"1\"}";
    PrecalcScores precalcScores = new PrecalcScores(serverProperties, json, Language.SPANISH);

    logger.info("Got " + precalcScores);
  }
}
