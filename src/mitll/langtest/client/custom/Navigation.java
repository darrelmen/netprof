package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.EnterKeyButtonHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.FormField;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.custom.UserList;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class Navigation extends BasicDialog implements RequiresResize {
  public static final ButtonType UNCLICKED = ButtonType.INFO;
  private final ExerciseController controller;
  LangTestDatabaseAsync service;
  UserManager userManager;
  int userListID;
  Button yourItems;
  Button lastClicked = null;
  ScrollPanel listScrollPanel;

  public Navigation(LangTestDatabaseAsync service, UserManager userManager, ExerciseController controller) {
    this.service = service;
   this.userManager = userManager;
   this.controller = controller;
  }

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   * @param thirdRow
   * @return
   */
  public Widget getNav(final FluidRow secondRow,// final Panel contentPanel,
                       final Panel thirdRow, final UserFeedback feedback, final PropertyHandler props) {
    final FluidContainer container = new FluidContainer();

    final FluidRow contentPanel = new FluidRow();
    FluidRow buttonRow = getButtonRow(secondRow, thirdRow, contentPanel);
    container.add(buttonRow);
    container.add(contentPanel);

   // container.add(chapters);
    return container;
  }

  private FluidRow getButtonRow(final FluidRow secondRow, final Panel thirdRow,final FluidRow contentPanel) {
    FluidRow buttonRow = new FluidRow();
    yourItems = new Button("Your Lessons", IconType.FOLDER_CLOSE);
    yourItems.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        // ask the server for your lessons
        System.out.println("clicked on your items.");
        hideChapterView(secondRow, thirdRow, contentPanel);

        selectTab(yourItems);
        viewLessons(contentPanel);
      }
    });
    yourItems.setType(UNCLICKED);
    buttonRow.add(yourItems);
    final Button create = new Button("Create", IconType.PLUS_SIGN);
    create.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectTab(create);
        hideChapterView(secondRow, thirdRow, contentPanel);

        doCreate(contentPanel);
        // TODO : add box to permit adding new entries
        // record audio --- how much feedback?
      }
    });
    create.setType(UNCLICKED);
    buttonRow.add(create);

    final Button browse = new Button("Browse", IconType.TH_LIST);
    browse.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectTab(browse);
        hideChapterView(secondRow, thirdRow, contentPanel);

        // ask the server for all the lists that are known.
        // maybe have a typeahead box to search over lists and items
      }
    });
    browse.setType(UNCLICKED);
    buttonRow.add(browse);

    final Button chapters = new Button("Chapters", IconType.BOOK);
    chapters.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        selectTab(chapters);

        // show traditional chapter widget
        showChapterView(secondRow, thirdRow, contentPanel);
      }
    });
    chapters.setType(UNCLICKED);
    buttonRow.add(chapters);
    return buttonRow;
  }

  // TODO : really this should be in tabs...
  private void showChapterView(FluidRow secondRow, Panel thirdRow, FluidRow contentPanel) {
    System.out.println("\n\n\n----> showChapterView");
    secondRow.setVisible(true);
    thirdRow.setVisible(true);
    contentPanel.setVisible(false);
  }

  private void hideChapterView(FluidRow secondRow, Panel thirdRow, FluidRow contentPanel) {
    System.out.println("\n\n\n----> hideChapterView\n\n\n");

    secondRow.setVisible(false);
    thirdRow.setVisible(false);
    contentPanel.setVisible(true);
  }

  private void selectTab(Button clickedOn) {
    clickedOn.setType(ButtonType.PRIMARY);
    if (lastClicked != null) lastClicked.setType(UNCLICKED);
    lastClicked = clickedOn;
  }

  public void showInitialState() {
    System.out.println("\n" +
      "\nshow initial state! --> \n\n\n\n");
    yourItems.fireEvent(new ButtonClickEvent());
  }

  @Override
  public void onResize() {
    setScrollPanelWidth(listScrollPanel);
  }

  private class ButtonClickEvent extends ClickEvent {
        /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  }

  private void viewLessons(final Panel thirdRow) {
    thirdRow.clear();
    thirdRow.getElement().setId("thirdRow");

    final FluidContainer child = new FluidContainer();
    thirdRow.add(child);
    child.addStyleName("exerciseBackground");
    service.getListsForUser(userManager.getUser(), false, new AsyncCallback<Collection<UserList>>() {
      @Override
      public void onFailure(Throwable caught) {
        //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void onSuccess(Collection<UserList> result) {
        listScrollPanel = new ScrollPanel();
        setScrollPanelWidth(listScrollPanel);
        final FluidContainer insideScroll = new FluidContainer();
        insideScroll.addStyleName("userListContainer");
        listScrollPanel.add(insideScroll);
        for (final UserList ul : result) {
          final FocusPanel widgets = new FocusPanel();
          widgets.addStyleName("userListContent");
          widgets.addStyleName("userListBackground");

          widgets.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
            //  Window.alert("Do something on select.");
              showList(ul,thirdRow);
            }
          });
          widgets.addMouseOverHandler(new MouseOverHandler() {
            @Override
            public void onMouseOver(MouseOverEvent event) {
              widgets.removeStyleName("userListBackground");
              widgets.addStyleName("blueBackground");
              widgets.addStyleName("handCursor");
              widgets.getElement().getStyle().setCursor(Style.Cursor.POINTER);
            }
          });
          widgets.addMouseOutHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent event) {
              widgets.removeStyleName("blueBackground");
              widgets.addStyleName("userListBackground");
              widgets.removeStyleName("handCursor");
            }
          });
          insideScroll.add(widgets);
          FluidContainer w = new FluidContainer();
          widgets.add(w);

          FluidRow r1 = new FluidRow();
          w.add(r1);


          //FlowPanel fp = new FlowPanel();
          Heading w1 = new Heading(2, "Title : " + ul.getName());
          //w1.addStyleName("floatLeft");
         // r1.add(fp);
          //fp.add(w1);
          r1.add(new Column(3,w1));
          Heading itemMarker = new Heading(3, ul.getExercises().size() + " items");
          itemMarker.addStyleName("subtitleForeground");
       //   itemMarker.addStyleName("floatRight");
          r1.add(new Column(3,itemMarker));
      //    fp.add(itemMarker);

          r1 = new FluidRow();
          w.add(r1);
          r1.add(new Heading(3,"Description : "+ul.getDescription()));

          r1 = new FluidRow();
          w.add(r1);
          r1.add(new Heading(3,"Class : "+ul.getClassMarker()));

          if (createdByYou(ul)) {
            r1 = new FluidRow();
            w.add(r1);
            r1.add(new HTML("<b>Created by you.</b>"));
          }
        }
        child.add(listScrollPanel);
      }
    });
  }

  private boolean createdByYou(UserList ul) {
    return ul.getCreator().id == userManager.getUser();
  }

  private void showList(UserList ul, Panel thirdRow) {
    FluidContainer container = new FluidContainer();
    thirdRow.clear();
    thirdRow.add(container);
    thirdRow.addStyleName("fullWidth2");

    container.getElement().setId("showListContainer");
    container.addStyleName("userListDarkerBlueColor");
    container.addStyleName("fullWidth2");

    FluidRow child = new FluidRow();
    container.add(child);
    //child.addStyleName("fullWidth2");

    //   child.add(new Heading(1,ul.getName()));

    FluidRow r1 = new FluidRow();
    child.add(r1);

    r1.add(new Column(3, new Heading(1, ul.getName())));
    Heading itemMarker = new Heading(3, ul.getExercises().size() + " items");
    itemMarker.addStyleName("subtitleForeground");
    //   itemMarker.addStyleName("floatRight");
    r1.add(new Column(3, itemMarker));

    boolean created = createdByYou(ul);
    if (created) {
      child = new FluidRow();
      container.add(child);
      child.add(new Heading(3,"<b>Created by you.</b>"));
    }
    FluidContainer operations = new FluidContainer();
    operations.addStyleName("userListOperations");
    FluidRow opRow = new FluidRow();
    operations.add(opRow);
/*
    FocusPanel pair = new FocusPanel();


    FlowPanel rowPair = new FlowPanel();

    rowPair.add(new Image(LangTest.LANGTEST_IMAGES + "NewProF1.png"));
    rowPair.add(new Heading(3,"Learn"));

    operations.add(rowPair);
    pair.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Window.alert("Do npf");
      }
    });
    //opRow.add(new Image(LangTest.LANGTEST_IMAGES + "NewProF1.png"));
    opRow.add(pair);
    opRow.add(new Image(LangTest.LANGTEST_IMAGES + "NewProF2.png"));

*/


    Button w = new Button("Learn Pronunciation", IconType.LIGHTBULB);
    opRow.add(w);
    w.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Window.alert("do npf!");
      }
    });
    Button practice = new Button("Practice", IconType.CHECK);
    opRow.add(practice);
    practice.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        Window.alert("do avp!");
      }
    });
    if (created) {
      Button w1 = new Button("Add Item", IconType.PLUS_SIGN);
      opRow.add(w1);
      w1.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Window.alert("add item to list");
        }
      });
      Button edit = new Button("Edit", IconType.EDIT);
      opRow.add(edit);
      edit.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Window.alert("edit items");
        }
      });
    }
    container.add(operations);
  }

  private void setScrollPanelWidth(ScrollPanel row) {
    if (row != null) {
      row.setWidth((Window.getClientWidth() * 0.9) + "px");
      row.setHeight((Window.getClientHeight() * 0.7) + "px");
    }
  }

  private void doCreate(Panel thirdRow) {
    // fill in the middle panel with a form to allow you to create a list
    // post the results to the server
    thirdRow.clear();
    FluidContainer child = new FluidContainer();
    thirdRow.add(child);
    child.addStyleName("userListContainer");

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
            showInitialState();
            // TODO : show enter item panel
          }
        });
      }
    });
    new EnterKeyButtonHelper(true).addKeyHandler(submit);
    // group = addControlGroupEntry(row, "Class", dliClass);
    row.add(submit);
  }
}
