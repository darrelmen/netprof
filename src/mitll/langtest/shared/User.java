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

  public User() {} // for serialization
  public User (long id,int age,int gender,int experience, String ipaddr, String password, long timestamp) {
     this.id = id;
    this.age = age;
    this.gender = gender;
    this.experience = experience;
    this.ipaddr = ipaddr;
    this.password = password;
    this.timestamp = timestamp;
  }
}
