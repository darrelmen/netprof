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

package mitll.langtest.server.database.audio;

import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.user.MiniUser;

/**
 * Created by go22670 on 1/23/17.
 */
public class AudioInfo {
  private final int userid;
  private final int resultID;
  private final int exerciseID;
  private final int projid;
  private final AudioType audioType;
  private final String audioRef;
  private final long timestamp;
  private final long durationInMillis;
  private final String transcript;
  private final float dnr;
  private final int gender;
  private final boolean hasProjectSpecificAudio;

  /**
   * @see BaseAudioDAO#addOrUpdateUser(int, int, AudioAttribute)
   * @param userid
   * @param exerciseID
   * @param projid
   * @param audioType
   * @param audioRef
   * @param timestamp
   * @param durationInMillis
   * @param transcript
   * @param dnr
   * @param resultID
   * @param realGender
   * @param hasProjectSpecificAudio
   */
  public AudioInfo(int userid,
                   int exerciseID,
                   int projid,
                   AudioType audioType,
                   String audioRef,
                   long timestamp,
                   long durationInMillis,
                   String transcript,
                   float dnr,
                   int resultID,
                   MiniUser.Gender realGender, boolean hasProjectSpecificAudio) {
    this.userid = userid;
    this.exerciseID = exerciseID;
    this.projid = projid;
    this.audioType = audioType;
    this.audioRef = audioRef;
    this.timestamp = timestamp;
    this.durationInMillis = durationInMillis;
    this.transcript = transcript;
    this.dnr = dnr;
    this.resultID = resultID;
    this.hasProjectSpecificAudio = hasProjectSpecificAudio;

    int gender;
    switch (realGender) {
      case Male:
        gender = 0;
        break;
      case Female:
        gender = 1;
        break;
      default:
        gender = 2;
    }

    this.gender = gender;
  }

  public int getUserid() {
    return userid;
  }

  public int getResultID() {
    return resultID;
  }

  public int getExerciseID() {
    return exerciseID;
  }

  public int getProjid() {
    return projid;
  }

  public AudioType getAudioType() {
    return audioType;
  }

  public String getAudioRef() {
    return audioRef;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getDurationInMillis() {
    return durationInMillis;
  }

  public String getTranscript() {
    return transcript;
  }

  public float getDnr() {
    return dnr;
  }

  public int getGender() {
    return gender;
  }

  public boolean isHasProjectSpecificAudio() {
    return hasProjectSpecificAudio;
  }
}
