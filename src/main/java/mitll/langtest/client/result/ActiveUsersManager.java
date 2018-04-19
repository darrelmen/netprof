package mitll.langtest.client.result;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.resources.ButtonSize;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
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
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.shared.analysis.UserInfo;
import mitll.langtest.shared.user.ActiveUser;
import mitll.langtest.shared.user.FirstLastUser;
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
    dialogBox.setText("Active Users (last day)");

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
        BasicUserContainer<ActiveUser> active_users = new ActiveUserBasicUserContainer();
        Panel tableWithPager = active_users.getTableWithPager(result);
        dialogVPanel.add(tableWithPager);
        Button ok = new Button("OK");
        ok.setType(ButtonType.SUCCESS);
        ok.setSize(ButtonSize.LARGE);
        ok.addClickHandler(event -> dialogBox.hide());
        DivWidget horiz = new DivWidget();
        horiz.setWidth("100%");
        horiz.add(ok);
        ok.addStyleName("floatRight");
        dialogVPanel.add(horiz);
      }
    });


    dialogBox.setWidget(dialogVPanel);

    dialogBox.show();
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
      super.addColumnsToTable(sortEnglish);

      addVisitedCol(getList());
      addLang(getList());
      addProj(getList());
    }

    protected int getDateWidth() {
      return INTCOL_WIDTH;
    }

    private final DateTimeFormat format = DateTimeFormat.getFormat("MMM d h:mm a");

    @Override protected String getFormattedDateString(Long itemDate) {
      Date date = new Date(itemDate);
      String signedUp = format.format(date);

//      // drop year if this year
//      if (signedUp.equals(todaysDate)) {
//        signedUp = todayTimeFormat.format(date);
//      } else if (todayYear.equals(signedUp.substring(signedUp.length() - 2))) {
//        signedUp = signedUp.substring(0, signedUp.length() - 4);
//      }
      return signedUp;
    }
    private void addLang(List<ActiveUser> list) {
      Column<ActiveUser, SafeHtml> userCol = new Column<ActiveUser, SafeHtml>(new PagingContainer.ClickableCell()) {
        @Override
        public SafeHtml getValue(ActiveUser shell) {
          return getNoWrapContent(shell.getLanguage());
        }
      };
      userCol.setSortable(true);
      table.setColumnWidth(userCol, NAME_WIDTH + "px");
      addColumn(userCol, new TextHeader("Language"));
      table.addColumnSortHandler(getLangSorter(userCol, list));
    }

    private void addProj(List<ActiveUser> list) {
      Column<ActiveUser, SafeHtml> userCol = new Column<ActiveUser, SafeHtml>(new PagingContainer.ClickableCell()) {
        @Override
        public SafeHtml getValue(ActiveUser shell) {
          return getNoWrapContent(shell.getProjectName());
        }
      };
      userCol.setSortable(true);
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
      return new Column<ActiveUser, SafeHtml>(new PagingContainer.ClickableCell()) {
        @Override
        public void onBrowserEvent(Cell.Context context, Element elem, ActiveUser object, NativeEvent event) {
          super.onBrowserEvent(context, elem, object, event);
          checkGotClick(object, event);
        }

        @Override
        public boolean isDefaultSortAscending() {
          return false;
        }

        @Override
        public SafeHtml getValue(ActiveUser shell) {
          return getFormattedDate(shell.getVisited());
        }
      };
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
