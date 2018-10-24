package mitll.langtest.client.scoring;

import mitll.langtest.shared.answer.AudioType;

public class ClientAudioContext {
  private final int exerciseID;
  private final int reqid;
  private final boolean shouldAddToTable;
  private final int dialogSessionID;

  private final AudioType audioType;

  public ClientAudioContext(int exerciseID,
                     int reqid,
                     boolean shouldAddToTable,
                     AudioType audioType,
                     int dialogSessionID) {
    this.exerciseID = exerciseID;
    this.reqid = reqid;
    this.shouldAddToTable = shouldAddToTable;
    this.audioType = audioType;
    this.dialogSessionID = dialogSessionID;
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

  public AudioType getAudioType() {
    return audioType;
  }
}
