/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

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
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.client.analysis.BasicUserContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.user.ActiveUser;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class ActiveUsersManager {
  private final Logger logger = Logger.getLogger("ActiveUsersManager");

  private static final int HOUR = 60 * 60 * 1000;
  private static final String LOGGED_IN = "Logged In";
  private static final String LAST_ACTIVITY = "Last Active";
  private static final int INTCOL_WIDTH = 110;
  public static final String OK = "OK";

  protected final ExerciseController controller;
  private static final int TOP = 56;

  /**
   * @param controller
   * @see mitll.langtest.client.banner.UserMenu#showActiveUsers
   */
  public ActiveUsersManager(ExerciseController controller) {
    this.controller = controller;
  }

  public void show(String title, int hours) {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();

    dialogBox.setText(title +
        (hours == 0 ? "" :
            (hours <= 24 ? " (last day)" : " (last week)")));

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    int left = 50;//((Window.getClientWidth()) / 2) - 200;
    dialogBox.setPopupPosition(left, TOP);

    final Panel dialogVPanel = new VerticalPanel();
    dialogVPanel.setWidth("100%");

    getUsers(hours, dialogBox, dialogVPanel);

    dialogBox.setWidget(dialogVPanel);

    dialogBox.show();
  }

  protected void getUsers(int hours, DialogBox dialogBox, Panel dialogVPanel) {
    controller.getUserService().getUsersSince(System.currentTimeMillis() - hours * HOUR, new AsyncCallback<List<ActiveUser>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.getMessageHelper().handleNonFatalError("getting active users", caught);
      }

      @Override
      public void onSuccess(List<ActiveUser> result) {
        gotUsers(result, dialogVPanel, dialogBox);
      }
    });
  }

  protected void addPrompt(Panel dialogVPanel) {
  }

  private ActiveUserBasicUserContainer activeUserBasicUserContainer;

  protected void reload() {
    activeUserBasicUserContainer.redraw();
  }

  protected void gotUsers(List<ActiveUser> result, Panel dialogVPanel, DialogBox dialogBox) {
    activeUserBasicUserContainer = getUserContainer();
    dialogVPanel.add(activeUserBasicUserContainer.getTableWithPager(result));


    DivWidget horiz = new DivWidget();
    horiz.setWidth("100%");
    addButtons(dialogBox, horiz);
    dialogVPanel.add(horiz);
  }

  protected ActiveUserBasicUserContainer getUserContainer() {
    return new ActiveUserBasicUserContainer();
  }

  public ActiveUser getCurrentSelection() {
    return activeUserBasicUserContainer.getCurrentSelection();
  }

  protected Button addButtons(DialogBox dialogBox, DivWidget horiz) {
    Button okButton = getOKButton(dialogBox);
    horiz.add(okButton);
    return okButton;
  }

  @NotNull
  private Button getOKButton(DialogBox dialogBox) {
    Button ok = getButton(OK);
    ok.addClickHandler(event -> gotOKClick(dialogBox));
    return ok;
  }

  protected void gotOKClick(DialogBox dialogBox) {
    logger.info("gotOKClick!!!");
    dialogBox.hide();
  }

  @NotNull
  protected Button getButton(String ok1) {
    Button ok = new Button(ok1);
    ok.setType(ButtonType.SUCCESS);
    ok.setSize(ButtonSize.LARGE);
    ok.addStyleName("floatRight");
    return ok;
  }

  protected class ActiveUserBasicUserContainer extends BasicUserContainer<ActiveUser> {
    protected ActiveUserBasicUserContainer() {
      super(ActiveUsersManager.this.controller, "activeUser", "User");
    }

    @Override
    protected String getDateColHeader() {
      return LOGGED_IN;
    }

    @Override
    protected void addColumnsToTable() {
      List<ActiveUser> list = getList();
      addUserID(list);
      super.addColumnsToTable();

      addVisitedCols(list);
    }

    protected void addVisitedCols(List<ActiveUser> list) {
      addVisitedCol(list);
      addLang(list);
      addProj(list);
    }

    protected void addUserID(List<ActiveUser> list) {
      Column<ActiveUser, SafeHtml> userCol = getClickable(this::getIDString);
      table.setColumnWidth(userCol, 55 + "px");
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

    /**
     * @see #get
     * @param list
     */
    protected void addVisitedCol(List<ActiveUser> list) {
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
