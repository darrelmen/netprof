package mitll.langtest.client.scoring;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.HeadlessPlayAudio;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.scoring.AlignmentOutput;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class AlignmentFetcher {
  private Logger logger = Logger.getLogger("AlignmentFetcher");

  private static final boolean DEBUG = true;

  private final Map<Integer, AlignmentOutput> alignments;
  private final int exerciseID;
  private final ExerciseController controller;
  private final ListInterface<?, ?> listContainer;
  //private ChoicePlayAudioPanel<ClientExercise> playAudio;
  // private ChoicePlayAudioPanel<ClientExercise> contextPlay;

  private HeadlessPlayAudio playAudio;
  private HeadlessPlayAudio contextPlay;
  private int req;
  private AudioChangeListener audioChangeListener, contextChangeListener;

  /**
   *
   * @param exerciseID
   * @param controller
   * @param listContainer
   * @param alignments
   * @param audioChangeListener
   * @param contextChangeListener
   */
  AlignmentFetcher(final int exerciseID,
                   final ExerciseController controller,
                   final ListInterface<?, ?> listContainer,
                   Map<Integer, AlignmentOutput> alignments,
                   AudioChangeListener audioChangeListener,
                   AudioChangeListener contextChangeListener) {
    this.exerciseID = exerciseID;
    this.controller = controller;
    this.listContainer = listContainer;
    this.alignments = alignments;
    this.audioChangeListener = audioChangeListener;
    this.contextChangeListener = contextChangeListener;
  }

  /**
   * @param listener
   * @see mitll.langtest.client.list.FacetExerciseList#getRefAudio
   */
  // @Override
  public void getRefAudio(RefAudioListener listener) {
    AudioAttribute currentAudioAttr = playAudio == null ? null : playAudio.getCurrentAudioAttr();
    int refID = currentAudioAttr == null ? -1 : currentAudioAttr.getUniqueID();

    AudioAttribute contextAudioAttr = contextPlay != null ? contextPlay.getCurrentAudioAttr() : null;
    int contextRefID = contextAudioAttr != null ? contextAudioAttr.getUniqueID() : -1;

    if (DEBUG) logger.info("getRefAudio asking for" +
            "\n\texercise  " + exerciseID +
            "\n\taudio     " + contextAudioAttr// +
//            "\n\talignment " + contextAudioAttr
        //    "\n\tspeed  " + currentAudioAttr.getSpeed() +
        //    "\n\tisMale " + currentAudioAttr.getUser().isMale()
    );
    Set<Integer> req = new HashSet<>();
    if (refID != -1) {
      if (DEBUG) {
        logger.info("getRefAudio asking for" +
                "\n\texercise  " + exerciseID +
                "\n\taudio    #" + refID +
                "\n\talignment " + currentAudioAttr.getAlignmentOutput()
            //    "\n\tspeed  " + currentAudioAttr.getSpeed() +
            //    "\n\tisMale " + currentAudioAttr.getUser().isMale()
        );
      }
      if (addToRequest(currentAudioAttr)) req.add(currentAudioAttr.getUniqueID());

      Set<AudioAttribute> allPossible = playAudio.getAllPossible();
      allPossible.forEach(audioAttribute -> {
        if (addToRequest(audioAttribute)) req.add(audioAttribute.getUniqueID());
      });
    }
    //else {
    //  logger.warning("getRefAudio huh? how can audio id be -1??? " + currentAudioAttr);
    //}

    if (contextRefID != -1) {
      // logger.info("getRefAudio asking for context " + contextRefID);
      if (DEBUG) {
        logger.info("getRefAudio asking for context" +
            "\n\texercise " + exerciseID +
            "\n\taudio #" + contextRefID +
            "\n\tspeed  " + contextAudioAttr.getSpeed() +
            "\n\tisMale " + contextAudioAttr.getUser().isMale()
        );
      }
      if (addToRequest(contextAudioAttr)) {
        req.add(contextRefID);

        if (DEBUG) {
          logger.info("getRefAudio added context" +
              "\n\taudio #" + contextRefID
          );
        }
      }

      Set<AudioAttribute> allPossible = contextPlay.getAllPossible();

      if (DEBUG) {
        logger.info("getRefAudio examining context" +
            "\n\taudio " + allPossible.size()
        );
      }
      allPossible.forEach(audioAttribute -> {
        if (addToRequest(audioAttribute)) {
          req.add(audioAttribute.getUniqueID());
        } else {
          if (DEBUG) {
            logger.info("getRefAudio  context" +
                "\n\taudio " + audioAttribute.getUniqueID() + " " + audioAttribute.getAudioType() + " not added to request."
            );
          }
        }
      });
    } else {
      // logger.warning("getRefAudio no context audio for " + exerciseID + " : has context widget " + (contextPlay != null));
    }

    if (req.isEmpty()) {
      if (DEBUG) {
        logger.info("getRefAudio for " + exerciseID + " already has alignments for audio #" + refID + " = " + alignments.containsKey(refID));
        logger.info("getRefAudio already has alignments for context " + contextRefID + " " + alignments.containsKey(contextRefID));
      }

      //registerSegments(refID, currentAudioAttr, contextRefID, contextAudioAttr);
      listener.refAudioComplete();
      if (listContainer == null || listContainer.isCurrentReq(getReq())) {
        cacheOthers(listener);
      }
    } else {
      ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();

      // threre could be a race where we go to get this after we log out...
      if (projectStartupInfo != null && (listContainer == null || listContainer.isCurrentReq(getReq()))) {
        getAlignments(listener, currentAudioAttr, refID, contextAudioAttr, contextRefID, req, projectStartupInfo.getProjectid());
      }
    }
  }

  /**
   * @param req
   * @see mitll.langtest.client.list.FacetExerciseList#makeExercisePanels
   */
  //@Override
  public void setReq(int req) {
    this.req = req;
  }

  // @Override
  public int getReq() {
    return req;
  }

  /**
   * Is the alignment already known and attached?
   *
   * @param currentAudioAttr
   * @return
   */
  private boolean addToRequest(AudioAttribute currentAudioAttr) {
    int refID = currentAudioAttr.getUniqueID();
    if (alignments.containsKey(refID)) {
      if (DEBUG)
        logger.info("addToRequest found " + refID + " " + currentAudioAttr.getAudioType() + " : " + alignments.get(refID));


      return false;
    } else {
      AlignmentOutput alignmentOutput = currentAudioAttr.getAlignmentOutput();
      if (alignmentOutput == null) {
        if (DEBUG)
          logger.info("addToRequest nope - no alignment for audio " + refID + " " + currentAudioAttr.getAudioType());
        return true;
      } else {
        if (DEBUG)
          logger.info("addToRequest remember audio " + refID + " " + alignmentOutput + " " + currentAudioAttr.getAudioType());
        rememberAlignment(refID, alignmentOutput);
        return false;
      }
    }
  }

  public void rememberAlignment(int refID, AlignmentOutput alignmentOutput) {
    alignments.put(refID, alignmentOutput);
  }

  public AlignmentOutput getAlignment(int refID) {
    return alignments.get(refID);
  }

  private void getAlignments(RefAudioListener listener,
                             AudioAttribute currentAudioAttr,
                             int refID,
                             AudioAttribute contextAudioAttr,
                             int contextRefID,
                             Set<Integer> req,
                             int projectid) {
    if (DEBUG) {
      logger.info("getAlignments asking scoring service for " + req.size() + " : " + req +
          " alignments for " + refID + " and context " + contextRefID);
    }
    final boolean needToShowRef = req.contains(refID);
    final boolean needToShowContextRef = req.contains(contextRefID);

    ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();
    if (projectStartupInfo != null && projectStartupInfo.isHasModel()) {
      controller.getScoringService().getAlignments(
          projectid,
          req,
          new AsyncCallback<Map<Integer, AlignmentOutput>>() {
            @Override
            public void onFailure(Throwable caught) {
              controller.handleNonFatalError("get alignments", caught);
            }

            @Override
            public void onSuccess(Map<Integer, AlignmentOutput> result) {
              alignments.putAll(result);

              if (needToShowRef) {
                audioChangeListener.audioChanged(refID, currentAudioAttr.getDurationInMillis());
              }
              if (needToShowContextRef) {
                //logger.info("registerSegments register " + refID + " context " + contextRefID);
                contextChangeListener.audioChanged(contextRefID, contextAudioAttr.getDurationInMillis());
              }

              cacheOthers(listener);
            }
          });
    }
  }

  /**
   * @param listener
   * @see #getRefAudio(RefAudioListener)
   */
  private void cacheOthers(RefAudioListener listener) {
    Set<Integer> req = getReqAudio();

    if (req.isEmpty()) {
      listener.refAudioComplete();
    } else {

      if (DEBUG)
        logger.info("cacheOthers (" + exerciseID + ") Asking for audio alignments for " + req.size() + " knownAlignments " + alignments.size());
      ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();
      if (projectStartupInfo != null) {
        controller.getScoringService().getAlignments(projectStartupInfo.getProjectid(),
            req, new AsyncCallback<Map<Integer, AlignmentOutput>>() {
              @Override
              public void onFailure(Throwable caught) {
                controller.handleNonFatalError("cacheOthers get alignments", caught);
              }

              @Override
              public void onSuccess(Map<Integer, AlignmentOutput> result) {
                alignments.putAll(result);
                listener.refAudioComplete();
              }
            });
      }
    }
  }

  private ProjectStartupInfo getProjectStartupInfo() {
    return controller.getProjectStartupInfo();
  }

  Set<Integer> getReqAudio() {
    Set<Integer> req = playAudio == null ? new HashSet<>() : new HashSet<>(playAudio.getAllAudioIDs());

//    logger.info("getRefAudio " + req.size() + " audio attrs");
    if (contextPlay != null) {
      req.addAll(contextPlay.getAllAudioIDs());
      //    logger.info("getRefAudio with context  " + req.size() + " audio attrs");
    }
    req.removeAll(alignments.keySet());
    //  logger.info("getRefAudio after removing known " + req.size() + " audio attrs");

    return req;
  }

  public void setPlayAudio(HeadlessPlayAudio playAudio) {
    this.playAudio = playAudio;
  }

  void setContextPlay( PlayAudioPanel contextPlay) {
    this.contextPlay = contextPlay;
  }
}
