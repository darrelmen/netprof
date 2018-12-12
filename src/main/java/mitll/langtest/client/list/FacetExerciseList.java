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

package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ListItem;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.base.UnorderedList;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.Range;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.banner.QuizHelper;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.download.DownloadEvent;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.client.scoring.RefAudioGetter;
import mitll.langtest.client.scoring.ScoreProgressBar;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static mitll.langtest.client.dialog.ExceptionHandlerDialog.getExceptionAsString;
import static mitll.langtest.client.scoring.ScoreFeedbackDiv.FIRST_STEP;
import static mitll.langtest.client.scoring.ScoreFeedbackDiv.SECOND_STEP;

/**
 * A facet list display - facets on the left that when you click on them
 * changes the content on the right.
 *
 * @param <T>
 * @param <U>
 */
public abstract class FacetExerciseList<T extends CommonShell & Scored, U extends HasID & CommonShell>
    extends HistoryExerciseList<T, U>
    implements ShowEventListener, ChoicesContainer {
  private Logger logger = Logger.getLogger("FacetExerciseList");

  private static final String RECORDED = "Recorded";

  private static final String PRACTICED = " practiced.";
  private static final String NO_SCORE = "No score yet.";
  private static final String PERFECT = "100% Perfect!";

  /**
   * @see #showAvgScore
   */
  private static final String AVG_SCORE = " avg. score";
  /**
   *
   */
  static final boolean DEBUG_STALE = true;
  private static final boolean DEBUG = false;
  private static final boolean DEBUG_CHOICES = false;
  private static final boolean DEBUGSCORE = false;
  private static final boolean DEBUG_SET_HISTORY = false;

  private static final String PAGE_SIZE_SELECTED = "pageSizeSelected";

  protected static final String GETTING_TYPE_VALUES = "getting type->values";

  /**
   *
   */
  private static final String NONE_PRACTICED_YET = "None practiced yet.";
  /**
   * @see #showNumberPracticed
   */
  private static final String ALL_PRACTICED = "All practiced!";

  /**
   *
   */
  public static final String LISTS = "Lists";

  /**
   * @see #addPageSize
   */
  private static final String PAGE_SIZE_HEADER = "View";

  /**
   * Smaller on a laptop.
   *
   * @seex mitll.langtest.client.banner.NewLearnHelper#getMyListLayout
   */
  private static final int FIVE_PAGE_SIZE = Window.getClientHeight() < 1080 ? 4 : 5;
  private static final int FIRST_PAGE_SIZE = FIVE_PAGE_SIZE;
  private static final List<Integer> PAGE_SIZE_CHOICES = Arrays.asList(1, FIVE_PAGE_SIZE, 10, 25);
  private static final String ITEMS_PAGE = " items/page";

  private static final int TOTAL = 28;
  private static final int CLOSE_TO_END = 2;

  /**
   * @see #getShowLess
   */
  private static final String SHOW_LESS = "<i>View fewer</i>";

  protected static final String ANY = "Any";
  private static final String MENU_ITEM = "menuItem";

  /**
   * @see #FacetExerciseList(Panel, Panel, ExerciseController, ListOptions, DivWidget, INavigation.VIEWS)
   * @see #setProgressVisible
   */
  protected ProgressBar practicedProgress, scoreProgress;
  private final DivWidget pagerAndSortRow;

  private List<String> typeOrder;
  private final Panel sectionPanel;
  private final DownloadHelper downloadHelper;
  private Panel tableWithPager;

  /**
   * for now only single selection
   */
  private Map<String, String> typeToSelection = new HashMap<>();
  /**
   * @see #addChoicesForType
   */
  private final Map<String, Boolean> typeToShowAll = new HashMap<>();
  private List<String> rootNodesInOrder = new ArrayList<>();
  private int freqid = 0;
  private DivWidget sortBox;
  private DivWidget pageSizeContainer;
  private Panel typeOrderContainer;

  /**
   * @see #setProgressBarScore
   * @see #setScore
   * @see #showAvgScore
   */
  private final Map<Integer, Float> exerciseToScore = new HashMap<>();
  private final Map<Integer, U> fetched = new ConcurrentHashMap<>();
  private final INavigation.VIEWS views;
  private final String pageSizeSelected;

  /**
   * @param secondRow             add the section panel to this row
   * @param currentExerciseVPanel
   * @param controller
   * @param listOptions
   * @param listHeader
   * @param views
   * @seex mitll.langtest.client.custom.content.NPFlexSectionExerciseList#NPFlexSectionExerciseList
   */
  protected FacetExerciseList(Panel secondRow,
                              Panel currentExerciseVPanel,
                              ExerciseController controller,
                              ListOptions listOptions,
                              DivWidget listHeader,
                              INavigation.VIEWS views) {
    super(currentExerciseVPanel, controller, listOptions);
    this.views = views;
    this.pageSizeSelected = PAGE_SIZE_SELECTED + "_" + views.toString();
    sectionPanel = new DivWidget();
    sectionPanel.getElement().setId("sectionPanel_" + getInstance());
    sectionPanel.addStyleName("rightFiveMargin");

    secondRow.add(sectionPanel);
    setUnaccountedForVertical(0);

    downloadHelper = new DownloadHelper();

    DivWidget breadRow = getBreadcrumbRow();
    // Todo : add this?
    // breadRow.add(new HTML("breadcrumbs go here"));
    {
      listHeader.getElement().setId("listHeader");
      listHeader.add(breadRow);
      listHeader.add(pagerAndSortRow = getPagerAndSort(controller));
      listHeader.setWidth("100%");
    }
    // addPrevNextPage(footer);
    finished = true;

    // TODO : don't do it - will keep around reference to dead components.
    // so for instance if in TwoColumnExercisePanel there's an addList, removeList, newList
    LangTest.EVENT_BUS.addHandler(DownloadEvent.TYPE, authenticationEvent -> downloadHelper.showDialog(controller.getHost()));
  }

  @NotNull
  private DivWidget getBreadcrumbRow() {
    DivWidget breadRow = new DivWidget();
    breadRow.setWidth("100%");
    breadRow.getElement().setId("breadRow");
    //    breadRow.addStyleName("floatLeftList");

    {
      breadRow.add(practicedProgress = new ProgressBar(ProgressBarBase.Style.DEFAULT));
      breadRow.add(scoreProgress = new ProgressBar(ProgressBarBase.Style.DEFAULT));
      styleProgressBarContainer(practicedProgress);
      practicedProgress.addStyleName("floatLeft");
      styleProgressBarContainer(scoreProgress);
      scoreProgress.addStyleName("floatRight");
    }
    return breadRow;
  }

  public void hideSectionPanel() {
    sectionPanel
        .getParent()
        .getParent()
        .setVisible(false);


    Widget parent = sectionPanel
        .getParent()
        .getParent()
        .getParent();

//    logger.info("parent is " + parent.getElement().getId());
    Iterator<Widget> iterator = ((HasWidgets) parent).iterator();
    /*Widget sibling =*/
    iterator.next();
//    logger.info("sibling is " + sibling.getElement().getId());
//
    Widget next = iterator.next();
    //   logger.info("next is " + next.getElement().getId());
    next.getElement().getStyle().setMarginRight(100, Style.Unit.PX);
  }

  /**
   * @param controller
   * @return
   * @see #FacetExerciseList
   */
  @NotNull
  protected DivWidget getPagerAndSort(ExerciseController controller) {
    DivWidget pagerAndSort = new DivWidget();
    pagerAndSort.getElement().setId("pagerAndSort");
    pagerAndSort.addStyleName("inlineFlex");
    pagerAndSort.setWidth("100%");

    pagerAndSort.add(tableWithPager);

    {
      tableWithPager.addStyleName("floatLeft");
      tableWithPager.setWidth("100%");

      tableWithPager.getElement().getStyle().setProperty("minWidth", "250px");
    }

    {
      // better name for primary and alternate choices
      DivWidget widgets = new DivWidget();
      widgets.addStyleName("topFiveMargin");

      {
        ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
        boolean isMandarin = projectStartupInfo != null && projectStartupInfo.getLanguageInfo() == Language.MANDARIN;
        boolean shouldSwap = projectStartupInfo != null && projectStartupInfo.isShouldSwap();
        widgets.add(new DisplayMenu(controller.getStorage(), this, isMandarin, shouldSwap).getRealViewMenu());
      }

      pagerAndSort.add(widgets);
    }

    addPageSize(pagerAndSort);

    pagerAndSort.add(this.sortBox = addSortBox());

    return pagerAndSort;
  }

  private void styleProgressBarContainer(ProgressBar progressBar) {
    Style style = progressBar.getElement().getStyle();
    style.setMarginTop(5, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);

    style.setHeight(25, Style.Unit.PX);
    style.setFontSize(16, Style.Unit.PX);

    progressBar.setWidth("49%");
    progressBar.setVisible(false);
  }

  /**
   * page size has a label, and the size choices...
   *
   * @param footer
   * @return
   * @see #FacetExerciseList
   */
  private void addPageSize(DivWidget footer) {
    pageSizeContainer = new DivWidget();
    {
      pageSizeContainer.addStyleName("floatRight");
      pageSizeContainer.addStyleName("rightFiveMargin");
      pageSizeContainer.addStyleName("inlineFlex");

      footer.add(pageSizeContainer);
    }

    {
      HTML label = new HTML(PAGE_SIZE_HEADER);
      label.addStyleName("floatLeft");
      label.addStyleName("topFiveMargin");
      label.addStyleName("rightFiveMargin");
      pageSizeContainer.add(label);
    }

    ListBox pagesize = getPageSizeChoices();
    pageSizeContainer.add(pagesize);
  }

  @NotNull
  private ListBox getPageSizeChoices() {
    ListBox pagesize = new ListBox();
    pagesize.getElement().getStyle().clearWidth();
    pagesize.addStyleName("floatLeft");

    getPageSizeChoiceValues().forEach(num -> pagesize.addItem(num + ITEMS_PAGE, "" + num));
    pagesize.setItemSelected(getChosenPageIndex(), true);
    pagesize.addChangeHandler(event -> onPageSizeChange(pagesize));

    return pagesize;
  }

  List<Integer> getPageSizeChoiceValues() {
    return PAGE_SIZE_CHOICES;
  }

  private int getChosenPageIndex() {
    int pageIndex = getPageIndex();
//    logger.info("getChosenPageIndex pageIndex " + pageIndex);
    return pageIndex == -1 ? getPageSizeChoiceValues().indexOf(getFirstPageSize()) : pageIndex;
  }

  protected int getFirstPageSize() {
    return FIRST_PAGE_SIZE;
  }

  private int getChosenPageSize() {
    return getPageSizeChoiceValues().get((getChosenPageIndex()));
  }

  protected int getPageIndex() {
    return controller.getStorage().getInt(pageSizeSelected);
  }

  private void onPageSizeChange(ListBox pagesize) {
    pagesize.setFocus(false);
    String value = pagesize.getValue();
    int i = Integer.parseInt(value);
    pagingContainer.setPageSize(i);
    controller.getStorage().setInt(pageSizeSelected, pagesize.getSelectedIndex());
    scrollToTop();
  }

  private void scrollToTop() {
    Window.scrollTo(0, 0);
  }

  private final boolean finished;
  private ListBox sortBoxReally;
  private ListSorting<T, U> listSorting;

  /**
   * @return
   * @see #getPagerAndSort(ExerciseController)
   */
  @NotNull
  private DivWidget addSortBox() {
    DivWidget w = new DivWidget();
    w.addStyleName("floatRight");

    HTML w1 = new HTML("Sort");
    w1.addStyleName("topFiveMargin");
    w1.addStyleName("rightFiveMargin");
    w1.addStyleName("floatLeft");
    w.addStyleName("inlineFlex");

    w.add(w1);

    listSorting = new ListSorting<>(this, views);
    sortBoxReally = listSorting.getSortBox();
    w.add(sortBoxReally);
    return w;
  }

  @Override
  protected void addMinWidthStyle(Panel leftColumn) {
  }

  protected void addTableWithPager(SimplePagingContainer<?> pagingContainer) {
    tableWithPager = pagingContainer.getTableWithPager(listOptions);
  }

  /**
   * @return
   * @see mitll.langtest.client.list.PagingExerciseList#addComponents
   */
  protected ClickablePagingContainer<T> makePagingContainer() {
    pagingContainer =
        new ClickablePagingContainer<T>(controller) {
          public void gotClickOnItem(T e) {
          }

          protected void addTable(Panel column) {
          }

          @Override
          protected void addColumnsToTable(boolean sortEnglish) {
          }

          /**
           * @see SimplePagingContainer#configureTable
           * @param newRange
           */
          @Override
          protected void gotRangeChanged(final Range newRange) {
            if (DEBUG) logger.info("gotRangeChanged event for " + newRange);
            FacetExerciseList.this.gotRangeChanged();
          }

          @Override
          protected int getNumTableRowsGivenScreenHeight() {
            int pageSize = getChosenPageSize();
            //     logger.info("getNumTableRowsGivenScreenHeight " + pageSize + " vs " + numToShow);
            return pageSize;
          }
        };

//    if (logger == null) logger = Logger.getLogger("FacetExerciseList");
//    logger.info("makePagingContainer -");
    return pagingContainer;
  }

  protected void gotRangeChanged() {
    askServerForExercise(-1);
  }

  /**
   * @see HistoryExerciseList#noSectionsGetExercises
   */
  public boolean getExercises() {
    addWidgets();
    return false;
  }

  /**
   * remove list selection if not for this instance.
   *
   * @seex mitll.langtest.client.custom.content.FlexListLayout#doInternalLayout(UserList, String)
   * @see #getExercises
   */
  @Override
  public void addWidgets() {
    sectionPanel.clear();
    addTypeAhead(sectionPanel);
    sectionPanel.add(typeOrderContainer = getWidgetsForTypes());
    SelectionState selectionState = getSelectionState(getHistoryToken());

    if (getStartupInfo() != null) {
      maybeSwitchProject(selectionState, getStartupInfo().getProjectid());
    }

    INavigation.VIEWS instance = selectionState.getView();
    if (instance != INavigation.VIEWS.NONE && instance != getInstance()) {
      logger.info("addWidgets selection '" + instance + "' != " + getInstance());// + " so removing " + remove);
    }

    restoreUIState(selectionState);
  }

  /**
   * TODO : don't do two requests for recording views
   *
   * @param prefix
   * @return
   */
  @Override
  protected ExerciseListRequest getExerciseListRequest(String prefix) {
//     String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("getExerciseListRequest for " + prefix));
//   logger.info("logException stack " + exceptionAsString);
    boolean context = views.isContext();

    if (context) logger.warning("\n\n\ngetExerciseListRequest view " + views);
    return super.getExerciseListRequest(prefix)
        .setAddFirst(false)
        .setOnlyExamples(context);
  }

  /**
   * Assume for the moment that the first type has the largest elements... and every other type nests underneath it.
   *
   * @return
   * @see #addWidgets
   */
  private Panel getWidgetsForTypes() {
    final UnorderedList container = new UnorderedList();
    container.getElement().setId("typeOrderContainer");
    container.getElement().getStyle().setMarginBottom(50, Style.Unit.PX);//"bottomFiveMargin");
    getTypeOrder();
    return container;
  }

  private void getTypeOrder() {
    ProjectStartupInfo projectStartupInfo = getStartupInfo();
    if (projectStartupInfo == null) logger.warning("no project startup info?");
    else {
      typeOrder = getTypeOrderSimple();
      //logger.info("\n\n\ngetTypeOrder type order " + typeOrder);
      this.rootNodesInOrder = new ArrayList<>(typeOrder);
      this.rootNodesInOrder.retainAll(projectStartupInfo.getRootNodes());
    }
  }

  /**
   * @param typeToSelection
   * @see #gotFilterResponse
   */
  private void setTypeToSelection(Map<String, String> typeToSelection) {
    this.typeToSelection = typeToSelection;
  }

  /**
   * Populate the filter choices on the left.
   * <p>
   * structure
   * nav
   * header (optional)
   * ul
   * li - each dimension
   * span - header
   * ul - choices in each dimension
   * choice - with span (qty)
   * <p>
   * optional collapse although it's really nice
   * <p>
   * breadcrumb with choices
   * <p>
   * rules -
   * click on header, removes selection of choice
   * click on choice, down select for that choice
   * <p>
   * ul
   * li - each dimension
   * span - header
   * ul - choices in each dimension
   * li - choice - with span (qty)
   *
   * @see #getTypeToValues
   * @see #gotFilterResponse
   */
  private void addFacetsForReal(Map<String, Set<MatchInfo>> typeToValues, Panel nav) {
    if (DEBUG) {
      logger.info("addFacetsForReal" +
          "\n\t# root nodes = " + rootNodesInOrder.size() + " " + rootNodesInOrder +
          "\n\ttype->distinct " + typeToValues.keySet() +
          "\n\ttype->sel      " + typeToSelection);
    }

    // nav -
    //   ul
    UnorderedList allTypesContainer = new UnorderedList(); //ul
    nav.clear();
    nav.add(allTypesContainer);

    for (String type : rootNodesInOrder) {
      // nav
      //  ul
      //   li - dimension
      boolean hasChoices = typeToValues.containsKey(type);

      if (hasChoices) {
        // logger.info("addDim " + type + " = " + keys);


        ListItem liForDimensionForType = getTypeContainer(type, typeToSelection.containsKey(type));
        allTypesContainer.add(liForDimensionForType);

        // nav
        //  ul
        //   li
        //    ul
        //
//        logger.info("addChoices --- " + type);

        // only the first two get a marking
        boolean addTypePrefix = typeOrder.indexOf(type) < 2;

        liForDimensionForType.add(addChoices(typeToValues, type, addTypePrefix));
      }
    }

    addDynamicFacets(typeToValues, allTypesContainer);
  }

  protected void addDynamicFacets(Map<String, Set<MatchInfo>> typeToValues, UnorderedList allTypesContainer) {
  }


  /**
   * @param typeToSection
   * @param prefix
   * @param onlyUninspected
   * @return
   */
  protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection,
                                                       String prefix,
                                                       boolean onlyUninspected) {
    ExerciseListRequest exerciseListRequest = getExerciseListRequest(prefix)
        .setTypeToSelection(typeToSection)
        .setOnlyUninspected(onlyUninspected)
        .setAddFirst(false);

    return exerciseListRequest;
  }

//  private int numEx;
//  private int numContext;

  /**
   * @param result
   * @see ExerciseList.SetExercisesCallback#onSuccess
   */
  @Override
  protected void setScores(ExerciseListWrapper<T> result) {
    Map<Integer, Float> idToScore = result.getIdToScore();
//    numEx = 0;
//    numContext = 0;
    for (T ex : result.getExercises()) {
      int id = ex.getID();
      if (idToScore.containsKey(id)) {
        ex.getMutableShell().setScore(idToScore.get(id));
      }

//      if (ex.isContext()) {
//        numContext++;
//      } else {
//        numEx++;
//        numContext += ex.getNumContext();
//      }
    }
  }

  protected UserList.LIST_TYPE getListType() {
    return UserList.LIST_TYPE.NORMAL;
  }

  @Override
  @NotNull
  public ListItem getTypeContainer(String type, boolean hasSelection) {
    if (type.isEmpty()) logger.warning("getTypeContainer huh? type is empty???");

    ListItem liForDimensionForType = getLIDimension(hasSelection);
    liForDimensionForType.add(hasSelection ? getTypeHeader(type) : getSimpleHeader(type));
    return liForDimensionForType;
  }

  private Panel getSimpleHeader(String type) {
    FlowPanel span = getSpan();
    span.getElement().setInnerText(type);
    span.addStyleName("header");
    return span;
  }

  /**
   * ul
   * span
   * li
   * ul
   * span
   * li ...
   * <p>
   * ul
   *
   * @param typeToValues
   * @param type
   * @param addTypePrefix
   * @return
   * @see #addFacetsForReal
   */
  @Override
  public Panel addChoices(Map<String, Set<MatchInfo>> typeToValues, String type, boolean addTypePrefix) {
    Panel choices = new UnorderedList(); // ul
    String selectionForType = typeToSelection.get(type);

    if (DEBUG_CHOICES) logger.info("addChoices " + type + "=" + selectionForType);

    if (selectionForType == null) { // no selection made, show all possible values for type
      Set<MatchInfo> keys = typeToValues.get(type);
      if (keys != null) {
        if (DEBUG_CHOICES) logger.info("addChoices for " + type + "=" + keys.size());
        if (type.equalsIgnoreCase(RECORDED)) {
          keys = new TreeSet<>(keys);
        }
        addChoicesForType(typeToValues, type, choices, keys, addTypePrefix);
      }
    } else {
      String childType = getChildForParent(type);
      if (childType != null) {
        // add the parent
        {
          Widget parentAnchor =
              typeToSelection.containsKey(childType) ?
                  getParentAnchor(type, selectionForType, childType, addTypePrefix) :
                  getSelectedAnchor(type, selectionForType, addTypePrefix);
          choices.add(parentAnchor);
        }

        // add the children
        {
          ListItem liForDimension = new ListItem();
          liForDimension.addStyleName("subdimension");
          liForDimension.addStyleName("refinement");
          choices.add(liForDimension);

          liForDimension.add(addChoices(typeToValues, childType, addTypePrefix));
        }
      } else {
        if (isListType(type)) {
          if (DEBUG_CHOICES) logger.info("addChoices addListChoice " + type + "=" + selectionForType);
          addListChoice(type, choices, selectionForType, addTypePrefix);
        } else {
          Set<MatchInfo> matchInfos = typeToValues.get(type);
          int num = -1;
          if (matchInfos != null) {
            Optional<MatchInfo> first = matchInfos.stream().filter(matchInfo -> matchInfo.getValue().equalsIgnoreCase(selectionForType)).findFirst();
            if (first.isPresent()) num = first.get().getCount();
          }
          if (DEBUG_CHOICES) {
            logger.info("addChoices getSelectedAnchor " + type + "=" + selectionForType +
                "\n\tkeys " + typeToValues.get(type));

          }
          String numberSuffix = num != -1 ? " (" + num + ")" : "";
          choices.add(getSelectedAnchor(type, selectionForType + numberSuffix, addTypePrefix));
        }
      }
    }
    return choices;
  }

  protected void addExerciseChoices(String dynamicFacet, ListItem liForDimensionForType, Set<MatchInfo> value) {
//    Set<MatchInfo> value = new HashSet<>();
//    value.add(e);

    Map<String, Set<MatchInfo>> typeToValues = new HashMap<>();
    typeToValues.put(dynamicFacet, value);

    //logger.info("addExerciseChoices --- for " + value);
    liForDimensionForType.add(addChoices(typeToValues, dynamicFacet, false));
  }

  /**
   * TODO : refactor to do something saner.
   *
   * @param type
   * @param choices
   * @param selectionForType
   * @param addTypePrefix
   */
  void addListChoice(String type, Panel choices, String selectionForType, boolean addTypePrefix) {
  }


  /**
   * @param typeToValues
   * @param type
   * @param choices
   * @param keys
   * @param addTypePrefix
   * @see #addChoices
   */
  private void addChoicesForType(Map<String, Set<MatchInfo>> typeToValues,
                                 String type,
                                 Panel choices,
                                 Set<MatchInfo> keys, boolean addTypePrefix) {
    int j = 0;

    int toShow = getToShow(rootNodesInOrder.size() + 1);
    int keysSize = keys.size();

    boolean hasMore = shouldShowMore(keysSize, toShow);
    Boolean showAll = typeToShowAll.getOrDefault(type, false);


    if (DEBUG_CHOICES) {
      logger.info("addChoicesForType" +
          "\n\ttype    " + type +
          "\n\tsize    " + keysSize +// "(" +types+
          "\n\tto show " + toShow +
          "\n\tkeys    " + keys.size() +
          "\n\thasMore " + hasMore +
          "\n\tshowAll " + showAll +
          "\n\tshowAll " + showAll
      );
    }

    for (MatchInfo key : keys) {
      addLIChoice(choices, getAnchor(type, key, key.getUserListID(), key.isItalic(), addTypePrefix));

      // add final "View all" link if we're not supposed to show all
      if (hasMore && !showAll && ++j == toShow) {
        addLIChoice(choices, getShowMoreAnchor(typeToValues, type, keysSize - j));
        break;
      }
    }
    if (hasMore && showAll) {
      addLIChoice(choices, getShowLess(typeToValues, type));
    }
  }

  private int getToShow(int size) {
    return TOTAL / size;
  }

  private boolean shouldShowMore(int keysSize, int toShow) {
    int diff = keysSize - toShow;
    return keysSize > toShow && diff > CLOSE_TO_END;
  }

  @NotNull
  private Anchor getShowLess(final Map<String, Set<MatchInfo>> typeToValues, final String type) {
    Anchor anchor = new Anchor();
    anchor.setHTML(SHOW_LESS);
    anchor.addClickHandler(event -> {
      typeToShowAll.put(type, false);
      addFacetsForReal(typeToValues, typeOrderContainer);
    });
    return anchor;
  }

  /**
   * @param typeToValues
   * @param type
   * @param remaining
   * @return
   * @see #addChoicesForType
   */
  @NotNull
  private Anchor getShowMoreAnchor(final Map<String, Set<MatchInfo>> typeToValues, final String type, int remaining) {
    Anchor anchor = new Anchor();
    String showMore = "<i>View all " + type + "s (" + remaining + ")</i>";
    anchor.setHTML(showMore);
    anchor.addClickHandler(event -> {
      typeToShowAll.put(type, true);
      addFacetsForReal(typeToValues, typeOrderContainer);
    });
    return anchor;
  }

  /**
   * @param type
   * @param selectionForType
   * @param addTypePrefix
   * @return
   * @see #addChoices(Map, String, boolean)
   */
  @NotNull
  Widget getSelectedAnchor(String type, String selectionForType, boolean addTypePrefix) {
    Panel anchor = getSpan();
    anchor.getElement().setInnerHTML((addTypePrefix ? type + " " : "") + selectionForType);
    anchor.addStyleName("selected");
    addTooltip(type, selectionForType, anchor);
    return anchor;
  }

  @NotNull
  private FlowPanel getSpan() {
    return new FlowPanel("span");
  }

  @NotNull
  private Panel getTypeHeader(String type) {
    Anchor headerAnchor = getHeaderAnchor(type);

    Panel headerContainer = getSpan();
    headerContainer.addStyleName(MENU_ITEM);
    headerContainer.add(headerAnchor);

    return headerContainer;
  }

  /**
   * @param refined
   * @return
   * @see #getTypeContainer
   */
  @NotNull
  private ListItem getLIDimension(boolean refined) {
    ListItem liForDimension = new ListItem();
    liForDimension.addStyleName("dimension");
    if (refined) liForDimension.addStyleName("refined");
    return liForDimension;
  }

  private void addLIChoice(Panel choices, Widget anchor1) {
    ListItem li = new ListItem();
    li.add(anchor1);
    choices.add(li);
  }

  /**
   * @param type
   * @param key
   * @param newUserListID
   * @param italic        true if list is not mine - I don't own it
   * @param addTypePrefix
   * @return
   */
  private Widget getAnchor(String type, MatchInfo key, int newUserListID, boolean italic, boolean addTypePrefix) {
    Panel span = getSpan();
    span.addStyleName(MENU_ITEM);

    addMatchInfoTooltip(type, key, span);

    String choiceName = key.getValue();

    String nameToUse = (addTypePrefix ? type + " " : "") + choiceName;
    Anchor anchor = getAnchor(nameToUse);
    ClickHandler choiceHandler = getChoiceHandler(type, choiceName, newUserListID);
    anchor.addClickHandler(choiceHandler);
    anchor.addStyleName("choice");

    // seems better but topics are long
//    anchor.getElement().getStyle().setWhiteSpace(Style.WhiteSpace.NOWRAP);

    if (italic) anchor.getElement().getStyle().setFontStyle(Style.FontStyle.ITALIC);

    span.add(anchor);
    span.add(getQty("" + key.getCount(), choiceHandler));

    return span;
  }

  @NotNull
  private Anchor getQty(String label, ClickHandler choiceHandler) {
    Anchor qty = getAnchor(label);
    qty.addClickHandler(choiceHandler);
    qty.addStyleName("qty");
    return qty;
  }

  private void addMatchInfoTooltip(String type, MatchInfo key, Panel span) {
    addTooltip(type, key.getValue() + key.getTooltip(), span);
  }

  private void addTooltip(String type, String value, Panel span) {
    new TooltipHelper().addTooltip(span, type + " " + value);
  }

  @NotNull
  private Anchor getHeaderAnchor(final String type) {
    Anchor typeSection = getAnchor(type); // li
    addRemoveClickHandler(type, typeSection);
    return typeSection;
  }

  @NotNull
  private Anchor getAnchor(String value) {
    Anchor anchor = new Anchor();
    anchor.setText(value);
    removeAnchorStyle(anchor);
    return anchor;
  }

  private void removeAnchorStyle(Anchor anchor) {
    anchor.removeStyleName("gwt-Anchor");
  }

  /**
   * @param type
   * @param value
   * @param childType
   * @param addTypePrefix
   * @return
   * @see #addChoices
   */
  @NotNull
  private Widget getParentAnchor(String type, String value, String childType, boolean addTypePrefix) {
    Panel span = getSpan();
    span.addStyleName(MENU_ITEM);

    {
      Anchor typeSection = getAnchor((addTypePrefix ? type + " " : "") + value); // li
      addRemoveClickHandler(childType, typeSection);
      span.add(typeSection);
    }

    addTooltip(type, value, span);

    return span;
  }

  /**
   * @param type
   * @param typeSection
   * @see #getHeaderAnchor(String)
   * @see #getParentAnchor
   */
  private void addRemoveClickHandler(final String type, FocusWidget typeSection) {
    typeSection.addClickHandler(event -> {
      Map<String, String> candidate = new HashMap<>(typeToSelection);
      boolean removed = removeSelection(type, candidate);
      if (!removed) {
        logger.warning("addRemoveClickHandler didn't remove" +
            "\n\tselection " + type +
            "\n\tstill     " + candidate);
      }

      int i = typeOrder.indexOf(type);
      if (i >= 0) {
        for (; i < typeOrder.size(); i++) {
          String key = typeOrder.get(i);
          //   logger.info("removing " +key);
          candidate.remove(key);
        }
      }
      setHistory(candidate);
    });
  }

  /**
   * @param childType
   * @return
   */
  private boolean removeSelection(String childType) {
    return removeSelection(childType, this.typeToSelection);
  }

  private boolean removeSelection(String childType, Map<String, String> typeToSelection) {
    String remove = typeToSelection.remove(childType);
    boolean did = remove != null;
    String childOfChild = getChildForParent(childType);
    if (childOfChild != null) {
      did |= removeSelection(childOfChild);
    }
    return did;
  }

  private String getChildForParent(String childType) {
    Map<String, String> parentToChild = getStartupInfo().getParentToChild();
    String s = parentToChild.get(childType);
//    logger.info("getChildForParent parent->child " + parentToChild);
//    logger.info("getChildForParent childType     " + childType + " = " + s);
    return s;
  }

  /**
   * Add this type=value pair to existing selections
   *
   * @param type
   * @param key
   * @return
   */
  @NotNull
  private ClickHandler getChoiceHandler(final String type, final String key, int newUserListID) {
    return event -> {
      Map<String, String> candidate = new HashMap<>(typeToSelection);  // existing set is in type->selection
      candidate.put(type, getChoiceHandlerValue(type, key, newUserListID));

      logger.info("getChoiceHandler click on " + type + "=" + key + ", list = " + newUserListID);
      logger.info("getChoiceHandler candidate " + candidate);

      setHistory(candidate);
    };
  }

  String getChoiceHandlerValue(String type, String key, int newUserListID) {
    return key;
  }

  boolean isListType(String type) {
    return false;
  }

  /**
   * Remember to keep the search term, if there is any.
   *
   * @param candidate
   * @see #getChoiceHandler
   * @see #addRemoveClickHandler
   */
  void setHistory(Map<String, String> candidate) {
    if (DEBUG_SET_HISTORY) logger.info("setHistory " + candidate);
    setHistoryItem(getHistoryToken(candidate) + keepSearchItem());
  }

  private int reqid = 0;

  /**
   * So you can specify a filter sequence that results in no exercises
   * The idea here is to unset downstream filters that until we have a set that
   * results in a non-zero set of exercises.
   * <p>
   * TODO : consider reqid check to deal with races...
   *
   * @param typeToSelection
   * @param userListID
   * @see #addRemoveClickHandler
   * @see #getChoiceHandler
   * @see #getSectionWidgetContainer
   */
  protected void getTypeToValues(Map<String, String> typeToSelection, int userListID) {
    if (isThereALoggedInUser()) {
      doOpacityFeedback();

      if (DEBUG) {
        List<Pair> pairs = getPairs(typeToSelection);
        logger.info("getTypeToValues request " + pairs + " list " + userListID);
      }

      final long then = System.currentTimeMillis();

      FilterRequest filterRequest = getFilterRequest(userListID, getPairs(typeToSelection));

      // logger.info("getTypeToValues Send req " + filterRequest);

      service.getTypeToValues(filterRequest,
          new AsyncCallback<FilterResponse>() {
            @Override
            public void onFailure(Throwable caught) {
              if (caught instanceof DominoSessionException) {
                logger.info("getTypeToValues : got " + caught);
              }
              controller.handleNonFatalError(GETTING_TYPE_VALUES, caught);
            }

            /**
             * fixes downstream selections that no longer make sense.
             * @param response
             */
            @Override
            public void onSuccess(FilterResponse response) {
              gotFilterResponse(response, then, typeToSelection);
            }
          });
    }
  }


  /**
   * TODO : push down the part about CONTENT and maybe list.
   *
   * @param typeToSelection
   * @return
   * @see #getTypeToValues
   */
  @NotNull
  protected List<Pair> getPairs(Map<String, String> typeToSelection) {
    List<Pair> pairs = new ArrayList<>();

    for (String type : getTypeOrderSimple()) {
      String s = typeToSelection.get(type);
      pairs.add(new Pair(type, (s == null) ? ANY : s));
    }

    return pairs;
  }

  @NotNull
  protected FilterRequest getFilterRequest(int userListID, List<Pair> pairs) {
    return new FilterRequest(incrReqID(), pairs, userListID);
  }

  protected int incrReqID() {
    return reqid++;
  }

  protected void gotFilterResponse(FilterResponse response, long then, Map<String, String> typeToSelection) {
    if (DEBUG) {
      logger.info("getTypeToValues took " + (System.currentTimeMillis() - then) + " to get" +
          "\n\ttype to selection " + typeToSelection +
          "\n\ttype to include   " + response.getTypesToInclude() +
          "\n\t#type to values   " + response.getTypeToValues().size() +
          "\n\ttype to values    " + response.getTypeToValues()
      );
    }

    boolean didChange = changeSelection(response.getTypesToInclude(), typeToSelection);
    setTypeToSelection(typeToSelection);
    addFacetsForReal(response.getTypeToValues(), typeOrderContainer);

    if (didChange) {
      gotSelection();
    } else {
      //   logger.info("gotFilterResponse - no change. ");
      restoreUIAndLoadExercises(History.getToken(), didChange);
    }
  }

  /**
   * @param typesToInclude
   * @param typeToSelection
   * @return true if we did actually change the currently visible selection state as a result of asking the server for the type-selection hierarchy
   */
  private boolean changeSelection(Set<String> typesToInclude, Map<String, String> typeToSelection) {
    boolean changed = false;
    for (String selectedType : new ArrayList<>(typeToSelection.keySet())) {
      boolean clearSelection = !typesToInclude.contains(selectedType);
      if (clearSelection) {
        if (removeSelection(selectedType, typeToSelection)) {
          changed = true;
        }
      }
    }
    return changed;
  }

  protected boolean isThereALoggedInUser() {
    return controller.getUser() > 0;
  }

  /**
   * @see #getTypeToValues
   */
  private void gotSelection() {
    pushNewSectionHistoryToken();
  }

  /**
   * TODO: figure out a way to not do multiple restores when jumping from progress.
   *
   * @param selectionState
   * @see HistoryExerciseList#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   * @see #restoreUIState
   */
  @Override
  protected void restoreListBoxState(SelectionState selectionState) {
    if (DEBUG) {
      logger.info("restoreListBoxState = '" + selectionState + "' " +
          "\n\t" + selectionState.getInfo());

//      String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("here for " + selectionState));
//      logger.info("logException stack:\n" + exceptionAsString);

    }
    super.restoreListBoxState(selectionState);
    downloadHelper.updateDownloadLinks(selectionState, typeOrder);
  }

  /**
   * x Add a line that spells out in text which lessons have been chosen, derived from the selection state.
   * x Show in the same type order as the button rows.
   *
   * @paramx selectionState to get the current selection state from
   * @see #onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  private void updateDownloadLinks() {
    // keep the download link info in sync with the selection
    downloadHelper.updateDownloadLinks(getSelectionState(getHistoryToken()), typeOrder);
  }

  /**
   * @seex #rememberAndLoadFirst
   */
  @Override
  protected void loadFirstExercise(String searchIfAny) {
    //  logger.info("loadFirstExercise : ---");
    if (isEmpty()) { // this can only happen if the database doesn't load properly, e.g. it's in use
      // logger.info("loadFirstExercise : current exercises is empty");
      //    gotEmptyExerciseList();
    } else {
      super.loadFirstExercise(searchIfAny);
    }
  }

  /**
   * @seex HistoryExerciseList.MySetExercisesCallback#onSuccess(mitll.langtest.shared.amas.ExerciseListWrapper)
   */
  @Override
  protected void gotEmptyExerciseList() {
    // logger.info("got empty exercise list.");
    clearExerciseContainer();
    showEmptySelection();
    hidePrevNext();
  }

  /**
   * @return
   * @see HistoryExerciseList##HistoryExerciseList
   */
  protected FacetContainer getSectionWidgetContainer() {
    return new FacetContainer() {
      /**
       * @see HistoryExerciseList#restoreListBoxState
       * @param selectionState
       * @param typeOrder
       */
      @Override
      public void restoreListBoxState(SelectionState selectionState, Collection<String> typeOrder) {
        if (DEBUG) logger.info("getSectionWidgetContainer.restoreListBoxState " +
            "\n\tt->sel " + selectionState +
            "\n\ttypeOrder " + typeOrder);
        Map<String, String> newTypeToSelection = getNewTypeToSelection(selectionState, typeOrder);
        if (typeToSelection.equals(newTypeToSelection) &&
            typeOrderContainer != null &&
            typeOrderContainer.iterator().hasNext()) {
          if (DEBUG)
            logger.info("getSectionWidgetContainer : restoreListBoxState state already consistent with " + newTypeToSelection);

        } else {
          getTypeToValues(newTypeToSelection, getUserListID(selectionState, newTypeToSelection));
        }
      }

      /**
       * @see PagingExerciseList#getHistoryTokenFromUIState
       * @return
       */
      @Override
      public String getHistoryToken() {
        return FacetExerciseList.this.getHistoryToken(FacetExerciseList.this.typeToSelection);
      }

      @Override
      public int getNumSelections() {
        return 0;
      }
    };
  }

  @NotNull
  protected Map<String, String> getNewTypeToSelection(SelectionState selectionState, Collection<String> typeOrder) {
    return getTypeToSelection(selectionState, typeOrder);
  }

  @NotNull
  protected Map<String, String> getTypeToSelection(SelectionState selectionState, Collection<String> typeOrder) {
    Map<String, String> newTypeToSelection = new HashMap<>();
    for (String type : typeOrder) {
      Collection<String> selections = selectionState.getTypeToSection().get(type);
      if (selections != null && !selections.isEmpty()) {
        newTypeToSelection.put(type, selections.iterator().next());
      }
    }

    // logger.info("getTypeToSelection " + newTypeToSelection);

    return newTypeToSelection;
  }

  int getUserListID(SelectionState selectionState, Map<String, String> newTypeToSelection) {
    return -1;
  }

  /**
   * @param typeToSelection
   * @return
   */
  @NotNull
  private String getHistoryToken(Map<String, String> typeToSelection) {
    StringBuilder builder = new StringBuilder();
//        logger.info("getHistoryToken t->sel " + typeToSelection);
    for (Map.Entry<String, String> pair : typeToSelection.entrySet()) {
      builder
          .append(pair.getKey())
          .append("=")
          .append(pair.getValue())
          .append(SECTION_SEPARATOR);
    }
    builder.append(getProjectParam());

    String s = builder.toString();
    //     logger.info("getHistoryToken token '" + s + "'");
    return s;
  }

  private String keepSearchItem() {
    SelectionState selectionState = getSelectionState(getHistoryToken());
    return selectionState.getSearch().isEmpty() ? "" : selectionState.getSearchEntry();
  }

  @NotNull
  private String getProjectParam() {
    return SelectionState.SECTION_SEPARATOR + SelectionState.PROJECT + "=" + getProjectID();
  }

  private int getProjectID() {
    return getStartupInfo() == null ? -1 : getCurrentProject();
  }

  /**
   * @param project
   * @see #onValueChange
   */
  @Override
  protected void projectChangedTo(int project) {
    controller.reallySetTheProject(project);
  }

  int getCurrentProject() {
    return getStartupInfo().getProjectid();
  }

  protected void pushFirstSelection(int exerciseID, String searchIfAny) {
    updateDownloadLinks();
    askServerForExercise(exerciseID);
  }

  /**
   * @param comparator
   * @see ListSorting#sortBy
   */
  @Override
  public void flushWith(Comparator<T> comparator) {
    //  logger.info("flushWith ");
    super.flushWith(comparator);
    askServerForExercise(-1);
  }


  /**
   * TODO : do different thing for AVP.
   * <p>
   * TODO : scroll to visible item
   * <p>
   * goes ahead and asks the server for the next item so we don't have to wait for it.
   *
   * @param itemID
   * @see ExerciseList#checkAndAskServer
   */
  protected void askServerForExercise(int itemID) {
    if (itemID > 0) pagingContainer.markCurrentExercise(itemID);

    Collection<Integer> visibleIDs = pagingContainer.getVisibleIDs();

    if (visibleIDs.isEmpty()) {
      if (DEBUG) logger.info("askServerForExercise skipping empty visible range?");
    } else {
      if (DEBUG) logger.info("askServerForExercise visible ids = " + visibleIDs);
//      logger.warning("askServerForExercise got " + getExceptionAsString(new Exception()));
      askServerForVisibleExercises(itemID, visibleIDs, incrReq());
    }
  }

  /**
   * @param itemID
   * @param visibleIDs
   * @param currentReq
   * @see #askServerForExercise
   */
  private void askServerForVisibleExercises(int itemID, Collection<Integer> visibleIDs, final int currentReq) {
    // logger.info("askServerForVisibleExercises ask for single -- " + itemID + " and " + visibleIDs.size());
    if (visibleIDs.isEmpty() && pagingContainer.isEmpty() && finished) {
      if (DEBUG) logger.info("askServerForVisibleExercises show empty -- ");
    } else {
      if (DEBUG) logger.info("askServerForVisibleExercises item " + itemID + " and " + visibleIDs.size());
      getVisibleExercises(getVisibleForDrill(itemID, visibleIDs), currentReq);
    }
  }

  protected Collection<Integer> getVisibleForDrill(int itemID, Collection<Integer> visibleIDs) {
    return visibleIDs;
  }

  @Override
  public boolean isCurrentReq(int reqID) {
    return reqID == -1 || reqID == getCurrentExerciseReq();
  }

  int getCurrentExerciseReq() {
    return freqid;
  }

  /**
   * @return
   * @see #askServerForExercise(int)
   * @see #makePagingContainer
   */
  protected int incrReq() {
    return ++freqid;
  }

  /**
   * @param reqID
   * @return
   * @see #makeExercisePanels
   * @see #showExercises
   */
  private boolean isStale(int reqID) {
    return !isCurrentReq(reqID);
  }

  protected void hidePrevNext() {
    hidePrevNextWidgets();
    setProgressVisible(false);
  }

  /**
   * @see #gotFullExercises
   */
  protected void hidePrevNextWidgets() {
    pageSizeContainer.setVisible(false);
    sortBox.setVisible(false);
    pagerAndSortRow.setVisible(false);
  }

  private void showPrevNext() {
    pageSizeContainer.setVisible(true);
    sortBox.setVisible(true);
    setProgressVisible(true);
    pagerAndSortRow.setVisible(true);
  }

  /**
   * @param visibleIDs
   * @param currentReq
   * @see #askServerForVisibleExercises(int, Collection, int)
   */
  protected void getVisibleExercises(final Collection<Integer> visibleIDs, final int currentReq) {
    doOpacityFeedback();
    checkAndGetExercises(visibleIDs, currentReq);
  }

  private void doOpacityFeedback() {
    Widget widget = innerContainer.getWidget();
    if (widget != null) {
      widget.getElement().getStyle().setOpacity(0.2);
      // logger.info("set opacity ");
    } else {
      //     logger.warning("not setting opacity...");
    }
  }

  protected void checkAndGetExercises(Collection<Integer> visibleIDs, int currentReq) {
    if (isCurrentReq(currentReq)) {
      if (DEBUG) logger.info("getVisibleExercises  req " + currentReq + " vs current " + getCurrentExerciseReq());
      reallyGetExercises(visibleIDs, currentReq);
    } else {
      logger.warning("getVisibleExercises skip stale req " + currentReq + " vs current " + getCurrentExerciseReq());
    }
  }

  /**
   * Look in the cache for known exercises, use them if available.
   *
   * @param visibleIDs
   * @param currentReq
   * @see #getVisibleExercises(Collection, int)
   */
  protected void reallyGetExercises(Collection<Integer> visibleIDs, final int currentReq) {
    if (DEBUG) {
      logger.info("reallyGetExercises " + visibleIDs.size() + " visible ids : " + visibleIDs + " currentReq " + currentReq);
    }

    List<U> alreadyFetched = new ArrayList<>();
    Set<Integer> requested = getRequested(visibleIDs, alreadyFetched);

    if (requested.isEmpty()) {
      if (DEBUG) logger.info("reallyGetExercises no req for " + alreadyFetched.size());
      gotFullExercises(currentReq, alreadyFetched);
    } else {
/*      logger.info("reallyGetExercises make" +
          "\n\treq for " + requested +
          "\n\tvisible " + visibleIDs);*/

      if (isThereALoggedInUser()) {
        getFullExercises(visibleIDs, currentReq, requested, alreadyFetched);
      } else {
        logger.warning("reallyGetExercises : not asking for " + requested);
      }
    }
  }

  @NotNull
  protected Set<Integer> getRequested(Collection<Integer> visibleIDs, List<U> alreadyFetched) {
    Set<Integer> requested = new HashSet<>();
    for (Integer id : visibleIDs) {
      U cachedExercise = getCachedExercise(id);
      if (cachedExercise == null) {
        requested.add(id);
      } else {
        alreadyFetched.add(cachedExercise);
      }
    }
    return requested;
  }

  protected abstract void getFullExercises(Collection<Integer> visibleIDs,
                                           int currentReq,
                                           Collection<Integer> requested,
                                           List<U> alreadyFetched);

  void fullExerciseFailure(Throwable caught) {
    logger.warning("getFullExercises got exception : " + caught);
    logger.warning("getFullExercises got " + getExceptionAsString(caught));
    dealWithRPCError(caught);
    hidePrevNext();
  }

  /**
   * TODO : how to avoid forced cast here?
   *
   * @param result
   * @param alreadyFetched
   * @return
   */
  @NotNull
  Map<Integer, ClientExercise> rememberFetched(ExerciseListWrapper<ClientExercise> result, List<ClientExercise> alreadyFetched) {
    Map<Integer, ClientExercise> idToEx = setScoreHistory(result.getScoreHistoryPerExercise(), result.getExercises());
    alreadyFetched.forEach(exercise -> idToEx.put(exercise.getID(), exercise));

    result.getExercises().forEach(clientEx -> addExerciseToCached((U) clientEx));
    return idToEx;
  }

  /**
   * TODO : why? ?>
   *
   * @param scoreHistoryPerExercise
   * @return
   * @see #rememberFetched(ExerciseListWrapper, List)
   * @see #goGetNextPage
   */
  @NotNull
  Map<Integer, ClientExercise> setScoreHistory(Map<Integer, CorrectAndScore> scoreHistoryPerExercise, List<ClientExercise> exercises) {
    Map<Integer, ClientExercise> idToEx = new HashMap<>();
    for (ClientExercise ex : exercises) {
      //   logger.info("setScoreHistory " + ex.getID() +  " " + ex);
      int id = ex.getID();
      idToEx.put(id, ex);

      CorrectAndScore correctAndScore = scoreHistoryPerExercise.get(id);
      setScoreOnExercise(ex, correctAndScore);

      // remember scores for context sentences too!
      ex.getDirectlyRelated().forEach(dir -> setScoreOnExercise(dir, scoreHistoryPerExercise.get(dir.getID())));
    }
    //logger.info("setScoreHistory now " + idToEx.size());

    return idToEx;
  }

  private void setScoreOnExercise(ClientExercise ex, CorrectAndScore correctAndScore) {
    // make sure we make a real exercise list here even if it's empty since we'll want to add to it later
    List<CorrectAndScore> scoreTotal = new ArrayList<>();
    if (correctAndScore != null) scoreTotal.add(correctAndScore);
    // List<CorrectAndScore> scoreTotal = correctAndScores == null ? new ArrayList<>() : correctAndScores;
    //  logger.info("attach score history " + scoreTotal.size() + " to exercise "+ id);
    ex.getMutableShell().setScores(scoreTotal);
  }

  /**
   * @param reqID
   * @param toShow
   * @see #reallyGetExercises
   * @see ClientExerciseFacetExerciseList#getFullExercisesSuccess
   */
  void gotFullExercises(final int reqID, Collection<U> toShow) {
    if (DEBUG) logger.info("gotFullExercises show req " + reqID + " exercises " + getIDs(toShow));
    if (isCurrentReq(reqID)) {
      if (toShow.isEmpty()) {
        hidePrevNext();
      } else {
        showExercises(toShow, reqID);
      }
    } else {
      if (DEBUG_STALE) {
        logger.info("gotFullExercises skip stale req " + reqID + " vs current " + getCurrentExerciseReq());
      }
    }
  }

  /**
   * @param result
   * @param reqID
   * @see #gotFullExercises
   */
  protected void showExercises(final Collection<U> result, final int reqID) {
    if (isStale(reqID)) {
      if (DEBUG_STALE)
        logger.info("showExercises Skip stale req " + reqID + " vs current " + getCurrentExerciseReq());
    } else {
      if (DEBUG) logger.info("showExercises show req " + reqID + " exercises " + getIDs(result));
      Scheduler.get().scheduleDeferred((Command) () -> showExercisesForCurrentReq(result, reqID));
    }
  }

  /**
   *
   */
  protected abstract void goGetNextPage();

  @NotNull
  Set<Integer> getNextPageIDs() {
    CommonShell currentSelection = pagingContainer.getCurrentSelection();
    List<T> items = pagingContainer.getItems();
    int i = items.indexOf(currentSelection);

    Set<Integer> toAskFor = getNextIDs(items, i, 10);
//    logger.info("goGetNextPage toAskFor " + toAskFor.size() + " : " + toAskFor);
    if (toAskFor.size() == 1) {
      toAskFor = getNextIDs(items, i, 20);
      //    logger.info("\tgoGetNextPage toAskFor " + toAskFor.size() + " : " + toAskFor);
    }
    return toAskFor;
  }

  @NotNull
  private Set<Integer> getNextIDs(List<T> items, int i, int n) {
    List<T> commonShells = pagingContainer.getItems().subList(i + 1, Math.min(items.size(), i + 1 + n));

    Set<Integer> toAskFor = new HashSet<>();
    commonShells.forEach(commonShell -> toAskFor.add(commonShell.getID()));

    toAskFor.removeAll(fetched.keySet());
    return toAskFor;
  }

  List<Integer> getIDs(Collection<U> result) {
    return result.stream().map(HasID::getID).collect(Collectors.toList());
  }

  protected void showExercisesForCurrentReq(Collection<U> result, int reqID) {
    if (isCurrentReq(reqID)) {
      reallyShowExercises(result, reqID);
      if (isCurrentReq(reqID)) {
        //logger.info("showExercises for progress current " + reqID);
        setProgressBarScore(getInOrder(), reqID);
      } else {
        if (DEBUG_STALE) logger.info("showExercises for progress stale " + reqID);
      }
    } else {
      if (DEBUG_STALE)
        logger.info("showExercises (2) skip stale req " + reqID + " vs current " + getCurrentExerciseReq());

    }
  }

  /**
   * @param result
   * @param reqID
   * @see #showExercisesForCurrentReq
   */
  private void reallyShowExercises(Collection<U> result, int reqID) {
    // logger.info("reallyShowExercises req " + reqID + " vs current " + getCurrentExerciseReq());

    DivWidget exerciseContainer = new DivWidget();
    populatePanels(result, reqID, exerciseContainer);

    innerContainer.setWidget(exerciseContainer);  // immediate feedback that something is happening...
    showPrevNext();
//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("here for " + reqID));
//    logger.info("logException stack:\n" + exceptionAsString);
  }

  protected void populatePanels(Collection<U> result, int reqID, DivWidget exerciseContainer) {
    long then = System.currentTimeMillis();
    List<RefAudioGetter> getters = makeExercisePanels(result, exerciseContainer, reqID);
    long now = System.currentTimeMillis();

    if (DEBUG)
      logger.info("reallyShowExercises made " + getters.size() + " panels in " + (now - then) + " millis for req " + getCurrentExerciseReq() + " ");

    if (!getters.isEmpty()) {
      getRefAudio(getters.iterator());
    }
  }

  /**
   * @param result
   * @param exerciseContainer
   * @param reqID
   * @return
   */
  private List<RefAudioGetter> makeExercisePanels(Collection<U> result,
                                                  DivWidget exerciseContainer,
                                                  final int reqID) {
    List<RefAudioGetter> getters = new ArrayList<>();
    boolean first = true;
/*
    if (isStale(reqID)) {
      logger.info("makeExercisePanels req " + reqID + " vs current " + getCurrentExerciseReq() + " for " + result.size() + " exercises");
    }
*/
//    logger.info("makeExercisePanels req " + reqID + " vs current " + getCurrentExerciseReq() + " for " + result.size() + " exercises");
    //long then = System.currentTimeMillis();

    boolean showFL = factory.getFLChoice();
    boolean showALTFL = factory.getALTFLChoice();

    PhonesChoices phoneChoices = factory.getPhoneChoices();
    for (U exercise : result) {
      if (isStale(reqID)) {
        if (DEBUG_STALE) {
          logger.info("makeExercisePanels stop stale req " + reqID + " vs current " + getCurrentExerciseReq());
        }
        break;
      }

      {
        Panel exercisePanel = factory.getExercisePanel(exercise);
        if (exercisePanel instanceof RefAudioGetter) {
          RefAudioGetter refAudioGetter = (RefAudioGetter) exercisePanel;
          getters.add(refAudioGetter);
          refAudioGetter.setReq(getCurrentExerciseReq());
          //   long then2 = System.currentTimeMillis();
          refAudioGetter.addWidgets(showFL, showALTFL, phoneChoices);
          // long now = System.currentTimeMillis();
          //     logger.info("makeExercisePanels took " +(now-then2) + " millis to make panel for " + exercise.getID());
        }
        if (first) {
          markCurrentExercise(exercise.getID());
          first = false;
        }
        if (isCurrentReq(reqID)) {
          //   long then2 = System.currentTimeMillis();
          exerciseContainer.add(exercisePanel);
          // long now = System.currentTimeMillis();
          // logger.info("makeExercisePanels took " +(now-then2) + " millis to add panel for " + exercise.getID());
        } else {
          logger.info("makeExercisePanels 2 stop stale req " + reqID + " vs current " + getCurrentExerciseReq() + " count = " + exerciseContainer.getWidgetCount());
        }
      }
    }

    Scheduler.get().scheduleDeferred(this::addPlayer);

/*
  long now = System.currentTimeMillis();
logger.info("makeExercisePanels took " + (now - then) + " req " + reqID + " vs current " +
        getCurrentExerciseReq() +
        " returning " + getters.size() + " : first = " + (result.isEmpty() ? "" : "" + result.iterator().next().getID()));*/

    return getters;
  }

  /**
   * Try to fix issue where sometimes somehoe basicMP3Player is null.
   */
  private native void addPlayer() /*-{
      $wnd.basicMP3Player && $wnd.basicMP3Player.init();
  }-*/;

  private U getCachedExercise(Integer id) {
    return fetched.get(id);
  }

  void addExerciseToCached(U exercise) {
    if (!fetched.containsKey(exercise.getID())) {
      fetched.put(exercise.getID(), exercise);
    }
  }

  /**
   * Don't recurse...
   *
   * @param iterator
   * @see #showExercises(Collection, int)
   */
  private void getRefAudio(final Iterator<RefAudioGetter> iterator) {
    if (iterator.hasNext()) {
      RefAudioGetter next = iterator.next();
      //logger.info("getRefAudio asking next panel...");

      if (isStale(next.getReq())) {
        logger.info("getRefAudio : skip stale req for panel...");
      } else {
        next.getRefAudio(() -> {
          if (iterator.hasNext()) {
            //     logger.info("\tgetRefAudio panel complete...");
            final int reqid = next.getReq();
            if (isCurrentReq(reqid)) {
              Scheduler.get().scheduleDeferred(() -> {
                if (isCurrentReq(reqid)) {
                  getRefAudio(iterator);
                } else {
                  if (DEBUG_STALE) logger.info("getRefAudio : 2 skip stale req (" + reqid +
                      ") for panel vs " + getCurrentExerciseReq());
                }
              });
            }
          } else {
            //   logger.info("\tgetRefAudio all panels complete...");
          }
        });
      }
    }
  }


  /**
   * @param displayed
   * @see #showExercisesForCurrentReq(Collection, int)
   */
  private void setProgressBarScore(Collection<T> displayed, final int reqid) {
    exerciseToScore.clear();

    if (displayed == null) {
      logger.warning("huh? display is null?");
      return;
    }
    float total = 0f;
    int withScore = 0;
    // long then = System.currentTimeMillis();
    if (DEBUGSCORE) logger.info("setProgressBarScore checking " + displayed.size());
    for (T exercise : displayed) {
      //  logger.info("ex " + exercise + " class " + exercise.getClass());
      if (exercise.hasScore()) {
        if (DEBUGSCORE) logger.info("\tsetProgressBarScore got " + exercise.getRawScore());
        //    exercisesWithScores.add(exercise.getID());
        float score = exercise.getScore();
        total += score;
        exerciseToScore.put(exercise.getID(), score);
        if (DEBUGSCORE) logger.info("# " + exercise.getID() + " Score " + score);
        withScore++;
      } else {
//        if (DEBUGSCORE)
//          logger.info("# " + exercise.getID() + " no score " + exercise.getEnglish() + " " + exercise.getForeignLanguage());
      }
      // if (!isCurrentReq(reqid)) break;
    }
    //  long now = System.currentTimeMillis();

/*    if (now - then > 100) {
      logger.info("setProgressBarScore took " + (now - then) + " millis");
    }*/

    if (isCurrentReq(reqid)) {
      showNumberPracticed(exerciseToScore.size(), displayed.size());
      if (DEBUGSCORE) logger.info("setProgressBarScore total " + total + " denom " + withScore);
      showAvgScore(total, withScore);
    }
  }


  /**
   * @param id
   * @param hydecScore
   * @see mitll.langtest.client.scoring.SimpleRecordAudioPanel#useResult
   */
  @Override
  public void setScore(int id, float hydecScore) {
    super.setScore(id, hydecScore);
    if (hydecScore > -1f) {
      if (DEBUGSCORE) logger.info("setScore # " + id + " Score " + hydecScore);

      T t = byID(id);
      if (t == null) {
        logger.info("setScore not adding score for " + id + " since likely a context sentence ");
      } else {
        exerciseToScore.put(id, hydecScore);
      }
    } else {
      logger.info("skipping low score for " + id);
    }
    showNumberPracticed(exerciseToScore.size(), pagingContainer.getSize());
    showAvgScore();
  }

  /**
   * filter to just those displayed...
   */
  private void showAvgScore() {
    float total = 0f;
    int withScore = exerciseToScore.size();
    // long then = System.currentTimeMillis();
    if (DEBUGSCORE) logger.info("showAvgScore checking " + withScore + " exercises ");

    for (Map.Entry<Integer, Float> pair : exerciseToScore.entrySet()) {
      total += pair.getValue();
      if (DEBUGSCORE) logger.info("\tshowAvgScore ex " + pair.getKey() + " = " + pair.getValue());
    }
    if (DEBUGSCORE) logger.info("showAvgScore total " + total + " denom " + withScore);

    showAvgScore(total, withScore);
  }

  private void showAvgScore(float total, int withScore) {
    float numer = total * 10f;
    int denom = withScore * 10;
    float fdenom = (float) denom;
    float avg = denom == 0 ? 0F : numer / fdenom;
    if (DEBUGSCORE) logger.info("showAvgScore total " + avg + " " + denom);
    showAvgScore(Math.round(avg * 100));
  }

  /**
   * @param num
   * @param denom
   * @see #setProgressBarScore
   */
  private void showNumberPracticed(int num, int denom) {
    showProgress(num, denom, practicedProgress, NONE_PRACTICED_YET, ALL_PRACTICED, PRACTICED, false);
  }

  private void showAvgScore(int num) {
    showProgress(num, 100, scoreProgress, NO_SCORE, PERFECT, AVG_SCORE, true);
  }

  protected void setProgressVisible(boolean visible) {
    practicedProgress.setVisible(visible);
    scoreProgress.setVisible(visible);
  }

  /**
   * @param num
   * @param denom
   * @param practicedProgress
   * @param zeroPercent
   * @param oneHundredPercent
   * @param suffix
   * @param useColorGradient
   * @see #showAvgScore(int)
   */
  protected void showProgress(int num,
                              int denom,
                              ProgressBar practicedProgress,
                              String zeroPercent,
                              String oneHundredPercent,
                              String suffix,
                              boolean useColorGradient) {
    float fnumer = (float) num;
    float fdenom = (float) denom;

    double score = fnumer / fdenom;
    double percent = 100 * score;
    if (DEBUGSCORE) logger.info("showProgress percent " + percent);

    double percent1 = Math.max(30, num == 0 ? 100 : percent);
    practicedProgress.setPercent(percent1);
    boolean allDone = num == denom;

    practicedProgress.setText(getPracticedText(num, denom, zeroPercent, oneHundredPercent, suffix));

    if (useColorGradient && num > 0) {
      double round = Math.max(percent, 30);
      if (percent == 0d) round = 100d;
      new ScoreProgressBar(false).setColor(practicedProgress, score, round);
    } else {
      practicedProgress.setColor(
          allDone ? ProgressBarBase.Color.SUCCESS :
              percent > SECOND_STEP ?
                  ProgressBarBase.Color.DEFAULT :
                  percent > FIRST_STEP ?
                      ProgressBarBase.Color.INFO :
                      ProgressBarBase.Color.WARNING);
      // bug #
      practicedProgress.getWidget(0).getElement().getStyle().setColor("black");
    }
    practicedProgress.setVisible(true);
  }

  protected String getPracticedText(int num, int denom, String zeroPercent, String oneHundredPercent, String suffix) {
    boolean allDone = num == denom;
    return num == 0 ? zeroPercent :
        allDone ? oneHundredPercent : getPracticedText(num, denom, suffix);
  }

  @NotNull
  protected String getPracticedText(int num, int denom, String suffix) {
    return num + suffix;
  }

  /**
   * @param toRemember
   * @see #rememberExercises
   */
  protected List<T> resort(List<T> toRemember) {
    List<T> commonShells = new ArrayList<>(toRemember);
    listSorting.sortLater(commonShells, sortBoxReally);
    return commonShells;
  }

  @Override
  public void gotShow() {
    // logger.warning("gotShow");
    askServerForExercise(-1);
  }

  @Override
  protected void noSectionsGetExercises(int exerciseID) {
    simpleLoadExercises(getHistoryToken(), getPrefix(), exerciseID);
  }


  protected Map<String, String> getTypeToSelection() {
    return typeToSelection;
  }

  /**
   * @see QuizHelper#clearListSelection
   */
/*  public void clearListSelection() {
    //logger.info("in list ---> clearListSelection ");
    Map<String, String> candidate = new HashMap<>(getTypeToSelection());
    candidate.remove(getDynamicFacet());
    setHistory(candidate);
  }*/

/*  @NotNull
  protected String getDynamicFacet() {
    return LISTS;
  }

  @NotNull
  protected boolean isDynamicFacetInteger() {
    return true;
  }*/
}
