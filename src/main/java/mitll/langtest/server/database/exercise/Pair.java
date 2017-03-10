package mitll.langtest.server.database.exercise;

/**
 * Created by go22670 on 3/9/17.
 */
public final class Pair {
  final String type;
  final String section;

  public Pair(String type, String section) {
    this.type = type;
    this.section = section;
  }
  public String toString() { return type + "=" + section; }
}
