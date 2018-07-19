package mitll.langtest.server.scoring;

import com.google.gson.JsonObject;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.image.ImageType;
import mitll.langtest.server.audio.image.TranscriptEvent;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.database.refaudio.IRefResultDAO;
import mitll.langtest.server.database.result.ISlimResult;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.AlignmentOutput;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AlignmentHelper {
  private static final Logger logger = LogManager.getLogger(AlignmentHelper.class);

  private static final boolean USE_PHONE_TO_DISPLAY = true;
  private static final boolean WARN_MISSING_REF_RESULT = false;


  private ServerProperties serverProps;
  private IRefResultDAO refResultDAO;

  public AlignmentHelper(ServerProperties serverProperties, IRefResultDAO refResultDAO) {
    this.serverProps = serverProperties;
    this.refResultDAO = refResultDAO;
  }

  /**
   * @param projectID
   * @param toAddAudioTo
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getFullExercises
   */
  public void addAlignmentOutput(int projectID, Project project, Collection<ClientExercise> toAddAudioTo) {
    if (project != null) {
      Map<Integer, AlignmentOutput> audioToAlignment = project.getAudioToAlignment();
      Map<Integer, AudioAttribute> idToAudio = new HashMap<>();

      logger.info("addAlignmentOutput : For project " + projectID + " found " + audioToAlignment.size() +
          " audio->alignment entries, trying to marry to " + toAddAudioTo.size() + " exercises");

      for (ClientExercise exercise : toAddAudioTo) {
        setAlignmentInfo(audioToAlignment, idToAudio, exercise);
        exercise.getDirectlyRelated().forEach(context -> setAlignmentInfo(audioToAlignment, idToAudio, context));
      }

      Map<Integer, AlignmentOutput> alignments = rememberAlignments(projectID, idToAudio, project.getLanguage());

      synchronized (audioToAlignment) {
        audioToAlignment.putAll(alignments);
      }
    }
  }

  /**
   * TODO : why not concurrent hash map...
   *
   * @param audioToAlignment
   * @param idToAudio
   * @param exercise
   */
  private void setAlignmentInfo(Map<Integer, AlignmentOutput> audioToAlignment,
                                Map<Integer, AudioAttribute> idToAudio,
                                ClientExercise exercise) {
    for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
      AlignmentOutput currentAlignment = audioAttribute.getAlignmentOutput();
      if (currentAlignment == null) {
        synchronized (audioToAlignment) {
          AlignmentOutput alignmentOutput1 = audioToAlignment.get(audioAttribute.getUniqueID());

          if (alignmentOutput1 == null) {
            idToAudio.put(audioAttribute.getUniqueID(), audioAttribute);
          } else {  // not sure how this can happen
            audioAttribute.setAlignmentOutput(alignmentOutput1);
          }
        }
      }
    }
  }

  /**
   * @param projectID
   * @param idToAudio
   * @param language
   * @return
   * @see #addAlignmentOutput
   */
  private Map<Integer, AlignmentOutput> rememberAlignments(int projectID,
                                                           Map<Integer, AudioAttribute> idToAudio, String language) {
    if (!idToAudio.isEmpty() && idToAudio.size() > 50)
      logger.info("rememberAlignments : asking for " + idToAudio.size() + " alignment outputs from database");

    Map<Integer, AlignmentOutput> alignments = getAlignmentsFromDB(projectID, idToAudio.keySet(), language);
    addAlignmentToAudioAttribute(idToAudio, alignments);
    return alignments;
  }


  private void addAlignmentToAudioAttribute(Map<Integer, AudioAttribute> idToAudio,
                                            Map<Integer, AlignmentOutput> alignments) {
    idToAudio.forEach((k, v) -> {
      AlignmentOutput alignmentOutput = alignments.get(k);
      if (alignmentOutput == null) {
        // logger.warn("addAlignmentToAudioAttribute : couldn't get alignment for audio #" + v.getUniqueID());
      } else {
        v.setAlignmentOutput(alignmentOutput);
      }
    });
  }

  /**
   * get alignment from db
   *
   * @param projid
   * @param audioIDs
   * @param language
   * @return
   * @see #rememberAlignments
   */
  private Map<Integer, AlignmentOutput> getAlignmentsFromDB(int projid, Set<Integer> audioIDs, String language) {
    //logger.info("getAlignmentsFromDB asking for " + audioIDs.size());
    if (audioIDs.isEmpty()) logger.warn("getAlignmentsFromDB not asking for any audio ids?");
    Map<Integer, ISlimResult> audioIDMap = getAudioIDMap(refResultDAO.getAllSlimForProjectIn(projid, audioIDs));
    logger.info("getAlignmentsFromDB found " + audioIDs.size() + "/" + audioIDMap.size() + " ref result alignments...");
    return parseJsonToGetAlignments(audioIDs, audioIDMap, language);
  }

  @NotNull
  private Map<Integer, ISlimResult> getAudioIDMap(Collection<ISlimResult> jsonResultsForProject) {
    Map<Integer, ISlimResult> audioToResult = new HashMap<>();
    jsonResultsForProject.forEach(slimResult -> audioToResult.put(slimResult.getAudioID(), slimResult));
    return audioToResult;
  }

  private Map<Integer, AlignmentOutput> parseJsonToGetAlignments(Collection<Integer> audioIDs,
                                                                 Map<Integer, ISlimResult> audioToResult,
                                                                 String language) {
    Map<Integer, AlignmentOutput> idToAlignment = new HashMap<>();
    for (Integer audioID : audioIDs) {
      // do we have alignment for this audio in the map
      ISlimResult cachedResult = audioToResult.get(audioID);

      if (cachedResult == null) { // nope, ask the database
        cachedResult = refResultDAO.getResult(audioID);
      }

      if (cachedResult == null || !cachedResult.isValid()) { // not in the database, recalculate it now?
        if (WARN_MISSING_REF_RESULT) logger.info("parseJsonToGetAlignments : nothing in database for audio " + audioID);
      } else {
        getCachedAudioRef(idToAlignment, audioID, cachedResult, language);  // OK, let's translate the db info into the alignment output
      }
    }
    return idToAlignment;
  }

  private void getCachedAudioRef(Map<Integer, AlignmentOutput> idToAlignment, Integer audioID, ISlimResult cachedResult, String language) {
    PrecalcScores precalcScores = getPrecalcScores(USE_PHONE_TO_DISPLAY, cachedResult, language);
    Map<ImageType, Map<Float, TranscriptEvent>> typeToTranscriptEvents =
        getTypeToTranscriptEvents(precalcScores.getJsonObject(), USE_PHONE_TO_DISPLAY, language);
    Map<NetPronImageType, List<TranscriptSegment>> typeToSegments = getTypeToSegments(typeToTranscriptEvents, language);
//    logger.info("getCachedAudioRef : cache HIT for " + audioID + " returning " + typeToSegments);
    idToAlignment.put(audioID, new AlignmentOutput(typeToSegments));
  }

  @NotNull
  private PrecalcScores getPrecalcScores(boolean usePhoneToDisplay, ISlimResult cachedResult, String language) {
    return new PrecalcScores(serverProps, cachedResult,
        usePhoneToDisplay || serverProps.usePhoneToDisplay(), language);
  }

  private Map<ImageType, Map<Float, TranscriptEvent>> getTypeToTranscriptEvents(JsonObject object,
                                                                                boolean usePhoneToDisplay, String language) {
    return
        new ParseResultJson(serverProps, language)
            .readFromJSON(object, "words", "w", usePhoneToDisplay, null);
  }

  /**
   * TODO : why four copies!!!
   *
   * @param typeToEvent
   * @param language
   * @return
   * @see #getCachedAudioRef
   */
  @NotNull
  private Map<NetPronImageType, List<TranscriptSegment>> getTypeToSegments(Map<ImageType, Map<Float, TranscriptEvent>> typeToEvent, String language) {
    Map<NetPronImageType, List<TranscriptSegment>> typeToEndTimes = new HashMap<>();

    Map<String, String> phoneToDisplay = serverProps.getPhoneToDisplay(language);
    for (Map.Entry<ImageType, Map<Float, TranscriptEvent>> typeToEvents : typeToEvent.entrySet()) {
      NetPronImageType key = NetPronImageType.valueOf(typeToEvents.getKey().toString());
      boolean isPhone = key == NetPronImageType.PHONE_TRANSCRIPT;

      List<TranscriptSegment> endTimes = typeToEndTimes.get(key);
      if (endTimes == null) {
        typeToEndTimes.put(key, endTimes = new ArrayList<>());
      }

      StringBuilder builder = new StringBuilder();
      for (Map.Entry<Float, TranscriptEvent> event : typeToEvents.getValue().entrySet()) {
        TranscriptEvent value = event.getValue();
        String event1 = value.getEvent();
        String displayName = isPhone ? getDisplayName(event1, phoneToDisplay) : event1;
        endTimes.add(new TranscriptSegment(value.getStart(), value.getEnd(), event1, value.getScore(), displayName, builder.length()));

        if (!isPhone) {
          builder.append(event1);
        }
      }
    }

    return typeToEndTimes;
  }


  private String getDisplayName(String event, Map<String, String> phoneToDisplay) {
    String displayName = phoneToDisplay.get(event);
    displayName = displayName == null ? event : displayName;
    return displayName;
  }
}
