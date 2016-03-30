package mitll.langtest.server.database.exercise;

import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.AudioDAO;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.custom.AddRemoveDAO;
import mitll.langtest.server.database.custom.UserExerciseDAO;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.shared.exercise.AudioExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * Created by go22670 on 2/10/16.
 */
public abstract class BaseExerciseDAO implements SimpleExerciseDAO<CommonExercise> {
	private static final Logger logger = Logger.getLogger(BaseExerciseDAO.class);
	private static final String CONTAINS_SEMI = "contains semicolon - should this item be split?";
	private static final String ENGLISH = "english";
	private static final String MISSING_ENGLISH = "missing english";

	private final Map<String, CommonExercise> idToExercise = new HashMap<>();
	protected final SectionHelper<CommonExercise> sectionHelper = new SectionHelper<>();
	protected final String language;
	protected final ServerProperties serverProps;
	private final UserListManager userListManager;
	private final boolean addDefects;

	private List<CommonExercise> exercises = null;
	private AddRemoveDAO addRemoveDAO;
	private UserExerciseDAO userExerciseDAO;
	private AttachAudio attachAudio;
	private AudioDAO audioDAO;

	public BaseExerciseDAO(ServerProperties serverProps, UserListManager userListManager, boolean addDefects) {
		this.serverProps = serverProps;
		this.userListManager = userListManager;
		this.language = serverProps.getLanguage();
		this.addDefects = addDefects;
	}

	public int getNumExercises() {
		return getRawExercises().size();
	}

	/**
	 * @return
	 * @see DatabaseImpl#getExercises()
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

	public void reload() {
    exercises = null;
    idToExercise.clear();
    sectionHelper.clear();
    getRawExercises();
  }

	private void afterReadingExercises() {
		addAlternatives(exercises);

		populateIdToExercise();

		addDefects(findDefects());

		// remove exercises to remove
		// mask over old items that have been overridden
		addOverlays(removeExercises());

		// add new items
		addNewExercises();

		Set<String> transcriptChanged = new HashSet<>();

   // logger.info("afterReadingExercises trying to attach audio to " + exercises.size());
		for (CommonExercise ex : exercises) {
			attachAudio.attachAudio(ex, transcriptChanged);
			String refAudioIndex = ex.getRefAudioIndex();
			if (refAudioIndex != null && !refAudioIndex.isEmpty()) {
				attachAudio.addOldSchoolAudio(refAudioIndex, (AudioExercise) ex);
			}
		}

		if (!transcriptChanged.isEmpty()) {
			logger.info("afterReadingExercises : found " + transcriptChanged.size() + " changed transcripts in set of " + exercises.size() + " items");
		}
	}

	/**
	 * @return
	 * @see DatabaseImpl#getSectionHelper()
	 */
	public SectionHelper<CommonExercise> getSectionHelper() {
		return sectionHelper;
	}

	/**
	 * TODO : what if they add a user exercise and add audio to it, or record new audio for other exercises???
	 *
	 * @param audioDAO
	 * @param mediaDir
	 * @param installPath
	 * @see DatabaseImpl#makeDAO
	 */
	private void setAudioDAO(AudioDAO audioDAO, String mediaDir, String installPath) {
		this.audioDAO = audioDAO;

		File fileInstallPath = new File(installPath);
		if (!fileInstallPath.exists()) {
			logger.warn("\n\n\nhuh? install path " + fileInstallPath.getAbsolutePath() + " doesn't exist???");
		}

		this.attachAudio = new AttachAudio(
				mediaDir,
				mediaDir.replaceAll("bestAudio", ""), fileInstallPath,
				serverProps.getAudioOffset(), audioDAO.getExToAudio());
	}

	/**
	 * @see #afterReadingExercises
	 */
	private void populateIdToExercise() {
//    logger.info("populateIdToExercise Examining " + exercises.size() + " exercises");
		for (CommonExercise e : exercises) {
			idToExercise.put(e.getID(), e);
			idToExercise.put(e.getDisplayID(), e);
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
	 * @see DatabaseImpl#getExerciseIDToRefAudio()
	 */
	public void attachAudio(Collection<CommonExercise> all) {
		attachAudio.setExToAudio(audioDAO.getExToAudio());
		int user = 0;
		int examined = 0;

		Collection<String> transcriptChanged = new HashSet<>();

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
			logger.info("attachAudio : found " + transcriptChanged.size() + " changed transcripts in set of " + exercises.size() + " items");
		}
	}

	/**
	 * TODO : better to use a filter
	 * Don't add overlays for exercises that have been removed.
	 *
	 * @param removes
	 */
	private void addOverlays(Collection<String> removes) {
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
		SortedSet<String> staleOverrides = new TreeSet<String>();
		for (CommonExercise userExercise : overrides) {
			String overrideID = userExercise.getID();
			if (removes.contains(overrideID)) {
				removedSoSkipped++;
			} else {
				if (isKnownExercise(overrideID)) {
					//  logger.info("for " + overrideID + " got " + userExercise.getUnitToValue());
					// don't use the unit->value map stored in the user exercise table...
					CommonExercise exercise = getExercise(overrideID);

					long predefUpdateTime = exercise.getUpdateTime();
					long userExUpdateTime = userExercise.getUpdateTime();

					if (userExUpdateTime > predefUpdateTime) {
						Map<String, String> unitToValue = exercise.getUnitToValue();
						userExercise.getCombinedMutableUserExercise().setUnitToValue(unitToValue);

						//logger.debug("addOverlays refresh exercise for " + userExercise.getID() + " " + userExercise.getUnitToValue());
						sectionHelper.refreshExercise(userExercise);
						addOverlay(userExercise);

//          Collection<CommonExercise> exercisesForSimpleSelectionState = sectionHelper.getExercisesForSimpleSelectionState(unitToValue);
//          for (CommonExercise exercise:exercisesForSimpleSelectionState) if (exercise.getID().equals(overrideID)) logger.warn("found " + exercise);
						override++;
					} else {
						skippedOverride++;
						if (skippedOverride < 5)
							logger.info("for " + overrideID + " skipping override, since predef exercise is newer " + new Date(predefUpdateTime) + " > " + new Date(userExUpdateTime));
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
	 * @param userExercise
	 * @return old exercises
	 * @see DatabaseImpl#editItem
	 */
	public CommonExercise addOverlay(CommonExercise userExercise) {
		String idOfNewExercise = userExercise.getID();
		CommonExercise currentExercise = getExercise(idOfNewExercise);

		if (currentExercise == null) {
			logger.error("addOverlay : huh? can't find " + userExercise);
		} else {
			//logger.debug("addOverlay at " +userExercise.getID() + " found " +currentExercise);
			synchronized (this) {
				int i = exercises.indexOf(currentExercise);
				if (i == -1) {
					logger.error("addOverlay : huh? couldn't find " + currentExercise);
				} else {
					exercises.set(i, userExercise);
				}
				idToExercise.put(idOfNewExercise, userExercise);
				//  logger.debug("addOverlay : after " + getExercise(userExercise.getID()));
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
	 * @return true if exercise with this id was removed
	 * @see DatabaseImpl#deleteItem(String)
	 */
	public boolean remove(String id) {
		synchronized (this) {
			CommonExercise remove = idToExercise.remove(id);
			if (remove == null) return false;
			return exercises.remove(remove);
		}
	}

	public void setDependencies(String mediaDir, String installPath,
															UserExerciseDAO userExerciseDAO, AddRemoveDAO addRemoveDAO, AudioDAO audioDAO) {
		this.userExerciseDAO = userExerciseDAO;
		this.addRemoveDAO = addRemoveDAO;
		setAudioDAO(audioDAO, mediaDir, installPath);
	}

	/**
	 * @param id
	 * @return
	 * @see UserExerciseDAO#getPredefExercise(String)
	 * @see DatabaseImpl#getExercise(String)
	 */
	public CommonExercise getExercise(String id) {
//    if (idToExercise.isEmpty()) {
//      logger.error("huh? couldn't find any exercises..? " + id);
//    }

		synchronized (this) {
			CommonExercise exercise = idToExercise.get(id);
			//if (exercise == null) logger.warn("no '" +id+"'  in " + idToExercise.keySet().size()+" keys");
		//	logger.debug("returning " +exercise + " for " +id);
			return exercise;
		}
	}

	private void addDefects(Map<String, Map<String, String>> exTofieldToDefect) {
		if (addDefects) {
			int count = 0;
			for (Map.Entry<String, Map<String, String>> pair : exTofieldToDefect.entrySet()) {
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
			logger.info("NOT Automatically adding " + exTofieldToDefect.size() + " defects");
		}
	}

	private Map<String, Map<String, String>> findDefects() {
		Map<String, Map<String, String>> idToDefectMap = new HashMap<>();

		for (CommonShell shell : exercises) {
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
	 * @see #getRawExercises()
	 */
	private void addNewExercises() {
		for (String id : addRemoveDAO.getAdds()) {
			CommonExercise where = userExerciseDAO.getWhere(id);
			if (where == null) {
				logger.error("getRawExercises huh? couldn't find user exercise from add exercise table in user exercise table : " + id);
			} else {
				if (isKnownExercise(id)) {
					logger.debug("addNewExercises SKIPPING new user exercise " + where.getID() + " since already added from spreadsheet : " + where);
				} else {
					logger.debug("addNewExercises adding new user exercise " + where.getID() + " : " + where);
					add(where);
					getSectionHelper().addExercise(where);
				}
			}
		}
	}

	private boolean isKnownExercise(String id) {
		return idToExercise.containsKey(id);
	}

	private Collection<String> removeExercises() {
		if (addRemoveDAO != null) {
			Collection<String> removes = addRemoveDAO.getRemoves();

			if (!removes.isEmpty())
				logger.debug("removeExercises : Removing " + removes.size() + " exercises marked as deleted.");

			for (String id : removes) {
				CommonExercise remove = idToExercise.remove(id);
				if (remove != null) {
					boolean remove1 = exercises.remove(remove);
					if (!remove1) logger.error("huh? remove inconsistency??");
					getSectionHelper().removeExercise(remove);
				}
			}
			return removes;
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
				//   if (orDefault.size() > 1) logger.info("For " + e.getID() + " found " + orDefault.size());
			}
		}
	}

	abstract List<CommonExercise> readExercises();

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
}
