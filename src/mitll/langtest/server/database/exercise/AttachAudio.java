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

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.MutableAudioExercise;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.Int;


import java.io.File;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/10/15.
 */
public class AttachAudio {
  private static final Logger logger = LogManager.getLogger(AttachAudio.class);
  private int missingExerciseCount = 0;
  private int c = 0;
  private Map<Integer, List<AudioAttribute>> exToAudio;
  private String language;
  private boolean checkAudioTranscript = true;
  private final AudioCheck audioCheck;
  //  private final AudioDAO audioDAO;
  private Map<String, List<AudioAttribute>> transcriptToAudio;
  /**
   * This needs to be consistent with reporting and filtering, let's turn it off for now.
   */
  private boolean useTranscriptToAudio = false;

  /**
   * TODO : is it OK not to do setExToAudio initially???
   *
   * @param exToAudio
   * @param language
   * @see BaseExerciseDAO#setAudioDAO
   */
  AttachAudio(Map<Integer, List<AudioAttribute>> exToAudio,
              String language,
              boolean checkAudioTranscript,
              ServerProperties serverProperties) {
    this.language = language;
    //  this.setExToAudio(exToAudio);
    this.checkAudioTranscript = checkAudioTranscript;
    this.audioCheck = new AudioCheck(serverProperties);
  }


  /**
   * Go looking for audio in the media directory ("bestAudio") and if there's a file there
   * under a matching exercise id, attach Fast and/or slow versions to this exercise.
   * <p>
   * Can override audio file directory with a non-empty refAudioIndex.D
   * <p>
   * Also uses audioOffset - audio index is an integer.
   *
   * @param refAudioIndex override place to look for audio
   * @param imported      to attach audio to
   * @see ExcelImport#getExercise
   */
/*  public <T extends AudioExercise> void addOldSchoolAudio(String refAudioIndex, T imported) {
    int id = imported.getID();
    String audioDir = refAudioIndex.length() > 0 ? findBest(refAudioIndex) : "" + id;
    if (audioOffset != 0) {
      audioDir = "" + (Integer.parseInt(audioDir.trim()) + audioOffset);
    }

    String parentPath = mediaDir + File.separator + audioDir + File.separator;
    String fastAudioRef = parentPath + FAST_WAV;
    String slowAudioRef = parentPath + SLOW_WAV;

    File test = new File(fastAudioRef);
    boolean exists = test.exists();
    if (!exists) {
      test = new File(installPath, fastAudioRef);
      exists = test.exists();
    }
    if (exists) {
      imported.addAudioForUser(ensureForwardSlashes(fastAudioRef), BaseUserDAO.DEFAULT_USER);
    }

    test = new File(slowAudioRef);
    exists = test.exists();
    if (!exists) {
      test = new File(installPath, slowAudioRef);
      exists = test.exists();
    }
    if (exists) {
      imported.addAudio(new AudioAttribute(ensureForwardSlashes(slowAudioRef), BaseUserDAO.DEFAULT_USER).markSlow());
    }
  }*/

  /**
   * Make sure every audio file we attach is a valid audio file -- it's really where it says it's supposed to be.
   * <p>
   * TODOx : rationalize media path -- don't force hack on bestAudio replacement
   * Why does it sometimes have the config dir on the front?
   *
   * @param imported
   * @param transcriptChanged
   * @see ExcelImport#attachAudio
   * @see ExcelImport#getRawExercises()
   */
  public <T extends CommonExercise> int attachAudio(T imported, Collection<Integer> transcriptChanged) {
    int id = imported.getID();
    int missing = 0;
//    logger.info("Attach audio to " + imported);
    if (exToAudio.containsKey(id) /*|| exToAudio.containsKey(id + "/1") || exToAudio.containsKey(id + "/2")*/) {
      List<AudioAttribute> audioAttributes = exToAudio.get(id);
      //   if (audioAttributes.isEmpty()) logger.info("huh? audio attr empty for " + id);
      Set<Integer> changedIDs = new HashSet<>();
      missing = attachAudio(imported, missing, audioAttributes, transcriptChanged, changedIDs);
      if (useTranscriptToAudio && changedIDs.size() > 0) {
        //logger.info("no matches for " + id);
        String transcript = imported.getForeignLanguage();
        if (transcriptToAudio.containsKey(transcript)) {
          //logger.info(language + "1) using transcript->audio map for " + exid + " : " + id);
          //   if (audioAttributes.isEmpty()) logger.info("huh? audio attr empty for " + id);
          missing = attachAudio(imported, missing, transcriptToAudio.get(transcript), transcriptChanged, new HashSet<>());
        }
//        else if (audioAttributes != null && !audioAttributes.isEmpty()) {
        //         logger.info("no match for '" + id + "'");
        //      }
      }
    } else if (exToAudio.isEmpty()) {
//      logger.error("ex->audio map is empty!\n\n\n");
    } else if (useTranscriptToAudio) {
      String transcript = imported.getForeignLanguage();
      if (transcriptToAudio.containsKey(transcript)) {
        logger.info(language + " 2) using transcript->audio map for : " + transcript);
        //   if (audioAttributes.isEmpty()) logger.info("huh? audio attr empty for " + id);
        missing = attachAudio(imported, missing, transcriptToAudio.get(transcript), transcriptChanged, new HashSet<>());
      }
    }
    return missing;
  }

  //int spew = 0;
  private int totalMissingContext = 0;

  /**
   * try to fix the audio path
   * <p>
   * We get the actual path from what's set in the database.
   * Only the hydra server can see the actual file to see if it's there.
   * We communicate that via the actual path field in the database.
   * Don't attach audio that doesn't meet the dynamic range minimum.
   *
   * @param <T>
   * @param exercise
   * @param missing
   * @param audioAttributes
   * @return
   * @paramx language
   * @see #attachAudio(CommonExercise, Collection)
   */
  private <T extends CommonExercise> int attachAudio(T exercise,
                                                     int missing,
                                                     Collection<AudioAttribute> audioAttributes,
                                                     Collection<Integer> transcriptChangedIDs,
                                                     Set<Integer> changedIDs) {
    MutableAudioExercise mutableAudio = exercise.getMutableAudio();

    boolean debug = false;//exercise.getID() == 17375;//exercise.getID() == 23125;

    if (audioAttributes == null) {
      missingExerciseCount++;
      if (missingExerciseCount < 10 || debug) {
        logger.error("attachAudio can't find " + exercise.getID());
      }
    } else if (!audioAttributes.isEmpty()) {
      Set<String> previouslyAttachedAudio = new HashSet<>();
      for (AudioAttribute audioAttribute : exercise.getAudioAttributes()) {
        previouslyAttachedAudio.add(audioAttribute.getAudioRef());
      }

      int m = 0;
//      if (debug) {
//        logger.info("attachAudio found " + audioAttributes.size() + " media '" + mediaDir1 + "' attr for " + exercise);
//      }

      for (AudioAttribute audio : audioAttributes) {
// so this is something like bestAudio/spanish/bestAudio/3742/regular_xxx.ogg
        String actualPath = audio.getActualPath();
        if (!actualPath.startsWith(ServerProperties.BEST_AUDIO)) {
          actualPath = ServerProperties.BEST_AUDIO + File.separator + actualPath;
        }
        String before = exercise.getForeignLanguage();
        String noAccents = StringUtils.stripAccents(before);
        String transcript = audio.getTranscript();
        String noAccentsTranscript = transcript == null ? null : StringUtils.stripAccents(transcript);

        if (!previouslyAttachedAudio.contains(actualPath)) {
          if (audio.isContextAudio()) {
            //logger.info("attachAudio for " + exercise.getID() + " found " + audio);
            Collection<CommonExercise> directlyRelated = exercise.getDirectlyRelated();
            if (directlyRelated.isEmpty()) {
              if (m++ < 50) {
                logger.warn(language + " : no context exercise on " + exercise);
              }
              if (totalMissingContext++ % 100 == 0) {
                logger.warn(language + " (total = " + totalMissingContext + ") no context exercise on " + exercise);
              }
            } else {
              // TODO : not sure why this is needed - wouldn't we do this on context exercises?
              // TODO : why only if there's one context exercise???
              if (directlyRelated.size() == 1) {
                audio.setAudioRef(actualPath);   // remember to prefix the path
                directlyRelated.iterator().next().getMutableAudio().addAudio(audio);
              } else if (!directlyRelated.isEmpty()) {
                logger.info("got more than 1 directly related ? " + directlyRelated.size());
              }
            }
          } else if (!checkAudioTranscript ||
              (audio.matchTranscript(before, transcript) ||
                  audio.matchTranscript(noAccents, noAccentsTranscript))) {
            addIfDNRAboveThreshold(mutableAudio, audio, actualPath);
          } else {
            transcriptChangedIDs.add(audio.getExid());
            changedIDs.add(audio.getExid());
/*							if (m++ < 10) {
                logger.warn("for " + exercise + " audio transcript " + audio.getTranscript() +
										" doesn't match : '" + removePunct(audio.getTranscript()) + "' vs '" + removePunct(exercise.getForeignLanguage()) + "'");
							}*/
          }
          previouslyAttachedAudio.add(actualPath);
//            logger.debug("exercise " +exercise.getOldID()+ " now " + exercise.getAudioAttributes());
        } else {
          logger.debug("skipping " + actualPath);
        }
/*        } else {
          missing++;
          c++;
          if (c < 5) {
            logger.warn("attachAudio file " + test.getAbsolutePath() + " does not exist - \t" + audioRef);
//            if (c < 2) {
//              logger.warn("installPath " + installPath + "mediaDir " + mediaDir + " mediaDir1 " + mediaDir1);
//            }
          }
        }*/
      }
    }
    return missing;
  }

  private void addIfDNRAboveThreshold(MutableAudioExercise mutableAudio, AudioAttribute audio, String child) {
    float dnr1 = audio.getDnr();
    boolean dnrOK = dnr1 < 0 || dnr1 > audioCheck.getMinDNR();

    if (dnrOK) {
      audio.setAudioRef(child);   // remember to prefix the path
      mutableAudio.addAudio(audio);
    } else {
//      logger.debug("attachAudio skipping audio file with low dynamic range " + test);
    }
  }

  /**
   * Assumes audio index field looks like : 11109 8723 8722 8721
   *
   * @return
   * @paramx refAudioIndex
   */
/*  private String findBest(String refAudioIndex) {
    String[] split = refAudioIndex.split("\\s+");
    return (split.length == 0) ? "" : split[0];
  }
  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }*/
  public void setExToAudio(Map<Integer, List<AudioAttribute>> exToAudio, Set<String> multiPron) {
    this.exToAudio = exToAudio;
    this.transcriptToAudio = new HashMap<>();

    logger.info("setExToAudio found " + multiPron.size() + " items with differing english translations");
    for (List<AudioAttribute> audioAttributes : exToAudio.values()) {
      for (AudioAttribute audioAttribute : audioAttributes) {
        String transcript = audioAttribute.getTranscript();
        boolean hasTranscript = transcript != null && !transcript.isEmpty();
        if (hasTranscript) {
          if (multiPron.contains(transcript)) {
//            logger.warn("setExToAudio transcript " + transcript + " has multiple translations");
          } else {
//            transcript = transcript.toLowerCase();
            List<AudioAttribute> audioAttributes1 = transcriptToAudio.get(transcript);
            if (audioAttributes1 == null) {
              transcriptToAudio.put(transcript, audioAttributes1 = new ArrayList<AudioAttribute>());
            }
            audioAttributes1.add(audioAttribute);
          }
        }

      }
    }
  }
}
