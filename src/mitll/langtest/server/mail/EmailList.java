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

package mitll.langtest.server.mail;

import mitll.langtest.server.database.user.UserDAO;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/26/15.
 */
public class EmailList {
  // MIT LL
  private static final String GORDON_VIDAVER = "gordon.vidaver@ll.mit.edu";
  private static final String DOUG_JONES = "daj@ll.mit.edu";
  static final String RAY_BUDD = "Raymond.Budd@ll.mit.edu";

  // DLI -
  private static final String GRIMMER = "michael.grimmer1@dliflc.edu";
  private static final String TAMAS_1 = "tamas.g.marius.civ@mail.mil";
  private static final String TAMAS_2 = "tamas.marius@dliflc.edu";
  private static final String SANDY   = "sandra.wagner@dliflc.edu";

  private static final String DEBUG_EMAIL = "debugEmail";
  private static final String TEST_EMAIL = "testEmail";
  private static final String EMAIL_ADDRESS = "emailAddress";
  private static final String APPROVAL_EMAIL = "approvalEmail";

  private static final String DEFAULT_EMAIL = GORDON_VIDAVER;
  private static final String APPROVERS = "approvers";
  private static final String APPROVER_EMAILS = "approverEmails";
  private static final String ADMINS = "admins";

  private static final List<String> DLI_APPROVERS = Arrays.asList(
      "Tamas",
      "Tamas",
      "Michael",
      "Sandy",
      "Gordon");

  private static final List<String> DLI_EMAILS = Arrays.asList(
      TAMAS_1,
      TAMAS_2,
      GRIMMER,
      SANDY,
      GORDON_VIDAVER);

  private static final Set<String> ADMINLIST = new HashSet<>(Arrays.asList(
      "gvidaver",

      "tmarius",
      "mgrimmer",
      "swagner",
      "gmarkovic",

      "djones",
      "jmelot",
      "rbudd",
      "pgatewood"));

  /**
   * Set this property for non-DLI deployments.
   */
  private static final String REPORT_EMAILS = "reportEmails";

  /**
   * Fix for https://gh.ll.mit.edu/DLI-LTEA/Development/issues/500
   */
  private List<String> reportEmails = Arrays.asList(TAMAS_1, TAMAS_2, GORDON_VIDAVER, DOUG_JONES, RAY_BUDD, GRIMMER);
  private List<String> approvers = DLI_APPROVERS;
  private List<String> approverEmails = DLI_EMAILS;
  private Set<String> admins = ADMINLIST;

  private final Properties props;

  public EmailList(Properties props) {
    this.props = props;

    String property = props.getProperty(APPROVERS);
    if (property != null) approvers = Arrays.asList(property.split(","));

    property = props.getProperty(APPROVER_EMAILS);
    if (property != null) approverEmails = Arrays.asList(property.split(","));

    property = props.getProperty(ADMINS);
    if (property != null) admins = new HashSet<>(Arrays.asList(property.split(",")));

    property = props.getProperty(REPORT_EMAILS);
    if (property != null) {
      if (property.trim().isEmpty()) reportEmails = Collections.emptyList();
      else reportEmails = Arrays.asList(property.split(","));
    }
  }

  public boolean isDebugEMail() {
    return getDefaultFalse(DEBUG_EMAIL);
  }

  public boolean isTestEmail() {
    return getDefaultFalse(TEST_EMAIL);
  }

  /**
   * @return
   * @see mitll.langtest.server.mail.EmailHelper#addContentDeveloper
   * @see EmailHelper#enableCDUser(String, String, String, String)
   */
  public List<String> getApprovers() {
    return approvers;
  }

  public List<String> getApproverEmails() {
    return approverEmails;
  }

  /**
   * @return
   * @see UserDAO#UserDAO
   */
  public Set<String> getAdmins() {
    return admins;
  }

  public List<String> getReportEmails() {
    return reportEmails;
  }

  public String getEmailAddress() {
    return props.getProperty(EMAIL_ADDRESS, DEFAULT_EMAIL);
  }

  public String getApprovalEmailAddress() {
    return props.getProperty(APPROVAL_EMAIL, DEFAULT_EMAIL);
  }

  private boolean getDefaultFalse(String param) {
    return props.getProperty(param, "false").equals("true");
  }
}
