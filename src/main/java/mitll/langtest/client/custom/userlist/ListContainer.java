package mitll.langtest.client.custom.userlist;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.analysis.MemoryItemContainer;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static mitll.langtest.shared.custom.UserList.LIST_TYPE.QUIZ;

/**
 * Created by go22670 on 7/6/17.
 */
public class ListContainer extends MemoryItemContainer<UserList<CommonShell>> {
  //private final Logger logger = Logger.getLogger("ListContainer");

  private static final String DESCRIPTION = "Description";
  private static final String CLASS = "Class";
  private static final String CREATOR = "Creator";
  /**
   * @see #addIsPublic
   */
  private static final String PUBLIC = "Public?";
  private static final String NUM_ITEMS = "#";
  private boolean slim;
  private boolean addOwnerToDescrip;

  private List<Button> buttons = new ArrayList<>();

  /**
   * @param controller
   * @param pageSize
   * @param slim
   * @param storageID
   * @param shortPageSize
   * @see ContentView#showContent(Panel, mitll.langtest.client.custom.INavigation.VIEWS)
   */
  ListContainer(ExerciseController controller, int pageSize, boolean slim, String storageID, int shortPageSize, boolean addOwnerToDescrip) {
    super(controller, "netprof" + ":" + controller.getUser() + ":" + storageID, "List",
        pageSize, shortPageSize);
    this.slim = slim;
    this.addOwnerToDescrip = addOwnerToDescrip;
  }

  void addButton(Button button) {
    buttons.add(button);
  }

  void enableAll() {
    buttons.forEach(button -> button.setEnabled(true));
  }

  void disableAll() {
    buttons.forEach(button -> button.setEnabled(false));
  }

  @NotNull
  protected String getDateColHeader() {
    return "Date";
  }

  protected int getIdWidth() {
    return 200;
  }

  @Override
  protected void setMaxWidth() {
  }

  @Override
  protected int getMaxLengthId() {
    return 45;
  }

  /**
   * @param sortEnglish
   * @see #configureTable
   */
  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    List<UserList<CommonShell>> list = getList();
    addItemID(list, getMaxLengthId());
    addNum();
    addDateCol(list);

    addDescrip();
    if (!slim) {
      addClass();
      addOwner();
    } else {
      addIsPublic();
      if (canMakeQuiz()) {
        addIsQuiz();
      }
    }
  }

  private boolean canMakeQuiz() {
    Collection<User.Permission> permissions = controller.getPermissions();
    return permissions.contains(User.Permission.TEACHER_PERM) || permissions.contains(User.Permission.PROJECT_ADMIN);
  }

  @Override
  protected int getIDCompare(UserList<CommonShell> o1, UserList<CommonShell> o2) {
    return o1.getName().compareTo(o2.getName());
  }

  @Override
  protected int getDateCompare(UserList<CommonShell> o1, UserList<CommonShell> o2) {
    return Long.compare(o1.getModified(), o2.getModified());
  }

  @Override
  protected String getItemLabel(UserList<CommonShell> shell) {
    return shell.getName();
  }

  @Override
  public Long getItemDate(UserList<CommonShell> shell) {
    return shell.getModified();
  }

  private void addNum() {
    Column<UserList<CommonShell>, SafeHtml> diff = getNum();
    diff.setSortable(true);
    addColumn(diff, new TextHeader(NUM_ITEMS));
    table.addColumnSortHandler(getNumSorter(diff, getList()));
    table.setColumnWidth(diff, 40 + "px");
  }

  private void addDescrip() {
    Column<UserList<CommonShell>, SafeHtml> diff = getDescription();
    diff.setSortable(true);
    addColumn(diff, new TextHeader(DESCRIPTION));
    table.addColumnSortHandler(getDiffSorter(diff, getList()));
    table.setColumnWidth(diff, 200 + "px");
  }

  private void addClass() {
    Column<UserList<CommonShell>, SafeHtml> diff = getListClass();
    diff.setSortable(true);
    addColumn(diff, new TextHeader(CLASS));
    table.addColumnSortHandler(getClassSorted(diff, getList()));
    table.setColumnWidth(diff, 100 + "px");
  }

  private void addOwner() {
    Column<UserList<CommonShell>, SafeHtml> diff = getOwner();
    diff.setSortable(true);
    addColumn(diff, new TextHeader(CREATOR));
    table.addColumnSortHandler(getOwnerSorted(diff, getList()));
    table.setColumnWidth(diff, 100 + "px");
  }

  private void addIsPublic() {
    Column<UserList<CommonShell>, SafeHtml> diff = getPublic();
    diff.setSortable(true);
    addColumn(diff, new TextHeader(PUBLIC));
    table.addColumnSortHandler(getPublicSorted(diff, getList()));
    table.setColumnWidth(diff, 50 + "px");
  }

  private void addIsQuiz() {
    Column<UserList<CommonShell>, SafeHtml> diff = getQuiz();
    diff.setSortable(true);
    addColumn(diff, new TextHeader("Quiz?"));
    table.addColumnSortHandler(getQuizSorted(diff, getList()));
    table.setColumnWidth(diff, 50 + "px");
  }

  private Column<UserList<CommonShell>, SafeHtml> getNum() {
    return new Column<UserList<CommonShell>, SafeHtml>(new ClickablePagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserList<CommonShell> object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(UserList<CommonShell> shell) {
        return getSafeHtml("" + shell.getNumItems());
      }
    };
  }


  private Column<UserList<CommonShell>, SafeHtml> getDescription() {
    return new Column<UserList<CommonShell>, SafeHtml>(new ClickablePagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserList<CommonShell> object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(UserList<CommonShell> shell) {
        String owner = addOwnerToDescrip && (shell.getUserID() != controller.getUser()) ? "(" + shell.getUserChosenID() + ") " : "";
        String description = owner + shell.getDescription();
        //   logger.info("Desc " + description + " length " + description.length());
        String truncate = truncate(description, 30);
        //   logger.info("truncate " + truncate + " length " + truncate.length());
        return getNoWrapContent(truncate);
      }
    };
  }

  private Column<UserList<CommonShell>, SafeHtml> getListClass() {
    return new Column<UserList<CommonShell>, SafeHtml>(new ClickablePagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserList<CommonShell> object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(UserList<CommonShell> shell) {
        return getSafeHtml(truncate(shell.getClassMarker()));
      }
    };
  }

  private Column<UserList<CommonShell>, SafeHtml> getOwner() {
    return new Column<UserList<CommonShell>, SafeHtml>(new ClickablePagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserList<CommonShell> object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(UserList<CommonShell> shell) {
        return getSafeHtml(shell.getUserChosenID());
      }
    };
  }

  private Column<UserList<CommonShell>, SafeHtml> getPublic() {
    return new Column<UserList<CommonShell>, SafeHtml>(new ClickablePagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserList<CommonShell> object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(UserList<CommonShell> shell) {
        return getSafeHtml(shell.isPrivate() ? "No" : "Yes");
      }
    };
  }

  private Column<UserList<CommonShell>, SafeHtml> getQuiz() {
    return new Column<UserList<CommonShell>, SafeHtml>(new ClickablePagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserList<CommonShell> object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(UserList<CommonShell> shell) {
        return getSafeHtml(shell.getListType() == QUIZ ? "Yes" : "No");
      }
    };
  }

  private ColumnSortEvent.ListHandler<UserList<CommonShell>> getNumSorter(Column<UserList<CommonShell>, SafeHtml> englishCol,
                                                                          List<UserList<CommonShell>> dataList) {
    ColumnSortEvent.ListHandler<UserList<CommonShell>> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparing(UserList::getNumItems));
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<UserList<CommonShell>> getDiffSorter(Column<UserList<CommonShell>, SafeHtml> englishCol,
                                                                           List<UserList<CommonShell>> dataList) {
    ColumnSortEvent.ListHandler<UserList<CommonShell>> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparing(UserList::getDescription));
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<UserList<CommonShell>> getOwnerSorted(Column<UserList<CommonShell>, SafeHtml> englishCol,
                                                                            List<UserList<CommonShell>> dataList) {
    ColumnSortEvent.ListHandler<UserList<CommonShell>> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparing(UserList::getUserChosenID));
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<UserList<CommonShell>> getPublicSorted(Column<UserList<CommonShell>, SafeHtml> englishCol,
                                                                             List<UserList<CommonShell>> dataList) {
    ColumnSortEvent.ListHandler<UserList<CommonShell>> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparing(UserList::isPrivate));
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<UserList<CommonShell>> getQuizSorted(Column<UserList<CommonShell>, SafeHtml> englishCol,
                                                                           List<UserList<CommonShell>> dataList) {
    ColumnSortEvent.ListHandler<UserList<CommonShell>> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparing(UserList::getListType));
    return columnSortHandler;
  }

  private ColumnSortEvent.ListHandler<UserList<CommonShell>> getClassSorted(Column<UserList<CommonShell>, SafeHtml> englishCol,
                                                                            List<UserList<CommonShell>> dataList) {
    ColumnSortEvent.ListHandler<UserList<CommonShell>> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparing(UserList::getClassMarker));
    return columnSortHandler;
  }
}
