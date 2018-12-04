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

package mitll.langtest.server.database.copy;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.H2DatabaseImpl;
import mitll.langtest.server.database.annotation.AnnotationDAO;
import mitll.langtest.server.database.annotation.SlickAnnotationDAO;
import mitll.langtest.server.database.annotation.UserAnnotation;
import mitll.langtest.server.database.audio.SlickAudioDAO;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.phone.Phone;
import mitll.langtest.server.database.phone.PhoneDAO;
import mitll.langtest.server.database.phone.RecordWordAndPhone;
import mitll.langtest.server.database.phone.SlickPhoneDAO;
import mitll.langtest.server.database.project.DialogPopulate;
import mitll.langtest.server.database.project.IProjectDAO;
import mitll.langtest.server.database.refaudio.RefResultDAO;
import mitll.langtest.server.database.refaudio.SlickRefResultDAO;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.database.result.ResultDAO;
import mitll.langtest.server.database.result.SlickResultDAO;
import mitll.langtest.server.database.reviewed.ReviewedDAO;
import mitll.langtest.server.database.reviewed.SlickReviewedDAO;
import mitll.langtest.server.database.reviewed.StateCreator;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.server.database.userlist.*;
import mitll.langtest.server.database.word.SlickWordDAO;
import mitll.langtest.server.database.word.Word;
import mitll.langtest.server.database.word.WordDAO;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.langtest.shared.project.ProjectType;
import mitll.npdata.dao.*;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;
import static mitll.langtest.server.ServerProperties.DEFAULT_NETPROF_AUDIO_DIR;
import static mitll.langtest.server.database.copy.CopyToPostgres.ACTION.*;
import static mitll.langtest.server.database.copy.CopyToPostgres.OPTIONS.*;
import static mitll.langtest.server.database.exercise.Project.MANDARIN;

/**
 * Take an old netprof 1 website (excel spreadsheet and h2.db file) and copy its info
 * into the postgres database of netprof 2.
 * <p>
 * Note we don't copy the event table, since the events from netprof 1 aren't relevant to netprof 2.
 *
 * @param <T>
 */
public class CopyToPostgres<T extends CommonShell> {
  private static final Logger logger = LogManager.getLogger(CopyToPostgres.class);

  private static final int WARN_RID_MISSING_THRESHOLD = 10;
  private static final int WARN_MISSING_THRESHOLD = 50;
  private static final String QUIZLET_PROPERTIES = "quizlet.properties";
  public static final String DEFAULT_PROPERTIES_FILE = "netprof.properties";
  private static final String CONFIG = "config";
  private static final String NO_TRANSCRIPT_FOUND = "no transcript found";
  private static final boolean ALLOW_DELETE = false;
  //public static final int PASUYA_ID = 736;

  private static DatabaseImpl database;

  enum ACTION {
    COPY("c"),

    DROP("d"),
    DROPALL("a"),
    DROPALLBUT("b"),

    UPDATEUSER("u"),
    UPDATE("x"),
    LATEST("l"),
    CREATED("z"),
    IMPORT("i"),
    MERGE("m"),
    NORM("y"),
    RECORDINGS("r"),
    SEND("s"),
    DIALOG("g"),
    CLEANDIALOG("n"),
    LIST("p"),
    REMAP("e"),
    UNKNOWN("k");

    private final String value;

    ACTION(String value) {
      this.value = value;
    }

    String getValue() {
      return value;
    }

    public String toLower() {
      return name().toLowerCase();
    }
  }

  enum OPTIONS {
    NAME("n", "optional name for the project (different from config)"),
    OPTCONFIG("p", "optional properties file within config directory (e.g. for pashto)"),
    EVAL("e", "mark the imported project as at the eval step and use project specific audio"),
    ORDER("o", "display order among projects of the same language"),
    SKIPREFRESULT("s", "skip loading ref result table (if you want to recalculate reference audio alignment)"),
    TO("t", "to project id"),
    ONDAY("w", "on day"),
    DATABASE("d", "into database"),
    PROPERTIES("i", "optional properties file to read from within /opt/netprof/config, one-to-one for each deployed webapp");

    private final String value;
    private final String desc;

    OPTIONS(String value, String desc) {
      this.value = value;
      this.desc = desc;
    }

    String getValue() {
      return value;
    }

    public String toLower() {
      return name().toLowerCase();
    }

    public String getDesc() {
      return desc;
    }
  }

  private static final String OPT_NETPROF_ROOT = DEFAULT_NETPROF_AUDIO_DIR;
  private static final String NETPROF_CONFIG_DIR = OPT_NETPROF_ROOT + File.separator + "config/";
  //private static final String NETPROF_PROPERTIES_FULL = NETPROF_CONFIG_DIR + DEFAULT_PROPERTIES_FILE;
  private static final String OPT_NETPROF = OPT_NETPROF_ROOT + File.separator + "import";

  private long getImportDate(String config, String optionalProperties) {
    return getSinceWhenFromOldConfig(config, optionalProperties, true);
  }

  /**
   * @param config
   * @param optionalProperties
   * @param optionalName
   * @param displayOrder
   * @param isEval
   * @param skipRefResult
   * @param doUpdate
   * @param doLatest
   * @param doSinceCreated
   * @param propertiesFile
   * @see #main
   */
  private boolean copyOneConfigCommand(String config,
                                       String optionalProperties,
                                       String optionalName,
                                       int displayOrder,
                                       boolean isEval,
                                       boolean skipRefResult,
                                       boolean doUpdate,
                                       boolean doLatest,
                                       boolean doSinceCreated,
                                       String optDatabase,
                                       String propertiesFile) {
    CopyToPostgres copyToPostgres = new CopyToPostgres();

    // previous update from h2...
    long sinceWhen = 0;

    if (doUpdate) {
      sinceWhen = getSinceWhenFromOldConfig(config, optionalProperties, false);
    }

    try (DatabaseImpl databaseLight = getDatabaseLight(config, true, false, optionalProperties, OPT_NETPROF, CONFIG, optDatabase, propertiesFile)) {
      while (getDefaultUser(databaseLight) < 1) {
        try {
          sleep(1000);
          logger.info("---> no default user yet.....");
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      int defaultUser = getDefaultUser(databaseLight);
      logger.info("default user = " + defaultUser);

      String language = databaseLight.getLanguage();
      ServerProperties serverProps = databaseLight.getServerProps();
      boolean hasModel = serverProps.hasModel();

      logger.info("copyOneConfigCommand :" +
          "\n\tloading  " + language +
          "\n\thasModel " + hasModel +
          "\n\tisEval   " + isEval +
          "\n\tdoUpdate " + doUpdate +
          "\n\tmodel    " + serverProps.getCurrentModel());

      String nameToUse = optionalName == null ? language : optionalName;
      boolean checkConvert = config.toLowerCase().startsWith("mandarintrad");
      if (checkConvert) logger.info("\n\n\n\n\ndoing special mandarin trad conversion\n\n\n\n\n");

      Language languageFor = getLanguageFor(language);

      copyToPostgres.copyOneConfig(
          databaseLight,

          languageFor.getCC(),
          nameToUse,
          displayOrder,
          ProjectType.NP,
          getProjectStatus(isEval, hasModel),
          skipRefResult,
          doUpdate,
          sinceWhen,
          doLatest,
          doSinceCreated,
          checkConvert);
      return true;
    } catch (Exception e) {
      logger.error("copyOneConfigCommand : got " + e, e);
      return false;
    }
  }

  @NotNull
  private Language getLanguageFor(String name) {
    try {
      if (name.equalsIgnoreCase(MANDARIN)) name = Language.MANDARIN.name();
      return Language.valueOf(name);
    } catch (IllegalArgumentException e) {
      logger.error("no known language  " + name);
      return Language.UNKNOWN;
    }
  }


  /**
   * Look under oldConfig for date.
   *
   * @param config
   * @param optionalProperties
   * @return
   */
  private long getSinceWhenFromOldConfig(String config, String optionalProperties, boolean closeDB) {
    long sinceWhen = 0;

    DatabaseImpl databaseLight = getDatabaseLight(config, true, false, optionalProperties,
        OPT_NETPROF, "oldConfig", null, DEFAULT_PROPERTIES_FILE);

    if (databaseLight == null) logger.warn("no old config under " + OPT_NETPROF);
    else {
      {
        ResultDAO resultDAO = new ResultDAO(databaseLight);
        List<Result> results = resultDAO.getResults();
        logger.info("got " + results.size() + " results");
        for (Result result : results) {
          if (result != null && result.getTimestamp() > sinceWhen) sinceWhen = result.getTimestamp();
        }
      }

      logger.info("latest result " + new Date(sinceWhen));

      {
        List<Result> results1 = new RefResultDAO(databaseLight, false).getResults();
        logger.info("got " + results1.size() + " ref results");
        long maxRes = 0;

        for (Result result : results1) {
          if (result != null && result.getTimestamp() > sinceWhen) sinceWhen = result.getTimestamp();
          if (result != null && result.getTimestamp() > maxRes) maxRes = result.getTimestamp();
        }
        logger.info("latest result " + new Date(sinceWhen) + " ref result " + new Date(maxRes));
      }

    }
    if (closeDB) databaseLight.close();
    logger.info("UPDATE : Since when " + new Date(sinceWhen));
    return sinceWhen;
  }

  @NotNull
  private ProjectStatus getProjectStatus(boolean isEval, boolean hasModel) {
    ProjectStatus status = ProjectStatus.PRODUCTION;
    if (!hasModel) status = ProjectStatus.DEVELOPMENT;
    else if (isEval) status = ProjectStatus.EVALUATION;
    return status;
  }

  @NotNull
  private CreateProject getCreateProject(ServerProperties serverProps) {
    return new CreateProject(serverProps.getHydra2Languages());
  }

/*  private void dropOneConfig(Integer projid) {
    DatabaseImpl database = getDatabase();
    IProjectDAO projectDAO = database.getProjectDAO();
    if (projectDAO.exists(projid)) {
      logger.info("Dropping #" + projid + " please wait...");
      long then = System.currentTimeMillis();
      //projectDAO.delete(projid);
      database.dropProject(projid);
      reportAfterDelete(projid, projectDAO, then, "drop");
    } else {
      logger.error("no project with that id");
    }

    database.close();
  }*/

/*  private void dropAllButOneConfig(Integer projid) {
    DatabaseImpl database = getDatabase();
    IProjectDAO projectDAO = database.getProjectDAO();
    if (projectDAO.exists(projid)) {
      logger.info("Dropping ALL PROJECTS except #" + projid + " please wait... might be awhile...");
      long then = System.currentTimeMillis();
      projectDAO.deleteAllBut(projid);
      reportAfterDelete(projid, projectDAO, then, "drop all but ");
    } else {
      logger.error("no project with that id");
    }

    database.close();
  }*/

  private void merge(int from, int to, String propertiesFile) {
    database = getDatabase(propertiesFile);
    Project fProject = database.getProject(from);
    Project tProject = database.getProject(to);
    logger.info("merging " +
        "\n\tproject " + fProject +
        "\n\tinto    " + tProject);
    if (fProject == null) {
      logger.warn("no from project by " + from);
      return;
    }
    if (tProject == null) {
      logger.warn("no to project by " + to);
      return;
    }
    if (!fProject.getLanguage().equalsIgnoreCase(tProject.getLanguage())) {
      logger.error("nope - not same language " + fProject.getLanguage() + " vs " + tProject.getLanguage());
      return;
    }
    boolean isChinese = fProject.getLanguageEnum() == Language.MANDARIN && tProject.getLanguageEnum() == Language.MANDARIN;
    database.updateProject(from, to, isChinese);
    database.close();
  }

  private void mergeRecordings(int from, int to, Date onDay, String propertiesFile) {
    database = getDatabase(propertiesFile);
    Project fProject = database.getProject(from);
    Project tProject = database.getProject(to);
    logger.info("merging " +
        "\n\tproject " + fProject +
        "\n\tinto    " + tProject);
    if (fProject == null) {
      logger.warn("no from project by " + from);
      return;
    }
    if (tProject == null) {
      logger.warn("no to project by " + to);
      return;
    }
    if (!fProject.getLanguage().equalsIgnoreCase(tProject.getLanguage())) {
      logger.error("nope - not same language " + fProject.getLanguage() + " vs " + tProject.getLanguage());
      return;
    }
    database.updateRecordings(from, to, onDay);
    database.close();
  }

  private void reportAfterDelete(int config, IProjectDAO projectDAO, long then, String drop) {
    long now = System.currentTimeMillis();

    Collection<SlickProject> all = projectDAO.getAll();
    logger.info("reportAfterDelete Took " + (now - then) + " millis to " + drop + " #" + config + ", now there are " + all.size() + " projects:");

    all.forEach(project -> logger.info("\t" + project));
  }

  private static DatabaseImpl getDatabase(String propertiesFile) {
    ServerProperties serverProps = getProps(propertiesFile);
    String war = "war";
    return new DatabaseImpl(getProps(propertiesFile), getPathHelper(war, serverProps), null, null);
  }

  @NotNull
  private static PathHelper getPathHelper(String war, ServerProperties serverProps) {
    return new PathHelper(war, serverProps);
  }

  private static DatabaseImpl getSimpleDatabase() {
    return new DatabaseImpl(getProps(DEFAULT_PROPERTIES_FILE));
  }

  private static ServerProperties getProps(String propertiesFile) {
    File file = new File(NETPROF_CONFIG_DIR, propertiesFile);
    if (!file.exists()) logger.error("can't find " + file.getAbsolutePath());
    String parent = file.getParentFile().getAbsolutePath();
    return new ServerProperties(parent, file.getName());
  }

  /**
   * @param config
   * @param useH2
   * @param optPropsFile
   * @param installPath
   * @param optDatabase
   * @param propertiesFile
   * @return null if can't find the config file
   * @see #copyOneConfigCommand
   */
  public static DatabaseImpl getDatabaseLight(String config,
                                              boolean useH2,
                                              boolean useLocal,
                                              String optPropsFile,
                                              String installPath,
                                              String rootConfigDir,
                                              String optDatabase, String propertiesFile) {
    // logger.info("getDatabaseLight db " + config + " optional props " + optPropsFile);
    String propsFile = optPropsFile != null ? optPropsFile : QUIZLET_PROPERTIES;

    logger.info("getDatabaseLight db " + config + " props " + propsFile);

    //String rootConfigDir = CONFIG;
    File configFile = new File(installPath + File.separator + rootConfigDir + File.separator + config + File.separator + propsFile);

    logger.info("getDatabaseLight path " + configFile.getAbsolutePath());

    ServerProperties serverProps = getServerProperties(config, propsFile, installPath);
    if (serverProps == null) {
      return null;
    }

    // String propertiesFile = DEFAULT_PROPERTIES_FILE;
    readProps(serverProps, getServerProperties("", propertiesFile, OPT_NETPROF_ROOT));

    if (useLocal) {
      serverProps.setLocalPostgres();
    }

    serverProps.setH2(useH2);

    if (optDatabase != null) {
      logger.info("\n\n\n\nusing " + optDatabase + " optional database\n\n\n");
      serverProps.setDBConfig(optDatabase);
    }

    String parent = configFile.getParentFile().getAbsolutePath();
    String name = configFile.getName();

    logger.info("getDatabaseLight parent " + parent + " name " + name);
    DatabaseImpl database = new H2DatabaseImpl(
        configFile.getParentFile().getAbsolutePath(),
        serverProps.getH2Database(),
        serverProps,
        getPathHelper(installPath, serverProps), false, null, false);

    database.setInstallPath(
        configFile.getParentFile().getAbsolutePath() + File.separator +
            database.getServerProps().getLessonPlan(),
        null);

    return database;
  }

  private static void readProps(ServerProperties serverProps, ServerProperties serverProps2) {
    String configFileFullPath = serverProps2.getConfigFileFullPath();
    try {
      logger.info("readProps reading from " + configFileFullPath);
      FileInputStream inStream = new FileInputStream(configFileFullPath);
      serverProps.getProps().load(inStream);
      inStream.close();
    } catch (Exception e) {
      logger.error("readProps can't find " + configFileFullPath);
    }
  }

  /**
   * @param config
   * @param propsFile
   * @param installPath
   * @return
   * @see #getDatabaseLight
   */
  private static ServerProperties getServerProperties(String config, String propsFile, String installPath) {
    String configDir = config.isEmpty() ? config : config + File.separator;
    File file = new File(installPath + File.separator + CONFIG + File.separator + configDir + propsFile);

    if (!file.exists()) {
      logger.error("\n\ngetServerProperties can't find config file " + file.getAbsolutePath());
      // no recovery!
      return null;
    } else {
      return new ServerProperties(file.getParentFile().getAbsolutePath(), file.getName());
    }
  }

  /**
   * @param db             to read from
   * @param cc             country code
   * @param optName        non-default name (not language) - null OK
   * @param projectType
   * @param status         i.e. not production
   * @param skipRefResult
   * @param doUpdate
   * @param oldSinceWhen
   * @param doLatest
   * @param doSinceCreated
   * @see #copyOneConfigCommand
   */
  public void copyOneConfig(DatabaseImpl db,
                            String cc,
                            String optName,
                            int displayOrder,
                            ProjectType projectType,
                            ProjectStatus status,
                            boolean skipRefResult,
                            boolean doUpdate,
                            long oldSinceWhen,
                            boolean doLatest,
                            boolean doSinceCreated,
                            boolean checkConvert) throws Exception {
    long then = System.currentTimeMillis();
    Collection<String> typeOrder = db.getTypeOrder(DatabaseImpl.IMPORT_PROJECT_ID);

    logger.info("copyOneConfig" +
        "\n\tproject            " + optName +
        "\n\tcc                  " + cc +
        "\n\ttype order          " + typeOrder +
        "\n\tdoUpdate            " + doUpdate +
        "\n\tfor import project id " + DatabaseImpl.IMPORT_PROJECT_ID);

    long sinceWhen = 0;


    CreateProject createProject = getCreateProject(db);
    int projectID = doUpdate ?
        createProject.getExisting(db, optName) :
        createProjectIfNotExists(db, cc, optName, displayOrder, typeOrder, projectType, status);  // TODO : course?

    logger.info("copyOneConfig" +
        "\n\tproject #             " + projectID +
        "\n\ttype order            " + typeOrder +
        "\n\tfor import project id " + DatabaseImpl.IMPORT_PROJECT_ID);

    if (doUpdate) {
      if (projectID == -1) {
        logger.error("no project found for " + createProject.getOldLanguage(db) + " or " + optName);
      } else {
        if (doLatest) {
          oldSinceWhen = createProject.getSinceWhenResults(db, projectID);
        }
        if (doSinceCreated) {
          oldSinceWhen = createProject.getSinceCreated(db, projectID);
        }
        sinceWhen = Math.max(createProject.getSinceWhenLastNetprof(db, projectID), oldSinceWhen);
        logger.info("UPDATE : sinceWhen is " + new Date(sinceWhen));
        long maxTime = copyAllTables(db, optName, status, skipRefResult, typeOrder, projectID, sinceWhen, checkConvert);
        if (maxTime == 0) {
          logger.info("UPDATE : no change - no updated results\n\n\n ");
        } else {
          logger.info("UPDATE : latest result or audio is " + new Date(maxTime));
          createProject.updateNetprof(db, projectID, maxTime);
        }
        db.close();
      }
    } else {
      // first add the user table
      // check once if we've added it before
      if (db.getUserExerciseDAO().isProjectEmpty(projectID)) {
        long maxTime = copyAllTables(db, optName, status, skipRefResult, typeOrder, projectID, sinceWhen, checkConvert);
        logger.info("CREATE : latest result or audio is " + new Date(maxTime));
        createProject.updateNetprof(db, projectID, maxTime);
      } else {
        logger.warn("\n\n\nProject #" + projectID + " (" + optName + ") already has exercises in it.  Not loading again...\n\n\n");
      }
      db.close();
    }
    long now = System.currentTimeMillis();

    logger.info("copyOneConfig took " + ((now - then) / 1000) + " seconds to load " + optName);
  }

  @NotNull
  private CreateProject getCreateProject(DatabaseImpl db) {
    return getCreateProject(db.getServerProps());
  }

  private long copyAllTables(DatabaseImpl db, String optName, ProjectStatus status, boolean skipRefResult,
                             Collection<String> typeOrder, int projectID,
                             long sinceWhen,
                             boolean checkConvert) throws Exception {
    if (sinceWhen > 0) logger.info("\n\n\n only changes since " + (new Date(sinceWhen)));

    ResultDAO resultDAO = new ResultDAO(db);
    Map<Integer, Integer> oldToNewUser = new UserCopy().copyUsers(db, projectID, resultDAO, optName, status);

    Map<Integer, String> idToFL = new HashMap<>();

    logger.info("copyOneConfig type order  " + typeOrder);

    if (typeOrder.isEmpty()) logger.error("huh? type order is empty????\\n\n\n");
    Map<String, Integer> parentExToChild = new HashMap<>();
    Map<String, Integer> exToID =
        copyUserAndPredefExercisesAndLists(db, projectID, oldToNewUser, typeOrder, sinceWhen, idToFL, parentExToChild, checkConvert);

    SlickResultDAO slickResultDAO = (SlickResultDAO) db.getResultDAO();

    long maxTime = copyResult(slickResultDAO, oldToNewUser, projectID, exToID, resultDAO, idToFL,
        db.getUserExerciseDAO().getUnknownExerciseID(),
        getDefaultUser(db), sinceWhen);

    logger.info("oldToNewUser num = " + oldToNewUser.size() + " exToID num = " + exToID.size());

    Set<Long> maxTimeAudio = new HashSet<>();
    // add the audio table
    Map<String, Integer> pathToAudioID = copyAudio(db, oldToNewUser, exToID, parentExToChild, projectID, sinceWhen, maxTimeAudio, checkConvert, idToFL);//, toConvertToAudio);
    // logger.info("pathToAudioID num = " + pathToAudioID.size());

    // copy ref results
    if (!skipRefResult) {
      copyRefResult(db, oldToNewUser, exToID, pathToAudioID, projectID, sinceWhen);
    }

    // TODO
    // TODO : fill in for words and phones that are since some moment...
    // copy results, words, and phones
    {
      Map<Integer, Integer> oldToNewResult = sinceWhen > 0 ?
          slickResultDAO.getOldToNewSince(projectID, sinceWhen) :
          slickResultDAO.getOldToNew(projectID);

      if (oldToNewResult.isEmpty()) {
        logger.error("\n\n\nold to new result is EMPTY!");
      }
      Map<Integer, Integer> oldToNewWordID = copyWordsAndGetIDMap(db, oldToNewResult, projectID);

      // phone DAO
      copyPhone(db, oldToNewResult, oldToNewWordID, projectID);
    }

    // anno DAO
    copyAnno(db, db.getUserDAO(), oldToNewUser, exToID, sinceWhen);

    copyReviewed(db, oldToNewUser, exToID, true, sinceWhen);
    copyReviewed(db, oldToNewUser, exToID, false, sinceWhen);

    if (!maxTimeAudio.isEmpty()) {
      Long next = maxTimeAudio.iterator().next();
      if (next > maxTime) maxTime = next;
    }
//    if (maxTime == 0) maxTime = System.currentTimeMillis();
    return maxTime;
  }

  private int getDefaultUser(DatabaseImpl db) {
    return db.getUserDAO().getDefaultUser();
  }

  /**
   * TODO : will get slower every time...
   *
   * @param db
   * @param oldToNewResult
   * @param projID
   * @return
   */
  @NotNull
  private Map<Integer, Integer> copyWordsAndGetIDMap(DatabaseImpl db, Map<Integer, Integer> oldToNewResult, int projID) {
    logger.info("copyWordsAndGetIDMap oldToNewResult " + oldToNewResult.size());
    SlickWordDAO slickWordDAO = (SlickWordDAO) db.getWordDAO();
    copyWord(db, oldToNewResult, slickWordDAO, projID);

    Map<Integer, Integer> oldToNewWordID = slickWordDAO.getOldToNew();
    logger.info("copyWordsAndGetIDMap old to new word id  " + oldToNewWordID.size());
    return oldToNewWordID;
  }

/*  private void copyEvents(DatabaseImpl db, int projectID, Map<Integer, Integer> oldToNewUser, Map<String, Integer> exToID) {
    if (COPY_EVENTS) {
      {
        int defectDetector = db.getUserDAO().getDefectDetector();
        EventDAO other = new EventDAO(db, defectDetector);
        ((SlickEventImpl) db.getEventDAO()).copyTableOnlyOnce(other, projectID, oldToNewUser, exToID);
      }
    }
  }*/

  private int createProjectIfNotExists(DatabaseImpl db,
                                       String cc,
                                       String optName,
                                       int displayOrder,
                                       Collection<String> typeOrder,
                                       ProjectType projectType,
                                       ProjectStatus status) {
    return getCreateProject(db)
        .createProjectIfNotExists(db, cc, optName, "", displayOrder, typeOrder, projectType, status);
  }

  /**
   * @param db
   * @param projectID
   * @param oldToNewUser
   * @param typeOrder
   * @param sinceWhen
   * @param idToFL
   * @param checkConvert
   * @return map of parent exercise to context sentence
   * @see #copyOneConfig
   */
  private Map<String, Integer> copyUserAndPredefExercisesAndLists(DatabaseImpl db,
                                                                  int projectID,
                                                                  Map<Integer, Integer> oldToNewUser,
                                                                  Collection<String> typeOrder,
                                                                  long sinceWhen,


                                                                  Map<Integer, String> idToFL,
                                                                  Map<String, Integer> parentToChild, boolean checkConvert) {
    boolean isUpdate = sinceWhen > 0;

    Map<String, Integer> exToID = isUpdate ?
        new ExerciseCopy().getOldToNewExIDs(db, projectID) :
        new ExerciseCopy().copyUserAndPredefExercises(db, oldToNewUser, projectID, idToFL, typeOrder, parentToChild, checkConvert);

    if (isUpdate) {
      List<CommonExercise> exercises = db.getExercises(projectID, true);

      exercises.forEach(commonExercise -> idToFL.put(commonExercise.getID(), commonExercise.getForeignLanguage()));
      exercises.forEach(commonExercise -> commonExercise.getDirectlyRelated().forEach(context -> {
        parentToChild.put(commonExercise.getOldID(), context.getID());
      }));

      logger.info("copyUserAndPredefExercisesAndLists now idToFl " + idToFL.size() + " and parentToChild " + parentToChild.size());
    }


    SlickUserListDAO slickUserListDAO = (SlickUserListDAO) db.getUserListManager().getUserListDAO();
    Set<Integer> includedOldLists = copyUserExerciseList(db, oldToNewUser, slickUserListDAO, projectID, sinceWhen);

    Map<Integer, Integer> oldToNewUserList = slickUserListDAO.getOldToNew(projectID);
    copyUserExerciseListVisitor(db, oldToNewUser, oldToNewUserList, (SlickUserListExerciseVisitorDAO) db.getUserListManager().getVisitorDAO(), sinceWhen);
    copyUserExListJoin(db, exToID, projectID, includedOldLists);
    return exToID;
  }

  private void copyUserExListJoin(DatabaseImpl db, Map<String, Integer> exToInt, int projectid, Set<Integer> includedOldLists) {
    SlickUserListDAO slickUserListDAO = (SlickUserListDAO) db.getUserListManager().getUserListDAO();
    Map<Integer, Integer> oldToNewUserList = slickUserListDAO.getOldToNew(projectid);
    copyUserExerciseListJoin(db, oldToNewUserList, exToInt, includedOldLists);
  }

  /**
   * TODO : replace config/language/bestAudio (E.g. farsi) with bestAudio
   *
   * @param db
   * @param oldToNewUser
   * @param exToID
   * @param projid
   * @param sinceWhen
   * @see #copyAllTables(DatabaseImpl, String, ProjectStatus, boolean, Collection, int, long, boolean)
   */
  private Map<String, Integer> copyAudio(DatabaseImpl db,
                                         Map<Integer, Integer> oldToNewUser,
                                         Map<String, Integer> exToID,
                                         Map<String, Integer> parentExToChild,
                                         int projid,
                                         long sinceWhen,
                                         Set<Long> maxTime,
                                         boolean fixTranscript,
                                         Map<Integer, String> idToFL

  ) {
    SlickAudioDAO slickAudioDAO = (SlickAudioDAO) db.getAudioDAO();

    List<SlickAudio> bulk = new ArrayList<>();
    List<AudioAttribute> audioAttributes = db.getH2AudioDAO()
        .getAudioAttributesByProjectThatHaveBeenChecked(projid, false)
        .stream().filter(audioAttribute -> audioAttribute.getTimestamp() > sinceWhen).collect(Collectors.toList());

    //  audioAttributes.addAll(toConvertToAudio);
    logger.info("copyAudio h2 audio  " + audioAttributes.size());// + " added " + toConvertToAudio.size());
    int missing = 0;
    int skippedMissingUser = 0;
    Set<String> missingExIDs = new TreeSet<>();
    long max = 0;
    for (AudioAttribute att : audioAttributes) {
      String oldexid = att.getOldexid();

      if (att.getTimestamp() > max) {
        max = att.getTimestamp();
      }

      Integer id = getModernIDForExercise(exToID, parentExToChild, att, oldexid);
      if (id != null) {
        att.setExid(id); // set the exercise id reference - either to a normal exercise or to a context exercise

        if (fixTranscript) {
          String exTrans = idToFL.get(id);
          String transcript = att.getTranscript();
          if (!transcript.equalsIgnoreCase(exTrans)) {
            logger.info("copyAudio change for " + id + " old " + oldexid +
                "\n\tfrom " + transcript +
                "\n\tto   " + exTrans);

            att.setTranscript(exTrans);
          }
        }

        {
          SlickAudio slickAudio = slickAudioDAO.getSlickAudio(att, oldToNewUser, projid);

          if (slickAudio != null) {
            bulk.add(slickAudio);
          } else {
            skippedMissingUser++;
          }
        }
      } else {
        missingExIDs.add(oldexid);
        if (missing < WARN_MISSING_THRESHOLD) logger.warn("copyAudio missing ex for " + att + " : " + oldexid);
        missing++;
      }
    }

    long then = System.currentTimeMillis();
    logger.info("copyAudio start    adding bulk : " + bulk.size() + " audio... " + skippedMissingUser + " were skipped due to missing user");
    slickAudioDAO.addBulk(bulk);
    logger.info("copyAudio finished adding bulk : " + bulk.size() + " audio...");

    long now = System.currentTimeMillis();

    if (missing > 0) {
      logger.warn("copyAudio had " + missing + "/" + audioAttributes.size() + " audio att due to missing ex fk : (" + missingExIDs.size() +
          ") : " +
          "" + missingExIDs);
    }

    Map<String, Integer> pairs = slickAudioDAO.getPairs(projid);
    logger.info("copyAudio took " + (now - then) + " for project " + projid + " , postgres audio count = " + pairs.size());

    maxTime.add(max);
    return pairs;
  }

  /**
   * @param exToID
   * @param parentExToChild
   * @param att
   * @param oldexid
   * @return
   * @see #copyAudio
   */
  private Integer getModernIDForExercise(Map<String, Integer> exToID, Map<String, Integer> parentExToChild, AudioAttribute att, String oldexid) {
    Integer id = exToID.get(oldexid);
    if (att.isContextAudio()) {
//      logger.info("copyAudio att " + att.getUniqueID() + " for ex  " + oldexid + " is context " + att.getAudioRef());
      Integer childID = parentExToChild.get(oldexid);
      if (childID == null) {
        //logger.info("copyAudio huh? no child for " + oldexid);
      } else {
        id = childID;
      }
    }
    return id;
  }

  /**
   * TODOx : set the exid int field
   *
   * @param db
   * @param dominoUserDAO
   * @param oldToNewUser
   * @param exToID
   * @param sinceWhen
   */
  private void copyAnno(DatabaseImpl db, IUserDAO dominoUserDAO, Map<Integer, Integer> oldToNewUser,
                        Map<String, Integer> exToID,
                        long sinceWhen) {
    SlickAnnotationDAO annotationDAO = (SlickAnnotationDAO) db.getAnnotationDAO();
    List<SlickAnnotation> bulk = new ArrayList<>();
    int missing = 0;
    Set<Long> missingUsers = new HashSet<>();

    Collection<UserAnnotation> all = new AnnotationDAO(db, dominoUserDAO)
        .getAll()
        .stream()
        .filter(userAnnotation -> userAnnotation.getTimestamp() > sinceWhen).collect(Collectors.toList());

    for (UserAnnotation annotation : all) {
      long creatorID = annotation.getCreatorID();
      Integer userID = oldToNewUser.get((int) creatorID);
      if (userID == null) {
        boolean add = missingUsers.add(creatorID);
        if (add) logger.warn("copyAnno no user " + creatorID);
      } else {
        annotation.setCreatorID(userID);
        Integer realID = exToID.get(annotation.getOldExID());
        if (realID == null) {
          missing++;
        } else {
          annotation.setExerciseID(realID);
          bulk.add(annotationDAO.toSlick(annotation));
        }
      }
    }
    if (missing > 0) logger.warn("missing " + missing + " out of " + all.size() + " users " + missingUsers);
    annotationDAO.addBulk(bulk);
  }

  private void copyPhone(DatabaseImpl db,
                         Map<Integer, Integer> oldToNewResult,
                         Map<Integer, Integer> oldToNewWordID,
                         int projID) {
    SlickPhoneDAO slickPhoneAO = (SlickPhoneDAO) db.getPhoneDAO();
    PhoneDAO phoneDAO = new PhoneDAO(db);

    List<SlickPhone> bulk = new ArrayList<>();
    int c = 0;
    int d = 0;

    Set<Integer> missingrids = new TreeSet<>();
    Set<Integer> missingwids = new TreeSet<>();
    for (Phone phone : phoneDAO.getAll(projID)) {
      int rid1 = phone.getRid();
      Integer rid = oldToNewResult.get(rid1);
      if (rid == null) {
        if (c++ < 50 && missingrids.add(rid1)) logger.warn("copyPhone phone : no rid " + rid1);
      } else {
        int wid1 = phone.getWid();
        Integer wid = oldToNewWordID.get(wid1);

        if (wid == null) {
          if (d++ < 50 && missingwids.add(wid1)) logger.warn("copyPhone phone : no word id " + wid1);
        } else {
          phone.setRID(rid);
          phone.setWID(wid);
          bulk.add(slickPhoneAO.toSlick(phone, projID));
        }
      }
    }
    if (c > 0) logger.warn("missing result id fks " + c + " : " + missingrids);
    if (d > 0) logger.warn("missing word   id fks " + d + " : " + missingwids);

    long then = System.currentTimeMillis();
    logger.info("copyPhone adding " + bulk.size());
    slickPhoneAO.addBulk(bulk);
    logger.info("copyPhone now has " + slickPhoneAO.getNumRows() + " took " + (System.currentTimeMillis() - then) + " millis.");
  }

  private final Set<Integer> missingRIDs = new HashSet<>();

  /**
   * @param db
   * @param oldToNewResult
   * @param slickWordDAO
   * @param projID
   * @see #copyOneConfig
   */
  private void copyWord(DatabaseImpl db,
                        Map<Integer, Integer> oldToNewResult,
                        SlickWordDAO slickWordDAO,
                        int projID) {
    long then = System.currentTimeMillis();
    List<SlickWord> bulk = new ArrayList<>();
    for (Word word : new WordDAO(db).getAll(projID)) {
      Integer rid = oldToNewResult.get((int) word.getRid());
      if (rid == null) {
        boolean add = missingRIDs.add(word.getRid());
        if (add && missingRIDs.size() < WARN_RID_MISSING_THRESHOLD)
          logger.warn("copyWord word has no rid " + word.getRid());
      } else {
        word.setRid(rid);
        bulk.add(slickWordDAO.toSlick(word));
      }
    }
    if (missingRIDs.size() > 0) logger.warn("word : missing " + missingRIDs.size() + " result id fk references");

    slickWordDAO.addBulk(bulk);
    long now = System.currentTimeMillis();

    logger.info("copyWord copy word - copied " + bulk.size() + " in " + (now - then) / 1000 + " seconds.");
  }

  /**
   * TODO : check for empty by project
   *
   * @param db
   * @param oldToNewUserList
   * @see #copyUserExListJoin
   */
  private void copyUserExerciseListJoin(DatabaseImpl db,
                                        Map<Integer, Integer> oldToNewUserList,
                                        Map<String, Integer> exToInt,
                                        Set<Integer> includedOldLists
  ) {
    SlickUserListExerciseJoinDAO slickUserListExerciseJoinDAO = (SlickUserListExerciseJoinDAO) db.getUserListManager().getUserListExerciseJoinDAO();

    Collection<UserListExerciseJoinDAO.Join> all = new UserListExerciseJoinDAO(db).getAll();
    List<UserListExerciseJoinDAO.Join> filtered = all
        .stream()
        .filter(join -> includedOldLists.contains(join.getUserlistid()))
        .collect(Collectors.toList());

    logger.info("copyUserExerciseListJoin copying " + filtered.size() + " exercise->list joins from total " + all.size());

    for (UserListExerciseJoinDAO.Join join : filtered) {
      int oldID = join.getUserlistid();
      Integer userListID = oldToNewUserList.get(oldID);
      if (userListID == null) {
        logger.warn("copyUserExerciseListJoin UserListManager join can't find user list " + oldID + " in " + oldToNewUserList.size());
      } else {
        String exerciseID = join.getExerciseID();
        Integer id = exToInt.get(exerciseID);
//        CommonExercise customOrPredefExercise = null;
        if (id == null) {
          if (!exerciseID.startsWith("Custom")) {
            logger.warn("copyUserExerciseListJoin Can't find exercise " + exerciseID + " in " + exToInt.size() + " ex->int map");
          }
        } else {
          //customOrPredefExercise = db.getCustomOrPredefExercise(projectid, id);
          slickUserListExerciseJoinDAO.addPair(userListID, id);
        }
//          logger.info("Adding user exercise join : " +join.userlistid + " adding " + exerciseID + " : " +customOrPredefExercise);
//        if (customOrPredefExercise == null) {
//          logger.error("can't find " + exerciseID + " in " + db.getExercises(projectid).size() + " exercises");
//        } else {
//          slickUserListExerciseJoinDAO.addPair(userListID, customOrPredefExercise.getID());
//        }
      }
    }
  }

  /**
   * TODO : check for empty by project
   *
   * @param db
   * @param oldToNewUser
   * @param slickUserListDAO
   * @param sinceWhen
   * @see #copyUserAndPredefExercisesAndLists
   */
  private Set<Integer> copyUserExerciseList(DatabaseImpl db,
                                            Map<Integer, Integer> oldToNewUser,
                                            SlickUserListDAO slickUserListDAO,
                                            int projid, long sinceWhen) {
    Collection<UserList<CommonShell>> oldUserLists = new UserListDAO(db, new UserDAO(db)).getAll();
    int count = 0;
    logger.info("copyUserExerciseList copying " + oldUserLists.size() + " user exercise lists");
    List<SlickUserExerciseList> bulk = new ArrayList<>();

    List<UserList<CommonShell>> filtered =
        oldUserLists
            .stream()
            .filter(commonShellUserList -> commonShellUserList.getModified() > sinceWhen)
            .collect(Collectors.toList());
    Set<Integer> oldUserListIncluded = new HashSet<>();
    for (UserList<CommonShell> list : filtered) {
      int oldID = list.getUserID();
      Integer newUserID = oldToNewUser.get(oldID);
      if (newUserID == null) {
        logger.warn("UserListManager can't find user " + oldID + " in " + oldToNewUser.size());
      } else {
        oldUserListIncluded.add(oldID);
        SlickUserExerciseList user = slickUserListDAO.toSlick2(list, newUserID, projid, -1);
        bulk.add(user);
        count++;
      }
    }
    slickUserListDAO.addBulk(bulk);
    logger.info("copyUserExerciseList copied  " + count + " user exercise lists");
    return oldUserListIncluded;
  }

  /**
   * TODO : check for empty by project
   *
   * @param db
   * @param oldToNewUser
   * @param oldToNewUserList
   * @param visitorDAO
   * @param sinceWhen
   */
  private void copyUserExerciseListVisitor(DatabaseImpl db,
                                           Map<Integer, Integer> oldToNewUser,
                                           Map<Integer, Integer> oldToNewUserList,
                                           SlickUserListExerciseVisitorDAO visitorDAO,
                                           long sinceWhen) {
    UserExerciseListVisitorDAO uelDAO = new UserExerciseListVisitorDAO(db);
    Collection<UserExerciseListVisitorDAO.Pair> all = uelDAO.getAll();
    logger.info("copying " + all.size() + " user exercise list visitors");
    List<UserExerciseListVisitorDAO.Pair> filtered =
        all
            .stream()
            .filter(pair -> pair.getWhen() > sinceWhen)
            .collect(Collectors.toList());

    for (UserExerciseListVisitorDAO.Pair pair : filtered) {
      int oldID = pair.getUser();
      Integer currentUser = oldToNewUser.get(oldID);
      if (currentUser == null) {
        logger.error("UserListManager can't find user " + oldID);
      } else {
        int olduserlist = pair.getListid();
        Integer current = oldToNewUserList.get(olduserlist);
        if (current == null) logger.error("can't find user list id " + olduserlist);
        else {
          visitorDAO.add(current, currentUser, pair.getWhen());
        }
      }
    }
  }

  /**
   * Make sure all results are copied, even when we have missing user id or exercise references.
   *
   * @param slickResultDAO
   * @param oldToNewUser
   * @param projid
   * @param exToID
   * @param resultDAO
   * @param sinceWhen
   * @see #copyAllTables
   */
  private long copyResult(
      SlickResultDAO slickResultDAO,
      Map<Integer, Integer> oldToNewUser,
      int projid,
      Map<String, Integer> exToID,
      ResultDAO resultDAO,
      Map<Integer, String> idToFL,
      int unknownExerciseID,
      int unknownUserID,
      long sinceWhen
  ) {
    List<SlickResult> bulk = new ArrayList<>();

    List<Result> results = resultDAO.getResults();
    List<Result> filtered = getSinceWhenFromOldConfig(results, sinceWhen);
    logger.info("copyResult " + projid + " : copying " + results.size() + " results, filtered to " + filtered.size() + " since " + new Date(sinceWhen));

    //  int missing = 0;
    int missing2 = 0;

    long maxTime = 0;
    int noTransCount = 0;
    logger.info("copyResult id->fl has " + idToFL.size() + " items");

    Set<Integer> missingUserIDs = new HashSet<>();

    // Map<String, Result> oldToResultID = new HashMap<>();
    for (Result result : filtered) {
      int oldUserID = result.getUserid();
      // boolean isPasuya = oldUserID == PASUYA_ID;

      String oldExID = result.getOldExID();
/*      if (checkConvert && isPasuya) {
      //  logger.info("found candidate " + result);
        Result prevRecording = oldToResultID.get(oldExID);
        if (prevRecording == null || result.getPronScore() > prevRecording.getPronScore()) {
          oldToResultID.put(oldExID, result);
        }
      }*/
      if (maxTime < result.getTimestamp()) maxTime = result.getTimestamp();
      Integer userID = oldToNewUser.get(oldUserID);
      if (userID == null) {
        boolean add = missingUserIDs.add(oldUserID);
        if (add) {
          logger.error("copyResult no user " + oldUserID);
        }
        userID = unknownUserID;
      }

      result.setUserID(userID);
      Integer realExID = exToID.get(oldExID);

      if (realExID == null) {
        missing2++;
        realExID = unknownExerciseID;
      }
      String transcript = idToFL.get(realExID);

      boolean noTrans = transcript == null;
      if (noTrans) noTransCount++;
      String transcript1 = noTrans ? NO_TRANSCRIPT_FOUND : transcript;
//      SlickResult e = slickResultDAO.toSlick(result, projid, realExID, transcript1);
      bulk.add(slickResultDAO.toSlick(result, projid, realExID, transcript1));
      if (bulk.size() % 50000 == 0) logger.info("copyResult : made " + bulk.size() + " results...");
    }

/*
    oldToResultID.values().forEach(result -> {
      Integer realExID = exToID.get(result.getOldExID());
      if (realExID == null) logger.warn("no ex for " + result.getOldExID());
      else {
        String transcript = idToFL.get(realExID);
        String answer = result.getAnswer();
        logger.info("making audio attr for " +result.getOldExID() + " id "+ realExID + " fl '" +
            "'");
        if (answer.startsWith("answers")) {
          answer = "bestAudio" + answer.substring("Answers".length());
         // logger.info("now " + answer);
        }
        AudioAttribute e = new AudioAttribute(-1,
            PASUYA_ID,
            realExID,
            answer,
            result.getTimestamp(),
            result.getDurationInMillis(),
            AudioType.REGULAR, null,
            transcript, "", result.getDynamicRange(), 1, MiniUser.Gender.Male);
        e.setOldexid(result.getOldExID());
        toConvertToAudio.add(e);
      }
    });
*/

    //    if (missing > 0) {
//      logger.warn("skipped " + missing + "/" + results.size() +
//          "  results b/c of exercise id fk missing");
//    }
    if (missing2 > 0) {
      logger.warn("copyResult : skipped " + missing2 + "/" + results.size() +
          "  results b/c of exercise id fk missing (old->new ids)");
    }
    if (!missingUserIDs.isEmpty()) {
      logger.warn("found " + missingUserIDs.size() + " missing users " + missingUserIDs);
    }
    logger.info("copyResult adding " + bulk.size() + " results...");
    long then = System.currentTimeMillis();
    slickResultDAO.addBulk(bulk);
    long now = System.currentTimeMillis();
    logger.info("copyResult added  " + bulk.size() + " results in " + (now - then) / 1000 + " seconds.");

    if (noTransCount > 0) {
      logger.warn("copyResult no trans found for  " + noTransCount);
    }
    return maxTime;
  }

  private void copyReviewed(DatabaseImpl db,
                            Map<Integer, Integer> oldToNewUser,
                            Map<String, Integer> exToID,
                            boolean isReviewed, long sinceWhen) {
    SlickReviewedDAO dao = (SlickReviewedDAO) (isReviewed ? db.getReviewedDAO() : db.getSecondStateDAO());

    String tableName = isReviewed ? ReviewedDAO.REVIEWED : ReviewedDAO.SECOND_STATE;
    ReviewedDAO originalDAO = new ReviewedDAO(db, tableName);
    List<SlickReviewed> bulk = new ArrayList<>();

    Collection<StateCreator> all = originalDAO
        .getAll()
        .stream()
        .filter(stateCreator -> stateCreator.getWhen() > sinceWhen).collect(Collectors.toList());

    logger.info("copyReviewed found " + all.size() + " for " + tableName);
    int missing = 0;
    Set<Integer> missingUsers = new HashSet<>();

    for (StateCreator stateCreator : all) {
      int creatorID = (int) stateCreator.getCreatorID();
      Integer userID = oldToNewUser.get(creatorID);
      if (userID == null) {
        boolean add = missingUsers.add(creatorID);
        if (add) logger.warn("copyReviewed no user " + creatorID);
      } else {
        Integer exid = exToID.get(stateCreator.getOldExID());
        if (exid != null) {
          stateCreator.setExerciseID(exid);
          stateCreator.setCreatorID(userID);
          bulk.add(dao.toSlick(stateCreator));
        } else missing++;
      }
    }
    if (missing > 0) {
      logger.warn("missing " + missing + " due to missing ex id fk");
    }
    dao.addBulk(bulk);
  }

  /**
   * @param db
   * @param oldToNewUser
   * @param exToID
   * @param projid
   * @param sinceWhen
   * @see #copyOneConfig
   */
  private void copyRefResult(DatabaseImpl db,
                             Map<Integer, Integer> oldToNewUser,
                             Map<String, Integer> exToID,
                             Map<String, Integer> pathToAudioID,
                             int projid,
                             long sinceWhen) {
    SlickRefResultDAO dao = (SlickRefResultDAO) db.getRefResultDAO();
    List<SlickRefResult> bulk = new ArrayList<>();
    Collection<Result> toImport = new RefResultDAO(db, false).getResults();
    List<Result> sinceWhen1 = getSinceWhenFromOldConfig(toImport, sinceWhen);
    logger.info("copyRefResult for project " + projid + " found " + toImport.size() +
        " original ref results, " + sinceWhen1.size() + " since " + new Date(sinceWhen));

    logger.info("copyRefResult found " + oldToNewUser.size() + " oldToNewUser entries.");
    logger.info("copyRefResult found " + exToID.size() + " ex to id entries.");
    logger.info("copyRefResult found " + pathToAudioID.size() + " path to audio id entries.");
    int missing = 0;
    Set<Integer> missingUsers = new HashSet<>();
    for (Result result : sinceWhen1) {
      int userid = result.getUserid();
      Integer userID = oldToNewUser.get(userid);
      if (userID == null) {
        boolean add = missingUsers.add(userid);
        if (add) logger.warn("copyRefResult no user " + userid);
      } else {
        result.setUserID(userID);
        Integer exid = exToID.get(result.getOldExID());
        if (exid != null) {
          result.setExid(exid);
          String answer = result.getAnswer();
//          logger.info("for " + exid+ " result id " +result.getID() +" got " + answer);
          Integer audioID = getAudioID(pathToAudioID, answer);

          if (audioID == null) {
            logger.warn("copyRefResult : can't find audio from audio table at '" + answer + "'");
          } else {
            bulk.add(dao.toSlick(projid, result, audioID));
          }

        } else missing++;
      }
    }
    logger.info("copyRefResult START : copying " + bulk.size() + " ref result. Currently has " + dao.getNumResults());

    long then = System.currentTimeMillis();
    dao.addBulk(bulk);
    long now = System.currentTimeMillis();
    long diff = (now - then) / 1000;
    if (missing > 0) logger.warn("copyRefResult missing " + missing + " due to missing ex id fk");

    logger.info("copyRefResult END   : added " + bulk.size() + " and now has " + dao.getNumResults() + " took " + diff + " seconds");
  }

  private List<Result> getSinceWhenFromOldConfig(Collection<Result> toImport, long sinceWhen) {
    return toImport.stream().filter(result -> result.getTimestamp() > sinceWhen).collect(Collectors.toList());
  }

  private Integer getAudioID(Map<String, Integer> pathToAudioID, String answer) {
    String[] bestAudios = answer.split("bestAudio");

    Integer audioID = null;

    if (bestAudios.length == 2) {
      String bestAudio = bestAudios[1];
      bestAudio = "bestAudio" + bestAudio;
      audioID = pathToAudioID.get(bestAudio);
      //   if (audioID == null) logger.warn("copyRefResult : can't find '" + bestAudio + "'");
    } else {
      audioID = pathToAudioID.get(answer);
      logger.info("path " + answer + " audio id " + audioID);
    }
    return audioID;
  }

  /**
   * Expects something like:
   * <p>
   * copy english
   * copy english -isEval optName
   * 0    1       2       3
   * copy pashto pashto1
   * drop pashto
   * dropAll destroy
   *
   * @param args
   */
  public static void main(String[] args) {
    Options options = getOptions();

    HelpFormatter formatter = new HelpFormatter();

    CommandLine cmd;

    try {
      cmd = new DefaultParser().parse(options, args);
    } catch (ParseException e) {
      logger.error(e.getMessage());
      new HelpFormatter().printHelp("copy", options);

      System.exit(1);
      return;
    }

    ACTION action = UNKNOWN;
    String config = null;

    int projID = -1;
    int from = -1, to = -1;
    Date onDay = null;
    String dropConfirm = null;

    String updateUsersFile = null;
    String optName = null;
    String optConfigValue = null;
    String optDatabase = null;
    String propertiesFile = DEFAULT_PROPERTIES_FILE;
    int displayOrderValue = 0;
    boolean isEval, skipRefResult;

    logger.info("command " + cmd);
    if (cmd.hasOption(COPY.toLower())) {
      action = COPY;
      config = cmd.getOptionValue(COPY.toLower());
    } else if (ALLOW_DELETE) {
      if (cmd.hasOption(DROP.toLower())) {
        action = DROP;
        String optionValue = cmd.getOptionValue(DROP.toLower());
        try {
          projID = Integer.parseInt(optionValue);
        } catch (NumberFormatException e) {
          warnParse(options, formatter, optionValue, DROP);
          return;
        }
      } else if (cmd.hasOption(DROPALLBUT.toLower())) {
        action = DROPALLBUT;
        String optionValue = cmd.getOptionValue(DROPALLBUT.toLower());
        try {
          projID = Integer.parseInt(optionValue);
        } catch (NumberFormatException e) {
          warnParse(options, formatter, optionValue, DROPALLBUT);
          return;
        }
      } else if (cmd.hasOption(DROPALL.toLower())) {
        action = DROPALL;
        dropConfirm = cmd.getOptionValue(DROPALL.toLower());
      }
    } else if (cmd.hasOption(UPDATEUSER.toLower())) {
      action = UPDATEUSER;
      updateUsersFile = cmd.getOptionValue(UPDATEUSER.toLower());
    } else if (cmd.hasOption(UPDATE.toLower())) {
      action = UPDATE;
      logger.info("1 action " + action + " config " + config);
      config = cmd.getOptionValue(UPDATE.toLower());
      logger.info("2 action " + action + " config " + config);
    } else if (cmd.hasOption(LATEST.toLower())) {
      action = LATEST;
      logger.info("1 action " + action + " config " + config);
      config = cmd.getOptionValue(LATEST.toLower());
      logger.info("2 action " + action + " config " + config);
    } else if (cmd.hasOption(CREATED.toLower())) {
      action = CREATED;
      config = cmd.getOptionValue(CREATED.toLower());
      logger.info("action " + action + " config " + config);
    } else if (cmd.hasOption(IMPORT.toLower())) {
      action = IMPORT;
      config = cmd.getOptionValue(IMPORT.toLower());
    } else if (cmd.hasOption(SEND.toLower())) {
      action = SEND;
    } else if (cmd.hasOption(DIALOG.toLower())) {
      action = DIALOG;
      to = Integer.parseInt(cmd.getOptionValue(DIALOG.toLower()));
    } else if (cmd.hasOption(CLEANDIALOG.toLower())) {
      action = CLEANDIALOG;
      to = Integer.parseInt(cmd.getOptionValue(CLEANDIALOG.toLower()));
    } else if (cmd.hasOption(LIST.toLower())) {
      action = LIST;
    } else if (cmd.hasOption(REMAP.toLower())) {
      action = REMAP;
      to = Integer.parseInt(cmd.getOptionValue(REMAP.toLower()));

    } else if (cmd.hasOption(NORM.toLower())) {
      action = NORM;
      String optionValue = cmd.getOptionValue(NORM.toLower());

      try {
        projID = Integer.parseInt(optionValue);
      } catch (NumberFormatException e) {
        warnParse(options, formatter, optionValue, NORM);
        return;
      }
    } else if (cmd.hasOption(MERGE.toLower())) {
      action = MERGE;
      from = Integer.parseInt(cmd.getOptionValue(MERGE.toLower()));
      to = Integer.parseInt(cmd.getOptionValue(TO.toLower()));
      logger.info("\n\naction " + action + " from " + from + " to " + to);
/*    } else if (cmd.hasOption(MERGEDAY.toLower())) {
      action = MERGEDAY;
      from = Integer.parseInt(cmd.getOptionValue(MERGEDAY.toLower()));
      to = Integer.parseInt(cmd.getOptionValue(TO.toLower()));

      String opt = cmd.getOptionValue(ONDAY.toLower());
      if (opt!= null) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM_dd_yyyy");
        try {
          Date parse = sdf.parse(opt);
          onDay = parse;
          logger.info("\n\naction " + action + " from " + from + " to " + to + " on day " + onDay);
        } catch (java.text.ParseException e) {
          logger.error("couldn't parse " + opt);
        }
      }*/
    } else if (cmd.hasOption(RECORDINGS.toLower())) {
      action = RECORDINGS;
      from = Integer.parseInt(cmd.getOptionValue(RECORDINGS.toLower()));
      to = Integer.parseInt(cmd.getOptionValue(TO.toLower()));

      String opt = cmd.getOptionValue(ONDAY.toLower());
      if (opt != null) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM_dd_yyyy");
        try {
          Date parse = sdf.parse(opt);
          onDay = parse;
          logger.info("\n\naction " + action + " from " + from + " to " + to + " on day " + onDay);
        } catch (java.text.ParseException e) {
          logger.error("couldn't parse " + opt);
        }
      }
      logger.info("\n\naction " + action + " from " + from + " to " + to + " on " + onDay);
    }

    logger.info("action " + action + " config " + config);

    if (cmd.hasOption(NAME.toLower())) {
      optName = cmd.getOptionValue(NAME.toLower());
    }
    if (cmd.hasOption(DATABASE.toLower())) {
      optDatabase = cmd.getOptionValue(DATABASE.toLower());
    }
    if (cmd.hasOption(OPTCONFIG.toLower())) {
      optConfigValue = cmd.getOptionValue(OPTCONFIG.toLower());
    }

    if (cmd.hasOption(PROPERTIES.toLower())) {
      propertiesFile = cmd.getOptionValue(PROPERTIES.toLower());
    }

    if (cmd.hasOption(ORDER.toLower())) {
      String optionValue = "";
      try {
        optionValue = cmd.getOptionValue(ORDER.toLower());
        displayOrderValue = Integer.parseInt(optionValue);
      } catch (NumberFormatException e) {
        logger.error("couldn't parse " + ORDER + " = " + optionValue);
        formatter.printHelp("copy", options);
        return;
      }
    }

    isEval = cmd.hasOption(EVAL.toLower());
    skipRefResult = cmd.hasOption(SKIPREFRESULT.toLower());
    logger.info("action = " + action);

    CopyToPostgres copyToPostgres = new CopyToPostgres();

    switch (action) {
      case UNKNOWN:
        formatter.printHelp("copy", options);
        break;
/*      case DROP:
        logger.info("drop project #" + config);
        try {
          copyToPostgres.dropOneConfig(projID);
        } catch (Exception e) {
          logger.error("couldn't drop config " + config, e);
        }
        break;
      case DROPALLBUT:
        logger.info("drop all but #" + config);
        try {
          copyToPostgres.dropAllButOneConfig(projID);
        } catch (Exception e) {
          logger.error("couldn't drop config " + config, e);
        }
        break;
      case DROPALL:
        logger.warn("really be sure that this is only during development and not during production!");
        if (dropConfirm.equals("destroy")) {
          doDropAll();
        } else {
          logger.info("please check with Gordon or Ray or somebody like that before doing this.");
        }
        break;*/
      case COPY:
        logger.info("copying " +
            "\nconfig    '" + config + "' " +
            "\noptconfig '" + optConfigValue + "' " +
            "\nname      '" + optName + "'" +
            "\norder     " + displayOrderValue +
            "\neval      " + isEval
        );
        try {
          boolean b = copyToPostgres.copyOneConfigCommand(config, optConfigValue, optName, displayOrderValue,
              isEval, skipRefResult, false, false, false, optDatabase, propertiesFile);
          //  if (!b) {
          doExit(b);  // ?
          // }
        } catch (Exception e) {
          logger.error("couldn't copy config " + config, e);
        }
        break;
      case UPDATEUSER:
        logger.info("map old user ids to new user ids");
        doUpdateUser(updateUsersFile, propertiesFile);
        doExit(true);  // ?
        break;
      case UPDATE:
        logger.info("import netprof 1 content into existing netprof project");
        boolean b = copyToPostgres.copyOneConfigCommand(config, optConfigValue, optName, displayOrderValue, isEval, skipRefResult, true, false, false, optDatabase, propertiesFile);
        // if (!b) {
        doExit(b);  // ?
        // }
        break;
      case LATEST:
        logger.info("import netprof 1 content into existing netprof project given latest target project date");
        boolean c = copyToPostgres.copyOneConfigCommand(config, optConfigValue, optName, displayOrderValue, isEval, skipRefResult, true, true, false, optDatabase, propertiesFile);
        // if (!b) {
        doExit(c);  // ?
        // }
        break;
      case CREATED:
        logger.info("import netprof 1 content into existing netprof project given latest target project date");
        boolean d = copyToPostgres.copyOneConfigCommand(config, optConfigValue, optName, displayOrderValue, isEval, skipRefResult, true, false, true, optDatabase, propertiesFile);
        // if (!b) {
        doExit(d);  // ?
        // }
        break;
      case IMPORT:
        logger.info("get import date from old config");
        long importDate = copyToPostgres.getImportDate(config, optConfigValue);
        logger.info("import date for '" + config + "'/ '" + optConfigValue + "' is " + new Date(importDate));
        doExit(true);  // ?
        break;
      case MERGE:
        logger.info("merge project from into project to");
        copyToPostgres.merge(from, to, propertiesFile);
        logger.info("merge project '" + from + "' into '" + to);
        doExit(true);  // ?
        break;
      case SEND:
        logger.info("send reports");
        copyToPostgres.sendReports(propertiesFile);
        logger.info("sent reports");
        doExit(true);  //
      case NORM:
        copyToPostgres.dumpNorm(projID, propertiesFile);
        doExit(true);  // ?
        break;
      case DIALOG:
        copyDialog(to, propertiesFile);
        doExit(true);  // ?
        break;
      case CLEANDIALOG:
        cleanDialog(to, propertiesFile);
        doExit(true);  // ?
        break;
      case LIST:
        listProjects(propertiesFile);
        doExit(true);  // ?
        break;
      case REMAP:
        remap(to, propertiesFile);
        doExit(true);  // ?
        break;
 /*       case MERGEDAY:
        logger.info("merge project from into project to");
        copyToPostgres.merge(from, to, onDay);
        logger.info("merge project '" + from + "' into '" + to);
        doExit(true);  // ?
        break;*/
      case RECORDINGS:
        logger.info("merge recordings for project from into project to " + onDay);
        copyToPostgres.mergeRecordings(from, to, onDay, propertiesFile);
        logger.info("merge recordings for project '" + from + "' into '" + to);
        doExit(true);  // ?
        break;
      default:
        formatter.printHelp("copy", options);
    }
  }

  /**
   * @see #main
   * @see DIALOG command
   * @param to
   * @param propertiesFile
   */
  private static void copyDialog(int to, String propertiesFile) {
    database = getDatabase(propertiesFile);
    if (to == -1) logger.error("remember to set the project id");
    else {
      Project project = database.getProject(to);
      if (project == null) logger.error("no project with id " + to);
      else {
        if (!new DialogPopulate(database, getPathHelper(database)).populateDatabase(project)) {
          logger.info("project " + project + " already has dialog data.");
        }
      }
    }
    if (database != null) database.close();
  }

  @NotNull
  private static PathHelper getPathHelper(DatabaseImpl database) {
    return getPathHelper("war", database.getServerProps());
  }

  private static void cleanDialog(int to, String propertiesFile) {
    database = getDatabase(propertiesFile);
    if (to == -1) logger.error("remember to set the project id");
    else {
      Project project = database.getProject(to);
      if (project == null) logger.error("no project with id " + to);
      else {
        boolean b = new DialogPopulate(database, getPathHelper(database)).cleanDialog(project);
        if (!b) logger.info("project " + project + " already has dialog data.");
      }
    }
    if (database != null) database.close();
  }

  /**
   * Only for Korean.
   *
   * @param to
   * @param propertiesFile
   */
  private static void remap(int to, String propertiesFile) {
    database = getDatabase(propertiesFile);
    if (to == -1) logger.error("remember to set the project id");
    else {
      Project project = database.getProject(to);
      if (project == null) logger.error("no project with id " + to);
      else {

        RecordWordAndPhone recordWordAndPhone = database.getRecordWordAndPhone();

        long then = System.currentTimeMillis();
        recordWordAndPhone.remapPhones(to, database.getResultDAO(), database.getServerProps(), project.getLanguageEnum());
        long now = System.currentTimeMillis();
        logger.info("took " + (now - then));


      }
    }
    if (database != null) database.close();
  }

  private static void listProjects(String propertiesFile) {
    database = getDatabase(propertiesFile);
    database.getProject(1);
    Collection<Project> projects = database.getProjects();
    logger.info("known projects in " + database.getDbConfig() + " = " + projects.size());
    projects.forEach(project -> logger.info("project #" + project.getID() + " : " + project));
    if (database != null) database.close();
  }

  private void dumpNorm(int projid, String propertiesFile) {
    DatabaseImpl database = getDatabase(propertiesFile);
    Project project = database.getProject(projid);

    try {
      String fileName = "normTextFor" + project.getName() + "_" + project.getLanguage() + ".txt";
      FileWriter fileWriter = new FileWriter(fileName);
      fileWriter.write("id|raw|normalized|context\n");

      int n = 20;
      while (project.getFullTrie() == null && n-- > 0) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      List<ClientExercise> rawExercises = new ArrayList<>(project.getRawExercises());
      project.getRawExercises().forEach(commonExercise -> rawExercises.addAll(commonExercise.getDirectlyRelated()));

      rawExercises.sort(Comparator.comparingInt(HasID::getID));
      rawExercises
          .forEach(commonExercise ->
              {
                try {
                  writeLine(project, fileWriter, commonExercise);
                 /* commonExercise.getDirectlyRelated().forEach(commonExercise1 ->
                      {
                        try {
                          writeLine(project, fileWriter, commonExercise1);
                        } catch (IOException e) {
                          logger.error("got " + e + " on " + commonExercise1, e);
                        }
                      }
                  );*/
                } catch (IOException e) {
                  logger.error("got " + e + " on " + commonExercise, e);
                }

              }
          );

      fileWriter.close();

      logger.info("Wrote to " + fileName);
    } catch (IOException e) {
      logger.error("Got " + e, e);
    }

  }

  private void writeLine(Project project, FileWriter fileWriter, ClientExercise commonExercise) throws IOException {
    String foreignLanguage = commonExercise.getForeignLanguage();
    String norm = project.getFullTrie().getNormalized(foreignLanguage);
    fileWriter.write(commonExercise.getID() + "|");
    fileWriter.write(foreignLanguage.trim() + "|");
    fileWriter.write(norm + "|");
    fileWriter.write(commonExercise.isContext() + "\n");
  }

  private void sendReports(String propertiesFile) {
    database = getDatabase(propertiesFile);
    database.getProject(2);
    database.sendReports();
    if (database != null) database.close();
  }

  private static void doExit(boolean b) {
    if (database != null) {
      database.close();
    }
    System.exit(b ? 0 : 1);
  }

  private static void warnParse(Options options, HelpFormatter formatter, String optionValue, ACTION drop) {
    logger.error("couldn't parse " + drop + " = " + optionValue);
    formatter.printHelp("drop", options);
  }

  @NotNull
  private static Options getOptions() {
    Options options = new Options();

    {
      Option copy = new Option(COPY.getValue(), COPY.toLower(), true, "copy this config or language into netprof");
      copy.setRequired(false);
      options.addOption(copy);
    }
    {
      Option copy = new Option(IMPORT.getValue(), IMPORT.toLower(), true, "get netprof 1 original import date");
      copy.setRequired(false);
      options.addOption(copy);
    }

    addDeleteOptions(options);

    addNonRequiredArg(options, OPTCONFIG);
    addNonRequiredArg(options, PROPERTIES);
    addNonRequiredArg(options, NAME);
    addNonRequiredNoArg(options, EVAL);
    addNonRequiredArg(options, ORDER);
    addNonRequiredNoArg(options, SKIPREFRESULT);

    {
      Option mapFile = new Option(UPDATEUSER.getValue(), UPDATEUSER.toLower(), true, "user mapping file (two column csv)");
      options.addOption(mapFile);
    }

    {
      Option mapFile = new Option(UPDATE.getValue(), UPDATE.toLower(), true, "update existing config");
      options.addOption(mapFile);
    }

    {
      Option mapFile = new Option(LATEST.getValue(), LATEST.toLower(), true, "update existing config since latest of target project");
      options.addOption(mapFile);
    }

    {
      Option mapFile = new Option(CREATED.getValue(), CREATED.toLower(), true, "update existing config since creation date of target project");
      options.addOption(mapFile);
    }

    {
      Option mapFile = new Option(MERGE.getValue(), MERGE.toLower(), true, "from project id");
      options.addOption(mapFile);

      ACTION recordings = RECORDINGS;
      mapFile = new Option(recordings.getValue(), recordings.toLower(), true, "from project id");
      options.addOption(mapFile);

      ACTION act = DIALOG;
      mapFile = new Option(act.getValue(), act.toLower(), true, "load dialog for project id");
      options.addOption(mapFile);

      ACTION act2 = CLEANDIALOG;
      mapFile = new Option(act2.getValue(), act2.toLower(), true, "clean dialogs from project id");
      options.addOption(mapFile);

      {
        ACTION act3 = REMAP;
        mapFile = new Option(act3.getValue(), act3.toLower(), true, "remap phone labels for project id");
        options.addOption(mapFile);
      }

      {
        ACTION act3 = LIST;
        mapFile = new Option(act3.getValue(), act3.toLower(), false, "list projects");
        options.addOption(mapFile);
      }

      addOption(options, TO);
      addOption(options, ONDAY);
      addOption(options, DATABASE);
    }
    {
      Option mapFile = new Option(NORM.getValue(), NORM.toLower(), true, "dumpNormExerciseTable");
      options.addOption(mapFile);
    }
    {
      Option mapFile = new Option(SEND.getValue(), SEND.toLower(), false, "send reports now");
      options.addOption(mapFile);
    }

    return options;
  }

  private static void addDeleteOptions(Options options) {
    if (ALLOW_DELETE) {
      {
        Option drop = new Option(DROP.getValue(), DROP.toLower(), true, "drop this project from netprof database");
        drop.setRequired(false);
        options.addOption(drop);
      }

      {
        Option drop = new Option(DROPALLBUT.getValue(), DROPALLBUT.toLower(), true, "drop all projects but this one from netprof database");
        drop.setRequired(false);
        options.addOption(drop);
      }

      {
        Option dropAll = new Option(DROPALL.getValue(), DROPALL.toLower(), true, "drop all tables in the netprof database");
        dropAll.setRequired(false);
        options.addOption(dropAll);
      }
    }
  }

  private static void addNonRequiredNoArg(Options options, OPTIONS skiprefresult) {
    Option skip = new Option(skiprefresult.getValue(), skiprefresult.toLower(), false, skiprefresult.getDesc());
    skip.setRequired(false);
    options.addOption(skip);
  }

  private static void addNonRequiredArg(Options options, OPTIONS skiprefresult) {
    Option skip = new Option(skiprefresult.getValue(), skiprefresult.toLower(), true, skiprefresult.getDesc());
    skip.setRequired(false);
    options.addOption(skip);
  }

  private static void addOption(Options options, OPTIONS to) {
    options.addOption(new Option(to.getValue(), to.toLower(), true, to.getDesc()));
  }

  /**
   * Drop all doesn't require mongo connection, etc.
   */
  private static void doDropAll() {
    DatabaseImpl database = null;
    try {
      logger.warn("OK hope this is what you want.");
      database = getSimpleDatabase();
      database.dropAll();
    } catch (Exception e) {
      logger.error("couldn't drop all tables, got " + e, e);
      String concat = database == null ? "" : database.getTables().concat(",\n");
      logger.info("doDropAll now there are " + concat);
    } finally {
      try {
        if (database != null) {
          database.close();
        }
      } catch (Exception e) {
        logger.error("Got " + e, e);
      }
    }
  }

  private static void doUpdateUser(String filename, String propertiesFile) {
    DatabaseImpl database = null;
    try {
      logger.warn("Mapping old user ids to new user ids.");
      database = getDatabase(propertiesFile);
      long then = System.currentTimeMillis();

      Map<Integer, Integer> oldToNew = getUserMapFromFile(filename);

      final DatabaseImpl fd = database;
      oldToNew.forEach((k, v) -> {
        logger.warn("doUpdateUser : update  " + k + "->" + v);
        updateUserIDForAllTables(fd, k, v);
      });

      long now = System.currentTimeMillis();

      logger.warn("Updated  " + oldToNew.size() + " user ids in " + (now - then) + " millis.");
      database.close();
    } catch (Exception e) {
      logger.error("couldn't update all tables, got " + e, e);
      String concat = database == null ? "" : database.getTables().concat(",\n");
      logger.info("tables : now there are " + concat);
    } finally {
      try {
        if (database != null) {
          database.close();
        }
      } catch (Exception e) {
        logger.error("Got " + e, e);
      }
    }
  }

  private static void updateUserIDForAllTables(DatabaseImpl fd, Integer k, Integer v) {
    fd.getAudioDAO().updateUser(k, v);
    fd.getAnswerDAO().updateUser(k, v);
    fd.getEventDAO().updateUser(k, v);
    fd.getReviewedDAO().updateUser(k, v);
    fd.getSecondStateDAO().updateUser(k, v);
  }

  @NotNull
  private static Map<Integer, Integer> getUserMapFromFile(String filename) throws IOException {
    File file = new File(filename);
    if (!file.exists()) {
      logger.warn("can't find file " + file.getAbsolutePath());
    }
    Stream<String> lines = Files.lines(file.toPath());
    Map<Integer, Integer> oldToNew = new HashMap<>();
    lines.forEach(line -> {
      String[] split = line.split(",");
      String old = split[0];
      String newUser = split[1];
      int i = -1;
      try {
        i = Integer.parseInt(old);
      } catch (NumberFormatException e) {
        logger.error("couldn't parse line " + oldToNew.size() + " : " + line);
        throw new IllegalArgumentException("bad file " + filename);
      }
      int i1 = -1;
      try {
        i1 = Integer.parseInt(newUser);
      } catch (NumberFormatException e) {
        logger.error("couldn't parse line " + oldToNew.size() + " : " + line);
        throw new IllegalArgumentException("bad file " + filename);
      }
      if (i != -1 && i1 != -1) {
        oldToNew.put(i, i1);
      }
    });

    logger.info("Map has " + oldToNew.size() + " : " + oldToNew);
    return oldToNew;
  }
}
