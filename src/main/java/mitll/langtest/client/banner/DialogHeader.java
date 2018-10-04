package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.dialog.IDialog;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

import static com.google.gwt.dom.client.Style.Unit.PX;

class DialogHeader {
  //private final Logger logger = Logger.getLogger("DialogHeader");
  // private static final int HEADER_HEIGHT = 120;
  private static final String HEIGHT = 100 + "px";

  /**
   * @see #getHeader
   */
//  private static final int ROW_WIDTH = 97;

  private final INavigation.VIEWS prev;
  private final INavigation.VIEWS next;
  private final ExerciseController controller;

  /**
   * @param controller
   * @param prev
   * @param next
   */
  DialogHeader(ExerciseController controller, INavigation.VIEWS prev, INavigation.VIEWS next) {
    this.controller = controller;
    this.prev = prev;
    this.next = next;
  }

  @NotNull
  public DivWidget getHeader(IDialog dialog) {
    DivWidget outer = new DivWidget();
    outer.getElement().setId("dialogInfo");
    {
      DivWidget row = new DivWidget();
      row.addStyleName("cardBorderShadow");

      //  row.setHeight(HEADER_HEIGHT + "px");

     // row.setWidth(ROW_WIDTH + "%");
      row.addStyleName("inlineFlex");

      row.add(getLeftArrow());
      row.add(getRightArrow());

      {
        com.google.gwt.user.client.ui.Image flag = getFlag(dialog.getImageRef());
        flag.addStyleName("floatLeft");
        row.add(flag);
      }

      DivWidget vert = new DivWidget();
      vert.getElement().setId("vert");
      row.add(vert);
      vert.addStyleName("leftTenMargin");

      {
        DivWidget titleDiv = new DivWidget();
        titleDiv.addStyleName("titleBlue");
        titleDiv.add(getFLTitle(dialog));
        vert.add(titleDiv);
      }

      {
        DivWidget titleDiv = new DivWidget();
        titleDiv.getElement().getStyle().setBackgroundColor("#dff4fc");
        titleDiv.add(getHeading(5, dialog.getEnglish()));
        vert.add(titleDiv);
      }

      {
        DivWidget oreintDiv = new DivWidget();
        Heading w1 = new Heading(5, dialog.getOrientation());
        w1.addStyleName("wrapword");

        oreintDiv.add(w1);
        vert.add(oreintDiv);
      }
      outer.add(row);
    }
    return outer;
  }


  @NotNull
  private com.google.gwt.user.client.ui.Image getFlag(String cc) {
    com.google.gwt.user.client.ui.Image image = new com.google.gwt.user.client.ui.Image(cc);
    image.setHeight(HEIGHT);
    image.setWidth(HEIGHT);
    return image;
  }


  @NotNull
  private Heading getFLTitle(IDialog dialog) {
    return getHeading(3, dialog.getForeignLanguage());
  }

  @NotNull
  private Heading getHeading(int size, String foreignLanguage) {
    Heading w1 = new Heading(size, foreignLanguage);//, dialog.getEnglish());
    w1.getElement().getStyle().setMarginTop(0, PX);
    w1.getElement().getStyle().setMarginBottom(5, PX);
    return w1;
  }

  @NotNull
  private Widget getLeftArrow() {
    DivWidget buttonDiv = new DivWidget();
    Button widgets = new Button("", IconType.ARROW_LEFT, event -> gotGoBack());
    new TooltipHelper().addTooltip(widgets, getPrevTooltip());

    widgets.addStyleName("leftFiveMargin");
    widgets.addStyleName("rightTenMargin");
    buttonDiv.add(widgets);
    return buttonDiv;
  }

  @NotNull
  private Widget getRightArrow() {
    DivWidget buttonDiv = new DivWidget();
    Button widgets = new Button("", IconType.ARROW_RIGHT, event -> gotGoForward());
    new TooltipHelper().addTooltip(widgets, getNextTooltip());
    widgets.addStyleName("leftFiveMargin");
    widgets.addStyleName("rightTenMargin");
    buttonDiv.add(widgets);
    widgets.setEnabled(next != null);
    return buttonDiv;
  }

  private void gotGoBack() {
    controller.getNavigation().show(getPrevView());
  }

  private void gotGoForward() {
    controller.getNavigation().show(getNextView());
  }

  private String getPrevTooltip() {
    return "Go back to " + getPrevView().toString();
  }

  private String getNextTooltip() {
    return getNextView() == null ? "" : "Go ahead to " + getNextView().toString();
  }

  @NotNull
  private INavigation.VIEWS getPrevView() {
    return prev;
  }

  @NotNull
  private INavigation.VIEWS getNextView() {
    return next;
  }
}
