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

package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;

/**
 * Object representing a user.
 *
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 5/17/12
 * Time: 3:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class User extends MiniUser {
  public static final String NOT_SET = "NOT_SET";
  private int experience;
  private String ipaddr;
  private String  passwordHash;
  private String  emailHash;
  private boolean enabled;
  private boolean admin;
  private int numResults;
  private float rate = 0.0f;
  private boolean complete;
  private float completePercent;
  private Kind userKind;
  private String nativeLang;
  private String dialect;
  private String device;
  private String resetKey;
  private String cdKey;
  private long timestamp;
  private Collection<Permission> permissions;

  public boolean isTeacher() {
    return getUserKind().equals(Kind.TEACHER);
  }
  public boolean isCD() {
    return getUserKind().equals(Kind.CONTENT_DEVELOPER);
  }

  public enum Kind implements IsSerializable { UNSET, STUDENT, TEACHER, CONTENT_DEVELOPER, ANONYMOUS }
  public enum Permission implements IsSerializable { QUALITY_CONTROL, RECORD_AUDIO, ENABLE_DEVELOPER }

  public User() {} // for serialization

  /**
   * @see mitll.langtest.server.database.custom.UserListManager#getQCUser()
   * @param id
   * @param age
   * @param gender
   * @param experience
   * @param ipaddr
   * @param password
   * @param enabled
   * @param permissions
   */
  public User(long id, int age, int gender, int experience, String ipaddr, String password,
              boolean enabled, Collection<Permission> permissions) {
    this(id, age, gender, experience, ipaddr, password, NOT_SET, NOT_SET, NOT_SET, enabled, false, permissions,
        Kind.STUDENT, "", "", "", "", System.currentTimeMillis());
  }

  /**
   * @see mitll.langtest.server.database.UserDAO#getUsers()
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
   * @param emailHash
   * @param device
   * @param resetPassKey
   * @param cdEnableKey
   * @param timestamp
   */
  public User(long id, int age, int gender, int experience, String ipaddr, String passwordH,
              String nativeLang, String dialect, String userID, boolean enabled, boolean isAdmin,
              Collection<Permission> permissions, Kind userKind, String emailHash, String device, String resetPassKey,
              String cdEnableKey, long timestamp) {
    super(id, age, gender, userID, isAdmin);
    this.experience = experience;
    this.ipaddr = ipaddr;
    this.passwordHash = passwordH;
    this.emailHash = emailHash;
    this.userKind = userKind;
    this.setEnabled(enabled);
    this.admin = isAdmin;
    this.permissions = permissions;
    this.nativeLang = nativeLang;
    this.dialect = dialect;
    this.device = device;
    this.resetKey = resetPassKey;
    this.cdKey = cdEnableKey;
    this.timestamp = timestamp;
  }

  public Collection<Permission> getPermissions() {
    return permissions;
  }
  public long getTimestampMillis() { return timestamp; }

  /**
   *
   * @param timestampMillis
   */
  public void setTimestampMillis(long timestampMillis) {
    this.timestamp = timestampMillis;
  }
  /**
   * @see mitll.langtest.client.user.UserTable#getTable
   * @return
   */
  public int getNumResults() {
    return numResults;
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getUsers
   * @param numResults
   */
  public void setNumResults(int numResults) { this.numResults = numResults; }

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
   * @see mitll.langtest.server.database.DatabaseImpl#getUsers()
   * @param completePercent
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

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getEmailHash() {
    return emailHash;
  }

  public Kind getUserKind() {
    return userKind;
  }

  public boolean isEnabled() {
    return enabled;
  }

  private void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean hasResetKey() {
    return resetKey != null && !resetKey.isEmpty();
  }

  public String getResetKey() {
    return resetKey;
  }

  public String getNativeLang() {
    return nativeLang;
  }

  public String getDialect() {
    return dialect;
  }

  public String getDevice() {
    return device;
  }

  public String toStringShort() {
    return "user " + getId() + "/" + getUserID() +
        " is a " + getGender() + " age " + getAge() +
        " kind " + getUserKind() +
        " perms " + getPermissions();
  }

  public String toString() {
    return "user " + getId() +  "/" + getUserID() +
        " is a " + getGender() + " age " + getAge() +
        " dialect " + getDialect() +
        " emailH " + getEmailHash() +
        " passH " + getPasswordHash() +
        " kind " + getUserKind()+
        " perms " + getPermissions() +
        " device " + getDevice() +
        " reset '" + resetKey + "'" +
        " cdenable '" + cdKey + "'"
        ;
  }
}
