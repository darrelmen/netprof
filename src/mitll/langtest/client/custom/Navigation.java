package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.resources.Bootstrap;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.client.dialog.EnterKeyButtonHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ListInterface;
import mitll.langtest.client.exercise.PagingExerciseList;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.scoring.GoodwaveExercisePanelFactory;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.FormField;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
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
  public static final String CHAPTERS = "Chapters";
  private static final boolean SHOW_CREATED = false;
  private final ExerciseController controller;
  LangTestDatabaseAsync service;
  UserManager userManager;
  int userListID;

  ScrollPanel listScrollPanel;
  UserFeedback feedback;
  private PropertyHandler props;
  ListInterface listInterface;

  public Navigation(LangTestDatabaseAsync service, UserManager userManager, ExerciseController controller, ListInterface listInterface) {
    this.service = service;
    this.userManager = userManager;
    this.controller = controller;
    this.listInterface = listInterface;
  }

  /**
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   * @paramx thirdRow
   * @return
   */
  public Widget getNav(//final FluidRow secondRow,
                       //final Panel thirdRow,
                       final Panel secondAndThird,


                       final UserFeedback feedback, final PropertyHandler props) {
    final FluidContainer container = new FluidContainer();

    this.feedback = feedback;
    this.props = props;
    //  final FluidRow contentPanel = new FluidRow();
    //FluidRow buttonRow = getButtonRow(secondRow, thirdRow, contentPanel);
    Panel buttonRow = getButtonRow2(secondAndThird);//secondRow, thirdRow, contentPanel);
    container.add(buttonRow);
    //  container.add(contentPanel);

    return container;
  }
  TabPanel tabPanel;
  Tab yourItems;
  Panel yourItemsContent;
  private Panel getButtonRow2(Panel secondAndThird) {
    tabPanel = new TabPanel(Bootstrap.Tabs.ABOVE);

    final TabAndContent yourStuff = makeTab(tabPanel, IconType.FOLDER_CLOSE, "Your Lists");
    yourItems = yourStuff.tab;
    yourItemsContent = yourStuff.content;
    yourItems.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        refreshViewLessons();
      }
    });
    refreshViewLessons();

    final TabAndContent create = makeTab(tabPanel, IconType.PLUS_SIGN, "Create");
    create.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        doCreate(create.content);
      }
    });

    final TabAndContent browse = makeTab(tabPanel, IconType.TH_LIST, "Browse");
    browse.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        viewLessons(browse.content, true);
      }
    });


    final TabAndContent chapters = makeTab(tabPanel, IconType.TH_LIST, CHAPTERS);
    chapters.content.add(secondAndThird);

    // so we can know when chapters is revealed and tell it to update it's lists
    tabPanel.addShowHandler(new TabPanel.ShowEvent.Handler() {
      @Override
      public void onShow(TabPanel.ShowEvent showEvent) {
       /* System.out.println("got shown event : '" +showEvent + "'\n" +
            "\ntarget " + showEvent.getTarget()+
            " ' target name '" + showEvent.getTarget().getName() + "'");*/
        if (listInterface.getCreatedPanel() != null && showEvent.getTarget().toString().contains(CHAPTERS)) {
          // System.out.println("\n\nupdating lists --->\n\n\n");

          ((GoodwaveExercisePanel) listInterface.getCreatedPanel()).wasRevealed();
        } else {
          // System.out.println("no goodwave panel yet --->\n\n\n");

        }
      }
    });

    return tabPanel;
  }

  private TabAndContent makeTab(TabPanel toAddTo,IconType iconType, String label) {
    Tab create = new Tab();
    create.setIcon(iconType);
    create.setHeading(label);
    toAddTo.add(create.asTabLink());
    final FluidContainer createContent = new FluidContainer();
    create.add(createContent);
    return new TabAndContent(create,createContent);
  }

  private static class TabAndContent {
    public Tab tab;
    public Panel content;
    public TabAndContent(Tab tab, Panel panel) { this.tab = tab; this.content = panel;}
  }

  private void refreshViewLessons() {
    viewLessons(yourItemsContent, false);
  }

  public void showInitialState() {
    System.out.println("show initial state! -->");
    tabPanel.selectTab(0);
    yourItems.fireEvent(new ButtonClickEvent());
    refreshViewLessons();
  }

  @Override
  public void onResize() {
    setScrollPanelWidth(listScrollPanel);
  }

  private class ButtonClickEvent extends ClickEvent {
        /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  }

  private void viewLessons(final Panel contentPanel, boolean getAll) {
    contentPanel.clear();
    contentPanel.getElement().setId("contentPanel");

    final FluidContainer child = new FluidContainer();
    contentPanel.add(child);
    child.addStyleName("exerciseBackground");
    if (getAll) {
      service.getUserListsForText("", new UserListCallback(contentPanel, child));
    } else {
      service.getListsForUser(userManager.getUser(), false, new UserListCallback(contentPanel, child));
    }
  }

  private boolean createdByYou(UserList ul) {
    return ul.getCreator().id == userManager.getUser();
  }

  private void showList(final UserList ul, Panel contentPanel) {
    FluidContainer container = new FluidContainer();
    contentPanel.clear();
    contentPanel.add(container);
    contentPanel.addStyleName("fullWidth2");

    container.getElement().setId("showListContainer");
    container.addStyleName("userListDarkerBlueColor");
    container.addStyleName("fullWidth2");

    FluidRow child = new FluidRow();
    container.add(child);

    FluidRow r1 = new FluidRow();
    child.add(r1);

    r1.add(new Column(3, new Heading(1, ul.getName())));
    Heading itemMarker = new Heading(3, ul.getExercises().size() + " items");
    itemMarker.addStyleName("subtitleForeground");
    //   itemMarker.addStyleName("floatRight");
    r1.add(new Column(3, itemMarker));

    boolean created = createdByYou(ul);
    if (created && SHOW_CREATED) {
      child = new FluidRow();
      container.add(child);
      child.add(new Heading(3,"<b>Created by you.</b>"));
    }
    FluidContainer operations = new FluidContainer();
    operations.addStyleName("userListOperations");
    FluidRow opRow = new FluidRow();
    operations.add(opRow);

    final FluidContainer listContent = new FluidContainer();
    Button w = new Button("Learn Pronunciation", IconType.LIGHTBULB);
    opRow.add(w);
    w.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        doNPF(ul, listContent);
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
    container.add(listContent);
  }

  private void doNPF(UserList ul, FluidContainer listContent) {
    System.out.println("doing npf!!! \n\n\n\n");
    //FlowPanel fp = new FlowPanel();
    // Panel fp = new FluidContainer();
    HorizontalPanel hp = new HorizontalPanel();

    //  FluidRow row = new FluidRow();
    // fp.add(row);

     listContent.add(hp);
    listContent.addStyleName("userListBackground");
    SimplePanel left = new SimplePanel();

    //  Column left = new Column(2);
    // row.add(left);
    //left.addStyleName("floatLeft");
    hp.add(left);
    SimplePanel right = new SimplePanel();
    //Column right = new Column(10);

    //  row.add(right);
    hp.add(right);
    //   right.addStyleName("floatRight");

    PagingExerciseList exerciseList = new PagingExerciseList(right, service, feedback, false, false, controller);/* {
      @Override
      public Panel makeExercisePanel(Exercise result) {
        Panel widgets = super.makeExercisePanel(result);
        goodwaveExercisePanel = (GoodwaveExercisePanel) widgets;
        System.out.println("got goodwave panel : " + goodwaveExercisePanel);
        return widgets;
      }
    };*/
    exerciseList.setFactory(new GoodwaveExercisePanelFactory(service, feedback, controller), userManager, 1);

    // left.addStyleName("inlineStyle");
    left.add(exerciseList.getExerciseListOnLeftSide(props));
    exerciseList.rememberAndLoadFirst(ul.getExercises());
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
    final EnterKeyButtonHelper enterKeyButtonHelper = new EnterKeyButtonHelper(true);
    FluidContainer child = new FluidContainer() {
      @Override
      protected void onUnload() {
        super.onUnload();
        enterKeyButtonHelper.removeKeyHandler();
      }
    };
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
        enterKeyButtonHelper.removeKeyHandler();
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
    enterKeyButtonHelper.addKeyHandler(submit);
    row.add(submit);
  }

  private class UserListCallback implements AsyncCallback<Collection<UserList>> {
    private final Panel contentPanel;
    private final FluidContainer child;

    public UserListCallback(Panel contentPanel, FluidContainer child) {
      this.contentPanel = contentPanel;
      this.child = child;
    }

    @Override
    public void onFailure(Throwable caught) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onSuccess(Collection<UserList> result) {

      System.out.println("Displayig " + result.size() + " items");
      if (result.isEmpty()) System.err.println("\n\nhuh? no results for user");
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
            showList(ul, contentPanel);
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
  }
}
