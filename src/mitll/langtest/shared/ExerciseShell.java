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
  protected STATE state = STATE.UNSET;
  protected STATE secondState = STATE.UNSET;

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
  public STATE getState() { return state;  }

  @Override
  public void setState(STATE state) { this.state = state;  }

  @Override
  public STATE getSecondState() {
    return secondState;
  }

  @Override
  public void setSecondState(STATE state) {
     this.secondState = state;
  }

  public CommonShell getShell() { return new ExerciseShell(getID(), getTooltip()); }

  @Override
  public boolean equals(Object other) {
    return other instanceof ExerciseShell && getID().equals(((ExerciseShell)other).getID());
  }

  public String toString() {
    return "Exercise id = " + getID() + "/" + getTooltip() + " states " + getState() + "/" + getSecondState();
  }
}
