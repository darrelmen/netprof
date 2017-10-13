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

import mitll.langtest.server.audio.image.ImageType;
import mitll.langtest.server.audio.image.TranscriptEvent;
import com.google.gson.JsonObject;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.services.ScoringService;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.audio.DecoderOptions;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.result.ISlimResult;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.server.scoring.PrecalcScores;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.AlignmentOutput;
import mitll.langtest.shared.scoring.ImageOptions;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class ScoringServiceImpl extends MyRemoteServiceServlet implements ScoringService {
  private static final Logger logger = LogManager.getLogger(ScoringServiceImpl.class);

  private static final boolean DEBUG = true;
  private static final String AUDIO_RECORDING = "audioRecording";
  private static final String WRITE_AUDIO_FILE = "writeAudioFile";

  /**
   * NOTE NOTE NOTE : doesn't make sure we have mp3 or ogg file equivalents...
   * maybe it should?
   *
   * @param resultID
   * @return
   * @see mitll.langtest.client.scoring.ReviewScoringPanel#scoreAudio
   */
  @Override
  public PretestScore getResultASRInfo(int resultID, ImageOptions imageOptions) {
    PretestScore asrScoreForAudio = null;
    try {
      Result result = db.getResultDAO().getResultByID(resultID);
      int exerciseID = result.getExerciseID();

      boolean isAMAS = serverProps.isAMAS();
      CommonShell exercise = null;
      String sentence = "";
      String transliteration = "";
      if (isAMAS) {
        exercise = db.getAMASExercise(exerciseID);
        sentence = exercise.getForeignLanguage();
      } else {
        CommonExercise exercise1 = db.getExercise(getProjectID(), exerciseID);

        if (exercise1 != null) {
          transliteration = exercise1.getTransliteration();
          exercise = exercise1;
          Collection<CommonExercise> directlyRelated = exercise1.getDirectlyRelated();
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
            getPrecalcScores(serverProps.usePhoneToDisplay(), result),

            new DecoderOptions()
                .setDoFlashcard(false)
                .setCanUseCache(serverProps.useScoreCache())
                .setUsePhoneToDisplay(serverProps.usePhoneToDisplay()));
      }
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }

    return asrScoreForAudio;
  }

  @Override
  public void recalcAlignments(int projid) {
    recalcAlignments(getUserIDFromSessionOrDB(), db.getProject(projid));
  }

  private void recalcAlignments(int userIDFromSession, Project project) {
    if (project.getWebservicePort() > 100) {
      try {
        String name = project.getProject().name();
        logger.info("recalcAlignments Doing project : " + name);

        int projectID = project.getID();
        List<Integer> audioIDs = getAllAudioIDs(projectID);

        long then = System.currentTimeMillis();
        AudioFileHelper audioFileHelper = project.getAudioFileHelper();
        logger.info("getAllAlignments Doing project " + name + " : " + audioIDs.size() + " audio cuts with " + audioFileHelper);

        Map<Integer, ISlimResult> audioToResult = getAudioIDMap(projectID);
        //    logger.info("getAllAlignments recalc " +audioToResult.size() + " alignments...");
        recalcAlignments(projectID, audioIDs, audioFileHelper, userIDFromSession, audioToResult, true);

        long now = System.currentTimeMillis();

        long l = (now - then) / 1000;
        long min = l / 60;
        logger.info("getAllAlignments Doing project " + name + " " + audioIDs.size() + " audio cuts took " + min + " minutes.");
      } catch (Exception e) {
        logger.error("getAllAlignments got " + e, e);
      }
    } else {
      logger.info("getAllAlignments no hydra service for " + project);
    }
  }

  /**
   * @param projectID
   * @return
   * @see #recalcAlignments(int, Project)
   */
  @NotNull
  private List<Integer> getAllAudioIDs(int projectID) {
    Map<Integer, List<AudioAttribute>> exToAudio = db.getAudioDAO().getExToAudio(projectID);
    List<Integer> audioIDs = new ArrayList<>();
    for (List<AudioAttribute> values : exToAudio.values()) {
      audioIDs.addAll(values.stream().map(AudioAttribute::getUniqueID).collect(Collectors.toList()));
    }
    return audioIDs;
  }

  @NotNull
  private Map<Integer, ISlimResult> getAudioIDMap(int id) {
    return getAudioIDMap(db.getRefResultDAO().getAllSlimForProject(id));
  }

  @NotNull
  private Map<Integer, ISlimResult> getAudioIDMap(Collection<ISlimResult> jsonResultsForProject) {
    Map<Integer, ISlimResult> audioToResult = new HashMap<>();
    for (ISlimResult slimResult : jsonResultsForProject) audioToResult.put(slimResult.getAudioID(), slimResult);
    return audioToResult;
  }

  /**
   * @param projid
   * @param audioIDs
   * @return
   * @see mitll.langtest.client.scoring.TwoColumnExercisePanel#getRefAudio
   */
  @Override
  public Map<Integer, AlignmentOutput> getAlignments(int projid, Set<Integer> audioIDs) {
    logger.info("getAlignments asking for " + audioIDs);
    Map<Integer, ISlimResult> audioIDMap = getAudioIDMap(db.getRefResultDAO().getAllSlimForProjectIn(projid, audioIDs));
    logger.info("getAlignments recalc " + audioIDMap.size() + " alignments...");
    return recalcAlignments(projid, audioIDs, getUserIDFromSessionOrDB(), audioIDMap, db.getProject(projid).hasModel());
    //  logger.info("getAligments for " + projid + " and " + audioIDs + " found " + idToAlignment.size());
  }

  private Map<Integer, AlignmentOutput> recalcAlignments(int projid,
                                                         Collection<Integer> audioIDs,
                                                         int userIDFromSession,
                                                         Map<Integer, ISlimResult> audioToResult,
                                                         boolean hasModel) {
    return recalcAlignments(projid, audioIDs, getAudioFileHelper(projid), userIDFromSession, audioToResult, hasModel);
  }

  private AudioFileHelper getAudioFileHelper(int projid) {
    return db.getProject(projid).getAudioFileHelper();
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
  private Map<Integer, AlignmentOutput> recalcAlignments(int projid,
                                                         Collection<Integer> audioIDs,
                                                         AudioFileHelper audioFileHelper,
                                                         int userIDFromSession,
                                                         Map<Integer, ISlimResult> audioToResult,
                                                         boolean hasModel) {
    Map<Integer, AlignmentOutput> idToAlignment = new HashMap<>();

    if (hasModel) {
      logger.info("recalcAlignments recalc " + audioIDs.size() + " audio ids");

      if (audioIDs.isEmpty()) logger.error("recalcAlignments huh? no audio for " + projid);

      for (Integer audioID : audioIDs) {
        // do we have alignment for this audio in the map
        recalcOneOrGetCached(projid, audioID, audioFileHelper, userIDFromSession, audioToResult.get(audioID), idToAlignment);
      }
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
  private void recalcOneOrGetCached(int projid, Integer audioID,
                                    AudioFileHelper audioFileHelper,
                                    int userIDFromSession,
                                    ISlimResult cachedResult,
                                    Map<Integer, AlignmentOutput> idToAlignment) {
    if (cachedResult == null) { // nope, ask the database
      cachedResult = db.getRefResultDAO().getResult(audioID);
    }

    if (cachedResult == null || !cachedResult.isValid()) { // not in the database, recalculate it now
      if (cachedResult != null && !cachedResult.isValid()) {
        boolean b = db.getRefResultDAO().removeByAudioID(audioID);
        logger.warn("getAlignmentsFromDB remove invalid ref result for audio id " + audioID + " = " + b);
      }

      PretestScore pretestScore = recalcRefAudioWithHelper(projid, audioID, audioFileHelper, userIDFromSession);
      if (pretestScore != null) {
        idToAlignment.put(audioID, pretestScore);
      }
    } else {
      logger.info("recalcOneOrGetCached : found cached result for projid " + projid + " audio id " + audioID);

      getCachedAudioRef(idToAlignment, audioID, cachedResult);  // OK, let's translate the db info into the alignment output
    }
  }

  private void getCachedAudioRef(Map<Integer, AlignmentOutput> idToAlignment, Integer audioID, ISlimResult cachedResult) {
    PrecalcScores precalcScores = getPrecalcScores(false, cachedResult);
    Map<ImageType, Map<Float, TranscriptEvent>> typeToTranscriptEvents =
        getTypeToTranscriptEvents(precalcScores.getJsonObject(), false);
    Map<NetPronImageType, List<TranscriptSegment>> typeToSegments = getTypeToSegments(typeToTranscriptEvents);
//    logger.info("getCachedAudioRef : cache HIT for " + audioID + " returning " + typeToSegments);
    idToAlignment.put(audioID, new AlignmentOutput(typeToSegments));
  }

  private PretestScore recalcRefAudioWithHelper(int projid,
                                                Integer audioID,
                                                AudioFileHelper audioFileHelper,
                                                int userIDFromSession) {
    AudioAttribute byID = db.getAudioDAO().getByID(audioID);
    if (byID != null) {
      CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(projid, byID.getExid());

      boolean contextAudio = byID.isContextAudio();

/*
      if (customOrPredefExercise != null) {
        logger.info("getAlignmentsFromDB decoding " + audioID +
            (contextAudio ? " CONTEXT" : "") +
            " for exercise " + byID.getExid() + " : '" +
            customOrPredefExercise.getEnglish() + "' = '" + customOrPredefExercise.getForeignLanguage() + "'");
      }
      */

      // cover for import bug...
      if (contextAudio &&
          customOrPredefExercise != null &&
          customOrPredefExercise.getDirectlyRelated() != null &&
          !customOrPredefExercise.getDirectlyRelated().isEmpty()) {
        customOrPredefExercise = customOrPredefExercise.getDirectlyRelated().iterator().next();

        //logger.info("getAlignmentsFromDB using " + customOrPredefExercise.getID() + " " + customOrPredefExercise.getEnglish() + " instead ");
      }

      logger.info("getAlignmentsFromDB decoding " + audioID + " for " + byID.getExid() + "...");
      return audioFileHelper.decodeAndRemember(customOrPredefExercise, byID, false, userIDFromSession);
    } else {
      logger.info("getAlignmentsFromDB can't find audio id " + audioID);
      return null;
    }
  }

  private Map<ImageType, Map<Float, TranscriptEvent>> getTypeToTranscriptEvents(JsonObject object,
                                                                                boolean usePhoneToDisplay) {
    return
        new ParseResultJson(db.getServerProps())
            .readFromJSON(object, "words", "w", usePhoneToDisplay, null);
  }

  /**
   * @param typeToEvent
   * @return
   * @see #getCachedAudioRef
   */
  @NotNull
  private Map<NetPronImageType, List<TranscriptSegment>> getTypeToSegments(Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent) {
    Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = new HashMap<>();

    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      List<TranscriptSegment> endTimes = typeToEndTimes.get(key);
      if (endTimes == null) {
        typeToEndTimes.put(key, endTimes = new ArrayList<>());
      }
      for (Map.Entry<Float, TranscriptEvent> event : typeToEvents.getValue().entrySet()) {
        TranscriptEvent value = event.getValue();
        endTimes.add(new TranscriptSegment(value.start, value.end, value.event, value.score));
      }
    }

    return typeToEndTimes;
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
                                          boolean usePhonemeMap) {
    File absoluteAudioFile = pathHelper.getAbsoluteAudioFile(testAudioFile.replaceAll(".ogg", ".wav"));

    int projectID = getProjectID();
    CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(projectID, exerciseID);

    String english = customOrPredefExercise == null ? "" : customOrPredefExercise.getEnglish();
    AudioFileHelper audioFileHelper = getAudioFileHelper(projectID);
    PrecalcScores precalcScores =
        audioFileHelper
            .checkForWebservice(exerciseID, english, sentence, projectID, getUserIDFromSessionOrDB(), absoluteAudioFile);

    return getPretestScore(reqid,
        (int) resultID,
        testAudioFile,
        sentence, transliteration, imageOptions,
        exerciseID, usePhonemeMap, precalcScores, audioFileHelper);
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
                                       AudioFileHelper audioFileHelper) {
    if (testAudioFile.equals(AudioConversion.FILE_MISSING)) return new PretestScore(-1);
    long then = System.currentTimeMillis();

    String[] split = testAudioFile.split(File.separator);
    String answer = split[split.length - 1];
//    String wavEndingAudio = answer.replaceAll(".mp3", ".wav").replaceAll(".ogg", ".wav");
    ISlimResult cachedResult = null;//db.getRefResultDAO().getResult(audioID);//exerciseID, wavEndingAudio);

    boolean usePhoneToDisplay1 = usePhoneToDisplay || serverProps.usePhoneToDisplay();
    if (cachedResult != null && precalcScores == null) {
      precalcScores = getPrecalcScores(usePhoneToDisplay, cachedResult);
      if (DEBUG)
        logger.debug("getPretestScore Cache HIT  : align exercise id = " + exerciseID + " file " + answer);
      //  +
      //  " found previous " + cachedResult.getUniqueID());
    } else {
      logger.debug("getPretestScore Cache MISS : align exercise id = " + exerciseID + " file " + answer);
    }

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

    if (resultID > -1 && cachedResult == null) { // alignment has two steps : 1) post the audio, then 2) do alignment
      db.rememberScore(resultID, asrScoreForAudio, true);
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
            .setDoFlashcard(false)
            .setCanUseCache(serverProps.useScoreCache())
            .setUsePhoneToDisplay(usePhoneToDisplay1)
    );
  }

  @NotNull
  private PrecalcScores getPrecalcScores(boolean usePhoneToDisplay, ISlimResult cachedResult) {
    return new PrecalcScores(serverProps, cachedResult, usePhoneToDisplay || serverProps.usePhoneToDisplay());
  }

  /**
   * Doesn't really need to be on the scoring service...
   *
   * @param resultID
   * @param roundTrip
   */
  @Override
  public void addRoundTrip(int resultID, int roundTrip) {
    db.getAnswerDAO().addRoundTrip(resultID, roundTrip);
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
   * @see mitll.langtest.client.scoring.SimplePostAudioRecordButton#postAudioFile(String)
   */
  @Override
  public AudioAnswer getAlignment(String base64EncodedString,
                                  String textToAlign,
                                  String transliteration,
                                  String identifier,
                                  int reqid,
                                  String device) {
    AudioAnswer audioAnswer = getAudioFileHelper().getAlignment(base64EncodedString, textToAlign, transliteration, identifier, reqid,
        serverProps.usePhoneToDisplay());

    if (!audioAnswer.isValid() && audioAnswer.getDurationInMillis() == 0) {
      logger.warn("getAlignment : huh? got zero length recording for " + identifier + " from " + device);
      logEvent(identifier, device);
    }
    return audioAnswer;
  }

  /**
   * @param projid
   * @return
   * @see mitll.langtest.client.project.ProjectEditForm#getUserForm
   */
  @Override
  public boolean isHydraRunning(int projid) {
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
  }

  private void logEvent(String exid, String device) {
    try {
      db.logEvent(AUDIO_RECORDING, WRITE_AUDIO_FILE, exid, "Writing audio - got zero duration!", -1, device);
    } catch (Exception e) {
      logger.error("got " + e, e);
    }
  }

  /**
   * Can't check if it's valid if we don't have a model.
   *
   * @param foreign
   * @return
   * @see mitll.langtest.client.custom.dialog.NewUserExercise#isValidForeignPhrase
   */
  @Override
  public boolean isValidForeignPhrase(String foreign, String transliteration) {
    return getAudioFileHelper().checkLTSOnForeignPhrase(foreign, transliteration);
  }
}