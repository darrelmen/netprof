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

import com.github.gwtbootstrap.client.ui.*;
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
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.instrumentation.EventRegistration;
import mitll.langtest.client.list.HistoryExerciseList;
import mitll.langtest.client.list.SelectionState;

import java.util.*;
import java.util.logging.Logger;

public class DownloadHelper {
  private final Logger logger = Logger.getLogger("DownloadHelper");
  //  private static final String DOWNLOAD_SPREADSHEET = "Download spreadsheet and audio for selected sections.";
  private static final String DOWNLOAD_AUDIO = "downloadAudio";
//  private final EventRegistration eventRegistration;

  private final HistoryExerciseList exerciseList;

  public DownloadHelper(EventRegistration eventRegistration,
                        HistoryExerciseList exerciseList) {
  //  this.eventRegistration = eventRegistration;
    this.exerciseList = exerciseList;
  }

  /**
   * @return
   * @see FlexSectionExerciseList#getBottomRow
   */
  public Panel getDownloadLinks() {
    FlexTable links = new FlexTable();
    Button download = new Button("Download", IconType.DOWNLOAD);
    download.setType(ButtonType.SUCCESS);
    selectionState = exerciseList.getSelectionState();
    download.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        showDialog();
      }
    });
    links.setWidget(0, 0, download

    );
    return links;
  }

  private SelectionState selectionState;

  /**
   * @param selectionState
   * @see FlexSectionExerciseList#showSelectionState(SelectionState)
   */
  public void updateDownloadLinks(SelectionState selectionState) {
    this.selectionState = selectionState;
  }

  private void showDialog() {
    isMale = false;
    isContext = false;
    isRegular = true;

    DivWidget container = new DivWidget();
    if (selectionState.isEmpty()) {
      container.add(new Heading(3, "Download spreadsheet for whole course."));
      container.add(new Heading(4, "Select a unit or chapter to download audio."));
    } else {

      DivWidget showGroup = getShowGroup(false);
      showGroup.addStyleName("topFiveMargin");
      showGroup.addStyleName("bottomFiveMargin");
      // showGroup.addStyleName("leftFiveMargin");

      FluidRow row = new FluidRow();
      row.add(new Heading(4, "Gender"));
      row.add(showGroup);
      container.add(row);

      row = new FluidRow();
      row.add(new Heading(4, "Content"));
      Widget buttonBarChoices = getButtonBarChoices(Arrays.asList("Vocabulary", "Context Sentences"), "vocab", ButtonType.SUCCESS);
      row.add(buttonBarChoices);

      container.add(row);

      row = new FluidRow();
      row.add(new Heading(4, "Speed"));
      Widget showGroup2 = getShowGroup2();
      row.add(showGroup2);
      // showGroup2.addStyleName("leftFiveMargin");


      container.add(row);
    }

    new DialogHelper(true).show(
        "Download Content",
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
        });
  }

  private Widget getButtonBarChoices(Collection<String> values, final String type, ButtonType buttonType) {
    ButtonToolbar toolbar = new ButtonToolbar();
    toolbar.getElement().setId("Choices_" + type);
    styleToolbar(toolbar);

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    toolbar.add(buttonGroup);


    Set<String> seen = new HashSet<String>();
    boolean isFirst = true;
    for (String v : values) {
      if (!seen.contains(v)) {
        Button choice1 = getChoice(buttonGroup, v);
        //buttons.add(choice1);
        choice1.setActive(isFirst);
        isFirst = false;
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
  private boolean isRegular = true;

  private final Image turtle = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "turtle_32.png"));
  private final Image turtleSelected = new Image(UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "turtle_32_selected.png"));

  private DivWidget getShowGroup(boolean selectFirst) {
    Collection<String> choices = new ArrayList<>();
    choices.add("M");
    choices.add("F");

    ButtonToolbar buttonToolbar = new ButtonToolbar();
    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    buttonToolbar.add(buttonGroup);

    boolean first = selectFirst;
    for (final String choice : choices) {
      com.github.gwtbootstrap.client.ui.Button choice1 = getChoice(choice, first, new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          //contextPlay.playAudio(getAudioRef(choice));
          if (choice.equals(M)) isMale = true;
          else if (choice.equals(F)) isMale = false;
        }
      });
      buttonGroup.add(choice1);
      if (choice.equals(M)) choice1.setIcon(IconType.MALE);
      else if (choice.equals(F)) choice1.setIcon(IconType.FEMALE);
      first = !selectFirst;
    }

    return buttonToolbar;
  }


  private Widget getShowGroup2() {
    FluidRow row = new FluidRow();
    ToggleButton pushButton = new ToggleButton(turtle, turtleSelected);
    pushButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        isRegular = !pushButton.isDown();

      }
    });
    row.add(pushButton);
    addTooltip(pushButton);
    row.setWidth("65px");
    row.getElement().getStyle().setMarginBottom(25, Style.Unit.PX);


    return row;
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

  /**
   * @param widget
   * @return
   * @see #getDownloadLink
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
