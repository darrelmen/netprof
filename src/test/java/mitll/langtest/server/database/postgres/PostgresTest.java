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

import mitll.langtest.server.database.BaseTest;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.copy.CopyToPostgres;
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.*;

public class PostgresTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(PostgresTest.class);

  // set to true to use a local postgres database
  private static final boolean doLocal = false;

  @Test
  public void testCreate() {
    getDatabaseVeryLight("spanish", "config.properties", false).createTables();
  }

  @Test
  public void testListTables() {
    DBConnection spanish = getConnection();
    listAndClose(spanish);
  }

  @Test
  public void testDropNetProf() {
    DBConnection spanish = getConnection();
    spanish.dropAll();
    listAndClose(spanish);
  }

  private void listAndClose(DBConnection spanish) {
    listTables(spanish);
    spanish.close();
  }

  private void listTables(DBConnection spanish) {
    scala.collection.immutable.List<String> listOfTables = spanish.getListOfTables();
    scala.collection.Iterator<String> iterator = listOfTables.iterator();
    for (; iterator.hasNext();
        ) {
      logger.info("got " + iterator.next());
    }
    logger.info("list tables " + listOfTables);
  }

  @Test
  public void testCopySpanish() {
    testCopy(getSpanish());
  }

  @Test
  public void testCopySerbian() {
    copyDev("serbian");
  }

  @Test
  public void testCopyFrench() {
    copyDev("french");
  }

  @Test
  public void testCopyHindi() {
    copyDev("hindi");
  }

  @Test
  public void testCopyCroatian() {
    copyDev("croatian");
  }

  private void copyDev(String hindi) {
    Info info = new Info(hindi);
    info.setDev(true);
    testCopy(info);
  }

  @Test
  public void testCopyRussian() {
    testCopy(getRussian());
  }

  @Test
  public void testCopyEnglish() {
    testCopy(getEnglish());
  }

  @Test
  public void testCopyPashto1() {
    testCopy(getPashto());
  }

  @Test
  public void testCopyPashto2() {
    testCopy(getPashto2());
  }

  @Test
  public void testCopyPashto3() {
    testCopy(getPashto3());
  }

  @Test
  public void testDari() {
    copyProd("Dari");
  }

  private void copyProd(String config) {
    testCopy(new Info(config));
  }

  @Test
  public void testEgyptian() {
    copyProd("Egyptian");
  }

  @Test
  public void testFarsi() {
    copyProd("Farsi");
  }

  @Test
  public void testGerman() {
    copyProd("German");
  }

  @Test
  public void testKorean() {
    copyProd("Korean");
  }

  @Test
  public void testMandarin() {
    copyProd("Mandarin");
  }

  @Test
  public void testIraqi() {
    copyProd("Iraqi");
  }

  /**
   * Broken?
   */
  @Test
  public void testJapanese() {
    copyProd("japanese");
  }

  /**
   *
   */
  @Test
  public void testLevantine() {
    copyProd("Levantine");
  }

  @Test
  public void testMSA() {
    copyProd("msa");
  }

  @Test
  public void testTagalog() {
    copyProd("Tagalog");
  }

  /**
   * databaseHost=hydra-dev
   * databaseUser=netprof
   * databasePassword=npadmin
   */
  @Test
  public void testCopyAll() {
    List<Info> toCopy = new ArrayList<>();
    toCopy.add(new Info("Dari"));
    toCopy.add(new Info("Egyptian"));
    toCopy.add(getEnglish());
    toCopy.add(new Info("Farsi"));
    toCopy.add(new Info("German"));
    toCopy.add(new Info("Iraqi"));
    toCopy.add(new Info("Japanese"));
    toCopy.add(new Info("Korean"));
    toCopy.add(new Info("Levantine"));
    toCopy.add(new Info("Mandarin"));
    toCopy.add(new Info("msa"));
    toCopy.add(getPashto());
    toCopy.add(getPashto2());
    toCopy.add(getPashto3());
    // toCopy.add(new Info("pashto","Pashto Intermediate Foreign Language","pashtoQuizlet2.properties"));
    // toCopy.add(new Info("pashto","Pashto Advanced Foreign Language","pashtoQuizlet3.properties"));
    toCopy.add(getRussian());
    toCopy.add(getSpanish());
    toCopy.add(new Info("Sudanese"));
    toCopy.add(new Info("Tagalog"));
    toCopy.add(new Info("Urdu"));

    testCopy(toCopy);
  }

  @Test
  public void testCopyPashtos() {
    List<Info> toCopy = new ArrayList<>();
    toCopy.add(getPashto());
    toCopy.add(getPashto2());
    toCopy.add(getPashto3());
    testCopy(toCopy);
  }

  private Info getPashto() {
    return new Info("pashto", "Pashto Elementary", "pashtoQuizlet1.properties", 0, false);
  }

  private Info getPashto2() {
    return new Info("pashto", "Pashto Intermediate", "pashtoQuizlet2.properties", 1, false);
  }

  private Info getPashto3() {
    return new Info("pashto", "Pashto Advanced", "pashtoQuizlet3.properties", 2, false);
  }

  private Info getSpanish() {
    return new Info("spanish");
  }

  private Info getRussian() {
    return new Info("russian");
  }

  private Info getEnglish() {
    return new Info("english");
  }

  boolean justReport = false;

  private void testCopy(Info info) {
    testCopy(Collections.singletonList(info));
  }

  private void testCopy(List<Info> infos) {
    CopyToPostgres cp = new CopyToPostgres();
    try {
      for (Info config : infos) {
        String cc = cp.getCC(config.language);
        long then = System.currentTimeMillis();
        logger.info("\n\n\n-------- STARTED  copy " + config + " " + cc);

        //  DatabaseImpl databaseLight = getDatabaseLight(config.language, true, "hydra-dev", "netprof", "npadmin", config.props);
        DatabaseImpl databaseLight = getDatabaseLight(config.language, true, doLocal, config.props);

        logger.info("\n\n\n-------- Got  databaseLight " + databaseLight);

        if (justReport) {
          SectionHelper<CommonExercise> sectionHelper = databaseLight.getSectionHelper();
          sectionHelper.report();
        } else {
          cp.copyOneConfig(databaseLight, cc, config.name, config.displayOrder, config.isDev());
          databaseLight.close();
        }

        long now = System.currentTimeMillis();
        logger.info("\n\n\n-------- FINISHED copy " + config + " " + cc + " in " + ((now - then) / 1000) + " seconds");
        log();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void log() {
    int MB = (1024 * 1024);
    Runtime rt = Runtime.getRuntime();
    long free = rt.freeMemory();
    long used = rt.totalMemory() - free;
    long max = rt.maxMemory();

    ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
    logger.debug(" current thread group " + threadGroup.getName() + " = " + threadGroup.activeCount() +
        " : # cores = " + Runtime.getRuntime().availableProcessors() + " heap info free " + free / MB + "M used " + used / MB + "M max " + max / MB + "M");
  }

  private class Info {
    final String name;
    final String language;
    final String props;
    int displayOrder = 0;
    private boolean isDev;

    Info(String language) {
      this(language, language, null, 0, false);
    }

    public Info(String language, String name, String props, int displayOrder, boolean isDev) {
      this.language = language;
      this.name = name;
      this.props = props;
      this.displayOrder = displayOrder;
      this.isDev = isDev;
    }

    public String toString() {
      return language + " : " + name + " : " + props;
    }

    public boolean isDev() {
      return isDev;
    }

    public Info setDev(boolean dev) {
      isDev = dev;
      return this;
    }
  }

  @Test
  public void testDeleteEnglish() {
    String english1 = "english";
    DatabaseImpl english = getDatabaseLight(english1, true);
    IProjectDAO projectDAO = english.getProjectDAO();
    Collection<SlickProject> all = projectDAO.getAll();
    for (SlickProject project : all) {
      logger.info("found " + project);
      if (project.language().equalsIgnoreCase(english1)) {
        logger.info("deleting " + project);
        projectDAO.delete(project.id());
        break;
      } else {
        logger.debug("not deleting " + project);
      }
    }

    english.close();

  }

  @Test
  public void testGetFirstSpanish() {
    DatabaseImpl spanish = getDatabaseLight("spanish", false);
    SlickProject next = spanish.getProjectDAO().getAll().iterator().next();
    ExerciseDAO<CommonExercise> exerciseDAO = spanish.getExerciseDAO(next.id());
    List<CommonExercise> rawExercises = exerciseDAO.getRawExercises();
    for (CommonExercise ex : rawExercises.subList(0, 100)) {
      logger.info("ex " + ex.getID() + " '" + ex.getEnglish() + "' '" + ex.getForeignLanguage() + "' : " +
          ex.getDirectlyRelated().size() + " context sentences.");
      for (CommonExercise cex : ex.getDirectlyRelated()) {
        logger.info("\t context " + cex.getID() + " '" + cex.getEnglish() + "' '" + cex.getForeignLanguage() + "'");
      }
    }
    logger.info("Got " + rawExercises.iterator().next());
    spanish.close();
    //   new CopyToPostgres().copyUserExListJoin(spanish);
  }


  @Test
  public void testGetContext() {
    DatabaseImpl database = getDatabaseLight("netProf", false);
    Collection<SlickProject> all = database.getProjectDAO().getAll();
    int toIndex = 10;
    for (SlickProject project : all) {
      String language = project.language();
      logger.info("lang " + language);
      if (language.equalsIgnoreCase("russian")) {
        ExerciseDAO<CommonExercise> exerciseDAO = database.getExerciseDAO(project.id());
        List<CommonExercise> rawExercises = exerciseDAO.getRawExercises();
        for (CommonExercise ex : rawExercises.subList(0, toIndex)) {
          logger.info("ex " + ex.getID() + " '" + ex.getEnglish() + "' '" + ex.getForeignLanguage() + "'" +
              " meaning '" + ex.getMeaning() +
              "' : " + ex.getDirectlyRelated().size() + " context sentences.");
          for (CommonExercise cex : ex.getDirectlyRelated()) {
            logger.info("\t context " + cex.getID() + " '" + cex.getEnglish() + "' '" + cex.getForeignLanguage() + "'");
          }
        }
        logger.info("Got " + rawExercises.iterator().next());

      }
    }
  }

  @Test
  public void testAudio() {
    DatabaseImpl database = getDatabaseLight("netProf", false);
    Collection<SlickProject> all = database.getProjectDAO().getAll();
    int toIndex = 10;
    for (SlickProject project : all) {
      String language = project.language();
      logger.info("lang " + language);
      if (language.equalsIgnoreCase("msa")) {
        CommonExercise exercise = database.getExercise(project.id(), 23125);
        logger.info("Got " + exercise);
        for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) logger.info("got " + audioAttribute);
      }
    }
  }

  @Test
  public void testNest() {
    DatabaseImpl database = getDatabaseLight("netProf", false);
    Collection<SlickProject> all = database.getProjectDAO().getAll();
    int toIndex = 10;
    for (SlickProject project : all) {
      String language = project.language();
      logger.info("lang " + language);
      //  String spanish = "spanish";
      String spanish = "msa";
      if (language.equalsIgnoreCase(spanish)) {
        Project project1 = database.getProject(project.id());

        SectionHelper<CommonExercise> sectionHelper = project1.getSectionHelper();
        logger.info("type order " + sectionHelper.getTypeOrder());
        sectionHelper.report();

        if (false) {
          Map<String, String> choice = new HashMap<>();
          // choice.put("Unit",  "1");
          choice.put("Chapter", "1");
//        choice.put("Sound", "rf");
          Collection<CommonExercise> exercisesForSelectionState = sectionHelper.getExercisesForSimpleSelectionState(choice);

          int b = 0;

          for (CommonExercise ex : exercisesForSelectionState) {
            if (b++ < 10)
              logger.info("1 found " + ex.getID() + " : " + ex.getForeignLanguage());
          }

          choice.put("Sound", "jj");
          exercisesForSelectionState = sectionHelper.getExercisesForSimpleSelectionState(choice);

          int c = 0;
          for (CommonExercise ex : exercisesForSelectionState) {
            if (c++ < 10)
              logger.info("2 found " + ex.getID() + " :\t" + ex.getForeignLanguage());
          }
        }
      }
    }
  }


  @Test
  public void testGetContextAll() {
    DatabaseImpl database = getDatabaseLight("netProf", false);
    Collection<SlickProject> all = database.getProjectDAO().getAll();
    int toIndex = 10;
    for (SlickProject project : all) {
      String language = project.language();
      logger.info("lang " + language);
      ExerciseDAO<CommonExercise> exerciseDAO = database.getExerciseDAO(project.id());
      List<CommonExercise> rawExercises = exerciseDAO.getRawExercises();
      for (CommonExercise ex : rawExercises.subList(0, toIndex)) {
        logger.info("ex " + ex.getID() + " '" + ex.getEnglish() + "' '" + ex.getForeignLanguage() + "'" +
            " meaning '" + ex.getMeaning() +
            "' : " + ex.getDirectlyRelated().size() + " context sentences.");
        for (CommonExercise cex : ex.getDirectlyRelated()) {
          logger.info("\t context " + cex.getID() + " '" + cex.getEnglish() + "' '" + cex.getForeignLanguage() + "'");
        }
      }
      logger.info("Got " + rawExercises.iterator().next());
    }
  }


  @Test
  public void testAnalysisWordsForUser() {
    DatabaseImpl database = getDatabaseLight("netProf", false);
    Collection<SlickProject> all = database.getProjectDAO().getAll();
    int toIndex = 10;
    for (SlickProject project : all) {
      String language = project.language();
      logger.info("lang " + language);
      int id = project.id();
      List<WordScore> wordScoresForUser = database.getAnalysis(id).getWordScoresForUser(4, 1);
      for (WordScore ws : wordScoresForUser) {
        logger.info("testWords got " + ws);
      }
    }
  }

  @Test
  public void testAddUserProject() {
    DatabaseImpl spanish = getDatabaseLight("spanish", false);
    SlickProject next = spanish.getProjectDAO().getAll().iterator().next();
    int id = next.id();
    User byID = spanish.getUserDAO().getUserByID("gvidaver");
    logger.info("user is " + byID + " project " + next);
    // spanish.rememberUserSelectedProject(byID, id);
//    for (SlickUserProject up : spanish.getUserProjectDAO().getAll()) {
//      logger.info("got " + up);
//    }
  }

  @Test
  public void testMostRecentProject() {
    DatabaseImpl spanish = getDatabaseLight("netprof", false);

    User byID = spanish.getUserDAO().getUserByID("gvidaver");
    logger.info("user is " + byID);
    int i = spanish.getUserProjectDAO().mostRecentByUser(byID.getID());
    logger.info("most recent is " + i);
    i = spanish.getUserProjectDAO().mostRecentByUser(999999);
    logger.info("most recent is " + i);
    i = spanish.getUserProjectDAO().mostRecentByUser(342);
    logger.info("most recent is " + i);
  }

/*
  @Test
  public void testUserCount() {
    DatabaseImpl spanish = getDatabaseLight("netprof", false);
    Map<User.Kind, Integer> counts = spanish.getUserDAO().getCounts();
    logger.info("got " + counts);
  }
*/


  @Test
  public void testProjects() {
    DatabaseImpl spanish = getDatabaseLight("spanish", false);
    spanish.getProjectDAO().getAll().iterator().next();
  }

  /**
   * Doesn't work?
   */
  @Test
  public void testListProjects() {
    DatabaseImpl spanish = getDatabaseLight("spanish", false);
    Collection<SlickProject> all = spanish.getProjectDAO().getAll();
    logger.info("found " + all.size());

    for (SlickProject project : all) logger.info("project" + project);
  }

  private static DBConnection getConnection() {  return new DBConnection(getProps().getDBConfig());  }
}
