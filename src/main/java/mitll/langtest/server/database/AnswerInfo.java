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

package mitll.langtest.server.database;

import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.scoring.AudioContext;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/24/16.
 */
public class AnswerInfo {
//  private static final Logger logger = LogManager.getLogger(AnswerInfo.class);

  private int userid;
  private int id;
  private int projid;
  private int questionID;
  private String answer;
  private String audioFile;
  private boolean valid;
  private AudioType audioType;
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
  private String transcript = "";
  private double snr;
  private String model;

  public int getProjid() {
    return projid;
  }

  public String getModel() {
    return model;
  }

  public static class RecordingInfo {
    String answer;
    String audioFile;
    String deviceType;
    String device;
    boolean withFlash;
    private String transcript = "";

    public RecordingInfo(RecordingInfo other, String audioFile) {
      this.answer = other.answer;
      this.audioFile = audioFile;
      this.deviceType = other.deviceType;
      this.device = other.device;
      this.withFlash = other.withFlash;
      this.transcript = other.transcript;
    }

    public RecordingInfo(String answer,
                         String audioFile,
                         String deviceType,
                         String device,
                         boolean withFlash,
                         String transcript) {
      this.answer = answer;
      this.audioFile = audioFile;
      this.deviceType = deviceType;
      this.device = device;
      this.withFlash = withFlash;
      this.transcript = transcript;
    }

    public String toString() {
      return "RecordingInfo answer " + answer + " audioFile " + audioFile + " device " + deviceType + "/" + device +
          " with flash " + withFlash + " transcript " + getTranscript();
    }

    public String getTranscript() {
      return transcript;
    }

/*    public void setTranscript(String transcript) {
      this.transcript = transcript;
    }*/
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

  /**
   *
   * @param audioContext
   * @param recordingInfo
   * @param validity
   * @param model
   */
  public AnswerInfo(AudioContext audioContext,
                    RecordingInfo recordingInfo,
                    AudioCheck.ValidityAndDur validity,
                    String model
  ) {
    this(audioContext.getUserid(),
        audioContext.getProjid(),
        audioContext.getExid(),
        audioContext.getQuestionID(),

        audioContext.getAudioType(),
        recordingInfo.answer,
        recordingInfo.audioFile,
        recordingInfo.deviceType,
        recordingInfo.device,

        recordingInfo.withFlash, validity, model);
    if (transcript.isEmpty()) {
      transcript = recordingInfo.getTranscript();
    }
  }

  private AnswerInfo(int userid,
                     int projid,
                     int id,
                     int questionID,
                     AudioType audioType,

                     String answer,
                     String audioFile,
                     String deviceType,
                     String device,
                     boolean withFlash,

                     AudioCheck.ValidityAndDur validity,
                     String model) {
    this.userid = userid;
    this.projid = projid;
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
    this.model = model;
  }

  public AnswerInfo(AnswerInfo other,
                    ScoreInfo scoreInfo) {
    this(other, scoreInfo.correct, scoreInfo.pronScore, scoreInfo.scoreJson, scoreInfo.processDur);
  }

  private AnswerInfo(AnswerInfo other,

                     boolean correct,
                     float pronScore,
                     String scoreJson,
                     int processDur) {
    this(other.getUserid(),
        other.getProjid(),
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
        processDur);
    setTranscript(other.getTranscript());
  }

  private AnswerInfo(int userid, int projid, int id, int questionID,
                     AudioType audioType, String answer, String audioFile,
                     String deviceType, String device, boolean withFlash,

                     long durationInMillis,
                     boolean valid,
                     String validity,
                     double snr,

                     boolean correct, float pronScore, String scoreJson, int processDur
                     //  int roundTripDur,
  ) {
    this.userid = userid;
    this.projid = projid;
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

 //   if (answer.isEmpty()) logger.debug("answer is not set?");
  }

  public int getUserid() {
    return userid;
  }

  public int getId() {
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

  public AudioType getAudioType() {
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

  public String getTranscript() {
    return transcript;
  }

  public void setTranscript(String transcript) {
    this.transcript = transcript;
  }

  public String toString() {
    return "answer for exid #" + id +
        " correct " + correct +
        " score " + pronScore +
        " audio type " + audioType +
        " answer " + answer +
        " process " + processDur +
        " validity " + validity +
        " snr " + snr +
        " json " + scoreJson;
  }
}
