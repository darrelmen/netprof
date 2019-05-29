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
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.Popover;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.github.gwtbootstrap.client.ui.constants.Trigger;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.project.ProjectMode;
import mitll.langtest.shared.user.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.google.gwt.dom.client.Style.Unit.PX;
import static mitll.langtest.client.LangTest.LANGTEST_IMAGES;
import static mitll.langtest.client.custom.INavigation.VIEWS.*;

/**
 * @see ListenViewHelper#addDialogHeader(IDialog, Panel)
 */
public class DialogHeader {
  // private final Logger logger = Logger.getLogger("DialogHeader");
  private static final String HEIGHT = 100 + "px";

  /**
   * @see #getRow()
   */
  private static final int ROW_WIDTH = 98;
  private static final String SPEAK_HINT = "<b>Speak</b> when you see the record icon.";
  private static final String PRESS_AND_HOLD_HINT = "<b>Press and hold</b> when recording.";
  private static final int ARROW_WIDTH = 190;
  public static final String DOWNLOAD = "Download";

  private final INavigation.VIEWS thisView;
  private final INavigation.VIEWS prev;
  private final INavigation.VIEWS next;
  private final ExerciseController<?> controller;

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
  protected DialogHeader(ExerciseController controller, INavigation.VIEWS thisView, INavigation.VIEWS prev, INavigation.VIEWS next) {
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
      boolean mine = dialog.getUserid() == controller.getUser();
      if (getPrevView() != null) {
        Widget leftArrow = getLeftArrow(mine);
        leftArrow.setWidth(sides + "%");
        row.add(leftArrow);
      }

      DivWidget middle = new DivWidget();
      row.add(middle);
      middle.setWidth(75 + "%");

      middle.add(getTitleBlurb());
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
        Widget rightArrow = getRightArrow(dialog, mine);
        rightArrow.setWidth(sides + "%");
        row.add(rightArrow);
      }

      outer.add(row);
    }
    return outer;
  }

  @NotNull
  private DivWidget getDialogLabels(IDialog dialog) {
    DivWidget vert = new DivWidget();
    vert.getElement().setId("vert");
    vert.addStyleName("leftTenMargin");
    vert.addStyleName("rightTenMargin");
    vert.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    vert.setHeight("100%");

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
  private DivWidget getTitleBlurb() {
    String toUse = "";
    String sub = "";
    if (thisView == LISTEN) {
      toUse = "Listen";
      sub = "to prepare to speak your part";
    } else if (thisView == REHEARSE) {
      toUse = "Rehearse";
      sub = "at your own speed";
    } else if (thisView == CORE_REHEARSE) {
      toUse = "Rehearse";
      sub = "at conversational speed";
    } else if (thisView == PERFORM_PRESS_AND_HOLD) {
      toUse = "Perform from memory";
      sub = "at your own speed";
    } else if (thisView == PERFORM) {
      toUse = "Perform from memory";
      sub = "at conversational speed";
    }

    DivWidget headingContainer = new DivWidget();

    headingContainer.getElement().getStyle().setColor("#52a452");
    Heading w = new Heading(3, toUse, sub);
    w.getElement().getStyle().setMarginTop(-4, PX);
    headingContainer.add(w);
    return headingContainer;
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
        row.add(new DivWidget());
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
            "<br/>" +
            PRESS_AND_HOLD_HINT +
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
      case CORE_EDITOR:
        row.add(getHint("<i><b>Edit</b> the core vocabulary of the turns. " +
//            "<br/><b>Record</b> audio for each turn. " +
//            "<br/>" +
//            PRESS_AND_HOLD_HINT +
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
    child.addStyleName("floatLeft");
    Style style = child.getElement().getStyle();

    style.setPaddingLeft(5, PX);
    style.setBackgroundColor("aliceblue");

    style.setMarginTop(-5, PX);

    style.setMarginLeft(89, PX);
    style.setPaddingRight(27, PX);
    style.setClear(Style.Clear.BOTH);
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
  private Widget getLeftArrow(boolean mine) {
    DivWidget buttonDiv = new DivWidget();
    Button widgets = new Button(getCapitalized(getPrevView().toString().toLowerCase()), IconType.ARROW_LEFT, event -> gotGoBack());
    new TooltipHelper().addTooltip(widgets, getPrevTooltip());

    styleButton(widgets);
    buttonDiv.add(widgets);

    setMinWidth(buttonDiv, ARROW_WIDTH);

//    if (shouldShowDialogEditor(mine) && thisView == CORE_EDITOR) {
//      buttonDiv.add(getEditorButton(CORE_REHEARSE, false));
//    }

    return buttonDiv;
  }

  @NotNull
  private String getCapitalized(String nameForAnswer) {
    return nameForAnswer.substring(0, 1).toUpperCase() + nameForAnswer.substring(1);
  }

  /**
   * Show turn editor option if you have the permissions and if we're on LISTEN.
   *
   * @return
   */
  @NotNull
  private Widget getRightArrow(IDialog dialog, boolean mine) {
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

    boolean b = shouldShowDialogEditor(mine);
    if (b && thisView == LISTEN) {
      buttonDiv.add(getTurnEditor());
    }
    if (b && thisView == TURN_EDITOR) {
      buttonDiv.add(getEditorButton(CORE_EDITOR, true));
    }
    if (b && thisView == CORE_REHEARSE) {
      buttonDiv.add(getEditorButton(CORE_EDITOR, true));
    }

    if (thisView == LISTEN || thisView == TURN_EDITOR || thisView == CORE_EDITOR) {
      buttonDiv.add(getDownloadLink());
    }

    if (!dialog.getCoreVocabulary().isEmpty() & thisView != CORE_EDITOR && thisView != SCORES) {
      buttonDiv.add(getCoreVocab(dialog));
    }

    return buttonDiv;
  }

  @NotNull
  private DivWidget getDownloadLink() {
    DivWidget widgets = new DivWidget();
    Anchor download = new Anchor(DOWNLOAD);
    download.addClickHandler(event -> gotDownload());
    download.addStyleName("leftFiveMargin");
    widgets.add(new Image(LANGTEST_IMAGES + "icon-excel.png"));
    widgets.add(download);

    widgets.addStyleName("floatRight");
    widgets.getElement().getStyle().setMarginTop(25, PX);
    widgets.getElement().getStyle().setMarginRight(12, PX);
    widgets.getElement().getStyle().setClear(Style.Clear.BOTH);
    return widgets;
  }

  @NotNull
  private Widget getCoreVocab(IDialog dialog) {
    HTML core_vocab = new HTML("<b>See Vocab</b>");

    Map<String, String> objectObjectHashMap = new TreeMap<>();

    for (ClientExercise exercise : dialog.getCoreVocabulary()) {
      objectObjectHashMap.put(exercise.getForeignLanguage(), exercise.getEnglish());
    }

    StringBuilder builder = new StringBuilder();
    objectObjectHashMap.forEach((k, v) -> builder.append(getTypeAndValue2(k, v)));
    builder.append(getTypeAndValue2(" ", " "));

    String message = "<div style='width:400px;margin-bottom:5px;padding-bottom:5px'>" + builder.toString() + "<br/></div>";

    DivWidget wrapper = new DivWidget();
    wrapper.add(core_vocab);

    wrapper.getElement().getStyle().setClear(Style.Clear.BOTH);
    wrapper.getElement().getStyle().setPaddingLeft(10, PX);
    wrapper.getElement().getStyle().setMarginLeft(10, PX);
    wrapper.addStyleName("floatRight");
    wrapper.getElement().getStyle().setZIndex(0);

    Popover popover = new Popover();
    popover.setContainer("body");

//    simplePopover(popover,
//        wrapper,
//        null,
//        message,
//        Placement.LEFT, true);

//    PopupPanel popupPanel = new PopupHelper().getPopupPanel(core_vocab, new HTML(message));

    final PopupPanel popupPanel = new DecoratedPopupPanel();
    popupPanel.setAutoHideEnabled(true);
    popupPanel.add(new HTML(message));

    core_vocab.addMouseOverHandler(event -> popupPanel.showRelativeTo(core_vocab));
    core_vocab.addMouseOutHandler(event -> popupPanel.hide());

    wrapper.getElement().getStyle().setMarginTop(27, PX);
    wrapper.addStyleName("rightFiveMargin");
    return wrapper;
  }

  private void simplePopover(Popover popover, Widget w, String heading, String message, Placement placement, boolean isHTML) {
    popover.setWidget(w);
    popover.setHtml(isHTML);
    popover.setText(message);
    popover.setPlacement(placement);

    popover.setTrigger(Trigger.HOVER);
    popover.setAnimation(false);
    popover.reconfigure();
    if (heading == null) {
      popover.getWidget().getElement().removeAttribute("data-original-title");
    }
  }

  @NotNull
  private String getTypeAndValue2(String type, String subtext) {
    return "<span style='width:100%;white-space:nowrap;" +
        "'>" +
        "<h5>" + "<span style='float:left;width:50%'>" + type + "</span>" +
        "<span style='margin-left:5px;margin-top:-19px;float:right;color:blue;width:50%'>" + subtext + "</span>" +
        "</h5>" +
        "</span>";
  }

  @NotNull
  private Button getTurnEditor() {
    return getEditorButton(TURN_EDITOR, true);
  }

  @NotNull
  private Button getEditorButton(INavigation.VIEWS turnEditor, boolean addFloatRight) {
    Button editButton = new Button(getCapitalized(turnEditor.toString()), addFloatRight ? IconType.ARROW_RIGHT : IconType.ARROW_LEFT, event -> controller.getNavigation().show(turnEditor));
    new TooltipHelper().createAddTooltip(editButton, "Go to " + turnEditor.toString(), addFloatRight ? Placement.LEFT : Placement.RIGHT);
    styleButton(editButton);
    if (addFloatRight) {
      editButton.addStyleName("floatRight");
    }
    if (!addFloatRight) {
      editButton.addStyleName("topFiftyMargin");
    } else editButton.getElement().getStyle().setMarginTop(30, PX);

    if (turnEditor == TURN_EDITOR || turnEditor == CORE_EDITOR) {
      editButton.setType(ButtonType.WARNING);
    }
    return editButton;
  }

  private void gotDownload() {
    new DownloadHelper().doSimpleDialogDownload(controller.getHost(), new SelectionState());
  }

  private void styleButton(Button rightButton) {
    rightButton.addStyleName("leftFiveMargin");
    rightButton.addStyleName("rightTenMargin");
  }

  private boolean shouldShowDialogEditor(boolean mine) {
    boolean isDialogMode = controller.getMode() == ProjectMode.DIALOG;
    List<Permission> temp = new ArrayList<>(DIALOG_EDITOR.getPerms());
    temp.retainAll(controller.getPermissions());
    //  logger.info("permission overlap is " + temp);
    boolean perm = !temp.isEmpty();
    if (!perm) {
      perm = mine;
    }
    return isDialogMode && perm;
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
