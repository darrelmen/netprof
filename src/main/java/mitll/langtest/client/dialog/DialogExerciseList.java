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
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.builder.shared.DivBuilder;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.analysis.ButtonMemoryItemContainer;
import mitll.langtest.client.analysis.MemoryItemContainer;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.custom.dialog.ButtonHelper;
import mitll.langtest.client.custom.dialog.SummaryDialogContainer;
import mitll.langtest.client.custom.userlist.TableAndPager;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.list.FacetExerciseList;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.scoring.UserListSupport;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import mitll.langtest.shared.exercise.FilterRequest;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A facet list display of dialogs.
 */
public class DialogExerciseList extends FacetExerciseList<IDialog, IDialog> {
  private final Logger logger = Logger.getLogger("DialogExerciseList");

  private static final String PLEASE_SELECT_A_DIALOG = "Please select a dialog";

  public static final String SUMMARY_DIALOG = "summaryDialog";
  private static final String LISTEN_TO_THE_DIALOG = "Listen to the dialog.";

  public static final String SCORE = "Score";
  private static final String STEP = "Step";
  private static final String DIALOG1 = "Dialog";

  //  private static final String INTERPRETER_PNG = "interpreter.png";
  public static final String INTERP_COLOR = "aliceblue";

  // private static final String ENGLISH = "english";
  public static final String LISTEN = "Listen";
  public static final String CORE_VOCAB = "Core Vocab";

  private static final String DOUBLE_CLICK_TO_LEARN_THE_LIST = "Double click to rehearse a dialog";
  // private static final String YOUR_LISTS1 = "Your Dialogs";
  public static final String DIALOG = "Dialog";
  private static final int MY_LIST_HEIGHT = 420;//500;//530;//560;

  /**
   * Gah - doesn't really work very well as me make the window narrower or wider
   * Gotta turn off the @media thing in bootstrap!
   */
//  private static final int CHOICES_WIDTH = 762;//970;//800;//970;

//  private static final int MAX_LENGTH_ID = 19;
//  private static final int MAX_LENGTH_ID1 = 2 * MAX_LENGTH_ID + 12;
//  private static final int NORMAL_MIN_HEIGHT = 101;// 67;
//  private static final int LANGUAGE_SIZE = 6;

  private Map<Integer, CorrectAndScore> scoreHistoryPerDialog;

  private static final boolean DEBUG = false;

  /**
   *
   */
//  private final ThumbnailChoices thumbnailChoices = new ThumbnailChoices();

  /**
   * @param topRow
   * @param currentExercisePanel
   * @param instanceName
   * @param listHeader
   * @param controller
   * @see DialogViewHelper#getMyListLayout
   */
  DialogExerciseList(Panel topRow, Panel currentExercisePanel,
                     INavigation.VIEWS instanceName,
                     DivWidget listHeader,
                     ExerciseController controller) {
    super(topRow, currentExercisePanel, controller, new ListOptions(instanceName), listHeader, INavigation.VIEWS.DIALOG);
  }

  /**
   * Don't show the show options
   */
  @Override
  protected void showPrevNext() {
    super.showPrevNext();
    setDownloadVisible(false);
  }

  @Override
  protected int getFirstPageSize() {
    return 15;
  }

  @Override
  protected int getChosenPageSize() {
    return 15;
  }

  protected void getTypeToValues(Map<String, String> typeToSelection, int userListID) {
    if (isThereALoggedInUser()) {
      final long then = System.currentTimeMillis();

      if (DEBUG) logger.info("getTypeToValues req " + typeToSelection);

      FilterRequest filterRequest = getFilterRequest(userListID, getPairs(typeToSelection));
      filterRequest.setUserID(controller.getUser());
      filterRequest.setProjID(controller.getProjectID());
      if (DEBUG) logger.info("getTypeToValues filterRequest " + filterRequest);
      controller.getDialogService().getTypeToValues(filterRequest, getTypeToValuesCallback(typeToSelection, then));
    }
  }

  @Override
  protected void addPageSize(DivWidget footer) {
  }

  /**
   * @param typeToSection
   * @param prefix
   * @param exerciseID
   * @param request
   */
  protected void getExerciseIDs(Map<String, Collection<String>> typeToSection,
                                String prefix,
                                int exerciseID,
                                ExerciseListRequest request) {
    waitCursorHelper.scheduleWaitTimer();

    if (DEBUG) {
      logger.info("getExerciseIDs " +
          "\n\trequest " + request +
          "\n\t ex     " + exerciseID + " type " + typeToSection);
    }

    if (controller.getUser() > 0) {
      controller.getDialogService().getDialogs(request,
          new SetExercisesCallback("" + "_" + typeToSection.toString(), prefix, exerciseID, request));
    }
  }

  @Override
  protected void setScores(ExerciseListWrapper<IDialog> result) {
    super.setScores(result);
    scoreHistoryPerDialog = result.getScoreHistoryPerExercise();
  }

  @Override
  protected void getFullExercises(Collection<Integer> visibleIDs,
                                  int currentReq,
                                  Collection<Integer> requested,
                                  List<IDialog> alreadyFetched) {
    if (prev != null) {
      prev.setEnabled(pagingContainer.hasPrevPage());
      next.setEnabled(pagingContainer.hasNextPage());
    }
    showDialogs(visibleIDs, getInOrder());
  }


  private void showDialogs(Collection<Integer> visibleIDs, List<IDialog> exercises) {
    List<IDialog> toShow = exercises.stream().filter(iDialog -> visibleIDs.contains(iDialog.getID())).collect(Collectors.toList());
    sortDialogs(toShow, visibleIDs);
    showExercisesForCurrentReq(toShow, incrReq());
  }

  private void sortDialogs(List<IDialog> toShow, Collection<Integer> visibleIDs) {
    List<Integer> ordered = new ArrayList<>(visibleIDs);
    toShow.sort(Comparator.comparingInt(o -> ordered.indexOf(o.getID())));
  }

  @Override
  protected void goGetNextPage() {
  }

  /**
   * replace with something very close to dialog editor list.
   *
   * @param result
   * @param reqID
   * @param exerciseContainer
   * @see #reallyShowExercises(Collection, int)
   */
  protected void populatePanels(Collection<IDialog> result, int reqID, DivWidget exerciseContainer) {
    //  long then = System.currentTimeMillis();
    //exerciseContainer.add(showProjectChoices(result));
    myContainer = showDialogs(result, exerciseContainer);
    //  long now = System.currentTimeMillis();
  }

  private MemoryItemContainer<IDialog> myContainer;

  private ButtonMemoryItemContainer<IDialog> showDialogs(Collection<IDialog> result, DivWidget left) {
//
//    Heading w = new Heading(3, "Please select a dialog");
//    w.getElement().getStyle().setMarginTop(0, Style.Unit.PX);
//    w.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
//    left.add(w);

    ButtonMemoryItemContainer<IDialog> myLists = new ReadOnlyDialogContainer();
    Panel tableWithPager = myLists.getTableWithPager(result);

//    myLists.setWidth(900, true);

//    tableWithPager.getElement().getStyle().setProperty("minWidth", "900px");


    tableWithPager.addStyleName("cardBorderShadow");

    new TooltipHelper().createAddTooltip(tableWithPager, DOUBLE_CLICK_TO_LEARN_THE_LIST, Placement.BOTTOM);

   new TableAndPager().addPagerAndHeader(tableWithPager, PLEASE_SELECT_A_DIALOG, left);

    tableWithPager.setHeight(MY_LIST_HEIGHT + "px");
    left.add(tableWithPager);

    DivWidget bb = new DivWidget();
    //  bb.setWidth("900px");
    DivWidget buttons = buttonContainer;

    buttons.add(share = getShare(myLists));
    buttons.add(getListenButton(myLists));

    bb.add(buttons);
    left.add(bb);

    return myLists;
  }

  public int getSelectedDialog() {
    return myContainer.getSelectedItem();
  }

  protected Button share;
//
//      , next, prev;

  private ButtonHelper<IDialog> buttonHelper = new ButtonHelper<IDialog>() {
    @Override
    protected String getName() {
      return DIALOG1;
    }
  };

  /**
   * @param container
   * @return
   * @see #showDialogs(Collection, DivWidget)
   */
 /* @NotNull
  private DivWidget getButtons(ButtonMemoryItemContainer<IDialog> container) {
    DivWidget buttons = buttonHelper.getButtonContainer();
    //buttons.addStyleName("floatRight");
    buttons.addStyleName("floatLeft");
    buttons.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);

    {
      buttons.add(prev = buttonHelper.getSuccessButton(container, "Prev"));
      prev.setType(ButtonType.INFO);
      prev.addClickHandler(event -> gotClickOnPrev());
    }
    {
      buttons.add(next = buttonHelper.getSuccessButton(container, "Next"));
      next.setType(ButtonType.INFO);
      next.addClickHandler(event -> gotClickOnNext());
      next.addStyleName("rightFiveMargin");
    }
    prev.setEnabled(pagingContainer.hasPrevPage());
    next.setEnabled(pagingContainer.hasNextPage());

    buttons.add(share = getShare(container));
    buttons.add(getListenButton(container));

    return buttons;
  }

  private void gotClickOnPrev() {
    if (pagingContainer.hasPrevPage()) {
      pagingContainer.prevPage();
    } else logger.info("has prev is false");
  }

  private void gotClickOnNext() {
    if (pagingContainer.hasNextPage()) {
      pagingContainer.nextPage();
    } else logger.info("has next is false");
  }
*/
  @NotNull
  private Button getListenButton(ButtonMemoryItemContainer<IDialog> container) {
    Button learn = buttonHelper.getSuccessButton(container, LISTEN);
    //learn.setType(ButtonType.INFO);


    learn.addClickHandler(event -> showLearnList(container));


    buttonHelper.addTooltip(learn, LISTEN_TO_THE_DIALOG);
    return learn;
  }

  private void showLearnList(ButtonMemoryItemContainer<IDialog> container) {
    controller.getNavigation().showDialogIn(getItemID(container), INavigation.VIEWS.LISTEN);
  }

  private int getItemID(ButtonMemoryItemContainer<IDialog> container) {
    if (container == null) return -1;
    else {
      IDialog currentSelection = container.getCurrentSelection();
      return currentSelection == null ? -1 : currentSelection.getID();
    }
  }

  /**
   * @param container
   * @return
   * @see #getButtons
   */
  private Button getShare(ButtonMemoryItemContainer<IDialog> container) {
    return buttonHelper.getShare(container);
  }

  @NotNull
  DivWidget getCommonButtonContainer(ButtonMemoryItemContainer<IDialog> container) {
    DivWidget buttons = buttonHelper.getCommonButtonContainer();
    buttons.add(getShare(container));
    return buttons;
  }

  /**
   * @see #showDialogs(Collection, DivWidget)
   */
  private class ReadOnlyDialogContainer extends SummaryDialogContainer<IDialog> {
    /**
     * @see #showDialogs(Collection, DivWidget)
     */
    ReadOnlyDialogContainer() {
      super(DialogExerciseList.this.controller, SUMMARY_DIALOG, 15);
    }

    @Override
    protected void setMaxWidth() {
      //  table.getElement().getStyle().setProperty("maxWidth", getMaxTableWidth() + "px");
    }

    @Override
    protected int getNumTableRowsGivenScreenHeight() {
      return 15;
    }

    @Override
    protected void addColumnsToTable() {
      super.addColumnsToTable();
      addScore(getList());
      addScoreStep(getList(), getMaxLengthId());
    }

    protected int getMaxLengthId() {
      return 25;
    }

    /**
     * @param list
     * @param maxLength
     */
    void addScoreStep(List<IDialog> list, int maxLength) {
      Column<IDialog, SafeHtml> userCol = getScoreStepColumn(maxLength);
      table.setColumnWidth(userCol, getIdWidth() + "px");
      addColumn(userCol, new TextHeader(STEP));
      table.addColumnSortHandler(getScoreStepSorter(userCol, list));
    }

    private Column<IDialog, SafeHtml> getScoreStepColumn(int maxLength) {
      return getTruncatedCol(maxLength, this::getScoreStepValue);
    }

    private String getScoreStepValue(IDialog thing) {
      return getScoreStep(thing);
    }

    private ColumnSortEvent.ListHandler<IDialog> getScoreStepSorter(Column<IDialog, SafeHtml> englishCol,
                                                                    List<IDialog> dataList) {
      ColumnSortEvent.ListHandler<IDialog> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
      columnSortHandler.setComparator(englishCol, this::getScoreStepCompare);
      return columnSortHandler;
    }

    private int getScoreStepCompare(IDialog o1, IDialog o2) {
      int i = getScoreStep(o1).compareTo(getScoreStep(o2));
      return i == 0 ? getScoreCompare(o1, o2) : i;
    }

    @Override
    public void gotClickOnItem(final IDialog user) {
      super.gotClickOnItem(user);
      setShareHREF(user);
    }

    void setShareHREF(IDialog user) {
      if (user != null) {
        setShareButtonHREF();
      }
    }

    private void setShareButtonHREF() {
      share.setHref(getMailTo());
    }

    @NotNull
    String getMailTo() {
      IDialog currentSelection = getCurrentSelection();
      return new UserListSupport(controller)
          .getMailToDialog(currentSelection.getID(), currentSelection.getForeignLanguage());
    }

    void addScore(List<IDialog> list) {
      Column<IDialog, SafeHtml> userCol = getClickable(this::getScoreVal);
      table.setColumnWidth(userCol, getIdWidth() + "px");
      addColumn(userCol, new TextHeader(SCORE));
      table.addColumnSortHandler(getScoreSorter(userCol, list));
    }

    private String getScoreVal(IDialog thing) {
      int v = getScore(thing);
      return v == 0 ? "" : "" + v;
    }

    private int getScore(IDialog thing) {
      CorrectAndScore correctAndScore = scoreHistoryPerDialog.get(thing.getID());
      return correctAndScore == null ? 0 : correctAndScore.getPercentScore();
    }

    private String getScoreStep(IDialog thing) {
      CorrectAndScore correctAndScore = scoreHistoryPerDialog.get(thing.getID());
      return correctAndScore == null ? "" : getViewForScore(correctAndScore).toString();
    }

    private ColumnSortEvent.ListHandler<IDialog> getScoreSorter(Column<IDialog, SafeHtml> englishCol,
                                                                List<IDialog> dataList) {
      ColumnSortEvent.ListHandler<IDialog> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
      columnSortHandler.setComparator(englishCol, this::getScoreCompare);
      return columnSortHandler;
    }

    private int getScoreCompare(IDialog o1, IDialog o2) {
      int i = Integer.compare(getScore(o1), getScore(o2));
      return i == 0 ? o1.getForeignLanguage().compareTo(o2.getForeignLanguage()) : i;
    }

    @NotNull
    @Override
    protected ListOptions getListOptions() {
      return super.getListOptions().setShowPager(false);
    }

    @Override
    protected boolean hasDoubleClick() {
      return true;
    }

    /**
     * @param selected
     * @see SimplePagingContainer#addDoubleClick
     */
    @Override
    protected void gotDoubleClickOn(IDialog selected) {
      //  logger.info("gotDoubleClickOn got double click on " + selected);
      // editList(getCurrentSelection());
      gotClickOnDialog(getCurrentSelection());
    }
  }

/*
  private Section showProjectChoices(Collection<IDialog> result) {
    // logger.info("showProjectChoices choices # = " + result.size() + " : nest level " + nest);
    final Section section = thumbnailChoices.getScrollingSection();

    {
      final Container flags = new Container();
      flags.setWidth(CHOICES_WIDTH + "px");
      //     flags.getElement().getStyle().setProperty("minWidth", "800px");
      flags.add(addFlags(result));
      section.add(flags);
    }

    return section;
  }
*/

/*
  private Thumbnails addFlags(Collection<IDialog> dialogs) {
    Thumbnails current = new Thumbnails();
    current.getElement().getStyle().setMarginBottom(70, Style.Unit.PX);

    dialogs
        .forEach(dialog -> {
//          logger.info("Got " + dialog.getID() + " " + dialog.getEnglish() + " " + dialog.getAttributes());
          Panel langIcon = getImageAnchor(dialog);
          if (langIcon != null) {
            current.add(langIcon);
          }
        });

    return current;
  }
*/

  /**
   * Use default image if none is associated...
   *
   * @param dialog
   * @return
   */
/*  private Panel getImageAnchor(IDialog dialog) {
    Thumbnail thumbnail = thumbnailChoices.getThumbnail();

    // logger.info("show image " + imageRef);
    String imageRef = dialog.getImageRef();
    if (imageRef == null || imageRef.isEmpty()) {
      imageRef = getDefaultImage();
    }
    PushButton button = new PushButton(getFlag(imageRef));
    button.addClickHandler(clickEvent -> gotClickOnDialog(dialog));
    thumbnail.add(button);

    {
      Map<String, String> props = new HashMap<>(dialog.getUnitToValue());
      dialog.getAttributes().forEach(attr ->
      {
        props.put(getProperty(attr), attr.getValue());
        if (isTitle(attr)) {
          props.put(ENGLISH, dialog.getEnglish());
        }
      });

      // props.put("type", dialog.getKind().toString());

      thumbnailChoices.addPopover(button, props, Placement.BOTTOM);
    }

    {
      DivWidget horiz = new DivWidget();
      setMinHeight(horiz, NORMAL_MIN_HEIGHT);
      horiz.add(getContainerWithButtons(dialog));

      thumbnail.add(horiz);
    }
    return thumbnail;
  }*/

/*
  @NotNull
  private String getDefaultImage() {
    return "langtest/cc/" + INTERPRETER_PNG;
  }
*/
  private void setMinHeight(UIObject horiz1, int normalMinHeight) {
    horiz1.getElement().getStyle().setProperty("minHeight", normalMinHeight + "px"); // so they wrap nicely
  }
/*

  private String getProperty(ExerciseAttribute attr) {
    return isTitle(attr) ? "title" : attr.getProperty();
  }
*/

/*
  private boolean isTitle(ExerciseAttribute attr) {
    return attr.getProperty().equals(DialogMetadata.FLTITLE.toString().toLowerCase());
  }
*/
/*

  @NotNull
  private com.google.gwt.user.client.ui.Image getFlag(String cc) {
    Image image = new Image(cc);
    image.setHeight("150px");
    image.setWidth("150px");
    return image;
  }
*/

/*  @NotNull
  private DivWidget getContainerWithButtons(IDialog dialog) {
    DivWidget container = new DivWidget();

    //  logger.info("dialog "+ dialog.getForeignLanguage() + " " + dialog.getEnglish());

    {
      String truncate = thumbnailChoices.truncate(dialog.getForeignLanguage(), MAX_LENGTH_ID);
      Heading label = getLabel(truncate);
      label.getElement().getStyle().setMarginTop(5, Style.Unit.PX);
      label.getElement().getStyle().setMarginBottom(0, Style.Unit.PX);
      container.add(label);
    }

    {
      String english = thumbnailChoices.truncate(dialog.getOrientation(), MAX_LENGTH_ID1);
      Heading label1 = getLabel(english);
      label1.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);
      label1.getElement().getStyle().setMarginTop(5, Style.Unit.PX);
      setMinHeight(label1, 40);

      container.add(label1);
    }

    container.add(getEmoticonRow(dialog));

    container.setWidth("100%");
    container.addStyleName("floatLeft");

    // no more distinction
    // container.getElement().getStyle().setBackgroundColor(dialog.getKind() == DialogType.DIALOG ? DIALOG_COLOR : INTERP_COLOR);
    container.getElement().getStyle().setBackgroundColor(INTERP_COLOR);
    return container;
  }*/

  /**
   * Show score and mode
   *
   * @param dialog
   * @return
   */
/*
  @NotNull
  private DivWidget getEmoticonRow(IDialog dialog) {
    Emoticon overallSmiley = getEmoticon(dialog);
    DivWidget smiley = new DivWidget();
    smiley.addStyleName("inlineFlex");

    smiley.add(overallSmiley);
    CorrectAndScore correctAndScore = scoreHistoryPerDialog.get(dialog.getID());
    if (correctAndScore != null) {
      INavigation.VIEWS viewForScore = getViewForScore(correctAndScore);
      if (viewForScore != INavigation.VIEWS.NONE) {
        HTML w = new HTML(viewForScore.toString());
        w.addStyleName("leftFiveMargin");
        w.getElement().getStyle().setMarginTop(2, Style.Unit.PX);
        smiley.add(w);
      }
    }
    return smiley;
  }
*/
  @NotNull
  private INavigation.VIEWS getViewForScore(CorrectAndScore correctAndScore) {
    String name = correctAndScore.getPath().toUpperCase();
    try {
      return INavigation.VIEWS.valueOf(name);
    } catch (IllegalArgumentException e) {
      logger.warning("couldn't parse " + name);
      return INavigation.VIEWS.NONE;
    }
  }

/*
  @NotNull
  private Emoticon getEmoticon(IDialog dialog) {
    Emoticon overallSmiley = new Emoticon();

    {
      CorrectAndScore orDefault = scoreHistoryPerDialog.getOrDefault(dialog.getID(), new CorrectAndScore(0F, null));
      int percentScore = orDefault.getPercentScore();
      if (percentScore > 0) {
        // logger.info("For " + dialog.getID() + " score " +percentScore);
        double score = Integer.valueOf(percentScore).doubleValue() / 100D;
        // logger.info("overallSmiley For " + dialog.getID() + " score " +score);
        overallSmiley.setEmoticon(score, controller.getLanguageInfo());
      } else {
        overallSmiley.setVisible(false);
      }
      //  else overallSmiley.setEmoticon(0.5,controller.getLanguageInfo());

      styleAnimatedSmiley(overallSmiley);
    }
    return overallSmiley;
  }
*/

  /**
   * TODO : make this a css entry
   *
   * @param overallSmiley
   */
/*
  private void styleAnimatedSmiley(Emoticon overallSmiley) {
    overallSmiley.setWidth(24 + "px");
    overallSmiley.setHeight(24 + "px");
    overallSmiley.getElement().getStyle().setPosition(Style.Position.RELATIVE);
  }

  @NotNull
  private Heading getLabel(String name) {
    return thumbnailChoices.getChoiceLabel(LANGUAGE_SIZE, name, false);
  }
*/

  /**
   * Will push be bad? Will other parts wake up?
   * Maybe replace item?
   *
   * @param dialog
   */
  private void gotClickOnDialog(IDialog dialog) {
    controller.getNavigation().showDialogIn(dialog.getID(), INavigation.VIEWS.LISTEN);
  }
}
