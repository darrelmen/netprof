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
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.server.services.AnalysisServiceImpl;
import mitll.langtest.shared.analysis.UserInfo;
import mitll.langtest.shared.custom.IUserListLight;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/20/15.
 */
public class UserContainer extends BasicUserContainer<UserInfo> implements TypeaheadListener, ReqCounter {
//  private final Logger logger = Logger.getLogger("UserContainer");

  private static final int SESSION_AVG_WIDTH = 85;

  private static final int MAX_NAME_LENGTH = 16;

  /**
   * @see #addName
   */
  private static final String NAME = "Name";
  private static final int SESSION_WIDTH = 95;
  /**
   *
   */
  private static final String POLY_NUMBER = "Session Compl.";//"Session Completed";
  private static final int LIFETIME_WIDTH = 60;
  private static final String LIFETIME = "Life. #";
  private static final int LIFETIME_AVG_WIDTH = 65;
  private static final String LIFETIME_AVG = "Life. Avg";
  private static final String OVERALL_SCORE = "Session Avg";//"Adjust.";

  /**
   * @seex #addLastSession
   */
//  private static final int COMPLETED_WIDTH = 60;
//  private static final String SCORE_FOR_COMPLETED = "Comp. Avg.";//"Completed Score";
  // private static final int CURRENT_WIDTH = 60;

  private static final boolean SHOW_MY_STUDENTS = false;
//  private static final String Y = "Y";

/*
  private static final String MINE = "Mine";
  private static final String MINE_ONLY = "Mine Only";
*/

  private static final String TRYING_TO_GET_STUDENTS = "trying to get students";

//  private static final String FILTER_BY1 = "Filter by";

  private static final int LIST_BOX_WIDTH = 150;

  /**
   * @see #useLists
   */
  private static final String NO_LIST = "(No Quiz)";
  //private static final int FILTER_BY = 19;

  /**
   *
   */
  //private static final String CURRENT = "Avg";


  private static final int MIN_RECORDINGS = AnalysisServiceImpl.MIN_RECORDINGS;

  /**
   * @see #UserContainer(ExerciseController, DivWidget, DivWidget, String)
   */
  private static final String STUDENT = "Student";
  /**
   * @see #getButtons
   */
  // private static final String MY_STUDENT = "My Student";
  // private static final int NUM_WIDTH = 50;

  /**
   * @see #changeSelectedUser(UserInfo)
   * @see
   */
  private final DivWidget rightSide;
  private final DivWidget overallBottom;
  /**
   *
   */
  private Set<Integer> myStudents;

  private Button add;
  private Button remove;
  private Button mineOnly;
  private final UserTypeahead userTypeahead = new UserTypeahead(this);
  //private List<UserInfo> remembered;
  private Collection<UserInfo> orig;

  /**
   * @param controller
   * @param rightSide
   * @see StudentAnalysis#StudentAnalysis
   */
  UserContainer(ExerciseController controller,
                DivWidget rightSide,
                DivWidget overallBottom,
                String selectedUserKey
  ) {
    super(controller, selectedUserKey, STUDENT);
    this.rightSide = rightSide;
    this.overallBottom = overallBottom;
    myStudents = new HashSet<>();
  }

  /**
   * @param users
   * @param leftSide
   * @see MemoryItemContainer#getTable
   */
  @Override
  protected void addTable(Collection<UserInfo> users, DivWidget leftSide) {
    orig = users;
    controller.getDLIClassService().getStudents(new AsyncCallback<Set<Integer>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.getMessageHelper().handleNonFatalError(TRYING_TO_GET_STUDENTS, caught);
      }

      @Override
      public void onSuccess(Set<Integer> result) {
        if (showOnlyMine()) {
          showMyStudents(result, users, leftSide);
        } else {
          showAllStudents(result, users, leftSide);
        }
      }
    });
  }

  private void showAllStudents(Set<Integer> result, Collection<UserInfo> users, DivWidget leftSide) {
    myStudents = result;
    if (mineOnly != null) {
      mineOnly.setEnabled(!myStudents.isEmpty());
    }
    getTableWithButtons(users, leftSide);
  }

  private void showMyStudents(Set<Integer> result, Collection<UserInfo> users, DivWidget leftSide) {
    myStudents = result;

    List<UserInfo> remembered = new ArrayList<>(users);

    List<UserInfo> filtered = new ArrayList<>();
    remembered.forEach(userInfo -> {
      if (myStudents.contains(userInfo.getID())) filtered.add(userInfo);
    });
    getTableWithButtons(filtered, leftSide);
  }

  private void getTableWithButtons(Collection<UserInfo> filtered, DivWidget leftSide) {
    Panel tableWithPager = getTableWithPager(filtered);
    leftSide.add(tableWithPager);
    ((Panel) tableWithPager.getParent()).add(getButtons());
  }

  private boolean showOnlyMine() {
    return SHOW_MY_STUDENTS && controller.getStorage().isTrue("mineOnly");
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

/*    if (!isPolyglot() && SHOW_MY_STUDENTS) {
      buttons.add(new HTML(MY_STUDENT));
      DivWidget addC = new DivWidget();
      addC.add(add = getAddButton());
      buttons.add(addC);
      DivWidget removeC = new DivWidget();
      removeC.add(remove = getRemoveButton());
      buttons.add(removeC);
      buttons.add(mineOnly = getMyStudents());
    }*/

    return buttons;
  }

/*
  @NotNull
  private Button getAddButton() {
    final Button add = new Button(Y, IconType.PLUS);
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
  }*/

  private void enableButtons() {
    boolean onMyList = myStudents.contains(getCurrentSelection().getID());
    if (add != null) {
      add.setEnabled(!onMyList);
      remove.setEnabled(onMyList);
    }
  }

/*  @NotNull
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
          controller.handleNonFatalError("removing a student from my students", caught);
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
  }*/


  /**
   * TODO : consider how to make this work for IE - inlineFlex doesn't
   *
   * @return
   * @see MemoryItemContainer#getTable
   */
  @Override
  protected IsWidget getBelowHeader() {
    DivWidget filterContainer = new DivWidget();
    filterContainer.setWidth("100%");
    filterContainer.addStyleName("leftFiveMargin");

    filterContainer.add(userTypeahead.getSearch());

    addListChoiceBox(filterContainer);
    return filterContainer;
  }

  protected void addListChoiceBox(DivWidget filterContainer) {
    DivWidget c = new DivWidget();
    c.addStyleName("leftFiveMargin");
    c.addStyleName("floatRight");
    c.add(getListBox());
    filterContainer.add(c);
  }

/*
  @NotNull
  private HTML getFilterLabel() {
    HTML filterLabel = new HTML(FILTER_BY1);

    filterLabel.addStyleName("floatLeft");
    filterLabel.addStyleName("rightFiveMargin");
    filterLabel.addStyleName("topFiveMargin");

    // mimic style of subtext on headers
    filterLabel.getElement().getStyle().setFontSize(14, Style.Unit.PX);
    filterLabel.getElement().getStyle().setColor("#999");
    return filterLabel;
  }
*/

/*
  @NotNull
  private Button getMyStudents() {
    Button mineOnly = new Button(MINE_ONLY);
    mineOnly.setToggle(true);
    mineOnly.setSize(ButtonSize.MINI);
    mineOnly.addStyleName("leftFiveMargin");

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
  }*/

/*
  private void showAllUsers() {
    setStorageMineOnly(false);
    populateTable(remembered);
  }
*/

/*
  private void setStorageMineOnly(boolean val) {
    controller.getStorage().setBoolean("mineOnly", val);
  }
*/

  /**
   * @seex #getMyStudents
   */
/*
  private void rememberAndFilter() {
    remembered = new ArrayList<>(getList());
    filterUsers();
  }
*/

/*
  private void filterUsers() {
    List<UserInfo> filtered = new ArrayList<>();

    remembered.forEach(userInfo -> {
      if (myStudents.contains(userInfo.getID())) filtered.add(userInfo);
    });

    populateTable(filtered);
  }
*/
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

      changeSelectedUser(getSelected());
    });
  }

  /**
   * @see SimplePagingContainer#configureTable
   */
  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    List<UserInfo> list = getList();
    addItemID(list, 20);
    addName(list);
    Column<UserInfo, SafeHtml> userInfoSafeHtmlColumn = addDateCol(list);
    table.getColumnSortList().push(userInfoSafeHtmlColumn);

    addNumber(list);
    addCurrent(list);

/*
      if (SHOW_MY_STUDENTS) addMine(list);
*/

    addLastTwoColumns(list);
  }

  protected void addLastTwoColumns(List<UserInfo> list) {
    addPolyNumber(list);
    //  addLastSession(list);
    /*Column<UserInfo, SafeHtml> column =*/
    addLastOverallScore(list);
    //  table.getColumnSortList().push(column);

    table.setWidth("100%", true);

    addTooltip();
  }

  private void addName(List<UserInfo> list) {
    Column<UserInfo, SafeHtml> userCol = getTruncatedCol(MAX_NAME_LENGTH, UserInfo::getName);
    table.setColumnWidth(userCol, NAME_WIDTH + "px");
    addColumn(userCol, new TextHeader(NAME));
    table.addColumnSortHandler(getNameSorter(userCol, list));
  }

  private ColumnSortEvent.ListHandler<UserInfo> getNameSorter(Column<UserInfo, SafeHtml> englishCol,
                                                              List<UserInfo> dataList) {
    ColumnSortEvent.ListHandler<UserInfo> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, this::getNameCompare);
    return columnSortHandler;
  }

  private void addCurrent(List<UserInfo> list) {
    Column<UserInfo, SafeHtml> current = getCurrent();
    addColumn(current, new TextHeader(getLifetimeAvgTitle()));
    table.setColumnWidth(current, LIFETIME_AVG_WIDTH + "px");
    table.addColumnSortHandler(getCurrentSorter(current, list));
  }

  @NotNull
  protected String getLifetimeAvgTitle() {
    return LIFETIME_AVG;
  }

/*  private Column<UserInfo, SafeHtml> addLastSession(List<UserInfo> list) {
    Column<UserInfo, SafeHtml> current = getLastSession();
    addColumn(current, new TextHeader(SCORE_FOR_COMPLETED));
    table.setColumnWidth(current, COMPLETED_WIDTH + "px");

    table.addColumnSortHandler(getLastSessionSorter(current, list));
    return current;
  }*/

  private Column<UserInfo, SafeHtml> addLastOverallScore(List<UserInfo> list) {
    Column<UserInfo, SafeHtml> current = getOverall();
    addColumn(current, new TextHeader(OVERALL_SCORE));
    table.setColumnWidth(current, SESSION_AVG_WIDTH + "px");
    table.addColumnSortHandler(getOverallSorter(current, list));
    return current;
  }

/*  private void addMine(List<UserInfo> list) {
    Column<UserInfo, SafeHtml> current = getMineCol();
    current.setSortable(true);
    addColumn(current, new TextHeader(MINE));
    table.setColumnWidth(current, 65 + "px");

    table.addColumnSortHandler(getMineSorter(current, list));
  }*/

  private void addNumber(List<UserInfo> list) {
    Column<UserInfo, SafeHtml> num = getNum();
    addColumn(num, new TextHeader(getLifetimeCountTitle()));
    table.addColumnSortHandler(getNumSorter(num, list));
    table.setColumnWidth(num, LIFETIME_WIDTH + "px");
  }

  @NotNull
  protected String getLifetimeCountTitle() {
    return LIFETIME;
  }

  private void addPolyNumber(List<UserInfo> list) {
    Column<UserInfo, SafeHtml> num = getPolyNum();
    addColumn(num, new TextHeader(POLY_NUMBER));
    table.addColumnSortHandler(getPolyNumSorter(num, list));
    table.setColumnWidth(num, SESSION_WIDTH + "px");
  }

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

  /**
   * Sort by % then by number in set.
   *
   * @param englishCol
   * @param dataList
   * @return
   */
  private ColumnSortEvent.ListHandler<UserInfo> getPolyNumSorter(Column<UserInfo, SafeHtml> englishCol,
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
              int p1 = getPercent(o1);
              int p2 = getPercent(o2);
              int compare = Integer.compare(p1, p2);
              if (compare == 0) {
                compare = Integer.compare(o1.getLastSessionNum(), o2.getLastSessionNum());
              }

              return compare == 0 ? getDateCompare(o1, o2) : compare;
            }
          }
          return -1;
        });
    return columnSortHandler;
  }

  private int getPercent(UserInfo o1) {
    int lastSessionNum1 = o1.getLastSessionNum();
    int lastSessionSize1 = o1.getLastSessionSize();
    if (lastSessionSize1 == -1) lastSessionSize1 = lastSessionNum1;
    return getPercent(lastSessionNum1, lastSessionSize1);
  }

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

/*  private ColumnSortEvent.ListHandler<UserInfo> getLastSessionSorter(Column<UserInfo, SafeHtml> englishCol,
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
              return Integer.compare(o1.getLastSessionScore(), o2.getLastSessionScore());
            }
          }
          return -1;
        });
    return columnSortHandler;
  }*/

  private ColumnSortEvent.ListHandler<UserInfo> getOverallSorter(Column<UserInfo, SafeHtml> englishCol,
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
              float s1 = getAdjustedScore(o1);
              float s2 = getAdjustedScore(o2);
              return Float.compare(s1, s2);
            }
          }
          return -1;
        });
    return columnSortHandler;
  }

/*
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
  }*/


  private Column<UserInfo, SafeHtml> getCurrent() {
    return getClickable(this::getCurrentText);
  }

  @NotNull
  private String getCurrentText(UserInfo shell) {
    return "" + shell.getCurrent();
  }

  @NotNull
  private String getNumText(UserInfo shell) {
    return "" + shell.getNum();
  }

/*  private Column<UserInfo, SafeHtml> getLastSession() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public boolean isDefaultSortAscending() {
        return false;
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        String columnText = getLastSessionScore(shell);
        return getSafeHtml(columnText);
      }
    };
  }*/

/*
  private Column<UserInfo, SafeHtml> getLastSession() {
    return getClickableDesc(this::getLastSessionScore);
  }
*/

/*  @NotNull
  private String getLastSessionScore(UserInfo shell) {
    int lastSessionScore = shell.getLastSessionScore() / 10;
    // return getSafeHtml("" + Integer.valueOf(lastSessionScore).floatValue()/10F);
    return "" + lastSessionScore;
  }*/

  private Column<UserInfo, SafeHtml> getOverall() {
    return getClickableDesc(this::getOverallScore);
  }

  @NotNull
  private String getOverallScore(UserInfo shell) {
    String columnText = "" + getAdjustedScore(shell);
    if (!columnText.contains(".")) columnText += ".0";
    return columnText;
  }

  private float getAdjustedScore(UserInfo shell) {
    int percent = getPercent(shell);
    //int lastSessionScore = shell.getLastSessionScore();
    //int lastSessionScore = shell.getLastSessionScore()/10;
    float v = Integer.valueOf(shell.getLastSessionScore()).floatValue();
    // return getSafeHtml("" + Integer.valueOf(lastSessionScore).floatValue()/10F);
    // return getSafeHtml("" + lastSessionScore);

    float lastf = v;//(float) lastSessionScore;
    if (percent < 50) {
      lastf *= 0.8f;
    } else if (percent < 60) {
      lastf *= 0.9f;
    }

    // return Math.round(lastf) / 10F;
    return Integer.valueOf(Math.round(lastf)).floatValue() / 10F;
  }

/*
  private Column<UserInfo, SafeHtml> getMineCol() {
    return new Column<UserInfo, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserInfo object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(UserInfo shell) {
        return getSafeHtml((myStudents.contains(shell.getID()) ? Y : "N"));
      }
    };
  }
*/

  private Column<UserInfo, SafeHtml> getNum() {
    return getClickable(this::getNumText);
  }

  private Column<UserInfo, SafeHtml> getPolyNum() {
    return getNoWrapCol(this::getPolyNumValue);
  }

  @NotNull
  private String getPolyNumValue(UserInfo shell) {
    int lastSessionNum = shell.getLastSessionNum();
    int lastSessionSize = shell.getLastSessionSize();
    if (lastSessionSize == -1) lastSessionSize = lastSessionNum;
    boolean same = lastSessionNum == lastSessionSize;
    return same ?
        ("" + lastSessionNum) :
        "" + lastSessionNum + "/" + lastSessionSize + " (" + getPercent(lastSessionNum, lastSessionSize) +
            "%)";
  }


  private int getPercent(float totalScore, float denom) {
    float v = totalScore / denom;
    // logger.info("ratio " + v);
    float fround = Math.round(v * 100);
    // logger.info("fround " + fround);

    return (int) (fround);
  }

  private int req = 0;

  @Override
  public int getReq() {
    return req;
  }

  private UserInfo lastSelected = null;

  /**
   * @see #selectAndClick
   * @see #checkGotClick
   * @param selectedUser
   */
  public void gotClickOnItem(final UserInfo selectedUser) {
    if (lastSelected != selectedUser) {
      //logger.info("gotClickOnItem " + selectedUser.getUserID());
      lastSelected = selectedUser;
      changeSelectedUser(selectedUser);
    }
  }

  private void changeSelectedUser(UserInfo selectedUser) {
    super.gotClickOnItem(selectedUser);
    enableButtons();
    rightSide.clear();

    addRightSideContent(selectedUser);
  }

  protected void addRightSideContent(UserInfo selectedUser) {
    rightSide.add(getAnalysisTab(selectedUser));
  }

  @NotNull
  protected Widget getAnalysisTab(UserInfo selectedUser) {
    return new AnalysisTab(controller,
        listid == -1 ? MIN_RECORDINGS : 0,
        overallBottom,
        selectedUser.getID(),
        selectedUser.getUserID(),
        listid,
        listid != -1,
        incrReq(),
        this, INavigation.VIEWS.LEARN);
  }

  int incrReq() {
    return req++;
  }

/*  public Button getAdd() {
    return add;
  }
  public Button getRemove() {
    return remove;
  }*/

  @Override
  public void gotKey(String text) {
    //logger.info("gotKey '" + text +"'");
    boolean onlyOne = getList().size() == 1;
    Collection<UserInfo> matches = getMatches(text);

    if (onlyOne && matches.size() == 1 &&
        getList().get(0).getID() == matches.iterator().next().getID()) {
      //  logger.info("skip...");
    } else {
      populateTable(matches);
    }
  }

  @NotNull
  private Collection<UserInfo> getMatches(String text) {
    //  logger.info("getMatches for " + text);
    List<UserInfo> matches = new ArrayList<>();

    String prefix = text.toLowerCase();

    for (UserInfo user : orig) {
      //String formattedDateString = getFormattedDateString(getItemDate(user)).toLowerCase();
      //logger.info("user "+ user.getID() + " " + user.getUserID() + " " + formattedDateString);
      if (user.getUserID().toLowerCase().startsWith(prefix)
          || user.getFirst().toLowerCase().startsWith(prefix)
          || user.getLast().toLowerCase().startsWith(prefix)
          || getFormattedDateString(getItemDate(user)).toLowerCase().startsWith(prefix)
      ) {
        matches.add(user);
      }
    }
    return matches.isEmpty() ? orig : matches;
  }
}
