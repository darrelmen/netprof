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
  public String  password;
 // private long timestamp;
  public boolean enabled;
  public boolean admin;
  private int numResults;
  private Demographics demographics;
  private float rate = 0.0f;
  private boolean complete;
  private float completePercent;

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
     this(id,age,gender,experience,ipaddr,password, NOT_SET, NOT_SET, NOT_SET,0,enabled,false, permissions);
  }
  /**
   * @see mitll.langtest.server.database.UserDAO#getUsers()
   * @param id
   * @param age
   * @param gender
   * @param experience
   * @param ipaddr
   * @param password
   * @param userID
   * @param timestamp
   * @param enabled
   * @param isAdmin
   * @param permissions
   */
  public User(long id, int age, int gender, int experience, String ipaddr, String password,
              String nativeLang, String dialect, String userID, long timestamp, boolean enabled, boolean isAdmin, Collection<Permission> permissions) {
    super(id,age,gender,nativeLang,dialect,userID);
    this.experience = experience;
    this.ipaddr = ipaddr;
    this.password = password;
  //  this.timestamp = timestamp;
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

/*
  public long getRawTimestamp() { return timestamp; }
*/

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

  public void setCompletePercent(float completePercent) {
    this.completePercent = completePercent;
  }

  public int getExperience() {
    return experience;
  }

  public String getIpaddr() {
    return ipaddr;
  }

  public String toString() {
    return "user " + getId() + " age " + getAge() + " gender " + getGender() + " native " + getNativeLang() + " dialect " + getDialect() + " demographics " + demographics;
  }

}
