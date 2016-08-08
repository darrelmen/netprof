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
import mitll.langtest.server.database.CopyToPostgres;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickProject;
import mitll.npdata.dao.SlickUserProject;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PostgresTest extends BaseTest {
  private static final Logger logger = Logger.getLogger(PostgresTest.class);

  @Test
  public void testCreate() {
    getDatabaseVeryLight("spanish", "quizlet.properties", false).createTables();
  }

  @Test
  public void testDrop() {
    DBConnection spanish = getConnection("spanish");
    spanish.dropAll();
    scala.collection.immutable.List<String> listOfTables = spanish.getListOfTables();

    logger.info("after drop " + listOfTables);
  }

  @Test
  public void testDropNetProf() {
    DBConnection spanish = getConnection("netProf");
    spanish.dropAll();
    scala.collection.immutable.List<String> listOfTables = spanish.getListOfTables();

    logger.info("after drop " + listOfTables);
  }

  @Test
  public void testCopySpanish() {
    List<Info> toCopy = new ArrayList<>();
    toCopy.add(getSpanish());
  }

  @Test
  public void testCopyRussian() {
    List<Info> toCopy = new ArrayList<>();
    toCopy.add(getRussian());
  }

  @Test
  public void testCopyEnglish() {
    List<Info> toCopy = new ArrayList<>();
    toCopy.add(getEnglish());
    testCopy(toCopy);
  }

  @Test
  public void testCopyPashto1() {
    List<Info> toCopy = new ArrayList<>();
    toCopy.add(getPashto());
    testCopy(toCopy);
  }

  @Test
  public void testCopyPashto2() {
    List<Info> toCopy = new ArrayList<>();
    toCopy.add(getPashto2());
    testCopy(toCopy);
  }

  /**
   * databaseHost=hydra-dev
   * databaseUser=netprof
   * databasePassword=npadmin
   */
  @Test
  public void testCopyAll() {
    List<Info> toCopy = new ArrayList<>();
    toCopy.add(getEnglish());
    toCopy.add(new Info("msa"));
    toCopy.add(getPashto());
    // toCopy.add(new Info("pashto","Pashto Intermediate Foreign Language","pashtoQuizlet2.properties"));
    // toCopy.add(new Info("pashto","Pashto Advanced Foreign Language","pashtoQuizlet3.properties"));
    toCopy.add(getRussian());
    toCopy.add(getSpanish());
    testCopy(toCopy);
  }

  Info getPashto() {
    return new Info("pashto", "Pashto Elementary", "pashtoQuizlet1.properties");
  }

  Info getPashto2() {
    return new Info("pashto", "Pashto Intermediate", "pashtoQuizlet2.properties");
  }

  Info getPashto3() {
    return new Info("pashto", "Pashto Advanced", "pashtoQuizlet3.properties");
  }

  Info getSpanish() {
    return new Info("spanish");
  }

  Info getRussian() {
    return new Info("russian");
  }

  Info getEnglish() {
    return new Info("english");
  }

  void testCopy(List<Info> infos) {
    CopyToPostgres cp = new CopyToPostgres();
    for (Info config : infos) {
      //logger.info("-------- copy " + config);
      String cc = cp.getCC(config.language);
      logger.info("-------- copy " + config + " " + cc);

      DatabaseImpl databaseLight = getDatabaseLight(config.language, true, "hydra-dev", "netprof", "npadmin", config.props);
      new CopyToPostgres().copyOneConfig(databaseLight, cc, config.name);

      //((DatabaseImpl) databaseLight).copyOneConfig(cc, optName);
    }
  }

  private class Info {
    String name;
    String language;
    String props;

    public Info(String language) {
      this(language, language, null);
    }

    public Info(String language, String name, String props) {
      this.language = language;
      this.name = name;
      this.props = props;
    }

    public String toString() {
      return language + " : " + name + " : " + props;
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
  }

  @Test
  public void testCopyProject() {
    testCreate();
    DatabaseImpl spanish = getDatabaseLight("spanish", true);
    CopyToPostgres copyToPostgres = new CopyToPostgres();
    copyToPostgres.createProjectIfNotExists(spanish, copyToPostgres.getCC("spanish"), null, "");
  }

  @Test
  public void testCopyUserExercises() {
    testCreate();
    DatabaseImpl spanish = getDatabaseLight("spanish", true);
    // new CopyToPostgres().copyOnlyUserExercises(spanish);
  }

  @Test
  public void testCopyUserJoinExercises() {
    testCreate();
    DatabaseImpl spanish = getDatabaseLight("spanish", true);
    //   new CopyToPostgres().copyUserExListJoin(spanish);
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
      List<WordScore> wordScoresForUser = database.getAnalysis(id).getWordScoresForUser(4, id, 1);
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
    for (SlickUserProject up : spanish.getUserProjectDAO().getAll()) {
      logger.info("got " + up);
    }
  }

  @Test
  public void testMostRecentProject() {
    DatabaseImpl spanish = getDatabaseLight("netprof", false);

    User byID = spanish.getUserDAO().getUserByID("gvidaver");
    logger.info("user is " + byID);
    int i = spanish.getUserProjectDAO().mostRecentByUser(byID.getId());
    logger.info("most recent is " + i);
    i = spanish.getUserProjectDAO().mostRecentByUser(999999);
    logger.info("most recent is " + i);
    i = spanish.getUserProjectDAO().mostRecentByUser(342);
    logger.info("most recent is " + i);
  }


  @Test
  public void testProjects() {
    DatabaseImpl spanish = getDatabaseLight("spanish", false);
    SlickProject next = spanish.getProjectDAO().getAll().iterator().next();

  }

  private static DBConnection getConnection(String config) {
    File file = new File("war" + File.separator + "config" + File.separator + config + File.separator + "quizlet.properties");
    String parent = file.getParentFile().getAbsolutePath();
    logger.info("path is " + parent);
    ServerProperties serverProps = new ServerProperties(parent, file.getName());
    return new DBConnection(serverProps.getDatabaseType(),
        serverProps.getDatabaseHost(), serverProps.getDatabasePort(), serverProps.getDatabaseName(), serverProps.getDatabaseUser(), serverProps.getDatabasePassword());
  }
}
