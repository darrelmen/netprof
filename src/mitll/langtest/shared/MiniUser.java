package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by GO22670 on 4/9/2014.
 */
public class MiniUser implements IsSerializable, Comparable<MiniUser> {
  private long id;
  private int age;
  private int gender;
  private String nativeLang;
  private String dialect;
  private String userID;

  public MiniUser() {
  } // for serialization

  /**
   * @param id
   * @param age
   * @param gender
   * @param userID
   */
  public MiniUser(long id, int age, int gender, String nativeLang, String dialect, String userID) {
    this.id = id;
    this.age = age;
    this.gender = gender;
    this.nativeLang = nativeLang;
    this.dialect = dialect;
    this.userID = userID;
  }

  public boolean isDefault() { return id < 0; }

  /**
   * @see mitll.langtest.server.database.UserDAO#getMiniUsers()
   * @param user
   */
  public MiniUser(User user) {
    this(user.getId(), user.getAge(), user.getGender(), user.getNativeLang(), user.getDialect(), user.getUserID());
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

  public String getNativeLang() {
    return nativeLang;
  }

  public String getDialect() {
    return dialect;
  }

  public String getUserID() {
    return userID;
  }

  public void setUserID(String userID) {
    this.userID = userID;
  }

  public String toString() {
    return "mini-user " + id + " age " + age + " gender " + gender + " native " + nativeLang + " dialect " + dialect;
  }
}