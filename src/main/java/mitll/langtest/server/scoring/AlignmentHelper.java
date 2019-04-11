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
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.AlignmentAndScore;
import mitll.langtest.shared.scoring.AlignmentOutput;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AlignmentHelper extends TranscriptSegmentGenerator {
  private static final Logger logger = LogManager.getLogger(AlignmentHelper.class);

  private static final boolean USE_PHONE_TO_DISPLAY = true;
  private static final boolean DEBUG = false;
  private static final boolean WARN_MISSING_REF_RESULT = false || DEBUG;

  private final IRefResultDAO refResultDAO;

  public AlignmentHelper(ServerProperties serverProperties, IRefResultDAO refResultDAO) {
    super(serverProperties);
    this.refResultDAO = refResultDAO;
  }

  /**
   * @param toAddAudioTo
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getFullExercises
   */
  public void addAlignmentOutput(Project project, Collection<ClientExercise> toAddAudioTo) {
    if (project != null) {
      int projectID = project.getID();
      Map<Integer, AlignmentOutput> audioToAlignment = project.getAudioToAlignment();
//      Map<Integer, AudioAttribute> idToAudio = new HashMap<>();

      logger.info("addAlignmentOutput : For project " + projectID + " found " + audioToAlignment.size() +
          " audio->alignment entries, trying to marry to " + toAddAudioTo.size() + " exercises");

      Set<Integer> ids = new HashSet<>();

      for (ClientExercise exercise : toAddAudioTo) {
        //  setAlignmentInfo(audioToAlignment, idToAudio, exercise);
        addAudioIDs(exercise, ids);
        //  exercise.getDirectlyRelated().forEach(context -> setAlignmentInfo(audioToAlignment, idToAudio, context));
        exercise.getDirectlyRelated().forEach(context -> addAudioIDs(context, ids));
      }

      ids.removeAll(audioToAlignment.keySet());

      // Map<Integer, AlignmentAndScore> alignments = getAlignmentsFromDB(projectID, idToAudio, project.getLanguageEnum());
      Map<Integer, AlignmentAndScore> alignments = getAlignmentsFromDB(projectID, ids, project.getLanguageEnum());

      // synchronized (audioToAlignment) {
      audioToAlignment.putAll(alignments);
      //}
    }
  }

  /**
   * TODO : why not concurrent hash map...
   *
   * Look in the map of
   *
   * @param audioToAlignment
   * @param idToAudio        for all that are missing alignment
   * @param exercise
   */
/*
  private void setAlignmentInfo(Map<Integer, AlignmentOutput> audioToAlignment,
                                Map<Integer, AudioAttribute> idToAudio,
                                ClientExercise exercise) {
    for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
      AlignmentOutput currentAlignment = audioAttribute.getAlignmentOutput();
      if (currentAlignment == null) {
        synchronized (audioToAlignment) {
          AlignmentOutput alignmentOutput = audioToAlignment.get(audioAttribute.getUniqueID());

          if (alignmentOutput == null) {
            idToAudio.put(audioAttribute.getUniqueID(), audioAttribute);
          } else {  // not sure how this can happen
            audioAttribute.setAlignmentOutput(alignmentOutput);
          }
        }
      }
    }
  }
*/
  private void addAudioIDs(ClientExercise exercise, Set<Integer> ids) {
    for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
      ids.add(audioAttribute.getUniqueID());
    }
  }


  /**
   * @param projectID
   * @param idToAudio
   * @param language
   * @return
   * @see #addAlignmentOutput
   */
/*  private Map<Integer, AlignmentAndScore> rememberAlignments(int projectID,
                                                           Map<Integer, AudioAttribute> idToAudio, Language language) {
    if (!idToAudio.isEmpty() && idToAudio.size() > 50 || DEBUG)
      logger.info("rememberAlignments : asking for " + idToAudio.size() + " alignment outputs from database");

    Map<Integer, AlignmentAndScore> alignments = getAlignmentsFromDB(projectID, idToAudio.keySet(), language);
    addAlignmentToAudioAttribute(idToAudio, alignments);
    return alignments;
  }*/


/*  private void addAlignmentToAudioAttribute(Map<Integer, AudioAttribute> idToAudio,
                                            Map<Integer, AlignmentAndScore> alignments) {
    idToAudio.forEach((k, v) -> {
      AlignmentOutput alignmentOutput = alignments.get(k);
      if (alignmentOutput == null) {
        // logger.warn("addAlignmentToAudioAttribute : couldn't get alignment for audio #" + v.getUniqueID());
      } else {
//        logger.info("addAlignmentToAudioAttribute set alignment output " + alignmentOutput + " on " + v.getUniqueID() + " : " + v.getTranscript());
        v.setAlignmentOutput(alignmentOutput);
      }
    });
  }*/

  /**
   * get alignment from db
   *
   * @param projid
   * @param audioIDs
   * @param language
   * @return
   * @see #rememberAlignments
   */
  private Map<Integer, AlignmentAndScore> getAlignmentsFromDB(int projid, Set<Integer> audioIDs, Language language) {
    if (DEBUG) logger.info("getAlignmentsFromDB asking for " + audioIDs.size());

    if (audioIDs.isEmpty()) {
      logger.warn("getAlignmentsFromDB not asking for any audio ids?");
    }
    Map<Integer, ISlimResult> audioIDMap = getAudioIDMap(refResultDAO.getAllSlimForProjectIn(projid, audioIDs));

    if (audioIDMap.size() != audioIDs.size() || DEBUG) {
      logger.info("getAlignmentsFromDB found " + audioIDs.size() + "/" + audioIDMap.size() + " ref result alignments...");
    }

    return parseJsonToGetAlignments(audioIDs, audioIDMap, language);
  }

  @NotNull
  private Map<Integer, ISlimResult> getAudioIDMap(Collection<ISlimResult> jsonResultsForProject) {
    Map<Integer, ISlimResult> audioToResult = new HashMap<>();
    jsonResultsForProject.forEach(slimResult -> audioToResult.put(slimResult.getAudioID(), slimResult));
    return audioToResult;
  }

  private Map<Integer, AlignmentAndScore> parseJsonToGetAlignments(Collection<Integer> audioIDs,
                                                                   Map<Integer, ISlimResult> audioToResult,
                                                                   Language language) {
    Map<Integer, AlignmentAndScore> idToAlignment = new HashMap<>();
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

  /**
   * TODO : Mark everything as full match for now...
   *
   * @param idToAlignment
   * @param audioID
   * @param cachedResult
   * @param language
   */
  private void getCachedAudioRef(Map<Integer, AlignmentAndScore> idToAlignment, Integer audioID,
                                 ISlimResult cachedResult, Language language) {
    PrecalcScores precalcScores = getPrecalcScores(USE_PHONE_TO_DISPLAY, cachedResult, language);
    Map<ImageType, Map<Float, TranscriptEvent>> typeToTranscriptEvents =
        getTypeToTranscriptEvents(precalcScores.getJsonObject(), USE_PHONE_TO_DISPLAY, language);
    Map<NetPronImageType, List<TranscriptSegment>> typeToSegments = getTypeToSegments(typeToTranscriptEvents, language);
    if (DEBUG)
      logger.info("getCachedAudioRef : cache HIT (" + language + ") for audio id=" + audioID + " returning " + typeToSegments);
    idToAlignment.put(audioID, new AlignmentAndScore(typeToSegments, cachedResult.getPronScore(), true));
  }

  @NotNull
  private PrecalcScores getPrecalcScores(boolean usePhoneToDisplay, ISlimResult cachedResult, Language language) {
    return new PrecalcScores(serverProps, cachedResult,
        usePhoneToDisplay || serverProps.usePhoneToDisplay(language), language);
  }

  private Map<ImageType, Map<Float, TranscriptEvent>> getTypeToTranscriptEvents(JsonObject object,
                                                                                boolean usePhoneToDisplay,
                                                                                Language language) {
    return
        new ParseResultJson(serverProps, language)
            .readFromJSON(object, "words", "w", usePhoneToDisplay, null, false);
  }
}
