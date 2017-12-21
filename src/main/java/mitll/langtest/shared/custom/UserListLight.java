package mitll.langtest.shared.custom;

import mitll.langtest.shared.exercise.HasID;

public class UserListLight implements IUserListLight {
  private int id = -1;
  private String name;

  public UserListLight() {

  }

  public UserListLight(int id, String name) {
    this.id = id;
    this.name = name;
  }

  public int getID() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int compareTo(HasID o) {
    return Integer.compare(getID(), o.getID());
  }
}
