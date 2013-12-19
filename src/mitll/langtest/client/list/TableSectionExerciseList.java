package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CellTable;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.event.ShownEvent;
import com.github.gwtbootstrap.client.ui.event.ShownHandler;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.bootstrap.FlexSectionExerciseList;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Largely assumes this is a flashcard teacher view.
 *
 * User: GO22670
 * Date: 4/23/13
 * Time: 4:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class TableSectionExerciseList extends FlexSectionExerciseList {
  private static final String FLASHCARD = "audio vocabulary practice";
  private static final String USER_PROMPT = "Choose a lesson, preview, and share " + FLASHCARD + " exercises.";

  private static final int FRAME_WIDTH = 1024-50-50;
  private static final int FRAME_HEIGHT = 640-30;
  private static final String FLASHCARDCOPY = FLASHCARD + "copy";
  private static final String TIMEDFLASHCARDCOPY = "timed" + FLASHCARD + "copy";
  private static final String FLASHCARD1 = "Audio Vocabulary Practice";
  private static final String TIMED_FLASHCARD = "Timed AVP";
  private Button flashcardCopy;
  private Button timedFlashcardCopy;
  private TextBox urlInputBox = new TextBox();
  private TextBox urlInputBox2 = new TextBox();
  private int tries = 10;
  private String token = "";
  private int frameHeight = FRAME_HEIGHT;
  private CellTable<? extends ExerciseShell> table;
  private ResponseChoice responseChoice;

  public TableSectionExerciseList(FluidRow secondRow, Panel currentExerciseVPanel, LangTestDatabaseAsync service,
                                  UserFeedback feedback, boolean showTurkToken, boolean showInOrder,
                                  /*boolean showListBox,*/ ExerciseController controller, String instance) {
    super(secondRow, currentExerciseVPanel, service, feedback, showTurkToken, showInOrder, /*showListBox,*/ controller, instance);
    setWidth("100%");
    this.frameHeight = controller.getFlashcardPreviewFrameHeight();
    responseChoice = new ResponseChoice(controller.getProps().getResponseType());
  }

  @Override
  public void clear() {  removeComponents();  }

 // @Override
  protected void addTypeAhead(FlowPanel column) {};

  protected void noSectionsGetExercises(long userID) {
    Map<String, Collection<String>> objectObjectMap = Collections.emptyMap();
    addTableToLayout(objectObjectMap);
  }

    //container.add(responseChoice.getResponseTypeWidget());
    @Override
  protected void addTableWithPager(PagingContainer<? extends ExerciseShell> pagingContainer) {  }

  private Panel getPagerAndTable(CellTable<?> table) {
    return getPagerAndTable(table, table);
  }

  private Panel getPagerAndTable(HasRows table, Widget tableAsPanel) {
    SimplePager pager = new SimplePager(SimplePager.TextLocation.CENTER, false, true);
    // Set the cellList as the display.
    pager.setDisplay(table);
    // Add the pager and list to the page.
    VerticalPanel vPanel = new VerticalPanel();
    vPanel.setBorderWidth(1);
    vPanel.add(tableAsPanel);

    FlowPanel outer = new FlowPanel();
    pager.addStyleName("tableCenter");
    outer.add(pager);
    outer.addStyleName("tableExerciseListHeader");

    vPanel.add(outer);

    return vPanel;
  }

  /**
   * @see #getAsyncTable(java.util.Map, int)
   * @param typeToSection
   * @param numResults
   * @param table
   * @return
   */
  private AsyncDataProvider<Exercise> createProvider(final Map<String, Collection<String>> typeToSection,
                                                     final int numResults, CellTable<Exercise> table) {
    AsyncDataProvider<Exercise> dataProvider = new AsyncDataProvider<Exercise>() {
      @Override
      protected void onRangeChanged(HasData<Exercise> display) {
        final int start = display.getVisibleRange().getStart();
        int end = start + display.getVisibleRange().getLength();
        end = end >= numResults ? numResults : end;
        //System.out.println("createProvider : asking for " + start +"->" + end);
        final int fend = end;
        service.getFullExercisesForSelectionState(typeToSection, start, end, new AsyncCallback<List<Exercise>>() {
          @Override
          public void onFailure(Throwable caught) {
            if (!caught.getMessage().trim().equals("0")) {
              Window.alert("getFullExercisesForSelectionState : Can't contact server.");
            }
          }

          @Override
          public void onSuccess(List<Exercise> result) {
            System.out.println("TableSectionExerciseList.createProvider : onSuccess for " + start + "->" + fend + " got " + result.size());
            updateRowData(start, result);
          }
        });
      }
    };
    setScrollPanelWidth();

    // Connect the table to the data provider.
    dataProvider.addDataDisplay(table);
    dataProvider.updateRowCount(numResults, true);

    return dataProvider;
  }

  /**
   * @see TableSectionExerciseList#addTableToLayout(java.util.Map)
   */
  private Widget getPreviewWidgets() {
    HorizontalPanel previewRow = new HorizontalPanel();
    previewRow.setWidth("100%");
    DOM.setStyleAttribute(previewRow.getElement(), "marginTop", "5px");
    previewRow.addStyleName("tableExerciseListHeader");

    urlInputBox.setText(getFlashcardLink());

    flashcardCopy = makeCopyButton(FLASHCARDCOPY);
    flashcardCopy.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        bindZero(flashcardCopy.getElement(),getFlashcardLink());
      }
    });
    Widget flashcardWidget = getFlashcard(FLASHCARD1, flashcardCopy, urlInputBox, false);
    previewRow.add(flashcardWidget);

    urlInputBox2.setText(getTimedFlashcardLink());

    timedFlashcardCopy = makeCopyButton(TIMEDFLASHCARDCOPY);
    timedFlashcardCopy.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        bindZero(timedFlashcardCopy.getElement(), getTimedFlashcardLink());
      }
    });
    updateFlashcardCopy();

    Widget flashcardWidget2 = getFlashcard(TIMED_FLASHCARD, timedFlashcardCopy, urlInputBox2, true);
    SimplePanel w = new SimplePanel(new Heading(6));
    w.setWidth("10px");
    w.setHeight("2px");
    previewRow.add(w);

    previewRow.add(flashcardWidget2);

    return previewRow;
  }

  /**
   * @see #getPreviewWidgets
   * @param title
   * @param copyButton
   * @param urlInputBox
   * @param timed
   * @return
   */
  private Widget getFlashcard(String title, Button copyButton, TextBox urlInputBox, boolean timed) {
    VerticalPanel titleAndURL = new VerticalPanel();

    Heading titleHeader = new Heading(6, title);
    DOM.setStyleAttribute(titleHeader.getElement(), "marginLeft", "5px");
    DOM.setStyleAttribute(titleHeader.getElement(), "marginTop", "5px");
    DOM.setStyleAttribute(titleHeader.getElement(), "marginBottom", "5px");

    titleAndURL.add(titleHeader);

    FlowPanel urlCopyPreviewContainer = new FlowPanel();
    titleAndURL.add(urlCopyPreviewContainer);
    urlCopyPreviewContainer.addStyleName("url-box");

    Heading flashcard = new Heading(5, "URL");
    flashcard.addStyleName("floatLeft");
    flashcard.addStyleName("shareTitle");
    urlCopyPreviewContainer.add(flashcard);

    // make url input
    urlInputBox.addStyleName("url-input");
    DOM.setStyleAttribute(urlInputBox.getElement(), "fontFamily",
      "\"Lucida Sans Typewriter\", \"Lucida Console\", Monaco, \"Bitstream Vera Sans Mono\",\"Courier New\", Courier, monospace;");
    urlCopyPreviewContainer.add(urlInputBox);

    // make flashcardCopy button
    Panel twoButtons = new FlowPanel();
    twoButtons.addStyleName("inlineBlockStyle");
    twoButtons.add(copyButton);

    Button preview = getPreviewButton(timed);
    twoButtons.add(preview);
    urlCopyPreviewContainer.add(twoButtons);

    return titleAndURL;
  }

  private Button getPreviewButton(final boolean doTimedFlashcard) {
    Button preview = new Button("Preview");
    preview.addStyleName("leftTenMargin");
    preview.setTitle("Preview " + FLASHCARD + "s");

    preview.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        getPreviewModal(doTimedFlashcard);
      }
    });
    return preview;
  }

  private void getPreviewModal(boolean doTimedFlashcard) {
    final Modal modal = new Modal();
    String title = FLASHCARD1 + " Preview";
    if (doTimedFlashcard) title = "Timed " + title;

    modal.setTitle(title);
    modal.setCloseVisible(true);
    modal.setKeyboard(false);

    modal.addShownHandler(new ShownHandler() {
      @Override
      public void onShown(ShownEvent shownEvent) {
        System.out.println("onShown " + shownEvent);
      }
    });

    final Frame frame = new Frame(doTimedFlashcard ? getTimedFlashcardLink() : getFlashcardLink());
    modal.add(frame);
/*    frame.addLoadHandler(new LoadHandler() {
      @Override
      public void onLoad(LoadEvent event) {
        System.out.println("getPreviewModal got load event " + event + " setting focus on frame");
        setFocusOnFrame(frame);
      }
    });*/
    frame.setWidth(FRAME_WIDTH + "px");
    frame.setHeight(frameHeight + "px");
    int modalWidth = FRAME_WIDTH + 50;
    modal.setWidth(modalWidth + "px");
    int heightSlip = 30;
    modal.setMaxHeigth(frameHeight + heightSlip + "px");
    DOM.setStyleAttribute(modal.getElement(), "marginLeft", (-modalWidth / 2) + "px");
    modal.show();
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
  private void doZero() {
    String widgetID = FLASHCARDCOPY;
    zero(GWT.getModuleBaseURL(), widgetID, widgetID + "Feedback");
    registerCallback();
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

  private String getFlashcardLink() {
    return getFlashcardURL() + "#" + token;
  }

  private String getTimedFlashcardLink() {
    return getFlashcardURL() + "&timedGame=true" + "#" + token;
  }

  private String getFlashcardURL() {
    return GWT.getHostPageBaseURL() + "?" + "flashcard" + "=true" +"&responseType="+responseChoice.getResponseType();
  }

  /**
   * @see #doZero()
   * @param moduleBaseURL
   * @param widgetID
   * @param widgetFeedbackID
   */
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

  /**
   * @see mitll.langtest.client.list.section.SectionExerciseList#pushNewSectionHistoryToken()
   * @param historyToken
   */
  protected void setModeLinks(String historyToken) {
    this.token = historyToken;
    updateFlashcardCopy();
  }

  private void setFocusOnFrame(final Frame frame) {
    Scheduler.get().scheduleDeferred(new Command() {
      public void execute() {
        JavaScriptObject cast = frame.getElement().cast();
        IFrameElement iframe = (IFrameElement) cast;
        Document contentDocument = iframe.getContentDocument();
        Element child = DOM.getChild(frame.getElement(), 0);
        com.google.gwt.dom.client.Element record_button1 = contentDocument.getElementById("record_button");
        System.out.println("setFocusOnFrame : got load event child " + child + " record button  " + record_button1);

        if (record_button1 == null) {
          new Timer() {
            @Override
            public void run() {
              if (tries-- > 0)
              setFocusOnFrame(frame);
            }

          }.schedule(1000);
        }
        else {
          iframe.focus();
        }
      }
    });
  }

  private native void registerCallback() /*-{
      $wnd.feedback = $entry(@mitll.langtest.client.list.TableSectionExerciseList::feedback(Ljava/lang/String;));
  }-*/;

  /**
   * Register callback point to this method.
   * @param feedback
   */
  private static void feedback(String feedback) {
    showPopup("Copied " + feedback + " to clipboard.");
  }

  @Override
  protected String getUserPrompt() {
    return USER_PROMPT;
  }

  /**
   * Must be public for GWT.create to work.
   */
  public interface Resources extends CellTable.Resources {
    @Override
    @Source({ CellTable.Style.DEFAULT_CSS, "FlashcardCellTableStyleSheet.css" })
    CellTable.Style cellTableStyle();
  }

  PagingContainer<Exercise> exercisePagingContainer;
  @Override
  protected PagingContainer<? extends ExerciseShell> makePagingContainer() {
    final TableSectionExerciseList outer = this;
    exercisePagingContainer = new PagingContainer<Exercise>(controller, 100) {
      /**
       * @see mitll.langtest.client.list.TableSectionExerciseList#getAsyncTable(java.util.Map, int)
       * @return
       */
      @Override
      public CellTable<Exercise> makeBootstrapCellTable(CellTable.Resources resources) {
        //Resources resources = GWT.create(Resources.class);
        CellTable<Exercise> table = new CellTable<Exercise>(PAGE_SIZE, resources);
        DOM.setStyleAttribute(table.getElement(), "marginBottom", "2px");

        table.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);

        table.setWidth("100%", true);
        table.setHeight("auto");

        // Add a selection model to handle user selection.
        final SingleSelectionModel<Exercise> selectionModel = new SingleSelectionModel<Exercise>();
        table.setSelectionModel(selectionModel);

        outer.addColumnsToTable(table);

        return table;
      }

      @Override
      protected int heightOfCellTableWith15Rows() {
        return 700;
      }
    };
    pagingContainer = exercisePagingContainer;
    return pagingContainer;
  }

  private void addColumnsToTable(CellTable<Exercise> table) {
    TextColumn<Exercise> english = new TextColumn<Exercise>() {
      @Override
      public String getValue(Exercise exercise) {
        if (exercise == null) return "";
        String englishSentence = exercise.getEnglishSentence();
        if (englishSentence == null) {
          List<Exercise.QAPair> englishQuestions = exercise.getEnglishQuestions();
          if (englishQuestions != null && !englishQuestions.isEmpty()) {
            Exercise.QAPair next = englishQuestions.iterator().next();
            return next.getQuestion();
          }
          return "";
        }
        else {
          return "" + englishSentence;
        }
      }
    };
    english.setSortable(true);
    table.addColumn(english, "Word/Expression");

    TextColumn<Exercise> flword = new TextColumn<Exercise>() {
      @Override
      public String getValue(Exercise exercise) {
        if (exercise == null) return "";
        String refSentence = exercise.getRefSentence();
        if (refSentence == null || refSentence.length() == 0) {
          List<Exercise.QAPair> questions = exercise.getForeignLanguageQuestions();
          if (questions != null && !questions.isEmpty()) {
            Exercise.QAPair next = questions.iterator().next();
            return next.getQuestion();
          }
          return "";
        } else {
          return "" + refSentence;
        }
      }
    };
    flword.setSortable(true);
    table.addColumn(flword, controller.getLanguage());

    TextColumn<Exercise> translit = new TextColumn<Exercise>() {
      @Override
      public String getValue(Exercise exercise) {
        return exercise == null ? "" : "" + exercise.getTranslitSentence();
      }
    };
    translit.setSortable(true);
    table.addColumn(translit, "Transliteration");
  }

  /**
   * When we get a history token push, select the exercise type, section, and optionally item.
   * @param typeToSection
   * @param item null is OK
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  @Override

  protected void loadExercises(Map<String, Collection<String>> typeToSection, final String item) {
   // System.out.println("loadExercises " + typeToSection + " and item '" + item + "'");
    addTableToLayout(typeToSection);
  }

  private void addTableToLayout(final Map<String, Collection<String>> typeToSection) {
    service.getNumExercisesForSelectionState(typeToSection, new AsyncCallback<Integer>() {
      @Override
      public void onFailure(Throwable caught) {
        if (!caught.getMessage().trim().equals("0")) {
          Window.alert("getNumExercisesForSelectionState : Couldn't contact server.");
        }
      }

      @Override
      public void onSuccess(Integer result) {
        System.out.println("getNumExercisesForSelectionState.onSuccess: num exercises for " + typeToSection + " is " + result);
        removeComponents();
        FlowPanel p = new FlowPanel();
        p.setWidth("100%");
        p.setHeight("100%");
        Widget asyncTable = getAsyncTable(typeToSection, result);
        p.add(asyncTable);
        add(p);
        FlowPanel container = new FlowPanel();
        container.add(getPreviewWidgets());
        add(container);

        doZero();
      }
    });
  }

  /**
   * @see #addTableToLayout(java.util.Map)
   * @param typeToSection
   * @param numResults
   * @return
   */
  private Widget getAsyncTable(Map<String, Collection<String>> typeToSection,int numResults) {
    Resources resources = GWT.create(Resources.class);

    CellTable<Exercise> table = exercisePagingContainer.makeBootstrapCellTable(resources);
    table.setStriped(true);
    table.setBordered(false);
    table.setWidth("100%");
    table.setHeight("100%");
    table.setRowCount(numResults, true);
    int numRows = pagingContainer.getNumTableRowsGivenScreenHeight();
    numRows = Math.min(10,numRows);
    table.setVisibleRange(0,Math.min(numResults,numRows));
    createProvider(typeToSection, numResults,table);

    this.table = table;

    Panel pagerAndTable = getPagerAndTable(table);
    pagerAndTable.setWidth("100%");
    return pagerAndTable;
  }

  @Override
  public void onResize() {
    setScrollPanelWidth();

    int numRows = pagingContainer.getNumTableRowsGivenScreenHeight();
    numRows = Math.min(10,numRows);

    if (table != null && table.getPageSize() != numRows) {
      System.out.println("onResize : num rows now " + numRows);
      table.setPageSize(numRows);
      System.out.println("onResize : redraw");

      table.redraw();
    }
  }
}
