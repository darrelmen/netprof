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

package mitll.langtest.shared.custom;

import com.google.gwt.user.client.rpc.IsSerializable;

public class QuizSpec implements IsSerializable {
  private int roundMinutes;
  private int minScore;
  private boolean showAudio;
  private boolean isDefault;
  private String accessCode = "";

  public  enum EXERCISETYPES implements IsSerializable { VOCAB, SENTENCES, BOTH }
  private EXERCISETYPES exercisetypes = EXERCISETYPES.VOCAB;

  public QuizSpec() {
    this(10, 30, false, true, "");
  }

  /**
   * @param roundMinutes
   * @param minScore
   * @param showAudio
   * @param accessCode
   * @see mitll.langtest.server.database.custom.UserListManager#getQuizInfo
   */
  public QuizSpec(int roundMinutes, int minScore, boolean showAudio, boolean isDefault, String accessCode) {
    this.roundMinutes = roundMinutes;
    this.minScore = minScore;
    this.showAudio = showAudio;
    this.isDefault = isDefault;
    this.accessCode = accessCode;
  }

  public int getRoundMinutes() {
    return roundMinutes;
  }

  public int getMinScore() {
    return minScore;
  }

  public boolean isShowAudio() {
    return showAudio;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public void setDefault(boolean aDefault) {
    isDefault = aDefault;
  }

  public String getAccessCode() {
    return accessCode;
  }

  public EXERCISETYPES getExercisetypes() {
    return exercisetypes;
  }

  public void setExercisetypes(EXERCISETYPES exercisetypes) {
    this.exercisetypes = exercisetypes;
  }


  public String toString() {
    return "quiz " +
        "\n\tminutes    " + roundMinutes +
        "\n\tminScore   " + minScore +
        "\n\tdefault    " + isDefault +
        "\n\taccessCode " + accessCode +
        "\n\tshowAudio  " + showAudio;
  }
}
