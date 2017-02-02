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

import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.userexercise.BaseUserExerciseDAO;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.server.database.userexercise.SlickUserExerciseDAO;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.Exercise;
import mitll.npdata.dao.SlickProject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/10/16.
 */
abstract class BaseExerciseDAO implements SimpleExerciseDAO<CommonExercise> {
  private static final Logger logger = LogManager.getLogger(BaseExerciseDAO.class);
  private static final String CONTAINS_SEMI = "contains semicolon - should this item be split?";
  private static final String ENGLISH = "english";
  private static final String MISSING_ENGLISH = "missing english";
  public static final boolean DEBUG = false;

  private final Map<Integer, CommonExercise> idToExercise = new HashMap<>();
  protected final SectionHelper<CommonExercise> sectionHelper = new SectionHelper<>();
  protected final String language;
  protected final ServerProperties serverProps;
  private final IUserListManager userListManager;
  private final boolean addDefects;

  private List<CommonExercise> exercises = null;
  private AddRemoveDAO addRemoveDAO;
  /**
   * @see #addNewExercises
   * @see #addOverlays
   * @see #setDependencies
   */
  private IUserExerciseDAO userExerciseDAO;
  private AttachAudio attachAudio;
  private IAudioDAO audioDAO;
  private int id;

  /**
   * @param serverProps
   * @param userListManager
   * @param addDefects
   * @param language
   * @see DBExerciseDAO#DBExerciseDAO
   */
  BaseExerciseDAO(ServerProperties serverProps, IUserListManager userListManager, boolean addDefects,
                  String language,
                  int id) {
    this.serverProps = serverProps;
    this.userListManager = userListManager;
    this.language = language;
    this.addDefects = addDefects;
    this.id = id;
  }

  /**
   * Only for AMAS.
   * @return
   */
  public int getNumExercises() {
    return getRawExercises().size();
  }

  public Set<Integer> getIDs() { return idToExercise.keySet(); }

  /**
   * @return
   * @see DatabaseImpl#getExercises(int)
   */
  public List<CommonExercise> getRawExercises() {
    synchronized (this) {
      if (exercises == null) {
        exercises = readExercises();
        afterReadingExercises();
      }
    }
    return exercises;
  }

  /**
   * Mainly to support reading from Domino after edits in Domino
   *
   * @see DatabaseImpl#reloadExercises(int)
   */
  public void reload() {
    exercises = null;
    idToExercise.clear();
    sectionHelper.clear();
    getRawExercises();
  }

  @Override
  public List<String> getTypeOrder() {
    return getSectionHelper().getTypeOrder();
  }

  /**
   * Do steps after reading the exercises.
   */
  private void afterReadingExercises() {
    addAlternatives(exercises);

    populateIdToExercise();

    addDefects(findDefects());

    // remove exercises to remove
    // mask over old items that have been overridden
    addOverlays(removeExercises());

    // add new items
    addNewExercises();

    attachAudio();
  }

  /**
   * Is this kosher to do - exToAudio
   * Worry about if the audio transcript doesn't match the exercise transcript
   * @see #setAudioDAO
   */
  private void attachAudio() {
    Set<Integer> transcriptChanged = new HashSet<>();

    logger.info("attachAudio afterReadingExercises trying to attach audio to " + exercises.size() + " with project id " + id);

    int c = 0;

    Map<Integer, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio(id);
    attachAudio.setExToAudio(exToAudio, getMultiPronWords(exercises));
    Set<String> allTranscripts = new HashSet<>();
    for (List<AudioAttribute> audioAttributes : exToAudio.values()) {
      for (AudioAttribute audioAttribute : audioAttributes) {
        allTranscripts.add(audioAttribute.getTranscript().toLowerCase());
      }
    }
    for (CommonExercise ex : exercises) {
      attachAudio.attachAudio(ex, transcriptChanged);

      Collection<AudioAttribute> audioAttributes = ex.getAudioAttributes();
      for (AudioAttribute audioAttribute : audioAttributes) {
        allTranscripts.remove(audioAttribute.getTranscript().toLowerCase());
      }

 /*     if (i < 25) {
        if (c++ < 25) {
          logger.warn(language + " (" + exercises.size() +
              ") -----------> adding old school audio for " + ex.getID() + " : " + serverProps.getLessonPlan());
        }
        String refAudioIndex = ex.getRefAudioIndex();
        if (refAudioIndex != null && !refAudioIndex.isEmpty()) {
          attachAudio.addOldSchoolAudio(refAudioIndex, (AudioExercise) ex);
        }
      }*/
    }

    logger.info("attachAudio found " + allTranscripts.size() + " orphan audio cuts - ");// + allTranscripts);

    //consistencyCheck();

    if (!transcriptChanged.isEmpty()) {
      logger.info("attachAudio afterReadingExercises : found " + transcriptChanged.size() + " changed transcripts in set of " + exercises.size() + " items");
    }
  }

  /**
   * @return
   * @see DatabaseImpl#getSectionHelper(int)
   * @see #addNewExercises()
   * @see #populateSections(Collection)
   */
  public SectionHelper<CommonExercise> getSectionHelper() {
    return sectionHelper;
  }

  /**
   * There must be an /opt/netProf/bestAudio directory for audio.
   *
   * Only validate audio when there is audio to validate.
   * <p>
   * TODO : what if they add a user exercise and add audio to it, or record new audio for other exercises???
   *
   * @param audioDAO
   * @param projectID
   * @see #setDependencies
   */
  private void setAudioDAO(IAudioDAO audioDAO, int projectID) {
    this.audioDAO = audioDAO;
//    File fileInstallPath = new File(installPath);
//    if (!fileInstallPath.exists()) {
//      logger.warn("\n\n\nhuh? install path " + fileInstallPath.getAbsolutePath() + " doesn't exist???");
//    }

    if (!serverProps.isLaptop()) {
      makeSureAudioIsThere(audioDAO, projectID);
    }

    Map<Integer, List<AudioAttribute>> exToAudio = audioDAO.getExToAudio(projectID);
    logger.info("setAudioDAO exToAudio " +exToAudio.size());
    this.attachAudio = new AttachAudio(exToAudio, language, serverProps.shouldCheckAudioTranscript(), serverProps);
  }

  private void makeSureAudioIsThere(IAudioDAO audioDAO, int projectID) {
    boolean foundFiles = audioDAO.didFindAnyAudioFiles(projectID);
    String mediaDir = serverProps.getMediaDir();
    File file = new File(mediaDir);
    if (file.exists()) {
      if (file.isDirectory()) {
        String[] list = file.list();
        if (list == null) {
          logger.error("setAudioDAO configuration error - can't get files from media directory " + mediaDir);
        } else if (list.length > 0) { // only on pnetprof (behind firewall), znetprof has no audio, might have a directory.
          logger.debug("setAudioDAO validating files under " + file.getAbsolutePath());
          if (serverProps.doAudioChecksInProduction() &&
              (serverProps.doAudioFileExistsCheck() || !foundFiles)) {
            audioDAO.validateFileExists(projectID, mediaDir, language);
          }
        }
      } else {
        logger.error("configuration error - expecting media directory " + mediaDir + " to be directory.");

      }
    } else {
      logger.warn("configuration error? - expecting a media directory " + mediaDir);
    }
  }

  /**
   * TODO : make it easy to look up by domino id.
   * Populate the lookup map.
   *
   * @see #afterReadingExercises
   */
  private void populateIdToExercise() {
    logger.info("populateIdToExercise Examining " + exercises.size() + " exercises");
    for (CommonExercise e : exercises) {
      idToExercise.put(e.getID(), e);
      idToExercise.put(e.getDominoID(), e);
    }

    int listSize = exercises.size();
    int mapSize = idToExercise.size();
    if (listSize != mapSize) {
      logger.warn(listSize + " but id->ex " + mapSize);
    }
  }

  /**
   * So the story is we get the user exercises out of the database on demand.
   * <p>
   * We need to join with the audio table entries every time.
   * <p>
   * TODO : also, this a lot of work just to get the one ref audio recording.
   *
   * @param all
   * @see DatabaseImpl#getExerciseIDToRefAudio(int)
   */
  public void attachAudio(Collection<CommonExercise> all) {
    int projectid = all.isEmpty() ? -1 : all.iterator().next().getProjectID();
    logger.info("attachAudio (" + projectid +
        ")" +
        " attach audio to " + all.size() + " exercises");

    attachAudio.setExToAudio(audioDAO.getExToAudio(projectid), getMultiPronWords(all));
    int user = 0;
    int examined = 0;

    Collection<Integer> transcriptChanged = new HashSet<>();

    for (CommonExercise ex : all) {
      if (!ex.hasRefAudio()) {
        attachAudio.attachAudio(ex, transcriptChanged);
        examined++;
        if (!ex.hasRefAudio()) user++;
      }
    }
    if (user > 0) {
      logger.info("attachAudio out of " + exercises.size() + //" " + missing +
          " are missing ref audio, out of " + examined + " user exercises missing = " + user);
    }

    if (!transcriptChanged.isEmpty()) {
      logger.info("attachAudio : found " + transcriptChanged.size() + " changed transcripts in set of " +
          exercises.size() + " items");
    }
  }

  /**
   * The idea here is to
   * @param all
   * @return
   */
  private Set<String> getMultiPronWords(Collection<CommonExercise> all) {
    Map<String, String> seen = new HashMap<>();
    Set<String> multiPron = new HashSet<>();

    for (CommonExercise ex : all) {
      String foreignLanguage = ex.getForeignLanguage();
      String english = seen.get(foreignLanguage);
      String english1 = ex.getEnglish();
      if (english != null && !english.equals(english1)) {
        multiPron.add(foreignLanguage);
//        logger.info("getMultiPronWords before " + foreignLanguage + " eng " + english + " vs " + english1);
      }
      seen.put(foreignLanguage, english1);
    }
    return multiPron;
  }

  /**
   * TODO : better to use a filter
   * Don't add overlays for exercises that have been removed.
   *
   * @param removes
   * @see #setDependencies
   * @deprecated only for old h2 world with immutable exercises from spreadsheet
   */
  protected void addOverlays(Collection<Integer> removes) {
    Collection<CommonExercise> overrides = userExerciseDAO.getOverrides();

    if (overrides.size() > 0) {
      logger.debug("addOverlays found " + overrides.size() + " overrides : ");
      // for (CommonUserExercise exercise : overrides) logger.info("\t" + exercise);
    }
    if (removes.size() > 0) {
      logger.debug("addOverlays found " + removes.size() + " to remove " + removes);
    }

    int override = 0;
    int skippedOverride = 0;
    int removedSoSkipped = 0;
    SortedSet<Integer> staleOverrides = new TreeSet<>();
    for (CommonExercise userExercise : overrides) {
      int overrideID = userExercise.getID();
      if (removes.contains(overrideID)) {
        removedSoSkipped++;
      } else {
        if (isKnownExercise(overrideID)) {
          //  logger.info("for " + overrideID + " got " + userExercise.getUnitToValue());
          // don't use the unit->value map stored in the user originalExercise table...
          CommonExercise originalExercise = getExercise(overrideID);

          long predefUpdateTime = originalExercise.getUpdateTime();
          long userExUpdateTime = userExercise.getUpdateTime();

          if (userExUpdateTime > predefUpdateTime) {
            Map<String, String> unitToValue = originalExercise.getUnitToValue();
            ((Exercise) userExercise).setUnitToValue(unitToValue);

            logger.debug("addOverlays refresh originalExercise for " + userExercise.getID() + " '" + userExercise.getForeignLanguage() +
                "' vs '" + originalExercise.getForeignLanguage() +
                "'");
            sectionHelper.refreshExercise(userExercise);
            addOverlay(userExercise);

//          Collection<CommonExercise> exercisesForSimpleSelectionState = sectionHelper.getExercisesForSimpleSelectionState(unitToValue);
//          for (CommonExercise originalExercise:exercisesForSimpleSelectionState) if (originalExercise.getOldID().equals(overrideID)) logger.warn("found " + originalExercise);
            override++;
          } else {
            skippedOverride++;
            if (skippedOverride < 5)
              logger.info("for " + overrideID + " skipping override, since predef originalExercise is newer " + new Date(predefUpdateTime) + " > " + new Date(userExUpdateTime));
          }
        } else {
          staleOverrides.add(overrideID);
          //logger.warn("----> addOverlays not adding as overlay '" + overrideID + "' since it's not in the original list of size " + idToExercise.size());
        }
      }
    }
    if (override > 0) {
      logger.debug("addOverlays overlay count was " + override);
    }
    if (skippedOverride > 0) {
      logger.debug("addOverlays skippedOverride count was " + skippedOverride);
    }
    if (!staleOverrides.isEmpty()) {
      logger.debug("addOverlays skipped " + staleOverrides.size() + " stale overrides - the original list doesn't contain them any more.");
    }
    if (removedSoSkipped > 0) {
      logger.debug("addOverlays skipped overlays b/c removed = " + removedSoSkipped);
    }
  }

  /**
   * When the QC process edits an exercise, a copy of the original becomes a UserExercise overlay,
   * which masks out the original.
   *
   * @param userExercise
   * @return old exercises
   * @see DatabaseImpl#editItem
   */
  public CommonExercise addOverlay(CommonExercise userExercise) {
    int idOfNewExercise = userExercise.getID();
    CommonExercise currentExercise = getExercise(idOfNewExercise);

    if (currentExercise == null) {
      logger.error("addOverlay : huh? can't find " + userExercise);
    } else {
      //logger.debug("addOverlay at " +userExercise.getOldID() + " found " +currentExercise);
      synchronized (this) {
        int i = exercises.indexOf(currentExercise);
        if (i == -1) {
          logger.error("addOverlay : huh? couldn't find " + currentExercise);
        } else {
          exercises.set(i, userExercise);
        }
        idToExercise.put(idOfNewExercise, userExercise);
        //  logger.debug("addOverlay : after " + getExercise(userExercise.getOldID()));
      }
    }
    return currentExercise;
  }

  /**
   * @param ue
   * @see DatabaseImpl#duplicateExercise
   * @see #addNewExercises()
   */
  public void add(CommonExercise ue) {
    synchronized (this) {
      exercises.add(ue);
      idToExercise.put(ue.getID(), ue);

      if (exercises.size() != idToExercise.size()) {
        logger.warn("add " + exercises.size() + " exercises but id->ex " + idToExercise.size());
      }
    }
  }

  /**
   * @param id
   * @return true if exercise with this id was removed
   * @see DatabaseImpl#deleteItem(int, int)
   */
  public boolean remove(int id) {
    synchronized (this) {
      CommonExercise remove = idToExercise.remove(id);
      return remove != null && exercises.remove(remove);
    }
  }

  /**
   * This DAO needs to talk to other DAOs.
   *
   * @param userExerciseDAO
   * @param addRemoveDAO
   * @param audioDAO
   * @param projid
   * @see mitll.langtest.server.database.project.ProjectManagement#setDependencies
   */
  public void setDependencies(IUserExerciseDAO userExerciseDAO,
                              AddRemoveDAO addRemoveDAO,
                              IAudioDAO audioDAO,
                              int projid) {
    this.userExerciseDAO = userExerciseDAO;
    this.addRemoveDAO = addRemoveDAO;
    setAudioDAO(audioDAO, projid);
  }

  public boolean isConfigured() { return audioDAO != null; }

  private int warns = 0;

  /**
   * Worries about colliding with add and remove on the idToExercise map.
   * NO database interaction - just map lookup by id.
   *
   * @param id
   * @return
   * @see BaseUserExerciseDAO#getPredefExercise
   * @see DatabaseImpl#getExercise
   */
  public CommonExercise getExercise(int id) {
    synchronized (this) {
      CommonExercise commonExercise = idToExercise.get(id);
      if (commonExercise == null) {
        if (warns++ < 50)
          logger.warn(this + " couldn't find exercise " + id + " in " + idToExercise.size() + " exercises");
      }
      return commonExercise;
    }
  }

  /**
   * @param exTofieldToDefect
   * @see #afterReadingExercises()
   */
  private void addDefects(Map<Integer, Map<String, String>> exTofieldToDefect) {
    if (addDefects) {
      int count = 0;
      logger.info("adding defects, num = " + exTofieldToDefect.size());

      for (Map.Entry<Integer, Map<String, String>> pair : exTofieldToDefect.entrySet()) {
        for (Map.Entry<String, String> fieldToDefect : pair.getValue().entrySet()) {
          if (userListManager.addDefect(pair.getKey(), fieldToDefect.getKey(), fieldToDefect.getValue())) {
            count++;
          }
        }
      }
      if (count > 0) {
        logger.info("Automatically added " + exTofieldToDefect.size() + "/" + count + " defects");
      }
    } else {
      if (DEBUG) logger.info("NOT Automatically adding " + exTofieldToDefect.size() + " defects");
    }
  }

  /**
   * Look at the exercises and automatically flag defects
   *
   * @return
   */
  private Map<Integer, Map<String, String>> findDefects() {
    Map<Integer, Map<String, String>> idToDefectMap = new HashMap<>();

    for (CommonExercise shell : exercises) {
      Map<String, String> fieldToDefect = new HashMap<>();
      checkForSemicolons(fieldToDefect, shell.getForeignLanguage(), shell.getTransliteration());

      if (shell.getEnglish().isEmpty()) {
        fieldToDefect.put(ENGLISH, MISSING_ENGLISH);
      }

      if (!fieldToDefect.isEmpty()) {
        idToDefectMap.put(shell.getID(), fieldToDefect);
      }
    }
    return idToDefectMap;
  }

  /**
   * TODO: Need to turn this back on eventually
   *
   * @see #getRawExercises()
   */
  @Deprecated
  private void addNewExercises() {
    if (addRemoveDAO != null) {
      for (AddRemoveDAO.IdAndTime id : addRemoveDAO.getAdds()) {
        CommonExercise where = userExerciseDAO.getByExID(id.getId());
        if (where == null) {
          logger.error("getRawExercises huh? couldn't find user exercise from add exercise table in user exercise table : " + id);
        } else {
          int id1 = where.getID();
          if (isKnownExercise(id.getId())) {
            logger.debug("addNewExercises SKIPPING new user exercise " + id1 + " since already added from spreadsheet : " + where);
          } else {
            logger.debug("addNewExercises adding new user exercise " + id1 + " : " + where);
            add(where);
            getSectionHelper().addExercise(where);
          }
        }
      }
    }
  }

  private boolean isKnownExercise(int id) {
    return idToExercise.containsKey(id);
  }

  /**
   * Some exercises are marked as deleted - remove them from the list of current exercises.
   *
   * @return
   */
  private Collection<Integer> removeExercises() {
    if (addRemoveDAO != null) {
      Collection<AddRemoveDAO.IdAndTime> removes = addRemoveDAO.getRemoves();

      if (!removes.isEmpty())
        logger.debug("removeExercises : Removing " + removes.size() + " exercises marked as deleted.");

      Set<Integer> idsToRemove = new HashSet<>();
      for (AddRemoveDAO.IdAndTime id : removes) {
        CommonExercise remove = idToExercise.remove(id.getId());
        if (remove != null && remove.getUpdateTime() < id.getTimestamp()) {
          boolean remove1 = exercises.remove(remove);
          if (!remove1) logger.error("huh? remove inconsistency??");
          getSectionHelper().removeExercise(remove);
          idsToRemove.add(id.getId());
        }
      }
      return idsToRemove;
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * Keep track of possible alternatives for each english word - e.g. Good Bye = Ciao OR Adios
   */
  private void addAlternatives(List<CommonExercise> exercises) {
    Map<String, Set<String>> englishToFL = new HashMap<>();
    for (CommonExercise e : exercises) {
      Set<String> defaultValue = new HashSet<>();
      Set<String> refs = englishToFL.getOrDefault(e.getEnglish(), defaultValue);
      if (refs.isEmpty()) englishToFL.put(e.getEnglish(), refs);
      refs.add(e.getForeignLanguage());
    }

    for (CommonExercise e : exercises) {
      final Set<String> defaultValue = new HashSet<>();
      Set<String> orDefault = englishToFL.getOrDefault(e.getEnglish(), defaultValue);
      if (orDefault.isEmpty()) {
        logger.error("huh? no fl for " + e);
      } else {
        e.getMutable().setRefSentences(orDefault);
        //   if (orDefault.size() > 1) logger.info("For " + e.getOldID() + " found " + orDefault.size());
      }
    }
  }

  /**
   * Actually read the exercises from a datasource.
   * Could be an excel spreadsheet, csv file, or URL.
   *
   * @return
   */
  abstract List<CommonExercise> readExercises();

  /**
   * @param fieldToDefect
   * @param foreignLanguagePhrase
   * @param translit
   * @see #findDefects()
   */
  private void checkForSemicolons(Map<String, String> fieldToDefect, String foreignLanguagePhrase, String translit) {
    if (foreignLanguagePhrase.contains(";")) {
      fieldToDefect.put(QCNPFExercise.FOREIGN_LANGUAGE, CONTAINS_SEMI);
    }
    if (translit.contains(";")) {
      fieldToDefect.put(QCNPFExercise.TRANSLITERATION, CONTAINS_SEMI);
    }
/*    if (INCLUDE_ENGLISH_SEMI_AS_DEFECT && english.contains(";")) {
      fieldToDefect.put(QCNPFExercise.ENGLISH, "contains semicolon - should this item be split?");
    }*/
  }

  /**
   * Build the nested hierarchy represented in the section helper.
   * So we can ask for things like all exercises in Chapter 1.
   *
   * @param exercises
   */
  void populateSections(Collection<CommonExercise> exercises) {
    for (CommonExercise ex : exercises) {
      Collection<SectionHelper.Pair> pairs = new ArrayList<>();
      for (Map.Entry<String, String> pair : ex.getUnitToValue().entrySet()) {
        pairs.add(getSectionHelper().addExerciseToLesson(ex, pair.getKey(), pair.getValue()));
      }
      getSectionHelper().addAssociations(pairs);
    }
  }
}
