package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/29/12
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExerciseShell implements IsSerializable, CommonShell {
  private String tooltip;
  protected String id;
  protected String state = "unset";

  public ExerciseShell() {}
  public ExerciseShell(String id, String tooltip) {
    this.id = id;
    setTooltip(tooltip);
  }

  public String getID() { return id; }
  public void setID(String id) { this.id = id; }
  public String getTooltip() { return tooltip; }
  public void setTooltip(String tooltip) {
    this.tooltip = tooltip;
    //if (tooltip.isEmpty() && !id.equals("-1")) throw new IllegalArgumentException("tooltip is empty for " + id);
  }

  @Override
  public String getState() { return state;  }

  @Override
  public void setState(String state) { this.state = state;  }

  public CommonShell getShell() { return new ExerciseShell(getID(), getTooltip()); }

  @Override
  public boolean equals(Object other) {
    return other instanceof ExerciseShell && getID().equals(((ExerciseShell)other).getID());
  }

  public String toString() { return "Exercise id = " +id; }
}
