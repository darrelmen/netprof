package mitll.langtest.shared.user;

import mitll.langtest.shared.exercise.HasID;

public class SimpleUser implements HasID, Comparable<HasID> {
  protected int id;
  protected String userID = "";
  protected String first = "";
  protected String last = "";
  private long lastChecked;

  public SimpleUser() {
  }

  SimpleUser(int id) {
    this.id = id;
  }

  /**
   * @see FirstLastUser
   * @param id
   * @param userid
   * @param first
   * @param last
   * @param lastChecked
   */
  SimpleUser(int id, String userid, String first, String last, long lastChecked) {
    this.id = id;
    this.userID = userid;
    this.first = first;
    this.last = last;
    this.lastChecked = lastChecked;
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

  public long getTimestampMillis() {
    return getLastChecked();
  }

  public long getLastChecked() {
    return lastChecked;
  }

  public void setLastChecked(long lastChecked) {
    this.lastChecked = lastChecked;
  }

  public String getName() {
    return first + " " + last;
  }

  public String toString() {
    return "user " + getID() + " : " + first + " " + last;
  }
}