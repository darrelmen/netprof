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
   * @param exerciseID
   * @param reqid
   * @param shouldAddToTable
   * @param audioType
   * @param dialogSessionID
   * @param recordingSessionID
   * @see PostAudioRecordButton#startRecording
   */
  ClientAudioContext(int exerciseID,
                     int reqid,
                     boolean shouldAddToTable,
                     AudioType audioType,
                     int dialogSessionID,
                     String recordingSessionID) {
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

  public String toString() {
    return "ex " + exerciseID + " req " + reqid + " add " + shouldAddToTable + " dialog " + dialogSessionID + " session " + recordingSessionID + " type " + audioType;
  }
}
