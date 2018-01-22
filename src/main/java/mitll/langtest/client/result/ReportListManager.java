package mitll.langtest.client.result;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.services.ProjectService;
import mitll.langtest.client.services.ProjectServiceAsync;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.project.ProjectProperty;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class ReportListManager {
  private final Logger logger = Logger.getLogger("ReportListManager");


  private final ExerciseController controller;
  private static final int TOP = 56;
  private static final String CLOSE = "Close";

  public ReportListManager(ExerciseController controller) {
    this.controller = controller;
  }

  public void showReportList() {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Report List");

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    int left = (Window.getClientWidth()) / 200;
    //int top = (Window.getClientHeight()) / 200;
    dialogBox.setPopupPosition(left, TOP);

    final Panel dialogVPanel = new VerticalPanel();
    dialogVPanel.setWidth("100%");

    final ProjectServiceAsync projectServiceAsync = GWT.create(ProjectService.class);

    projectServiceAsync.getListProperty(1, ProjectProperty.REPORT_LIST, new AsyncCallback<List<String>>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.getMessageHelper().handleNonFatalError("getting number of recordings", caught);
      }

      @Override
      public void onSuccess(List<String> num) {
//        populateTable(num, dialogVPanel, dialogBox, getCloseButton(dialogBox));

        Collections.sort(num);
        List<MyEmail> toAdd = new ArrayList<>();
        num.forEach(n -> toAdd.add(new MyEmail(toAdd.size(), n)));
        SimplePagingContainer<MyEmail> myLists = new StringSimplePagingContainer(toAdd);
        Panel tableWithPager = myLists.getTableWithPager(new ListOptions());

        dialogVPanel.add(tableWithPager);
        dialogVPanel.add(getCloseButton(dialogBox));
        //   addPagerAndHeader(tableWithPager, YOUR_LISTS, left);
        tableWithPager.setHeight(300 + "px");

        //  left.add(getButtons(ListView.this.myLists));
      }
    });

    dialogBox.setWidget(dialogVPanel);

    dialogBox.show();
  }


  private Button getCloseButton(final DialogBox dialogBox) {
    final Button closeButton = new Button(CLOSE);
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButtonLessTopMargin");
//    eventRegistration.register(closeButton, "N/A", "Close recordings dialog");
    // Add a handler to send the name to the server
    closeButton.addClickHandler(event -> dialogBox.hide());
    return closeButton;
  }

  private static class MyEmail implements HasID {

    int id;
    String value;

    public MyEmail() {
    }

    public MyEmail(int id, String value) {
      this.id = id;
      this.value = value;
    }

    @Override
    public int getID() {
      return id;
    }

    @Override
    public int compareTo(@NotNull HasID o) {
      return Integer.compare(id, o.getID());
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return id + " : " + value;
    }
  }

  MyEmail current = null;

  private class StringSimplePagingContainer extends ClickablePagingContainer<MyEmail> {
    private final List<MyEmail> num;

    public StringSimplePagingContainer(List<MyEmail> num) {
      super(ReportListManager.this.controller);
      this.num = num;
    }

    @Override
    public void gotClickOnItem(MyEmail e) {
      logger.info("got  click on " + e);
      current = e;
    }

    @Override
    public Panel getTableWithPager(ListOptions listOptions) {
      Panel tableWithPager = super.getTableWithPager(listOptions);
      num.forEach(this::addItem);
      return tableWithPager;
    }

    @Override
    protected void addColumnsToTable(boolean sortEnglish) {
      addItemID(200);
    }

    protected void addItemID(int maxLength) {
      Column<MyEmail, SafeHtml> userCol = getItemColumn(maxLength);
      userCol.setSortable(true);
      // table.setColumnWidth(userCol, getIdWidth() + "px");
      addColumn(userCol, new TextHeader("User"));
      //  table.addColumnSortHandler(getUserSorter(userCol, list));
    }


    private Column<MyEmail, SafeHtml> getItemColumn(int maxLength) {
      return new Column<MyEmail, SafeHtml>(new PagingContainer.ClickableCell()) {
        @Override
        public void onBrowserEvent(Cell.Context context, Element elem, MyEmail object, NativeEvent event) {
          super.onBrowserEvent(context, elem, object, event);

          logger.info("Got click " + object);
        }

        @Override
        public SafeHtml getValue(MyEmail shell) {
          return getSafeHtml(shell.getValue());

        }

        protected SafeHtml getSafeHtml(String columnText) {
          return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
        }
      };
    }

    @Override
    protected void addItem(MyEmail item) {
      super.addItem(item);
    }
  }
}
