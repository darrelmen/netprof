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

import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.ProgressBar;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ListItem;
import com.github.gwtbootstrap.client.ui.base.ProgressBarBase;
import com.github.gwtbootstrap.client.ui.base.UnorderedList;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.Range;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.scoring.ListChangedEvent;
import mitll.langtest.client.services.ListServiceAsync;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.scoring.SimpleRecordAudioPanel.FIRST_STEP;
import static mitll.langtest.client.scoring.SimpleRecordAudioPanel.SECOND_STEP;

public abstract class FacetExerciseList extends HistoryExerciseList<CommonShell, CommonExercise> {
  private final Logger logger = Logger.getLogger("FacetExerciseList");

  public static final String LISTS = "Lists";
  private static final String PAGE_SIZE_HEADER = "View";

  public static int FIRST_PAGE_SIZE = 5;
  private static final List<Integer> PAGE_SIZE_CHOICES = Arrays.asList(FIRST_PAGE_SIZE, /*5,*/ 10, 25/*, 50*/);
  private static final String ITEMS_PAGE = " items/page";

  private static final int TOTAL = 28;//32;
  private static final int CLOSE_TO_END = 2;
  private static final String SHOW_LESS = "<i>View fewer</i>";
  private static final String SHOW_MORE = "<i>View all</i>";
  private static final String ANY = "Any";
  private static final String MENU_ITEM = "menuItem";
  private final ProgressBar progressBar;

  private List<String> typeOrder;
  private final Panel sectionPanel;
  private final DownloadHelper downloadHelper;
  private final int numToShow;
  protected Panel tableWithPager;

  /**
   * TODO : for now only single selection
   */
  private Map<String, String> typeToSelection = new HashMap<>();
  private final Map<String, Boolean> typeToShowAll = new HashMap<>();
  private Set<String> rootNodesInOrder = new HashSet<>();
  //private int selectedUserListID = -1;
  private Map<Integer, String> idToName = new HashMap<>();

  /**
   * @param secondRow             add the section panel to this row
   * @param currentExerciseVPanel
   * @param controller
   * @param listOptions
   * @param listHeader
   * @param footer
   * @paramx numToShow
   * @see mitll.langtest.client.custom.content.NPFlexSectionExerciseList#NPFlexSectionExerciseList
   */
  public FacetExerciseList(Panel secondRow,
                           Panel currentExerciseVPanel,
                           ExerciseController controller,
                           ListOptions listOptions,
                           DivWidget listHeader,
                           DivWidget footer,
                           int numToShow) {
    super(currentExerciseVPanel, controller, listOptions);

    this.numToShow = numToShow;
    sectionPanel = new DivWidget();
    sectionPanel.getElement().setId("sectionPanel_" + getInstance());
    sectionPanel.addStyleName("rightFiveMargin");

    secondRow.add(sectionPanel);
    setUnaccountedForVertical(0);

    // TODO : connect this back up
    downloadHelper = new DownloadHelper(this);

    DivWidget breadRow = new DivWidget();
    breadRow.setWidth("100%");
    breadRow.getElement().setId("breadRow");
    //  breadRow.addStyleName("floatLeftList");

    breadRow.add(progressBar = new ProgressBar(ProgressBarBase.Style.DEFAULT));
    getProgressBarContainer(progressBar);
    // Todo : add this
    // breadRow.add(new HTML("breadcrumbs go here"));
    listHeader.getElement().setId("listHeader");
    listHeader.add(breadRow);

    DivWidget pagerAndSort = new DivWidget();
    pagerAndSort.getElement().setId("pagerAndSort");
    pagerAndSort.addStyleName("inlineFlex");
    pagerAndSort.setWidth("100%");

    listHeader.add(pagerAndSort);

    pagerAndSort.add(tableWithPager);
    tableWithPager.addStyleName("floatLeft");
    tableWithPager.setWidth("100%");
    this.sortBox = addSortBox(controller);
    //pagesize =

    addPageSize(pagerAndSort);
    pagerAndSort.add(sortBox);

    // addPrevNextPage(footer);
    finished = true;

    // so for instance if in TwoColumnExercisePanel there's an addList, removeList, newList
    LangTest.EVENT_BUS.addHandler(ListChangedEvent.TYPE, authenticationEvent -> {
      gotListChanged();
    });
  }

  private void getProgressBarContainer(ProgressBar progressBar) {
    Style style = progressBar.getElement().getStyle();
    style.setMarginTop(5, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);
    progressBar.setVisible(false);
    // return progressBar;
  }

  // private Button prev, next;
  private final DivWidget sortBox;

/*  @Deprecated
  private void addPrevNextPage(DivWidget footer) {
    DivWidget buttonDiv = new DivWidget();
    buttonDiv.addStyleName("floatRight");
    addPrevPageButton(buttonDiv);
    addNextPageButton(buttonDiv);


  //  footer.add(buttonDiv);

    buttonDiv.addStyleName("alignCenter");

   // pagesize = addPageSize(footer);

//    hidePrevNext();
//    enablePrevNext();
  }*/
/*
  private void addPrevPageButton(DivWidget buttonDiv) {
    Button prev = new Button("Previous Page");
    prev.getElement().setId("PrevNextList_Previous");
    prev.setType(ButtonType.SUCCESS);
    prev.setIcon(IconType.CARET_LEFT);
    prev.setIconSize(IconSize.LARGE);

    prev.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        prev.setEnabled(false);
        pagingContainer.prevPage();
        scrollToTop();
        //enablePrevNext();
      }
    });
    this.prev = prev;
    buttonDiv.add(prev);
  }

  private void addNextPageButton(DivWidget buttonDiv) {
    Button next = new Button("Next Page");
    next.getElement().setId("PrevNextList_Next");
    next.setType(ButtonType.SUCCESS);
    next.setIcon(IconType.CARET_RIGHT);
    next.setIconPosition(IconPosition.RIGHT);
    next.setIconSize(IconSize.LARGE);
    next.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        next.setEnabled(false);
        pagingContainer.nextPage();
        scrollToTop();

      }
    });

    buttonDiv.add(next);
    this.next = next;
    next.addStyleName("leftTenMargin");
  }*/

  //private ListBox pagesize;
  private DivWidget pageSizeContainer;

  private ListBox addPageSize(DivWidget footer) {
    HTML label = new HTML(PAGE_SIZE_HEADER);
    label.addStyleName("floatLeft");
    label.addStyleName("topFiveMargin");
    label.addStyleName("rightFiveMargin");

    pageSizeContainer = new DivWidget();
    pageSizeContainer.addStyleName("floatRight");
    pageSizeContainer.addStyleName("rightFiveMargin");
    pageSizeContainer.addStyleName("inlineFlex");
    footer.add(pageSizeContainer);
    pageSizeContainer.add(label);

    ListBox pagesize = new ListBox();
    pagesize.getElement().getStyle().clearWidth();
    pageSizeContainer.add(pagesize);

    pagesize.addStyleName("floatLeft");

    for (Integer num : PAGE_SIZE_CHOICES) {
      pagesize.addItem(num + ITEMS_PAGE, "" + num);
    }

    int pageSize = getPageIndex();
    if (pageSize != -1) {
      pagesize.setItemSelected(pageSize, true);
      //  logger.info("page size now at " + pageSize);
    }

    pagesize.addChangeHandler(event -> onPageSizeChange(pagesize));

    return pagesize;
  }

  private int getPageIndex() {
    return controller.getStorage().getInt("pageSizeSelected");
  }

  private void onPageSizeChange(ListBox pagesize) {
    pagesize.setFocus(false);
    String value = pagesize.getValue();
    int i = Integer.parseInt(value);
    pagingContainer.setPageSize(i);
    controller.getStorage().setInt("pageSizeSelected", pagesize.getSelectedIndex());
    scrollToTop();
  }

  private void scrollToTop() {
    Window.scrollTo(0, 0);
  }

  private boolean finished = false;

  @NotNull
  private DivWidget addSortBox(ExerciseController controller) {
    DivWidget w = new DivWidget();
    w.addStyleName("floatRight");
    HTML w1 = new HTML("Sort");
    w1.addStyleName("topFiveMargin");
    w1.addStyleName("rightFiveMargin");
    w1.addStyleName("floatLeft");
    w.addStyleName("inlineFlex");

    w.add(w1);
    w.add(new ListSorting<>(this).getSortBox(controller));
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
  protected ClickablePagingContainer<CommonShell> makePagingContainer() {
    pagingContainer =
        new ClickablePagingContainer<CommonShell>(controller
        ) {
          public void gotClickOnItem(CommonShell e) {
          }

          @Override
          protected void addColumnsToTable(boolean sortEnglish) {
          }

          @Override
          protected void gotRangeChanged(Range newRange) {
            logger.info("makePagingContainer : gotRangeChanged for " + newRange);
            gotVisibleRangeChanged(getIdsForRange(newRange));
          }

          @Override
          protected int getNumTableRowsGivenScreenHeight() {
            int pageSize = getPageIndex();
            if (pageSize != -1) {
              try {
                return PAGE_SIZE_CHOICES.get(pageSize);
              } catch (Exception e) {
                return numToShow;
              }
            } else {

              return numToShow;
            }
          }
        };
    return pagingContainer;
  }

  /**
   * NO-OP on drill view
   *
   * @param idsForRange
   */
  protected void gotVisibleRangeChanged(Collection<Integer> idsForRange) {
    askServerForExercises(-1, idsForRange);
  }

  /**
   * @param userID
   * @see HistoryExerciseList#noSectionsGetExercises
   */
  public boolean getExercises(final long userID) {
    addWidgets();
    return false;
  }

  /**
   * @see
   */
 /* public void loadFirst() {
    // pushFirstSelection(getFirstID(), getTypeAheadText());
    // restoreUIState(getSelectionState(getHistoryToken()));
    //selectionStateChanged(getHistoryToken());

    goToFirst(getTypeAheadText(), getFirstID());

  }
*/
/*  protected void goToFirst(String searchIfAny, int exerciseID) {
    logger.info("goToFirst Go to first " + searchIfAny + " " + exerciseID);
//    if (exerciseID < 0) {
//      loadFirstExercise(searchIfAny);
//    } else {
    markCurrentExercise(exerciseID);
    // logger.info("goToFirst pushFirstSelection " + exerciseID + " searchIfAny '" + searchIfAny + "'");
//      pushFirstSelection(exerciseID, searchIfAny);

    //selectionStateChanged(getHistoryToken());

//    SelectionState selectionState = getSelectionState(getHistoryToken());
    //   loadFromSelectionState(selectionState, selectionState);

    checkAndAskServer(exerciseID);

    // }
  }*/

  /**
   * @seex mitll.langtest.client.bootstrap.FlexSectionExerciseList#getExercises(long)
   * @seex mitll.langtest.client.custom.content.FlexListLayout#doInternalLayout(UserList, String)
   * @see #getExercises(long)
   */
  @Override
  public void addWidgets() {
    sectionPanel.clear();
    addTypeAhead(sectionPanel);
    sectionPanel.add(typeOrderContainer = getWidgetsForTypes());
    restoreUIState(getSelectionState(getHistoryToken()));
  }

  private Panel typeOrderContainer;

  /**
   * Assume for the moment that the first type has the largest elements... and every other type nests underneath it.
   *
   * @return
   * @see #addWidgets
   */
  private Panel getWidgetsForTypes() {
    final UnorderedList container = new UnorderedList();
    container.getElement().setId("typeOrderContainer");
    getTypeOrder();
    return container;
  }

  private void getTypeOrder() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    typeOrder = projectStartupInfo.getTypeOrder();

    this.rootNodesInOrder = new HashSet<>(projectStartupInfo.getTypeOrder());
    this.rootNodesInOrder.retainAll(projectStartupInfo.getRootNodes());
  }

  /**
   * @param typeToSelection
   * @see #getTypeToValues
   */
  private void setTypeToSelection(Map<String, String> typeToSelection) {
    this.typeToSelection = typeToSelection;
  }


  /**
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
   */
  private void addFacetsForReal(Map<String, Set<MatchInfo>> typeToValues, Panel nav) {
    logger.info("addFacetsForReal" +//\n\tfor " +
        //" nodes" +
        "\n\t# root nodes = " + rootNodesInOrder.size() + " " + rootNodesInOrder +
        "\n\ttype->distinct " + typeToValues.keySet() +
        "\n\ttype->sel      " + typeToSelection);

    // nav -
    //   ul
    UnorderedList allTypesContainer = new UnorderedList(); //ul
    nav.clear();
    nav.add(allTypesContainer);

    for (String type : rootNodesInOrder) {
      // nav
      //  ul
      //   li - dimension
      ListItem liForDimensionForType = getTypeContainer(type);
      allTypesContainer.add(liForDimensionForType);

      // nav
      //  ul
      //   li
      //    ul
      //
      liForDimensionForType.add(addChoices(typeToValues, type));
    }

    {
      ListItem liForDimensionForType = getTypeContainer(LISTS);
      allTypesContainer.add(liForDimensionForType);

      populateListChoices(liForDimensionForType);
      liForDimensionForList = liForDimensionForType;
    }
  }

  private ListItem liForDimensionForList;

  private void gotListChanged() {
    if (liForDimensionForList != null) {
      populateListChoices(liForDimensionForList);
    }
  }

  /**
   * @param typeToSection
   * @param prefix
   * @param onlyWithAudioAnno
   * @param onlyUnrecorded
   * @param onlyDefaultUser
   * @param onlyUninspected
   * @return
   */
  protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection,
                                                       String prefix,
                                                       boolean onlyWithAudioAnno,
                                                       boolean onlyUnrecorded,
                                                       boolean onlyDefaultUser,
                                                       boolean onlyUninspected) {
    ExerciseListRequest exerciseListRequest = getRequest(prefix)
        .setTypeToSelection(typeToSection)
        .setOnlyWithAudioAnno(onlyWithAudioAnno)
        .setOnlyUnrecordedByMe(onlyUnrecorded)
        .setOnlyDefaultAudio(onlyDefaultUser)
        .setOnlyUninspected(onlyUninspected);

    //logger.info("Type->sel " + typeToSection);
    if (typeToSection.containsKey(LISTS)) {
      Collection<String> strings = typeToSection.get(LISTS);

      //only one user list can be selected, and they don't nest
      String next = strings.iterator().next();
      try {
        Integer userListID = Integer.parseInt(next);
        exerciseListRequest.setUserListID(userListID);
        logger.info("getExerciseListRequest userlist = " + userListID);
      } catch (NumberFormatException e) {
        logger.warning("couldn't parse " + next);
      }
    }
    return exerciseListRequest;
  }

  /**
   * TODO: reverse this - get the lists first, then build the facets
   *
   * @param liForDimensionForType
   */
  private void populateListChoices(ListItem liForDimensionForType) {
    ListServiceAsync listService = controller.getListService();
    //logger.info("populateListChoices ");
    listService.getListsForUser(true, false, new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        Map<String, Set<MatchInfo>> typeToValues = new HashMap<>();

        typeToValues.put(LISTS, getMatchInfoForEachList(result));

        Widget first = liForDimensionForType.getWidget(0);

        Panel w = addChoices(typeToValues, LISTS);

        liForDimensionForType.clear();
        liForDimensionForType.add(first);
        liForDimensionForType.add(w);
      }
    });
  }

  @NotNull
  private Set<MatchInfo> getMatchInfoForEachList(Collection<UserList<CommonShell>> result) {
    Set<MatchInfo> value = new HashSet<>();
    idToName.clear();
    for (UserList<?> list : result) {
      value.add(new MatchInfo(list.getName(), list.getNumItems(), list.getID()));
      idToName.put(list.getID(), list.getName());
    }
    return value;
  }


  @NotNull
  private ListItem getTypeContainer(String type) {
    boolean hasSelection = typeToSelection.containsKey(type);
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
   * @return
   * @see #addFacetsForReal
   */
  private Panel addChoices(Map<String, Set<MatchInfo>> typeToValues, String type) {
    Panel choices = new UnorderedList(); // ul
    String selectionForType = typeToSelection.get(type);

    // logger.info("addChoices " + type + "=" + selectionForType);
    if (selectionForType == null) { // no selection made, show all possible values for type
      Set<MatchInfo> keys = typeToValues.get(type);
      if (keys != null) {
        addChoicesForType(typeToValues, type, choices, keys);
      }
    } else {
      String childType = getChildForParent(type);
      if (childType != null) {
        Widget parentAnchor =
            typeToSelection.containsKey(childType) ?
                getParentAnchor(type, selectionForType, childType) :
                getSelectedAnchor(type, selectionForType);
        choices.add(parentAnchor);

        ListItem liForDimension = new ListItem();
        liForDimension.addStyleName("subdimension");
        liForDimension.addStyleName("refinement");
        choices.add(liForDimension);

        liForDimension.add(addChoices(typeToValues, childType));
      } else {
        if (type.equalsIgnoreCase(LISTS)) {
          try {
            int i = Integer.parseInt(selectionForType);
            String s = idToName.get(i);
            if (s != null) selectionForType = s;
          } catch (NumberFormatException e) {
            logger.warning("could n't parse " + selectionForType);
          }
        }
        choices.add(getSelectedAnchor(type, selectionForType));
      }
    }
    return choices;
  }

  private void addChoicesForType(Map<String, Set<MatchInfo>> typeToValues,
                                 String type,
                                 Panel choices,
                                 Set<MatchInfo> keys) {
    int j = 0;
    int toShow = TOTAL / typeToValues.keySet().size();
    int diff = keys.size() - toShow;
    boolean hasMore = keys.size() > toShow && diff > CLOSE_TO_END;
    Boolean showAll = typeToShowAll.getOrDefault(type, false);
    for (MatchInfo key : keys) {
      addLIChoice(choices, getAnchor(type, key, key.getUserListID()));

      if (hasMore && !showAll && ++j == toShow) {
        addLIChoice(choices, getShowMoreAnchor(typeToValues, type));
        break;
      }
    }
    if (hasMore && showAll) {
      addLIChoice(choices, getShowLess(typeToValues, type));
    }
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
   * @return
   */
  @NotNull
  private Anchor getShowMoreAnchor(final Map<String, Set<MatchInfo>> typeToValues, final String type) {
    Anchor anchor = new Anchor();

    anchor.setHTML(SHOW_MORE);
    anchor.addClickHandler(event -> {
      typeToShowAll.put(type, true);
      addFacetsForReal(typeToValues, typeOrderContainer);
    });
    return anchor;
  }

  /**
   * @param type
   * @param selectionForType
   * @return
   * @see #addChoices(Map, String)
   */
  @NotNull
  private Widget getSelectedAnchor(String type, String selectionForType) {
    Panel anchor = getSpan();
    anchor.getElement().setInnerHTML(selectionForType);
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

  private Widget getAnchor(String type, MatchInfo key, int newUserListID) {
    Panel span = getSpan();
    span.addStyleName(MENU_ITEM);

    addMatchInfoTooltip(type, key, span);

    String choiceName = key.getValue();
    ClickHandler choiceHandler = getChoiceHandler(type, choiceName, newUserListID);

    Anchor anchor = getAnchor(choiceName);
    anchor.addClickHandler(choiceHandler);
    anchor.addStyleName("choice");
    span.add(anchor);

    {
      String countLabel = "" + key.getCount();

      Anchor qty = getAnchor(countLabel);
      qty.addClickHandler(choiceHandler);
      qty.addStyleName("qty");
      span.add(qty);
    }

    return span;
  }

  private void addMatchInfoTooltip(String type, MatchInfo key, Panel span) {
    addTooltip(type, key.getValue(), span);
  }

  private Tooltip addTooltip(String type, String value, Panel span) {
    return new TooltipHelper().addTooltip(span, type + " " + value);
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
   * @return
   * @see #addChoices
   */
  @NotNull
  private Widget getParentAnchor(String type, String value, String childType) {
    Panel span = getSpan();
    span.addStyleName(MENU_ITEM);
    Anchor typeSection = getAnchor(value); // li
    //if (type.equals(LISTS)) userListID = -1;
    addRemoveClickHandler(childType, typeSection);
    span.add(typeSection);
    addTooltip(type, value, span);
    return span;
  }

  private void addRemoveClickHandler(final String type, Anchor typeSection) {
    typeSection.addClickHandler(event -> {
      removeSelection(type);
      int selectedUserListID = -1;
      if (type.equals(LISTS)) {
        selectedUserListID = -1;
      } else if (typeToSelection.containsKey(LISTS)) {
        try {
          String s = typeToSelection.get(LISTS);
          logger.info("addRemoveClickHandler list sel is " + s);
          selectedUserListID = Integer.parseInt(s);
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
      }

      getTypeToValues(typeToSelection, selectedUserListID);
    });
  }

  private boolean removeSelection(String childType) {
    Map<String, String> typeToSelection = this.typeToSelection;
    return removeSelection(childType, typeToSelection);
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
    return controller.getProjectStartupInfo().getParentToChild().get(childType);
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
      Map<String, String> candidate = new HashMap<>(typeToSelection);
      candidate.put(type, type.equalsIgnoreCase(LISTS) ? "" + newUserListID : key);
//      logger.info("getChoiceHandler " + type + "=" + key + " " + newUserListID);
      getTypeToValues(candidate, newUserListID);
    };
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
   * @see #getSectionWidgetContainer()
   */
  private void getTypeToValues(Map<String, String> typeToSelection, int userListID) {
    List<Pair> pairs = getPairs(typeToSelection);
    logger.info("getTypeToValues request " + pairs + " list " + userListID);
    controller.getExerciseService().getTypeToValues(new FilterRequest(reqid++, pairs, userListID),
        new AsyncCallback<FilterResponse>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          /**
           * TODO : fix downstream selections that no longer make sense.
           * @param response
           */
          @Override
          public void onSuccess(FilterResponse response) {
            Map<String, Set<MatchInfo>> result = response.getTypeToValues();
            //   logger.info("getTypeToValues for " + pairs + " got " + result.size());

            if (response.getUserListID() != -1) {

            }
            changeSelection(response.getTypesToInclude(), typeToSelection);
            setTypeToSelection(typeToSelection);
            addFacetsForReal(response.getTypeToValues(), typeOrderContainer);
            gotSelection();
          }
        });
  }

  @NotNull
  private List<Pair> getPairs(Map<String, String> typeToSelection) {
    List<Pair> pairs = new ArrayList<>();

    for (String type : controller.getProjectStartupInfo().getTypeOrder()) {
      String s = typeToSelection.get(type);
      if (s == null) {
        pairs.add(new Pair(type, ANY));
      } else {
        pairs.add(new Pair(type, s));
      }
    }
    if (typeToSelection.containsKey(LISTS)) {
      pairs.add(new Pair(LISTS, typeToSelection.get(LISTS)));
    }
    return pairs;
  }

  private boolean changeSelection(Set<String> typesToInclude, Map<String, String> typeToSelection) {
    boolean removed = false;

    for (String selectedType : new ArrayList<>(typeToSelection.keySet())) {
      boolean clearSelection = !typesToInclude.contains(selectedType);
      if (clearSelection) {
        if (removeSelection(selectedType, typeToSelection)) {
          removed = true;
        }
      }
    }

    return removed;
  }

  /**
   * @seex ButtonBarSectionWidget#getChoice(ButtonGroup, String)
   */
  private void gotSelection() {
    pushNewSectionHistoryToken();
  }

  /**
   * @param selectionState
   * @see HistoryExerciseList#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  @Override
  protected void restoreListBoxState(SelectionState selectionState) {
    //  logger.info("restoreListBoxState " + selectionState);
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
    // keep the download link info in sync with the selection
    // Map<String, Collection<String>> typeToSection = selectionState.getTypeToSection();
    //  logger.info("showSelectionState : typeOrder " + typeOrder + " selection state " + typeToSection);
    downloadHelper.updateDownloadLinks(selectionState, typeOrder);
  }

  /**
   * @seex #rememberAndLoadFirst
   */
  @Override
  protected void loadFirstExercise(String searchIfAny) {
    // logger.info("loadFirstExercise : ---");
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

  protected FacetContainer getSectionWidgetContainer() {
    return new FacetContainer() {
      /**
       * @see HistoryExerciseList#restoreListBoxState
       * @param selectionState
       * @param typeOrder
       */
      @Override
      public void restoreListBoxState(SelectionState selectionState, Collection<String> typeOrder) {
        //      logger.info("restoreListBoxState t->sel    " + selectionState + " typeOrder " + typeOrder);
        Map<String, String> newTypeToSelection = getNewTypeToSelection(selectionState, typeOrder);
        if (typeToSelection.equals(newTypeToSelection) && typeOrderContainer.iterator().hasNext()) {
          logger.info("restoreListBoxState state already consistent with " + newTypeToSelection);
        } else {
          int userListID = -1;
          try {
            userListID = newTypeToSelection.containsKey(LISTS) ? Integer.parseInt(newTypeToSelection.get(LISTS)) : -1;
          } catch (NumberFormatException e) {
            logger.warning("can't parse " + newTypeToSelection.get(LISTS));
          }
          getTypeToValues(newTypeToSelection, userListID);
        }
      }

      /**
       * @see HistoryExerciseList#getHistoryTokenFromUIState
       * @return
       */
      @Override
      public String getHistoryToken() {
        StringBuilder builder = new StringBuilder();
//        logger.info("getHistoryToken t->sel " + typeToSelection);

        for (Map.Entry<String, String> pair : typeToSelection.entrySet()) {
          builder.append(pair.getKey()).append("=").append(pair.getValue()).append(SECTION_SEPARATOR);
        }

        String s = builder.toString();
//        logger.info("getHistoryToken token '" + s + "'");
        return s;
      }

      @Override
      public int getNumSelections() {
        return 0;
      }
    };
  }

  @NotNull
  private Map<String, String> getNewTypeToSelection(SelectionState selectionState, Collection<String> typeOrder) {
    typeOrder = new ArrayList<>(typeOrder);
    typeOrder.add(LISTS);

    Map<String, String> newTypeToSelection = new HashMap<>();
    for (String type : typeOrder) {
      Collection<String> selections = selectionState.getTypeToSection().get(type);
      if (selections != null && !selections.isEmpty()) {
        newTypeToSelection.put(type, selections.iterator().next());
      }
    }
    return newTypeToSelection;
  }

  private int freqid = 0;

  /**
   * TODO : do different thing for AVP.
   * <p>
   * TODO : scroll to visible item
   * <p>
   * goes ahead and asks the server for the next item so we don't have to wait for it.
   *
   * @param itemID
   * @see ListInterface#checkAndAskServer(int)
   */
  protected void askServerForExercise(int itemID) {
    askServerForExercises(itemID, pagingContainer.getVisibleIDs());
  }

  private void askServerForExercises(int itemID, Collection<Integer> visibleIDs) {
    logger.info("askServerForExercises ask for single -- " + itemID + " and " + visibleIDs.size());
    if (visibleIDs.isEmpty() && pagingContainer.isEmpty() && finished) {
      logger.info("askServerForExercises show empty -- ");
      //  showEmptyExercise();
    } else {
      if (numToShow == 1 && itemID > 0) {
        visibleIDs = new ArrayList<>();
        visibleIDs.add(itemID);
        logger.info("askServerForExercises ask for single -- " + itemID);
      }

      // ensureAudio(visibleIDs);
      getExercises(visibleIDs);
    }
  }

  private boolean isStaleReq(ExerciseListWrapper<?> req) {
    return !isCurrentReq(req);
  }

  private boolean isCurrentReq(ExerciseListWrapper<?> req) {
    return req.getReqID() == freqid - 1;
  }

/*  private void ensureAudio(Collection<Integer> visibleIDs) {
    long then = System.currentTimeMillis();
    controller.getAudioService().ensureAudioForIDs(controller.getProjectStartupInfo().getProjectid(), visibleIDs,
        new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            getExercises(visibleIDs);
          }

          @Override
          public void onSuccess(Void result) {
            long now = System.currentTimeMillis();
            getExercises(visibleIDs);
            logger.info("ensureAudio OK, ensured audio... in " + (now - then) + " millis");
          }
        });
  }*/

  private void hidePrevNext() {
    hidePrevNextWidgets();
    progressBar.setVisible(false);
    //logger.info("hidePrevNext ------- ");
    //  clearExerciseContainer();
  }

  private void hidePrevNextWidgets() {
//    logger.info("hidePrevNextWidgets ------- ");

/*    prev.setVisible(false);
    next.setVisible(false);*/
    pageSizeContainer.setVisible(false);
    sortBox.setVisible(false);
  }

  private void showPrevNext() {
    //  logger.info("showPrevNext ------- ");

/*    prev.setVisible(true);
    next.setVisible(true);*/
    pageSizeContainer.setVisible(true);
    sortBox.setVisible(true);
    progressBar.setVisible(true);

/*
    enablePrevNext();
*/
  }

/*  private void enablePrevNext() {
    // logger.info("enablePrevNext ------- ");
    prev.setEnabled(pagingContainer.hasPrevPage());
    next.setEnabled(pagingContainer.hasNextPage());
  }*/

  /**
   * @param visibleIDs
   * @seex #ensureAudio
   */
  private void getExercises(Collection<Integer> visibleIDs) {
    long then = System.currentTimeMillis();

    logger.info("getExercises asking for " + visibleIDs.size() + " visible ");

    service.getFullExercises(freqid++, visibleIDs, false,
        new AsyncCallback<ExerciseListWrapper<CommonExercise>>() {
          @Override
          public void onFailure(Throwable caught) {
            dealWithRPCError(caught);
            hidePrevNext();
          }

          @Override
          public void onSuccess(ExerciseListWrapper<CommonExercise> result) {
            long now = System.currentTimeMillis();
            logger.info("getExercises took " + (now - then) + " to get " + result.getExercises().size() + " exercises");
            if (isCurrentReq(result)) {
              gotFullExercises(result);
            } else {
              logger.info("getExercises : ignoring req " + result.getReqID() + " vs current " + freqid);
            }
          }
        });
  }

  private void gotFullExercises(ExerciseListWrapper<CommonExercise> result) {
    if (result.getExercises().isEmpty()) {
      //showEmptyExercise();
      hidePrevNext();
    } else {
      if (numToShow == 1) { // hack for avp
        hidePrevNextWidgets();
        showExercises(result.getExercises(), result);
        progressBar.setVisible(false);
      } else {
        showExercises(result.getExercises(), result);
      }
    }
  }

  private Set<Integer> exercisesWithScores = new HashSet<>();

  /**
   * @param result
   * @param wrapper
   * @see #gotFullExercises
   */
  private void showExercises(Collection<CommonExercise> result, ExerciseListWrapper<CommonExercise> wrapper) {
    if (numToShow == 1) { // drill/avp/flashcard
      CommonExercise next = result.iterator().next();
      addExerciseWidget(next, wrapper);
      markCurrentExercise(next.getID());
    } else {
      DivWidget exerciseContainer = new DivWidget();
//      Panel scrollPanel = new ScrollPanel(exerciseContainer);
//      DockLayoutPanel layoutPanel = new DockLayoutPanel(Style.Unit.PX);
//      layoutPanel.add(scrollPanel);

      int reqID = wrapper.getReqID();
      boolean first = true;
      for (CommonExercise exercise : result) {
        if (isStaleReq(wrapper)) {
          logger.info("showExercises stop stale req " + reqID + " vs  current " + (freqid - 1));
          break;
        }
        if (first) {
          markCurrentExercise(exercise.getID());
          first = false;
        }
        exerciseContainer.add(factory.getExercisePanel(exercise, wrapper));
      }
      if (isStaleReq(wrapper)) {
        logger.info("showExercises Skip stale req " + reqID);
      } else {
        Scheduler.get().scheduleDeferred(new Command() {
          public void execute() {
            setProgressBarScore(getInOrder());
          }
        });
        //  innerContainer.setWidget(layoutPanel);
        innerContainer.setWidget(exerciseContainer);
//        innerContainer.getElement().getStyle().setPosition(Style.Position.FIXED);
//        innerContainer.getElement().getStyle().setTop(85, Style.Unit.PX);
//        innerContainer.getElement().getStyle().setRight(210, Style.Unit.PX);
        showPrevNext();
      }
    }
  }

  private void setProgressBarScore(Collection<CommonShell> result) {
    exercisesWithScores.clear();
    for (CommonShell exercise : result) {
      if (exercise.getScore() > -1) exercisesWithScores.add(exercise.getID());
    }
    //progressBar.setPercent(100f*(float)exercisesWithScores.size()/(float)result.size());
    showScore(exercisesWithScores.size(), result.size());
  }

  @Override
  public void setScore(int id, float hydecScore) {
    super.setScore(id, hydecScore);
    if (hydecScore > -1f) exercisesWithScores.add(id);
    showScore(exercisesWithScores.size(), pagingContainer.getSize());
  }

  private void showScore(int num, int denom) {
    double score = (float) num / (float) denom;
    double percent = 100 * score;

    // logger.info("showScore percent is " + percent);
    progressBar.setPercent(num == 0 ? 10 : percent);
    boolean allDone = num == denom;

    String text =
        num == 0 ? "None practiced yet." :
            allDone ? "All practiced!" : (num +
                //" out of " + denom +
                " practiced.");

    progressBar.setText(text);//"" + Math.round(score));//(score));
    progressBar.setColor(
        allDone ? ProgressBarBase.Color.SUCCESS :
            percent > SECOND_STEP ?
                ProgressBarBase.Color.DEFAULT :
                percent > FIRST_STEP ?
                    ProgressBarBase.Color.INFO :
                    ProgressBarBase.Color.WARNING);

    progressBar.setVisible(true);

 /*   if (progressTooltip == null) {
      progressTooltip = new TooltipHelper().addTooltip(progressBar, text);
    } else {
      progressTooltip.setText(text);
      progressTooltip.reconfigure();
    }*/
  }
}
