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

package mitll.langtest.shared.user;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.client.user.UserTable;
import mitll.langtest.client.user.SignUpForm;
import mitll.langtest.server.database.user.DominoUserDAOImpl;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.shared.project.ProjectStartupInfo;

import java.util.*;

import static mitll.langtest.shared.user.User.Kind.STUDENT;
import static mitll.langtest.shared.user.User.Permission.*;

public class User extends MiniUser {
  public static final String NOT_SET = "NOT_SET";
  @Deprecated
  private int experience;
  private String ipaddr;

  /**
   * @deprecated
   */
  private String passwordHash;

  /*
    private String emailHash;
  */
  private String email = "";
  private boolean enabled;
  private boolean admin;
  private int numResults;
  private float rate = 0.0f;
  private boolean complete;
  private float completePercent = 0.0f;
  private Kind userKind;
  @Deprecated
  private String nativeLang;
  private String dialect;
  private String device;
  @Deprecated
  private String resetKey;
  private String affiliation;

  private Collection<Permission> permissions;
  private ProjectStartupInfo startupInfo;

  public enum Kind implements IsSerializable {
    UNSET("Unset", "UST", false),
    INTERNAL("INTERNAL", "INT", false),  // for users we keep to maintain referencial integrity, for instance an importUser

    STUDENT("Student", "STU", true),
    TEACHER("Teacher", "TCHR", true),
    QAQC("QAQC", "QAQC", true), // someone who can edit content
    CONTENT_DEVELOPER("Content Developer", "CDEV", true), // someone who can edit content and record audio
    AUDIO_RECORDER("Audio Recorder", "AREC", true),       // someone who is just an audio recorder
    TEST("Test Account", "TST", true),                   // e.g. for developers at Lincoln or DLI, demo accounts
    SPAM("Spam Account", "SPM", true),                   // for marking nuisance accounts
    PROJECT_ADMIN("Project Admin", "PrAdmin", true),         // invite new users, admin accounts below
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

  /**
   * For right now,  you can only choose to be a student initially.
   *
   * @return
   * @see SignUpForm#getRoles
   */
  public static Collection<User.Kind> getSelfChoiceRoles() {
    return Arrays.asList(STUDENT);
  }

  /**
   * These are the permissions you get when you are invited by program manager or admin
   *
   * @param role
   * @return
   * @see DominoUserDAOImpl#getUserKind
   */
  public static Collection<Permission> getInitialPermsForRole(Kind role) {
    switch (role) {
      case STUDENT:
        return Collections.emptyList();
      case TEACHER:
        return Arrays.asList(
            TEACHER_PERM//,
//            INVITE // other stuedents
        );
      case AUDIO_RECORDER:
        return Collections.singleton(
            RECORD_AUDIO);
      case QAQC:
      case CONTENT_DEVELOPER:
        return Arrays.asList(
            RECORD_AUDIO,
            QUALITY_CONTROL);
      case PROJECT_ADMIN:
      case ADMIN:
        return Arrays.asList(
            TEACHER_PERM,
            RECORD_AUDIO,
            QUALITY_CONTROL
        );
      default:
        return Collections.emptyList();
    }
  }

  /**
   * When you sign up yourself (not invited, you request these permissions).
   * The only roles are student and teacher for self-sign up.
   *
   * @param role
   * @return
   */
/*
  public static Collection<Permission> getRequestedPermsForRole(Kind role) {
    switch (role) {
      case STUDENT:
        return Collections.emptyList();

      case TEACHER:
        return Arrays.asList(TEACHER_PERM);

      default:
        return Collections.emptyList();
    }
  }
*/

  /**
   * These are the set of possible permissions you can have when you are one of these users.
   *
   * @paramx role
   * @return
   */
/*
  public static Collection<Permission> getPossiblePermsForRole(Kind role) {
    switch (role) {
      case STUDENT:
        return Collections.emptyList();
      case TEACHER:
        return Arrays.asList(
            TEACHER_PERM, // allows them to via the full analysis tab, edit students
            RECORD_AUDIO,
            DEVELOP_CONTENT//, // make new projects, edit via domino
            //INVITE,
            //         EDIT_STUDENT//,
            //EDIT_USER
        );  // students
      case AUDIO_RECORDER:
        return Collections.singleton(RECORD_AUDIO);
      case CONTENT_DEVELOPER:
        return Arrays.asList(
            RECORD_AUDIO,
            QUALITY_CONTROL);
      case PROJECT_ADMIN:
        return Arrays.asList(
            TEACHER_PERM,
            RECORD_AUDIO,
            DEVELOP_CONTENT
            //    INVITE,
//            EDIT_STUDENT//,
            //  EDIT_USER
        );
      default:
        return Collections.emptyList();
    }
  }
*/

  public enum Permission implements IsSerializable {
    TEACHER_PERM("View Student Data"), // gets to see teacher things like student analysis, invite
    QUALITY_CONTROL("Quality Control"),
    RECORD_AUDIO("Record Audio"),
    DEVELOP_CONTENT("Develop Content"),
    PROJECT_ADMIN("Project Admin");//? make new projects? edit via domino?

    String name;

    Permission() {
    }

    Permission(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  public User() {
  } // for serialization

  /**
   * @param id
   * @param age
   * @param gender
   * @param experience
   * @param ipaddr
   * @param password
   * @param enabled
   * @param permissions
   * @see mitll.langtest.server.database.custom.UserListManager#getQCUser()
   */
  public User(int id, int age, int gender, Gender realGender, int experience, String ipaddr, String password,
              boolean enabled, Collection<Permission> permissions) {
    this(id, age, gender, realGender, experience, ipaddr, password, NOT_SET, NOT_SET, NOT_SET, enabled, false, permissions,
        STUDENT,
        "",
        "", "", //"",
        System.currentTimeMillis(), "OTHER");
  }

  public User(User copy) {
    this(copy.getID(),
        copy.getAge(),
        copy.getGender(),
        copy.getRealGender(),
        copy.getExperience(),
        copy.getIpaddr(),
        copy.getPasswordHash(),
        copy.getNativeLang(),
        copy.getDialect(),
        copy.getUserID(),
        copy.isEnabled(),
        copy.isAdmin(),
        copy.getPermissions(),
        copy.getUserKind(),
        copy.getEmail(),
        copy.getDevice(),
        copy.getResetKey(),
        copy.getTimestampMillis(),
        copy.getAffiliation());
  }

  /**
   * @param id
   * @param age
   * @param gender
   * @param experience
   * @param ipaddr
   * @param passwordH
   * @param userID
   * @param enabled
   * @param isAdmin
   * @param permissions
   * @param userKind
   * @param email
   * @param device
   * @param resetPassKey
   * @param timestamp
   * @param affiliation
   * @paramx cdEnableKey
   * @seex mitll.langtest.server.database.user.SlickUserDAOImpl#toUser
   * @see mitll.langtest.server.database.user.DominoUserDAOImpl#toUser
   * @see UserDAO#getUsers
   */
  public User(int id,
              int age,
              int gender,
              Gender realGender,
              int experience,
              String ipaddr,
              String passwordH,
              String nativeLang, String dialect,
              String userID, boolean enabled, boolean isAdmin,
              Collection<Permission> permissions, Kind userKind,

              String email,

              String device,
              String resetPassKey,
              //String cdEnableKey,
              long timestamp,//, boolean isActive
              String affiliation) {
    super(id, age, gender == 0, realGender, userID, isAdmin);
    this.experience = experience;
    this.ipaddr = ipaddr;
    if (passwordH == null) passwordH = "";
    this.passwordHash = passwordH;
    this.email = email;
    //  this.emailHash = emailHash;
    this.userKind = userKind;
    this.setEnabled(enabled);
    this.admin = isAdmin;
    this.permissions = permissions;
    this.nativeLang = nativeLang;
    this.dialect = dialect;
    this.device = device;
    this.resetKey = resetPassKey;
    //  this.cdKey = cdEnableKey;
    this.timestamp = timestamp;
    this.affiliation = affiliation;
  }

  public boolean isStudent() {
    return getUserKind().equals(STUDENT);
  }

  public boolean isTeacher() {
    return getUserKind().equals(Kind.TEACHER);
  }

  /**
   * @return
   * @see mitll.langtest.client.user.UserManager#gotNewUser(User)
   */
  public boolean isCD() {
    return getUserKind().equals(Kind.CONTENT_DEVELOPER);
  }

  /**
   * @param startupInfo
   * @see mitll.langtest.server.database.DatabaseImpl#setStartupInfo
   */
  public void setStartupInfo(ProjectStartupInfo startupInfo) {
    this.startupInfo = startupInfo;
  }

  public ProjectStartupInfo getStartupInfo() {
    return startupInfo;
  }

  public String getFirst() {
    return first;
  }

  public void setFirst(String first) {
    this.first = first;
  }

  public String getLast() {
    return last;
  }

  public void setLast(String last) {
    this.last = last;
  }

  public Collection<Permission> getPermissions() {
    return permissions;
  }

  /**
   * @return
   * @see UserTable#getTable
   */
  public int getNumResults() {
    return numResults;
  }

  /**
   * @param numResults
   * @see mitll.langtest.server.database.DatabaseImpl#getUsers
   */
  public void setNumResults(int numResults) {
    this.numResults = numResults;
  }

  /**
   * @return
   * @see mitll.langtest.client.InitialUI#gotUser(User)
   */
  public boolean isAdmin() {
    return admin;
  }

  public void setRate(float rate) {
    this.rate = rate;
  }

  public float getRate() {
    return rate;
  }

  public boolean isComplete() {
    return complete;
  }

  public void setComplete(boolean complete) {
    this.complete = complete;
  }

  public float getCompletePercent() {
    return completePercent;
  }

  /**
   * @param completePercent
   * @see mitll.langtest.server.database.DatabaseImpl#getUsers()
   */
  public void setCompletePercent(float completePercent) {
    this.completePercent = completePercent;
  }

  public int getExperience() {
    return experience;
  }

  public String getIpaddr() {
    return ipaddr;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.copy.UserCopy#addUser
   * @see mitll.langtest.server.database.copy.UserCopy#copyUsers
   */
  public String getPasswordHash() {
    return passwordHash;
  }

  public Kind getUserKind() {
    return userKind;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Deprecated
  public boolean hasResetKey() {
    return resetKey != null && !resetKey.isEmpty();
  }

  @Deprecated
  public String getResetKey() {
    return resetKey;
  }

  @Deprecated
  public String getNativeLang() {
    return nativeLang;
  }

  public String getDialect() {
    return dialect;
  }

  public String getDevice() {
    return device;
  }

  public String getEmail() {
    return email;
  }

  /**
   * @param email
   * @see mitll.langtest.client.userops.EditUserForm#gotSignUp
   */
  public void setEmail(String email) {
    this.email = email;
  }

//  public void setUserKind(Kind userKind) {
//    this.userKind = userKind;
//  }

  public void setDialect(String dialect) {
    this.dialect = dialect;
  }

  public void setPermissions(Collection<Permission> permissions) {
    this.permissions = permissions;
  }

  public String getFullName() {
    return first != null && !first.isEmpty() || last != null && !last.isEmpty() ? first + " " + last : getUserID();
  }

  public void setAdmin(boolean admin) {
    this.admin = admin;
  }

  public String getAffiliation() {
    return affiliation;
  }

  /**
   * Two cases-
   * legacy users that are missing first, last, and email
   * new users who become audio recorders, for whom we need more information
   *
   * @return
   */
  public boolean isValid() {
    boolean hasStandardInfo =
        //   isValid(emailHash) &&
        isValidEmailGrammar(email) &&
            isValid(first) &&
            isValid(last);

    // must have a gender (and ideally age and dialect) if you want to record audio
//    boolean hasOptInfo =
//        (getPermissions().isEmpty() ||
//            ((getPermissions().contains(Permission.RECORD_AUDIO) ||
//                getPermissions().contains(Permission.DEVELOP_CONTENT)) &&
//                getRealGender() != Gender.Unspecified));

    boolean recordInfoSet =
        !(getPermissions().contains(Permission.RECORD_AUDIO) ||
            getPermissions().contains(Permission.DEVELOP_CONTENT)) ||
            getRealGender() != Gender.Unspecified;

    return hasStandardInfo && recordInfoSet;
  }

  private boolean isValidEmailGrammar(String text) {
    return text != null && !text.isEmpty() && text.trim().toUpperCase().matches("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$");
  }

  public boolean isValid(String email) {
    return email != null && !email.isEmpty();
  }

/*  public String toStringShort() {
    return "user " + getID() + "/" + getUserID() +
        "\n\tis a " + getGender() + "/" + getRealGender() +
        "\n\tage  " + getAge() +
        "\n\tkind " + getUserKind() +
        "\n\tperms " + getPermissions();
  }*/

  public String toString() {
    String email = getEmail();
    return "user " +
        "\n\tid     " + getID() +
        "\n\tuserid " + getUserID() +
        (first.isEmpty() ? "" : "\n\tfirst " + first) +
        (last.isEmpty() ? "" : "\n\tlast  " + last) +
        "\n\tis a   " + getGender() + "/" + getRealGender() +
        (getAge() < 99 && getAge() > 0 ? "\n\tage     " + getAge() : "") +
        (isAdmin() ? "\n\tadmin   " + isAdmin() : "") +
        (!isEnabled() ? "\n\tenabled   " + isEnabled() : "") +
//        "\n\tdialect " + getDialect() +
//        "\n\temailH " + getEmailHash() +
        (email.isEmpty() ? "" : "\n\temail " + email) +
        "\n\tpassH  " + getPasswordHash() +
        (getUserKind() == STUDENT ? "" : "\n\tkind   " + getUserKind()) +
        (getPermissions().isEmpty() ? "" :
            "\n\tperms   " + getPermissions()) +
        (getDevice() == null || getDevice().isEmpty() ? "" : "\n\tdevice " + getDevice()) +
        (resetKey == null || resetKey.isEmpty() ? "" : "\n\treset  '" + resetKey + "'") +
        //" cdenable '" + cdKey + "'" +
        (startupInfo == null ? "" : "\n\tstartup  " + startupInfo)
        ;
  }
}
