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
public class User implements IsSerializable {
  public long id;
  public int age;
  public int gender;
  public int experience;
  public String ipaddr;
  public String  password;
  public long timestamp;
  /**
   * @deprecated avoid COUHES issues
   */
  public String firstName;
  /**
   * @deprecated avoid COUHES issues
   */
  public String lastName;
  public String nativeLang,dialect;
  public String userID;
  public boolean enabled;
  public boolean admin;

  public User() {} // for serialization

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
              String first, String last, String nativeLang, String dialect, String userID, long timestamp, boolean enabled, boolean isAdmin) {
     this.id = id;
    this.age = age;
    this.gender = gender;
    this.experience = experience;
    this.ipaddr = ipaddr;
    this.password = password;
    this.timestamp = timestamp;
    this.firstName = first;
    this.lastName = last;
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

  public String toString() {
    return "user " + id + " age " + age + " gender " + gender +
        " name " + firstName + " " + lastName + " native " + nativeLang + " dialect " + dialect;
  }
}
