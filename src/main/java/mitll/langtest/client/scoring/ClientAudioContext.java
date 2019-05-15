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
    return "ex " + exerciseID +
        "\n\treq " + reqid +
        "\n\tadd " + shouldAddToTable +
        "\n\tdialog  " + dialogSessionID +
        "\n\tsession " + recordingSessionID +
        "\n\ttype    " + audioType;
  }
}
