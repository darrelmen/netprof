package mitll.langtest.shared.user;

import mitll.langtest.shared.exercise.HasID;

public class FirstLastUser implements HasID, Comparable<HasID> {
  protected int id;
  protected String userID;
  protected String first = "";
  protected String last = "";

  private transient long lastChecked;

  public FirstLastUser() {
  }

  public FirstLastUser(int id) {
    this.id = id;
  }

  public FirstLastUser(int id, String userid, String first, String last, long lastChecked) {
    this.id = id;
    this.userID = userid;
    this.first = first;
    this.last = last;
    this.lastChecked =lastChecked;
  }

  @Override
  public int getID() {
    return id;
  }

  /**
   * First name
   *
   * @return
   */
  public String getFirst() {
    return first;
  }

  /**
   * Last name
   *
   * @return
   */
  public String getLast() {
    return last;
  }

  public void setFirst(String first) {
    this.first = first;
  }

  public void setLast(String last) {
    this.last = last;
  }

  @Override
  public int compareTo(HasID o) {
    return Integer.compare(id, o.getID());
  }

  public String getUserID() {
    return userID;
  }

  public long getLastChecked() {
    return lastChecked;
  }

}
