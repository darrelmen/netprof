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
import mitll.langtest.server.decoder.RefResultDecoder;
import mitll.langtest.server.scoring.SmallVocabDecoder;
import mitll.langtest.server.trie.ExerciseTrie;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.npdata.dao.SlickProject;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Has everything associated with a project
 */
public class Project {
  private static final Logger logger = Logger.getLogger(Project.class);

  private static final String WEBSERVICE_HOST_IP1 = "webserviceHostIP";
  private static final String WEBSERVICE_HOST_PORT = "webserviceHostPort";
  private static final String WEBSERVICE_HOST_IP = "127.0.0.1";

  private SlickProject project;
  private List<String> typeOrder;
  private ExerciseDAO<CommonExercise> exerciseDAO;
  private JsonSupport jsonSupport;
  private SlickAnalysis analysis;
  private AudioFileHelper audioFileHelper;
  private ExerciseTrie<CommonExercise> fullTrie = null;
  private RefResultDecoder refResultDecoder;
  private String relativeConfigDir;
  private PathHelper pathHelper;
  private DatabaseImpl db;
  private ServerProperties serverProps;

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#addSingleProject(ExerciseDAO)
   * @param exerciseDAO
   */
  public Project(ExerciseDAO<CommonExercise> exerciseDAO) {
    this.exerciseDAO = exerciseDAO;
  }

  public Project(SlickProject project,
                 PathHelper pathHelper,
                 ServerProperties serverProps,
                 DatabaseImpl db,
                 LogAndNotify logAndNotify,
                 String relativeConfigDir) {
    this.project = project;
    this.typeOrder = Arrays.asList(project.first(), project.second());
    // String prop = project.getProp(ServerProperties.MODELS_DIR);
    // logger.info("Project got " + ServerProperties.MODELS_DIR + ": " + prop);
    audioFileHelper = new AudioFileHelper(pathHelper, serverProps, db, logAndNotify, this);
    // logger.info("Project got " + audioFileHelper);
    // logger.info("Project got " + audioFileHelper.getCollator());

    this.relativeConfigDir = relativeConfigDir;
    this.db = db;
    this.serverProps = serverProps;
    this.pathHelper = pathHelper;
  }

  public String getLanguage() { return project.language();  }

  /**
   * Only public to support deletes...
   */
  public <T extends CommonShell> void buildExerciseTrie(DatabaseImpl db) {
    logger.info("db " + db);
    logger.info("audioFileHelper " + getAudioFileHelper());
    fullTrie = new ExerciseTrie<>(getExercisesForUser(), project.language(), getSmallVocabDecoder());
  }

  private Collection<CommonExercise> getExercisesForUser() {
    return getRawExercises();
  }

  private SmallVocabDecoder getSmallVocabDecoder() {
    return getAudioFileHelper() == null ? null : getAudioFileHelper().getSmallVocabDecoder();
  }


  public boolean isNoModel() {
    return project.getProp(ServerProperties.MODELS_DIR) == null;
  }

  public boolean hasModel() {
    return !isNoModel();
  }

  public SlickProject getProject() {
    return project;
  }

  public List<String> getTypeOrder() {
    return typeOrder;
  }

  /**
   * @param exerciseDAO
   * @see DatabaseImpl#setExerciseDAO(Project) (String, boolean)
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

  public SectionHelper<CommonExercise> getSectionHelper() {
    return exerciseDAO.getSectionHelper();
  }

  public void setJsonSupport(JsonSupport jsonSupport) {
    this.jsonSupport = jsonSupport;
  }

  public JsonSupport getJsonSupport() {
    return jsonSupport;
  }

  public void setAnalysis(SlickAnalysis analysis) {
    this.analysis = analysis;
    String language = project == null ? "unk" : project.language();
    fullTrie = new ExerciseTrie<>(getExercisesForUser(), language, getSmallVocabDecoder());
    this.refResultDecoder = new RefResultDecoder(db, serverProps, pathHelper, getAudioFileHelper(), hasModel());
    refResultDecoder.doRefDecode(getExercisesForUser(), relativeConfigDir);
  }

  public SlickAnalysis getAnalysis() {
    return analysis;
  }

  public AudioFileHelper getAudioFileHelper() {
    return audioFileHelper;
  }

  public ExerciseTrie getFullTrie() {
    return fullTrie;
  }

  public void stopDecode() {
    refResultDecoder.setStopDecode(true);
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

  public CommonExercise getExercise(int id) {
    return exerciseDAO.getExercise(id);
  }

  public String toString() {
    return "Project project = " + project + " types " + typeOrder + " exercise dao " + exerciseDAO;
  }
}
