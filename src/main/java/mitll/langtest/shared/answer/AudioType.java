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

package mitll.langtest.shared.answer;

import com.google.gwt.user.client.rpc.IsSerializable;

public enum AudioType implements IsSerializable {
  UNSET("unset"),

  REGULAR(Constants.REGULAR1, "", Constants.REGULAR1),
  SLOW("slow", "", "slow"),
  @Deprecated FAST_AND_SLOW("fastAndSlow"),  // really old legacy thing
  PRACTICE("practice"),  // or avp or flashcard
  // FLASHCARD("flashcard"),
  LEARN("learn"),
  TEXT("text"),

  @Deprecated REVIEW("review"),      // maybe only see this in legacy data
  @Deprecated RECORDER("recorder"),  // maybe only see this in legacy data

  CONTEXT_REGULAR("context=regular", "context", Constants.REGULAR1),
  CONTEXT_SLOW("context=slow", "context", "slow");

  private final String text;
  private final String type;
  private final String speed;

  AudioType(final String text) {
    this.text = text;
    type = "";
    speed = "";
  }

  AudioType(final String text, String type, String speed) {
    this.text = text;
    this.type = type;
    this.speed = speed;
  }


  @Override
  public String toString() {
    return text;
  }

  public String getType() {
    return type;
  }

  public String getSpeed() {
    return speed;
  }

  public boolean isRegularSpeed() {
    return speed.equals(Constants.REGULAR1);
  }

  public boolean isSlowSpeed() {
    return speed.equals("slow");
  }

  public boolean isRef() {
    return this==REGULAR || this==SLOW || isContext();
  }

  public boolean isContext() {
    return this == CONTEXT_REGULAR || this == CONTEXT_SLOW;
  }

  private static class Constants {
    static final String REGULAR1 = "regular";
  }
}
