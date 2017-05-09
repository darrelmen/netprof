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
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.trie.ExerciseTrie;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.project.ProjectStatus;
import mitll.npdata.dao.SlickProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Has everything associated with a project
 */
public class Project implements PronunciationLookup {
  private static final Logger logger = LogManager.getLogger(Project.class);

  private static final String WEBSERVICE_HOST_IP1 = "webserviceHostIP";
  public static final String WEBSERVICE_HOST_PORT = "webserviceHostPort";
  private static final String WEBSERVICE_HOST_IP = "127.0.0.1";

  private SlickProject project;
  private ExerciseDAO<CommonExercise> exerciseDAO;
  private JsonSupport jsonSupport;
  private SlickAnalysis analysis;
  private AudioFileHelper audioFileHelper;
  private ExerciseTrie<CommonExercise> fullTrie = null;
  private ExerciseTrie<CommonExercise> fullContextTrie = null;
  private RefResultDecoder refResultDecoder;
  private PathHelper pathHelper;
  private DatabaseImpl db;
  private ServerProperties serverProps;
  private boolean isRTL;
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
   * @see DatabaseImpl#rememberProject
   */
  public Project(SlickProject project,
                 PathHelper pathHelper,
                 ServerProperties serverProps,
                 DatabaseImpl db,
                 LogAndNotify logAndNotify) {
    this.project = project;
    audioFileHelper = new AudioFileHelper(pathHelper, serverProps, db, logAndNotify, this);
    this.db = db;
    this.serverProps = serverProps;
    this.pathHelper = pathHelper;
  }

  public void setProject(SlickProject project) {
    this.project = project;
  }

  public String getLanguage() {
    return project == null ? "No project yet" : project.language();
  }

  public boolean isEnglish() {
    return getLanguage().equalsIgnoreCase("english");
  }


  private SmallVocabDecoder getSmallVocabDecoder() {
    return getAudioFileHelper() == null ? null : getAudioFileHelper().getSmallVocabDecoder();
  }

  public boolean isNoModel() {
    return getModelsDir() == null;
  }

  public boolean hasModel() {
    return !isNoModel();
  }

  public SlickProject getProject() {
    return project;
  }

  public boolean isRetired() {
    return project != null && ProjectStatus.valueOf(project.status()) == ProjectStatus.RETIRED;
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
      //logger.info("getTypeOrder Type order " + types);
    }

    return types;
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

  public void setJsonSupport(JsonSupport jsonSupport) {
    this.jsonSupport = jsonSupport;
  }

  public JsonSupport getJsonSupport() {
    return jsonSupport;
  }

  /**
   * @param analysis
   * @see IProjectManagement#configureProject
   */
  public void setAnalysis(SlickAnalysis analysis) {
    this.analysis = analysis;
    buildExerciseTrie();
    this.refResultDecoder = new RefResultDecoder(db, serverProps, pathHelper, getAudioFileHelper(), hasModel());
  }

  /**
   * Only public to support deletes...
   *
   * @see mitll.langtest.server.services.QCServiceImpl#deleteItem
   */
  public <T extends CommonShell> void buildExerciseTrie() {
    fullTrie = new ExerciseTrie<>(getRawExercises(), project.language(), getSmallVocabDecoder(), true);
    fullContextTrie = new ExerciseTrie<>(getRawExercises(), project.language(), getSmallVocabDecoder(), false);
  }

  /**
   * @see mitll.langtest.server.services.AudioServiceImpl#recalcRefAudio
   */
  public void recalcRefAudio() {
    Collection<CommonExercise> exercisesForUser = getRawExercises();
    logger.info("recalcRefAudio " + project + " " + exercisesForUser.size() + " exercises.");
    refResultDecoder.writeRefDecode(getLanguage(), exercisesForUser, project.id());
  }

  public SlickAnalysis getAnalysis() {
    return analysis;
  }

  public AudioFileHelper getAudioFileHelper() {
    return audioFileHelper;
  }

  public ExerciseTrie<CommonExercise> getFullTrie() {
    return fullTrie;
  }

  public ExerciseTrie<CommonExercise> getFullContextTrie() {
    return fullContextTrie;
  }

  public void stopDecode() {
    if (refResultDecoder != null) refResultDecoder.setStopDecode(true);
  }

  public String getWebserviceIP() {
    String prop = getProp(WEBSERVICE_HOST_IP1);
    if (prop == null) prop = WEBSERVICE_HOST_IP;
    return prop;
  }

  public int getWebservicePort() {
    String prop = getProp(WEBSERVICE_HOST_PORT);
    if (prop == null) prop = "-1";
    int ip = Integer.parseInt(prop);
    if (ip == 1)
      logger.error("No webservice host port found.");
    return ip;
  }

  public String getModelsDir() {
    return project.getProp(ServerProperties.MODELS_DIR);
  }

  private String getProp(String webserviceHostIp1) {
    return getProject().getProp(webserviceHostIp1);
  }

  public CommonExercise getExerciseByID(int id) {
    return exerciseDAO.getExercise(id);
  }

  /**
   * Only accept an exact match
   *
   * @param prefix
   * @return
   * @see mitll.langtest.server.ScoreServlet#getExerciseIDFromText
   * @see mitll.langtest.server.services.ListServiceImpl#getExerciseByVocab
   */
  public CommonExercise getExerciseBySearch(String prefix) {
    List<CommonExercise> exercises1 = fullTrie.getExercises(prefix);
    return getMatchEither(prefix, exercises1);
  }

  public CommonExercise getExerciseBySearchBoth(String english, String fl) {
//    logger.info("getExerciseBySearchBoth looking for '" + english +
//        "' and '" + fl +
//        "' found ");
    List<CommonExercise> exercisesInVocab = fullTrie.getExercises(english);
    CommonExercise exercise = getFirstMatchingLength(english, fl, exercisesInVocab);

    logger.info("getExerciseBySearchBoth looking for '" + english +
        "' and '" + fl +
        "' found " + exercise);

    if (exercise == null && !english.isEmpty()) {
      List<CommonExercise> fullContextTrieExercises = fullContextTrie.getExercises(english);
      exercise = getFirstMatchingLength(english, fl, fullContextTrieExercises);
      if (exercise != null && !exercise.getDirectlyRelated().isEmpty()) {
        exercise = exercise.getDirectlyRelated().iterator().next();
      }
      logger.info("\tgetExerciseBySearchBoth context looking for '" + english + "' found " + exercise);
    }

    if (exercise == null) {
      exercise = getMatchEither(english, fl, exercisesInVocab);
      logger.info("\tgetExerciseBySearchBoth looking for '" + english + " and " +fl +
          " found " + exercise);
    }

    if (exercise == null && !fl.isEmpty()) {
      List<CommonExercise> fullContextTrieExercises = fullContextTrie.getExercises(fl);
      logger.info("\tinitially context num = " + fullContextTrieExercises.size());
      exercise = getMatchEither(english, fl, fullContextTrieExercises);
      if (exercise != null && !exercise.getDirectlyRelated().isEmpty()) {
        exercise = exercise.getDirectlyRelated().iterator().next();
      }
      logger.info("\tgetExerciseBySearchBoth context looking for '" + english + " or '" +fl+
          "' found " + exercise);
      if (exercise == null && !fullContextTrieExercises.isEmpty()) {
        exercise = fullContextTrieExercises.iterator().next();
        if (exercise != null && !exercise.getDirectlyRelated().isEmpty()) {
          exercise = exercise.getDirectlyRelated().iterator().next();
        }
        logger.info("\tnow returning " + exercise);
      }
    }

    return exercise;
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

  @Override
  public String getPronunciations(String transcript, String transliteration) {
    return hasModel() ? audioFileHelper.getPronunciations(transcript, transliteration) : "";
  }

  @Override
  public int getNumPhones(String transcript, String transliteration) {
    return hasModel() ? audioFileHelper.getNumPhones(transcript, transliteration) : 0;
  }

  /*
  public Map<Integer, ExercisePhoneInfo> getExToPhone() {
    return exToPhone;
  }
*/

  /**
   * @return
   * @see ProjectManagement#configureProjects
   * @see ProjectManagement#getExercises
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

  public int getID() {
    return project.id();
  }

  public void ensureAudio(Set<CommonExercise> toAddAudioTo) {
    refResultDecoder.ensure(getLanguage(), toAddAudioTo);
  }

  public String toString() {
    return "Project project = " + project + " types " + getTypeOrder() + " exercise dao " + exerciseDAO;
  }
}
