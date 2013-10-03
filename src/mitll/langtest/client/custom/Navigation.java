package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.FormField;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.custom.UserList;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class Navigation extends BasicDialog {
  private final ExerciseController controller;
  LangTestDatabaseAsync service;
  UserManager userManager;
  int userListID;
  public Navigation(LangTestDatabaseAsync service, UserManager userManager, ExerciseController controller) {
    this.service = service;
   this.userManager = userManager;
    this.controller = controller;
  }

  public Widget getNav(final Panel thirdRow) {
    FluidContainer container = new FluidContainer();
    Button yourItems = new Button("Your Lessons", IconType.FOLDER_CLOSE);
    yourItems.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        // ask the server for your lessons
        viewLessons(thirdRow);
      }
    });
    container.add(yourItems);
    Button create = new Button("Create", IconType.PLUS_SIGN);
    create.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        doCreate(thirdRow);
        // TODO : add box to permit adding new entries
        // record audio --- how much feedback?
      }
    });
    container.add(create);
    Button browse = new Button("Browse", IconType.TH_LIST);
    browse.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        // ask the server for all the lists that are known.
        // maybe have a typeahead box to search over lists and items
      }
    });
    container.add(browse);
    Button chapters = new Button("Chapters", IconType.BOOK);
    chapters.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        // show traditional chapter widget
      }
    });
    container.add(chapters);
    return container;
  }

  private void viewLessons(Panel thirdRow) {
    thirdRow.clear();
    final FluidContainer child = new FluidContainer();
    thirdRow.add(child);

    service.getListsForUser(userManager.getUser(),new AsyncCallback<Collection<UserList>>() {
      @Override
      public void onFailure(Throwable caught) {
        //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void onSuccess(Collection<UserList> result) {
        PagingContainer<UserList> userListPagingContainer = new PagingContainer<UserList>(controller);


        FluidRow row = new FluidRow();

        Panel container = userListPagingContainer.addTableWithPager();
        for (ExerciseShell item : result) {
          userListPagingContainer.addExerciseToList2(item);

        }
        userListPagingContainer.flush();

        row.add(container);
        child.add(row);
      }
    });
  }

  private void doCreate(Panel thirdRow) {
    // fill in the middle panel with a form to allow you to create a list
    // post the results to the server
    thirdRow.clear();
    FluidContainer child = new FluidContainer();
    thirdRow.add(child);

    FluidRow row = new FluidRow();
    child.add(row);
    final Heading header = new Heading(2,"Create a New List");
    row.add(header);

    row = new FluidRow();
    child.add(row);
   // final TextBox titleBox = new TextBox();
   // ControlGroup group = addControlGroupEntry(row,"Title", titleBox);
   // row.add(group);

    final FormField titleBox = addControlFormField(row, "Title");

    row = new FluidRow();
    child.add(row);
    final TextArea area = new TextArea();
    /*ControlGroup group =*/ addControlGroupEntry(row,"Description", area);

    row = new FluidRow();
    child.add(row);
   /* final TextBox dliClass = new TextBox();
    group = addControlGroupEntry(row, "Class", dliClass);
    row.add(group);*/

    final FormField classBox = addControlFormField(row, "Class");


    row = new FluidRow();
    child.add(row);
    Button submit = new Button("Create List");
    submit.setType(ButtonType.PRIMARY);
    submit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        System.out.println("creating list for " + titleBox + " " + area.getText() + " and " + classBox.getText());

        // TODO : validate

        service.addUserList(userManager.getUser(), titleBox.getText(), area.getText(), classBox.getText(), new AsyncCallback<Integer>() {
          @Override
          public void onFailure(Throwable caught) {
            //To change body of implemented methods use File | Settings | File Templates.
          }

          @Override
          public void onSuccess(Integer result) {
            userListID = result;
            System.out.println("userListID " + userListID);

            // TODO : show enter item panel
          }
        });
      }
    });
    // group = addControlGroupEntry(row, "Class", dliClass);
    row.add(submit);
  }
}
