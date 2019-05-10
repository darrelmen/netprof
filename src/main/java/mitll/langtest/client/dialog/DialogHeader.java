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
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.project.ProjectMode;
import mitll.langtest.shared.user.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.google.gwt.dom.client.Style.Unit.PX;
import static mitll.langtest.client.custom.INavigation.VIEWS.*;

/**
 * @see ListenViewHelper#addDialogHeader(IDialog, Panel)
 */
public class DialogHeader {
  //private final Logger logger = Logger.getLogger("DialogHeader");
  private static final String HEIGHT = 100 + "px";

  /**
   * @see #getRow()
   */
  private static final int ROW_WIDTH = 98;
  private static final String SPEAK_HINT = "<b>Speak</b> when you see the record icon.";
  private static final String PRESS_AND_HOLD_HINT = "<b>Press and hold</b> when recording.";
  public static final int ARROW_WIDTH = 190;

  private final INavigation.VIEWS thisView;
  private final INavigation.VIEWS prev;
  private final INavigation.VIEWS next;
  private final ExerciseController controller;

  //private static final int HINT_WIDTH = 250;
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
            "<b>Rehearse</b> the dialog at your own speed.<br/>" +
            s +
            "<br/>" +
            "<b>Arrow keys</b> to advance to next turn or go back." +
            "</i>";

    ARROW_KEY_TIP_CORE =
        "<i>" +
            "<b>Remember</b> the core words.<br/>" +
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
      DivWidget row = getRow();

      int midWidth = 50;
      int sides = (100 - midWidth) / 2;
      // add prev view
      if (getPrevView() != null) {
        Widget leftArrow = getLeftArrow();
        leftArrow.setWidth(sides + "%");
        row.add(leftArrow);
      }

      DivWidget middle = new DivWidget();
      row.add(middle);
      middle.setWidth(75 + "%");

      // add image
      {
        com.google.gwt.user.client.ui.Image image = getImage(dialog.getImageRef());
        image.addStyleName("floatLeft");
        image.addStyleName("rightFiveMargin");
        middle.add(image);
      }

      middle.add(getDialogLabels(dialog));

      //add next view arrow
      if (getNextView() != null) {
        Widget rightArrow = getRightArrow();
        rightArrow.setWidth(sides + "%");
        row.add(rightArrow);
      }

      outer.add(row);
    }
    return outer;
  }

//  private void alignCenter(DivWidget rowOne) {
//    rowOne.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
//  }

  @NotNull
  private DivWidget getDialogLabels(IDialog dialog) {
    DivWidget vert = new DivWidget();
    vert.getElement().setId("vert");
    vert.addStyleName("leftTenMargin");
    vert.addStyleName("rightTenMargin");

    {
      DivWidget titleDiv = new DivWidget();
      titleDiv.addStyleName("titleBlue");
      titleDiv.add(getFLTitle(dialog));
      vert.add(titleDiv);
    }

    String english = dialog.getEnglish();
    String orientation = dialog.getOrientation();

    boolean onlyTwoLines = dialog.getForeignLanguage().equalsIgnoreCase(english);
    String secondLine = onlyTwoLines ? orientation : english;

    {
      DivWidget titleDiv = new DivWidget();
      titleDiv.getElement().getStyle().setBackgroundColor("#dff4fc");
      titleDiv.add(getHeading(5, secondLine));
      vert.add(titleDiv);
    }

    if (!onlyTwoLines) {
      DivWidget oreintDiv = new DivWidget();
      Heading w1 = new Heading(5, orientation);
      w1.addStyleName("wrapword");

      oreintDiv.add(w1);
      vert.add(oreintDiv);
    }
    addViewHint(vert);

    return vert;
  }

  @NotNull
  private DivWidget getRow() {
    DivWidget row = new DivWidget();
    row.addStyleName("cardBorderShadow");

    setRowWidth(row);
    row.addStyleName("inlineFlex");

    setMinWidth(row, 850);
    return row;
  }

  protected void setRowWidth(DivWidget row) {
    row.setWidth(ROW_WIDTH + "%");
  }

  private void setMinWidth(DivWidget row, int i) {
    row.getElement().getStyle().setProperty("minWidth", i + "px");
  }

  private void addViewHint(DivWidget row) {
    switch (thisView) {
//      case STUDY:
//        row.add(getHint("<i><b>Study</b> the core words and phrases used in the dialog. " +
//            "<br/><b>Record</b> yourself to get ready for rehearsing the dialog. " +
//            "<br/>" +
//            PRESS_AND_HOLD_HINT +
//            "</i>"));
//        break;
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
      case TURN_EDITOR:
        row.add(getHint("<i><b>Edit</b> the text of the turns. " +
            "<br/><b>Record</b> audio for each turn. " +
            "<br/>" +
            PRESS_AND_HOLD_HINT +
            "</i>"));
        break;
      default:
        break;
    }
  }


  @NotNull
  private Widget getHint(String keyBindings) {
    Widget child = new HTML(keyBindings);

    child.addStyleName("cardBorderShadow");
    child.addStyleName("floatRight");
    //  child.addStyleName("leftFiveMargin");
    Style style = child.getElement().getStyle();
    style.setProperty("marginLeft", "auto");
    style.setPaddingLeft(5, PX);
    style.setBackgroundColor("aliceblue");
    //child.setWidth(HINT_WIDTH + "px");
    child.setWidth(79 + "%");
    return child;
  }

  @NotNull
  private com.google.gwt.user.client.ui.Image getImage(String cc) {
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

    styleButton(widgets);
    buttonDiv.add(widgets);

    setMinWidth(buttonDiv, ARROW_WIDTH);

    return buttonDiv;
  }

  @NotNull
  private String getCapitalized(String nameForAnswer) {
    return nameForAnswer.substring(0, 1).toUpperCase() + nameForAnswer.substring(1);
  }

  /**
   * Show turn editor option if you have the permissions and if we're on LISTEN.
   * @return
   */
  @NotNull
  private Widget getRightArrow() {
    DivWidget buttonDiv = new DivWidget();
    setMinWidth(buttonDiv, ARROW_WIDTH);

    String nameForAnswer = getNextView() == null ? "" : getNextView().toString().toLowerCase();

    {
      Button rightButton = new Button(getCapitalized(nameForAnswer), IconType.ARROW_RIGHT, event -> gotGoForward());
      new TooltipHelper().createAddTooltip(rightButton, getNextTooltip(), Placement.LEFT);
      styleButton(rightButton);
      rightButton.addStyleName("floatRight");
      rightButton.setEnabled(next != null);

      buttonDiv.add(rightButton);
    }

    if (shouldShowDialogEditor() && thisView == LISTEN) {
      Button editButton = new Button(getCapitalized(TURN_EDITOR.toString()), IconType.ARROW_RIGHT, event -> controller.getNavigation().show(TURN_EDITOR));
      new TooltipHelper().createAddTooltip(editButton, getNextTooltip(), Placement.LEFT);
      styleButton(editButton);
      editButton.addStyleName("floatRight");
      editButton.addStyleName("topFiftyMargin");
      editButton.setType(ButtonType.WARNING);

      buttonDiv.add(editButton);
    }
    return buttonDiv;
  }

  private void styleButton(Button rightButton) {
    rightButton.addStyleName("leftFiveMargin");
    rightButton.addStyleName("rightTenMargin");
  }


  private boolean shouldShowDialogEditor() {
    boolean isDialogMode = controller.getMode() == ProjectMode.DIALOG;
    List<Permission> temp = new ArrayList<>(DIALOG_EDITOR.getPerms());
    temp.retainAll(controller.getPermissions());
    //  logger.info("permission overlap is " + temp);
    return isDialogMode && !temp.isEmpty();
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
