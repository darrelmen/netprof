/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.mail;

import java.util.*;

/**
 * Created by go22670 on 10/26/15.
 */
public class EmailList {
  private static final String DEBUG_EMAIL = "debugEmail";
  private static final String TEST_EMAIL = "testEmail";
  private static final String EMAIL_ADDRESS = "emailAddress";

  private static final String APPROVAL_EMAIL = "approvalEmail";
  private static final String GORDON_VIDAVER = "gordon.vidaver@ll.mit.edu";
  private static final String DOUG_JONES = "daj@ll.mit.edu";
  public static final String RAY_BUDD = "Raymond.Budd@ll.mit.edu";
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

  private static final String TAMAS_1 = "tamas.g.marius.civ@mail.mil";
  private static final String TAMAS_2 = "tamas.marius@dliflc.edu";
  private static final List<String> DLI_EMAILS = Arrays.asList(
      TAMAS_1,
      TAMAS_2,
      "michael.grimmer1@dliflc.edu",
      "sandra.wagner@dliflc.edu",
      GORDON_VIDAVER);


  private static final Set<String> ADMINLIST = new HashSet<String>(Arrays.asList("gvidaver", "tmarius",
      "mgrimmer",
      "swagner", "gmarkovic", "djones", "jmelot", "rbudd", "pgatewood"));

  /**
   * Set this property for non-DLI deployments.
   */
  private static final String REPORT_EMAILS = "reportEmails";

  private List<String> reportEmails = Arrays.asList(TAMAS_1, TAMAS_2, GORDON_VIDAVER, DOUG_JONES, RAY_BUDD);
  private List<String> approvers = DLI_APPROVERS;
  private List<String> approverEmails = DLI_EMAILS;
  private Set<String> admins = ADMINLIST;

  private Properties props;

  public EmailList(Properties props) {
    this.props = props;

    String property = props.getProperty(APPROVERS);
    if (property != null) approvers = Arrays.asList(property.split(","));

    property = props.getProperty(APPROVER_EMAILS);
    if (property != null) approverEmails = Arrays.asList(property.split(","));

    property = props.getProperty(ADMINS);
    if (property != null) admins = new HashSet<String>(Arrays.asList(property.split(",")));

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
   * @see mitll.langtest.server.mail.EmailHelper#enableCDUser(String, String, String)
   */
  public List<String> getApprovers() {
    return approvers;
  }

  public List<String> getApproverEmails() {
    return approverEmails;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.UserDAO#UserDAO
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
