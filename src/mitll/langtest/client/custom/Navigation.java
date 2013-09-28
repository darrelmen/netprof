package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.ControlLabel;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserManager;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class Navigation {
  LangTestDatabaseAsync service;
  UserManager userManager;
  int userListID;
  public Navigation(LangTestDatabaseAsync service, UserManager userManager) {
    this.service = service;
   this.userManager = userManager;
  }

  public Widget getNav(final Panel thirdRow) {
    FluidContainer container = new FluidContainer();
    Button yourItems = new Button("Your Lessons", IconType.FOLDER_CLOSE);
    yourItems.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        // ask the server for your lessons
      }
    });
    container.add(yourItems);
    Button create = new Button("Create", IconType.PLUS_SIGN);
    create.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        // fill in the middle panel with a form to allow you to create a list
        // post the results to the server
        thirdRow.clear();
        FluidContainer child = new FluidContainer();
        thirdRow.add(child);

        FluidRow row = new FluidRow();
        child.add(row);
        final TextBox titleBox = new TextBox();
        ControlGroup group = addControlGroupEntry(row,"Title", titleBox);
        row.add(group);

        row = new FluidRow();
        child.add(row);
        final TextArea area = new TextArea();
        group = addControlGroupEntry(row,"Description", area);
        row.add(group);

        row = new FluidRow();
        child.add(row);
        final TextBox dliClass = new TextBox();
        group = addControlGroupEntry(row, "Class", dliClass);
        row.add(group);

        row = new FluidRow();
        child.add(row);
        Button submit = new Button("Create List");
        submit.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            service.addUserList(userManager.getUser(), titleBox.getText(),area.getText(),dliClass.getText(), new AsyncCallback<Integer>() {
              @Override
              public void onFailure(Throwable caught) {
                //To change body of implemented methods use File | Settings | File Templates.
              }

              @Override
              public void onSuccess(Integer result) {
                userListID = result;
              }
            });
          }
        });
        // group = addControlGroupEntry(row, "Class", dliClass);
        row.add(submit);

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

  private ControlGroup addControlGroupEntry(Panel dialogBox, String label, Widget widget) {
    final ControlGroup userGroup = new ControlGroup();
    userGroup.addStyleName("leftFiveMargin");
    userGroup.add(new ControlLabel(label));
    userGroup.add(widget);

    dialogBox.add(userGroup);
    return userGroup;
  }
}
