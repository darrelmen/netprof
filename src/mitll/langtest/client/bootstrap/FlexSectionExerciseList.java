package mitll.langtest.client.bootstrap;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SectionExerciseList;
import mitll.langtest.client.exercise.SectionWidget;
import mitll.langtest.client.exercise.SelectionState;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.SectionNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 3/5/13
 * Time: 5:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlexSectionExerciseList extends SectionExerciseList {
  public static final int HEADING_FOR_LABEL = 4;
  private static final String USER_PROMPT = "Choose a lesson, preview, and share flashcard exercises.";
  public static final int FRAME_WIDTH = 1024-50-50;
  public static final int FRAME_HEIGHT = 640;
  public static final String FLASHCARDCOPY = "flashcardcopy";
  public static final String TIMEDFLASHCARDCOPY = "timedflashcardcopy";
  private final List<ButtonType> buttonTypes = new ArrayList<ButtonType>();
  private Map<String,ButtonType> typeToButton = new HashMap<String, ButtonType>();
  private int numSections = 0;

  public FlexSectionExerciseList(FluidRow secondRow, Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                                 UserFeedback feedback,
                                 boolean showTurkToken, boolean showInOrder, boolean showListBox, ExerciseController controller) {
    super(currentExerciseVPanel, service, feedback, showTurkToken, showInOrder, showListBox, controller);

    Panel child = sectionPanel = new FluidContainer();
    DOM.setStyleAttribute(sectionPanel.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(sectionPanel.getElement(), "paddingRight", "2px");
    secondRow.add(new Column(12,child));

    buttonTypes.add(ButtonType.PRIMARY);
    buttonTypes.add(ButtonType.SUCCESS);
    buttonTypes.add(ButtonType.INFO);
    buttonTypes.add(ButtonType.WARNING);
  }

  @Override
  protected void addComponents() {
    addTableWithPager();
  }

  public void getExercises(final long userID) {
    System.out.println("Flex : Get exercises for user=" + userID);
    this.userID = userID;
    sectionPanel.clear();

    Panel flexTable = getWidgetsForTypes(userID);

    if (!showListBoxes) {
      SelectionState selectionState = getSelectionState(History.getToken());
      loadExercises(selectionState.typeToSection, selectionState.item);
    }

    sectionPanel.add(flexTable);
  }

  /**
   * Assume for the moment that the first type has the largest elements... and every other type nests underneath it.
   *
   * @see mitll.langtest.client.exercise.SectionExerciseList#useInitialTypeToSectionMap(java.util.Map, long)
   * @paramx result
   * @param userID
   * @return
   */
  protected Panel getWidgetsForTypes(final long userID) {
    final FluidContainer container = new FluidContainer();
    DOM.setStyleAttribute(container.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(container.getElement(), "paddingRight", "2px");

    service.getTypeOrder(new AsyncCallback<Collection<String>>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("getTypeOrder can't contact server. got " + caught);
      }

      @Override
      public void onSuccess(final Collection<String> sortedTypes) {
        if (showListBoxes) {
          service.getSectionNodes(new AsyncCallback<List<SectionNode>>() {
            @Override
            public void onFailure(Throwable caught) {
              Window.alert("getSectionNodes can't contact server. got " + caught);
            }

            @Override
            public void onSuccess(List<SectionNode> result) {
              addButtonRow(result, userID, container, sortedTypes);
            }
          });
        }
        else {
          addStudentTypeAndSection(container, sortedTypes);
        }
      }
    });

    return container;
  }

  private void addStudentTypeAndSection(FluidContainer container,Collection<String> sortedTypes) {
    String token = unencodeToken(History.getToken());
    SelectionState selectionState = getSelectionState(token);

    for (final String type : sortedTypes) {
      Collection<String> typeValue = selectionState.typeToSection.get(type);
      if (typeValue != null) {
        FluidRow fluidRow = new FluidRow();
        container.add(fluidRow);

        fluidRow.add(new Column(2, new Heading(4,type)));
        fluidRow.add(new Column(1, new Heading(4,typeValue.toString())));
      }
    }
  }

  private Panel panelInsideScrollPanel;
  private Panel scrollPanel;
  private Panel  clear;
  private Panel label;

  /**
   * @see #getWidgetsForTypes(long)
   * @param rootNodes
   * @param userID
   * @param container
   * @param types
   */
  private void addButtonRow(List<SectionNode> rootNodes, long userID, FluidContainer container, Collection<String> types) {
    System.out.println("getWidgetsForTypes (success) for user = " + userID + " got types " + types);
    String firstType = types.iterator().next(); // e.g. unit!

    container.add(getInstructionRow());
    FlexTable firstTypeRow = new FlexTable();
    container.add(firstTypeRow);
    firstTypeRow.addStyleName("alignTop");

    populateButtonGroups(types);

    ButtonGroupSectionWidget buttonGroupSectionWidget = (ButtonGroupSectionWidget)typeToBox.get(firstType);

    Panel labelContainer = new VerticalPanel();
    addLabelWidgetForRow(labelContainer,firstType,typeToButton.get(firstType),buttonGroupSectionWidget);
    label = labelContainer;

    FlowPanel l2 = new FlowPanel();
    l2.add(labelContainer);
    l2.addStyleName("bottomMargin");

    firstTypeRow.setWidget(0, 0, l2);

    Panel clearColumnContainer = new VerticalPanel();

    ButtonWithChildren clearButton = makeClearButton();
    addClickHandlerToButton(clearButton, ANY, buttonGroupSectionWidget);
    buttonGroupSectionWidget.addButton(clearButton);
    clearColumnContainer.add(clearButton);


    FlowPanel c2 = new FlowPanel();
    c2.add(clearColumnContainer);
    c2.addStyleName("bottomMargin");
    //clearColumnContainer.addStyleName("bottomMargin");

    firstTypeRow.setWidget(0, 1, c2);

    for (String type : types) {
      if (type.equals(firstType)) continue;
      makeSectionWidget(labelContainer, clearColumnContainer, type, (ButtonGroupSectionWidget)typeToBox.get(type));
    }

    clear = clearColumnContainer;

    panelInsideScrollPanel = new HorizontalPanel();
    panelInsideScrollPanel.addStyleName("blueBackground");
    panelInsideScrollPanel.addStyleName("borderSpacing");

    makeScrollPanel(firstTypeRow, panelInsideScrollPanel);

    // add columns for each section within first type...

    Collection<String> sectionsInType = getLabels(rootNodes);
    Map<String, SectionNode> nameToNode = getNameToNode(rootNodes);
    sectionsInType = getSortedItems(sectionsInType);
    numSections = sectionsInType.size();

    List<String> subTypes = new ArrayList<String>(types);
    List<String> subs = subTypes.subList(1, subTypes.size());
    String subType = subs.isEmpty() ? null : subs.iterator().next();
    // add a column for every root type
    Widget last = null;
    long then = System.currentTimeMillis();
    for (String sectionInFirstType : sectionsInType) {
      FlowPanel columnContainer = new FlowPanel();
      ButtonWithChildren buttonWithChildren = addColumnButton(columnContainer,sectionInFirstType, buttonGroupSectionWidget);
      last = columnContainer;
      DOM.setStyleAttribute(columnContainer.getElement(), "marginBottom", "5px");
      panelInsideScrollPanel.add(columnContainer);

      if (subType != null) {
        SectionNode sectionNode = nameToNode.get(sectionInFirstType);
        ButtonGroupSectionWidget sectionWidget1 = (ButtonGroupSectionWidget)typeToBox.get(subType);
        Panel rowContainer = new FlowPanel();

        HorizontalPanel rowForChildren = new HorizontalPanel();

        rowForChildren.setWidth("100%");
        rowContainer.addStyleName("rowPadding");
        rowForChildren.setHorizontalAlignment(ALIGN_LEFT);
        List<ButtonWithChildren> buttonWithChildrens = addButtonGroup(rowForChildren, sectionNode.getChildren(), subType, subs, sectionWidget1);
        buttonWithChildren.setChildren(buttonWithChildrens);
        //columnContainer.add(rowForChildren);
        rowContainer.add(rowForChildren);
        columnContainer.add(rowContainer);

        sectionWidget1.addRow(rowContainer);
      }
    }
    long now = System.currentTimeMillis();
    System.out.println("\tgetWidgetsForTypes took " + (now-then) + " millis");

    setSizesAndPushFirst(last);
    addBottomText(container);
  }

  private void makeScrollPanel(FlexTable firstTypeRow, Panel panelInside) {
    this.scrollPanel = new ScrollPanel(panelInside);
    firstTypeRow.setWidget(0,2,scrollPanel);
    setScrollPanelWidth();
  }

  private void populateButtonGroups(Collection<String> types) {
    typeToBox.clear();
    typeToButton.clear();

    int j = 0;
    for (String type : types) {
      ButtonGroupSectionWidget buttonGroupSectionWidget1 = new ButtonGroupSectionWidget(type,typeToBox);
      typeToBox.put(type,buttonGroupSectionWidget1);
      typeToButton.put(type, buttonTypes.get(j++ % types.size()));
    }

    System.out.println("populateButtonGroups " + types + " : " + typeToBox);
  }

  private Map<String, SectionNode> getNameToNode(List<SectionNode> rootNodes) {
    Map<String,SectionNode> nameToNode = new HashMap<String, SectionNode>();
    for (SectionNode n : rootNodes) nameToNode.put(n.getName(), n);
    return nameToNode;
  }


  protected List<String> getLabels(List<SectionNode> nodes) {
    List<String> items = new ArrayList<String>();
    for (SectionNode n : nodes) items.add(n.getName());
    return items;
  }

  private void addLabelWidgetForRow(Panel labelRow, String firstType, ButtonType buttonType, SectionWidget buttonGroupSectionWidget) {
    Heading widget = makeLabelWidget(firstType);
    String color = getButtonTypeStyle(buttonType);

    buttonGroupSectionWidget.addLabel(widget, color);
    labelRow.add(widget);
  }

  private Heading makeLabelWidget(String firstType) {
    Heading widget = new Heading(HEADING_FOR_LABEL, firstType);
    DOM.setStyleAttribute(widget.getElement(), "webkitMarginBefore", "0");
    DOM.setStyleAttribute( widget.getElement(), "webkitMarginAfter", "0");
    DOM.setStyleAttribute( widget.getElement(), "marginTop", "0px");
    DOM.setStyleAttribute( widget.getElement(), "marginBottom", "15px");
    return widget;
  }

  @Override
  public void onValueChange(ValueChangeEvent<String> event) {
    super.onValueChange(event);

    if (showListBoxes) {
      showSelectionState(event);
    }
  }

  /**
   * Add a line that spells out in text which lessons have been chosen.
   * @param event
   */
  private void showSelectionState(ValueChangeEvent<String> event) {
    SelectionState selectionState = new SelectionState(event);

    StringBuilder status = new StringBuilder();
    Set<Map.Entry<String, Collection<String>>> entries = selectionState.typeToSection.entrySet();
    for (Map.Entry<String, Collection<String>> part : entries) {
        String statusForType = part.getKey() + " " + part.getValue().toString().replaceAll("\\[", "").replaceAll("\\]", "");
        status.append(statusForType).append(" ");
    }
    statusHeader.setText(status.toString());
    if (entries.isEmpty()) {
      statusHeader.setText("Showing all entries");
    }
  }

  private Heading statusHeader = new Heading(4);

  private void addBottomText(FluidContainer container) {
    FluidRow status = new FluidRow();
    status.addStyleName("alignCenter");
    status.addStyleName("inlineStyle");
    container.add(status);
    status.add(statusHeader);
    statusHeader.setText("Showing all entries");
  }

  /**
   * @see TableSectionExerciseList#addTableToLayout(java.util.Map)
   * @param container
   */
  protected void addPreviewWidgets(Panel container) {
    HorizontalPanel fluidRow = new HorizontalPanel();
    container.add(fluidRow);
    fluidRow.setWidth("100%");
    //   Widget emailWidget = getEmailWidget();
    //  fluidRow.add(new Column(2, /*3,*/ emailWidget));

//    Widget hideBoxesWidget = getHideBoxesWidget();
    //fluidRow.add(new Heading(5, "Preview and share"));

    urlInputBox.setText(getFlashcardLink());

    flashcardCopy = makeCopyButton(FLASHCARDCOPY);
    flashcardCopy.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        bindZero(flashcardCopy.getElement(),getFlashcardLink());
      }
    });
    Widget flashcardWidget = getFlashcard("Flashcard", FLASHCARDCOPY, flashcardCopy, urlInputBox, false);
    fluidRow.add(flashcardWidget);

    urlInputBox2.setText(getTimedFlashcardLink());

    timedFlashcardCopy = makeCopyButton(TIMEDFLASHCARDCOPY);
    timedFlashcardCopy.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        bindZero(timedFlashcardCopy.getElement(),getTimedFlashcardLink());
      }
    });
    updateFlashcardCopy();

    Widget flashcardWidget2 = getFlashcard("Timed Flashcard", TIMEDFLASHCARDCOPY, timedFlashcardCopy, urlInputBox2, true);
    SimplePanel w = new SimplePanel(new Heading(6));
    w.setWidth("20px");
    w.setHeight("2px");
    fluidRow.add(w);

    fluidRow.add(flashcardWidget2);
  }

  private Button flashcardCopy;
  private Button timedFlashcardCopy;
  private TextBox urlInputBox = new TextBox();
  private TextBox urlInputBox2 = new TextBox();

  protected Widget getFlashcard(String title, String copyButtonID, Button copyButton,TextBox urlInputBox,boolean timed) {
    VerticalPanel outer = new VerticalPanel();
    Heading titleHeader = new Heading(6, title);

    outer.add(titleHeader);
    FlowPanel panel = new FlowPanel();
    outer.add(panel);
    panel.addStyleName("url-box");

    Heading flashcard = new Heading(5, "URL");
    flashcard.addStyleName("floatLeft");
    flashcard.addStyleName("shareTitle");
    panel.add(flashcard);

    // make url input
    urlInputBox.addStyleName("url-input");
    DOM.setStyleAttribute(urlInputBox.getElement(), "fontFamily",
      "\"Lucida Sans Typewriter\", \"Lucida Console\", Monaco, \"Bitstream Vera Sans Mono\",\"Courier New\", Courier, monospace;");
    panel.add(urlInputBox);

    // make flashcardCopy button
    panel.add(copyButton);

    // make flashcardCopy feedback
    FlowPanel container = new FlowPanel();
    container.addStyleName("floatRight");
    Heading copiedFeedback = new Heading(6, "");
    copiedFeedback.getElement().setId(copyButtonID +"Feedback");
    copiedFeedback.addStyleName("floatRight");
    container.add(copiedFeedback);

    panel.add(copiedFeedback);
    Button preview = getPreviewButton(timed);
    panel.add(preview);

    return outer;
  }

  private Button getPreviewButton(final boolean doTimedFlashcard) {
    Button preview = new Button("Preview");
    preview.addStyleName("leftTenMargin");
    preview.setTitle("Preview flashcards");

    preview.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Modal modal = new Modal(false, true);
        String title = "Flashcard Preview";
        if (doTimedFlashcard) title = "Timed " + title;
        modal.setTitle(title);
        modal.setCloseVisible(true);
        Frame w = new Frame(doTimedFlashcard ? getTimedFlashcardLink() : getFlashcardLink());
        modal.add(w);
        w.setWidth(FRAME_WIDTH + "px");
        w.setHeight(FRAME_HEIGHT +"px");
        int modalWidth = FRAME_WIDTH + 50;
        modal.setWidth(modalWidth + "px");
        int heightSlip = 70;
        modal.setMaxHeigth(FRAME_HEIGHT+ heightSlip + "px");
        DOM.setStyleAttribute(modal.getElement(), "marginLeft", (-modalWidth / 2) + "px");
        modal.show();
      }
    });
    return preview;
  }

  private Button makeCopyButton(String copyButtonID) {
    Button copy = new Button("Copy", IconType.COPY);
    copy.addStyleName("leftTenMargin");
    copy.getElement().setId(copyButtonID);
    copy.setTitle("Copy to clipboard.");
    return copy;
  }

  /**
   * @see TableSectionExerciseList#addTableToLayout(java.util.Map)
   */
  protected void doZero() {
    String widgetID = FLASHCARDCOPY;
    zero(GWT.getModuleBaseURL(), widgetID, widgetID + "Feedback");
    registerCallback();
  }

  /**
   * @see #setModeLinks(String)
   */
  private void updateFlashcardCopy() {
    String flashcardLink = getFlashcardLink();
    if (flashcardCopy != null) {
      flashcardCopy.getElement().setAttribute("data-clipboard-text", flashcardLink);
    }
    urlInputBox.setText(flashcardLink);

    String timedFlashcardLink = getTimedFlashcardLink();
    if (timedFlashcardCopy != null) {
      timedFlashcardCopy.getElement().setAttribute("data-clipboard-text", timedFlashcardLink);
    }
    urlInputBox2.setText(timedFlashcardLink);
  }

  private static void feedback(String feedback) {
    showPopup("Copied " + feedback + " to clipboard.");
  }

  private static void showPopup(String html) {
    final PopupPanel pleaseWait = new DecoratedPopupPanel();
    pleaseWait.setAutoHideEnabled(true);
    pleaseWait.add(new HTML(html));
    pleaseWait.center();

    Timer t = new Timer() {
      @Override
      public void run() {
        pleaseWait.hide();
      }
    };
    t.schedule(3000);
  }

  private native void registerCallback() /*-{
      $wnd.feedback = $entry(@mitll.langtest.client.bootstrap.FlexSectionExerciseList::feedback(Ljava/lang/String;));
  }-*/;

  private String getFlashcardLink() {
    return GWT.getHostPageBaseURL() + "?flashcard=true#" + token;
  }

  private String getTimedFlashcardLink() {
    return GWT.getHostPageBaseURL() + "?flashcard=true" +
      "&timedGame=true" +
      "#" + token;
  }

  private native void zero(String moduleBaseURL,String widgetID,String widgetFeedbackID)  /*-{
      var stuff =  $wnd.document.getElementById(widgetID);
      //alert("Stuff is " +stuff);

      var clip = new $wnd.ZeroClipboard( stuff, {
          moviePath: moduleBaseURL + "swf/ZeroClipboard.swf"
      } );
      clip.setHandCursor(true);
      clip.on( 'load', function(client) {
        //  $wnd.alert( "1 movie is loaded" );
      } );

      clip.on( 'complete', function(client, args) {
       //   this.style.display = 'none'; // "this" is the element that was clicked
       //   alert("Copied text to clipboard: " + args.text );
      //    $wnd.document.getElementById(widgetFeedbackID).innerHTML = "Copied!";
         $wnd.feedback(args.text);
      } );

      clip.on( 'dataRequested', function ( client, args ) {
          //clip.setText( 'Copied to clipboard.' );
      } );

  }-*/;

  /**
   * @param me
   * @param strMsg
   */
  private native void bindZero(com.google.gwt.user.client.Element me, String strMsg)/*-{
      var clip = new $wnd.ZeroClipboard();
      clip.setText(strMsg);
      clip.glue(me);
  }-*/;

  private String token = "";

  /**
   * @see mitll.langtest.client.exercise.SectionExerciseList#pushNewSectionHistoryToken()
   * @param historyToken
   */
  protected void setModeLinks(String historyToken) {
    this.token = historyToken;
    updateFlashcardCopy();
  }

  private Panel getInstructionRow() {
    Panel instructions = new FluidRow();
    instructions.addStyleName("alignCenter");
    instructions.addStyleName("inlineStyle");

    Heading heading = new Heading(5, USER_PROMPT);
    instructions.add(heading);
    return instructions;
  }

  /**
   * @see #addButtonRow(java.util.List, long, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   * @paramx type
   * @param sectionInFirstType
   * @param buttonGroupSectionWidget
   * @return
   */
  private ButtonWithChildren addColumnButton(FlowPanel columnContainer,
                                             final String sectionInFirstType,
                                             final ButtonGroupSectionWidget buttonGroupSectionWidget) {
    columnContainer.addStyleName("inlineStyle");
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

  private void addClickHandlerToButton(final ButtonWithChildren overallButton, final String sectionInFirstType, final ButtonGroupSectionWidget buttonGroupSectionWidget) {
    overallButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        List<String> selections = new ArrayList<String>();
        selections.add(sectionInFirstType);
      //  System.out.println("got click on " + overallButton + " with children " + overallButton.getButtonChildren());
        buttonGroupSectionWidget.selectItem(selections, true, typeToBox);
       // buttonGroupSectionWidget.addEnabledButtons(overallButton.getButtonChildren());
        pushNewSectionHistoryToken();
      }
    });
  }

  @Override
  protected void selectItem(String type, Collection<String> sections) {
    ButtonGroupSectionWidget listBox = (ButtonGroupSectionWidget)typeToBox.get(type);

    listBox.selectItem(sections, false, typeToBox);
  }

  /**
   * @see #addColumnButton
   * @param title
   * @return Button with this title and the initial type
   */
  private ButtonWithChildren makeOverallButton(String type, String title) {
    ButtonWithChildren overallButton = new ButtonWithChildren(title, type);

    overallButton.setWidth("100%");
    DOM.setStyleAttribute(overallButton.getElement(), "paddingLeft", "0px");
    DOM.setStyleAttribute(overallButton.getElement(), "paddingRight", "0px");
    //DOM.setStyleAttribute(overallButton.getElement(), "borderWidth", "0");

    overallButton.setType(ButtonType.PRIMARY);
    return overallButton;
  }

  private ButtonWithChildren makeClearButton() {
    ButtonWithChildren clear = new ButtonWithChildren(ANY, ANY);

    DOM.setStyleAttribute(clear.getElement(), "marginTop", "5px");
    DOM.setStyleAttribute(clear.getElement(), "marginBottom", "12px");

    clear.setType(ButtonType.DEFAULT);
    return clear;
  }

  private void setSizesAndPushFirst(Widget columnContainer) {
    int offsetHeight = columnContainer.getOffsetHeight()+5;

    System.out.println("height is " + offsetHeight);
    //scrollPanel.setHeight(Math.max(50, offsetHeight) + "px");
    panelInsideScrollPanel.setHeight(Math.max(50, offsetHeight) + "px");
    // panelInsideScrollPanel.getParent().setHeight(Math.max(50, offsetHeight) + "px");

    int width = Window.getClientWidth() - label.getOffsetWidth() - clear.getOffsetWidth()- 90;
    System.out.println("setting width to " +width);
   // scrollPanel.setWidth(Math.max(300, width) + "px");
    scrollPanel.setWidth("100%");
    pushFirstListBoxSelection();
  }

  /**
   * Add label and clear button to row for each type
   * @see #addButtonRow(java.util.List, long, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   * @param labelContainer
   * @param clearColumnContainer
   * @param typeForOriginal
   * @param sectionWidget
   * @return
   */
  private SectionWidget makeSectionWidget(Panel labelContainer, Panel clearColumnContainer, String typeForOriginal,
                                          ButtonGroupSectionWidget sectionWidget) {
    ButtonType buttonType = typeToButton.get(typeForOriginal);
    // make label

    Widget labelWidget = getLabelWidget(typeForOriginal);
    labelContainer.add(labelWidget);
    String color = getButtonTypeStyle(buttonType);
    sectionWidget.addLabel(labelWidget,color);

    // make clear button
    Button sectionButton = makeSubgroupButton(sectionWidget, ANY, buttonType, true);
    sectionWidget.addButton(sectionButton);

    clearColumnContainer.add(sectionButton);
    return sectionWidget;
  }

  private String getButtonTypeStyle(ButtonType buttonType) {
    return (buttonType.equals(ButtonType.PRIMARY)) ? "primaryButtonColor" :buttonType.equals(ButtonType.SUCCESS) ? "successButtonColor"
        : (buttonType.equals(ButtonType.INFO)) ? "infoButtonColor" : "warningButtonColor";
  }

  /**
   * @see FlexSectionExerciseList#addButtonRow(java.util.List, long, com.github.gwtbootstrap.client.ui.FluidContainer, java.util.Collection)
   * @param parentColumn
   * @param rootNodes
   * @param typeForOriginal
   * @param remainingTypes
   * @param sectionWidget
   */
  private List<ButtonWithChildren> addButtonGroup(HorizontalPanel parentColumn, List<SectionNode> rootNodes, String typeForOriginal,
                              List<String> remainingTypes, ButtonGroupSectionWidget sectionWidget) {
    Map<String, SectionNode> nameToNode = getNameToNode(rootNodes);

    List<String> sortedItems = getSortedItems(getLabels(rootNodes));
    ButtonType buttonType = typeToButton.get(typeForOriginal);

    List<String> objects = Collections.emptyList();
    List<String> subs = remainingTypes.isEmpty() ? objects : remainingTypes.subList(1, remainingTypes.size());
    String subType = subs.isEmpty() ? null : subs.iterator().next();
    int n = sortedItems.size();
    List<ButtonWithChildren> buttonChildren = new ArrayList<ButtonWithChildren>();
    for (int i = 0; i<n; i++){
      String section = sortedItems.get(i);
      SectionNode sectionNode = nameToNode.get(section);

      List<SectionNode> children = sectionNode.getChildren();

      Panel rowForSection;
      ButtonWithChildren buttonForSection = makeSubgroupButton(sectionWidget, /*subType,*/ section, buttonType, false);
      buttonChildren.add(buttonForSection);
      if (n > 1 && i < n-1) {
        buttonForSection.addStyleName("buttonMargin");
      }

      if (!children.isEmpty() && subType != null) {
        rowForSection = new VerticalPanel();

        DOM.setStyleAttribute(buttonForSection.getElement(), "paddingLeft", "0px");
        DOM.setStyleAttribute(buttonForSection.getElement(), "paddingRight", "0px");
        buttonForSection.setWidth("100%");
        rowForSection.add(buttonForSection);
        // recurse on children
        HorizontalPanel horizontalContainerForChildren = new HorizontalPanel();
        rowForSection.add(horizontalContainerForChildren);
        sectionWidget.addRow(rowForSection);
        rowForSection.addStyleName("rowPadding");

        System.out.println("for " + typeForOriginal + "=" + section + " recurse on " + children.size() +
          " children at " + subType);
        List<ButtonWithChildren> subButtons = addButtonGroup(horizontalContainerForChildren, children, subType, subs, (ButtonGroupSectionWidget) typeToBox.get(subType));
        buttonForSection.setChildren(subButtons);
      } else {
        rowForSection = buttonForSection;
      }
      parentColumn.add(rowForSection);
    }
    return buttonChildren;
  }

  @Override
  public void onResize() {
    super.onResize();

   // int offsetHeight = lastColumn.getOffsetHeight()+5;
   // System.out.println("onResize height is " + offsetHeight);

    setScrollPanelWidth();

 /*   if (scrollPanel.getOffsetHeight() != offsetHeight) {
    scrollPanel.setHeight(Math.max(50, offsetHeight) + "px");
    }
*/
  }

  protected void setScrollPanelWidth() {
    if (label != null) {
      int width = Window.getClientWidth() - label.getOffsetWidth() - clear.getOffsetWidth() - 100;
      System.out.println("scrollPanel width is " + width);
      scrollPanel.setWidth(Math.max(300, width) + "px");
    }
  }

  /**
   * @see #makeSectionWidget
   * @param typeForOriginal
   * @return
   */
  private Widget getLabelWidget(String typeForOriginal) {
    Heading widget = new Heading(HEADING_FOR_LABEL, typeForOriginal);
   // DOM.setStyleAttribute(widget.getElement(), "webkitMarginBefore", "10px");
 //   DOM.setStyleAttribute( widget.getElement(), "webkitMarginAfter", "10px");
  //  DOM.setStyleAttribute( widget.getElement(), "marginTop", "2px");
    DOM.setStyleAttribute( widget.getElement(), "marginBottom", "10px");
    DOM.setStyleAttribute( widget.getElement(), "marginRight", "5px");
    return widget;
  }

  /**
   * @see #addButtonGroup(com.google.gwt.user.client.ui.HorizontalPanel, java.util.List, String, java.util.List, ButtonGroupSectionWidget)
   * @see #makeSectionWidget
   * @param sectionWidgetFinal
   * @param section
   * @param buttonType
   * @param isClear
   * @return
   */
  private ButtonWithChildren makeSubgroupButton(final ButtonGroupSectionWidget sectionWidgetFinal,
                                                /*String sectionType,*/ final String section,
                                    ButtonType buttonType, boolean isClear) {
    //System.out.println("making button " + type + "=" + section);
    ButtonWithChildren sectionButton = new ButtonWithChildren(section, sectionWidgetFinal.getType());
  //  System.out.println("made button " + sectionButton);

    boolean shrinkHorizontally = numSections > 5;
  //  System.out.println("num " + numSections + " shrinkHorizontally " +shrinkHorizontally );

    if (!isClear && shrinkHorizontally) { // squish buttons if we have lots of sections
      DOM.setStyleAttribute(sectionButton.getElement(), "paddingLeft", "6px");
      DOM.setStyleAttribute(sectionButton.getElement(), "paddingRight", "6px");
    }

    sectionButton.setType(isClear ? ButtonType.DEFAULT : buttonType);
    sectionButton.setEnabled(!isClear);

    if (isClear) {
    //  DOM.setStyleAttribute(sectionButton.getElement(), "bottomMargin", "12px");
      DOM.setStyleAttribute(sectionButton.getElement(), "marginBottom", "15px");
    }

    addClickHandlerToButton(sectionButton, section, sectionWidgetFinal);

    sectionWidgetFinal.addButton(sectionButton);


    return sectionButton;
  }

  public static class ButtonWithChildren extends Button {
    List<ButtonWithChildren> children = new ArrayList<ButtonWithChildren>();
    String type;
    public ButtonWithChildren(String caption, String type) {
      super(caption);
      this.type = type;
    }

    public void addChild(ButtonWithChildren b) {
      children.add(b);
    }

    public void setChildren(List<ButtonWithChildren> children) {
      this.children = children;
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

    public boolean hasChildren() {
      return !children.isEmpty();
    }
  }

  /**
   * @see #setOtherListBoxes(java.util.Map)
   * @param result
   */
  @Override
  protected void populateListBoxAfterSelection(Map<String, Collection<String>> result) {
    throw new IllegalArgumentException("don't call me");
/*    Set<String> typesMentioned = new HashSet<String>(typeToBox.keySet());
    for (Map.Entry<String, Collection<String>> pair : result.entrySet()) {
      SectionWidget sectionWidget = typeToBox.get(pair.getKey());
      if (sectionWidget != null) {
        sectionWidget.enableInSet(pair.getValue());
      }
    }
    typesMentioned.removeAll(result.keySet());
    for (String remainingType : typesMentioned) {
      SectionWidget sectionWidget = typeToBox.get(remainingType);
      if (sectionWidget != null) {
        sectionWidget.enableAll();
      }
    }*/
  }
}
