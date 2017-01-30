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

package mitll.langtest.shared.scoring;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.shared.Result;

/**
 * reqid               request id from the client, so it can potentially throw away out of order responses
 * user                who is answering the question
 * id            exercise within the plan
 * questionID          question within the exercise
 * audioType           regular or fast then slow audio recording
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 2/26/16.
 */
public class AudioContext implements IsSerializable {
  /**
   * request id from the client, so it can potentially throw away out of order responses
   */
  private int reqid;

  /**
   * who is answering the question
   */
  private int userid;

  /**
   * exercise id
   */
  private String id;
  /**
   * question within the exercise
   */
  private int questionID;
  /**
   * regular or fast then slow audio recording
   */
  private String audioType;

  /**
   * @param userid
   * @param id
   * @param questionID
   * @param audioType
   * @see
   */
  public AudioContext(int reqid,
                      int userid,
                      String id,
                      int questionID,
                      String audioType) {
    this.reqid = reqid;
    this.userid = userid;
    this.id = id;
    this.questionID = questionID;
    this.audioType = audioType;
  }

  public AudioContext() {
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

  public int getReqid() {
    return reqid;
  }

  /**
   * @see mitll.langtest.server.LangTestDatabaseImpl#writeAudioFile
   * @return
   */
  public String getAudioType() {
    return audioType;
  }

  public boolean isRecording() {
    return audioType.equals(Result.AUDIO_TYPE_REGULAR) || audioType.equals(Result.AUDIO_TYPE_SLOW) ||
         audioType.startsWith("context");
  }

  public String toString() {
    return "user " + userid + " id " + id + " q " + questionID + " req " + reqid + " type " + audioType;
  }
}
