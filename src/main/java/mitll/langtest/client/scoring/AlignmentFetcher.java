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

package mitll.langtest.client.scoring;

import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.HeadlessPlayAudio;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.scoring.AlignmentAndScore;
import mitll.langtest.shared.scoring.AlignmentOutput;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class AlignmentFetcher {
  private final Logger logger = Logger.getLogger("AlignmentFetcher");

  private static final boolean DEBUG = false;

  /**
   *
   */
  private final Map<Integer, AlignmentOutput> alignments;
  private final int exerciseID;
  private final ExerciseController controller;
  private final ListInterface<?, ?> listContainer;

  private HeadlessPlayAudio playAudio;
  private HeadlessPlayAudio contextPlay;
  private int req;
  private final AudioChangeListener audioChangeListener;
  private final AudioChangeListener contextChangeListener;

  /**
   * @param exerciseID
   * @param controller
   * @param listContainer
   * @param alignments
   * @param audioChangeListener
   * @param contextChangeListener
   * @see DialogExercisePanel#DialogExercisePanel
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
  public void getRefAudio(RefAudioListener listener) {
//    AudioAttribute currentAudioAttr = playAudio == null ? null : playAudio.getCurrentAudioAttr();
//    int refID = currentAudioAttr == null ? -1 : currentAudioAttr.getUniqueID();
//
//    AudioAttribute contextAudioAttr = contextPlay != null ? contextPlay.getCurrentAudioAttr() : null;
//    int contextRefID = contextAudioAttr != null ? contextAudioAttr.getUniqueID() : -1;

    Set<Integer> req = getReqAudioIDs();
    int before = req.size();
    Set<Integer> knownIDs = getKnownIDs(req);

    if (DEBUG) {
      logger.info("getRefAudio asking for" +
              "\n\texercise  " + exerciseID + //" : " +
              "\n\tbefore    " + before +
              "\n\tafter     " + req.size()
          // +
          //           "\n\taudio     " + contextAudioAttr// +
//            "\n\talignment " + contextAudioAttr
          //    "\n\tspeed  " + currentAudioAttr.getSpeed() +
          //    "\n\tisMale " + currentAudioAttr.getUser().isMale()
      );
    }

    int refID = getRefID();
    int contextRefID = getContextRefID();
    AudioAttribute currentAudioAttr = playAudio == null ? null : playAudio.getCurrentAudioAttr();
    AudioAttribute contextAudioAttr = contextPlay != null ? contextPlay.getCurrentAudioAttr() : null;

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

      final boolean needToShowRef = knownIDs.contains(refID);
      final boolean needToShowContextRef = knownIDs.contains(contextRefID);

      boolean hasProject = projectStartupInfo != null;

      if (hasProject && (needToShowRef || needToShowContextRef)) {
        tellListenersAboutAlignment(needToShowRef, refID, currentAudioAttr, needToShowContextRef, contextRefID, contextAudioAttr);

        if (needToShowRef) {  // means it was already shown just now
          req.remove(refID);
        }
        if (needToShowContextRef) { // means it was already shown just now
          req.remove(contextRefID);
        }
        if (DEBUG) logger.info("getRefAudio for " + exerciseID + " now " + req.size());
      }
      req.removeAll(knownIDs);
      if (DEBUG) logger.info("getRefAudio for " + exerciseID + " after removing known. now " + req.size());

      // there could be a race where we go to get this after we log out...
      if (hasProject && (listContainer == null || listContainer.isCurrentReq(getReq())) && !req.isEmpty()) {
        getAlignments(listener, currentAudioAttr, refID, contextAudioAttr, contextRefID, req, projectStartupInfo.getProjectid());
      } else if (req.isEmpty()) {
        listener.refAudioComplete();
        if (listContainer == null || listContainer.isCurrentReq(getReq())) {
          cacheOthers(listener);
        }
      }
    }
  }

  private int getContextRefID() {
    AudioAttribute contextAudioAttr = contextPlay != null ? contextPlay.getCurrentAudioAttr() : null;
    return contextAudioAttr != null ? contextAudioAttr.getUniqueID() : -1;
  }

  private int getRefID() {
    AudioAttribute currentAudioAttr = playAudio == null ? null : playAudio.getCurrentAudioAttr();
    return currentAudioAttr == null ? -1 : currentAudioAttr.getUniqueID();
  }

  Set<Integer> getReqAudioIDs() {
    AudioAttribute currentAudioAttr = playAudio == null ? null : playAudio.getCurrentAudioAttr();
    int refID = currentAudioAttr == null ? -1 : currentAudioAttr.getUniqueID();

    AudioAttribute contextAudioAttr = contextPlay != null ? contextPlay.getCurrentAudioAttr() : null;
    int contextRefID = contextAudioAttr != null ? contextAudioAttr.getUniqueID() : -1;
//    if (DEBUG) logger.info("getReqAudioIDs asking for" +
//            "\n\texercise      " + exerciseID +
//            "\n\tcontext audio " + contextAudioAttr// +
////            "\n\talignment " + contextAudioAttr
//        //    "\n\tspeed  " + currentAudioAttr.getSpeed() +
//        //    "\n\tisMale " + currentAudioAttr.getUser().isMale()
//    );
    return getReqAudioIDs(currentAudioAttr, refID, contextAudioAttr, contextRefID);
  }

  @NotNull
  private Set<Integer> getReqAudioIDs(AudioAttribute currentAudioAttr, int refID, AudioAttribute contextAudioAttr, int contextRefID) {
    Set<Integer> req = new HashSet<>();
    if (refID != -1) {
      if (DEBUG) {
        logger.info("getReqAudioIDs asking for" +
                "\n\texercise  " + exerciseID +
                "\n\taudio    #" + refID //+
            //  "\n\talignment " + currentAudioAttr.getAlignmentOutput()
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
        logger.info("getReqAudioIDs asking for context" +
            "\n\texercise " + exerciseID +
            "\n\taudio #" + contextRefID +
            "\n\tspeed  " + contextAudioAttr.getSpeed() +
            "\n\tisMale " + contextAudioAttr.isMale()
        );
      }
      if (addToRequest(contextAudioAttr)) {
        req.add(contextRefID);

        if (DEBUG) {
          logger.info("getReqAudioIDs added context" +
              "\n\taudio #" + contextRefID
          );
        }
      }

      Set<AudioAttribute> allPossible = contextPlay.getAllPossible();

      if (DEBUG) {
        logger.info("getReqAudioIDs examining context" +
            "\n\taudio " + allPossible.size()
        );
      }
      allPossible.forEach(audioAttribute -> {
        if (addToRequest(audioAttribute)) {
          req.add(audioAttribute.getUniqueID());
        } else {
          if (DEBUG) {
            logger.info("getReqAudioIDs  context" +
                "\n\taudio " + audioAttribute.getUniqueID() + " " + audioAttribute.getAudioType() + " not added to request."
            );
          }
        }
      });
    } else {
      // logger.warning("getRefAudio no context audio for " + exerciseID + " : has context widget " + (contextPlay != null));
    }
    return req;
  }

  /**
   * @param req
   * @see mitll.langtest.client.list.FacetExerciseList#makeExercisePanels
   */
  public void setReq(int req) {
    this.req = req;
  }

  public int getReq() {
    return req;
  }

  /**
   * Is the alignment already known and attached?
   *
   * @param currentAudioAttr
   * @return
   * @see #getRefAudio(RefAudioListener)
   */
  private boolean addToRequest(AudioAttribute currentAudioAttr) {
/*    int refID = currentAudioAttr.getUniqueID();
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
    }*/
    return true;
  }

  /**
   * @param refID
   * @param alignmentOutput
   * @see #addToRequest
   */
  void rememberAlignment(int refID, AlignmentOutput alignmentOutput) {
    if (DEBUG) {
      if (alignments.containsKey(refID)) {
        logger.info("rememberAlignment : already has alignment for " + refID);
      }
    }
    alignments.put(refID, alignmentOutput);
  }

  /**
   * @param refID
   * @return
   * @see DialogExercisePanel#audioChanged(int, long)
   */
  public AlignmentOutput getAlignment(int refID) {
    return alignments.get(refID);
  }

  /**
   * TODO : how can we not know what to show? -- how can needToShowContextRef = false???
   *
   * @param listener
   * @param currentAudioAttr
   * @param refID
   * @param contextAudioAttr
   * @param contextRefID
   * @param req
   * @param projectid
   */
  private void getAlignments(RefAudioListener listener,
                             AudioAttribute currentAudioAttr,
                             int refID,
                             AudioAttribute contextAudioAttr,
                             int contextRefID,
                             Set<Integer> req,
                             int projectid) {

    final boolean needToShowRef = req.contains(refID);
    final boolean needToShowContextRef = req.contains(contextRefID);

    if (DEBUG) {
      logger.info("getAlignments asking scoring service for exid " + exerciseID +
          "\n\tfor " + req.size() + " : " + req +
          "\n\talignments for " + refID + " and context " + contextRefID +
          "\n\tneedToShowRef        " + needToShowRef +
          "\n\tneedToShowContextRef " + needToShowContextRef
      );
    }

    ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();
    if (projectStartupInfo != null && projectStartupInfo.isHasModel()) {
      controller.getScoringService().getAlignments(
          projectid,
          req,
          new AsyncCallback<Map<Integer, AlignmentAndScore>>() {
            @Override
            public void onFailure(Throwable caught) {
              controller.handleNonFatalError("get alignments", caught);
            }

            @Override
            public void onSuccess(Map<Integer, AlignmentAndScore> result) {
              if (result == null) {
                logger.warning("no alignments for " + req);
              } else {
                if (DEBUG) {
                  result.forEach((k, v) -> logger.info("getAlignments from server got " + k + " = " + v));
                }
              }

              alignments.putAll(result);

              tellListenersAboutAlignment(needToShowRef, refID, currentAudioAttr, needToShowContextRef, contextRefID, contextAudioAttr);

              cacheOthers(listener);
            }
          });
    }
  }

  private void tellListenersAboutAlignment(boolean needToShowRef, int refID, AudioAttribute currentAudioAttr, boolean needToShowContextRef, int contextRefID, AudioAttribute contextAudioAttr) {
    if (needToShowRef) {
      if (DEBUG) logger.info("tellListenersAboutAlignment 1 register " + refID);
      audioChangeListener.audioChanged(refID, currentAudioAttr.getDurationInMillis());
    }
    if (needToShowContextRef) {
      if (DEBUG) logger.info("tellListenersAboutAlignment 2 register context " + contextRefID);
      contextChangeListener.audioChanged(contextRefID, contextAudioAttr.getDurationInMillis());
    }
  }

  /**
   * Talk to the server to get alignments
   *
   * @param listener
   * @see #getRefAudio(RefAudioListener)
   */
  private void cacheOthers(RefAudioListener listener) {
    Set<Integer> req = getAllReqAudioIDs();

    Set<Integer> knownIDs = getKnownIDs(req);
    if (DEBUG) logger.info("cacheOthers : From " + req.size() + " : known " + knownIDs.size());

    req.removeAll(knownIDs);

    if (req.isEmpty()) {
      listener.refAudioComplete();
    } else {
      if (DEBUG) {
        logger.info("cacheOthers (" + exerciseID + ") Asking for audio alignments for " + req.size() + " knownAlignments " + alignments.size());
      }

      getAndRememberAlignents(listener, req);
    }
  }

  private void getAndRememberAlignents(RefAudioListener listener, Set<Integer> req) {
    ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();
    if (projectStartupInfo != null) {
      controller.getScoringService().getAlignments(projectStartupInfo.getProjectid(),
          req, getOnComplete(listener));
    }
  }

  @NotNull
  private AsyncCallback<Map<Integer, AlignmentAndScore>> getOnComplete(RefAudioListener listener) {
    return new AsyncCallback<Map<Integer, AlignmentAndScore>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getOnComplete get alignments", caught);
      }

      @Override
      public void onSuccess(Map<Integer, AlignmentAndScore> result) {
        if (DEBUG) logger.info("getOnComplete " + result.size() + " : " + result.keySet());

        alignments.putAll(result);
        listener.refAudioComplete();
      }
    };
  }

/*  void getAndRememberCachedAlignents(RefAudioListener listener, Set<Integer> req) {
    Set<Integer> knownIDs = getKnownIDs(req);
    if (DEBUG) logger.info("getAndRememberCachedAlignents : From " + req.size() + " : known " + knownIDs.size());

    req.removeAll(knownIDs);

    if (!req.isEmpty()) {
      ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();
      if (projectStartupInfo != null) {
        controller.getScoringService().getCachedAlignments(projectStartupInfo.getProjectid(),
            req, getOnComplete(listener));
      }
    }
  }*/

  private ProjectStartupInfo getProjectStartupInfo() {
    return controller.getProjectStartupInfo();
  }

  /**
   * @return only return audio ids that haven't been asked for yet.
   * @see #cacheOthers
   */
  Set<Integer> getAllReqAudioIDs() {
    Set<Integer> req = playAudio == null ? new HashSet<>() : new HashSet<>(playAudio.getAllAudioIDs());

//    logger.info("getAllReqAudioIDs " + req.size() + " audio attrs : " +req);
    if (contextPlay != null) {
      req.addAll(contextPlay.getAllAudioIDs());
      //    logger.info("getAllReqAudioIDs with context  " + req.size() + " audio attrs");
    }
    //removeKnownAudioIDs(req);

    return req;
  }

  private Set<Integer> getKnownIDs(Set<Integer> req) {
    //int before = req.size();

    Set<Integer> known = new HashSet<>();

    req.forEach(r -> {
      if (alignments.containsKey(r)) {
        known.add(r);
      }
    });

//    if (!req.isEmpty()) {
//      logger.info("getAllReqAudioIDs before " + before + ", after removing known " + req.size() + " audio attrs");
//    }
    return known;
  }

  public void setPlayAudio(HeadlessPlayAudio playAudio) {
    this.playAudio = playAudio;
  }

  void setContextPlay(PlayAudioPanel contextPlay) {
    this.contextPlay = contextPlay;
  }
}
