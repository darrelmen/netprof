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

import audio.image.ImageType;
import audio.image.TranscriptEvent;
import com.google.gson.JsonObject;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.services.ScoringService;
import mitll.langtest.server.audio.AudioConversion;
import mitll.langtest.server.audio.AudioFileHelper;
import mitll.langtest.server.audio.DecoderOptions;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.database.result.ISlimResult;
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
  public void getAllAlignments() {
    final int userIDFromSession = getUserIDFromSession();

    new Thread(() -> db.getProjects().forEach(project -> {
      logger.info("Doing project " + project);

      int id = project.getID();
      Map<Integer, List<AudioAttribute>> exToAudio = db.getAudioDAO().getExToAudio(id);
      List<Integer> ids = new ArrayList<>();
      for (List<AudioAttribute> values : exToAudio.values()) {
        ids.addAll(values.stream().map(AudioAttribute::getUniqueID).collect(Collectors.toList()));
      }

      long then = System.currentTimeMillis();
      AudioFileHelper audioFileHelper = project.getAudioFileHelper();
      logger.info("Doing project " + project + " " + ids.size() + " audio cuts with " + audioFileHelper);

      // getAlignments(id, ids);

      recalcAlignments(id, ids, new HashMap<>(), audioFileHelper, userIDFromSession);

      long now = System.currentTimeMillis();

      long l = (now - then) / 1000;
      long min = l / 60;
      logger.info("Doing project " + project + " " + ids.size() + " audio cuts took " + min + " minutes.");

    })).start();

  }

  @Override
  public Map<Integer, AlignmentOutput> getAlignments(int projid, Collection<Integer> audioIDs) {
    Map<Integer, AlignmentOutput> idToAlignment = new HashMap<>();
//    logger.info("getAlignments asking for " + audioIDs);

    int userIDFromSession = getUserIDFromSession();
    recalcAlignments(projid, audioIDs, idToAlignment, userIDFromSession);
  //  logger.info("getAligments for " + projid + " and " + audioIDs + " found " + idToAlignment.size());
    return idToAlignment;
  }

  private void recalcAlignments(int projid, Collection<Integer> audioIDs, Map<Integer, AlignmentOutput> idToAlignment, int userIDFromSession) {
    AudioFileHelper audioFileHelper = getAudioFileHelper();
    recalcAlignments(projid, audioIDs, idToAlignment, audioFileHelper, userIDFromSession);
  }

  private void recalcAlignments(int projid, Collection<Integer> audioIDs, Map<Integer, AlignmentOutput> idToAlignment,
                                AudioFileHelper audioFileHelper, int userIDFromSession) {
    for (Integer audioID : audioIDs) {
      ISlimResult cachedResult = db.getRefResultDAO().getResult(audioID);//exerciseID, wavEndingAudio);
      if (cachedResult == null || !cachedResult.isValid()) {
        if (cachedResult != null && !cachedResult.isValid()) {
          boolean b = db.getRefResultDAO().removeByAudioID(audioID);
          logger.error("getAlignments remove invalid ref result for audio id " + audioID + " = " + b);
        }
        recalcRefAudioWithHelper(projid, idToAlignment, audioID, audioFileHelper, userIDFromSession);
      } else {
        getCachedAudioRef(idToAlignment, audioID, cachedResult);
      }
    }
  }

  private void getCachedAudioRef(Map<Integer, AlignmentOutput> idToAlignment, Integer audioID, ISlimResult cachedResult) {
    PrecalcScores precalcScores = getPrecalcScores(false, cachedResult);
    Map<ImageType, Map<Float, TranscriptEvent>> typeToTranscriptEvents =
        getTypeToTranscriptEvents(precalcScores.getJsonObject(), false);
    Map<NetPronImageType, List<TranscriptSegment>> typeToSegments = getTypeToSegments(typeToTranscriptEvents);
//    logger.info("cache HIT for " + audioID + " returning " + typeToSegments);
    idToAlignment.put(audioID, new AlignmentOutput(typeToSegments));
  }

//  private void recalcAudioRef(int projid, Map<Integer, AlignmentOutput> idToAlignment, Integer audioID) {
//    AudioFileHelper audioFileHelper = getAudioFileHelper();
//    recalcRefAudioWithHelper(projid, idToAlignment, audioID, audioFileHelper);
//  }

  private void recalcRefAudioWithHelper(int projid,
                                        Map<Integer, AlignmentOutput> idToAlignment,
                                        Integer audioID,
                                        AudioFileHelper audioFileHelper,
                                        int userIDFromSession) {
    AudioAttribute byID = db.getAudioDAO().getByID(audioID);
    if (byID != null) {
      CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(projid, byID.getExid());

      boolean contextAudio = byID.isContextAudio();

/*
      if (customOrPredefExercise != null) {
        logger.info("getAlignments decoding " + audioID +
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

        //logger.info("getAlignments using " + customOrPredefExercise.getID() + " " + customOrPredefExercise.getEnglish() + " instead ");
      }

      PretestScore pretestScore =
          audioFileHelper.decodeAndRemember(customOrPredefExercise, byID, false, userIDFromSession);
      //logger.info("getAlignments decoding " + audioID + " for " + byID.getExid() + " got " + pretestScore);
      idToAlignment.put(audioID, pretestScore);
    } else {
      logger.info("getAlignments can't find audio id " + audioID);
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

    CommonExercise customOrPredefExercise = db.getCustomOrPredefExercise(getProjectID(), exerciseID);

    String english = customOrPredefExercise == null ? "" : customOrPredefExercise.getEnglish();
    PrecalcScores precalcScores =
        getAudioFileHelper().checkForWebservice(exerciseID, english, sentence, getProjectID(), getUserIDFromSession(), absoluteAudioFile);

    return getPretestScore(reqid,
        (int) resultID,
        testAudioFile,
        sentence, transliteration, imageOptions,
        exerciseID, usePhonemeMap, precalcScores);
  }
/*
  public AlignmentOutput getAlignment(int audioID) {
    ISlimResult cachedResult =  db.getRefResultDAO().getResult(audioID);
    if (cachedResult == null) {

    }
    else {
      return cachedResult;
    }
  }*/

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
   * @paramx audioID
   */
  private PretestScore getPretestScore(int reqid,
                                       int resultID,
                                       String testAudioFile,
                                       String sentence,
                                       String transliteration,
                                       ImageOptions imageOptions,
                                       int exerciseID,
                                       boolean usePhoneToDisplay,
                                       PrecalcScores precalcScores) {
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
        usePhoneToDisplay1);

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

  private PretestScore getPretestScoreMaybeUseCache(int reqid, String testAudioFile, String sentence, String transliteration, ImageOptions imageOptions, int exerciseID, PrecalcScores precalcScores, boolean usePhoneToDisplay1) {
    return getAudioFileHelper().getASRScoreForAudio(
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
                                  int reqid, String device) {
    AudioAnswer audioAnswer = getAudioFileHelper().getAlignment(base64EncodedString, textToAlign, transliteration, identifier, reqid,
        serverProps.usePhoneToDisplay());

    if (!audioAnswer.isValid() && audioAnswer.getDurationInMillis() == 0) {
      logger.warn("huh? got zero length recording " + identifier);
      logEvent("audioRecording", "writeAudioFile", identifier, "Writing audio - got zero duration!", -1, device);
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

  private void logEvent(String id, String widgetType, String exid, String context, int userid,
                        String device) {
    try {
      db.logEvent(id, widgetType, exid, context, userid, device);
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