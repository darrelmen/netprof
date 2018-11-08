package mitll.langtest.client.scoring;

import mitll.langtest.shared.answer.AudioType;

public class ClientAudioContext {
  private final int exerciseID;
  private final int reqid;
  private final boolean shouldAddToTable;
  private final int dialogSessionID;
  private final String recordingSessionID;

  private final AudioType audioType;

  /**
   * @see PostAudioRecordButton#startRecording
   * @param exerciseID
   * @param reqid
   * @param shouldAddToTable
   * @param audioType
   * @param dialogSessionID
   * @param recordingSessionID
   */
  ClientAudioContext(int exerciseID,
                     int reqid,
                     boolean shouldAddToTable,
                     AudioType audioType,
                     int dialogSessionID, String recordingSessionID) {
    this.exerciseID = exerciseID;
    this.reqid = reqid;
    this.shouldAddToTable = shouldAddToTable;
    this.audioType = audioType;
    this.dialogSessionID = dialogSessionID;
    this.recordingSessionID = recordingSessionID;
  }

  public int getExerciseID() {
    return exerciseID;
  }

  public int getReqid() {
    return reqid;
  }

  public boolean isShouldAddToTable() {
    return shouldAddToTable;
  }

  public int getDialogSessionID() {
    return dialogSessionID;
  }

  /**
   * For quiz!
   *
   * @return
   */
  public String getRecordingSessionID() {
    return recordingSessionID;
  }

  public AudioType getAudioType() {
    return audioType;
  }
}
