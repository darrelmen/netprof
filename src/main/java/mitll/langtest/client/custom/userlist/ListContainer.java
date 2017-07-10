package mitll.langtest.client.custom.userlist;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import mitll.langtest.client.analysis.MemoryItemContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

/**
 * Created by go22670 on 7/6/17.
 */
public class ListContainer extends MemoryItemContainer<UserList<CommonShell>> {
  private boolean slim = false;

  ListContainer(ExerciseController controller, int pageSize, boolean slim, String storageID) {
    super(controller, "netprof" + ":" + controller.getUser() + ":" + storageID, "List",
        pageSize);
    this.slim = slim;
  }

  @NotNull
  protected String getDateColHeader() {
    return "Date";
  }

  protected int getIdWidth() {
    return 200;
  }

/*
  protected int getMaxTableWidth() {
    return 800;
  }
*/

  @Override
  protected void setMaxWidth() {
    // int tableWidth = getMaxTableWidth();
    // table.getElement().getStyle().setProperty("maxWidth", tableWidth + "px");
  }

  @Override
  protected int getMaxLengthId() {
    return 45;
  }

//  protected int getPageSize() {
//    return 20;
//  }

  @Override
  protected void addColumnsToTable(boolean sortEnglish) {
    super.addColumnsToTable(sortEnglish);
    addDescrip();
    if (!slim) {
      addClass();
      addOwner();
    } else {
      addIsPublic();
    }
    // addIsPublic();
  }

//  protected boolean shouldHighlight(UserList<CommonShell> object) {
//    return object.getUserID() == controller.getUser();
//  }

  @Override
  protected int getNameCompare(UserList<CommonShell> o1, UserList<CommonShell> o2) {
    return o1.getName().compareTo(o2.getName());
  }

  @Override
  protected int getDateCompare(UserList<CommonShell> o1, UserList<CommonShell> o2) {
    return Long.valueOf(o1.getModified()).compareTo(o2.getModified());
  }

  @Override
  protected String getItemLabel(UserList<CommonShell> shell) {
    return shell.getName();
  }

  @Override
  public Long getItemDate(UserList<CommonShell> shell) {
    return shell.getModified();
  }

  private void addDescrip() {
    Column<UserList<CommonShell>, SafeHtml> diff = getDescription();
    diff.setSortable(true);
    addColumn(diff, new TextHeader("Description"));
    table.addColumnSortHandler(getDiffSorter(diff, getList()));
    table.setColumnWidth(diff, 200 + "px");
  }

  private void addClass() {
    Column<UserList<CommonShell>, SafeHtml> diff = getListClass();
    diff.setSortable(true);
    addColumn(diff, new TextHeader("Class"));
    table.addColumnSortHandler(getClassSorted(diff, getList()));
    table.setColumnWidth(diff, 100 + "px");
  }

  private void addOwner() {
    Column<UserList<CommonShell>, SafeHtml> diff = getOwner();
    diff.setSortable(true);
    addColumn(diff, new TextHeader("Creator"));
    table.addColumnSortHandler(getOwnerSorted(diff, getList()));
    table.setColumnWidth(diff, 100 + "px");
  }

  private void addIsPublic() {
    Column<UserList<CommonShell>, SafeHtml> diff = getPublic();
    diff.setSortable(true);
    addColumn(diff, new TextHeader("Public"));
    table.addColumnSortHandler(getPublicSorted(diff, getList()));
    table.setColumnWidth(diff, 50 + "px");
  }

  private Column<UserList<CommonShell>, SafeHtml> getDescription() {
    return new Column<UserList<CommonShell>, SafeHtml>(new PagingContainer.ClickableCell()) {
      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, UserList<CommonShell> object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        checkGotClick(object, event);
      }

      @Override
      public SafeHtml getValue(UserList<CommonShell> shell) {
        return getSafeHtml(truncate(shell.getDescription()));
      }
    };
  }

  private Column<UserList<CommonShell>, SafeHtml> getListClass() {
    return new Column<UserList<CommonShell>, SafeHtml>(new PagingContainer.ClickableCell()) {
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
    return new Column<UserList<CommonShell>, SafeHtml>(new PagingContainer.ClickableCell()) {
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
    return new Column<UserList<CommonShell>, SafeHtml>(new PagingContainer.ClickableCell()) {
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

  private ColumnSortEvent.ListHandler<UserList<CommonShell>> getClassSorted(Column<UserList<CommonShell>, SafeHtml> englishCol,
                                                                            List<UserList<CommonShell>> dataList) {
    ColumnSortEvent.ListHandler<UserList<CommonShell>> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
    columnSortHandler.setComparator(englishCol, Comparator.comparing(UserList::getClassMarker));
    return columnSortHandler;
  }
}
