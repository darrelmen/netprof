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

package mitll.langtest.server.services;

import com.google.gson.JsonObject;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.services.ScoringService;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.audio.image.ImageType;
import mitll.langtest.server.audio.image.TranscriptEvent;
import mitll.langtest.server.database.audio.EnsureAudioHelper;
import mitll.langtest.server.database.audio.IEnsureAudioHelper;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.result.ISlimResult;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.server.scoring.PrecalcScores;
import mitll.langtest.server.scoring.TranscriptSegmentGenerator;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ModelType;
import mitll.langtest.shared.result.MonitorResult;
import mitll.langtest.shared.scoring.*;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class ScoringServiceImpl extends MyRemoteServiceServlet implements ScoringService {
  private static final Logger logger = LogManager.getLogger(ScoringServiceImpl.class);

  private static final String UPDATING_PROJECT_INFO = "updating project info";

  private static final boolean USE_PHONE_TO_DISPLAY = true;
  private static final int SLOW_ROUND_TRIP = 3000;
  private static final String RECALC_ALIGNMENTS = "recalc alignments";

  private IEnsureAudioHelper ensureAudioHelper;
  private TranscriptSegmentGenerator transcriptSegmentGenerator;
  // private static final boolean DEBUG = true;

  /**
   * Sanity checks on answers and bestAudio dir
   */
  @Override
  public void init() {
    super.init();
    ensureAudioHelper = new EnsureAudioHelper(db, pathHelper);
    this.transcriptSegmentGenerator = new TranscriptSegmentGenerator(db.getServerProps());
  }

  /**
   * NOTE NOTE NOTE : doesn't make sure we have mp3 or ogg file equivalents...
   * maybe it should?
   *
   * @param resultID
   * @return
   * @see mitll.langtest.client.scoring.ReviewScoringPanel#scoreAudio
   */
  @Override
  public PretestScore getResultASRInfo(int resultID, ImageOptions imageOptions) throws DominoSessionException, RestrictedOperationException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();

    if (hasAdminPerm(userIDFromSessionOrDB)) {
      PretestScore asrScoreForAudio = null;
      try {
        Result result = db.getResultDAO().getResultByID(resultID);
        int exerciseID = result.getExerciseID();

        boolean isAMAS = serverProps.isAMAS();
        CommonShell exercise = null;
        String sentence = "";
        String transliteration = "";
        int projectIDFromUser = getProjectIDFromUser();
        if (isAMAS) {
          exercise = db.getAMASExercise(exerciseID);
          sentence = exercise.getForeignLanguage();
        } else {
          CommonExercise exercise1 = db.getExercise(projectIDFromUser, exerciseID);

          if (exercise1 != null) {
            transliteration = exercise1.getTransliteration();
            exercise = exercise1;
            Collection<ClientExercise> directlyRelated = exercise1.getDirectlyRelated();
            sentence =
                result.getAudioType().isContext() && !directlyRelated.isEmpty() ?
                    directlyRelated.iterator().next().getForeignLanguage() :
                    exercise.getForeignLanguage();
          }
        }

        // maintain backward compatibility - so we can show old recordings of ref audio for the context sentence
//      String sentence = isAMAS ? exercise.getForeignLanguage() :
//          (result.getAudioType().isContext()) ?
//              db.getExercise(exerciseID).getDirectlyRelated().iterator().next().getForeignLanguage() :
//              exercise.getForeignLanguage();

        if (exercise == null) {
          logger.warn(getLanguage() + " can't find exercise id " + exerciseID);
          return new PretestScore();
        } else {
          String audioFilePath = result.getAnswer();
          Project project = db.getProject(projectIDFromUser);
          //  String language = project.getLanguage();

          // NOTE : actively avoid doing this -
          //ensureMP3(audioFilePath, sentence, "" + result.getUserid());
          //logger.info("resultID " +resultID+ " temp dir " + tempDir.getAbsolutePath());
          asrScoreForAudio = getAudioFileHelper().getASRScoreForAudio(
              1,
              audioFilePath,
              sentence,
              transliteration,
              imageOptions,
              "" + exerciseID,
              getPrecalcScores(serverProps.usePhoneToDisplay(project.getLanguageEnum()), result, project.getLanguageEnum()),

              new DecoderOptions()
                  .setDoDecode(false)
                  .setCanUseCache(serverProps.useScoreCache())
                  .setUsePhoneToDisplay(serverProps.usePhoneToDisplay(project.getLanguageEnum())), isKaldi(project));

        }
      } catch (Exception e) {
        logger.error("Got " + e, e);
      }

      return asrScoreForAudio;
    } else {
      throw getRestricted("result asr");
    }
  }

  private boolean isKaldi(Project project) {
    return project.getModelType() == ModelType.KALDI;
  }

/*  @Override
  public void recalcAllAlignments() throws DominoSessionException, RestrictedOperationException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    if (hasAdminPerm(userIDFromSessionOrDB)) {
      db.getProjects().stream()
          .filter(project -> project.getStatus() == ProjectStatus.PRODUCTION)
          .forEach(project -> recalcAlignments(userIDFromSessionOrDB, project));
    } else {
      throw getRestricted("recalc alignments");
    }
  }*/

  /**
   * @param projid
   * @throws DominoSessionException
   * @throws RestrictedOperationException
   * @see mitll.langtest.client.project.ProjectChoices#recalcProject
   */
  @Override
  public void recalcAlignments(int projid) throws DominoSessionException, RestrictedOperationException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    if (hasAdminPerm(userIDFromSessionOrDB)) {
      recalcAlignments(userIDFromSessionOrDB, db.getProject(projid));
    } else {
      throw getRestricted(RECALC_ALIGNMENTS);
    }
  }

  /**
   * Only if we have a valid webservice port.
   *
   * @param userIDFromSession
   * @param project
   * @see #recalcAlignments(int)
   */
  private void recalcAlignments(int userIDFromSession, Project project) {
    if (project.getWebservicePort() > 100) {
      try {
        String name = project.getProject().name();
        int projectID = project.getID();
        List<Integer> audioIDs = getAllAudioIDs(projectID);

        long then = System.currentTimeMillis();
        AudioFileHelper audioFileHelper = project.getAudioFileHelper();
        logger.info("recalcAlignments Doing project " + name + " : " + audioIDs.size() + " audio cuts with " + audioFileHelper);

        //Map<Integer, ISlimResult> audioToResult = getAudioIDMap(projectID);
        //    logger.info("getAllAlignments recalc " +audioToResult.size() + " alignments...");
        recalcAlignments(projectID, audioIDs, audioFileHelper, userIDFromSession, getAudioIDMap(projectID), db.getProject(projectID).hasModel());

        long now = System.currentTimeMillis();

        long l = (now - then) / 1000;
        long min = l / 60;
        logger.info("recalcAlignments Doing project " + name + " " + audioIDs.size() + " audio cuts took " + min + " minutes.");
      } catch (Exception e) {
        logger.error("recalcAlignments got " + e, e);
      }
    } else {
      logger.info("recalcAlignments no hydra service for " + project);
    }
  }

  /**
   * @param projectID
   * @return
   * @see #recalcAlignments(int, Project)
   */
  @NotNull
  private List<Integer> getAllAudioIDs(int projectID) {
    boolean hasProjectSpecificAudio = db.getProject(projectID).hasProjectSpecificAudio();
    Map<Integer, List<AudioAttribute>> exToAudio = db.getAudioDAO().getExToAudio(projectID, hasProjectSpecificAudio);
    List<Integer> audioIDs = new ArrayList<>(exToAudio.size());
    exToAudio.values().forEach(audioAttributes -> audioIDs.addAll(audioAttributes
        .stream()
        .map(AudioAttribute::getUniqueID)
        .collect(Collectors.toList())));

    return audioIDs;
  }

  @NotNull
  private Map<Integer, ISlimResult> getAudioIDMap(int id) {
    return getAudioIDMap(db.getRefResultDAO().getAllSlimForProject(id));
  }

  @NotNull
  private Map<Integer, ISlimResult> getAudioIDMap(Collection<ISlimResult> jsonResultsForProject) {
    Map<Integer, ISlimResult> audioToResult = new HashMap<>(jsonResultsForProject.size());
    jsonResultsForProject.forEach(iSlimResult -> audioToResult.put(iSlimResult.getAudioID(), iSlimResult));
    return audioToResult;
  }

  /**
   * @param projid
   * @param audioIDs
   * @return
   * @see mitll.langtest.client.scoring.AlignmentFetcher#getAlignments
   * @see mitll.langtest.client.scoring.AlignmentFetcher#cacheOthers
   */
  @Override
  public Map<Integer, AlignmentAndScore> getAlignments(int projid, Set<Integer> audioIDs) throws DominoSessionException {
//    logger.info("getAlignments project " + projid + " asking for " + audioIDs);
    Map<Integer, ISlimResult> audioIDMap = getAudioIDMap(db.getRefResultDAO().getAllSlimForProjectIn(projid, audioIDs));

    {
      Project project = getProject(projid);

      if (project != null && project.hasProjectSpecificAudio()) {
        List<CommonExercise> exercisesForAudio =
            audioIDMap
                .values()
                .stream()
                .map(iSlimResult -> db.getExercise(projid, iSlimResult.getExID()))
                .collect(Collectors.toList());

        logger.info("ensure compressed audio for " + exercisesForAudio.size() + " exercises");
        new EnsureAudioHelper(db, pathHelper).ensureCompressedAudio(exercisesForAudio, getLanguageEnum(project));
      }
    }

  //  logger.info("getAlignments project " + projid + " asking for " + audioIDs + " audio ids, found " + audioIDMap.size() + " remembered alignments...");
    Map<Integer, AlignmentAndScore> audioIDToAlignment = recalcAlignments(projid, audioIDs, getUserIDFromSessionOrDB(), audioIDMap, db.getProject(projid).hasModel());
    return audioIDToAlignment;
    //  logger.info("getAligments for " + projid + " and " + audioIDs + " found " + idToAlignment.size());
  }

  public AlignmentAndScore getStudentAlignment(int projid, int resultID) {
    CorrectAndScore correctAndScoreForResult = db.getResultDAO().getCorrectAndScoreForResult(resultID, getProject(projid).getLanguageEnum());
    return correctAndScoreForResult == null ? null :
        new AlignmentAndScore(correctAndScoreForResult.getScores(), correctAndScoreForResult.getScore(), true);
  }

  private Map<Integer, AlignmentAndScore> recalcAlignments(int projid,
                                                           Collection<Integer> audioIDs,
                                                           int userIDFromSession,
                                                           Map<Integer, ISlimResult> audioToResult,
                                                           boolean hasModel) {
    return recalcAlignments(projid, audioIDs, getAudioFileHelper(projid), userIDFromSession, audioToResult, hasModel);
  }

  /**
   * @param projid
   * @param audioIDs
   * @param audioFileHelper
   * @param userIDFromSession
   * @param audioToResult
   * @param hasModel
   * @return
   */
  private Map<Integer, AlignmentAndScore> recalcAlignments(int projid,
                                                           Collection<Integer> audioIDs,
                                                           AudioFileHelper audioFileHelper,
                                                           int userIDFromSession,
                                                           Map<Integer, ISlimResult> audioToResult,
                                                           boolean hasModel) {
    Map<Integer, AlignmentAndScore> idToAlignment = new HashMap<>();

    if (hasModel) {
//      logger.info("recalcAlignments recalc " + audioIDs.size() + " audio ids for project #" + projid);
      if (audioIDs.isEmpty()) logger.error("recalcAlignments huh? no audio for " + projid);

      Set<Integer> completed = new HashSet<>(audioToResult.size());
      audioIDs.forEach(audioID ->
          recalcOneOrGetCached(projid, audioID, audioFileHelper, userIDFromSession, audioToResult.get(audioID), idToAlignment, completed, audioIDs.size()));

    } else {
      logger.info("recalcAlignments : no hydra for project " + projid + " so not recalculating alignments.");
    }

    return idToAlignment;
  }

  /**
   * @param projid
   * @param audioID
   * @param audioFileHelper
   * @param userIDFromSession
   * @param cachedResult
   * @param idToAlignment
   */
  private void recalcOneOrGetCached(int projid,
                                    Integer audioID,
                                    AudioFileHelper audioFileHelper,
                                    int userIDFromSession,
                                    ISlimResult cachedResult,
                                    Map<Integer, AlignmentAndScore> idToAlignment,
                                    Set<Integer> completed,
                                    int total) {
    if (cachedResult == null) { // nope, ask the database
      cachedResult = db.getRefResultDAO().getResult(audioID);
    }

    if (cachedResult == null || !cachedResult.isValid()) { // not in the database, recalculate it now
      if (cachedResult != null && !cachedResult.isValid()) {
        boolean b = db.getRefResultDAO().removeByAudioID(audioID);
        if (!b) {
          logger.warn("recalcOneOrGetCached remove invalid ref result for audio id " + audioID + " = " + b);
        }
      }

      PretestScore pretestScore = recalcRefAudioWithHelper(projid, audioID, audioFileHelper, userIDFromSession);
      if (pretestScore != null) {
        idToAlignment.put(audioID, pretestScore);
      }
    } else {
      logger.info("recalcOneOrGetCached : found cached result for projid " + projid + " audio id " + audioID);
      getCachedAudioRef(idToAlignment, audioID, cachedResult, db.getProject(projid).getLanguageEnum());  // OK, let's translate the db info into the alignment output
    }
    completed.add(audioID);

    if (completed.size() % 500 == 0) {
      logger.info("recalcOneOrGetCached : project " + projid + " completed " + completed.size() + "/" + total + "(" +
          Math.round(100f * (float) completed.size() / (float) total) +
          "% )");
    }
  }

  /**
   * TODO : mark full match true for now - also see other call!
   *
   * @param idToAlignment
   * @param audioID
   * @param cachedResult
   * @param language
   */
  private void getCachedAudioRef(Map<Integer, AlignmentAndScore> idToAlignment,
                                 Integer audioID, ISlimResult cachedResult, Language language) {
    PrecalcScores precalcScores = getPrecalcScores(USE_PHONE_TO_DISPLAY, cachedResult, language);
    Map<ImageType, Map<Float, TranscriptEvent>> typeToTranscriptEvents =
        getTypeToTranscriptEvents(precalcScores.getJsonObject(), USE_PHONE_TO_DISPLAY, language);
    Map<NetPronImageType, List<TranscriptSegment>> typeToSegments = transcriptSegmentGenerator.getTypeToSegments(typeToTranscriptEvents, language);
//    logger.info("getCachedAudioRef : cache HIT for " + audioID + " returning " + typeToSegments);
    idToAlignment.put(audioID, new AlignmentAndScore(typeToSegments, cachedResult.getPronScore(), true));
  }


  private PretestScore recalcRefAudioWithHelper(int projid,
                                                Integer audioID,
                                                AudioFileHelper audioFileHelper,
                                                int userIDFromSession) {
    AudioAttribute byID = db.getAudioDAO().getByID(audioID, db.getProject(projid).hasProjectSpecificAudio());
    if (byID != null) {
      CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(projid, byID.getExid());

      //boolean contextAudio = byID.isContextAudio();

/*
      if (customOrPredefExercise != null) {
        logger.info("getAlignmentsFromDB decoding " + audioID +
            (contextAudio ? " RECORD_CONTEXT" : "") +
            " for exercise " + byID.getExid() + " : '" +
            customOrPredefExercise.getEnglish() + "' = '" + customOrPredefExercise.getForeignLanguage() + "'");
      }
      */

      // cover for import bug...
      if (byID.isContextAudio() &&
          customOrPredefExercise != null &&
          customOrPredefExercise.getDirectlyRelated() != null &&
          !customOrPredefExercise.getDirectlyRelated().isEmpty()) {
        customOrPredefExercise = customOrPredefExercise.getDirectlyRelated().iterator().next().asCommon();

        //logger.info("getAlignmentsFromDB using " + customOrPredefExercise.getID() + " " + customOrPredefExercise.getEnglish() + " instead ");
      }

      logger.info("recalcRefAudioWithHelper decoding audio #" + audioID + " '" + byID.getTranscript() + "' for exercise #" + byID.getExid() + "...");
      return audioFileHelper.decodeAndRemember(customOrPredefExercise, byID, false, userIDFromSession, null);
    } else {
      logger.info("recalcRefAudioWithHelper can't find audio id " + audioID);
      return null;
    }
  }

  private Map<ImageType, Map<Float, TranscriptEvent>> getTypeToTranscriptEvents(JsonObject object,
                                                                                boolean usePhoneToDisplay,
                                                                                Language language) {
    return
        new ParseResultJson(db.getServerProps(), language)
            .readFromJSON(object, "words", "w", usePhoneToDisplay, null, false);
  }

  /**
   * And check if there is no hydra dcodr available locally, and if so, try to use one on dev.
   * <p>
   * So first we check and see if we've already done alignment for this audio (if reference audio), and if so, we grab the Result
   * object out of the result table and use it and it's json to generate the score info and transcript inmages.
   *
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param exerciseID
   * @param usePhonemeMap
   * @return
   * @see ASRScoringAudioPanel#scoreAudio
   */
  public PretestScore getASRScoreForAudio(int reqid,
                                          long resultID,
                                          String testAudioFile,
                                          String sentence,
                                          String transliteration,

                                          ImageOptions imageOptions,
                                          int exerciseID,
                                          boolean usePhonemeMap) throws DominoSessionException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    File absoluteAudioFile = pathHelper.getAbsoluteAudioFile(testAudioFile.replaceAll(".ogg", ".wav").replaceAll(".mp3",".wav"));

    int projectID = getProjectIDFromUser(userIDFromSessionOrDB);
    CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(projectID, exerciseID);

    String english = customOrPredefExercise == null ? "" : customOrPredefExercise.getEnglish();
    AudioFileHelper audioFileHelper = getAudioFileHelper(projectID);
    PrecalcScores precalcScores =
        audioFileHelper
            .checkForWebservice(exerciseID, english, sentence, projectID, userIDFromSessionOrDB, absoluteAudioFile);

    Language languageEnum = getProject(projectID).getLanguageEnum();
    String absPath = absoluteAudioFile.getAbsolutePath();
    return getPretestScore(reqid,
        (int) resultID,
        testAudioFile,
        sentence, transliteration, imageOptions,
        exerciseID, usePhonemeMap, precalcScores, audioFileHelper, projectID, userIDFromSessionOrDB, absPath, languageEnum);
  }

  /**
   * Be careful - we lookup audio file by .wav extension
   *
   * @param reqid
   * @param resultID
   * @param testAudioFile
   * @param sentence
   * @param exerciseID
   * @param usePhoneToDisplay
   * @param precalcScores
   * @param absPath
   * @param language
   * @return
   */
  private PretestScore getPretestScore(int reqid,
                                       int resultID,
                                       String testAudioFile,
                                       String sentence,
                                       String transliteration,
                                       ImageOptions imageOptions,
                                       int exerciseID,
                                       boolean usePhoneToDisplay,
                                       PrecalcScores precalcScores,
                                       AudioFileHelper audioFileHelper,
                                       int projID,
                                       int userIDFromSessionOrDB,

                                       String absPath, Language language) {
    if (testAudioFile.equals(AudioConversion.FILE_MISSING))
      return new PretestScore(-1).setStatus("can't find audio file");

    long then = System.currentTimeMillis();

    //String[] split = testAudioFile.split(File.separator);
    //String answer = split[split.length - 1];
//    String wavEndingAudio = answer.replaceAll(".mp3", ".wav").replaceAll(".ogg", ".wav");
    //  ISlimResult cachedResult = null;//db.getRefResultDAO().getResult(audioID);//exerciseID, wavEndingAudio);

    boolean usePhoneToDisplay1 = usePhoneToDisplay || serverProps.usePhoneToDisplay(language);
/*    if (cachedResult != null && precalcScores == null) {
      precalcScores = getPrecalcScores(usePhoneToDisplay, cachedResult, language);
      if (DEBUG)
        logger.debug("getPretestScore Cache HIT  : align exercise id = " + exerciseID + " file " + answer);
      //  +
      //  " found previous " + cachedResult.getUniqueID());
    } else {
      logger.debug("getPretestScore Cache MISS : align exercise id = " + exerciseID + " file " + answer);
    }*/

    PretestScore asrScoreForAudio = getPretestScoreMaybeUseCache(reqid,
        testAudioFile,
        sentence,
        transliteration,
        imageOptions,
        exerciseID,
        precalcScores,
        usePhoneToDisplay1,
        audioFileHelper);

    long timeToRunHydec = System.currentTimeMillis() - then;

    logger.debug("getPretestScore : scoring" +
        "\n\tfile     " + testAudioFile +
        "\n\texid     " + exerciseID +
        "\n\tsentence " + sentence.length() + " characters long" +
        "\n\tscore    " + asrScoreForAudio.getHydecScore() +
        "\n\ttook     " + timeToRunHydec + " millis " +
        "\n\tusePhoneToDisplay " + usePhoneToDisplay1);

    if (resultID > -1 /*&& cachedResult == null*/) { // alignment has two steps : 1) post the audio, then 2) do alignment
      db.rememberScore(projID, resultID, asrScoreForAudio, true);
      Project project = db.getProjectManagement().getProject(projID, false);
      project.addAnswerToUser(testAudioFile, userIDFromSessionOrDB);
      project.addAnswerToUser(absPath, userIDFromSessionOrDB);
    }
    return asrScoreForAudio;
  }

  /**
   * @param reqid
   * @param testAudioFile
   * @param sentence
   * @param transliteration
   * @param imageOptions
   * @param exerciseID
   * @param precalcScores
   * @param usePhoneToDisplay1
   * @return
   * @see #getPretestScore
   */
  private PretestScore getPretestScoreMaybeUseCache(int reqid,
                                                    String testAudioFile,
                                                    String sentence, String transliteration,
                                                    ImageOptions imageOptions,
                                                    int exerciseID,
                                                    PrecalcScores precalcScores,
                                                    boolean usePhoneToDisplay1,
                                                    AudioFileHelper audioFileHelper) {
    return audioFileHelper.getASRScoreForAudio(
        reqid,
        testAudioFile,
        sentence,
        transliteration,
        imageOptions,
        "" + exerciseID,
        precalcScores,
        new DecoderOptions()
            .setDoDecode(false)
            .setCanUseCache(serverProps.useScoreCache())
            .setUsePhoneToDisplay(usePhoneToDisplay1),
        audioFileHelper.isKaldi());
  }

  @NotNull
  private PrecalcScores getPrecalcScores(boolean usePhoneToDisplay, ISlimResult cachedResult, Language language) {
    return new PrecalcScores(serverProps, cachedResult, shouldUsePhoneDisplay(usePhoneToDisplay, language), language);
  }

  private boolean shouldUsePhoneDisplay(boolean usePhoneToDisplay, Language language) {
    return usePhoneToDisplay || serverProps.usePhoneToDisplay(language);
  }

  /**
   * Doesn't really need to be on the scoring service...
   *
   * Send email if it's slow.
   *
   * @param resultID
   * @param roundTrip
   */
  @Override
  public void addRoundTrip(int resultID, int roundTrip) {
    db.getAnswerDAO().addRoundTrip(resultID, roundTrip);
    warnWhenSlow(resultID, roundTrip);
  }

  private void warnWhenSlow(int resultID, int roundTrip) {
    if (roundTrip > SLOW_ROUND_TRIP) {
      try {
        int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
        int projectIDFromUser = getProjectIDFromUser(userIDFromSessionOrDB);
        Project project = getProject(projectIDFromUser);
        if (project.getModelType() == ModelType.KALDI) {
          if (roundTrip > 5000) {
            sendSlowEmail(resultID, roundTrip, userIDFromSessionOrDB);
          }
        } else {
          sendSlowEmail(resultID, roundTrip, userIDFromSessionOrDB);
        }
      } catch (Exception e) {
        logger.warn("addRoundTrip got " + e, e);
      }
    }
  }

  private void sendSlowEmail(int resultID, int roundTrip, int userIDFromSessionOrDB) {
    String userChosenID = db.getUserDAO().getUserChosenID(userIDFromSessionOrDB);
    new Thread(() -> sendEmail("Slow round trip : " + roundTrip,
        getInfo("Slow round trip (" + roundTrip + ") recording #" + resultID +
            " by user #" + userIDFromSessionOrDB + "/" + userChosenID))).start();
  }

  /**
   * A low overhead way of doing alignment.
   * <p>
   * Useful for conversational dialogs - Jennifer Melot's project.
   *
   * @param base64EncodedString
   * @param textToAlign
   * @param identifier
   * @param reqid
   * @param device
   * @return
   * @see mitll.langtest.client.scoring.SimplePostAudioRecordButton#postAudioFile
   */
/*  @Override
  public AudioAnswer getAlignment(String base64EncodedString,
                                  String textToAlign,
                                  String transliteration,
                                  String identifier,
                                  int reqid,
                                  String device) throws DominoSessionException {
    AudioAnswer audioAnswer = getAudioFileHelper()
        .getAlignment(base64EncodedString, textToAlign, transliteration, identifier, reqid,
            serverProps.usePhoneToDisplay());

    if (!audioAnswer.isValid() && audioAnswer.getDurationInMillis() == 0) {
      logger.warn("getAlignment : huh? got zero length recording for " + identifier + " from " + device);
      logEvent(identifier, device, -1);
    }
    return audioAnswer;
  }*/

  /**
   * @param projid
   * @return
   * @see mitll.langtest.client.project.ProjectEditForm#getUserForm
   */
  @Override
  public boolean isHydraRunning(int projid) throws DominoSessionException, RestrictedOperationException {
    if (hasAdminPerm(getUserIDFromSessionOrDB())) {
      Project project = db.getProject(projid);
      if (project == null) {
        logger.debug("isHydraRunning no project with id " + projid);
        return false;
      } else {
        try {
          //  logger.debug("isHydraRunning  project with id " + projid);
          AudioFileHelper audioFileHelper = project.getAudioFileHelper();
          //logger.debug("isHydraRunning  audioFileHelper " + audioFileHelper);
          boolean hydraAvailable = audioFileHelper.isHydraAvailable();
          // logger.debug("isHydraRunning  hydraAvailable " + hydraAvailable);
          boolean hydraAvailableCheckNow = audioFileHelper.isHydraAvailableCheckNow();
          // logger.debug("isHydraRunning  isHydraAvailableCheckNow " + hydraAvailableCheckNow);

          if (!hydraAvailable && hydraAvailableCheckNow) audioFileHelper.setAvailable();
          return hydraAvailableCheckNow;
        } catch (Exception e) {
          logger.error("got " + e, e);
          return false;
        }
      }
    } else {
      throw getRestricted("checking hydra status");
    }
  }

/*  private void logEvent(String exid, String device, int projID) {
    try {
      db.logEvent(AUDIO_RECORDING, WRITE_AUDIO_FILE, exid, "Writing audio - got zero duration!", -1, device, projID);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }*/

  /**
   * TODO : remove me - we don't do this anymore.
   * <p>
   * Can't check if it's valid if we don't have a model.
   *
   * @paramx foreign
   * @return
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#isValidForeignPhrase
   */
   @Override
  public boolean isValidForeignPhrase(String foreign, String transliteration) throws DominoSessionException {
    return getAudioFileHelper().checkLTSOnForeignPhrase(foreign, transliteration);
  }

  /**
   * @param projID should be a projid from project table...
   * @throws DominoSessionException
   * @throws RestrictedOperationException
   * @see mitll.langtest.client.LangTest#tellHydraServerToRefreshProject
   */
  @Override
  public void configureAndRefresh(int projID) throws DominoSessionException, RestrictedOperationException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    if (hasAdminOrCDPerm(userIDFromSessionOrDB)) {
      if (projID == -1) {
        logger.error("huh? why sending bad project id?");
      } else {
        Project project = db.getProject(projID);
        if (project != null) {
          db.configureProject(project, true);
          db.getProjectManagement().refreshProjects();
        } else {
          logger.error("configureAndRefresh : no project with id " + projID);
        }
      }
    } else {
      logger.warn("configureAndRefresh project " + projID + " user #" + userIDFromSessionOrDB + " does not have permission?");
      throw getRestricted(UPDATING_PROJECT_INFO);
    }
  }

  @Override
  public void ensureAudio(int resultID) throws DominoSessionException {
    ensureAudioForAnswers(getProjectIDFromUser(), db.getResultDAO().getMonitorResultByID(resultID));
  }

  private void ensureAudioForAnswers(int projectID, MonitorResult result) {
    Language language = db.getLanguageEnum(projectID);
    Map<Integer, User> idToUser = new HashMap<>();

    if (result == null) logger.error("couldn't find result in " + projectID);
    else {
      String path = result.getAnswer();
      CommonExercise commonExercise = db.getExercise(projectID, result.getExID());
      /*String actualPath =*/
      ensureAudioHelper.ensureCompressedAudio(result.getUserid(), commonExercise, path, result.getAudioType(), language, idToUser, true);
//    logger.info("ensureAudioForAnswers initial path " + path + " compressed actual " + actualPath);
    }
  }
}