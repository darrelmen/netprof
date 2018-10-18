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

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.JsonSupport;
import mitll.langtest.server.database.analysis.SlickAnalysis;
import mitll.langtest.server.database.project.IProjectManagement;
import mitll.langtest.server.database.project.ProjectManagement;
import mitll.langtest.server.decoder.RefResultDecoder;
import mitll.langtest.server.scoring.ASRWebserviceScoring;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.trie.ExerciseTrie;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Pair;
import mitll.langtest.shared.project.*;
import mitll.langtest.shared.scoring.AlignmentOutput;
import mitll.langtest.shared.scoring.RecalcRefResponse;
import mitll.npdata.dao.SlickProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static mitll.langtest.server.database.project.ProjectManagement.logMemory;
import static mitll.langtest.shared.project.ProjectProperty.*;

/**
 * Has everything associated with a project
 * <p>
 * TODO : give this an interface
 */
public class Project implements IPronunciationLookup {
  private static final Logger logger = LogManager.getLogger(Project.class);

  private static final String HYDRA_2 = "hydra2";
  private static final String H_2 = "h2";
  private static final String HYDRA = "hydra";
  private static final Set<ProjectStatus> toSkip = new HashSet<>(Arrays.asList(ProjectStatus.RETIRED, ProjectStatus.DELETED));

  /**
   * @see #getWebserviceHost
   * @see mitll.langtest.server.database.project.ProjectDAO#update
   * @see ProjectManagement#getProjectInfo
   */
  /**
   * Initially the choices should be hydra and hydra2 (or maybe hydra-dev and hydra2-dev)
   *
   * @see #getWebserviceHost
   */
  public static final String WEBSERVICE_HOST_DEFAULT = "127.0.0.1";
  public static final String TRUE = Boolean.TRUE.toString();

  /**
   * @see #getWebservicePort
   * @see mitll.langtest.server.database.project.ProjectDAO#update(int, ProjectInfo)
   */

  private SlickProject project;
  private ExerciseDAO<CommonExercise> exerciseDAO;
  private JsonSupport jsonSupport;
  private SlickAnalysis analysis;
  private AudioFileHelper audioFileHelper;
  /**
   * @see #makeItemTrie(List, SmallVocabDecoder)
   */
  private ExerciseTrie<CommonExercise> fullTrie = null;
  private ExerciseTrie<CommonExercise> fullContextTrie = null;
  private RefResultDecoder refResultDecoder;
  private PathHelper pathHelper;
  private DatabaseImpl db;
  private ServerProperties serverProps;
  private boolean isRTL;
  private final Map<Integer, AlignmentOutput> audioToAlignment = new HashMap<>();

  private Map<String, Integer> fileToRecorder = new HashMap<>();
  private List<IDialog> dialogs = new ArrayList();
  private final ISection<IDialog> dialogSectionHelper = new SectionHelper<>();

  //private ExerciseTrie<CommonExercise> phoneTrie;
  //private Map<Integer, ExercisePhoneInfo> exToPhone;

  /**
   * @param exerciseDAO
   * @see mitll.langtest.server.database.project.ProjectManagement#addSingleProject
   */
  public Project(ExerciseDAO<CommonExercise> exerciseDAO) {
    this.exerciseDAO = exerciseDAO;
  }

  /**
   * @param project
   * @param pathHelper
   * @param serverProps
   * @param db
   * @param logAndNotify
   * @see ProjectManagement#rememberProject(PathHelper, ServerProperties, LogAndNotify, SlickProject, DatabaseImpl)
   */
  public Project(SlickProject project,
                 PathHelper pathHelper,
                 ServerProperties serverProps,
                 DatabaseImpl db,
                 LogAndNotify logAndNotify) {
    this.project = project;
    this.db = db;
    audioFileHelper = new AudioFileHelper(pathHelper, serverProps, db, logAndNotify, this);
    this.serverProps = serverProps;
    this.pathHelper = pathHelper;
  }

  public int getID() {
    return project == null ? -1 : project.id();
  }

  public void setProject(SlickProject project) {
    this.project = project;
  }

  public String getLanguage() {
    return project == null ? "No project yet" : project.language();
  }

  public Language getLanguageEnum() {
    return project == null ? Language.UNKNOWN : getLanguageFor();
  }

  @NotNull
  private Language getLanguageFor() {
    try {
      return Language.valueOf(project.language().toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.error("no known language  " + project.language());
      return Language.UNKNOWN;
    }
  }

  public boolean isEnglish() {
    return getLanguage().equalsIgnoreCase("english");
  }

  private SmallVocabDecoder getSmallVocabDecoder() {
    return getAudioFileHelper() == null ? null : getAudioFileHelper().getSmallVocabDecoder();
  }

  public boolean hasModel() {
    return !isNoModel();
  }

  private boolean isNoModel() {
    return getModelsDir() == null || getModelsDir().isEmpty();
  }

  public SlickProject getProject() {
    return project;
  }

  public ProjectType getKind() {
    return ProjectType.valueOf(project.kind());
  }

  public boolean shouldLoad() {
    return project != null && !toSkip.contains(ProjectStatus.valueOf(project.status()));
  }

  /**
   * If we can't get the type order out of the section helper... find it from the project
   *
   * @return
   */
  public List<String> getTypeOrder() {
    ISection<CommonExercise> sectionHelper = getSectionHelper();

    List<String> types = sectionHelper == null ? Collections.EMPTY_LIST : sectionHelper.getTypeOrder();
    if (project != null && (types == null || types.isEmpty())) {
      types = new ArrayList<>();
      String first = project.first();
      String second = project.second();
      if (first != null && !first.isEmpty()) types.add(first);
      if (second != null && !second.isEmpty()) types.add(second);
//      logger.info("getTypeOrder type order " + types);
    }

    return types;
  }

  @NotNull
  public List<String> getBaseTypeOrder( ) {
    List<String> typeOrder = new ArrayList<>();
    SlickProject project1 = getProject();
    if (!project1.first().isEmpty()) {
      typeOrder.add(project1.first());
    } else {
      logger.error("huh? project " + project + " first type is empty?");
    }
    if (!project1.second().isEmpty()) {
      typeOrder.add(project1.second());
    }
    return typeOrder;
  }

  /**
   * @param exerciseDAO
   * @see mitll.langtest.server.database.project.ProjectManagement#setExerciseDAO
   */
  public void setExerciseDAO(ExerciseDAO<CommonExercise> exerciseDAO) {
    this.exerciseDAO = exerciseDAO;
  }

  public ExerciseDAO<CommonExercise> getExerciseDAO() {
    return exerciseDAO;
  }

  public List<CommonExercise> getRawExercises() {
    return exerciseDAO.getRawExercises();
  }

  public ISection<CommonExercise> getSectionHelper() {
    return exerciseDAO == null ? null : exerciseDAO.getSectionHelper();
  }

  public ISection<IDialog> getDialogSectionHelper() {
    return dialogSectionHelper;
  }

  public void setJsonSupport(JsonSupport jsonSupport) {
    this.jsonSupport = jsonSupport;
  }

  public JsonSupport getJsonSupport() {
    return jsonSupport;
  }

  /**
   * @param analysis
   * @see ProjectManagement#configureProject
   */
  public void setAnalysis(SlickAnalysis analysis) {
    this.analysis = analysis;
    buildExerciseTrie();
    this.refResultDecoder = new RefResultDecoder(db, serverProps, pathHelper, getAudioFileHelper());
  }

  /**
   * @see #setAnalysis(SlickAnalysis)
   */
  private <T extends CommonShell> void buildExerciseTrie() {
    final List<CommonExercise> rawExercises = getRawExercises();
    SmallVocabDecoder smallVocabDecoder = getSmallVocabDecoder();

    new Thread(() -> makeItemTrie(rawExercises, smallVocabDecoder), "makeFullTrie_" + getID()).start();
    new Thread(() -> makeContextTrie(rawExercises, smallVocabDecoder), "makeContextTrie_" + getID()).start();
  }

  /**
   * @param rawExercises
   * @param smallVocabDecoder
   * @see #buildExerciseTrie
   */
  private void makeItemTrie(List<CommonExercise> rawExercises, SmallVocabDecoder smallVocabDecoder) {
    logger.info("buildExerciseTrie : build trie from " + rawExercises.size() + " exercises for " + project);
    long then = System.currentTimeMillis();
    fullTrie = new ExerciseTrie<>(rawExercises, project.language(), smallVocabDecoder, true, false);
    logger.info("buildExerciseTrie : for " + project.id() + " took " + (System.currentTimeMillis() - then) + " millis to build trie for " + rawExercises.size() + " exercises");
  }

  private void makeContextTrie(List<CommonExercise> rawExercises, SmallVocabDecoder smallVocabDecoder) {
    long then1 = System.currentTimeMillis();

    if (fullContextTrie != null) {
      logger.warn("buildExerciseTrie : rebuilding full context trie for " + rawExercises.size() + " exercises.");
    }

    logger.info("buildExerciseTrie : START context for " + project.id() + " for " + rawExercises.size() + " exercises.");
//      List<CommonExercise> copy = new ArrayList<>(rawExercises);
/*
      if (copy.size() > 1000) {
        copy = copy.subList(0, 1000);
      }
*/
    long before = logMemory();
    fullContextTrie = new ExerciseTrie<>(rawExercises, project.language(), smallVocabDecoder, false, false);
    long after = logMemory();
    logger.info("buildExerciseTrie : END context for " + project.id() + " took " + (System.currentTimeMillis() - then1) + " millis to build context trie for " + rawExercises.size() +
        " exercises, used " + (after - before) + " MB");
  }

  /**
   * @see mitll.langtest.server.services.AudioServiceImpl#recalcRefAudio
   */
  public RecalcRefResponse recalcRefAudio() {
    Collection<CommonExercise> exercisesForUser = getRawExercises();
    logger.info("recalcRefAudio " + project + " for " + exercisesForUser.size() + " exercises.");
    return refResultDecoder.writeRefDecode(getLanguageEnum(), exercisesForUser, project.id());
  }

  public SlickAnalysis getAnalysis() {
    return analysis;
  }

  public AudioFileHelper getAudioFileHelper() {
    return audioFileHelper;
  }

  /**
   * @return
   */
  public ExerciseTrie<CommonExercise> getFullTrie() {
    return fullTrie;
  }

  public boolean isTrieBuilt() {
    return fullTrie != null;
  }

  ExerciseTrie<CommonExercise> getFullContextTrie() {
    return fullContextTrie;
  }

  public void stopDecode() {
    if (refResultDecoder != null) refResultDecoder.setStopDecode(true);
  }

  /**
   * Not for right now... - we only run locally.
   *
   * @return
   * @see ASRWebserviceScoring#getWebserviceIP
   */
  public String getWebserviceHost() {
    String prop = getProp(WEBSERVICE_HOST);
    if (prop == null || prop.isEmpty()) {
      prop = WEBSERVICE_HOST_DEFAULT;
    }
    return prop;
  }

  /**
   * @return
   * @see ASRWebserviceScoring#getWebservicePort
   */
  public int getWebservicePort() {
    String prop = getProp(WEBSERVICE_HOST_PORT);
    if (prop == null || prop.isEmpty()) prop = "-1";
    try {
      int ip = Integer.parseInt(prop);
      if (ip == 1) {
        logger.error("getWebservicePort No webservice host port found.");
      }
      return ip;
    } catch (NumberFormatException e) {
      logger.error("getWebservicePort for " + this + " couldn't parse prop for " + WEBSERVICE_HOST_PORT);
      return -1;
    }
  }

  public boolean isOnIOS() {
    String prop = getProp(SHOW_ON_IOS);
    return prop != null && prop.equalsIgnoreCase(TRUE);
  }

  public String getModelsDir() {
    return getProp(MODELS_DIR);
  }


  public ModelType getModelType() {
    String prop = getProp(MODEL_TYPE);

    if (prop == null || prop.isEmpty()) {
      return ModelType.HYDRA;
    } else {
      try {
        return ModelType.valueOf(prop);
      } catch (IllegalArgumentException e) {
        logger.error("couldn't parse '" + prop + "' as model type enum?");
        return ModelType.HYDRA;
      }
    }
  }

  public boolean hasProjectSpecificAudio() {
    return getProp(AUDIO_PER_PROJECT).equalsIgnoreCase(TRUE);
  }

  public boolean shouldSwapPrimaryAndAlt() {
    return getProp(SWAP_PRIMARY_AND_ALT).equalsIgnoreCase(TRUE);
  }

  private Map<String, String> propCache = new HashMap<>();

  /**
   * Re populate cache really.
   */
  public void clearPropCache() {
//    logger.debug("clear project #" + getID());
    propCache.clear();
    putAllProps();
  }

  public String getProp(ProjectProperty projectProperty) {
    return getProp(projectProperty.getName());
  }

  /**
   * Latchy
   *
   * @param prop
   * @return
   */
  private String getProp(String prop) {
    String s = propCache.get(prop);
    if (s == null) {
      putAllProps();

      String propValue = db.getProjectDAO().getPropValue(getID(), prop);  // blank if miss, not null
      //   logger.info("getProp : project " + getID() + " prop " + prop + " = " + propValue);

      if (propValue == null) {
        logger.warn("huh? no prop value for " + prop);
      } else {
        propCache.put(prop, propValue);
//        logger.info("getProp " + prop + " = " + propValue);
      }
      return propValue;
    } else {
      return s;
    }
  }

  private void putAllProps() {
    propCache.putAll(db.getProjectDAO().getProps(getID()));
  }

  int spew = 0;

  /**
   * @param id
   * @return
   * @see
   */
  public CommonExercise getExerciseByID(int id) {
    if (id == 2) {
      logger.warn("getExerciseByID project # " + getID() + " : skip request for unknown exercise (2)");
      return null;
    } else {
      CommonExercise exercise = exerciseDAO.getExercise(id);
      if (exercise == null) {
        spew++;
        if (spew < 10 || spew % 100 == 0) {
          logger.info("getExerciseByID project # " + getID() + " : no exercise for #" + id + " in " + exerciseDAO.getNumExercises() + " exercises?");
        }
      }
      return exercise;
    }
  }

  /**
   * Only accept an exact match
   *
   * @param prefix
   * @return
   * @seex mitll.langtest.server.services.ListServiceImpl#getExerciseByVocab
   * @see mitll.langtest.server.ScoreServlet#getExerciseIDFromText
   */
  public CommonExercise getExerciseBySearch(String prefix) {
    return getMatchEither(prefix, fullTrie.getExercises(prefix));
  }

  public CommonExercise getExerciseBySearchBoth(String english, String fl) {
    List<CommonExercise> exercises = fullTrie.getExercises(fl);
    if (exercises.size() == 1) {
      CommonExercise next = exercises.iterator().next();
      logger.info("getExerciseBySearchBoth found only #" + next.getID() + " '" + next.getEnglish() + "' '" + next.getForeignLanguage() + "' for search '" + english + "' '" + fl + "'");
      return next;
    } else if (!exercises.isEmpty()) {
      List<CommonExercise> collect1 = exercises.stream().filter(ex -> ex.getForeignLanguage().equalsIgnoreCase(fl)).collect(Collectors.toList());
      logger.info("getExerciseBySearchBoth found on fl match " + collect1.size() + " for '" + english + "' '" + fl + "'");

      if (collect1.isEmpty()) {
        collect1 = exercises.stream().filter(ex -> ex.getEnglish().equalsIgnoreCase(english)).collect(Collectors.toList());
        logger.info("\tgetExerciseBySearchBoth found on english match " + collect1.size() + " for '" + english + "' '" + fl + "'");
      }

      if (collect1.isEmpty()) {
        CommonExercise next = exercises.iterator().next();
        logger.info("getExerciseBySearchBoth returning near match only #" + next.getID() + " " + next.getEnglish() + " " + next.getForeignLanguage() + " for '" + english + "' '" + fl + "'");
        return next;
      } else {
        CommonExercise next = collect1.iterator().next();

        logger.info("getExerciseBySearchBoth returning first of " + collect1.size() +
            " match" +
            "\n\t#       " + next.getID() +
            "\n\tenglish " + next.getEnglish() +
            "\n\tfl      " + next.getForeignLanguage() +
            "\n\tfor     " + english +
            "\n\tfl      " + fl);

        return next;
      }
    } else {
      List<CommonExercise> exercisesInVocab = fullTrie.getExercises(english);
      CommonExercise exercise = getFirstMatchingLength(english, fl, exercisesInVocab);

      logger.info("getExerciseBySearchBoth looking for '" + english +
          "' and '" + fl +
          "' found " + exercise);

      if (exercise == null && !english.isEmpty()) {
        List<CommonExercise> fullContextTrieExercises = fullContextTrie.getExercises(english);
        exercise = getFirstMatchingLength(english, fl, fullContextTrieExercises);
        if (exercise != null && !exercise.getDirectlyRelated().isEmpty()) {
          exercise = getFirstContext(exercise);
        }
        logger.info("\tgetExerciseBySearchBoth context looking for '" + english + "' found " + exercise);
      }

      if (exercise == null) {
        exercise = getMatchEither(english, fl, exercisesInVocab);
        logger.info("\tgetExerciseBySearchBoth looking for '" + english + " and " + fl +
            " found " + exercise);
      }

      if (exercise == null && !fl.isEmpty()) {
        List<CommonExercise> fullContextTrieExercises = fullContextTrie.getExercises(fl);
        logger.info("\tinitially context num = " + fullContextTrieExercises.size());
        exercise = getMatchEither(english, fl, fullContextTrieExercises);
        if (exercise != null && !exercise.getDirectlyRelated().isEmpty()) {
          exercise = getFirstContext(exercise);
        }
        logger.info("\tgetExerciseBySearchBoth context looking for '" + english + " or '" + fl +
            "' found " + exercise);
        if (exercise == null && !fullContextTrieExercises.isEmpty()) {
          exercise = fullContextTrieExercises.iterator().next();
          if (exercise != null && !exercise.getDirectlyRelated().isEmpty()) {
            exercise = getFirstContext(exercise);
          }
          logger.info("\tnow returning " + exercise);
        }
      }

      return exercise;
    }
  }

  private CommonExercise getFirstContext(CommonExercise exercise) {
    return exercise.getDirectlyRelated().iterator().next().asCommon();
  }

  private CommonExercise getFirstMatchingLength(String english, String fl, List<CommonExercise> exercises1) {
    Optional<CommonExercise> first = exercises1
        .stream()
        .filter(p ->
            p.getForeignLanguage().length() == fl.length() &&
                p.getEnglish().equalsIgnoreCase(english))
        .findFirst();
    return first.orElse(null);
  }

  private CommonExercise getMatchEither(String prefix, List<CommonExercise> exercises1) {
    Optional<CommonExercise> first = exercises1
        .stream()
        .filter(p ->
            p.getForeignLanguage().equalsIgnoreCase(prefix) ||
                p.getEnglish().equalsIgnoreCase(prefix))
        .findFirst();
    return first.orElse(null);
  }

  private CommonExercise getMatchEither(String prefix, String fl, List<CommonExercise> exercises1) {
    Optional<CommonExercise> first = exercises1
        .stream()
        .filter(p ->
            p.getForeignLanguage().equalsIgnoreCase(fl) ||
                p.getEnglish().equalsIgnoreCase(prefix))
        .findFirst();
    return first.orElse(null);
  }

/*  public void setPhoneTrie(ExerciseTrie<CommonExercise> phoneTrie) {
    this.phoneTrie = phoneTrie;
  }

  public ExerciseTrie<CommonExercise> getPhoneTrie() {
    return phoneTrie;
  }*/

/*
  public void setExToPhone(Map<Integer, ExercisePhoneInfo> exToPhone) {
    this.exToPhone = exToPhone;
  }
*/

  /**
   * @param transcript
   * @param transliteration
   * @return
   * @see mitll.langtest.server.database.userexercise.SlickUserExerciseDAO#getExercisePhoneInfoFromDict
   */
  @Override
  public String getPronunciationsFromDictOrLTS(String transcript, String transliteration) {
    return hasModel() ? audioFileHelper.getPronunciationsFromDictOrLTS(transcript, transliteration) : "";
  }

  @Override
  public int getNumPhonesFromDictionary(String transcript, String transliteration) {
    return hasModel() ? audioFileHelper.getNumPhonesFromDictionary(transcript, transliteration) : 0;
  }

  @Override
  public boolean hasDict() {
    return hasModel() && audioFileHelper.hasDict();
  }

  /**
   * @return
   * @see ProjectManagement#configureProjects
   * @see IProjectManagement#getExercises
   */
  public boolean isConfigured() {
    return exerciseDAO != null && exerciseDAO.isConfigured();
  }

  public ProjectStatus getStatus() {
    try {
      return ProjectStatus.valueOf(project.status());
    } catch (IllegalArgumentException e) {
      logger.error("Got " + e, e);
      return null;
    }
  }

  public boolean isRTL() {
    return isRTL;
  }

  public void setRTL(boolean RTL) {
    isRTL = RTL;
  }

  public void ensureAudio(Set<CommonExercise> toAddAudioTo) {
    refResultDecoder.ensure(getLanguage(), toAddAudioTo);
  }

  /**
   * @see mitll.langtest.server.scoring.AlignmentHelper#addAlignmentOutput(int, Project, Collection)
   * @return
   */
  public Map<Integer, AlignmentOutput> getAudioToAlignment() {
    return audioToAlignment;
  }

  /**
   * So if you're on hydra2, you only handle certain languages...
   *
   * @return true if this server handles this project.
   */
  public boolean isMyProject() {
    String hostName = serverProps.getHostName();

    boolean myProject = true;
    String webserviceHost = getWebserviceHost();

    if (hostName.startsWith(HYDRA_2)) {
      myProject = webserviceHost.equalsIgnoreCase(H_2);
    } else if (hostName.startsWith(HYDRA)) {
      myProject = webserviceHost.equalsIgnoreCase(WEBSERVICE_HOST_DEFAULT);
    }
    if (myProject) {
      logger.info("isMyProject project " + project.id() + " on " + hostName + " will check lts and count phones.");
    } else {
      logger.info("isMyProject project " + project.id() + " on " + hostName + " will NOT check lts and count phones.");
    }
    return myProject;
  }

  public Map<String, Integer> getFileToRecorder() {
    return fileToRecorder;
  }

  public void setFileToRecorder(Map<String, Integer> fileToRecorder) {
    this.fileToRecorder = fileToRecorder;
  }

  public Integer getUserForFile(String requestURI) {
    Integer integer = fileToRecorder.get(requestURI);
//    if (integer == null) {
    //     logger.warn("getUserForFile  can't find " + requestURI + " in " + fileToRecorder.size());
    //  }
    return integer;
  }

  /**
   * @param testAudioFile
   * @param userIDFromSessionOrDB
   * @see mitll.langtest.server.services.ScoringServiceImpl#getPretestScore
   */
  public void addAnswerToUser(String testAudioFile, int userIDFromSessionOrDB) {
    fileToRecorder.put(testAudioFile, userIDFromSessionOrDB);
    //logger.info("addAnswerToUser project " + getProject().id() + " now has " + fileToRecorder.size());
  }

  public int getPort() {
    try {
      String prop = getProp(WEBSERVICE_HOST_PORT);
      if (prop == null || prop.isEmpty()) return -1;
      else return Integer.parseInt(prop);
    } catch (NumberFormatException e) {
      logger.error("for " + project + " got " + e);
      return -1;
    }
  }

  public String getName() {
    return project.name();
  }

  public List<IDialog> getDialogs() {
    return dialogs;
  }

  public Collection<Integer> getDialogExerciseIDs(int dialogID) {
    Set<Integer> dialogExercises = new HashSet<>();
    if (dialogID != -1) {
      Optional<IDialog> first = getDialogs().stream().filter(iDialog -> iDialog.getID() == dialogID).findFirst();
      if (first.isPresent()) {
        first.get().getExercises().forEach(clientExercise -> dialogExercises.add(clientExercise.getID()));
        first.get().getCoreVocabulary().forEach(clientExercise -> dialogExercises.add(clientExercise.getID()));
      } else logger.warn("can't find dialog " + dialogID);
    }
    return dialogExercises;
  }

  /**
   * @param dialogs
   * @see mitll.langtest.server.database.project.DialogPopulate#addDialogInfo
   */
  public void setDialogs(List<IDialog> dialogs) {
    this.dialogs = dialogs;

    List<List<Pair>> seen = new ArrayList<>();
    List<String> typeOrder = getTypeOrder();

    String unitType = typeOrder.size() > 0 ? typeOrder.get(0) : "";
    boolean hasUnitType = !unitType.isEmpty();

    String chapterType = typeOrder.size() > 1 ? typeOrder.get(1) : "";
    boolean hasChapterType = !chapterType.isEmpty();

    dialogs.forEach(dialog -> {
      List<Pair> pairs = new ArrayList<>();
      String unit = dialog.getUnit();
      if (!unit.isEmpty() && hasUnitType) {
        pairs.add(new Pair(unitType, unit));
      }

      String chapter = dialog.getChapter();
      if (!chapter.isEmpty() && hasChapterType) {
        pairs.add(new Pair(chapterType, chapter));
      }

      pairs.addAll(dialog.getAttributes());
      this.dialogSectionHelper.addPairs(dialog, pairs);

      seen.add(pairs);
    });

    dialogSectionHelper.rememberTypesInOrder(typeOrder, seen);
  }

  public String toString() {
    return "Project\n\tproject = " + project + "\n\ttypes " + getTypeOrder() + " exercise dao " + exerciseDAO;
  }
}
