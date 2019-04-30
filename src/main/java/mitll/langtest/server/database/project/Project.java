/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.project;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ScoreServlet;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.JsonSupport;
import mitll.langtest.server.database.analysis.SlickAnalysis;
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.server.database.exercise.IPronunciationLookup;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.SectionHelper;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.server.decoder.RefResultDecoder;
import mitll.langtest.server.json.JsonExport;
import mitll.langtest.server.scoring.ASRWebserviceScoring;
import mitll.langtest.server.scoring.AlignmentHelper;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.services.AudioServiceImpl;
import mitll.langtest.server.services.ScoringServiceImpl;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static mitll.langtest.server.database.project.ProjectManagement.logMemory;
import static mitll.langtest.shared.project.ProjectProperty.*;

/**
 * Has everything associated with a project
 * <p>
 * TODO : give this an interface
 */
public class Project implements IPronunciationLookup, IProject {
  private static final Logger logger = LogManager.getLogger(Project.class);

  private static final String HYDRA_2 = "hydra2";
  private static final String H_2 = "h2";
  private static final String HYDRA = "hydra";
  private static final Set<ProjectStatus> toSkip = new HashSet<>(Arrays.asList(ProjectStatus.RETIRED, ProjectStatus.DELETED));

  /**
   * @see #getWebserviceHost
   * @see ProjectDAO#update
   * @see ProjectManagement#getProjectInfo
   */
  public static final String TRUE = Boolean.TRUE.toString();
  public static final String MANDARIN = "Mandarin";

  private static final boolean REPORT_ON_DIALOG_TYPES = false;
  private static final boolean DEBUG_FILE_LOOKUP = false;

  /**
   * @see #getWebservicePort
   * @see ProjectDAO#update(int, ProjectInfo)
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
  private final ConcurrentMap<Integer, AlignmentOutput> audioToAlignment = new ConcurrentHashMap<>();

  private Map<String, Integer> fileToRecorder = new ConcurrentHashMap<>();
  private Map<String, Boolean> unknownFiles = new ConcurrentHashMap<>();

  /**
   * @see #setDialogs
   */
//  private List<IDialog> dialogs = new ArrayList<>();
  private Map<Integer, IDialog> idToDialog = new ConcurrentHashMap<>();

  private final ISection<IDialog> dialogSectionHelper = new SectionHelper<>();
  private JsonExport jsonExport;
  /**
   *
   */
  private final Map<String, String> propCache = new ConcurrentHashMap<>();

  /**
   * @param exerciseDAO
   * @see ProjectManagement#addSingleProject
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

  @Override
  public int getID() {
    return project == null ? -1 : project.id();
  }

  public void setProject(SlickProject project) {
    this.project = project;
  }

  @Override
  public String getLanguage() {
    return project == null ? "No project yet" : project.language();
  }

  @Override
  public Language getLanguageEnum() {
    return project == null ? Language.UNKNOWN : getLanguageFor();
  }

  @NotNull
  private Language getLanguageFor() {
    try {
      return Language.valueOf(getLangName());
    } catch (IllegalArgumentException e) {
      logger.error("no known language  " + project.language());
      return Language.UNKNOWN;
    }
  }

  @NotNull
  private String getLangName() {
    String name = project.language().toUpperCase();
    if (name.equalsIgnoreCase(MANDARIN)) name = Language.MANDARIN.name();
    return name;
  }

  public boolean isEnglish() {
    return getLanguageEnum() == Language.ENGLISH;
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

  @Override
  public SlickProject getProject() {
    return project;
  }

  @Override
  public ProjectType getKind() {
    return ProjectType.valueOf(project.kind());
  }

  boolean shouldLoad() {
    return project != null && !toSkip.contains(ProjectStatus.valueOf(project.status()));
  }

  /**
   * If we can't get the type order out of the section helper... find it from the project
   *
   * @return
   */
  @Override
  public List<String> getTypeOrder() {
    ISection<CommonExercise> sectionHelper = getSectionHelper();

    List<String> types = sectionHelper == null ? Collections.emptyList() : sectionHelper.getTypeOrder();
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

  /**
   * Cheesy code to avoid adding a duplicate type -- needed?
   *
   * @return
   */
  @NotNull
  public List<String> getBaseTypeOrder() {
    List<String> typeOrder = new ArrayList<>();
    SlickProject project1 = getProject();
    {
      String first = project1.first();
      if (!first.isEmpty()) {
        typeOrder.add(first);
      } else {
        logger.error("getBaseTypeOrder huh? project " + project + " first type is empty?");
      }
    }

    {
      String second = project1.second();
      if (!second.isEmpty() && !typeOrder.contains(second)) {
        typeOrder.add(second);
      }
    }
    return typeOrder;
  }

  /**
   * @param exerciseDAO
   * @see ProjectManagement#setExerciseDAO
   */
  public void setExerciseDAO(ExerciseDAO<CommonExercise> exerciseDAO) {
    this.exerciseDAO = exerciseDAO;
  }

  public ExerciseDAO<CommonExercise> getExerciseDAO() {
    return exerciseDAO;
  }

  @Override
  public List<CommonExercise> getRawExercises() {
    return exerciseDAO.getRawExercises();
  }

  @Override
  public ISection<CommonExercise> getSectionHelper() {
    return exerciseDAO == null ? null : exerciseDAO.getSectionHelper();
  }

  public synchronized ISection<IDialog> getDialogSectionHelper() {
    return dialogSectionHelper;
  }

  void setJsonSupport(JsonSupport jsonSupport) {
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
   * @see AudioServiceImpl#recalcRefAudio
   * @see mitll.langtest.client.project.ProjectEditForm#recalcRefAudio
   */
  public RecalcRefResponse recalcRefAudio(int userID) {
    Collection<CommonExercise> exercisesForUser = getRawExercises();
    logger.info("recalcRefAudio " + project + " for " + exercisesForUser.size() + " exercises.");
    return refResultDecoder.writeRefDecode(getLanguageEnum(), exercisesForUser, project.id(), userID);
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

  public ExerciseTrie<CommonExercise> getFullContextTrie() {
    return fullContextTrie;
  }

  void stopDecode() {
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
      prop = IProject.WEBSERVICE_HOST_DEFAULT;
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
    //  logger.info("getModelType (" + getID() + ") " + MODEL_TYPE + " : " + prop);

    if (prop == null || prop.isEmpty()) {
      return ModelType.HYDRA;
    } else {
      try {
        ModelType modelType = ModelType.valueOf(prop);
        //    logger.info("\tgetModelType (" + getID() + ") " + MODEL_TYPE + " : " + prop + " : " + modelType);
        return modelType;
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


  /**
   * Re populate cache really.
   */
  public void clearPropCache() {
//    logger.debug("clear project #" + getID());
    propCache.clear();
    putAllProps();
  }

  String getProp(ProjectProperty projectProperty) {
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
      //  logger.info("getProp : project " + getID() + " prop " + prop + " = " + propValue);

      if (propValue == null) {
        logger.warn("huh? no prop value for " + prop);
      } else {
        propCache.put(prop, propValue);
//        logger.info("getProp " + prop + " = " + propValue);
      }
      return propValue;
    } else {
      //  logger.info("getProp : project #" + getID() + " : '" + prop + "' = '" + s + "'");
      return s;
    }
  }

  private void putAllProps() {
    int id = getID();
    propCache.putAll(db.getProjectDAO().getProps(id));

    //  logger.info("getProp : project #" + getID() + " props " + propCache);
  }

  private int spew = 0;

  /**
   * @param id
   * @return
   * @see
   */
  @Override
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
   * @see ScoreServlet#getExerciseIDFromText
   */
  public CommonExercise getExerciseByExactMatch(String prefix) {
    return getMatchEitherFLOrEnglish(prefix, fullTrie.getExercises(prefix));
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
        exercise = getMatchEitherFLOrEnglish(english, fl, exercisesInVocab);
        logger.info("\tgetExerciseBySearchBoth looking for '" + english + " and " + fl +
            " found " + exercise);
      }

      if (exercise == null && !fl.isEmpty()) {
        List<CommonExercise> fullContextTrieExercises = fullContextTrie.getExercises(fl);
        logger.info("\tinitially context num = " + fullContextTrieExercises.size());
        exercise = getMatchEitherFLOrEnglish(english, fl, fullContextTrieExercises);
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

  /**
   * Case insensitive.
   *
   * @param prefix
   * @param exercises1
   * @return
   */
  private CommonExercise getMatchEitherFLOrEnglish(String prefix, List<CommonExercise> exercises1) {
    Optional<CommonExercise> first = exercises1
        .stream()
        .filter(p ->
            p.getForeignLanguage().equalsIgnoreCase(prefix) ||
                p.getEnglish().equalsIgnoreCase(prefix))
        .findFirst();
    return first.orElse(null);
  }

  private CommonExercise getMatchEitherFLOrEnglish(String prefix, String fl, List<CommonExercise> exercises1) {
    Optional<CommonExercise> first = exercises1
        .stream()
        .filter(p ->
            p.getForeignLanguage().equalsIgnoreCase(fl) ||
                p.getEnglish().equalsIgnoreCase(prefix))
        .findFirst();
    return first.orElse(null);
  }

  /**
   * @param transcript
   * @param transliteration
   * @return
   * @see SlickUserExerciseDAO#getExercisePhoneInfoFromDict
   */
  @Override
  public String getPronunciationsFromDictOrLTS(String transcript, String transliteration) {
    return hasModel() ? audioFileHelper.getPronunciationsFromDictOrLTS(transcript, transliteration) : "";
  }

  @Override
  public int getNumPhonesFromDictionary(String transcript, String transliteration) {
    return transcript.isEmpty() ? 0 : hasModel() ? audioFileHelper.getNumPhonesFromDictionary(transcript, transliteration) : 0;
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
   * ConcurrentHashMap!
   *
   * @return
   * @see AlignmentHelper#addAlignmentOutput(Project, Collection)
   */
  public ConcurrentMap<Integer, AlignmentOutput> getAudioToAlignment() {
    return audioToAlignment;
  }

  /**
   * So if you're on hydra2, you only handle certain languages...
   *
   * @return true if this server handles this project.
   */
  boolean isMyProject() {
    String hostName = serverProps.getHostName();

    boolean myProject = true;
    String webserviceHost = getWebserviceHost();

    if (hostName.startsWith(HYDRA_2)) {
      myProject = webserviceHost.equalsIgnoreCase(H_2);
    } else if (hostName.startsWith("score1")) {
      myProject = webserviceHost.equalsIgnoreCase("s1");
    } else if (hostName.startsWith("score2")) {
      myProject = webserviceHost.equalsIgnoreCase("s2");
    } else if (hostName.startsWith(HYDRA)) {
      myProject = webserviceHost.equalsIgnoreCase(IProject.WEBSERVICE_HOST_DEFAULT);
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

  /**
   * TODO NO why did you do this?
   *
   * @param fileToRecorder
   */
  public void setFileToRecorder(Map<String, Integer> fileToRecorder) {
    this.fileToRecorder = fileToRecorder;
  }

  Integer getUserForFile(String requestURI) {
    Integer user = fileToRecorder.get(requestURI);

    if (user == null) {
      if (unknownFiles.containsKey(requestURI)) {
        //logger.warn("getUserForFile (" + getID() + ") OK, already know we can't find the user for " + requestURI);
      } else {
        int studentForPath = db.getResultDAO().getStudentForPath(getID(), requestURI);
        if (studentForPath > 0) {
          fileToRecorder.put(requestURI, studentForPath);
          //logger.info("getUserForFile (" + getID() + ") remember '" + requestURI + "' = " + studentForPath + " now " + fileToRecorder.size());
          user = studentForPath;
        } else {
          unknownFiles.put(requestURI, false);
          if (DEBUG_FILE_LOOKUP) {
            logger.info("getUserForFile (" + getID() + ") can't find user " +
                "for " + requestURI + " = " + studentForPath + " now " + unknownFiles.size() + " " + unknownFiles.containsKey(requestURI));
          }
        }
      }
    } else {
      if (DEBUG_FILE_LOOKUP) {
        logger.info("getUserForFile (" + getID() + ") remember '" + requestURI + "' = " + user + " now " + fileToRecorder.size());
      }
    }
//    if (integer == null) {
    //     logger.warn("getUserForFile  can't find " + requestURI + " in " + fileToRecorder.size());
    //  }
    return user;
  }

  /**
   * @param testAudioFile
   * @param userIDFromSessionOrDB
   * @see ScoringServiceImpl#getPretestScore
   */
  public void addAnswerToUser(String testAudioFile, int userIDFromSessionOrDB) {
    fileToRecorder.put(testAudioFile, userIDFromSessionOrDB);
    //logger.info("addAnswerToUser project " + getProject().id() + " now has " + fileToRecorder.size());
  }

  int getPort() {
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

  /**
   * @return
   */
  public Collection<IDialog> getDialogs() {
    return idToDialog.values();
  }

  public IDialog getDialog(int id) {
    return idToDialog.get(id);
//    List<IDialog> collect = dialogs.stream().filter(dialog -> dialog.getID() == id).collect(Collectors.toList());
//    collect.forEach(logger::info);
//    return collect.get(0);
  }

  public Collection<Integer> getDialogExerciseIDs(int dialogID) {
    Set<Integer> dialogExercises = new HashSet<>();
    if (dialogID != -1) {
      IDialog first = getDialog(dialogID);//.stream().filter(iDialog -> iDialog.getID() == dialogID).findFirst();
      if (first != null) {
        first.getExercises().forEach(clientExercise -> dialogExercises.add(clientExercise.getID()));
        first.getCoreVocabulary().forEach(clientExercise -> dialogExercises.add(clientExercise.getID()));
      } else logger.warn("can't find dialog " + dialogID);
    }
    return dialogExercises;
  }

  /**
   * @param dialogs
   * @see DialogPopulate#addDialogInfo
   */
  public void setDialogs(List<IDialog> dialogs) {
    dialogs.forEach(dialog -> idToDialog.put(dialog.getID(), dialog));
    // this.dialogs = dialogs;
    createDialogSectionHelper(idToDialog.values());
  }

  private synchronized void createDialogSectionHelper(Collection<IDialog> dialogs) {
    dialogSectionHelper.clear();

    List<String> typeOrder = getTypeOrder();

    String unitType = typeOrder.size() > 0 ? typeOrder.get(0) : "";
    boolean hasUnitType = !unitType.isEmpty();

    String chapterType = typeOrder.size() > 1 ? typeOrder.get(1) : "";
    boolean hasChapterType = !chapterType.isEmpty();

    List<List<Pair>> seen = new ArrayList<>();
    dialogs.forEach(dialog -> {
      List<Pair> pairs = new ArrayList<>();
      {
        String unit = dialog.getUnit();
        if (!unit.isEmpty() && hasUnitType) {
          pairs.add(new Pair(unitType, unit));
        }
      }
      {
        String chapter = dialog.getChapter();
        if (!chapter.isEmpty() && hasChapterType) {
          pairs.add(new Pair(chapterType, chapter));
        }
      }

      pairs.addAll(dialog.getAttributes());
      this.dialogSectionHelper.addPairs(dialog, pairs);

      seen.add(pairs);
    });

    dialogSectionHelper.rememberTypesInOrder(typeOrder, seen);

    if (REPORT_ON_DIALOG_TYPES) {
      logger.info("report on dialog types");
      dialogSectionHelper.report();
    }
  }

  public JsonExport getJsonExport() {
    return jsonExport;
  }

  public void setJsonExport(JsonExport jsonExport) {
    this.jsonExport = jsonExport;
  }

  public PathHelper getPathHelper() {
    return pathHelper;
  }

  public String toString() {
    return "Project\n\t(" + getTypeOrder() + ") : project " + project;// + "\n\ttypes " + getTypeOrder() + " exercise dao " + exerciseDAO;
  }

  /**
   * @param id
   * @return true if removed the dialog
   */
  public boolean forgetDialog(int id) {
    boolean b = idToDialog.remove(id) != null;
    if (b) {
      createDialogSectionHelper(idToDialog.values());
    }
    return b;
  }
}
