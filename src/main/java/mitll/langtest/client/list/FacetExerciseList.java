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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.Range;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.download.DownloadEvent;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.client.download.ShowEvent;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.scoring.ListChangedEvent;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.client.scoring.RefAudioGetter;
import mitll.langtest.client.services.ListServiceAsync;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.custom.IUserList;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static mitll.langtest.client.dialog.ExceptionHandlerDialog.getExceptionAsString;
import static mitll.langtest.client.scoring.ScoreFeedbackDiv.FIRST_STEP;
import static mitll.langtest.client.scoring.ScoreFeedbackDiv.SECOND_STEP;

public abstract class FacetExerciseList extends HistoryExerciseList<CommonShell, CommonExercise> {
  private final Logger logger = Logger.getLogger("FacetExerciseList");


  public static final String PAGE_SIZE_SELECTED = "pageSizeSelected";

  private static final String ADDING_VISITOR = "adding visitor";
  private static final String GETTING_LISTS_FOR_USER = "getting simple lists for user";
  private static final String GETTING_TYPE_VALUES = "getting type->values";

  /**
   *
   */
  private static final boolean DEBUG_STALE = true;

  private static final String NONE_PRACTICED_YET = "None practiced yet.";
  /**
   * @see #showScore
   */
  private static final String ALL_PRACTICED = "All practiced!";
  private static final String LINK_FOR_CONTENT = "Link for content in different project.";
  private static final String PLEASE_CHANGE = "Please change to that project if you want to see the item.";

  public static final String LISTS = "Lists";

  /**
   * @see #addPageSize
   */
  private static final String PAGE_SIZE_HEADER = "View";

  /**
   * Smaller on a laptop.
   *
   * @see mitll.langtest.client.banner.NewLearnHelper#getMyListLayout
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

  /**
   * @see #getShowMoreAnchor
   */
  private static final String SHOW_MORE = "<i>View all</i>";
  private static final String ANY = "Any";
  private static final String MENU_ITEM = "menuItem";


  private final ProgressBar progressBar;
  private final DivWidget pagerAndSortRow;

  private List<String> typeOrder;
  private final Panel sectionPanel;
  private final DownloadHelper downloadHelper;
  // private final int numToShow;
  private Panel tableWithPager;
  private boolean isDrill;
  /**
   * for now only single selection
   */
  private Map<String, String> typeToSelection = new HashMap<>();
  /**
   * @see #addChoicesForType
   */
  private final Map<String, Boolean> typeToShowAll = new HashMap<>();
  private List<String> rootNodesInOrder = new ArrayList<>();
  private final Map<Integer, String> idToListName = new HashMap<>();
  private int freqid = 0;
  private DivWidget sortBox;
  private DivWidget pageSizeContainer;

  /**
   * @param secondRow             add the section panel to this row
   * @param currentExerciseVPanel
   * @param controller
   * @param listOptions
   * @param listHeader
   * @param isDrillView           hack - should not have drill stuff in here... although eventually there won't be a drill view
   * @see mitll.langtest.client.custom.content.NPFlexSectionExerciseList#NPFlexSectionExerciseList
   */
  public FacetExerciseList(Panel secondRow,
                           Panel currentExerciseVPanel,
                           ExerciseController controller,
                           ListOptions listOptions,
                           DivWidget listHeader,
                           boolean isDrillView) {
    super(currentExerciseVPanel, controller, listOptions);

    this.isDrill = isDrillView;
    sectionPanel = new DivWidget();
//    sectionPanel.getElement().setId("sectionPanel_" + getInstance());
    sectionPanel.addStyleName("rightFiveMargin");

    secondRow.add(sectionPanel);
    setUnaccountedForVertical(0);

    downloadHelper = new DownloadHelper(this);

    DivWidget breadRow = new DivWidget();
    breadRow.setWidth("100%");
    breadRow.getElement().setId("breadRow");
    //  breadRow.addStyleName("floatLeftList");

    breadRow.add(progressBar = new ProgressBar(ProgressBarBase.Style.DEFAULT));
    styleProgressBarContainer(progressBar);
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

    // so for instance if in TwoColumnExercisePanel there's an addList, removeList, newList
    LangTest.EVENT_BUS.addHandler(ListChangedEvent.TYPE, authenticationEvent -> {
      gotListChanged();
    });

    LangTest.EVENT_BUS.addHandler(DownloadEvent.TYPE, authenticationEvent -> {
      downloadHelper.showDialog(controller.getHost());
    });

    // should be better for change visibility...
    LangTest.EVENT_BUS.addHandler(ShowEvent.TYPE, authenticationEvent -> {
      askServerForExercise(-1);
    });
  }

  /**
   * @param controller
   * @return
   */
  @NotNull
  private DivWidget getPagerAndSort(ExerciseController controller) {
    DivWidget pagerAndSort = new DivWidget();
    pagerAndSort.getElement().setId("pagerAndSort");
    pagerAndSort.addStyleName("inlineFlex");
    pagerAndSort.setWidth("100%");

    pagerAndSort.add(tableWithPager);
    tableWithPager.addStyleName("floatLeft");
    tableWithPager.setWidth("100%");
    tableWithPager.getElement().getStyle().setProperty("minWidth", "250px");

//    DivWidget expander = new DivWidget();
//    expander.setWidth("100%");
//    pagerAndSort.add(expander);
//    expander.addStyleName("floatRight");
    {
      Dropdown realViewMenu = new DisplayMenu(controller.getStorage()).getRealViewMenu();
      DivWidget widgets = new DivWidget();
      widgets.addStyleName("topFiveMargin");
      widgets.add(realViewMenu);
      pagerAndSort.add(widgets);
    }

    addPageSize(pagerAndSort);

    pagerAndSort.add(this.sortBox = addSortBox(controller.getLanguage()));

    return pagerAndSort;
  }

  private void styleProgressBarContainer(ProgressBar progressBar) {
    Style style = progressBar.getElement().getStyle();
    style.setMarginTop(5, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);
    progressBar.setVisible(false);
  }

  /**
   * @param footer
   * @return
   * @see #FacetExerciseList
   */
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

    PAGE_SIZE_CHOICES.forEach(num -> pagesize.addItem(num + ITEMS_PAGE, "" + num));

    int chosenPageSize = getChosenPageIndex();
//    logger.info("chose size " + chosenPageSize);
    pagesize.setItemSelected(chosenPageSize, true);
    pagesize.addChangeHandler(event -> onPageSizeChange(pagesize));

    return pagesize;
  }

  private int getChosenPageIndex() {
    int pageIndex = getPageIndex();
//    logger.info("getChosenPageIndex pageIndex " + pageIndex);
    return pageIndex == -1 ? PAGE_SIZE_CHOICES.indexOf(FIRST_PAGE_SIZE) : pageIndex;
  }

  private int getChosenPageSize() {
    return PAGE_SIZE_CHOICES.get((getChosenPageIndex()));
  }

  private int getPageIndex() {
    return controller.getStorage().getInt(PAGE_SIZE_SELECTED);
  }

  private void onPageSizeChange(ListBox pagesize) {
    pagesize.setFocus(false);
    String value = pagesize.getValue();
    int i = Integer.parseInt(value);
    pagingContainer.setPageSize(i);
    controller.getStorage().setInt(PAGE_SIZE_SELECTED, pagesize.getSelectedIndex());
    scrollToTop();
  }

  private void scrollToTop() {
    Window.scrollTo(0, 0);
  }

  private final boolean finished;
  ListBox sortBoxReally;

  @NotNull
  private DivWidget addSortBox(String language) {
    DivWidget w = new DivWidget();
    w.addStyleName("floatRight");

    HTML w1 = new HTML("Sort");
    w1.addStyleName("topFiveMargin");
    w1.addStyleName("rightFiveMargin");
    w1.addStyleName("floatLeft");
    w.addStyleName("inlineFlex");

    w.add(w1);

    sortBoxReally = new ListSorting<>(this).getSortBox(language);
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
  protected ClickablePagingContainer<CommonShell> makePagingContainer() {
    pagingContainer =
        new ClickablePagingContainer<CommonShell>(controller
        ) {
          public void gotClickOnItem(CommonShell e) {
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
            askServerForExercise(-1);
//            long then = System.currentTimeMillis();
            //     logger.info("gotRangeChanged event for " + newRange);
/*            final int currentReq = incrReq();

            //  logger.info("makePagingContainer : gotRangeChanged for " + newRange);
            Scheduler.get().scheduleDeferred((Command) () -> {
              if (isCurrentReq(currentReq)) {
//                long now = System.currentTimeMillis();
                //   logger.info("gotRangeChanged (" + (now - then) + ") req " + currentReq + "  for  " + newRange);
                gotVisibleRangeChanged(getIdsForRange(newRange), currentReq);
              }
//              else {
//                logger.warning("gotRangeChanged STALE req " + currentReq + "  for  " + newRange);
//              }
            });*/
          }

          @Override
          protected int getNumTableRowsGivenScreenHeight() {
            int pageSize = getChosenPageSize();
            //     logger.info("getNumTableRowsGivenScreenHeight " + pageSize + " vs " + numToShow);
            return pageSize;
          }
        };
    return pagingContainer;
  }

  /**
   * NO-OP on drill view
   *
   * @param idsForRange
   * @param currentReq
   * @see #makePagingContainer
   */
  protected void gotVisibleRangeChanged(Collection<Integer> idsForRange, final int currentReq) {
    askServerForVisibleExercises(-1, idsForRange, currentReq);
  }

  /**
   * @see HistoryExerciseList#noSectionsGetExercises
   */
  public boolean getExercises() {
    addWidgets();
    return false;
  }

  @Override
  protected ExerciseListRequest getRequest(String prefix) {
    ExerciseListRequest request = super.getRequest(prefix);
    request.setAddFirst(false);
    return request;
  }

  /**
   * @seex mitll.langtest.client.custom.content.FlexListLayout#doInternalLayout(UserList, String)
   * @see ListInterface#getExercises
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
    if (projectStartupInfo == null) logger.warning("no project startup info?");
    else {
      typeOrder = projectStartupInfo.getTypeOrder();
      //    logger.info("getTypeOrder type order " + typeOrder);
      this.rootNodesInOrder = new ArrayList<>(typeOrder);
      this.rootNodesInOrder.retainAll(projectStartupInfo.getRootNodes());
    }
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
/*
    logger.info("addFacetsForReal" +
        "\n\t# root nodes = " + rootNodesInOrder.size() + " " + rootNodesInOrder +
        "\n\ttype->distinct " + typeToValues.keySet() +
        "\n\ttype->sel      " + typeToSelection);
        */

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
      // logger.info("addChoices --- " + type);
      liForDimensionForType.add(addChoices(typeToValues, type));
    }

    allTypesContainer.add(liForDimensionForList = addListFacet(typeToValues));
  }

  private ListItem liForDimensionForList;

  /**
   * @see #FacetExerciseList(Panel, Panel, ExerciseController, ListOptions, DivWidget, boolean)
   */
  private void gotListChanged() {
    if (liForDimensionForList != null) {
      populateListChoices(liForDimensionForList, null);
    }
  }

  @NotNull
  private ListItem addListFacet(Map<String, Set<MatchInfo>> typeToValues) {
    ListItem liForDimensionForType = getTypeContainer(LISTS);
    populateListChoices(liForDimensionForType, typeToValues);
    return liForDimensionForType;
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
        .setOnlyUninspected(onlyUninspected)
        .setAddFirst(false);

    //logger.info("Type->sel " + typeToSection);
    if (typeToSection.containsKey(LISTS)) {
      Collection<String> strings = typeToSection.get(LISTS);

      //only one user list can be selected, and they don't nest
      String next = strings.iterator().next();
      try {
        Integer userListID = Integer.parseInt(next);
        exerciseListRequest.setUserListID(userListID);
        //   logger.info("getExerciseListRequest userlist = " + userListID);
      } catch (NumberFormatException e) {
        logger.warning("couldn't parse " + next);
      }
    }
    return exerciseListRequest;
  }

  /**
   * Cheesy hack so we can deal with adding lists without reloading the whole facets
   */
  private Map<String, Set<MatchInfo>> lastTypeToValues;

  /**
   * TODO: reverse this - get the lists first, then build the facets
   *
   * @param liForDimensionForType
   * @param typeToValues          - need to remember this and pass it through so later link clicks will have it
   * @see #addFacetsForReal
   */
  private void populateListChoices(ListItem liForDimensionForType, Map<String, Set<MatchInfo>> typeToValues) {
    ListServiceAsync listService = controller.getListService();
    //logger.info("populateListChoices --- ");

    if (typeToValues == null) {
      typeToValues = lastTypeToValues;
      //logger.info("populateListChoices --- using remembered  " + typeToValues);
    } else {
      lastTypeToValues = new HashMap<>();
      lastTypeToValues.putAll(typeToValues);
    }

    final Map<String, Set<MatchInfo>> finalTypeToValues = typeToValues;
    final long then = System.currentTimeMillis();

    listService.getSimpleListsForUser(true, true, new AsyncCallback<Collection<IUserList>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError(GETTING_LISTS_FOR_USER, caught);
      }

      @Override
      public void onSuccess(Collection<IUserList> result) {
        logger.info("took " + (System.currentTimeMillis() - then) + " to get lists for user.");

        finalTypeToValues.put(LISTS, getMatchInfoForEachList(result));

        Widget favorites = liForDimensionForType.getWidget(0);
        liForDimensionForType.clear();
        liForDimensionForType.add(favorites);
        //  logger.info("populateListChoices --- for " + result.size() + " lists ");
        liForDimensionForType.add(addChoices(finalTypeToValues, LISTS));
      }
    });
  }

  @NotNull
  private Set<MatchInfo> getMatchInfoForEachList(Collection<IUserList> result) {
    Set<MatchInfo> value = new HashSet<>();
    idToListName.clear();
    int currentUser = controller.getUser();
    for (IUserList list : result) {
      boolean isVisit = list.getUserID() != currentUser;
      String tooltip = isVisit ? " from " + list.getUserChosenID() : "";
      value.add(new MatchInfo(list.getName(), list.getNumItems(), list.getID(), isVisit, tooltip));
      idToListName.put(list.getID(), list.getName());
    }
    return value;
  }

  @NotNull
  private ListItem getTypeContainer(String type) {
    if (type.isEmpty()) logger.warning("huh? type is empty???");
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

    //logger.info("addChoices " + type + "=" + selectionForType);

    if (selectionForType == null) { // no selection made, show all possible values for type
      Set<MatchInfo> keys = typeToValues.get(type);
      if (keys != null) {
        //  logger.info("addChoices for " + type + "=" + keys.size());
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
          //   logger.info("addChoices addListChoice " + type + "=" + selectionForType);
          addListChoice(type, choices, selectionForType);
        } else {
          //  logger.info("addChoices getSelectedAnchor " + type + "=" + selectionForType);
          choices.add(getSelectedAnchor(type, selectionForType));
        }
      }
    }
    return choices;
  }

  private void addListChoice(String type, Panel choices, String selectionForType) {
    try {
      int userListID = Integer.parseInt(selectionForType);

      String listName = idToListName.get(userListID);
      if (listName != null) {
        //      logger.info("addListChoice selected : adding " + listName + " " + type);
        choices.add(getSelectedAnchor(type, listName));
      } else {
        logger.info("addListChoice couldn't find list " + userListID + " in known lists...");
        addVisitor(type, choices, userListID);
      }
    } catch (NumberFormatException e) {
      logger.warning("addListChoice couldn't parse " + selectionForType);
    }
  }

  private void addVisitor(String type, Panel choices, int userListID) {
    //logger.info("addVisitor " + type + " : " + userListID);
    controller.getListService().addVisitor(userListID, controller.getUser(), new AsyncCallback<UserList>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError(ADDING_VISITOR, caught);
      }

      @Override
      public void onSuccess(UserList result) {
        if (result.getProjid() != getCurrentProject()) {
          logger.warning("addVisitor list " + result.getName() + " is NOT in the project #" + result.getProjid());
        } else {
          choices.add(getSelectedAnchor(type, result.getName()));
        }
      }
    });
  }

  /**
   * @param typeToValues
   * @param type
   * @param choices
   * @param keys
   * @see #addChoices
   */
  private void addChoicesForType(Map<String, Set<MatchInfo>> typeToValues,
                                 String type,
                                 Panel choices,
                                 Set<MatchInfo> keys) {
    int j = 0;
    int size = rootNodesInOrder.size() + 1;

    int toShow = TOTAL / size;
    int diff = keys.size() - toShow;

    boolean hasMore = keys.size() > toShow && diff > CLOSE_TO_END;
    Boolean showAll = typeToShowAll.getOrDefault(type, false);

/*
    logger.info("addChoices" +
        "\n\ttype    " + type +
        "\n\tsize    " + size +// "(" +types+
        "\n\tto show " + toShow +
        "\n\tkeys    " + keys.size() +
        "\n\thasMore " + hasMore +
        "\n\tshowAll " + showAll
    );
    */

    for (MatchInfo key : keys) {
      addLIChoice(choices, getAnchor(type, key, key.getUserListID(), key.isItalic()));

      // add final "View all" link if we're not supposed to show all
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

  /**
   * @param type
   * @param key
   * @param newUserListID
   * @param italic        true if list is not mine - I don't own it
   * @return
   */
  private Widget getAnchor(String type, MatchInfo key, int newUserListID, boolean italic) {
    Panel span = getSpan();
    span.addStyleName(MENU_ITEM);

    addMatchInfoTooltip(type, key, span);

    String choiceName = key.getValue();

    Anchor anchor = getAnchor(choiceName);
    ClickHandler choiceHandler = getChoiceHandler(type, choiceName, newUserListID);
    anchor.addClickHandler(choiceHandler);
    anchor.addStyleName("choice");

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
   * @return
   * @see #addChoices
   */
  @NotNull
  private Widget getParentAnchor(String type, String value, String childType) {
    Panel span = getSpan();
    span.addStyleName(MENU_ITEM);
    Anchor typeSection = getAnchor(value); // li
    addRemoveClickHandler(childType, typeSection);
    span.add(typeSection);
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
        logger.warning("didn't remove" +
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
    Map<String, String> parentToChild = controller.getProjectStartupInfo().getParentToChild();
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
      Map<String, String> candidate = new HashMap<>(typeToSelection);
      String value = type.equalsIgnoreCase(LISTS) ? "" + newUserListID : key;
      candidate.put(type, value);

      //  logger.info("getChoiceHandler " + type + "=" + key + " " + newUserListID + " value " + value);
      setHistory(candidate);
    };
  }

  /**
   * Remember to keep the search term, if there is any.
   *
   * @param candidate
   * @see #getChoiceHandler
   * @see #addRemoveClickHandler
   */
  private void setHistory(Map<String, String> candidate) {
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
   * @see #getSectionWidgetContainer()
   */
  private void getTypeToValues(Map<String, String> typeToSelection, int userListID) {
    boolean hasUser = controller.getUser() > 0;
    if (!hasUser) return;

    List<Pair> pairs = getPairs(typeToSelection);
    logger.info("getTypeToValues request " + pairs + " list " + userListID + " has user " + hasUser);
    final long then = System.currentTimeMillis();

    controller.getExerciseService().getTypeToValues(new FilterRequest(reqid++, pairs, userListID),
        new AsyncCallback<FilterResponse>() {
          @Override
          public void onFailure(Throwable caught) {
            if (caught instanceof DominoSessionException) {
              logger.info("got " + caught);
            }
//            else {
            controller.handleNonFatalError(GETTING_TYPE_VALUES, caught);
            //          }
          }

          /**
           * fixes downstream selections that no longer make sense.
           * @param response
           */
          @Override
          public void onSuccess(FilterResponse response) {
            logger.info("took " + (System.currentTimeMillis() - then) + " to get type to values.");

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
      pairs.add(new Pair(type, (s == null) ? ANY : s));
    }
    if (typeToSelection.containsKey(LISTS)) {
      pairs.add(new Pair(LISTS, typeToSelection.get(LISTS)));
    }
    return pairs;
  }

  private void changeSelection(Set<String> typesToInclude, Map<String, String> typeToSelection) {
    // boolean removed = false;
    for (String selectedType : new ArrayList<>(typeToSelection.keySet())) {
      boolean clearSelection = !typesToInclude.contains(selectedType);
      if (clearSelection) {
        if (removeSelection(selectedType, typeToSelection)) {
          //     removed = true;
        }
      }
    }
    //return removed;
  }

  /**
   * @see #getTypeToValues
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
    //logger.info("restoreListBoxState = '" + selectionState + "'");
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
    SelectionState selectionState = getSelectionState(getHistoryToken());
    // keep the download link info in sync with the selection
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
//        logger.info("restoreListBoxState t->sel    " + selectionState + " typeOrder " + typeOrder);
        Map<String, String> newTypeToSelection = getNewTypeToSelection(selectionState, typeOrder);
        if (typeToSelection.equals(newTypeToSelection) && typeOrderContainer.iterator().hasNext()) {
//          logger.info("getSectionWidgetContainer : restoreListBoxState state already consistent with " + newTypeToSelection);
        } else {
          int userListID = getUserListID(newTypeToSelection);
          if (userListID != -1) {
            int projid = selectionState.getProject();
            int currentProject = getCurrentProject();
            boolean isForSameProject = projid == -1 || projid == currentProject;
            userListID = isForSameProject ? userListID : -1;

            if (!isForSameProject) {
              logger.warning("getProjectIDForList : list is for project " + projid + " but current is " + currentProject);
              new ModalInfoDialog(LINK_FOR_CONTENT,
                  PLEASE_CHANGE);
            }

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
        return FacetExerciseList.this.getHistoryToken(FacetExerciseList.this.typeToSelection);
      }

      @Override
      public int getNumSelections() {
        return 0;
      }
    };
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
      builder.append(pair.getKey()).append("=").append(pair.getValue()).append(SECTION_SEPARATOR);
    }
    builder.append(getProjectParam());

    // keepSearchItem(builder);

    String s = builder.toString();


//        logger.info("getHistoryToken token '" + s + "'");
    return s;
  }

  private String keepSearchItem() {
    SelectionState selectionState = getSelectionState(getHistoryToken());
    String search = selectionState.getSearch();
    if (!search.isEmpty()) {
      return selectionState.getSearchEntry();
    } else return "";
  }

  @NotNull
  private String getProjectParam() {
    int projectid = controller.getProjectStartupInfo() == null ? -1 : getCurrentProject();
    return SelectionState.SECTION_SEPARATOR + SelectionState.PROJECT + "=" + projectid;
  }


  @Override
  protected void projectChangedTo(int project) {
    controller.reallySetTheProject(project);
  }

  private int getUserListID(Map<String, String> newTypeToSelection) {
    int userListID = -1;
    try {
      userListID = newTypeToSelection.containsKey(LISTS) ? Integer.parseInt(newTypeToSelection.get(LISTS)) : -1;
    } catch (NumberFormatException e) {
      logger.warning("can't parse " + newTypeToSelection.get(LISTS));
    }
    return userListID;
  }


  private int getCurrentProject() {
    return controller.getProjectStartupInfo().getProjectid();
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

  void pushFirstSelection(int exerciseID, String searchIfAny) {
    if (isDrillView()) {
      super.pushFirstSelection(exerciseID, searchIfAny);
    } else {
      updateDownloadLinks();
      askServerForExercise(-1);
    }
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
   *
   */
  protected void askServerForExercise(int itemID) {
    final int currentReq = incrReq();
    askServerForVisibleExercises(itemID, pagingContainer.getVisibleIDs(), currentReq);
  }

  /**
   * @param itemID
   * @param visibleIDs
   * @param currentReq
   * @see #gotVisibleRangeChanged
   */
  private void askServerForVisibleExercises(int itemID, Collection<Integer> visibleIDs, final int currentReq) {
    // logger.info("askServerForExercises ask for single -- " + itemID + " and " + visibleIDs.size());
    if (visibleIDs.isEmpty() && pagingContainer.isEmpty() && finished) {
      //   logger.info("askServerForExercises show empty -- ");
      //  showEmptyExercise();
    } else {
      getExercises(setVisibleForDrill(itemID, visibleIDs), currentReq);
    }
  }

  private Collection<Integer> setVisibleForDrill(int itemID, Collection<Integer> visibleIDs) {
    if (isDrillView() && itemID > 0) {
      visibleIDs = new ArrayList<>();
      visibleIDs.add(itemID);
      // logger.info("askServerForExercises ask for single -- " + itemID);
    }
    return visibleIDs;
  }

  @Override
  public boolean isCurrentReq(int reqID) {
    return reqID == -1 || reqID == getCurrentExerciseReq();
  }

  private int getCurrentExerciseReq() {
    return freqid;
  }

  /**
   * @see #askServerForExercise(int)
   * @see #makePagingContainer
   * @return
   */
  private int incrReq() {
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

  private void hidePrevNext() {
    hidePrevNextWidgets();
    progressBar.setVisible(false);
  }

  /**
   * @see #gotFullExercises
   */
  private void hidePrevNextWidgets() {
    pageSizeContainer.setVisible(false);
    sortBox.setVisible(false);
    pagerAndSortRow.setVisible(false);
  }

  private void showPrevNext() {
    pageSizeContainer.setVisible(true);
    sortBox.setVisible(true);
    progressBar.setVisible(true);
    pagerAndSortRow.setVisible(true);
  }

  /**
   * @param visibleIDs
   * @param currentReq
   * @see #askServerForExercises(int, Collection, int)
   */
  private void getExercises(final Collection<Integer> visibleIDs, final int currentReq) {
    if (!isDrillView()) { // no blink
      Widget widget = innerContainer.getWidget();
      if (widget != null) {
        widget.getElement().getStyle().setOpacity(0.2);
      }
//      setExerciseContainer(new DivWidget());
    }
    //    logger.warning("getExercises NO timer " + diff);
    // final int currentReq = incrReq();
//    Scheduler.get().scheduleDeferred((Command) () -> {
    //logger.info("showExercises for check " + reqID);
    if (isCurrentReq(currentReq)) {
      reallyGetExercises(visibleIDs, currentReq);
    } else {
      logger.info("getExercises skip stale req " + currentReq + " vs current " + getCurrentExerciseReq());
    }
    //  });
  }

  /**
   * @param visibleIDs
   * @param currentReq
   * @see
   */
  private void reallyGetExercises(Collection<Integer> visibleIDs, final int currentReq) {
    //  logger.info("reallyGetExercises " + visibleIDs.size() + " visible ids : " + visibleIDs);
    long then = System.currentTimeMillis();
    List<Integer> requested = new ArrayList<>();
    List<CommonExercise> alreadyFetched = new ArrayList<>();
    for (Integer id : visibleIDs) {
      CommonExercise cachedExercise = getCachedExercise(id);
      if (cachedExercise == null) {
        requested.add(id);
      } else {
        alreadyFetched.add(cachedExercise);
      }
    }

    if (requested.isEmpty()) {
//      logger.info("reallyGetExercises no req for " + alreadyFetched.size());
      gotFullExercises(currentReq, alreadyFetched);
    } else {
/*      logger.info("reallyGetExercises make" +
          "\n\treq for " + requested +
          "\n\tvisible " + visibleIDs);*/

      if (controller.getUser() > 0) {
        getFullExercises(visibleIDs, currentReq, requested, alreadyFetched);
      } else {
        logger.warning("not asking for " + requested);
      }
    }
  }

  private void getFullExercises(Collection<Integer> visibleIDs, int currentReq, List<Integer> requested, List<CommonExercise> alreadyFetched) {
    long then = System.currentTimeMillis();
    service.getFullExercises(currentReq, requested,
        new AsyncCallback<ExerciseListWrapper<CommonExercise>>() {
          @Override
          public void onFailure(Throwable caught) {
            logger.warning("getExercises got exception : " + caught);
            logger.warning("getExercises got " + getExceptionAsString(caught));
            dealWithRPCError(caught);
            hidePrevNext();
          }

          @Override
          public void onSuccess(final ExerciseListWrapper<CommonExercise> result) {
            //                    logger.info("reallyGetExercises onSuccess " + visibleIDs.size() + " visible ids : " + visibleIDs);
            if (result.getExercises() != null) {
              long now = System.currentTimeMillis();
              int size = result.getExercises().isEmpty() ? 0 : result.getExercises().size();
              if (now - then > 1000) {
                logger.info("getFullExercisesSuccess took " + (now - then) + " to get " + size + " exercises");
              }

              getFullExercisesSuccess(result, alreadyFetched, visibleIDs);
            }
          }
        });
  }

  /**
   * @param result
   * @param alreadyFetched
   * @param visibleIDs
   * @see #reallyGetExercises
   */
  private void getFullExercisesSuccess(ExerciseListWrapper<CommonExercise> result,
                                       List<CommonExercise> alreadyFetched,
                                       Collection<Integer> visibleIDs) {
    // long now = System.currentTimeMillis();
    int size = result.getExercises().isEmpty() ? 0 : result.getExercises().size();
    //  logger.info("getFullExercisesSuccess got " + size + " exercises vs " + visibleIDs.size() + " visible.");
    int reqID = result.getReqID();
    List<CommonExercise> toShow = new ArrayList<>();

    {
      Map<Integer, CommonExercise> idToEx = setScoreHistory(result);
      for (CommonExercise ex : alreadyFetched) {
        idToEx.put(ex.getID(), ex);
      }
      result.getExercises().forEach(this::addExerciseToCached);
      // logger.info("\tgetFullExercisesSuccess for each visible : " + visibleIDs );

      for (int id : visibleIDs) {
        CommonExercise e = idToEx.get(id);
        if (e == null) {
          logger.warning("\n\ngetFullExercisesSuccess : huh? can't find exercise for visible id " + id + " in " + idToEx.keySet());
        } else {
          //   logger.info("getFullExercisesSuccess : show id " + id + " = " + e.getID() + " : " + e.getEnglish());
          toShow.add(e);
        }
      }
    }

    if (isCurrentReq(reqID)) {
      gotFullExercises(reqID, toShow);
    } else {
      if (DEBUG_STALE)
        logger.info("getFullExercisesSuccess : ignoring req " + reqID + " vs current " + getCurrentExerciseReq());
    }
  }

  private void setExerciseContainer(DivWidget w) {
    innerContainer.setWidget(w);  // immediate feedback that something is happening...
    //   w.getElement().getStyle().setOpacity(1);
  }

  private CommonExercise getCachedExercise(Integer id) {
    return fetched.get(id);
  }


  @NotNull
  private Map<Integer, CommonExercise> setScoreHistory(ExerciseListWrapper<CommonExercise> result) {
    Map<Integer, CommonExercise> idToEx = new HashMap<>();
    Map<Integer, List<CorrectAndScore>> scoreHistoryPerExercise = result.getScoreHistoryPerExercise();
    for (CommonExercise ex : result.getExercises()) {
      //   logger.info("setScoreHistory " + ex.getID() +  " " + ex);
      int id = ex.getID();
      idToEx.put(id, ex);
      List<CorrectAndScore> correctAndScores = scoreHistoryPerExercise.get(id);

      // make sure we make a real exercise list here even if it's empty since we'll want to add to it later
      List<CorrectAndScore> scoreTotal = correctAndScores == null ? new ArrayList<>() : correctAndScores;
      //  logger.info("attach score history " + scoreTotal.size() + " to exercise "+ id);
      ex.getMutable().setScores(scoreTotal);
    }
    //logger.info("setScoreHistory now " + idToEx.size());

    return idToEx;
  }

  private void gotFullExercises(final int reqID, List<CommonExercise> toShow) {
    if (isCurrentReq(reqID)) {
      if (toShow.isEmpty()) {
        hidePrevNext();
      } else {
        if (isDrillView()) { // hack for avp
          hidePrevNextWidgets();
          showExercises(toShow, reqID);
          progressBar.setVisible(false);
        } else {
          //      logger.info("gotFullExercises : showing " + toShow.size() + " exercises");
          showExercises(toShow, reqID);
        }
      }
    } else {
      if (DEBUG_STALE) {
        logger.info("gotFullExercises skip stale req " + reqID + " vs current " + getCurrentExerciseReq());
      }
    }
  }

  private final Set<Integer> exercisesWithScores = new HashSet<>();
  private final Map<Integer, CommonExercise> fetched = new HashMap<>();

  /**
   * @param result
   * @param reqID
   * @see #gotFullExercises
   */
  private void showExercises(final Collection<CommonExercise> result, final int reqID) {
    if (isDrillView()) { // drill/avp/flashcard
      showDrill(result);
    } else {
      if (isStale(reqID)) {
        if (DEBUG_STALE)
          logger.info("showExercises Skip stale req " + reqID + " vs current " + getCurrentExerciseReq());
      } else {
        Scheduler.get().scheduleDeferred((Command) () -> {
          //logger.info("showExercises for check " + reqID);
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
        });

  /*      Scheduler.get().scheduleDeferred((Command) () -> {
          if (isCurrentReq(reqID)) {
            //logger.info("showExercises for progress current " + reqID);
            setProgressBarScore(getInOrder(), reqID);
          } else {
            if (DEBUG_STALE)  logger.info("showExercises for progress stale " + reqID);
          }
        });*/
      }
    }
  }

  private boolean isDrillView() {
    return isDrill;
  }

  /**
   * @param result
   * @param reqID
   * @see #showExercises(Collection, int)
   */
  private void reallyShowExercises(Collection<CommonExercise> result, int reqID) {
    //logger.info("reallyShowExercises req " + reqID + " vs current " + getCurrentExerciseReq());
    DivWidget exerciseContainer = new DivWidget();
    long then = System.currentTimeMillis();
    List<RefAudioGetter> getters = makeExercisePanels(result, exerciseContainer, reqID);
    long now = System.currentTimeMillis();

    logger.info("reallyShowExercises made " + getters.size() + " panels in " + (now - then) + " millis");

    if (!getters.isEmpty()) {
      getRefAudio(getters.iterator());
    }

    setExerciseContainer(exerciseContainer);
    showPrevNext();

//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("here for " + reqID));
//    logger.info("logException stack:\n" + exceptionAsString);
    // new ListSorting<>(this).sortLater(sortBoxReally, controller.getLanguage());
  }

  /**
   * @param result
   * @param exerciseContainer
   * @param reqID
   * @return
   */
  private List<RefAudioGetter> makeExercisePanels(Collection<CommonExercise> result,
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
    for (CommonExercise exercise : result) {
      if (isStale(reqID)) {
        logger.info("makeExercisePanels stop stale req " + reqID + " vs current " + getCurrentExerciseReq());
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

/*
  long now = System.currentTimeMillis();
logger.info("makeExercisePanels took " + (now - then) + " req " + reqID + " vs current " +
        getCurrentExerciseReq() +
        " returning " + getters.size() + " : first = " + (result.isEmpty() ? "" : "" + result.iterator().next().getID()));*/

    return getters;
  }

  private void addExerciseToCached(CommonExercise exercise) {
    if (!fetched.containsKey(exercise.getID())) {
      fetched.put(exercise.getID(), exercise);
    }
  }

  private void showDrill(Collection<CommonExercise> result) {
    CommonExercise next = result.iterator().next();
    markCurrentExercise(next.getID());
    addExerciseWidget(next);
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
            logger.info("\tgetRefAudio panel complete...");
            final int reqid = next.getReq();
            if (isCurrentReq(reqid)) {
              Scheduler.get().scheduleDeferred(() -> {
                if (isCurrentReq(reqid)) {
                  getRefAudio(iterator);
                } else {
                  if (DEBUG_STALE) logger.info("getRefAudio : 2 skip stale req (" +reqid+
                      ") for panel vs " + getCurrentExerciseReq());
                }
              });
            }
          } else {
            logger.info("\tgetRefAudio all panels complete...");
          }
        });
      }
    }
  }

  /**
   * @param result
   * @see #showExercises
   */
  private void setProgressBarScore(Collection<CommonShell> result, final int reqid) {
    exercisesWithScores.clear();
    long then = System.currentTimeMillis();
    //logger.info("setProgressBarScore checking " + result.size());
    for (CommonShell exercise : result) {
      if (exercise.hasScore()) {
        // logger.info("\tsetProgressBarScore got " + exercise.getRawScore());
        exercisesWithScores.add(exercise.getID());
      }
      // if (!isCurrentReq(reqid)) break;
    }
    //  long now = System.currentTimeMillis();

/*    if (now - then > 100) {
      logger.info("setProgressBarScore took " + (now - then) + " millis");
    }*/

    if (isCurrentReq(reqid)) {
      showScore(exercisesWithScores.size(), result.size());
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
      exercisesWithScores.add(id);
    } else {
      logger.info("skipping low score for " + id);
    }
    showScore(exercisesWithScores.size(), pagingContainer.getSize());
  }

  private void showScore(int num, int denom) {
    double score = (float) num / (float) denom;
    double percent = 100 * score;
    progressBar.setPercent(num == 0 ? 10 : percent);
    boolean allDone = num == denom;

    String text =
        num == 0 ? NONE_PRACTICED_YET :
            allDone ? ALL_PRACTICED : (num + " practiced.");

    progressBar.setText(text);
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
