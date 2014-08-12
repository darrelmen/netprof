package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;

/**
 * Object representing a user.
 *
 * UserManager: go22670
 * Date: 5/17/12
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
  private Demographics demographics;
  private float rate = 0.0f;
  private boolean complete;
  private float completePercent;
  private Kind userKind;

  public static enum Kind implements IsSerializable { UNSET, STUDENT, TEACHER, CONTENT_DEVELOPER, ADMIN }

  public static enum Permission implements IsSerializable { QUALITY_CONTROL, RECORD_AUDIO }

  private Collection<Permission> permissions;

  public User() {} // for serialization

  /**
   * @see mitll.langtest.server.database.UserDAO#getUsers()
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
     this(id,age,gender,experience,ipaddr,password, NOT_SET, NOT_SET, NOT_SET, enabled,false, permissions, Kind.STUDENT, "");
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
   */
  public User(long id, int age, int gender, int experience, String ipaddr, String passwordH,
              String nativeLang, String dialect, String userID, boolean enabled, boolean isAdmin,
              Collection<Permission> permissions, Kind userKind, String emailHash) {
    super(id,age,gender,nativeLang,dialect,userID);
    this.experience = experience;
    this.ipaddr = ipaddr;
    this.passwordHash = passwordH;
    this.emailHash = emailHash;
    this.userKind = userKind;
    this.enabled = enabled;
    this.admin = isAdmin;
    this.permissions = permissions;
  }

  public Collection<Permission> getPermissions() {
    return permissions;
  }

  public String getTimestamp() {
	  if (ipaddr == null) return "";
    if (ipaddr.contains("at ")) {
      int i = ipaddr.lastIndexOf("at ")+"at ".length();
      return ipaddr.substring(i);
    }
    else return "";
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

  public Demographics getDemographics() {
    return demographics;
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#joinWithDLIUsers(java.util.List)
   * @param demographics
   */
  public void setDemographics(Demographics demographics) {
    this.demographics = demographics;
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

  public String toString() {
    return "user " + getId() +  "/" + getUserID() +
        " is a " + getGender() + " age " + getAge() +
        " native lang = " + getNativeLang() +
        " dialect " + getDialect() +
        " emailH " + getEmailHash() +
        " passH " + getPasswordHash() +
        " kind " + getUserKind()+
        " demo " + demographics + " perms " + getPermissions();
  }
}
