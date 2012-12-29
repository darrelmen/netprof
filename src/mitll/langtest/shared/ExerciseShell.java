package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/29/12
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExerciseShell implements IsSerializable {
  protected String tooltip;
  protected String id;

  public ExerciseShell() {}
  public ExerciseShell(String id, String tooltip) {
    this.id = id;
    this.tooltip = tooltip;
  }

  public String getID() { return id; }
  public String getTooltip() { return tooltip; }
  @Override
  public boolean equals(Object other) {
    return other instanceof ExerciseShell && id.equals(((ExerciseShell)other).getID());
  }
  public String toString() {
      return "Exercise id = " +id;
  }
}
