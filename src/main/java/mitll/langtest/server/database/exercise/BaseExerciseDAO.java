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

package mitll.langtest.server.database.exercise;

import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.userexercise.BaseUserExerciseDAO;
import mitll.langtest.server.database.userexercise.IUserExerciseDAO;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.Exercise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

abstract class BaseExerciseDAO implements SimpleExerciseDAO<CommonExercise> {
  private static final Logger logger = LogManager.getLogger(BaseExerciseDAO.class);
  private static final String CONTAINS_SEMI = "contains semicolon - should this item be split?";
  private static final String ENGLISH = "english";
  private static final String MISSING_ENGLISH = "missing english";
  private static final boolean DEBUG = false;
  private static final String SEMI = ";";
  private static final int MAX_WARNS = Integer.MAX_VALUE;

  /**
   * @see
   */
  final Map<Integer, CommonExercise> idToExercise = new HashMap<>();
  private final Map<String, CommonExercise> oldidToExercise = new HashMap<>();

  private final ISection<CommonExercise> sectionHelper = new SectionHelper<>();

  final String language;
  final ServerProperties serverProps;
  private final IUserListManager userListManager;
  private final boolean addDefects;

  private List<CommonExercise> exercises = null;
  AddRemoveDAO addRemoveDAO;
  /**
   * @see #addNewExercises
   * @see #addOverlays
   * @see #setDependencies
   */
  IUserExerciseDAO userExerciseDAO;
  private IAudioDAO audioDAO;

  /**
   * @param serverProps
   * @param userListManager
   * @param addDefects
   * @param language
   * @see DBExerciseDAO#DBExerciseDAO
   */
  BaseExerciseDAO(ServerProperties serverProps,
                  IUserListManager userListManager,
                  boolean addDefects,
                  String language) {
    this.serverProps = serverProps;
    this.userListManager = userListManager;
    this.language = language;
    this.addDefects = addDefects;
    // logger.debug("language is " + language + " add defects " + addDefects);
  }

  public Set<Integer> getIDs() {
    synchronized (idToExercise) {
      return idToExercise.keySet();
    }
  }

  /**
   * Only for AMAS.
   *
   * @return
   */
  public int getNumExercises() {
    return getRawExercises().size();
  }

  /**
   * @return
   * @see ExerciseServices#getExercises(int, boolean)
   */
  public List<CommonExercise> getRawExercises() {
    synchronized (idToExercise) {
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
   * @see mitll.langtest.server.database.project.ProjectManagement#configureProject
   */
  public void reload() {
    synchronized (idToExercise) {
      long then = System.currentTimeMillis();
      logger.info("START : reloading exercises ");

      exercises = null;
      idToExercise.clear();
      oldidToExercise.clear();
      sectionHelper.clear();
      getRawExercises();

      logger.info("END   : reloading exercises in " + (System.currentTimeMillis() - then) + " millis");
    }
  }

  /**
   * Do steps after reading the exercises.
   *
   * @see #getRawExercises
   */
  private void afterReadingExercises() {
    addAlternatives(exercises);

    populateIdToExercise();

    addDefects(findDefects());

    // remove exercises to remove
    // mask over old items that have been overridden
    if (userExerciseDAO != null) {
      Collection<Integer> removes = removeExercises();
      if (!removes.isEmpty())
        logger.info("remove these (" + removes.size() + ") " + removes);
      addOverlays(removes);
    }

    // add new items
    addNewExercises();
  }

  @Override
  public List<String> getTypeOrder() {
    return getSectionHelper().getTypeOrder();
  }

  /**
   * @return
   * @see DatabaseImpl#getSectionHelper(int)
   * @see #addNewExercises()
   */
  public ISection<CommonExercise> getSectionHelper() {
    return sectionHelper;
  }

  /**
   * There must be an /opt/netProf/bestAudio directory for audio.
   * <p>
   * Only validate audio when there is audio to validate.
   * <p>
   * TODO : what if they add a user exercise and add audio to it, or record new audio for other exercises???
   *
   * @param audioDAO
   * @param projectID
   * @param isMyProject
   * @see #setDependencies
   */
  void setAudioDAO(IAudioDAO audioDAO, int projectID, boolean isMyProject) {
    this.audioDAO = audioDAO;
    if (!serverProps.useProxy() && isMyProject) {
      logger.info("setAudioDAO makeSureAudioIsThere " + projectID);
      audioDAO.makeSureAudioIsThere(projectID, language, false);
    }
  }

  CommonExercise getCommonExercise(int id) {
    CommonExercise commonExercise;
    synchronized (idToExercise) { //?
      commonExercise = idToExercise.get(id);
    }
    return commonExercise;
  }

  public CommonExercise forget(int id) {
    CommonExercise commonExercise;
    synchronized (idToExercise) { //?
      commonExercise = idToExercise.remove(id);
    }
    return commonExercise;
  }

  /**
   * TODO : make it easy to look up by domino id.
   * Populate the lookup map.
   *
   * @see #afterReadingExercises
   */
  protected void populateIdToExercise() {
    // logger.info("populateIdToExercise Examining " + exercises.size() + " exercises");
    for (CommonExercise e : exercises) {
      idToExercise.put(e.getID(), e);
      // idToExercise.put(e.getDominoID(), e);
      oldidToExercise.put(e.getOldID(), e);
    }

    int listSize = exercises.size();
    int mapSize = idToExercise.size();
    if (listSize != mapSize) {
      logger.warn("populateIdToExercise exercises num = " + listSize + " but id->ex " + mapSize);
    }
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
    Collection<CommonExercise> overrides = userExerciseDAO.getOverrides(false);

    if (overrides.size() > 0) {
      logger.debug("addOverlays found " + overrides.size() + " overrides : ");
      // for (CommonUserExercise exercise : overrides) logger.info("\t" + exercise);
    }
    if (removes.size() > 0) {
      logger.debug("addOverlays found " + removes.size() + " to remove " + removes);
    }

    int override = 0;
    int skippedOverride = 0;
    int nomatch = 0;
    int removedSoSkipped = 0;
    SortedSet<Integer> staleOverrides = new TreeSet<>();
    for (CommonExercise userExercise : overrides) {
      int overrideID = userExercise.getID();
      if (removes.contains(overrideID)) {
        removedSoSkipped++;
      } else {
        if (isKnownExercise(overrideID)) {
          String oldID = userExercise.getOldID();
          logger.info("addOverlays for " +
              "\n\tid #   " + overrideID +
              "\n\told id " + oldID +
              "\n\tgot    " + userExercise.getUnitToValue() + " " + oldID + " " + userExercise.getEnglish() + " " + userExercise.getForeignLanguage());
          // don't use the unit->value map stored in the user originalExercise table...
          // CommonExercise originalExercise = getExercise(overrideID);
          CommonExercise originalExercise = getExerciseOld(oldID);

          if (originalExercise == null) {
            logger.warn("can't find original exercise for " + oldID);
            logger.warn("can't find original exercise for " + userExercise);

            nomatch++;
          } else {
            long predefUpdateTime = originalExercise.getUpdateTime();
            long userExUpdateTime = userExercise.getUpdateTime();

            if (userExUpdateTime > predefUpdateTime) {
              Map<String, String> unitToValue = originalExercise.getUnitToValue();
              ((Exercise) userExercise).setUnitToValue(unitToValue);

              logger.debug("addOverlays refresh originalExercise for" +
                  "\n\tID     " + userExercise.getID() +
                  "\n\tOLD id " + oldID +
                  "\n\t fl    '" + userExercise.getForeignLanguage() +
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
          }
        } else {
          staleOverrides.add(overrideID);
          //logger.warn("----> addOverlays not adding as overlay '" + overrideID + "' since it's not in the original list of size " + idToExercise.size());
        }
      }
    }
    if (override > 0) {
      logger.debug("addOverlays overlay childCount was " + override);
    }
    if (nomatch > 0) {
      logger.warn("addOverlays nomatch was " + nomatch);
    }
    if (skippedOverride > 0) {
      logger.debug("addOverlays skippedOverride childCount was " + skippedOverride);
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
  private CommonExercise addOverlay(CommonExercise userExercise) {
    int idOfNewExercise = userExercise.getID();
    CommonExercise currentExercise = getExerciseOld(userExercise.getOldID());

    if (currentExercise == null) {
      logger.info("addOverlay : huh? can't find " + userExercise);
    } else {
      logger.info("addOverlay at " + userExercise.getOldID() + " found " + currentExercise);
      synchronized (idToExercise) {
        int i = exercises.indexOf(currentExercise);
        logger.info("addOverlay at " + i + " when looking for " + currentExercise);
        if (i == -1) {
          logger.error("addOverlay : huh? couldn't find " + currentExercise);
        } else {
          logger.info("addOverlay step on " + i + " : " + exercises.get(i));

          exercises.set(i, userExercise);
        }
        idToExercise.put(idOfNewExercise, userExercise);
        oldidToExercise.put(userExercise.getOldID(), userExercise);
        //  logger.debug("addOverlay : after " + getExercise(userExercise.getOldID()));
      }
    }
    return currentExercise;
  }

  /**
   * @param ue
   * @seex DatabaseImpl#duplicateExercise
   * @see #addNewExercises()
   */
  public void add(CommonExercise ue) {
    synchronized (idToExercise) {
      exercises.add(ue);
      idToExercise.put(ue.getID(), ue);

      logger.info("add : put " + ue.getID() + " ue " + ue);
      oldidToExercise.put(ue.getOldID(), ue);

      if (exercises.size() != idToExercise.size()) {
        logger.warn("add " + exercises.size() + " exercises but id->ex " + idToExercise.size());
      }
    }
  }

  /**
   * This DAO needs to talk to other DAOs.
   *
   * @param userExerciseDAO
   * @param addRemoveDAO
   * @param audioDAO
   * @param projid
   * @param isMyProject
   * @see mitll.langtest.server.database.project.ProjectManagement#setDependencies
   */
  public void setDependencies(IUserExerciseDAO userExerciseDAO,
                              AddRemoveDAO addRemoveDAO,
                              IAudioDAO audioDAO,
                              int projid,

                              Database database,
                              boolean isMyProject) {
    this.userExerciseDAO = userExerciseDAO;
    this.addRemoveDAO = addRemoveDAO;
    setAudioDAO(audioDAO, projid, isMyProject);
  }

  public boolean isConfigured() {
    return audioDAO != null;
  }

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
    synchronized (idToExercise) { //?
      CommonExercise commonExercise = idToExercise.get(id);
      if (commonExercise == null) {
        if (id != userExerciseDAO.getUnknownExerciseID()) {

          if (warns++ < MAX_WARNS)
            logger.warn(this + " getExercise : couldn't find exercise " + id +
                " in " + idToExercise.size() + " exercises (" + warns + " warned)");
        }
      }
      return commonExercise;
    }
  }

  /**
   * @param id
   * @return
   * @see #addOverlay(CommonExercise)
   */
  private CommonExercise getExerciseOld(String id) {
    synchronized (idToExercise) {
      CommonExercise commonExercise = oldidToExercise.get(id);
      if (commonExercise == null) {
        if (warns++ < MAX_WARNS) {
          logger.warn(this + " couldn't find exercise '" + id + "' in " + oldidToExercise.size() + " exercises (" + warns + " warned)");
          //   logger.warn(" : " + oldidToExercise.keySet());
        }
      }
      return commonExercise;
    }
  }

  /**
   * @param exTofieldToDefect
   * @see #afterReadingExercises()
   */
  private void addDefects(Map<CommonExercise, Map<String, String>> exTofieldToDefect) {
    if (addDefects) {
      int count = 0;
      logger.info("addDefects adding defects, num = " + exTofieldToDefect.size());

      for (Map.Entry<CommonExercise, Map<String, String>> pair : exTofieldToDefect.entrySet()) {
        for (Map.Entry<String, String> fieldToDefect : pair.getValue().entrySet()) {
          if (userListManager.addDefect(pair.getKey(), fieldToDefect.getKey(), fieldToDefect.getValue())) {
            count++;
          }
        }
      }
      if (count > 0) {
        logger.info("addDefects Automatically added " + exTofieldToDefect.size() + "/" + count + " defects");
      }
    } else {
      if (DEBUG) logger.info("addDefects NOT Automatically adding " + exTofieldToDefect.size() + " defects");
    }
  }

  /**
   * Look at the exercises and automatically flag defects
   *
   * @return
   */
  private Map<CommonExercise, Map<String, String>> findDefects() {
    Map<CommonExercise, Map<String, String>> idToDefectMap = new HashMap<>();

    for (CommonExercise shell : exercises) {
      Map<String, String> fieldToDefect = new HashMap<>();
      checkForSemicolons(fieldToDefect, shell.getForeignLanguage(), shell.getTransliteration());

      if (shell.getEnglish().isEmpty()) {
        fieldToDefect.put(ENGLISH, MISSING_ENGLISH);
      }

      if (!fieldToDefect.isEmpty()) {
        idToDefectMap.put(shell, fieldToDefect);
      }
    }
    return idToDefectMap;
  }

  /**
   * Only on import...
   *
   * @see #getRawExercises
   * @see #afterReadingExercises
   */
  private void addNewExercises() {
    if (addRemoveDAO != null) {
      for (AddRemoveDAO.IdAndTime id : addRemoveDAO.getAdds()) {
        String oldid = id.getOldid();
        CommonExercise where = userExerciseDAO.getByExOldID(oldid, -1);
        if (where == null) {
          logger.error("getRawExercises huh? couldn't find user exercise from add exercise table in user exercise table : " + id);
        } else {
          int id1 = where.getID();
          if (isKnownExercise(oldid)) {
            logger.info("addNewExercises SKIPPING new user exercise " + id1 + " since already added from spreadsheet : " + where);
          } else {
            logger.info("addNewExercises adding new user exercise " + id1 + " : " + where);
            add(where);
            getSectionHelper().addExercise(where);
          }
        }
      }
    }
//    else logger.info("no add remove dao...");
  }

  private boolean isKnownExercise(int id) {
    return idToExercise.containsKey(id);
  }

  private boolean isKnownExercise(String id) {
    return oldidToExercise.containsKey(id);
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
        logger.warn("removeExercises : Removing " + removes.size() + " exercises marked as deleted.");

      Set<Integer> idsToRemove = new HashSet<>();
      for (AddRemoveDAO.IdAndTime id : removes) {
        // CommonExercise remove = idToExercise.remove(id.getId());
        CommonExercise remove = oldidToExercise.remove(id.getOldid());

        if (remove != null && remove.getUpdateTime() < id.getTimestamp()) {
          logger.warn("removeExercises remove " + remove.getID() + " " + remove.getOldID() + " " + remove.getEnglish() + " " + remove.getForeignLanguage());

          boolean remove1 = exercises.remove(remove);
          if (!remove1) logger.error("huh? remove inconsistency??");
          getSectionHelper().removeExercise(remove);
          idsToRemove.add(remove.getID());
        } else if (remove != null) {
          logger.warn("removeExercises 2 remove " + remove.getID() + " " + remove.getOldID() + " " + remove.getEnglish() + " " + remove.getForeignLanguage());
        } else {
          logger.error("can't find remove exercise by " + id.getOldid());
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
    if (foreignLanguagePhrase.contains(SEMI)) {
      fieldToDefect.put(QCNPFExercise.FOREIGN_LANGUAGE, CONTAINS_SEMI);
    }
    if (translit.contains(SEMI)) {
      fieldToDefect.put(QCNPFExercise.TRANSLITERATION, CONTAINS_SEMI);
    }
  }

  public void updatePhones(int id, int count) {
  }

  public void bulkImport() {
  }

  public boolean update(CommonExercise toChange) {
    return false;
  }
}
