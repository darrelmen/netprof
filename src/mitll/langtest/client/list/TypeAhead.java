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

import java.util.logging.Logger;

/**
 * Created by go22670 on 9/25/14.
 */
public class TypeAhead {
//  private Logger logger = Logger.getLogger("TypeAhead");
  private final SafeUri white = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "white_32x32.png");

  private final TextBox typeAhead = new TextBox();

  /**
   * @param column
   * @param waitCursor
   * @param title
   * @param hasFirstFocus
   * @see mitll.langtest.client.list.PagingExerciseList#addTypeAhead(com.google.gwt.user.client.ui.Panel)
   */
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

  public String getText() {
    return typeAhead.getText();
  }

  public Widget getWidget() {
    return typeAhead;
  }

  /**
   * On key up, do something, like go get a new list given a search term.
   */
  private void makeTypeAhead() {
    typeAhead.setWidth("240px");
    getTypeAhead().getElement().setId("ExerciseList_TypeAhead");

    getTypeAhead().setDirectionEstimator(true);   // automatically detect whether text is RTL
    getTypeAhead().addKeyUpHandler(new KeyUpHandler() {
      public void onKeyUp(KeyUpEvent event) {
        String text = getTypeAhead().getText();
        gotTypeAheadEntry(text);
      }
    });
  }

  /**
   * Subclass please.
   *
   * @param text
   */
  public void gotTypeAheadEntry(String text) {
  }

  private Widget getControlGroup(Image waitCursor, String title) {
    Panel flow = new HorizontalPanel();
    flow.add(getTypeAhead());
    flow.add(waitCursor);
    configureWaitCursor(waitCursor);

    return getControlGroup(title, flow);
  }

  private void configureWaitCursor(Image waitCursor) {
    waitCursor.getElement().getStyle().setMarginTop(-7, Style.Unit.PX);
    waitCursor.setUrl(white);
  }

  /**
   * @param label
   * @param user
   * @return
   * @see mitll.langtest.client.result.ResultManager#populateTable
   */
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

  public void setText(String text) {
    typeAhead.setText(text);
  }
}
