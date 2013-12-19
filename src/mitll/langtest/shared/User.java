package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Object representing a user.
 *
 * UserManager: go22670
 * Date: 5/17/12
 * Time: 3:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class User implements IsSerializable, Comparable<User> {
  public long id;
  public int age;
  public int gender;
  public int experience;
  public String ipaddr;
  public String  password;
  public long timestamp;
  public String nativeLang,dialect;
  public String userID;
  public boolean enabled;
  public boolean admin;
  private int numResults;
  private Demographics demographics;
  private float rate = 0.0f;

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
   */
  public User(long id, int age, int gender, int experience, String ipaddr, String password,
               boolean enabled) {
     this(id,age,gender,experience,ipaddr,password, "NOT_SET","NOT_SET","NOT_SET",0,enabled,false);
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
   */
  public User(long id, int age, int gender, int experience, String ipaddr, String password,
              String nativeLang, String dialect, String userID, long timestamp, boolean enabled, boolean isAdmin) {
     this.id = id;
    this.age = age;
    this.gender = gender;
    this.experience = experience;
    this.ipaddr = ipaddr;
    this.password = password;
    this.timestamp = timestamp;
    this.nativeLang = nativeLang;
    this.dialect = dialect;
    this.userID = userID;
    this.enabled = enabled;
    this.admin = isAdmin;
  }

  public boolean isMale() { return gender == 0; }

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
  public void setNumResults(int numResults) {
    this.numResults = numResults;
  }

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

  @Override
  public int compareTo(User o) {
    return id < o.id ? -1 : id > o.id ? +1 : 0;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof User) && compareTo((User)obj) == 0;
  }

  @Override
  public int hashCode() {
    return new Long(id).hashCode();
  }

  public String toString() {
    return "user " + id + " age " + age + " gender " + gender + " native " + nativeLang + " dialect " + dialect+ " demographics " + demographics;
  }
}
