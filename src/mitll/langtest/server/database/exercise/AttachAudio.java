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
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.MutableAudioExercise;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/10/15.
 */
public class AttachAudio {
  private static final Logger logger = Logger.getLogger(AttachAudio.class);
//  private static final String FAST_WAV = "Fast" + ".wav";
//  private static final String SLOW_WAV = "Slow" + ".wav";

  private int missingExerciseCount = 0;
  private int c = 0;
  //  private final int audioOffset;
//private final String mediaDir;
 // private final String mediaDir1;
  //  private final File installPath;
  private Map<Integer, List<AudioAttribute>> exToAudio;
  private String language;

  /**
   * @param exToAudio
   * @param language
   * @see BaseExerciseDAO#setAudioDAO
   */
  AttachAudio(Map<Integer, List<AudioAttribute>> exToAudio,
                     String language) {
    //  this.mediaDir = mediaDir;
    // this.mediaDir1 = mediaDir1;
   // this.mediaDir1 = "";
    //  logger.info("media dir '" + mediaDir + "' '" + mediaDir1 + "'");
    this.language = language;
//    this.installPath = installPath;
    this.setExToAudio(exToAudio);
    //this.audioOffset = audioOffset;
  }


  /**
   * Go looking for audio in the media directory ("bestAudio") and if there's a file there
   * under a matching exercise id, attach Fast and/or slow versions to this exercise.
   * <p>
   * Can override audio file directory with a non-empty refAudioIndex.
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
   * @paramx id
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
      missing = attachAudio(imported, missing, audioAttributes, transcriptChanged, language);
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
   *
   * @param <T>
   * @param exercise
   * @param missing
   * @param audioAttributes
   * @param language
   * @return
   * @see #attachAudio(CommonExercise, Collection)
   */
  private <T extends CommonExercise> int attachAudio(T exercise,
                                                     int missing,
                                                     Collection<AudioAttribute> audioAttributes,
                                                     Collection<Integer> transcriptChangedIDs,
                                                     String language) {
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
          actualPath = ServerProperties.BEST_AUDIO + File.separator +actualPath;
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
          } else if ((audio.matchTranscript(before, transcript) ||
              audio.matchTranscript(noAccents, noAccentsTranscript))) {
            audio.setAudioRef(actualPath);   // remember to prefix the path
            mutableAudio.addAudio(audio);
          } else {
            transcriptChangedIDs.add(audio.getExid());
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
  public void setExToAudio(Map<Integer, List<AudioAttribute>> exToAudio) {
    this.exToAudio = exToAudio;
  }
}
