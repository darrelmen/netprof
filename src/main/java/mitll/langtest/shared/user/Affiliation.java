package mitll.langtest.shared.user;

import mitll.langtest.shared.exercise.HasID;

/**
 * Created by go22670 on 1/8/17.
 */
public class Affiliation implements HasID {
  private String abb;
  private String disp;
  private int id;

  public Affiliation() {
  }

  public Affiliation(int id, String abb, String disp) {
    this.id = id;
    this.abb = abb;
    this.disp = disp;
  }
/*
  @Override
  public String getOldID() {
    return null;
  }*/

  @Override
  public int getID() {
    return id;
  }

  @Override
  public int compareTo(HasID o) {
    return Integer.compare(id, o.getID());
  }

  public String getAbb() {
    return abb;
  }

  public String getDisp() {
    return disp;
  }
  public String toString() { return abb; }
}
