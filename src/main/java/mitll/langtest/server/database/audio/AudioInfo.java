package mitll.langtest.server.database.audio;

import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.user.MiniUser;

/**
 * Created by go22670 on 1/23/17.
 */
public class AudioInfo {
  private int userid;
  private int resultID;
  private int exerciseID;
  private int projid;
  private AudioType audioType;
  private String audioRef;
  private long timestamp;
  private long durationInMillis;
  private String transcript;
  private float dnr;
  private int gender;

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
                   MiniUser.Gender realGender) {
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

/*  public AudioInfo(SlickAudio slickAudio, int projid, int exid) {
    this(slickAudio.userid(),
        exid,
        projid,
        AudioType.valueOf(slickAudio.audiotype()),
        slickAudio.audioref(),
        slickAudio.modified().getTime(),
        slickAudio.duration(),
        slickAudio.transcript(),
        slickAudio.dnr(),
        slickAudio.resultid());
  }*/

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
}
