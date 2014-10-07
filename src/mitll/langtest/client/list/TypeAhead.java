package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.Controls;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.base.TextBox;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;

/**
 * Created by go22670 on 9/25/14.
 */
public class TypeAhead {
  private TextBox typeAhead = new TextBox();
  private String lastTypeAheadValue = "";

  //private Timer waitTimer = null;
  //private SafeUri animated = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress28.gif");
  private SafeUri white = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "white_32x32.png");

  public TypeAhead(boolean hasFirstFocus) {
    makeTypeAhead();
    checkFocus(hasFirstFocus);
  }

  public TypeAhead(Panel column, Image waitCursor, String title, boolean hasFirstFocus) {
    makeTypeAhead();

    column.add(getControlGroup(waitCursor, title));

    checkFocus(hasFirstFocus);
  }

  private void checkFocus(boolean hasFirstFocus) {
    if (hasFirstFocus) {
      Scheduler.get().scheduleDeferred(new Command() {
        public void execute() {
          getTypeAhead().setFocus(true);
        }
      });
    }
  }

  private void makeTypeAhead() {
    getTypeAhead().getElement().setId("ExerciseList_TypeAhead");
    getTypeAhead().setDirectionEstimator(true);   // automatically detect whether text is RTL
    getTypeAhead().addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        String text = getTypeAhead().getText();
        if (!text.equals(lastTypeAheadValue)) {
          //System.out.println("addTypeAhead : looking for '" + text + "' (" + text.length() + " chars)");
          gotTypeAheadEntry(text);
          lastTypeAheadValue = text;
        }
      }
    });
  }

  private Widget getControlGroup(Image waitCursor, String title) {
    Panel flow = new HorizontalPanel();
    flow.add(getTypeAhead());
    flow.add(waitCursor);
    configureWaitCursor(waitCursor);

    //addControlGroupEntry(column, title, flow);
    return getControlGroup(title, flow);
  }

  public void configureWaitCursor(Image waitCursor) {
    waitCursor.getElement().getStyle().setMarginTop(-7, Style.Unit.PX);
    waitCursor.setUrl(white);
  }

  public void gotTypeAheadEntry(String text) {}

/*  private ControlGroup addControlGroupEntry(Panel dialogBox, String label, Widget user) {
    final ControlGroup userGroup = getControlGroup(label, user);

    dialogBox.add(userGroup);
    return userGroup;
  }*/

  public ControlGroup getControlGroup(String label) {
    return getControlGroup(label, getTypeAhead());
  }

  public static ControlGroup getControlGroup(String label, Widget user) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.addStyleName("leftFiveMargin");

    Controls controls = new Controls();
    userGroup.add(new ControlLabel(label));
    controls.add(user);
    userGroup.add(controls);
    return userGroup;
  }

  public TextBox getTypeAhead() {
    return typeAhead;
  }
}
