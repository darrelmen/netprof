/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.dialog.IDialog;
import org.jetbrains.annotations.NotNull;

import static com.google.gwt.dom.client.Style.Unit.PX;

public class DialogHeader {
  //private final Logger logger = Logger.getLogger("DialogHeader");
  private static final String HEIGHT = 100 + "px";

  /**
   * @see #getHeader
   */
  private static final int ROW_WIDTH = 98;
  private static final String SPEAK_HINT = "<b>Speak</b> when you see the record icon.";
  private static final String PRESS_AND_HOLD_HINT = "<b>Press and hold</b> when recording.";

  private final INavigation.VIEWS thisView;
  private final INavigation.VIEWS prev;
  private final INavigation.VIEWS next;
  private final ExerciseController controller;
  // private static final int KEY_PRESS_WIDTH = 125;
  private static final int HINT_WIDTH = 250;//350;//300;
  private static final String SPACE_PRESS_AND_HOLD = "<b>Space</b> to record (press and hold.)";

  private String ARROW_KEY_TIP;
  private String ARROW_KEY_TIP_CORE;

  /**
   * @param controller
   * @param thisView
   * @param prev
   * @param next
   * @see mitll.langtest.client.banner.NewContentChooser#showScores(DivWidget, IDialog)
   */
  public DialogHeader(ExerciseController controller, INavigation.VIEWS thisView, INavigation.VIEWS prev, INavigation.VIEWS next) {
    this.controller = controller;
    this.thisView = thisView;
    this.prev = prev;
    this.next = next;

    String s = thisView.isPressAndHold() ? SPACE_PRESS_AND_HOLD : SPEAK_HINT;

    ARROW_KEY_TIP =
        "<i>" +
            "<b>Rehearse</b> the dialog at your own speed.<br/><br/>" +
            s +
            "<br/>" +
            "<b>Arrow keys</b> to advance to next turn or go back." +
            "</i>";

    ARROW_KEY_TIP_CORE =
        "<i>" +
            "<b>Remember</b> the core words.<br/><br/>" +
            s +
            "<br/>" +
            "<b>Arrow keys</b> to advance to next turn or go back." +
            "</i>";

  }

  @NotNull
  public DivWidget getHeader(IDialog dialog) {
    DivWidget outer = new DivWidget();
    outer.getElement().setId("dialogInfo");
    {
      DivWidget row = new DivWidget();
      row.addStyleName("cardBorderShadow");

      row.setWidth(ROW_WIDTH + "%");
      row.addStyleName("inlineFlex");

      if (getPrevView() != null) {
        row.add(getLeftArrow());
      }
      if (getNextView() != null) {
        row.add(getRightArrow());
      }

      {
        com.google.gwt.user.client.ui.Image flag = getFlag(dialog.getImageRef());
        flag.addStyleName("floatLeft");
        row.add(flag);
      }

      DivWidget vert = new DivWidget();
      vert.getElement().setId("vert");
      row.add(vert);
      vert.addStyleName("leftTenMargin");
      vert.addStyleName("rightTenMargin");

      String foreignLanguage = dialog.getForeignLanguage();

      {
        DivWidget titleDiv = new DivWidget();
        titleDiv.addStyleName("titleBlue");
        titleDiv.add(getFLTitle(dialog));
        vert.add(titleDiv);
      }

      String english = dialog.getEnglish();
      String orientation = dialog.getOrientation();

      boolean onlyTwoLines = foreignLanguage.equalsIgnoreCase(english);
      String secondLine = onlyTwoLines ? orientation : english;
      // if (!foreignLanguage.equalsIgnoreCase(english)) {
      {
        DivWidget titleDiv = new DivWidget();
        titleDiv.getElement().getStyle().setBackgroundColor("#dff4fc");
        titleDiv.add(getHeading(5, secondLine));
        vert.add(titleDiv);
      }
      //}

      if (!onlyTwoLines) {
        DivWidget oreintDiv = new DivWidget();
        Heading w1 = new Heading(5, orientation);
        w1.addStyleName("wrapword");

        oreintDiv.add(w1);
        vert.add(oreintDiv);
      }
      outer.add(row);

      addViewHint(row);
    }
    return outer;
  }

  private void addViewHint(DivWidget row) {
    switch (thisView) {
      case STUDY:
        row.add(getHint("<i><b>Study</b> the core words and phrases used in the dialog. " +
            "<br/><b>Record</b> yourself to get ready for rehearsing the dialog. " +
            "<br/>" +
            PRESS_AND_HOLD_HINT +
            "</i>"));
        break;
      case LISTEN:
        row.add(getHint("<i><b>Listen</b> to the reference dialog to prepare to rehearse it.</i>"));
        break;
      case REHEARSE:
        row.add(getHint(ARROW_KEY_TIP));
        break;
      case CORE_REHEARSE:
        row.add(getHint(ARROW_KEY_TIP_CORE));
        break;
      case PERFORM:
        row.add(getHint("<i>Now carry on a natural conversation. " +
            "<br/>" + SPEAK_HINT +
            "<br/>" +

            "</i>"));
        break;
      case PERFORM_PRESS_AND_HOLD:
        row.add(getHint("<i>Now carry on a natural conversation. " +
            "<br/>" + PRESS_AND_HOLD_HINT +
            "<br/>" +

            "</i>"));
        break;
      default:
        break;
    }
  }


  @NotNull
  private Widget getHint(String keyBindings) {
    Widget child = new HTML(keyBindings);
    child.addStyleName("floatRight");
  //  child.addStyleName("leftFiveMargin");
    child.getElement().getStyle().setProperty("marginLeft", "auto");
    child.setWidth(HINT_WIDTH + "px");
    return child;
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
    Heading w1 = new Heading(size, foreignLanguage);
    w1.getElement().getStyle().setMarginTop(0, PX);
    w1.getElement().getStyle().setMarginBottom(5, PX);
    return w1;
  }

  @NotNull
  private Widget getLeftArrow() {
    DivWidget buttonDiv = new DivWidget();
    Button widgets = new Button(getCapitalized(getPrevView().toString().toLowerCase()), IconType.ARROW_LEFT, event -> gotGoBack());
    new TooltipHelper().addTooltip(widgets, getPrevTooltip());

    widgets.addStyleName("leftFiveMargin");
    widgets.addStyleName("rightTenMargin");
    buttonDiv.add(widgets);
    return buttonDiv;
  }

  @NotNull
  private String getCapitalized(String nameForAnswer) {
    return nameForAnswer.substring(0, 1).toUpperCase() + nameForAnswer.substring(1);
  }

  @NotNull
  private Widget getRightArrow() {
    DivWidget buttonDiv = new DivWidget();
    String nameForAnswer = getNextView() == null ? "" : getNextView().toString().toLowerCase();
    Button widgets = new Button(getCapitalized(nameForAnswer), IconType.ARROW_RIGHT, event -> gotGoForward());
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

  private INavigation.VIEWS getPrevView() {
    return prev;
  }

  /**
   * @return null if nothing comes next
   */
  private INavigation.VIEWS getNextView() {
    return next;
  }
}
