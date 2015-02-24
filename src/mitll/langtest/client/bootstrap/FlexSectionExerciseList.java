package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.list.HistoryExerciseList;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.SectionNode;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/5/13
 * Time: 5:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlexSectionExerciseList extends HistoryExerciseList {
  private Logger logger = Logger.getLogger("FlexSectionExerciseList");
  private static final int LABEL_MARGIN_BOTTOM = 10;

  private static final int HEADING_FOR_LABEL = 4;
  private static final int UNACCOUNTED_WIDTH = 60;
  private static final int CLASSROOM_VERTICAL_EXTRA = 270;
  private static final String SHOWING_ALL_ENTRIES = "Showing all entries";
  private static final String DOWNLOAD_SPREADSHEET = "Download spreadsheet and audio for selected sections.";
  private static final String DOWNLOAD_AUDIO = "downloadAudio";
//  private static final String CONTEXT = "context";

  private final List<ButtonType> buttonTypes = new ArrayList<ButtonType>();
  private final Map<String, ButtonType> typeToButton = new HashMap<String, ButtonType>();
  private int numSections = 0;
  private Panel panelInsideScrollPanel;
  private ScrollPanel scrollPanel;
  private Panel clearColumnContainer;
  private Panel labelColumn;
  private final Heading statusHeader = new Heading(4);
  private Collection<String> typeOrder;
  private final Panel sectionPanel;
  private Anchor downloadLink;
  private Anchor contextDownloadLink;

  /**
   * @param secondRow             add the section panel to this row
   * @param currentExerciseVPanel
   * @param service
   * @param feedback
   * @param controller
   * @param instance
   * @param incorrectFirst
   * @see mitll.langtest.client.custom.content.NPFHelper.FlexListLayout.MyFlexSectionExerciseList#MyFlexSectionExerciseList(com.google.gwt.user.client.ui.Panel, com.google.gwt.user.client.ui.Panel, String, boolean)
   */
  public FlexSectionExerciseList(Panel secondRow, Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                                 UserFeedback feedback,
                                 ExerciseController controller,
                                 String instance, boolean incorrectFirst) {
    super(currentExerciseVPanel, service, feedback, controller, true, instance, incorrectFirst);

    sectionPanel = new FluidContainer();
    sectionPanel.getElement().setId("sectionPanel_" + instance);

    sectionPanel.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
    sectionPanel.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);
    secondRow.add(new Column(12, sectionPanel));

    buttonTypes.add(ButtonType.PRIMARY);
    buttonTypes.add(ButtonType.SUCCESS);
    buttonTypes.add(ButtonType.INFO);
    buttonTypes.add(ButtonType.WARNING);
    setUnaccountedForVertical(CLASSROOM_VERTICAL_EXTRA);
  }

  /**
   * @see mitll.langtest.client.list.PagingExerciseList#PagingExerciseList
   */
  @Override
  protected void addComponents() {
    PagingContainer exerciseShellPagingContainer = makePagingContainer();
    addTableWithPager(exerciseShellPagingContainer);
  }

  /**
   *
   * @param userID
   * @see mitll.langtest.client.LangTest#configureUIGivenUser(long)
   */
  public boolean getExercises(final long userID) {
    //System.out.println("FlexSectionExerciseList.getExercises : Get exercises for user=" + userID + " instance " + getInstance());
    this.userID = userID;
    addWidgets();
    return false;
  }

  public void addWidgets() {
    sectionPanel.clear();
    sectionPanel.add(getWidgetsForTypes());
  }

  /**
   * Assume for the moment that the first type has the largest elements... and every other type nests underneath it.
   *
   * @return
   * @see mitll.langtest.client.list.ListInterface#getExercises(long)
   */
  private Panel getWidgetsForTypes() {
    final FluidContainer container = new FluidContainer();
    container.getElement().setId("typeOrderContainer");
    container.getElement().getStyle().setPaddingLeft(2, Style.Unit.PX);
    container.getElement().getStyle().setPaddingRight(2, Style.Unit.PX);

    getTypeOrder(container);

    return container;
  }

  private void getTypeOrder(final FluidContainer container) {
    typeOrder = controller.getStartupInfo().getTypeOrder();
    addButtonRow(controller.getStartupInfo().getSectionNodes(), container, typeOrder);
  }

  @Override
  protected Collection<String> getTypeOrder(Map<String, Collection<String>> selectionState2) { return typeOrder;  }

  /**
   * @param rootNodes
   * @param container
   * @param types
   * @see #getTypeOrder(com.github.gwtbootstrap.client.ui.FluidContainer)
   */
  private void addButtonRow(List<SectionNode> rootNodes, final FluidContainer container, Collection<String> types) {
/*    System.out.println("FlexSectionExerciseList.addButtonRow for user = " + userID + " got types " +
      types + " num root nodes " + rootNodes.size() + " instance " + instance);*/
    if (types.isEmpty()) {
      System.err.println("huh? types is empty?");
      return;
    }
    showDefaultStatus();

    FlexTable firstTypeRow = new FlexTable();
    firstTypeRow.getElement().setId("firstTypeRow");
    container.add(firstTypeRow);
    firstTypeRow.addStyleName("alignTop");

    populateButtonGroups(types);

    String firstType = types.iterator().next(); // e.g. unit!
    ButtonGroupSectionWidget buttonGroupSectionWidget = (ButtonGroupSectionWidget) typeToBox.get(firstType);

    boolean usuallyThereWillBeAHorizScrollbar = rootNodes.size() > 6;

    this.labelColumn = makeLabelColumn(firstType, firstTypeRow, buttonGroupSectionWidget);
    this.clearColumnContainer = makeClearColumn(usuallyThereWillBeAHorizScrollbar, types, firstType, firstTypeRow, buttonGroupSectionWidget);

    makePanelInsideScrollPanel(firstTypeRow);

    // add columns for each section within first type...

    Collection<String> sectionsInType = new ItemSorter().getSortedItems(getLabels(rootNodes));
    Map<String, SectionNode> nameToNode = getNameToNode(rootNodes);
    numSections = sectionsInType.size();

    List<String> subTypes = new ArrayList<String>(types);
    List<String> subs = subTypes.subList(1, subTypes.size());
    String subType = subs.isEmpty() ? null : subs.iterator().next();
    // add a column for every root type
    Widget last = null;
    long then = System.currentTimeMillis();
    for (String sectionInFirstType : sectionsInType) {
      Panel sectionColumn = new FlowPanel();
      ButtonWithChildren buttonWithChildren = addColumnButton(sectionColumn, sectionInFirstType, buttonGroupSectionWidget);
      buttonWithChildren.setButtonGroup(buttonGroupSectionWidget);
      last = sectionColumn;
      sectionColumn.getElement().getStyle().setMarginBottom((usuallyThereWillBeAHorizScrollbar ? 5 :0), Style.Unit.PX);
      panelInsideScrollPanel.add(sectionColumn);

      if (subType != null) {
        ButtonGroupSectionWidget sectionWidget1 = (ButtonGroupSectionWidget) typeToBox.get(subType);

       // System.out.println("addButtonRow adding row for " + subType + " under " + sectionInFirstType);

        HorizontalPanel rowForChildren = new HorizontalPanel();
        rowForChildren.setWidth("100%");
        rowForChildren.setHorizontalAlignment(ALIGN_LEFT);
        SectionNode sectionNode = nameToNode.get(sectionInFirstType);
        buttonWithChildren.setChildren(addButtonGroup(rowForChildren, sectionNode.getChildren(),
          subType, subs, sectionWidget1));
        //sectionColumn.add(rowForChildren);

        // make row container
        Panel rowContainer = new FlowPanel();
        rowContainer.addStyleName("rowPadding");
        rowContainer.add(rowForChildren);
        sectionColumn.add(rowContainer);

        sectionWidget1.addRow(rowContainer);
      }
    }

    long now = System.currentTimeMillis();
    if (now - then > 300)
      System.out.println("\taddButtonRow took " + (now - then) + " millis" + " instance " + getInstance());

    if (last != null) setSizesAndPushFirst();

    DivWidget bottomRow = getBottomRow();
    addBottomText(bottomRow);
    container.add(bottomRow);
  }

  private DivWidget getBottomRow() {
    FlexTable links = new FlexTable();
    links.setWidget(0, 0,downloadLink = getDownloadLink());
    if (controller.isTeacher()) {
      links.setWidget(0, 1,contextDownloadLink = getContextDownloadLink());
    }
    else {
      logger.info("user is not a teacher.");
    }
    DivWidget bottomRow = new DivWidget();
    bottomRow.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);
    DivWidget left = new DivWidget();
    left.addStyleName("floatLeftList");
    left.add(links);
    bottomRow.add(left);
    return bottomRow;
  }

  /**
   * @see #addButtonRow
   * @return
   */
  private Anchor getDownloadLink() {
    final Anchor downloadLink = new Anchor(getDownloadURL());
    addTooltip(downloadLink);
    downloadLink.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.logEvent(downloadLink,"DownloadLink","N/A","downloading audio");
      }
    });
    downloadLink.getElement().setId("DownloadLink_" + getInstance());
    return downloadLink;
  }

  private Anchor getContextDownloadLink() {
    final Anchor downloadLink = new Anchor(getDownloadContextURL());
    new TooltipHelper().addTooltip(downloadLink, "Download spreadsheet and context audio for selected sections.");
    downloadLink.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        controller.logEvent(downloadLink,"ContextDownloadLink","N/A","downloading context audio");
      }
    });
    downloadLink.getElement().setId("ContextDownloadLink_" + getInstance());
    return downloadLink;
  }

  private SafeHtml getDownloadURL() {
    SelectionState selectionState = getSelectionState(getHistoryToken(""));
    return getURLForDownload(selectionState);
  }

  private SafeHtml getDownloadContextURL() {
    SelectionState selectionState = getSelectionState(getHistoryToken(""));
    return getURLForContextDownload(selectionState);
  }

  /**
   * @see #showSelectionState(mitll.langtest.client.list.SelectionState)
   * @param selectionState
   * @return
   */
  private SafeHtml getURLForDownload(SelectionState selectionState) {
    return getUrlDownloadLink(selectionState, DOWNLOAD_AUDIO,"download","Download");
  }

  private SafeHtml getURLForContextDownload(SelectionState selectionState) {
    return getUrlDownloadLink(selectionState, DOWNLOAD_AUDIO,"context","Context");
  }

  private SafeHtml getUrlDownloadLink(SelectionState selectionState, String command, String request,String title) {
    Map<String, Collection<String>> typeToSection = selectionState.getTypeToSection();

    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    sb.appendHtmlConstant("<a class='" +"icon-download"+
      "' href='" +
        command +
      "?request="+request+"&" +typeToSection+
      "'" +
      ">");
    sb.appendEscaped(" " + title);
    sb.appendHtmlConstant("</a>");
    return sb.toSafeHtml();
  }

  /**
   * @see #addButtonRow(java.util.List, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   * @see #getDownloadLink
   * @param widget
   * @return
   */
  private Tooltip addTooltip(Widget widget) {
    return new TooltipHelper().addTooltip(widget, FlexSectionExerciseList.DOWNLOAD_SPREADSHEET);
  }

  /**
   * Label is in column 0
   * @param firstType
   * @param firstTypeRow
   * @param buttonGroupSectionWidget
   * @see #addButtonRow
   */
  private Panel makeLabelColumn(String firstType, FlexTable firstTypeRow,
                                ButtonGroupSectionWidget buttonGroupSectionWidget) {
    Panel labelColumn = new VerticalPanel();
    labelColumn.getElement().setId("FlexSectionExerciseList_labelColumn");
    addLabelWidgetForRow(labelColumn, firstType, typeToButton.get(firstType), buttonGroupSectionWidget);
    firstTypeRow.setWidget(0, 0, makeFlowPanel(labelColumn, false));
    return labelColumn;
  }

  /**
   * Clear button is in column 1
   * @param usuallyThereWillBeAHorizScrollbar
   * @param types
   * @param firstType
   * @param firstTypeRow
   * @param buttonGroupSectionWidget
   * @see #addButtonRow(java.util.List, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   */
  private Panel makeClearColumn(boolean usuallyThereWillBeAHorizScrollbar, Collection<String> types, String firstType,
                                FlexTable firstTypeRow,
                                ButtonGroupSectionWidget buttonGroupSectionWidget) {
    Panel clearColumnContainer = new VerticalPanel();
    addClearButton(buttonGroupSectionWidget, clearColumnContainer);
    firstTypeRow.setWidget(0, 1,  makeFlowPanel(clearColumnContainer, usuallyThereWillBeAHorizScrollbar));

    for (String type : types) {
      if (type.equals(firstType)) continue;
      makeSectionWidget(labelColumn, clearColumnContainer, type, (ButtonGroupSectionWidget) typeToBox.get(type));
    }
    return clearColumnContainer;
  }

  private void addClearButton(ButtonGroupSectionWidget buttonGroupSectionWidget, Panel clearColumnContainer) {
    ButtonWithChildren clearButton = makeClearButton();
    addClickHandlerToButton(clearButton, ANY, buttonGroupSectionWidget);
    buttonGroupSectionWidget.addButton(clearButton);
    clearColumnContainer.add(clearButton);
  }

  /**
   *
   * @param labelContainer
   * @param usuallyThereWillBeAHorizScrollbar
   * @return
   * @see #makeLabelColumn
   */
  private Panel makeFlowPanel(Panel labelContainer, boolean usuallyThereWillBeAHorizScrollbar) {
    Panel l2 = new FlowPanel();
    l2.add(labelContainer);
/*    if (usuallyThereWillBeAHorizScrollbar) { // hack rule of thumb
      l2.addStyleName("bottomMargin");
    }*/
    return l2;
  }

  /**
   * @see #addButtonRow(java.util.List, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   * @param firstTypeRow
   */
  private void makePanelInsideScrollPanel(FlexTable firstTypeRow) {
    panelInsideScrollPanel = new HorizontalPanel();
    panelInsideScrollPanel.addStyleName("blueBackground");
    panelInsideScrollPanel.addStyleName("borderSpacing");
    panelInsideScrollPanel.getElement().setId("panelInsideScrollPanel");
    makeScrollPanel(firstTypeRow, panelInsideScrollPanel);
  }

  /**
   * @see #makePanelInsideScrollPanel(com.google.gwt.user.client.ui.FlexTable)
   * Button groups are in column 2
   * @param firstTypeRow
   * @param panelInside
   */
  private void makeScrollPanel(FlexTable firstTypeRow, Panel panelInside) {
    this.scrollPanel = new ScrollPanel(panelInside);
    this.scrollPanel.addStyleName("leftFiveMargin");
    this.scrollPanel.getElement().setId("scrollPanel");
    firstTypeRow.setWidget(0, 2, scrollPanel);
  }

  /**
   * @see #addButtonRow(java.util.List, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   * @param types
   */
  private void populateButtonGroups(Collection<String> types) {
    typeToBox.clear();
    typeToButton.clear();

    int j = 0;
    for (String type : types) {
      ButtonGroupSectionWidget buttonGroupSectionWidget1 = new ButtonGroupSectionWidget(type);
      typeToBox.put(type, buttonGroupSectionWidget1);
      typeToButton.put(type, buttonTypes.get(j++ % types.size()));
    }
  }

  private Map<String, SectionNode> getNameToNode(List<SectionNode> rootNodes) {
    Map<String, SectionNode> nameToNode = new HashMap<String, SectionNode>();
    for (SectionNode n : rootNodes) nameToNode.put(n.getName(), n);
    return nameToNode;
  }

  private List<String> getLabels(List<SectionNode> nodes) {
    List<String> items = new ArrayList<String>();
    for (SectionNode n : nodes) items.add(n.getName());
    return items;
  }

  /**
   * @see #makeLabelColumn(String, com.google.gwt.user.client.ui.FlexTable, ButtonGroupSectionWidget)
   * @param labelRow
   * @param firstType
   * @param buttonType
   * @param buttonGroupSectionWidget
   */
  private void addLabelWidgetForRow(Panel labelRow, String firstType, ButtonType buttonType, SectionWidget buttonGroupSectionWidget) {
    Widget widget = makeLabelWidget(firstType);
    String color = getButtonTypeStyle(buttonType);

    buttonGroupSectionWidget.addLabel(widget, color);
    labelRow.add(widget);
  }

  private Heading makeLabelWidget(String firstType) {
    Heading widget = new Heading(HEADING_FOR_LABEL, firstType);
    // TODO : necessary?
    //DOM.setStyleAttribute(widget.getElement(), "webkitMarginBefore", "0");
   // DOM.setStyleAttribute(widget.getElement(), "webkitMarginAfter", "0");
    widget.getElement().getStyle().setMarginTop(0, Style.Unit.PX);
    widget.getElement().getStyle().setMarginBottom(LABEL_MARGIN_BOTTOM, Style.Unit.PX);
    widget.getElement().getStyle().setProperty("whiteSpace","nowrap");
    return widget;
  }

  /**
   * @see HistoryExerciseList#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @param selectionState
   */
  @Override
  protected void restoreListBoxState(SelectionState selectionState) {
    super.restoreListBoxState(selectionState);
    showSelectionState(selectionState);
  }

  /**
   * Add a line that spells out in text which lessons have been chosen, derived from the selection state.
   * Show in the same type order as the button rows.
   *
   * @param selectionState to get the current selection state from
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  private void showSelectionState(SelectionState selectionState) {
    //System.out.println("FlexSectionExerciseList.showSelectionState : got " + event + " and state '" + selectionState +"'");

    // keep the download link info in sync with the selection
    Map<String, Collection<String>> typeToSection = selectionState.getTypeToSection();
    if (downloadLink != null) downloadLink.setHTML(getURLForDownload(selectionState));
    if (contextDownloadLink != null) contextDownloadLink.setHTML(getURLForContextDownload(selectionState));

    if (typeToSection.isEmpty()) {
      showDefaultStatus();
    } else {
      StringBuilder status = new StringBuilder();

      //System.out.println("showSelectionState : typeOrder " + typeOrder + " selection state " + typeToSection);

      for (String type : typeOrder) {
        Collection<String> selectedItems = typeToSection.get(type);
        if (selectedItems != null) {
         // String statusForType = type + " " + selectedItems.toString().replaceAll("\\[", "").replaceAll("\\]", "");
          List<String> sorted = new ArrayList<String>();
          for (String selectedItem : selectedItems) {
            sorted.add(selectedItem);
          }
          Collections.sort(sorted);
          StringBuilder status2 = new StringBuilder();
          for (String item : sorted) status2.append(item).append(", ");
          String s = status2.toString();
          if (!s.isEmpty()) s = s.substring(0,s.length()-2);
          String statusForType = type + " " + s;
          status.append(statusForType).append(" and ");
        }
      }
      String text = status.toString();
      if (text.length() > 0) text = text.substring(0, text.length() - " and ".length());
      statusHeader.setText(text);
    }
  }

  private void showDefaultStatus() {
    statusHeader.setText(SHOWING_ALL_ENTRIES);
  }

  /**
   * @param container
   * @see #addButtonRow
   */
  void addBottomText(Panel container) {
    Panel status = getStatusRow();
    container.add(status);
    status.addStyleName("leftFiftyPercentMargin");
   // return status;
  }

  Panel getStatusRow() {
    Panel status = new DivWidget();
    status.getElement().setId("statusRow");
    status.addStyleName("alignCenter");
    status.addStyleName("inlineBlockStyle");
    status.add(statusHeader);
    statusHeader.getElement().setId("statusHeader");
    statusHeader.getElement().getStyle().setMarginTop(0, Style.Unit.PX);
    statusHeader.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
    return status;
  }

  /**
   * @param sectionInFirstType
   * @param buttonGroupSectionWidget
   * @return
   * @see #addButtonRow
   */
  private ButtonWithChildren addColumnButton(Panel columnContainer,
                                             final String sectionInFirstType,
                                             final ButtonGroupSectionWidget buttonGroupSectionWidget) {
    columnContainer.addStyleName("inlineBlockStyle");
    // add a button
    ButtonWithChildren overallButton = makeOverallButton(buttonGroupSectionWidget.getType(), sectionInFirstType);
    addClickHandlerToButton(overallButton, sectionInFirstType, buttonGroupSectionWidget);
    buttonGroupSectionWidget.addButton(overallButton);

    Panel rowAgain = new FlowPanel();
    columnContainer.add(rowAgain);

    rowAgain.add(overallButton);
    buttonGroupSectionWidget.addRow(rowAgain);
    rowAgain.addStyleName("rowPadding");

    return overallButton;
  }

  private void addClickHandlerToButton(final ButtonWithChildren overallButton, final String sectionInFirstType,
                                       final ButtonGroupSectionWidget buttonGroupSectionWidget) {
    overallButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        buttonGroupSectionWidget.simpleSelectItem(Collections.singleton(sectionInFirstType));
        pushNewSectionHistoryToken();
      }
    });
  }

  /**
   * So when we get a URL with a bookmark in it, we have to make the UI appear consistent with it.
   *
   * @param type
   * @param sections
   * @see mitll.langtest.client.list.HistoryExerciseList#restoreListBoxState(mitll.langtest.client.list.SelectionState)
   */
  @Override
  protected void selectItem(String type, Collection<String> sections) {
    ButtonGroupSectionWidget listBox = (ButtonGroupSectionWidget) typeToBox.get(type);
    listBox.clearSelectionState();
    //System.out.println("FlexSectionExerciseList.selectItem : instance " + instance+ " selecting " + type + "=" + sections);
    listBox.selectItem(sections);
  }

  /**
   * @param type
   * @see HistoryExerciseList#restoreListBoxState(mitll.langtest.client.list.SelectionState)
   */
  @Override
  protected void clearEnabled(String type) {
    ButtonGroupSectionWidget listBox = (ButtonGroupSectionWidget) typeToBox.get(type);
    listBox.clearEnabled();
  }

  protected void enableAllButtonsFor(String type) {
    ButtonGroupSectionWidget listBox = (ButtonGroupSectionWidget) typeToBox.get(type);
    listBox.enableAll();
  }

  /**
   * @seex HistoryExerciseList.MySetExercisesCallback#onSuccess(mitll.langtest.shared.ExerciseListWrapper)
   */
  protected void gotEmptyExerciseList() { showEmptySelection(); }

  /**
   * @param title
   * @return Button with this title and the initial type
   * @see #addColumnButton
   */
  private ButtonWithChildren makeOverallButton(String type, String title) {
    ButtonWithChildren overallButton = makeButton(title, type);
    overallButton.setWidth("100%");
    overallButton.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
    overallButton.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);

    overallButton.setType(ButtonType.PRIMARY);
    return overallButton;
  }

  private ButtonWithChildren makeClearButton() {
    ButtonWithChildren clear = makeButton(ANY, ANY);
    clear.getElement().getStyle().setMarginTop(5, Style.Unit.PX);
    clear.getElement().getStyle().setMarginBottom(12, Style.Unit.PX);
    clear.setType(ButtonType.DEFAULT);
    return clear;
  }

  /**
   * Actually kick off getting the exercises.
   * @see #addButtonRow(java.util.List, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   */
  private void setSizesAndPushFirst() {
    //System.out.println("setSizesAndPushFirst instance " + instance);
    pushFirstListBoxSelection();

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        setScrollPanelWidth();
      }
    });
  }

  /**
   * Add label and clear button to row for each type
   *
   * @param labelContainer
   * @param clearColumnContainer
   * @param typeForOriginal
   * @param sectionWidget
   * @return
   * @see #makeClearColumn(boolean, java.util.Collection, String, com.google.gwt.user.client.ui.FlexTable, ButtonGroupSectionWidget)
   */
  private SectionWidget makeSectionWidget(Panel labelContainer, Panel clearColumnContainer, String typeForOriginal,
                                          ButtonGroupSectionWidget sectionWidget) {
    // make label
    Widget labelWidget = getLabelWidget(typeForOriginal);
    labelContainer.add(labelWidget);
    ButtonType buttonType = typeToButton.get(typeForOriginal);
    sectionWidget.addLabel(labelWidget, getButtonTypeStyle(buttonType));

    // make clear button
    Button sectionButton = makeSubgroupButton(sectionWidget, ANY, buttonType, true);
    sectionWidget.addButton(sectionButton);

    clearColumnContainer.add(sectionButton);
    return sectionWidget;
  }

  private String getButtonTypeStyle(ButtonType buttonType) {
    return (buttonType.equals(ButtonType.PRIMARY)) ? "primaryButtonColor" : buttonType.equals(ButtonType.SUCCESS) ? "successButtonColor"
      : (buttonType.equals(ButtonType.INFO)) ? "infoButtonColor" : "warningButtonColor";
  }

  /**
   * @param parentColumn
   * @param rootNodes
   * @param typeForOriginal
   * @param remainingTypes
   * @param sectionWidget
   * @see FlexSectionExerciseList#addButtonRow
   */
  private List<ButtonWithChildren> addButtonGroup(Panel parentColumn, List<SectionNode> rootNodes,
                                                  String typeForOriginal,
                                                  List<String> remainingTypes, ButtonGroupSectionWidget sectionWidget) {
    Map<String, SectionNode> nameToNode = getNameToNode(rootNodes);

    List<String> sortedItems = new ItemSorter().getSortedItems(getLabels(rootNodes));
    ButtonType buttonType = typeToButton.get(typeForOriginal);

    List<String> objects = Collections.emptyList();
    List<String> subs = remainingTypes.isEmpty() ? objects : remainingTypes.subList(1, remainingTypes.size());
    String subType = subs.isEmpty() ? null : subs.iterator().next();
    int n = sortedItems.size();
    List<ButtonWithChildren> buttonChildren = new ArrayList<ButtonWithChildren>();
    for (int i = 0; i < n; i++) {
      String section = sortedItems.get(i);
      SectionNode sectionNode = nameToNode.get(section);

      List<SectionNode> children = sectionNode.getChildren();

      ButtonWithChildren buttonForSection = makeSubgroupButton(sectionWidget, section, buttonType, false);
      buttonChildren.add(buttonForSection);
      buttonForSection.setButtonGroup(sectionWidget);

      // System.out.println("addButtonGroup : for " + typeForOriginal + "=" + section + " recurse num children = " + children.size() + " group " + sectionWidget + " for " + buttonForSection);

      if ((n > 1 && i < n - 1) || n == 1) {
        buttonForSection.addStyleName("buttonMargin");
      }

      Panel rowForSection;
      if (!children.isEmpty() && subType != null) {
        rowForSection = new VerticalPanel();

        buttonForSection.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
        buttonForSection.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);
        buttonForSection.setWidth("100%");
        rowForSection.add(buttonForSection);

/*        Panel containerForButtons = new SimplePanel();
        containerForButtons.add(buttonForSection);
        rowForSection.add(containerForButtons);
        sectionWidget.addRow(containerForButtons);*/

        // recurse on children
        HorizontalPanel horizontalContainerForChildren = new HorizontalPanel();
        rowForSection.add(horizontalContainerForChildren);
        sectionWidget.addRow(rowForSection);
        rowForSection.addStyleName("rowPadding");

        System.out.println("\taddButtonGroup : for " + typeForOriginal + "=" + section + " recurse on " + children.size() +
          " children at " + subType);
        ButtonGroupSectionWidget sectionWidget1 = (ButtonGroupSectionWidget) typeToBox.get(subType);
        List<ButtonWithChildren> subButtons =
          addButtonGroup(horizontalContainerForChildren, children, subType, subs, sectionWidget1);
        buttonForSection.setChildren(subButtons);
        //buttonForSection.setButtonGroup(sectionWidget1);
      } else {

        rowForSection = buttonForSection;

      /*  rowForSection = new SimplePanel();
        rowForSection.addStyleName("topFiveMargin");
        sectionWidget.addRow(rowForSection);
        rowForSection.add(buttonForSection);*/
      }
      parentColumn.add(rowForSection);
    }
    return buttonChildren;
  }

  @Override
  public void onResize() {
    super.onResize();

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        //System.out.println("FlexSectionExerciseList : deferred set scroll panel width instance " + instance);
        setScrollPanelWidth();
      }
    });
  }

  void setScrollPanelWidth() {
    if (labelColumn != null) {
      int leftSideWidth = labelColumn.getOffsetWidth() + clearColumnContainer.getOffsetWidth();
      if (leftSideWidth == 0) leftSideWidth = 130;
      int width = Window.getClientWidth() - leftSideWidth - UNACCOUNTED_WIDTH;
/*      System.out.println("FlexSectionExeciseList.setScrollPanelWidth : scrollPanel width is " + width +" client " +Window.getClientWidth() +
        " label col " +labelColumn.getOffsetWidth() + " clear " +clearColumnContainer.getOffsetWidth() + " unacct "+UNACCOUNTED_WIDTH);*/
      scrollPanel.setWidth(Math.max(300, width) + "px");
    }
//    else {
     // System.out.println("\tsetScrollPanelWidth : labelColumn is null instance " + instance);
  //  }
  }

  /**
   * @param typeForOriginal
   * @return
   * @see #makeSectionWidget
   */
  private Widget getLabelWidget(String typeForOriginal) {
    Heading widget = new Heading(HEADING_FOR_LABEL, typeForOriginal);
    Style style = widget.getElement().getStyle();
    style.setMarginBottom(10, Style.Unit.PX);
    style.setMarginRight(5, Style.Unit.PX);
    return widget;
  }

  /**
   * @param sectionWidgetFinal
   * @param section
   * @param buttonType
   * @param isClear
   * @return
   * @see #addButtonGroup(com.google.gwt.user.client.ui.Panel, java.util.List, String, java.util.List, ButtonGroupSectionWidget)
   * @see #makeSectionWidget
   */
  private ButtonWithChildren makeSubgroupButton(final ButtonGroupSectionWidget sectionWidgetFinal,
                                                final String section,
                                                ButtonType buttonType, boolean isClear) {
    //System.out.println("making button " + type + "=" + section);
    ButtonWithChildren sectionButton = makeButton(section, sectionWidgetFinal.getType());

    //  System.out.println("made button " + sectionButton);

    boolean shrinkHorizontally = numSections > 5;
    //  System.out.println("num " + numSections + " shrinkHorizontally " +shrinkHorizontally );

    if (!isClear && shrinkHorizontally) { // squish buttons if we have lots of sections
      sectionButton.getElement().getStyle().setPaddingLeft(6, Style.Unit.PX);
      sectionButton.getElement().getStyle().setPaddingRight(6, Style.Unit.PX);
    }

    sectionButton.setType(isClear ? ButtonType.DEFAULT : buttonType);
    sectionButton.setEnabled(!isClear);

    if (isClear) {
      sectionButton.getElement().getStyle().setMarginBottom(LABEL_MARGIN_BOTTOM, Style.Unit.PX);
    }

    addClickHandlerToButton(sectionButton, section, sectionWidgetFinal);

    sectionWidgetFinal.addButton(sectionButton);

    return sectionButton;
  }

  private ButtonWithChildren makeButton(String caption, String type) {
    ButtonWithChildren widgets = new ButtonWithChildren(caption, type);
    controller.register(widgets, "unknown");

    return widgets;
  }

  public static class ButtonWithChildren extends Button {
    private List<ButtonWithChildren> children = new ArrayList<ButtonWithChildren>();
    private final String type;
    private ButtonWithChildren parent;
    private ButtonGroupSectionWidget buttonGroupContainer;

    public ButtonWithChildren(String caption, String type) {
      super(caption);
      this.type = type;
      getElement().setId("Button_"+caption+"_"+type);
    }

    /**
     * @param children
     * @see FlexSectionExerciseList#addButtonGroup(com.google.gwt.user.client.ui.Panel, java.util.List, String, java.util.List, ButtonGroupSectionWidget)
     * @see FlexSectionExerciseList#addButtonRow
     */
    public void setChildren(List<ButtonWithChildren> children) {
      this.children = children;
      for (ButtonWithChildren child : children) {
        child.setParentButton(this);
      }
    }

    public void setButtonGroup(ButtonGroupSectionWidget buttonGroupContainer) {
      this.buttonGroupContainer = buttonGroupContainer;
    }

    public ButtonGroupSectionWidget getButtonGroup() {
      return buttonGroupContainer;
    }

    public ButtonWithChildren getParentButton() {
      return parent;
    }

/*    public List<ButtonWithChildren> getSiblings() {
      List<ButtonWithChildren> siblings = new ArrayList<ButtonWithChildren>();
      if (parent != null) {
        for (ButtonWithChildren button : parent.children) {
          if (button != this) siblings.add(button);
        }
      }
      return siblings;
    }*/

    public void setParentButton(ButtonWithChildren parent) {
      this.parent = parent;
    }

    public List<ButtonWithChildren> getButtonChildren() {
      return children;
    }

    public String getButtonType() {
      return type;
    }

    public String getTypeOfChildren() {
      return children.isEmpty() ? "" : children.iterator().next().getButtonType();
    }

    public String toString() {
      return "Button " + type + " = " + getText().trim() +
        (children.isEmpty() ? "" :
          ", children type " + getTypeOfChildren() + " num " + getButtonChildren().size());
    }
  }
}
