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
import mitll.hlt.domino.shared.model.user.DBUser;
import mitll.langtest.client.initial.UILifecycle;
import mitll.langtest.client.user.SignUpForm;
import mitll.langtest.server.database.user.DominoUserDAOImpl;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.shared.project.ProjectStartupInfo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static mitll.langtest.shared.user.Kind.STUDENT;
import static mitll.langtest.shared.user.User.Permission.*;

public class User extends MiniUser implements ReportUser {
  private static final String NOT_SET = "NOT_SET";

  @Deprecated
  private int experience;
  /**
   * @deprecated
   */
  private String passwordHash;
  @Deprecated
  private String nativeLang;
  @Deprecated
  private String dialect;

  private String resetKey ="";

  private String ipaddr;

  private String email = "";
  private boolean enabled;
  private boolean admin;

  private Kind userKind;

  private String device;

  private boolean hasAppPermission;

  private Collection<Permission> permissions;
  private ProjectStartupInfo startupInfo;

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
        return Collections.singletonList(TEACHER_PERM);
      case AUDIO_RECORDER:
        return Collections.singleton(RECORD_AUDIO);
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
   * @return
   * @see mitll.langtest.server.services.MyRemoteServiceServlet#getPermissions
   */
  public boolean isHasAppPermission() {
    return hasAppPermission;
  }

  public void setResetKey(String resetKey) {
    this.resetKey = resetKey;
  }

  /**
   * Closely related to {@link mitll.langtest.client.custom.INavigation.VIEWS}
   */
  public enum Permission implements IsSerializable {
    TEACHER_PERM("View Student Data"),  // gets to see teacher things like student analysis, invite
    QUALITY_CONTROL("Quality Control"), // mark defects, fix defects
    RECORD_AUDIO("Record Audio"),       // record audio
    DEVELOP_CONTENT("Develop Content"), // not sure how different from Record Audio
    PROJECT_ADMIN("Project Admin"),     // make new projects, edit via domino
    /**
     * @see DominoUserDAOImpl#toUser
     */
    POLYGLOT("Polyglot");     // only see polyglot projects

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
   * @see mitll.langtest.server.database.custom.UserListManager#getQCUser
   */
  public User(int id, int age, int gender, Gender realGender, int experience, String ipaddr, String password,
              boolean enabled, Collection<Permission> permissions) {
    this(id, NOT_SET, age, gender, realGender, experience, ipaddr, password, NOT_SET, NOT_SET, enabled, false, permissions,
        STUDENT,
        "",
        "", "", //"",
        System.currentTimeMillis(), "OTHER", true);
  }

  /**
   * @see DominoUserDAOImpl#toUser
   * @param id
   * @param userID
   * @param gender
   * @param realGender
   * @param enabled
   * @param isAdmin
   * @param userKind
   * @param email
   * @param device
   * @param timestamp
   * @param affiliation
   * @param hasAppPermission
   */
  public User(int id,
              String userID,
              int gender,
              Gender realGender,
              boolean enabled, boolean isAdmin,
              Kind userKind,
              String email,
              String device,
              long timestamp,
              String affiliation,
              boolean hasAppPermission) {
    this(id,
        userID, 99,
        gender,
        realGender,
        0,
        "",
        "",
        "",
        "",
        enabled,
        isAdmin,
        Collections.emptyList(),
        userKind,
        email,
        device, "",
        timestamp, affiliation, hasAppPermission
    );
  }

  /**
   * @param id
   * @param userID
   * @param age
   * @param gender
   * @param experience
   * @param ipaddr
   * @param passwordH
   * @param enabled
   * @param isAdmin
   * @param permissions
   * @param userKind
   * @param email
   * @param device
   * @param resetPassKey
   * @param timestamp
   * @param affiliation
   * @param hasAppPermission
   * @see mitll.langtest.server.database.user.DominoUserDAOImpl#toUser
   * @see UserDAO#getUsers
   */
  public User(int id,
              String userID,
              int age,
              int gender,
              Gender realGender,
              int experience,
              String ipaddr,
              String passwordH,
              String nativeLang, String dialect,
              boolean enabled, boolean isAdmin,
              Collection<Permission> permissions, Kind userKind,

              String email,

              String device,
              String resetPassKey,
              long timestamp,
              String affiliation,
              boolean hasAppPermission) {
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
    setLastChecked(timestamp);
//    this.timestamp = timestamp;
    this.affiliation = affiliation;
    this.hasAppPermission = hasAppPermission;
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

  /**
   * @return
   * @see mitll.langtest.client.LangTest#setProjectStartupInfo
   */
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
   * @param permissions
   */
  public void setPermissions(Collection<Permission> permissions) {
    this.permissions = permissions;
  }

  /**
   * @return
   * @see UILifecycle#gotUser
   */
  public boolean isAdmin() {
    return admin;
  }

  @Deprecated
  public int getExperience() {
    return experience;
  }

  @Override
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

  @Override
  public Kind getUserKind() {
    return userKind;
  }

  /**
   * @return
   * @see mitll.langtest.server.services.MyRemoteServiceServlet#getPermissions
   */
  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean hasResetKey() {
    return resetKey != null && !resetKey.isEmpty();
  }

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

  @Override
  public String getDevice() {
    return device;
  }

  public String getEmail() {
    return email;
  }

  /**
   * @param email
   * @see mitll.langtest.server.services.OpenUserServiceImpl#addUser
   */
  public void setEmail(String email) {
    this.email = email;
  }

  public void setDialect(String dialect) {
    this.dialect = dialect;
  }


  public String getFullName() {
    return first != null && !first.isEmpty() || last != null && !last.isEmpty() ? getName() : getUserID();
  }

  public void setAdmin(boolean admin) {
    this.admin = admin;
  }

  /**
   * Two cases-
   * legacy users that are missing first, last, and email
   * new users who become audio recorders, for whom we need more information
   *
   * @return
   */
  public boolean isValid() {
    return isForgivingValid() &&
        isValid(first) &&
        isValid(last) &&

        // must have a gender (and ideally age and dialect) if you want to record audio

        (!(getPermissions().contains(Permission.RECORD_AUDIO) ||
            getPermissions().contains(Permission.DEVELOP_CONTENT)) ||
            getRealGender() != Gender.Unspecified);
  }

  public boolean isForgivingValid() {
    return hasValidEmail() && isValid(getAffiliation());
  }

  public boolean hasValidEmail() {
    return isValidEmailGrammar(email);
  }

  private boolean isValidEmailGrammar(String text) {
    return text != null && !text.isEmpty() && text.trim().toUpperCase().matches("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$");
  }

  public boolean isValid(String email) {
    return email != null && !email.trim().isEmpty();
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
        "\n\tid      " + getID() +
        "\n\tuserid  " + getUserID() +
        (first.isEmpty() ? "" :
            "\n\tfirst   " + first) +
        (last.isEmpty() ? "" :
            "\n\tlast    " + last) +
        "\n\tis a    " + getGender() + "/" + getRealGender() +
        (getAge() < 99 && getAge() > 0 ? "\n\tage     " + getAge() : "") +
        (isAdmin() ?
            "\n\tadmin   " + isAdmin() : "") +
        (!isEnabled() ?
            "\n\tenabled " + isEnabled() : "") +
//        "\n\tdialect " + getDialect() +
//        "\n\temailH " + getEmailHash() +
        (email.isEmpty() ? "" :
            "\n\temail   " + email) +
        (getPasswordHash().isEmpty() ? "" :
            "\n\tpassH   " + getPasswordHash()) +
        (getUserKind() == STUDENT ? "" :
            "\n\tkind    " + getUserKind()) +
        (getPermissions().isEmpty() ? "" :
            "\n\tperms   " + getPermissions()) +
        (getDevice() == null || getDevice().isEmpty() ? "" :
            "\n\tdevice  " + getDevice()) +
        (resetKey == null || resetKey.isEmpty() ? "" :
            "\n\treset   '" + resetKey + "'") +
        (startupInfo == null ? "" :
            "\n\tstartup  " + startupInfo) +
        "\n\thasPermission  " + isHasAppPermission() +
        "\n\taffiliation '" + getAffiliation() + "'"
        ;
  }
}
