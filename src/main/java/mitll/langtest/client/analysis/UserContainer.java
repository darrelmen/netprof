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

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.server.services.AnalysisServiceImpl;
import mitll.langtest.shared.analysis.UserInfo;
import mitll.langtest.shared.custom.IUserListLight;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/20/15.
 */
public class UserContainer extends BasicUserContainer<UserInfo> {
  private final Logger logger = Logger.getLogger("UserContainer");

  public static final int MAX_LENGTH = 11;
  public static final int TABLE_WIDTH = 600;
  public static final String TRYING_TO_GET_STUDENTS = "trying to get students";

  private static final String FILTER_BY1 = "Filter by";

  private static final int LIST_BOX_WIDTH = 150;
  private static final int FIRST_WIDTH = 100;
  private static final int LAST_WIDTH = 110;

  private static final String NO_LIST = "(No List)";
  private static final int FILTER_BY = 19;
  private static final String CURRENT = "Avg";
  private static final int CURRENT_WIDTH = 60;
  /*
    private static final int DIFF_WIDTH = 55;
    private static final int INITIAL_SCORE_WIDTH = 75;
    private static final String DIFF_COL_HEADER = "+/-";
    */
  private static final int MIN_RECORDINGS = AnalysisServiceImpl.MIN_RECORDINGS;

  /**
   *
   */
  private static final String STUDENT = "Student";

  private final ShowTab learnTab;
  private final DivWidget rightSide;
  private final DivWidget overallBottom;
  /**
   *
   */
  private Set<Integer> myStudents;

  private Button add;
  private Button remove;
  private Button mineOnly;

  /**
   * @param controller
   * @param rightSide
   * @see StudentAnalysis#StudentAnalysis
   */
  UserContainer(ExerciseController controller,
                DivWidget rightSide,
                DivWidget overallBottom,
                ShowTab learnTab,
                String selectedUserKey
  ) {
    super(controller, selectedUserKey, STUDENT);
    this.rightSide = rightSide;
    this.learnTab = learnTab;
    // logger.info("overall bottom is " + overallBottom.getElement().getId() + " selected " + selectedUserKey);
    this.overallBottom = overallBottom;
    myStudents = new HashSet<>();
  }

  protected int getMaxLengthId() {
    return MAX_LENGTH;
  }
  protected int getMaxTableWidth() {
    return TABLE_WIDTH;
  }

  @Override
  protected void addTable(Collection<UserInfo> users, DivWidget leftSide) {
    if (showOnlyMine()) {
      controller.getDLIClassService().getStudents(new AsyncCallback<Set<Integer>>() {
        @Override
        public void onFailure(Throwable caught) {
          controller.getMessageHelper().handleNonFatalError(TRYING_TO_GET_STUDENTS, caught);
        }

        @Override
        public void onSuccess(Set<Integer> result) {
          myStudents = result;

          remembered = new ArrayList<>(users);
          List<UserInfo> filtered = new ArrayList<>();
          remembered.forEach(userInfo -> {
            if (myStudents.contains(userInfo.getID())) filtered.add(userInfo);
          });
          getTableWithButtons(filtered, leftSide);
        }
      });
    } else {
      controller.getDLIClassService().getStudents(new AsyncCallback<Set<Integer>>() {
        @Override
        public void onFailure(Throwable caught) {
          controller.getMessageHelper().handleNonFatalError(TRYING_TO_GET_STUDENTS, caught);
        }

        @Override
        public void onSuccess(Set<Integer> result) {
          myStudents = result;
          mineOnly.setEnabled(!myStudents.isEmpty());

//          Panel tableWithPager = getTableWithPager(users);
//          leftSide.add(tableWithPager);
//          ((Panel) tableWithPager.getParent()).add(getButtons());
          getTableWithButtons(users, leftSide);
        }
      });
    }
  }

  private void getTableWithButtons(Collection<UserInfo> filtered, DivWidget leftSide) {
    Panel tableWithPager = getTableWithPager(filtered);
    leftSide.add(tableWithPager);
    ((Panel) tableWithPager.getParent()).add(getButtons());
  }

  @Override
  public Panel getTableWithPager(Collection<UserInfo> users) {
    Panel tableWithPager = super.getTableWithPager(users);
    tableWithPager.getElement().getStyle().setProperty("minHeight", "317px");
    return tableWithPager;
  }

  private boolean showOnlyMine() {
    return controller.getStorage().isTrue("mineOnly");
  }

  /**
   * add/remove should change list in table if filter selected.
   * button state should reflect list the student is on
   *
   * @return
   */
  @NotNull
  private DivWidget getButtons() {
    DivWidget buttons = new DivWidget();
    buttons.addStyleName("floatLeft");
    buttons.addStyleName("inlineFlex");
    buttons.addStyleName("topFiveMargin");
    buttons.add(new HTML("My Student"));
    buttons.add(add = getAddButton());
    buttons.add(remove = getRemoveButton());

    return buttons;
  }


  @NotNull
  private Button getAddButton() {
    final Button add = new Button("Y", IconType.PLUS);
    add.addStyleName("leftFiveMargin");
    add.setSize(ButtonSize.MINI);

    add.addClickHandler(event ->
    {
      add.setEnabled(false);
      int id = getSelected().getID();
      controller.getDLIClassService().add(id, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          controller.handleNonFatalError("adding a student to my students", caught);
        }

        @Override
        public void onSuccess(Void result) {
          myStudents.add(id);
          remove.setEnabled(true);
          table.redraw();
          mineOnly.setEnabled(true);
          // update the list if filter selected
        }
      });
    });
    add.setType(ButtonType.SUCCESS);
    return add;
  }

  private void enableButtons() {
    UserInfo currentSelection = getCurrentSelection();

    boolean onMyList = myStudents.contains(currentSelection.getID());
    add.setEnabled(!onMyList);
    remove.setEnabled(onMyList);
  }

  @NotNull
  private Button getRemoveButton() {
    final Button remove = new Button("N", IconType.MINUS);
    remove.setSize(ButtonSize.MINI);
    remove.addStyleName("leftFiveMargin");
    remove.addClickHandler(event -> {
      remove.setEnabled(false);
      int id = getSelected().getID();
      controller.getDLIClassService().remove(id, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          controller.handleNonFatalError("removing a student from my students",caught);
        }

        @Override
        public void onSuccess(Void result) {
          // update the list if filter selected
          myStudents.remove(id);
          add.setEnabled(true);
          if (showOnlyMine()) filterUsers();
          else table.redraw();

          mineOnly.setEnabled(!myStudents.isEmpty());

          if (myStudents.isEmpty()) {
            //   logger.info("student list is empty!");
            mineOnly.setActive(false);
            showAllUsers();
          }
        }
      });
    });
    remove.setType(ButtonType.SUCCESS);
    return remove;
  }


  /**
   * @return
   * @see MemoryItemContainer#getTable
   */
  @Override
  protected IsWidget getRightOfHeader() {
    DivWidget filterContainer = new DivWidget();

    filterContainer.addStyleName("floatRight");
    filterContainer.getElement().getStyle().setMarginTop(FILTER_BY, Style.Unit.PX);

    HTML w1 = new HTML(FILTER_BY1);

    w1.addStyleName("floatLeft");
    w1.addStyleName("rightFiveMargin");
    w1.addStyleName("topFiveMargin");

    // mimic style of subtext on headers
    w1.getElement().getStyle().setFontSize(14, Style.Unit.PX);
    w1.getElement().getStyle().setColor("#999");

    filterContainer.add(w1);

    filterContainer.add(getListBox());
    filterContainer.addStyleName("leftFiveMargin");
    filterContainer.add(mineOnly = getMyStudents());

    return filterContainer;
  }

  private List<UserInfo> remembered;

  @NotNull
  private Button getMyStudents() {
    Button mineOnly = new Button("Mine Only");
    mineOnly.setToggle(true);
    mineOnly.setSize(ButtonSize.MINI);
    mineOnly.addStyleName("leftFiveMargin");
    mineOnly.addStyleName("topFiveMargin");

    mineOnly.setActive(showOnlyMine());

    mineOnly.addClickHandler(event -> {
      if (mineOnly.isToggled()) {
        showAllUsers();
      } else {
        setStorageMineOnly(true);
        rememberAndFilter();
      }
    });

    return mineOnly;
  }

  private void showAllUsers() {
    setStorageMineOnly(false);
    populateTable(remembered);
  }

  private void setStorageMineOnly(boolean val) {
    controller.getStorage().setBoolean("mineOnly", val);
  }

  private void rememberAndFilter() {
    remembered = new ArrayList<>(getList());
    filterUsers();
  }

  private void filterUsers() {
    List<UserInfo> filtered = new ArrayList<>();

    remembered.forEach(userInfo -> {
      if (myStudents.contains(userInfo.getID())) filtered.add(userInfo);
    });

    populateTable(filtered);
  }

  @NotNull
  private ListBox getListBox() {
    ListBox listBox = new ListBox();

    listBox.setWidth(LIST_BOX_WIDTH + "px");
    listBox.addStyleName("floatLeft");
    listBox.getElement().getStyle().setMarginBottom(2, Style.Unit.PX);

    controller.getListService().getLightListsForUser(true, false, new AsyncCallback<Collection<IUserListLight>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("getting my lists", caught);
      }

      @Override
      public void onSuccess(Collection<IUserListLight> result) {
        useLists(result, listBox);
      }
    });
    return listBox;
  }

  private List<Integer> rememberedLists;
  private int listid = -1;

  private void useLists(Collection<IUserListLight> result, ListBox listBox) {
    this.rememberedLists = new ArrayList<>();
    result.forEach(ul -> rememberedLists.add(ul.getID()));

    // logger.info("There are " + result.size() + " lists");
    listBox.addItem(NO_LIST);
    result.forEach(ul -> listBox.addItem(ul.getName()));
    listBox.addChangeHandler(event -> {
      int selectedIndex = listBox.getSelectedIndex();

      if (selectedIndex == 0) {
        listid = -1;
      } else {
        Integer listID = rememberedLists.get(selectedIndex - 1);
        //    logger.info("selected index " + selectedIndex + " " + listID);
        listid = listID;
      }

      gotClickOnItem(getSelected());
    });
  }

  /**
   * @see SimplePagingContainer#configureTable
   */
  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    List<UserInfo> list = getList();
    addItemID(list, 20);
    addFirstName(list);
    addLastName(list);
    addDateCol(list);

    addNumber(list);
    addCurrent(list);
    addMine(list);
    //addFinalScore();
    //addDate();

    table.getColumnSortList().push(dateCol);
    table.setWidth("100%", true);

    addTooltip();
  }

  private void addFirstName(List<UserInfo> list) {
    Column<UserInfo, SafeHtml> userCol = new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml(shell.getFirst());
      }
    };
    userCol.setSortable(true);
    table.setColumnWidth(userCol, FIRST_WIDTH + "px");
    addColumn(userCol, new TextHeader("First"));
    table.addColumnSortHandler(getFirstSorter(userCol, list));
  }

  private ColumnSortEvent.ListHandler<UserInfo> getFirstSorter(Column<UserInfo, SafeHtml> englishCol,
                                                               List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, this::getFirstCompare);
    return columnSortHandler;
  }

  private void addLastName(List<UserInfo> list) {
    Column<UserInfo, SafeHtml> userCol = new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml(shell.getLast());
      }
    };
    userCol.setSortable(true);
    table.setColumnWidth(userCol, LAST_WIDTH + "px");
    addColumn(userCol, new TextHeader("Last"));
    table.addColumnSortHandler(getLastSorter(userCol, list));
  }

  private ColumnSortEvent.ListHandler<UserInfo> getLastSorter(Column<UserInfo, SafeHtml> englishCol,
                                                              List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, this::getLastCompare);
    return columnSortHandler;
  }

/*  private void addDate() {
    Column<UserInfo, SafeHtml> diff = getDiff();
    diff.setSortable(true);
    addColumn(diff, new TextHeader(DIFF_COL_HEADER));
    table.addColumnSortHandler(getDiffSorter(diff, getList()));
    table.setColumnWidth(diff, DIFF_WIDTH + "px");
  }*/

  private void addCurrent(List<UserInfo> list) {
    Column<UserInfo, SafeHtml> current = getCurrent();
    current.setSortable(true);
    addColumn(current, new TextHeader(CURRENT));
    table.setColumnWidth(current, CURRENT_WIDTH + "px");

    table.addColumnSortHandler(getCurrentSorter(current, list));
  }

  private void addMine(List<UserInfo> list) {
    Column<UserInfo, SafeHtml> current = getMineCol();
    current.setSortable(true);
    addColumn(current, new TextHeader("Mine"));
    table.setColumnWidth(current, 65 + "px");

    table.addColumnSortHandler(getMineSorter(current, list));
  }

  private void addNumber(List<UserInfo> list) {
    Column<UserInfo, SafeHtml> num = getNum();
    num.setSortable(true);
    addColumn(num, new TextHeader("#"));
    table.addColumnSortHandler(getNumSorter(num, list));
    table.setColumnWidth(num, 50 + "px");
  }

/*
  private void addFinalScore() {
    Column<UserInfo, SafeHtml> start = getFinal();
    start.setSortable(true);
    addColumn(start, new TextHeader("Latest"));
    table.setColumnWidth(start, INITIAL_SCORE_WIDTH + "px");
    table.addColumnSortHandler(getFinalSorter(start, getList()));
  }
*/

  /**
   * @param englishCol
   * @param dataList
   * @return
   */
  private ColumnSortEvent.ListHandler<UserInfo> getNumSorter(Column<UserInfo, SafeHtml> englishCol,
                                                             List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);


    columnSortHandler.setComparator(englishCol,
        (o1, o2) -> {
          if (o1 == o2) {
            return 0;
          }

          // Compare the name columns.
          if (o1 != null) {
            if (o2 == null) return 1;
            else {
              int compare = Integer.compare(o1.getNum(), o2.getNum());
              return compare == 0 ? getDateCompare(o1, o2) : compare;
            }
          }
          return -1;
        });
    return columnSortHandler;
  }


/*  private ColumnSortEvent.ListHandler<UserInfo> getFinalSorter(Column<UserInfo, SafeHtml> englishCol,
                                                               List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol,
        (o1, o2) -> {
          if (o1 == o2) {
            return 0;
          }

          // Compare the name columns.
          if (o1 != null) {
            if (o2 == null) return 1;
            else {
              int compare = Integer.compare(o1.getFinalScores(), o2.getFinalScores());
              return compare == 0 ? getDateCompare(o1, o2) : compare;
            }
          }
          return -1;
        });
    return columnSortHandler;
  }*/

  private ColumnSortEvent.ListHandler<UserInfo> getCurrentSorter(Column<UserInfo, SafeHtml> englishCol,
                                                                 List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol,
        (o1, o2) -> {
          if (o1 == o2) {
            return 0;
          }

          // Compare the name columns.
          if (o1 != null) {
            if (o2 == null) return 1;
            else {
              return Integer.compare(o1.getCurrent(), o2.getCurrent());
            }
          }
          return -1;
        });
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<UserInfo> getMineSorter(Column<UserInfo, SafeHtml> englishCol,
                                                              List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol,
        (o1, o2) -> {
          if (o1 == o2) {
            return 0;
          }

          // Compare the name columns.
          if (o1 != null) {
            if (o2 == null) return 1;
            else {
              return Boolean.compare(myStudents.contains(o1.getID()), myStudents.contains(o2.getID()));
              //        return Integer.valueOf(o1.getAvg()).compareTo(o2.getAvg());
            }
          }
          return -1;
        });
    return columnSortHandler;
  }

/*
  private ColumnSortEvent.ListHandler<UserInfo> getDiffSorter(Column<UserInfo, SafeHtml> englishCol,
                                                              List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol,
        (o1, o2) -> {
          if (o1 == o2) {
            return 0;
          }

          // Compare the name columns.
          if (o1 != null) {
            if (o2 == null) return 1;
            else {
              int compare = Integer.compare(o1.getDiff(), o2.getDiff());
              return compare;
            }
          }
          return -1;
        });
    return columnSortHandler;
  }*/

  private Column<UserInfo, SafeHtml> getCurrent() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml("" + shell.getCurrent());
      }
    };
  }

  private Column<UserInfo, SafeHtml> getMineCol() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml((myStudents.contains(shell.getID()) ? "Y" : "N"));
      }
    };
  }

/*  private Column<UserInfo, SafeHtml> getFinal() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml("" + shell.getFinalScores());
      }
    };
  }

  private Column<UserInfo, SafeHtml> getDiff() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml("" + shell.getDiff());
      }
    };
  }*/

  private Column<UserInfo, SafeHtml> getNum() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml("" + shell.getNum());
      }
    };
  }

  /**
   * @param user
   */
  public void gotClickOnItem(final UserInfo user) {
//    logger.info("gotClickOnItem " + user.getUserID());
    super.gotClickOnItem(user);
    enableButtons();
    rightSide.clear();
    rightSide.add(new AnalysisTab(controller, learnTab, listid == -1 ? MIN_RECORDINGS : 0, overallBottom, user.getID(), user.getUserID(), listid));
  }

  public Button getAdd() {
    return add;
  }

  public Button getRemove() {
    return remove;
  }
}
