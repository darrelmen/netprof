package mitll.langtest.client.result;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.client.analysis.BasicUserContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.user.ActiveUser;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

public class ActiveUsersManager {
  //private final Logger logger = Logger.getLogger("ActiveUsersManager");

  private static final int HOUR = 60 * 60 * 1000;
  private static final String LOGGED_IN = "Logged In";
  private static final String LAST_ACTIVITY = "Last Active";
  private static final int INTCOL_WIDTH = 110;

  private final ExerciseController controller;
  private static final int TOP = 56;

  /**
   * @param controller
   * @see mitll.langtest.client.banner.UserMenu.ReportListHandler
   */
  public ActiveUsersManager(ExerciseController controller) {
    this.controller = controller;
  }

  public void show(int hours) {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Active Users" + (hours <= 24 ? " (last day)" : " (last week)"));

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    int left = ((Window.getClientWidth()) / 2) - 200;
    dialogBox.setPopupPosition(left, TOP);

    final Panel dialogVPanel = new VerticalPanel();
    dialogVPanel.setWidth("100%");

    controller.getUserService().getUsersSince(System.currentTimeMillis() - hours * HOUR, new AsyncCallback<List<ActiveUser>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.getMessageHelper().handleNonFatalError("getting active users", caught);
      }

      @Override
      public void onSuccess(List<ActiveUser> result) {
        dialogVPanel.add(new ActiveUserBasicUserContainer().getTableWithPager(result));

        DivWidget horiz = new DivWidget();
        horiz.setWidth("100%");
        horiz.add(getOKButton(dialogBox));
        dialogVPanel.add(horiz);
      }
    });


    dialogBox.setWidget(dialogVPanel);

    dialogBox.show();
  }

  @NotNull
  private Button getOKButton(DialogBox dialogBox) {
    Button ok = new Button("OK");
    ok.setType(ButtonType.SUCCESS);
    ok.setSize(ButtonSize.LARGE);
    ok.addStyleName("floatRight");
    ok.addClickHandler(event -> dialogBox.hide());
    return ok;
  }

  private class ActiveUserBasicUserContainer extends BasicUserContainer<ActiveUser> {
    ActiveUserBasicUserContainer() {
      super(ActiveUsersManager.this.controller, "activeUser", "User");
    }

    @Override
    protected String getDateColHeader() {
      return LOGGED_IN;
    }

    @Override
    protected void addColumnsToTable(boolean sortEnglish) {
      List<ActiveUser> list = getList();
      addUserID(list);
      super.addColumnsToTable(sortEnglish);

      addVisitedCol(list);
      addLang(list);
      addProj(list);
    }

    void addUserID(List<ActiveUser> list) {
      Column<ActiveUser, SafeHtml> userCol = getClickable(this::getIDString);
      table.setColumnWidth(userCol, 45 + "px");
      addColumn(userCol, new TextHeader("ID"));
      table.addColumnSortHandler(getIDSorter(userCol, list));
    }

    @NotNull
    private String getIDString(ActiveUser shell) {
      return "" + shell.getID();
    }

    private ColumnSortEvent.ListHandler<ActiveUser> getIDSorter(Column<ActiveUser, SafeHtml> englishCol,
                                                                List<ActiveUser> dataList) {
      ColumnSortEvent.ListHandler<ActiveUser> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
      columnSortHandler.setComparator(englishCol, this::getRealIDCompare);
      return columnSortHandler;
    }


    protected int getDateWidth() {
      return INTCOL_WIDTH;
    }

    private final DateTimeFormat format = DateTimeFormat.getFormat("MMM d h:mm a");

    @Override
    protected String getFormattedDateString(Long itemDate) {
      return format.format(new Date(itemDate));
    }

    private void addLang(List<ActiveUser> list) {
      Column<ActiveUser, SafeHtml> userCol = getNoWrapCol(ActiveUser::getLanguage);
      table.setColumnWidth(userCol, NAME_WIDTH + "px");
      addColumn(userCol, new TextHeader("Language"));
      table.addColumnSortHandler(getLangSorter(userCol, list));
    }

    private void addProj(List<ActiveUser> list) {
      Column<ActiveUser, SafeHtml> userCol = getNoWrapCol(ActiveUser::getProjectName);
      table.setColumnWidth(userCol, NAME_WIDTH + "px");
      addColumn(userCol, new TextHeader("Project"));
      table.addColumnSortHandler(getProjSorter(userCol, list));
    }

    private void addVisitedCol(List<ActiveUser> list) {
      Column<ActiveUser, SafeHtml> dateCol = getVisitedColumn();
      dateCol.setSortable(true);
      addColumn(dateCol, new TextHeader(LAST_ACTIVITY));
      table.setColumnWidth(dateCol, INTCOL_WIDTH + "px");
      table.addColumnSortHandler(getVisitedSorter(dateCol, list));
    }

    private ColumnSortEvent.ListHandler<ActiveUser> getVisitedSorter(Column<ActiveUser, SafeHtml> englishCol,
                                                                     List<ActiveUser> dataList) {
      ColumnSortEvent.ListHandler<ActiveUser> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
      columnSortHandler.setComparator(englishCol, this::getVisitedCompare);
      return columnSortHandler;
    }

    private int getVisitedCompare(ActiveUser o1, ActiveUser o2) {
      if (o1 == o2) {
        return 0;
      }

      // Compare the name columns.
      if (o1 != null) {
        if (o2 == null) return 1;
        else {
          return Long.compare(o1.getVisited(), o2.getVisited());
        }
      }
      return -1;
    }

    private Column<ActiveUser, SafeHtml> getVisitedColumn() {
      return getClickableDesc(shell -> getNoWrapDate(shell.getVisited()));
    }

    private ColumnSortEvent.ListHandler<ActiveUser> getLangSorter(Column<ActiveUser, SafeHtml> englishCol,
                                                                  List<ActiveUser> dataList) {
      ColumnSortEvent.ListHandler<ActiveUser> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
      columnSortHandler.setComparator(englishCol, this::getLangCompare);
      return columnSortHandler;
    }

    private ColumnSortEvent.ListHandler<ActiveUser> getProjSorter(Column<ActiveUser, SafeHtml> englishCol,
                                                                  List<ActiveUser> dataList) {
      ColumnSortEvent.ListHandler<ActiveUser> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataList);
      columnSortHandler.setComparator(englishCol, this::getProjCompare);
      return columnSortHandler;
    }

    int getLangCompare(ActiveUser o1, ActiveUser o2) {
      if (o1 == o2) {
        return 0;
      }

      // Compare the name columns.
      if (o1 != null) {
        if (o2 == null) return 1;
        else {
          int i = o1.getLanguage().compareTo(o2.getLanguage());
          return i == 0 ? getDateCompare(o1, o2) : i;
        }
      }
      return -1;
    }

    int getProjCompare(ActiveUser o1, ActiveUser o2) {
      if (o1 == o2) {
        return 0;
      }

      // Compare the name columns.
      if (o1 != null) {
        if (o2 == null) return 1;
        else {
          int i = o1.getProjectName().compareTo(o2.getProjectName());
          return i == 0 ? getDateCompare(o1, o2) : i;
        }
      }
      return -1;
    }
  }
}
