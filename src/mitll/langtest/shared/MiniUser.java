package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Much of the time the UI doesn't need to know a lot about a user so just send the little it needs.
 *
 * Created by GO22670 on 4/9/2014.
 */
public class MiniUser implements IsSerializable, Comparable<MiniUser> {
  private long id;
  private int age;
  private int gender;
  private String userID;

  public MiniUser() {
  } // for serialization

  /**
   * @param id
   * @param age
   * @param gender
   * @param userID
   */
  public MiniUser(long id, int age, int gender, String userID) {
    this.id = id;
    this.age = age;
    this.gender = gender;
    this.userID = userID;
  }

  public boolean isDefault() { return id < 0; }

  /**
   * @see mitll.langtest.server.database.UserDAO#getMiniUsers()
   * @param user
   */
  public MiniUser(User user) {
    this(user.getId(), user.getAge(), user.getGender(), new String(user.getUserID()));
  }

  public boolean isMale() {
    return gender == 0;
  }

  @Override
  public int compareTo(MiniUser o) {
    return id < o.id ? -1 : id > o.id ? +1 : 0;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof MiniUser) && compareTo((MiniUser)obj) == 0;
  }

  @Override
  public int hashCode() {
    return new Long(id).hashCode();
  }

  public int getGender() {
    return gender;
  }

  public long getId() {
    return id;
  }

  public int getAge() {
    return age;
  }

  public String getUserID() {
    return userID;
  }

  public void setUserID(String userID) {
    this.userID = userID;
  }

  public String toString() {
    return "mini-user " + id + " : " + age + " yr old " + (isMale() ? "male":"female");
  }
}