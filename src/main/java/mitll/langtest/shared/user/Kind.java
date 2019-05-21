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

package mitll.langtest.shared.user;

import com.google.gwt.user.client.rpc.IsSerializable;

public enum Kind implements IsSerializable {
  UNSET("Unset", "UST", false),
  INTERNAL("INTERNAL", "INT", false),  // for users we keep to maintain referential integrity, for instance an importUser

  STUDENT("Student", "STU", true),

  TEACHER("Teacher", "TCHR", true),
  QAQC("QAQC", "QAQC", true), // someone who can edit content
  CONTENT_DEVELOPER("Content Developer", "CDEV", true), // someone who can edit content and record audio
  AUDIO_RECORDER("Audio Recorder", "AREC", true),       // someone who is just an audio recorder

  TEST("Test Account", "TST", true),                   // e.g. for developers at Lincoln or DLI, demo accounts

  SPAM("Spam Account", "SPM", true),                   // for marking nuisance accounts

  PROJECT_ADMIN("Project Admin", "PrAdmin", true),    // invite new users, admin accounts below
  GROUP_ADMIN("Group Admin", "GrAM", true),    // group admins...
  ADMIN("System Admin", "UM", true);                  // invite project admins, closed set determined by server properties

  String name;
  String role;
  boolean show;

  Kind() {
  }

  Kind(String name, String role, boolean show) {
    this.name = name;
    this.role = role;
    this.show = show;
  }

  public String getName() {
    return name;
  }

  public String getRole() {
    return role;
  }

  public boolean shouldShow() {
    return show;
  }
}
