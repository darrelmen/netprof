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
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 11/10/15.
 */
class AttachAudio {
  private static final Logger logger = Logger.getLogger(AttachAudio.class);
  //private static final String FAST_WAV = "Fast" + ".wav";
  //private static final String SLOW_WAV = "Slow" + ".wav";
  private final AudioCheck audioCheck;

  private int missingExerciseCount = 0;
  private int c = 0;
  //private final int audioOffset;
  private final String /*mediaDir, */mediaDir1;
  private final File installPath;
  private Map<String, List<AudioAttribute>> exToAudio;
  private boolean checkAudioTranscript = true;
  private Map<String, List<AudioAttribute>> transcriptToAudio;

  /**
   * Map<String, List<AudioAttribute>> exToAudio,
   *
   * @param mediaDir1
   * @param installPath
   * @param checkAudioTranscript
   * @see BaseExerciseDAO#setAudioDAO
   */
  AttachAudio(String mediaDir1,
              File installPath,
              boolean checkAudioTranscript,
              ServerProperties serverProperties) {
    this.mediaDir1 = mediaDir1;
    this.installPath = installPath;
    this.checkAudioTranscript = checkAudioTranscript;
    this.audioCheck = new AudioCheck(serverProperties);
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
   * @see BaseExerciseDAO#afterReadingExercises
   */
/*  @Deprecated
  public <T extends AudioExercise> void addOldSchoolAudio(String refAudioIndex, T imported) {
    String audioDir = refAudioIndex.length() > 0 ? findBest(refAudioIndex) : imported.getID();
    if (audioOffset != 0) {
      audioDir = "" + (Integer.parseInt(audioDir.trim()) + audioOffset);
    }

    String parentPath = mediaDir + File.separator + audioDir + File.separator;

    {
      String fastAudioRef = parentPath + FAST_WAV;
      File test = new File(fastAudioRef);
      boolean exists = test.exists();
      if (!exists) {
        test = new File(installPath, fastAudioRef);
        exists = test.exists();
      }
      if (exists) {
        imported.addAudioForUser(ensureForwardSlashes(fastAudioRef), UserDAO.DEFAULT_USER);
      }
    }

    {
      String slowAudioRef = parentPath + SLOW_WAV;
      File test = new File(slowAudioRef);
      boolean exists = test.exists();
      if (!exists) {
        test = new File(installPath, slowAudioRef);
        exists = test.exists();
      }
      if (exists) {
        imported.addAudio(new AudioAttribute(ensureForwardSlashes(slowAudioRef), UserDAO.DEFAULT_USER).markSlow());
      }
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
  public <T extends CommonExercise> int attachAudio(T imported, Collection<String> transcriptChanged) {
    String id = imported.getID();
    int missing = 0;

    if (exToAudio.containsKey(id) || exToAudio.containsKey(id + "/1") || exToAudio.containsKey(id + "/2")) {
      List<AudioAttribute> audioAttributes = exToAudio.get(id);
      //   if (audioAttributes.isEmpty()) logger.info("huh? audio attr empty for " + id);
      missing = attachAudio(imported, missing, audioAttributes, transcriptChanged);
      if (transcriptChanged.size() > 0) {
        //logger.info("no matches for " + id);
        id = imported.getForeignLanguage();
        if (transcriptToAudio.containsKey(id)) {
//          logger.info("1) using transcript->audio map for " + imported.getID() + " : " + id);
          //   if (audioAttributes.isEmpty()) logger.info("huh? audio attr empty for " + id);
          missing = attachAudio(imported, missing, transcriptToAudio.get(id), transcriptChanged);
        }
//        else if (audioAttributes != null && !audioAttributes.isEmpty()) {
 //         logger.info("no match for '" + id + "'");
  //      }
      }
    } else {
      id = imported.getForeignLanguage();
      if (transcriptToAudio.containsKey(id)) {
        List<AudioAttribute> audioAttributes = transcriptToAudio.get(id);
        //       logger.info("2) using transcript->audio map for " + imported.getID() + " : " + id);
        //   if (audioAttributes.isEmpty()) logger.info("huh? audio attr empty for " + id);
        missing = attachAudio(imported, missing, audioAttributes, transcriptChanged);
      }
    }
    return missing;
  }

  /**
   * Don't attach audio that doesn't meet the dynamic range minimum.
   *
   * @param imported
   * @param missing
   * @param audioAttributes
   * @param <T>
   * @return
   * @see #attachAudio(CommonExercise, Collection)
   */
  private <T extends CommonExercise> int attachAudio(T imported,
                                                     int missing,
                                                     Collection<AudioAttribute> audioAttributes,
                                                     Collection<String> transcriptChangedIDs) {
    MutableAudioExercise mutableAudio = imported.getMutableAudio();

    if (audioAttributes == null) {
      missingExerciseCount++;
      if (missingExerciseCount < 10) {
        String id = imported.getID();
        logger.error("attachAudio can't find " + id);
      }
    } else if (!audioAttributes.isEmpty()) {
      Set<String> audioPaths = new HashSet<>();
      for (AudioAttribute audioAttribute : imported.getAudioAttributes()) {
        audioPaths.add(audioAttribute.getAudioRef());
      }

      for (AudioAttribute audio : audioAttributes) {
        String child = mediaDir1 + File.separator + audio.getAudioRef();
        File test = new File(installPath, child);

        boolean exists = test.exists();
        if (!exists) {
          test = new File(installPath, audio.getAudioRef());
          exists = test.exists();
          child = audio.getAudioRef();
        }

        if (exists) {
          if (!audioPaths.contains(child)) {
            String before = imported.getForeignLanguage();
            String noAccents = StringUtils.stripAccents(before);
            String transcript = audio.getTranscript();
            String noAccentsTranscript = transcript == null ? null : StringUtils.stripAccents(transcript);

            if (!checkAudioTranscript ||
                (audio.matchTranscript(before, transcript) ||
                    audio.matchTranscript(noAccents, noAccentsTranscript))
                ) {
              addIfDNRAboveThreshold(mutableAudio, audio, child, test);
            } else {

              transcriptChangedIDs.add(audio.getExid());
//              logger.warn("for " + imported + " audio transcript " + audio.getTranscript() +
//                  " doesn't match : '" + noAccents + "' vs '" + noAccentsTranscript + "'");
            }
            audioPaths.add(child);
//            logger.debug("imported " +imported.getID()+ " now " + imported.getAudioAttributes());
          } else {
            logger.debug("attachAudio skipping audio file already seen = " + child);
          }
        } else {
          missing++;
          c++;
          if (c < 5) {
            logger.warn("attachAudio file " + test.getAbsolutePath() + " does not exist - \t" + audio.getAudioRef());
//            if (c < 2) {
//              logger.warn("installPath " + installPath + "mediaDir " + mediaDir + " mediaDir1 " + mediaDir1);
//            }
          }
        }
      }
    }
    return missing;
  }

  private void addIfDNRAboveThreshold(MutableAudioExercise mutableAudio, AudioAttribute audio, String child, File test) {
    float dnr1 = audio.getDnr();
    boolean dnrOK = dnr1 < 0 || dnr1 > audioCheck.getMinDNR();

    if (dnrOK) {
      audio.setAudioRef(child);   // remember to prefix the path
      mutableAudio.addAudio(audio);
    } else {
      logger.debug("attachAudio skipping audio file with low dynamic range " + test);
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

  /**
   * @param exToAudio
   * @param multiPron
   * @see BaseExerciseDAO#afterReadingExercises
   * @see BaseExerciseDAO#attachAudio
   */
  void setExToAudio(Map<String, List<AudioAttribute>> exToAudio, Set<String> multiPron) {
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
