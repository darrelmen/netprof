/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.download;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.ToggleType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.list.SelectionState;

import java.util.*;
import java.util.logging.Logger;

public class DownloadHelper {
  private final Logger logger = Logger.getLogger("DownloadHelper");

  //  private static final String DOWNLOAD_SPREADSHEET = "Download spreadsheet and audio for selected sections.";
  private static final String DOWNLOAD_AUDIO = "downloadAudio";
//  private final EventRegistration eventRegistration;

  private final FlexSectionExerciseList exerciseList;

  public DownloadHelper(EventRegistration eventRegistration, FlexSectionExerciseList exerciseList) {
    //  this.eventRegistration = eventRegistration;
    this.exerciseList = exerciseList;
  }

  /**
   * @return
   * @see FlexSectionExerciseList#getBottomRow
   */
  public Panel getDownloadLinks() {
    Button download = new Button("Download", IconType.DOWNLOAD);
    download.setType(ButtonType.PRIMARY);
    selectionState = exerciseList.getSelectionState();
    download.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        showDialog();
      }
    });

    return download;
  }

  private SelectionState selectionState;

  /**
   * @param selectionState
   * @see FlexSectionExerciseList#showSelectionState(SelectionState)
   */
  public void updateDownloadLinks(SelectionState selectionState) {
    this.selectionState = selectionState;
  }

  private Heading status1 = new Heading(4, "");
  private Heading status2 = new Heading(4, "");
  private Heading status3 = new Heading(4, "");

  private void showDialog() {
    isMale = false;
    isContext = false;
    isRegular = true;

    DivWidget container = new DivWidget();
    if (selectionState.isEmpty()) {
      container.add(new Heading(3, "Download spreadsheet for whole course."));
      container.add(new Heading(4, "Select a unit or chapter to download audio."));
    } else {

      status1.setText("");
      status1.setHeight("20px");
      isRegularSet = false;
      isContextSet = false;
      isMaleSet    = false;

      status2.setText("");
      status2.setHeight("20px");

      status3.setText("");
      status3.setHeight("20px");

      FluidRow row = getContentRow();
      container.add(row);

      row = getGenderRow();
      container.add(row);

      row = getSpeedRow();
      container.add(row);

      row = new FluidRow();
      container.add(row);

      Well well = new Well();
      Panel vert = new VerticalPanel();
      well.add(vert);
      vert.add(status1);
      status1.getElement().getStyle().setMarginTop(0, Style.Unit.PX);
      status1.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
      vert.add(status2);
      status2.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
      vert.add(status3);
      status3.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
      row.add(well);
    }

    closeButton = new DialogHelper(true).show(
        "Download Audio and Spreadsheet",
        Collections.emptyList(),
        container,
        "Download",
        "Cancel",
        new DialogHelper.CloseListener() {
          @Override
          public void gotYes() {
            String urlForDownload = toDominoUrl(DOWNLOAD_AUDIO) + getURL(DOWNLOAD_AUDIO, DOWNLOAD_AUDIO, selectionState.getTypeToSection());
            new DownloadIFrame(urlForDownload);
          }

          @Override
          public void gotNo() {

          }
        }, 550);
    closeButton.setType(ButtonType.SUCCESS);
    closeButton.setEnabled(selectionState.isEmpty());
  }

  private FluidRow getSpeedRow() {
    FluidRow row;
    row = new FluidRow();
    row.add(new Heading(4, "Speed"));
    Widget showGroup2 = getSpeedChoices();
    row.add(showGroup2);
    return row;
  }

  private FluidRow getContentRow() {
    FluidRow row;
    row = new FluidRow();
    row.add(new Heading(4, "Content"));
    Widget buttonBarChoices = getButtonBarChoices(Arrays.asList("Vocabulary", "Context Sentences"), "vocab", ButtonType.DEFAULT);
    row.add(buttonBarChoices);
    return row;
  }

  private FluidRow getGenderRow() {
    DivWidget showGroup = getGenderChoices(false);
    showGroup.addStyleName("topFiveMargin");
    showGroup.addStyleName("bottomFiveMargin");

    FluidRow row = new FluidRow();
    row.add(new Heading(4, "Gender"));
    row.add(showGroup);
    return row;
  }

  private Button closeButton;

  private void showStatus() {
    status1.setText(isContextSet ? isContext ? "Context Sentences " : "Vocabulary Items " : "");
    status2.setText(isMaleSet ? isMale ? "Male Audio " : "Female Audio " : "");
    status3.setText((isRegularSet ? isRegular ? "Regular Speed" : "Slow Speed" : ""));
    closeButton.setEnabled(isMaleSet && isContextSet && isRegularSet);
  }

  private Widget getButtonBarChoices(Collection<String> values, final String type, ButtonType buttonType) {
    ButtonToolbar toolbar = new ButtonToolbar();
    toolbar.getElement().setId("Choices_" + type);
    styleToolbar(toolbar);

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    toolbar.add(buttonGroup);

    Set<String> seen = new HashSet<String>();

    for (String v : values) {
      if (!seen.contains(v)) {
        Button choice1 = getChoice(buttonGroup, v);
        choice1.setType(buttonType);
        buttonGroup.add(choice1);
        seen.add(v);
      }
    }
    return toolbar;
  }

  private Button getChoice(ButtonGroup buttonGroup, final String text) {
    final Button onButton = new Button(text);
    ClickHandler handler = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          public void execute() {
            isContext = !(text.equals("Vocabulary"));
            isContextSet = true;
            showStatus();
          }
        });
      }
    };

    Button choice1 = configure(text, handler, onButton);
    buttonGroup.add(choice1);
    return choice1;
  }

  private void styleToolbar(ButtonToolbar toolbar) {
    Style style = toolbar.getElement().getStyle();
    int topToUse = 10;
    style.setMarginTop(topToUse, Style.Unit.PX);
    style.setMarginBottom(topToUse, Style.Unit.PX);
    //   style.setMarginLeft(5, Style.Unit.PX);
  }

  private Button configure(String title, ClickHandler handler, Button onButton) {
    onButton.setType(ButtonType.INFO);
    String s = "Choice_" + title;
    onButton.getElement().setId(s);
    onButton.addClickHandler(handler);
    onButton.setActive(false);
    return onButton;
  }

  private static final String M = "M";
  private static final String F = "F";

  private boolean isMale = false;
  private boolean isContext = false;
  private boolean isRegular = false;

  private boolean isContextSet = false;
  private boolean isRegularSet = false;
  private boolean isMaleSet = false;

  private final Image turtle = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "turtle_32.png"));
  private final Image turtleSelected = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "turtle_32_selected.png"));

  private final Image rabbit = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "rabbit32.png"));
  private final Image rabbitSelected = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "rabbit32_selected.png"));

  private DivWidget getGenderChoices(boolean selectFirst) {
    ButtonGroup buttonGroup = new ButtonGroup();
    ButtonToolbar buttonToolbar = getToolbar(buttonGroup);

    for (final String choice : Arrays.asList(M, F)) {
      com.github.gwtbootstrap.client.ui.Button choice1 = getChoice(choice, false, new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          if (choice.equals(M)) isMale = true;
          else if (choice.equals(F)) isMale = false;
          isMaleSet = true;
          showStatus();
        }
      });
      buttonGroup.add(choice1);
      if (choice.equals(M)) choice1.setIcon(IconType.MALE);
      else if (choice.equals(F)) choice1.setIcon(IconType.FEMALE);
    }

    return buttonToolbar;
  }

  private ButtonToolbar getToolbar(ButtonGroup buttonGroup) {
    ButtonToolbar buttonToolbar = new ButtonToolbar();
    buttonGroup.setToggle(ToggleType.RADIO);
    buttonToolbar.add(buttonGroup);
    return buttonToolbar;
  }

  private Panel getToolbar2() {
    Panel buttonToolbar = new HorizontalPanel();
    //  buttonGroup.setToggle(ToggleType.RADIO);
    //buttonToolbar.add(buttonGroup);
    return buttonToolbar;
  }

  private Widget getSpeedChoices() {
    Panel buttonToolbar = getToolbar2();
    buttonToolbar.setHeight("40px");
    String choice = "Regular";

    regular = getChoice2(choice, rabbit, rabbitSelected, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        isRegular = regular.isDown();
        isRegularSet = true;
        showSpeeds();
      }
    });
    buttonToolbar.add(regular);

    String choice2 = "Slow";
    slow = getChoice2(choice2, turtle, turtleSelected, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        isRegular = !slow.isDown();
        logger.info("got slow click " + isRegular);
        isRegularSet = true;
        showSpeeds();
      }
    });
    //slow.getElement().getStyle().setMarginLeft(64, Style.Unit.PX);
    buttonToolbar.add(slow);

//    return buttonToolbar;
    // addTooltip(pushButton);
    //row.setWidth("65px");
    buttonToolbar.getElement().getStyle().setMarginBottom(25, Style.Unit.PX);

//    return row;
    return buttonToolbar;
  }

  ToggleButton regular, slow;

  private void showSpeeds() {
    regular.setDown(isRegular);
    slow.setDown(!isRegular);
    showStatus();
  }

  private com.github.gwtbootstrap.client.ui.Button getChoice(String title, boolean isActive, ClickHandler handler) {
    com.github.gwtbootstrap.client.ui.Button onButton = new com.github.gwtbootstrap.client.ui.Button(title.equals(M) ? "" : title.equals(F) ? "" : title);
    onButton.getElement().setId("Choice_" + title);
    //   controller.register(onButton, exercise.getID());
    onButton.addClickHandler(handler);
    onButton.setActive(isActive);
    onButton.getElement().getStyle().setZIndex(0);
    return onButton;
  }

  private ToggleButton getChoice2(String title, Image upImage, Image downImage, ClickHandler handler) {
    //com.github.gwtbootstrap.client.ui.Button onButton = new com.github.gwtbootstrap.client.ui.Button(title.equals(M) ? "" : title.equals(F) ? "" : title);

    ToggleButton onButton = new ToggleButton(upImage, downImage);
    onButton.getElement().setId("Choice_" + title);
    //   controller.register(onButton, exercise.getID());
    onButton.addClickHandler(handler);
    //  onButton.setActive(isActive);
    onButton.getElement().getStyle().setZIndex(0);
    onButton.setWidth("50" +
        "px");
    onButton.setHeight("32" +
        "px");
    return onButton;
  }

  /**
   * @param widget
   * @return
   * @see #getSpeedChoices
   */
  private void addTooltip(Widget widget) {
    new TooltipHelper().addTooltip(widget, "Click for slow speed audio.");
  }


  private String toDominoUrl(String relativeLoc) {
    String baseUrl = GWT.getHostPageBaseURL();
    StringBuilder dominoUrl = new StringBuilder();

    if ((!(baseUrl.endsWith("/")) && (!(relativeLoc.startsWith("/"))))) {
      dominoUrl.append(baseUrl + "/");
    } else if (baseUrl.endsWith("/") && relativeLoc.startsWith("/")) {
      dominoUrl.append(baseUrl.substring(0, baseUrl.length() - 1)); // remove extra slash
    } else {
      dominoUrl.append(baseUrl);
    }
    dominoUrl.append(relativeLoc);
    return dominoUrl.toString();
  }

  private String getURL(String command, String request, Map<String, Collection<String>> typeToSection) {
    return //command +
        "?" +
            "request" +
            "=" + request +
            "&unit=" + typeToSection +
            "&male=" + isMale +
            "&regular=" + isRegular +
            "&context=" + isContext
        ;
  }
}
