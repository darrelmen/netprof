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

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.base.*;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
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
import mitll.langtest.client.scoring.AudioSelectedEvent;
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
  public static final String LISTS = "Lists";
  private final Logger logger = Logger.getLogger("FacetExerciseList");

  public static final String PAGE_SIZE_HEADER = "View";

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

    LangTest.EVENT_BUS.addHandler(ListChangedEvent.TYPE, authenticationEvent -> {
      gotListChanged();
    });
  }

  private Widget getProgressBarContainer(ProgressBar progressBar) {
/*      HTML label = new HTML("Score");
      label.addStyleName("topFiveMargin");
      label.addStyleName("leftTenMargin");
      label.addStyleName("floatLeft");*/
    //   Panel afterPlayWidget = new DivWidget();
    //    afterPlayWidget.add(label);
    // afterPlayWidget.add(progressBar);

    //  progressBar.setWidth(PROGRESS_BAR_WIDTH + "px");
    // progressBar.addStyleName("floatLeft");
    //   progressBar.getElement().getStyle().setMarginTop(15, Style.Unit.PX);

    Style style = progressBar.getElement().getStyle();
    style.setMarginTop(5, Style.Unit.PX);
    style.setMarginLeft(5, Style.Unit.PX);
    //style.setMarginBottom(0, Style.Unit.PX);

    //afterPlayWidget.addStyleName("floatLeft");
    //afterPlayWidget.setVisible(false);
    progressBar.setVisible(false);

    return progressBar;
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

  protected Panel tableWithPager;

  protected void addTableWithPager(SimplePagingContainer<CommonShell> pagingContainer) {
    tableWithPager = pagingContainer.getTableWithPager(listOptions);
    //   return tableWithPager;
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
            //  logger.info("makePagingContainer : gotRangeChanged for " + newRange);
            askServerForExercises(-1, getIdsForRange(newRange));
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
   * @param userID
   * @see HistoryExerciseList#noSectionsGetExercises
   */
  public boolean getExercises(final long userID) {
    addWidgets();
    return false;
  }


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
   * TODO : for now only single selection
   */
  private Map<String, String> typeToSelection = new HashMap<>();
  private final Map<String, Boolean> typeToShowAll = new HashMap<>();
  private Set<String> rootNodesInOrder = new HashSet<>();

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
   * @see #getTypeToValue
   */
  private void addFacetsForReal(Map<String, Set<MatchInfo>> typeToValues, Panel nav) {

    logger.info("addFacetsForReal" +//\n\tfor " +
        //" nodes" +
        "\n\t# root nodes = " + rootNodesInOrder.size() +
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
    if (liForDimensionForList != null)
      populateListChoices(liForDimensionForList);
  }

  protected ExerciseListRequest getExerciseListRequest(Map<String, Collection<String>> typeToSection, String prefix,
                                                       boolean onlyWithAudioAnno, boolean onlyUnrecorded,
                                                       boolean onlyDefaultUser, boolean onlyUninspected) {
    ExerciseListRequest exerciseListRequest = getRequest(prefix)
        .setTypeToSelection(typeToSection)
        .setOnlyWithAudioAnno(onlyWithAudioAnno)
        .setOnlyUnrecordedByMe(onlyUnrecorded)
        .setOnlyDefaultAudio(onlyDefaultUser)
        .setOnlyUninspected(onlyUninspected);

    //logger.info("Type->sel " + typeToSection);
    //logger.info("nameToID " + nameToID);

    if (typeToSection.containsKey(LISTS)) {
      Collection<String> strings = typeToSection.get(LISTS);

      String next = strings.iterator().next();
      Integer integer = nameToID.get(next);
      if (integer != null) {
        exerciseListRequest.setUserListID(integer);
        logger.info("userlist = " + integer);
      }
    }
    return exerciseListRequest;
  }

  private Map<String, Integer> nameToID = new HashMap<>();

  private void populateListChoices(ListItem liForDimensionForType
  ) {
    ListServiceAsync listService = controller.getListService();
    listService.getListsForUser(true, false, new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        Map<String, Set<MatchInfo>> typeToValues = new HashMap<>();
        Set<MatchInfo> value = new HashSet<>();
        typeToValues.put(LISTS, value);
        for (UserList<?> list : result) {
          value.add(new MatchInfo(list.getName(), list.getNumItems()));
          nameToID.put(list.getName(), list.getID());
        }
        Widget first = liForDimensionForType.getWidget(0);
        liForDimensionForType.clear();
        liForDimensionForType.add(first);
        liForDimensionForType.add(addChoices(typeToValues, LISTS));
      }
    });
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
   * @see #addFacetsForReal(Map, Panel)
   */
  private Panel addChoices(Map<String, Set<MatchInfo>> typeToValues, String type) {
    Panel choices = new UnorderedList(); // ul
    String selectionForType = typeToSelection.get(type);
    if (selectionForType != null) {
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
        choices.add(getSelectedAnchor(type, selectionForType));
      }
    } else {
      Set<MatchInfo> keys = typeToValues.get(type);
      if (keys != null) {
        addChoicesForType(typeToValues, type, choices, keys);
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
      addLIChoice(choices, getAnchor(type, key));

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

  private Widget getAnchor(String type, MatchInfo key) {
    Panel span = getSpan();

    addMatchInfoTooltip(type, key, span);
    span.addStyleName(MENU_ITEM);
    Anchor anchor = getAnchor(key.getValue());
    anchor.addClickHandler(getChoiceHandler(type, key.getValue()));
    anchor.addStyleName("choice");
    span.add(anchor);

    String countLabel = "" + key.getCount();

    Anchor qty = //getSpan();
        getAnchor(countLabel);
    qty.addClickHandler(getChoiceHandler(type, key.getValue()));

    qty.addStyleName("qty");
//    qty.getElement().setInnerHTML(countLabel);

    span.add(qty);

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

  private void addRemoveClickHandler(final String type, Anchor typeSection) {
    typeSection.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        removeSelection(type);
        getTypeToValue(typeToSelection);
      }
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

  @NotNull
  private ClickHandler getChoiceHandler(final String type, final String key) {
    return event -> {
      Map<String, String> candidate = new HashMap<>(typeToSelection);
      candidate.put(type, key);
      logger.info("getChoiceHandler " + type + "=" + key);
      getTypeToValue(candidate);
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
   * @see #addRemoveClickHandler(String, Anchor)
   * @see #getChoiceHandler(String, String)
   * @see #getSectionWidgetContainer()
   */
  private void getTypeToValue(Map<String, String> typeToSelection) {
    List<Pair> pairs = getPairs(typeToSelection);
    logger.info("getTypeToValue request " + pairs);
    controller.getExerciseService().getTypeToValues(new FilterRequest(reqid++, pairs),
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
            logger.info("getTypeToValues for " + pairs + " got " + result.size());
            /*boolean b =*/
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

    Collection<String> typesWithSelections = new ArrayList<String>(typeToSelection.keySet());

    for (String selectedType : typesWithSelections) {
      boolean clearSelection = !typesToInclude.contains(selectedType);
      if (clearSelection) {
        if (removeSelection(selectedType, typeToSelection)) {
          removed = true;
        } else {
        }
      } else {
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
    logger.info("restoreListBoxState " + selectionState);
//    List<String> typeOrder = controller.getProjectStartupInfo().getTypeOrder();
//    List<String> added = new ArrayList<>(typeOrder);
//    added.add(LISTS);
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
      logger.info("loadFirstExercise : current exercises is empty");
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
        logger.info("restoreListBoxState t->sel    " + selectionState);
        logger.info("restoreListBoxState typeOrder " + typeOrder);
        typeOrder = new ArrayList<>(typeOrder);
        typeOrder.add(LISTS);

        Map<String, String> newTypeToSelection = new HashMap<>();
        for (String type : typeOrder) {
          Collection<String> selections = selectionState.getTypeToSection().get(type);
          if (selections != null && !selections.isEmpty()) {
            newTypeToSelection.put(type, selections.iterator().next());
          }
        }
        getTypeToValue(newTypeToSelection);
      }

      /**
       * @see HistoryExerciseList#getHistoryTokenFromUIState
       * @return
       */
      @Override
      public String getHistoryToken() {
        StringBuilder builder = new StringBuilder();
        //logger.info("getHistoryToken t->sel " + typeToSelection);

        for (Map.Entry<String, String> pair : typeToSelection.entrySet()) {
          builder.append(pair.getKey()).append("=").append(pair.getValue()).append(SECTION_SEPARATOR);
        }

  /*
        if (id != -1) builder.append(SECTION_SEPARATOR).append("item=").append(id);
        */

        String s = builder.toString();
        // logger.info("getHistoryToken token '" + s + "'");

        return s;
      }

      @Override
      public int getNumSelections() {
        return 0;
      }
    };
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
    if (visibleIDs.isEmpty() && pagingContainer.isEmpty() && finished) {
      // logger.info("askServerForExercises show empty -- ");
      //  showEmptyExercise();
    } else {
      if (numToShow == 1 && itemID > 0) {
        visibleIDs = new ArrayList<>();
        visibleIDs.add(itemID);
        logger.info("askServerForExercises ask for single -- " + itemID);
      }

      ensureAudio(visibleIDs);
    }
  }

  private boolean isStaleReq(ExerciseListWrapper<?> req) {
    return !isCurrentReq(req);
  }

  private boolean isCurrentReq(ExerciseListWrapper<?> req) {
    return req.getReqID() == freqid - 1;
  }

  private void ensureAudio(Collection<Integer> visibleIDs) {
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
  }

  private void hidePrevNext() {
    hidePrevNextWidgets();
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
   * @see #ensureAudio
   */
  private void getExercises(Collection<Integer> visibleIDs) {
    long then = System.currentTimeMillis();
    //  logger.info("getExercises asking for " + visibleIDs.size());
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
        // int widgetCount = innerContainer.getWidgetCount();
//        if (getCurrentExerciseID() == result.getExercises().iterator().next().getID()) {// && widgetCount == 1) {
//          logger.info("skip current " + getCurrentExerciseID());
//        } else {
        showExercises(result.getExercises(), result);
        progressBar.setVisible(false);
        //    }
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
    if (numToShow == 1 /*&& result.size() == 1*/) {
      CommonExercise next = result.iterator().next();
      addExerciseWidget(next, wrapper);
      markCurrentExercise(next.getID());
    } else {
      DivWidget exerciseContainer = new DivWidget();
//      Panel scrollPanel = new ScrollPanel(exerciseContainer);
//
//      DockLayoutPanel layoutPanel = new DockLayoutPanel(Style.Unit.PX);
//      layoutPanel.add(scrollPanel);

      int reqID = wrapper.getReqID();
      for (CommonExercise exercise : result) {
        if (isStaleReq(wrapper)) {
          logger.info("showExercises stop stale req " + reqID + " vs  current " + (freqid - 1));
          break;
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

  // private Tooltip progressTooltip = null;

  private void showScore(int num, int denom) {
    double score = (float) num / (float) denom;

    //  double percent = score / 100d;
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
