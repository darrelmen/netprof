package mitll.langtest.server.database;

import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.shared.MonitorResult;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.scoring.AudioContext;

/**
 * Created by go22670 on 2/24/16.
 */
public class AnswerInfo {
  private int userid;
  private String id;
  private int questionID;
  private String answer;
  private String audioFile;
  private boolean valid;
  private String audioType;
  private long durationInMillis;
  private boolean correct;
  private float pronScore;
  private String deviceType;
  private String device;
  private String scoreJson;
  private boolean withFlash;
  private int processDur;
  private int roundTripDur;
  private String validity;
  private double snr;

  public static class RecordingInfo {
    String answer;
    String audioFile;
    String deviceType;
    String device;
    boolean withFlash;

    public RecordingInfo(RecordingInfo other,String audioFile) {
      this.answer = other.answer;
      this.audioFile = audioFile;
      this.deviceType = other.deviceType;
      this.device = other.device;
      this.withFlash = other.withFlash;
    }

    public RecordingInfo(String answer,
                         String audioFile,
                         String deviceType, String device,
                         boolean withFlash) {
      this.answer = answer;
      this.audioFile = audioFile;
      this.deviceType = deviceType;
      this.device = device;
      this.withFlash = withFlash;
    }
  }

  public static class ScoreInfo {
    boolean correct;
    float pronScore;
    String scoreJson;
    int processDur;

    public ScoreInfo(boolean correct,
                     float pronScore,
                     String scoreJson,
                     int processDur) {
      this.correct = correct;
      this.pronScore = pronScore;
      this.scoreJson = scoreJson;
      this.processDur = processDur;
    }
  }

  public AnswerInfo(AudioContext audioContext,
                    RecordingInfo recordingInfo,
                    AudioCheck.ValidityAndDur validity
  ) {
    this(audioContext.getUserid(),
        audioContext.getId(),
        audioContext.getQuestionID(),
        audioContext.getAudioType(),

        recordingInfo.answer,
        recordingInfo.audioFile,
        recordingInfo.deviceType,
        recordingInfo.device,
        recordingInfo.withFlash,

        validity);
  }

  public AnswerInfo(int userid,
                    String id,
                    int questionID,
                    String audioType,

                    String answer,
                    String audioFile,
                    String deviceType, String device,
                    boolean withFlash,

                    AudioCheck.ValidityAndDur validity) {
    this.userid = userid;
    this.id = id;
    this.questionID = questionID;
    this.answer = answer;
    this.audioFile = audioFile;
    this.valid = validity.isValid();
    this.audioType = audioType;
    this.durationInMillis = validity.durationInMillis;
    this.deviceType = deviceType;
    this.device = device;
    this.withFlash = withFlash;
    this.validity = validity.getValidity().name();
    this.roundTripDur = 0;//roundTripDur;
    this.snr = validity.getMaxMinRange();
  }

  public AnswerInfo(AnswerInfo other,
                    ScoreInfo scoreInfo) {
    this(other, scoreInfo.correct, scoreInfo.pronScore, scoreInfo.scoreJson, scoreInfo.processDur);
  }

  public AnswerInfo(AnswerInfo other,

                    boolean correct,
                    float pronScore,
                    String scoreJson,
                    int processDur) {
    this(other.getUserid(),
        other.getId(),
        other.getQuestionID(),
        other.getAudioType(),
        other.getAnswer(),
        other.getAudioFile(),
        other.getDeviceType(),
        other.getDevice(),
        other.isWithFlash(),

        other.getDurationInMillis(),
        other.isValid(),
        other.getValidity(),
        other.getSnr(),

        correct,
        pronScore,
        scoreJson,
        processDur
    );
  }

  public AnswerInfo(int userid, String id, int questionID,
                    String audioType, String answer, String audioFile,
                    String deviceType, String device, boolean withFlash,

                    long durationInMillis,
                    boolean valid,
                    String validity,
                    double snr,

                    boolean correct, float pronScore, String scoreJson, int processDur
                    //  int roundTripDur,
  ) {
    this.userid = userid;
    this.id = id;
    this.questionID = questionID;
    this.answer = answer;
    this.audioFile = audioFile;
    this.valid = valid;
    this.audioType = audioType;
    this.durationInMillis = durationInMillis;
    this.correct = correct;
    this.pronScore = pronScore;
    this.deviceType = deviceType;
    this.device = device;
    this.scoreJson = scoreJson;
    this.withFlash = withFlash;
    this.validity = validity;
    this.processDur = processDur;
    this.roundTripDur = 0;//roundTripDur;
    this.snr = snr;
  }

  public int getUserid() {
    return userid;
  }

  public String getId() {
    return id;
  }

  public int getQuestionID() {
    return questionID;
  }

  public String getAnswer() {
    return answer;
  }

  public String getAudioFile() {
    return audioFile;
  }

  public boolean isValid() {
    return valid;
  }

  public String getAudioType() {
    return audioType;
  }

  public long getDurationInMillis() {
    return durationInMillis;
  }

  public boolean isCorrect() {
    return correct;
  }

  public float getPronScore() {
    return pronScore;
  }

  public String getDeviceType() {
    return deviceType;
  }

  public String getDevice() {
    return device;
  }

  public String getScoreJson() {
    return scoreJson;
  }

  public boolean isWithFlash() {
    return withFlash;
  }

  public int getProcessDur() {
    return processDur;
  }

  public int getRoundTripDur() {
    return roundTripDur;
  }

  public String getValidity() {
    return validity;
  }

  public double getSnr() {
    return snr;
  }

  public String toString() {
    return "answer for exid #" + id + " correct " + correct + " score " + pronScore +
        " audio type " + audioType + " answer " + answer + " process " + processDur +
        " validity " + validity + " snr " + snr +
        " json " + scoreJson;
  }
}
