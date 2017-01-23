package mitll.langtest.server.database.audio;

import mitll.langtest.shared.answer.AudioType;

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

  public AudioInfo(int userid,
                   int exerciseID,
                   int projid,
                   AudioType audioType,
                   String audioRef,
                   long timestamp,
                   long durationInMillis,
                   String transcript,
                   float dnr,
                   int resultID) {
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
}
