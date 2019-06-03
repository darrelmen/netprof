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

package mitll.langtest.server.database;

import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.scoring.AudioContext;

public class AnswerInfo {
  //  private static final Logger logger = LogManager.getLogger(AnswerInfo.class);
  private final int userid;
  private final int id;
  private final int projid;
  private final int questionID;
  private final String answer;
  private final String audioFile;
  private final boolean valid;
  private final AudioType audioType;
  private final long durationInMillis;
  private boolean correct;
  private float pronScore;
  private final String deviceType;
  private final String device;
  private String scoreJson = "";

  private int processDur;
  private final int roundTripDur;
  private final String validity;
  private String transcript = "";
  private String normtranscript = "";
  private final double snr;

  public int getProjid() {
    return projid;
  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#recordInResults(AudioContext, RecordingInfo, AudioCheck.ValidityAndDur, AudioAnswer)
   * @param recoSentence
   */
  public void setNormTranscript(String recoSentence) {
    this.normtranscript = recoSentence;
  }

  public static class RecordingInfo {
    final String answer;
    final String audioFile;
    final String deviceType;
    final String device;
    private String transcript = "";
    private String normtranscript = "";

    public RecordingInfo(RecordingInfo other, String audioFile) {
      this.answer = other.answer;
      this.audioFile = audioFile;
      this.deviceType = other.deviceType;
      this.device = other.device;
      this.transcript = other.transcript;
      this.normtranscript = other.normtranscript;
    }

    public RecordingInfo(String answer,
                         String audioFile,
                         String deviceType,
                         String device,
                         String transcript,
                         String normtranscript) {
      this.answer = answer;
      this.audioFile = audioFile;
      this.deviceType = deviceType;
      this.device = device;
      this.transcript = transcript;
      this.normtranscript = normtranscript;
    }

    public String getTranscript() {
      return transcript;
    }

    String getNormtranscript() {
      return normtranscript;
    }

    public String toString() {
      String ns = getNormtranscript().isEmpty() ? "" : "\n\tnorm       " + getNormtranscript();
      String as = answer.isEmpty() ? "" : "\n\tanswer     " + answer;
      return "RecordingInfo " +
          as +
          "\n\taudioFile  " + audioFile + " device " + deviceType + "/" + device +
          "\n\ttranscript " + getTranscript() +
          ns;
    }
  }

  public static class ScoreInfo {
    final boolean correct;
    final float pronScore;
    final String scoreJson;
    final int processDur;

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

        validity, model);
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
    this.durationInMillis = validity.getDurationInMillis();
    this.deviceType = deviceType;
    this.device = device;

    this.validity = validity.getValidity().name();
    this.roundTripDur = 0;
    this.snr = validity.getDynamicRange();
    //   this.model = model;
  }

  /**
   * @see mitll.langtest.server.audio.AudioFileHelper#recordInResults(AudioContext, RecordingInfo, AudioCheck.ValidityAndDur, AudioAnswer)
   * @param other
   * @param scoreInfo
   * @param model
   */
  public AnswerInfo(AnswerInfo other, ScoreInfo scoreInfo, String model) {
    this(other, scoreInfo.correct, scoreInfo.pronScore, scoreInfo.scoreJson, scoreInfo.processDur);
    // this.model = model;
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
                     String deviceType, String device,

                     long durationInMillis,
                     boolean valid,
                     String validity,
                     double snr,

                     boolean correct, float pronScore, String scoreJson, int processDur
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

  public String getNormTranscript() {
    return normtranscript;
  }

  public void setTranscript(String transcript) {
    this.transcript = transcript;
  }

  public String toString() {
    String s = answer.isEmpty() ? "" : " answer " + answer;
    return "answer for exid #" + id +
        " correct " + correct +
        " score " + pronScore +
        " audio type " + audioType +
        s +
        " process " + processDur +
        " validity " + validity +
        " snr " + snr +
        " json " + scoreJson;
  }
}
