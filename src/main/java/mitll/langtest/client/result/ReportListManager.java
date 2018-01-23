package mitll.langtest.client.result;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.exercise.ClickablePagingContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.services.ProjectService;
import mitll.langtest.client.services.ProjectServiceAsync;
import mitll.langtest.client.user.UserDialog;
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
  private final ProjectServiceAsync projectServiceAsync = GWT.create(ProjectService.class);
  private static final int MY_LIST_HEIGHT = 300;

  public ReportListManager(ExerciseController controller) {
    this.controller = controller;
  }

  public void showReportList() {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("Report List");

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    int left = ((Window.getClientWidth()) / 2) - 200;
    //int top = (Window.getClientHeight()) / 200;
    dialogBox.setPopupPosition(left, TOP);

    final Panel dialogVPanel = new VerticalPanel();
    dialogVPanel.setWidth("100%");


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
        ClickablePagingContainer<MyEmail> emails = new StringSimplePagingContainer(toAdd);
        Panel tableWithPager = emails.getTableWithPager(new ListOptions());

        tableWithPager.setHeight(MY_LIST_HEIGHT + "px");

        dialogVPanel.add(tableWithPager);
        DivWidget bottom = new DivWidget();
        bottom.addStyleName("inlineFlex");
        bottom.add(getButtons(emails));
        bottom.add(getCloseButton(dialogBox));


        if (!num.isEmpty()) emails.markCurrentExercise(1);
        dialogVPanel.add(bottom);
        tableWithPager.setHeight(300 + "px");
      }
    });

    dialogBox.setWidget(dialogVPanel);

    dialogBox.show();
  }

  ClickablePagingContainer<MyEmail> emails;

  @NotNull
  private DivWidget getButtons(ClickablePagingContainer<MyEmail> emails) {
    this.emails = emails;
    DivWidget buttons = new DivWidget();
    buttons.addStyleName("inlineFlex");
    buttons.addStyleName("topFiveMargin");
    buttons.add(getRemoveButton());
    buttons.add(getAddButton());

    return buttons;
  }

  TextBox email;
  Button addButton;

  /**
   * @return
   * @see #getButtons
   */
  @NotNull
  private DivWidget getAddButton() {
    final Button add = new Button("", IconType.PLUS);
    add.addClickHandler(event -> doAdd());
    add.setType(ButtonType.SUCCESS);
    add.setEnabled(false);
    addTooltip(add, "Add report recipient email.");
    this.addButton = add;
    DivWidget widgets = new DivWidget();
    widgets.addStyleName("inlineFlex");
    email = new TextBox();
    email.setVisibleLength(100);
    email.addKeyUpHandler(new KeyUpHandler() {
      @Override
      public void onKeyUp(KeyUpEvent event) {
        //add.setEnabled(!email.getText().isEmpty());
        // boolean validEmail = new UserDialog(controller.getProps()).isValidEmail(email.getText());
        //logger.info("valid " + validEmail + " for " + email.getText());
        String newEmail = email.getText();
        add.setEnabled(!newEmail.isEmpty() &&
            new UserDialog(controller.getProps()).isValidEmail(newEmail) &&
            !getCurrentValues().contains(newEmail.trim()));
      }
    });
/*
    email.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(KeyPressEvent event) {
        add.setEnabled(!email.getText().isEmpty());

        boolean validEmail = new UserDialog(controller.getProps()).isValidEmail(email.getText());

        logger.info("valid " + validEmail + " for " + email.getText());
        add.setEnabled(validEmail);
      }
    });*/
    email.addStyleName("leftFiveMargin");
    widgets.add(email);
    widgets.add(add);
    add.addStyleName("leftFiveMargin");
    return widgets;
  }

  private void doAdd() {
    List<String> strings = getCurrentValues();

    strings.add(email.getValue());

    projectServiceAsync.setListProperty(1, ProjectProperty.REPORT_LIST, strings, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(Boolean result) {

        int id = emails.getSize() + 1;
        emails.addExercise(new MyEmail(id, email.getValue()));
        emails.markCurrentExercise(id);

       // emails.redraw();

        email.setText("");
        addButton.setEnabled(false);
      }
    });

  }

  @NotNull
  private List<String> getCurrentValues() {
    List<String> strings = new ArrayList<>();
    for (int i = 0; i < emails.getNumItems(); i++) {
      strings.add(emails.getItems().get(i).getValue().trim());
    }
    return strings;
  }

  //private Button delete;

  @NotNull
  private Button getRemoveButton() {
    final Button add = new Button("", IconType.MINUS);
    //delete = add;
    // add.setEnabled(false);
    add.addStyleName("leftFiveMargin");
    add.addClickHandler(event -> gotDelete(add));
    add.setType(ButtonType.DANGER);
    addTooltip(add, "Remove recipient.");

    // add.setEnabled(!myLists.isEmpty());
    // myLists.addButton(add);
    return add;
  }

  private void gotDelete(Button delete) {
    if (!emails.isEmpty()) {
      current = emails.getCurrentSelection();
    }
    if (current != null) {
      delete.setEnabled(false);
      // controller.logEvent(delete, "Button", current, "Delete");
      logger.warning("current is  " + current);
      List<String> strings = new ArrayList<>();
      for (int i = 0; i < emails.getNumItems(); i++) {
        MyEmail myEmail = emails.getItems().get(i);
        if (myEmail.getID() != current.getID()) {
          strings.add(myEmail.getValue());
        }
      }
      current = null;

      projectServiceAsync.setListProperty(1, ProjectProperty.REPORT_LIST, strings, new AsyncCallback<Boolean>() {
        @Override
        public void onFailure(Throwable caught) {

        }

        @Override
        public void onSuccess(Boolean result) {
          delete.setEnabled(true);

          emails.forgetItem(emails.getCurrentSelection());
          emails.markCurrentExercise(emails.getSize());
        }
      });

    } else logger.warning("current is null ");
  }

  private void addTooltip(Widget add, String tip) {
    new TooltipHelper().addTooltip(add, tip);
  }


  private Button getCloseButton(final DialogBox dialogBox) {
    final Button closeButton = new Button(CLOSE);
    closeButton.setEnabled(true);
    closeButton.addStyleName("floatRight");
    closeButton.getElement().setId("closeButtonLessTopMargin");
//    eventRegistration.register(closeButton, "N/A", "Close recordings dialog");
    // Add a handler to send the name to the server
    closeButton.addClickHandler(event -> dialogBox.hide());
    closeButton.getElement().getStyle().setMarginLeft(100, Style.Unit.PX);
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

  private MyEmail current = null;

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
