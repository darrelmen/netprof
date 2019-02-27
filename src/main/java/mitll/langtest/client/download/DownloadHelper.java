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
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.list.FacetExerciseList;
import mitll.langtest.client.list.SelectionState;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static mitll.langtest.client.download.DownloadContainer.getDownloadAudio;

/**
 *
 */
public class DownloadHelper implements IShowStatus {
  // private final Logger logger = Logger.getLogger("DownloadHelper");

  private static final String INCLUDE_AUDIO = "Include Audio?";
  public static final String AMPERSAND = "___AMPERSAND___";
  public static final String COMMA = "___COMMA___";

  private static final String VOCABULARY = "Vocabulary";
  /**
   * @see #showDialog(String, FacetExerciseList)
   */
  private static final String DOWNLOAD_AUDIO_AND_SPREADSHEET = "Download Content Spreadsheet (and Audio)";
  private static final String SPEED = "Speed";
  private static final String CONTENT = "Content";
  private static final String CONTEXT_SENTENCES = "Context Sentences";
  private static final List<String> CONTENT_CHOICES = Arrays.asList(VOCABULARY, CONTEXT_SENTENCES);
  private static final String GENDER = "Gender";

  /**
   *
   */
  private static final String DOWNLOAD = "Download";
  private static final String CANCEL = "Cancel";
  private static final int TOP_TO_USE = 10;

  private SelectionState selectionState = null;
  private Collection<String> typeOrder;
  private final SpeedChoices speedChoices;
  private boolean includeAudio = false;

  public DownloadHelper() {
    this.speedChoices = new SpeedChoices(this, false);
  }

  /**
   * @param selectionState
   * @see mitll.langtest.client.list.FacetExerciseList#restoreListBoxState
   */
  public void updateDownloadLinks(SelectionState selectionState, Collection<String> typeOrder) {
    this.selectionState = selectionState;
    this.typeOrder = typeOrder;
  }


  private final Heading status1 = new Heading(4, "");
  private final Heading status2 = new Heading(4, "");
  private final Heading status3 = new Heading(4, "");
  private DivWidget outer;
  private FacetExerciseList<?, ?> facetExerciseList;

  /**
   * @param host
   * @param widgets
   * @see mitll.langtest.client.list.FacetExerciseList#FacetExerciseList
   */
  public void showDialog(String host, FacetExerciseList<?, ?> widgets) {
    isMale = false;
    isContext = false;
    this.facetExerciseList = widgets;

    DivWidget container = new DivWidget();
    isContextSet = false;
    isMaleSet = false;

    styleStatus(status1);
    styleStatus(status2);
    styleStatus(status3);

    final String search = selectionState.getSearch();

    container.add(getIntroWidget(search));

    container.add(getIncludeAudio());

    outer = new DivWidget();
    outer.setVisible(includeAudio);
    outer.add(getContentRow());
    outer.add(getGenderRow());
    outer.add(getSpeedRow());
    outer.add(getStatusArea());

    container.add(outer);

    closeButton = new DialogHelper(true).show(
        DOWNLOAD_AUDIO_AND_SPREADSHEET,
        Collections.emptyList(),
        container,
        DOWNLOAD,
        CANCEL,
        new DialogHelper.CloseListener() {
          @Override
          public boolean gotYes() {
            new DownloadIFrame(toDominoUrl(getDownloadAudio(host)) + getURL());
            return true;
          }

          @Override
          public void gotNo() {

          }

          @Override
          public void gotHidden() {

          }
        }, 550);

    closeButton.setType(ButtonType.SUCCESS);
    closeButton.setEnabled(!includeAudio);
  }

  @NotNull
  private FluidRow getIntroWidget(String search) {
    FluidRow row = new FluidRow();
    String description = selectionState.getDescription(typeOrder, true);
    // includeAudio = !selectionState.isEmpty();
    if (!search.isEmpty()) {
      description += " and searching '" + search + "'";
    }

    int list = selectionState.getList();
    if (list != -1) {
      String listName = facetExerciseList.getListName(list);
      if (listName != null) {
        String description1 = "List " + listName;
        description = (description.isEmpty()) ? description1 : description + " and " + description1;
      }
    }

    Heading w = new Heading(4, description);
    //w.addStyleName("blueColor");
    row.add(w);
    return row;
  }

  private void styleStatus(Heading status1) {
    status1.setText("");
    status1.setHeight("20px");
  }

  private FluidRow getStatusArea() {
    FluidRow row = new FluidRow();

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
    return row;
  }


  private Widget getIncludeAudio() {
    FluidRow row = new FluidRow();
    CheckBox w = new CheckBox(INCLUDE_AUDIO);
    w.setValue(includeAudio);
    w.addClickHandler(event -> {
      includeAudio = w.getValue();
      gotIncludeAudio(includeAudio);
    });
    row.add(w);
    row.addStyleName("bottomFiveMargin");
    row.addStyleName("topFiveMargin");
    return row;
  }

  private void gotIncludeAudio(boolean includeAudio) {
    outer.setVisible(includeAudio);
    if (!includeAudio) {
      closeButton.setEnabled(true);
    } else {
      closeButton.setEnabled(isMaleSet && isContextSet && speedChoices.isThereASpeedChoice());
    }
  }

  private Widget getSpeedRow() {
    FluidRow row = new FluidRow();
    row.add(new Heading(4, SPEED));
    row.add(speedChoices.getSpeedChoices());
    return row;
  }

  private FluidRow getContentRow() {
    FluidRow row = new FluidRow();
    row.add(new Heading(4, CONTENT));
    row.add(getButtonBarChoices(CONTENT_CHOICES, "vocab"));
    return row;
  }

  private FluidRow getGenderRow() {
    FluidRow row = new FluidRow();
    row.add(new Heading(4, GENDER));

    {
      DivWidget showGroup = getGenderChoices();
      showGroup.addStyleName("topFiveMargin");
      showGroup.addStyleName("bottomFiveMargin");
      row.add(showGroup);
    }

    return row;
  }

  private Button closeButton;

  public void showStatus() {
    status1.setText(isContextSet ? isContext ? "Context Sentences " : "Vocabulary Items " : "");
    status2.setText(isMaleSet ? isMale ? "Male Audio " : "Female Audio " : "");
    boolean thereASpeedChoice = speedChoices.isThereASpeedChoice();
    // logger.info("speed choice made = " + thereASpeedChoice);
    status3.setText(thereASpeedChoice ? speedChoices.getStatus() : "");
    if (closeButton != null) {
      closeButton.setEnabled(isMaleSet && isContextSet && thereASpeedChoice);
    }
  }

  private Widget getButtonBarChoices(Collection<String> choices, final String type) {
    ButtonToolbar toolbar = new ButtonToolbar();
    toolbar.getElement().setId("Choices_" + type);
    styleToolbar(toolbar);

    ButtonGroup buttonGroup = new ButtonGroup();
    buttonGroup.setToggle(ToggleType.RADIO);
    toolbar.add(buttonGroup);

    Set<String> seen = new HashSet<>();

    for (String choice : choices) {
      if (!seen.contains(choice)) {
        Button choice1 = getChoice(buttonGroup, choice);
        choice1.setType(ButtonType.DEFAULT);
        buttonGroup.add(choice1);
        seen.add(choice);
      }
    }
    return toolbar;
  }

  /**
   * Only regular speed audio for context.
   *
   * @param buttonGroup
   * @param text
   * @return
   */
  private Button getChoice(ButtonGroup buttonGroup, final String text) {
    ClickHandler handler = event -> Scheduler.get().scheduleDeferred(() -> {
      isContext = !(text.equals(VOCABULARY));
      isContextSet = true;
      if (isContext) {
        speedChoices.chooseReg();
      }
      speedChoices.setEnabled(!isContext);
      showStatus();
    });

    Button choice1 = configure(text, handler, new Button(text));
    buttonGroup.add(choice1);
    return choice1;
  }

  private void styleToolbar(ButtonToolbar toolbar) {
    Style style = toolbar.getElement().getStyle();
    style.setMarginTop(TOP_TO_USE, Style.Unit.PX);
    style.setMarginBottom(TOP_TO_USE, Style.Unit.PX);
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

  private boolean isContextSet = false;
  private boolean isMaleSet = false;

  private DivWidget getGenderChoices() {
    ButtonGroup buttonGroup = new ButtonGroup();
    ButtonToolbar buttonToolbar = getToolbar(buttonGroup);

    for (final String choice : Arrays.asList(M, F)) {
      // logger.info("making choice " + choice);
      com.github.gwtbootstrap.client.ui.Button choiceButton = getChoice(choice, false, event -> {

        if (choice.equals(M)) isMale = true;
        else if (choice.equals(F)) isMale = false;

        isMaleSet = true;

        showStatus();
      });
      buttonGroup.add(choiceButton);
      if (choice.equals(M)) choiceButton.setIcon(IconType.MALE);
      else if (choice.equals(F)) choiceButton.setIcon(IconType.FEMALE);
    }

    return buttonToolbar;
  }

  private ButtonToolbar getToolbar(ButtonGroup buttonGroup) {
    ButtonToolbar buttonToolbar = new ButtonToolbar();
    buttonGroup.setToggle(ToggleType.RADIO);
    buttonToolbar.add(buttonGroup);
    return buttonToolbar;
  }

  private com.github.gwtbootstrap.client.ui.Button getChoice(String title, boolean isActive, ClickHandler handler) {
    com.github.gwtbootstrap.client.ui.Button onButton =
        new com.github.gwtbootstrap.client.ui.Button(title.equals(M) ? "" : title.equals(F) ? "" : title);
    onButton.getElement().setId("Choice_" + title);
    //   controller.register(onButton, exercise.getID());
    onButton.addClickHandler(handler);
    onButton.setActive(isActive);
    onButton.getElement().getStyle().setZIndex(0);
    return onButton;
  }

  private String toDominoUrl(String relativeLoc) {
    String baseUrl = GWT.getHostPageBaseURL();
    StringBuilder dominoUrl = new StringBuilder();

    if ((!(baseUrl.endsWith("/")) && (!(relativeLoc.startsWith("/"))))) {
      dominoUrl.append(baseUrl + "/");
    } else if (baseUrl.endsWith("/") && relativeLoc.startsWith("/")) {
      dominoUrl.append(baseUrl, 0, baseUrl.length() - 1); // remove extra slash
    } else {
      dominoUrl.append(baseUrl);
    }
    dominoUrl.append(relativeLoc);
    return dominoUrl.toString();
  }

  /**
   * Map comma and ampersand to encoded versions so they make it through parsing the url in the download servlet.
   * <p>
   * Probably better ways to do this...
   *
   * @param typeToSection
   * @param search
   * @return
   * @see mitll.langtest.server.DownloadServlet#getTypeToSelectionFromRequest
   */
  private String getURL() {
    final String search = selectionState.getSearch();

    Map<String, Collection<String>> typeToSection = selectionState.getTypeToSection();
    String encode = URL.encodeQueryString(getSelectionMap(typeToSection).toString());

/*    logger.info("getURL " +
        "\n\torig '" + typeToSection.toString() + "'" +
        "\n\tsel '" + sel + "'" +
        " = '" + encode +
        "'");*/

    String dialogParam = selectionState.getDialog() > 0 ? "&d=" + selectionState.getDialog() : "";
    return "?" +
        "request=" + DownloadContainer.DOWNLOAD_AUDIO +
        "&unit=" + encode +
        dialogParam +
        "&male=" + isMale +
        "&regular=" + speedChoices.isRegular() +
        "&context=" + isContext +
        "&audio=" + includeAudio +
        "&search=" + URL.encodeQueryString(search);
  }

  @NotNull
  private Map<String, Collection<String>> getSelectionMap(Map<String, Collection<String>> typeToSection) {
    Map<String, Collection<String>> ts = new HashMap<>();
    typeToSection.forEach((k, v) -> {
      List<String> newV = new ArrayList<>();
      v.forEach(value -> newV.add(
          value
              .replaceAll(",", COMMA)
              .replaceAll("&", AMPERSAND)
      ));
      ts.put(k, newV);
    });
    return ts;
  }
}
