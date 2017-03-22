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
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.ListItem;
import com.github.gwtbootstrap.client.ui.base.UnorderedList;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.download.DownloadHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

public abstract class FacetExerciseList extends NPExerciseList<ListSectionWidget> {
  private final Logger logger = Logger.getLogger("FacetExerciseList");

  public static final String ENGLISH_ASC = "English (A-Z)";
  public static final String ENGLISH_DSC = "English (Z-A)";
  public static final String LENGTH_SHORT_TO_LONG = "Length : short to long";
  public static final String LENGTH_LONG_TO_SHORT = "Length : long to short";
  public static final String SCORE_LOW_TO_HIGH = "Score : low to high";
  public static final String SCORE_DSC = "Score : high to low";

  public static final int MAX_TO_SHOW = 4;
  private static final int TOTAL = 32;
  private static final String SHOW_LESS = "<i>View fewer</i>";
  private static final String SHOW_MORE = "<i>View all</i>";
  public static final String ANY = "Any";
  public static final String MENU_ITEM = "menuItem";

  private static final int CLASSROOM_VERTICAL_EXTRA = 270;

  private List<String> typeOrder;
  private final Panel sectionPanel;
  private final DownloadHelper downloadHelper;
  DivWidget listHeader;

  /**
   * @param secondRow             add the section panel to this row
   * @param currentExerciseVPanel
   * @param controller
   * @param listOptions
   * @param listHeader
   */
  public FacetExerciseList(Panel secondRow,
                           Panel currentExerciseVPanel,
                           ExerciseController controller,
                           ListOptions listOptions, DivWidget listHeader) {
    super(currentExerciseVPanel, controller, listOptions.setShowTypeAhead(true));

    sectionPanel = new DivWidget();
    sectionPanel.getElement().setId("sectionPanel_" + getInstance());
    sectionPanel.addStyleName("rightFiveMargin");
    secondRow.add(sectionPanel);
    setUnaccountedForVertical(CLASSROOM_VERTICAL_EXTRA);
    downloadHelper = new DownloadHelper(this);

    this.listHeader = listHeader;
    DivWidget w = new DivWidget();
    w.addStyleName("floatRight");
    listHeader.add(w);
    w.add(new HTML("Sort by"));
    getSortBox(controller, w);
    locale = controller.getProjectStartupInfo().getLocale();
    logger.info("for " + controller.getLanguage() + " " + locale);
    //   w1.add(new )
    //  logger.info("made ex list....");
  }

  private String locale;

  private void getSortBox(ExerciseController controller, DivWidget w) {
    ListBox w1 = new ListBox();
    w.add(w1);
    String language = controller.getLanguage();


    w1.addItem(ENGLISH_ASC);
    w1.addItem(ENGLISH_DSC);
    String langASC = language + " ascending";
    w1.addItem(langASC);
    String langDSC = language + " descending";
    w1.addItem(langDSC);
    w1.addItem(LENGTH_SHORT_TO_LONG);
    w1.addItem(LENGTH_LONG_TO_SHORT);
    w1.addItem(SCORE_LOW_TO_HIGH);
    w1.addItem(SCORE_DSC);
    w1.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        String selectedValue = w1.getSelectedValue();
        if (selectedValue.equals(LENGTH_SHORT_TO_LONG)) {
          sortBy((o1, o2) -> {
            return compareShells(o1, o2, compPhones(o1, o2));
          });
        } else if (selectedValue.equals(LENGTH_LONG_TO_SHORT)) {
          sortBy((o1, o2) -> {
            int i = -1 * compPhones(o1, o2);
            return compareShells(o1, o2, i);
          });
        } else if (selectedValue.equals(ENGLISH_ASC)) {
          sortBy((o1, o2) -> compEnglish(o1, o2));
        } else if (selectedValue.equals(ENGLISH_DSC)) {
          sortBy((o1, o2) -> -1 * compEnglish(o1, o2));
        } else if (selectedValue.equals(langASC)) {
          sortBy((o1, o2) -> compForeign(o1, o2));
        } else if (selectedValue.equals(langDSC)) {
          sortBy((o1, o2) -> -1 * compForeign(o1, o2));
        } else if (selectedValue.equals(SCORE_LOW_TO_HIGH)) {
          sortBy((o1, o2) -> {
            int i = Float.valueOf(o1.getScore()).compareTo(o2.getScore());
            return compareShells(o1, o2, i);
          });
        } else if (selectedValue.equals(SCORE_DSC)) {
          sortBy((o1, o2) -> {
            int i = -1 * Float.valueOf(o1.getScore()).compareTo(o2.getScore());
            return compareShells(o1, o2, i);
          });
        }
      }
    });
  }

  private int compPhones(CommonShell o1, CommonShell o2) {
    return Integer.valueOf(o1.getNumPhones()).compareTo(o2.getNumPhones());
  }
/*
  private String getLocale(String lang) {
    switch (lang) {
      case "Pashto":
        return "ps";
      case "Spanish":
        return "es";
      default:
        return "en";
    }
  }*/

  private int compareShells(CommonShell o1, CommonShell o2, int i) {
    if (i == 0) i = compForeign(o1, o2);
    if (i == 0) i = compEnglish(o1, o2);
    return i;
  }

  public native int compare(String source, String target); /*-{
      return source.localeCompare(target);
  }-*/

  public native int compareWithLocale(String source, String target, String locale); /*-{
      return source.localeCompare(target, locale);
  }-*/


  public static native int compareAgain(String source, String target) /*-{
      return source.localeCompare(target);
  }-*/;

  public static native int compareAgainLocale(String source, String target, String locale) /*-{
      return source.localeCompare(target);
  }-*/;

  private int compForeign(CommonShell o1, CommonShell o2) {
    //   return o1.getForeignLanguage().compareTo(o2.getForeignLanguage());
    //  return compareWithLocale(o1.getForeignLanguage(), o2.getForeignLanguage(), locale);
   // return compareAgain(o1.getForeignLanguage(), o2.getForeignLanguage());
    return compareAgainLocale(o1.getForeignLanguage(), o2.getForeignLanguage(),locale);
  }

  private int compEnglish(CommonShell o1, CommonShell o2) {
    return o1.getEnglish().trim().toLowerCase().compareTo(o2.getEnglish().toLowerCase().trim());
  }

  /**
   * @see PagingExerciseList#PagingExerciseList
   */
  @Override
  protected void addComponents() {
    addTableWithPager(makePagingContainer());
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
    sectionPanel.add(typeOrderContainer = getWidgetsForTypes());
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
//    container.getElement().getStyle().setPaddingLeft(2, Style.Unit.PX);
//    container.getElement().getStyle().setPaddingRight(2, Style.Unit.PX);
    getTypeOrder(container);
    return container;
  }

  private void getTypeOrder(final UnorderedList container) {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    typeOrder = projectStartupInfo.getTypeOrder();
    //  logger.info("getTypeOrder type order is " + typeOrder);
    addFacets(container, projectStartupInfo.getRootNodes(), projectStartupInfo.getTypeToDistinct());
  }

  private Panel firstTypeRow;

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
   *
   * @param nav
   * @param types
   * @param typeToDistinct
   * @paramx rootNodes
   * @see #getTypeOrder
   */
  private void addFacets(
      final UnorderedList nav,
      Collection<String> types,
      Map<String, Set<MatchInfo>> typeToDistinct) {
    logger.info("addChoiceRow for user = " + controller.getUser() + " got rootNodesInOrder " +
        types);// + " num root nodes " + rootNodes.size());

    if (types.isEmpty()) {
      logger.warning("addChoiceRow : huh? rootNodesInOrder is empty?");
      return;
    }

    addFacetsForReal(typeToDistinct, nav);
    pushFirstListBoxSelection();
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

/*
  private static class TypeInfo {
    private String selection;
    private boolean showAll;

    public TypeInfo(String selection,boolean showAll) {
      this.selection = selection;
      this.showAll = showAll;
    }

    public String getSelection() {
      return selection;
    }

    public boolean isShowAll() {
      return showAll;
    }

    public String toString() { return selection + (showAll ? "show all":""); }

    public void setShowAll(boolean showAll) {
      this.showAll = showAll;
    }
  }*/

  /**
   * ul
   * li - each dimension
   * span - header
   * ul - choices in each dimension
   * li - choice - with span (qty)
   *
   * @paramx rootNodes
   * @paramx rootNodesInOrder
   * @see #addFacets
   */
  private void addFacetsForReal(Map<String, Set<MatchInfo>> typeToValues, Panel nav) {
    logger.info("addFacetsForReal\n\tfor " +
        " nodes" +
        "\n\tand " + rootNodesInOrder.size() +
        " type->distinct " + typeToValues.keySet() + "\n\ttype->sel " + typeToSelection);
    Collection<String> rootNodes = controller.getProjectStartupInfo().getRootNodes();

    this.rootNodesInOrder = new HashSet<>(controller.getProjectStartupInfo().getTypeOrder());
    this.rootNodesInOrder.retainAll(rootNodes);

    // nav -
    //   ul
    UnorderedList allTypesContainer = new UnorderedList(); //ul
    nav.clear();
    nav.add(allTypesContainer);

    for (String type : rootNodesInOrder) {
      // nav
      //  ul
      //   li - dimension
      boolean hasSelection = typeToSelection.containsKey(type);
      ListItem liForDimensionForType = getLIDimension(hasSelection);
      allTypesContainer.add(liForDimensionForType);
      Panel typeHeader = hasSelection ? getTypeHeader(type) : getSimpleHeader(type);
      liForDimensionForType.add(typeHeader);

      // nav
      //  ul
      //   li
      //    ul
      //
      liForDimensionForType.add(addChoices(typeToValues, type));
    }
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
    boolean hasMore = keys.size() > toShow;
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
    anchor.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        typeToShowAll.put(type, false);
        addFacetsForReal(typeToValues, typeOrderContainer);
      }
    });
    return anchor;
  }

  @NotNull
  private Anchor getShowMoreAnchor(final Map<String, Set<MatchInfo>> typeToValues, final String type) {
    Anchor anchor = new Anchor();

    anchor.setHTML(SHOW_MORE);
    anchor.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        typeToShowAll.put(type, true);
        addFacetsForReal(typeToValues, typeOrderContainer);
      }
    });
    return anchor;
  }

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
    anchor.addClickHandler(getHandler(type, key.getValue()));
    anchor.addStyleName("choice");
    span.add(anchor);

    FlowPanel qty = getSpan();
    qty.addStyleName("qty");
    qty.getElement().setInnerHTML("" + key.getCount());
    span.add(qty);
    return span;
  }

  private void addMatchInfoTooltip(String type, MatchInfo key, Panel span) {
    String value = key.getValue();
    addTooltip(type, value, span);
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
//        logger.info("ty->sel remove " + typeToSelection);
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
  private ClickHandler getHandler(final String type, final String key) {
    return new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Map<String, String> candidate = new HashMap<>(typeToSelection);
        candidate.put(type, key);
        //      logger.info("getHandler t->sel " + typeToSelection);
        getTypeToValue(candidate);
      }
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
   */
  private void getTypeToValue(Map<String, String> typeToSelection) {
    List<Pair> pairs = new ArrayList<>();

    for (String type : typeOrder) {
      String s = typeToSelection.get(type);
      if (s == null) {
        pairs.add(new Pair(type, ANY));
      } else {
        pairs.add(new Pair(type, s));
      }
    }

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
            logger.info("getTypeToValues for " + pairs + " got " + result);
            boolean b = changeSelection(response.getTypesToInclude(), typeToSelection);

            setTypeToSelection(typeToSelection);

            addFacetsForReal(result, typeOrderContainer);
            gotSelection();
          }
        });
  }

  private boolean changeSelection(Set<String> typesToInclude, Map<String, String> typeToSelection) {
    boolean removed = false;

    Collection<String> typesWithSelections = new ArrayList<String>(typeToSelection.keySet());
    //logger.info(" - found " + typesWithSelections + " selected rootNodesInOrder vs " + typesToInclude);

    for (String selectedType : typesWithSelections) {
      boolean clearSelection = !typesToInclude.contains(selectedType);
      if (clearSelection) {
        if (removeSelection(selectedType, typeToSelection)) {
          //    logger.info("removed selection " + selectedType);
          removed = true;
        } else {
          //  logger.info("no selection " + selectedType);
        }
      } else {
//        logger.info(" - found " + selectedType + " selected type in " + typesToInclude);
      }
    }

    //  logger.info("changeSelection removed --- " + removed);

    return removed;
  }

  /**
   * @seex ButtonBarSectionWidget#getChoice(ButtonGroup, String)
   */
  private void gotSelection() {
    //logger.info("gotSelection --- ");
    pushNewSectionHistoryToken();
  }

  /**
   * @param selectionState
   * @see HistoryExerciseList#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)
   */
  @Override
  protected void restoreListBoxState(SelectionState selectionState) {
    // logger.info("restoreListBoxState " + selectionState);
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
    Map<String, Collection<String>> typeToSection = selectionState.getTypeToSection();
    //  logger.info("showSelectionState : typeOrder " + typeOrder + " selection state " + typeToSection);
    downloadHelper.updateDownloadLinks(selectionState, typeOrder);
  }

  /**
   * @seex #rememberAndLoadFirst(List, CommonExercise, String)
   */
  @Override
  protected void loadFirstExercise(String searchIfAny) {
    // logger.info("loadFirstExercise : ---");
    if (isEmpty()) { // this can only happen if the database doesn't load properly, e.g. it's in use
      logger.info("loadFirstExercise : current exercises is empty?");
      gotEmptyExerciseList();
    } else {
      super.loadFirstExercise(searchIfAny);
    }
  }

  /**
   * @seex HistoryExerciseList.MySetExercisesCallback#onSuccess(mitll.langtest.shared.amas.ExerciseListWrapper)
   */
  @Override
  protected void gotEmptyExerciseList() {
    // showEmptySelection();
  }

  protected FacetContainer getSectionWidgetContainer() {
    return new FacetContainer() {
      @Override
      public void restoreListBoxState(SelectionState selectionState, Collection<String> typeOrder) {
        // logger.info("restoreListBoxState t->sel " + selectionState);

        Map<String, String> newTypeToSelection = new HashMap<>();
        for (String type : typeOrder) {
          Collection<String> selections = selectionState.getTypeToSection().get(type);
          if (selections != null && !selections.isEmpty()) {
            newTypeToSelection.put(type, selections.iterator().next());
          }
        }
        getTypeToValue(newTypeToSelection);
      }

      @Override
      public String getHistoryToken() {
        StringBuilder builder = new StringBuilder();
        //logger.info("getHistoryToken t->sel " + typeToSelection);

        for (Map.Entry<String, String> pair : typeToSelection.entrySet()) {
          builder.append(pair.getKey()).append("=").append(pair.getValue()).append(SECTION_SEPARATOR);
        }

        String s = builder.toString();
        //logger.info("getHistoryToken token " + s);

        return s;
      }

      @Override
      public int getNumSelections() {
        return 0;
      }
    };
  }
}
