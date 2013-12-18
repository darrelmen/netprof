package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.custom.UserList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class Navigation implements RequiresResize {
  private static final String CHAPTERS = "Chapters";
  private static final boolean SHOW_CREATED = false;
  private static final String YOUR_LISTS = "Your Lists";
  public static final String PRACTICE = "Practice";
  private final ExerciseController controller;
  private LangTestDatabaseAsync service;
  private UserManager userManager;
  private long userListID;

  private ScrollPanel listScrollPanel;
  private UserFeedback feedback;
  private ListInterface listInterface;
  private NPFHelper npfHelper;
  private NPFHelper avpHelper;
  private EditItem editItem;

  public Navigation(final LangTestDatabaseAsync service, final UserManager userManager,
                    final ExerciseController controller, final ListInterface listInterface) {
    this.service = service;
    this.userManager = userManager;
    this.controller = controller;
    this.listInterface = listInterface;
    npfHelper = new NPFHelper(service, feedback, userManager, controller);
    avpHelper = new AVPHelper(feedback,service, userManager, controller);
    editItem = new EditItem(service,userManager,controller);
  }

  /**
   * @return
   * @paramx thirdRow
   * @see mitll.langtest.client.LangTest#onModuleLoad2
   */
  public Widget getNav(final Panel secondAndThird, final UserFeedback feedback) {
    FluidContainer container = new FluidContainer();
    this.feedback = feedback;
    Panel buttonRow = getButtonRow2(secondAndThird);
    container.add(buttonRow);
    this.container = container;
    return container;
  }

  public Widget getContainer() { return container; }

  private Widget container;
  private TabPanel tabPanel;
  private Tab yourItems;
  private Panel yourItemsContent;
  private TabAndContent browse;
  private TabAndContent review;

  private Panel getButtonRow2(Panel secondAndThird) {
    tabPanel = new TabPanel();

    // your list tab
    final TabAndContent yourStuff = makeTab(tabPanel, IconType.FOLDER_CLOSE, YOUR_LISTS);
    yourItems = yourStuff.tab;
    yourItemsContent = yourStuff.content;
    yourItems.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        refreshViewLessons();
      }
    });
    refreshViewLessons();

    // create tab
    final TabAndContent create = makeTab(tabPanel, IconType.PLUS_SIGN, "Create");
    final CreateListDialog createListDialog = new CreateListDialog(this,service,userManager);
    create.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        createListDialog.doCreate(create.content);
      }
    });

    // browse tab
    browse = makeTab(tabPanel, IconType.TH_LIST, "Browse");
    browse.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        viewBrowse();
      }
    });

    // chapter tab
    final TabAndContent chapters = makeTab(tabPanel, IconType.TH_LIST, CHAPTERS);
    chapters.content.add(secondAndThird);

    if (controller.isReviewMode()) {
      System.out.println("\n\n\nadding review tab");

      review = makeTab(tabPanel, IconType.EDIT, "Review");
      review.tab.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          viewReview(review.content);
        }
      });
    }
    else {
      System.out.println("\n" +
        "\n" +
        "\nnot adding review tab");
    }

    // so we can know when chapters is revealed and tell it to update it's lists
    tabPanel.addShowHandler(new TabPanel.ShowEvent.Handler() {
      @Override
      public void onShow(TabPanel.ShowEvent showEvent) {
       /* System.out.println("got shown event : '" +showEvent + "'\n" +
            "\ntarget " + showEvent.getTarget()+
            " ' target name '" + showEvent.getTarget().getName() + "'");*/
        String targetName = showEvent.getTarget() == null ? "" : showEvent.getTarget().toString();

        System.out.println("got shown event : '" +showEvent + "' target '" + targetName + "'");

        boolean wasChapters = targetName.contains(CHAPTERS);
        Panel createdPanel = listInterface.getCreatedPanel();
        boolean hasCreated = createdPanel != null;
        if (hasCreated && wasChapters) {
          System.out.println("\tgot chapters! created panel :  has created " + hasCreated + " was revealed  " + createdPanel.getClass());
          ((NPFExercise) createdPanel).wasRevealed();
        }
      }
    });

    return tabPanel;
  }

  public void checkMode() {
  }

  private TabAndContent makeTab(TabPanel toAddTo, IconType iconType, String label) {
    Tab create = new Tab();
    create.setIcon(iconType);
    create.setHeading(label);
    toAddTo.add(create.asTabLink());
    final FluidContainer createContent = new FluidContainer();
    create.add(createContent);
    zeroPadding(createContent);
    return new TabAndContent(create, createContent);
  }

  void zeroPadding(Panel createContent) {
    DOM.setStyleAttribute(createContent.getElement(), "paddingLeft", "0px");
    DOM.setStyleAttribute(createContent.getElement(), "paddingRight", "0px");
  }

  public long getUserListID() {
    return userListID;
  }

  public void setUserListID(long userListID) {
    this.userListID = userListID;
  }

  public static class TabAndContent {
    public Tab tab;
    public FluidContainer content;

    public TabAndContent(Tab tab, FluidContainer panel) {
      this.tab = tab;
      this.content = panel;
    }
  }

  /**
   * @see #getButtonRow2(com.google.gwt.user.client.ui.Panel)
   * @see #showMyLists()
   */
  private void refreshViewLessons() { viewLessons(yourItemsContent, false);  }

  public void showInitialState() {
    System.out.println("showInitialState show initial state for " + userManager.getUser() +
      " : getting user lists " + controller.isReviewMode());
    checkMode();
    service.getListsForUser(userManager.getUser(), false, true, new AsyncCallback<Collection<UserList>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Collection<UserList> result) {
        if (result.size() == 1 && result.iterator().next().getExercises().isEmpty()) {
          service.getUserListsForText("", new AsyncCallback<Collection<UserList>>() {
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Collection<UserList> result) {
              if (!result.isEmpty()) {
                // show site-wide browse list instead
                showBrowse();
              } else { // otherwise show the chapters tab
                tabPanel.selectTab(3);
              }
            }
          });
        } else {
          System.out.println("\tshowInitialState show initial state for " + userManager.getUser() + " found " + result.size() + " lists");

          showMyLists();
        }
      }
    });
  }

  private void showMyLists() {
    tabPanel.selectTab(0);
    yourItems.fireEvent(new ButtonClickEvent());
    refreshViewLessons();
  }

  private void showBrowse() {
    tabPanel.selectTab(2);
    browse.tab.fireEvent(new ButtonClickEvent());
    viewBrowse();
  }

  private void viewBrowse() {
    viewLessons(browse.content, true);
  }

  /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  private void viewLessons(final Panel contentPanel, boolean getAll) {
    contentPanel.clear();
    contentPanel.getElement().setId("contentPanel");

    final Panel child = new DivWidget();
    contentPanel.add(child);
    child.addStyleName("exerciseBackground");
    listScrollPanel = new ScrollPanel();

    if (getAll) {
      System.out.println("viewLessons----> getAll = " + getAll);
      service.getUserListsForText("", new UserListCallback(contentPanel, child,listScrollPanel, "lessons"));
    } else {
      System.out.println("viewLessons for " + userManager.getUser());
      service.getListsForUser(userManager.getUser(), false, true, new UserListCallback(contentPanel, child,listScrollPanel, "lessons"));
    }
  }

  private void viewReview(final Panel contentPanel) {
    contentPanel.clear();
    contentPanel.getElement().setId("viewReview_contentPanel");

    final Panel child = new DivWidget();
    contentPanel.add(child);
    child.addStyleName("exerciseBackground");
    System.out.println("\n\nreviewLessons for " + userManager.getUser());
    service.getReviewList(new AsyncCallback<UserList>() {
      @Override
      public void onFailure(Throwable caught) {
        //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void onSuccess(UserList result) {
        new UserListCallback(contentPanel,child,new ScrollPanel(), "review").onSuccess(Collections.singleton(result));
      }
    });
  }

  @Override
  public void onResize() {
    setScrollPanelWidth(listScrollPanel);
    npfHelper.onResize();
    avpHelper.onResize();
  }

  private class ButtonClickEvent extends ClickEvent {

  }

  private boolean createdByYou(UserList ul) {
    return ul.getCreator().id == userManager.getUser();
  }

  /**
   * @see Navigation.UserListCallback#getDisplayRowPerList(mitll.langtest.shared.custom.UserList)
   * @param ul
   * @param contentPanel
   */
  private void showList(final UserList ul, Panel contentPanel, final String instanceName) {

    System.out.println("showList " +ul);

    FluidContainer container = new FluidContainer();
    contentPanel.clear();
    contentPanel.add(container);
    container.getElement().setId("showListContainer");
    container.addStyleName("fullWidth2");
    DOM.setStyleAttribute(container.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(container.getElement(), "paddingRight", "2px");

    FluidRow child = new FluidRow();
    container.add(child);

    FluidRow r1 = new FluidRow();
    child.add(r1);
    child.addStyleName("userListDarkerBlueColor");

    r1.add(new Column(3, new Heading(1, ul.getName())));
    HTML itemMarker = new HTML(ul.getExercises().size() + " items");
    listToMarker.put(ul,itemMarker);
    itemMarker.addStyleName("subtitleForeground");
    r1.add(new Column(3, itemMarker));

    boolean created = createdByYou(ul) || instanceName.equals("review");
    if (created && SHOW_CREATED) {
      child = new FluidRow();
      container.add(child);
      child.add(new Heading(3, "<b>Created by you.</b>"));
    }
    final FluidContainer listContent = new FluidContainer();

    Panel operations = getListOperations(ul, created, instanceName);
    container.add(operations);
    container.add(listContent);

    service.addVisitor(ul, (long)controller.getUser(), new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {}
      @Override
      public void onSuccess(Void result) {}
    });
  }

  private Panel getListOperations(final UserList ul, final boolean created, final String instanceName) {
    final TabPanel tabPanel = new TabPanel();
    System.out.println("getListOperations : '" + instanceName);
    final TabAndContent learn = makeTab(tabPanel, IconType.LIGHTBULB, "Learn Pronunciation");
    learn.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        System.out.println("\t\tgetListOperations : '" + instanceName +"'");

        npfHelper.showNPF(ul, learn, instanceName.equals("review") ? "review" : "learn");
      }
    });

    final TabAndContent practice = makeTab(tabPanel, IconType.CHECK, PRACTICE);
    practice.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        avpHelper.showNPF(ul, practice, "practice");
      }
    });

    TabAndContent addItem = null;
    if (created && !ul.isPrivate()) {
      if (!instanceName.equals("review")) {
        addItem = makeTab(tabPanel, IconType.PLUS_SIGN, "Add Item");
        final TabAndContent finalAddItem = addItem;
        addItem.tab.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            showAddItem(ul, finalAddItem);
          }
        });
      }

      final TabAndContent edit = makeTab(tabPanel, IconType.EDIT, "Edit");
      edit.tab.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          showEditItem(ul, edit);
        }
      });
    }

    final TabAndContent finalAddItem = addItem;
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        if (created && !ul.isPrivate() && ul.isEmpty() && finalAddItem != null) {
          tabPanel.selectTab(2);
          showAddItem(ul, finalAddItem);
        } else {
          tabPanel.selectTab(0);
          npfHelper.showNPF(ul, learn, instanceName.equals("review") ? "review" : "learn");
        }
      }
    });

    return tabPanel;
  }

  private void showAddItem(UserList ul, TabAndContent addItem) {
    addItem.content.clear();
    Panel widgets = addItem(ul);
    addItem.content.add(widgets);
  }

  private void showEditItem(UserList ul, TabAndContent addItem) {
    addItem.content.clear();
    Panel widgets = editItem.editItem(ul, listToMarker.get(ul));
    addItem.content.add(widgets);
  }

  /**
   * @param ul
   * @return
   * @see #showAddItem(mitll.langtest.shared.custom.UserList, mitll.langtest.client.custom.Navigation.TabAndContent)
   */
  private Panel addItem(UserList ul) {
    HorizontalPanel hp = new HorizontalPanel();
    SimplePanel left = new SimplePanel();
    hp.add(left);
    SimplePanel right = new SimplePanel();
    hp.add(right);

    PagingContainer<ExerciseShell> pagingContainer = new PagingContainer<ExerciseShell>(controller, 100); // todo fix hack
    Panel container = pagingContainer.getTableWithPager();
    left.add(container);
    for (ExerciseShell es : ul.getExercises()) {
      pagingContainer.addExerciseToList2(es);
    }
    pagingContainer.flush();
    right.add(new NewUserExercise(service,userManager,controller, listToMarker.get(ul)).addNew(ul, pagingContainer, right));
    return hp;
  }

  private void setScrollPanelWidth(ScrollPanel row) {
    if (row != null) {
      //row.setWidth((Window.getClientWidth() * 0.95) + "px");
      row.setHeight((Window.getClientHeight() * 0.7) + "px");
    }
  }

  private class UserListCallback implements AsyncCallback<Collection<UserList>> {
    private final Panel contentPanel;
    private final Panel child;
    private final ScrollPanel listScrollPanel;
    String instanceName;

    public UserListCallback(Panel contentPanel, Panel child, ScrollPanel listScrollPanel, String instanceName) {
      this.contentPanel = contentPanel;
      this.child = child;
      this.listScrollPanel = listScrollPanel;
      this.instanceName = instanceName;
    }

    @Override
    public void onFailure(Throwable caught) {
    }

    @Override
    public void onSuccess(Collection<UserList> result) {
      System.out.println("\tUserListCallback : Displaying " + result.size() + " user lists for " + instanceName);
      // if (result.isEmpty()) System.err.println("\n\nhuh? no results for user");
      if (result.isEmpty()) {
        child.add(new Heading(3, "No lists created yet."));
      } else {
        listScrollPanel.getElement().setId("scrollPanel");

        setScrollPanelWidth(listScrollPanel);
        final Panel insideScroll = new DivWidget();
        insideScroll.getElement().setId("insideScroll");
        insideScroll.addStyleName("userListContainer");

        listScrollPanel.add(insideScroll);
        boolean anyAdded = false;
        for (final UserList ul : result) {
          if (!ul.isEmpty() || !ul.isPrivate()) {
            anyAdded = true;
            insideScroll.add(getDisplayRowPerList(ul));
          }
        }
        if (!anyAdded) {
          insideScroll.add(new Heading(3, "No lists created or visited yet."));
        }
        child.add(listScrollPanel);
      }
    }

    private void setScrollPanelWidth(ScrollPanel row) {
      if (row != null) {
        row.setHeight((Window.getClientHeight() * 0.7) + "px");
      }
    }

    private Panel getDisplayRowPerList(final UserList ul) {
      final FocusPanel widgets = new FocusPanel();

      widgets.addStyleName("userListContent");
      widgets.addStyleName("userListBackground");
      widgets.addStyleName("leftTenMargin");
      widgets.addStyleName("rightTenMargin");

      widgets.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          showList(ul, contentPanel, instanceName);
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
      FluidContainer w = new FluidContainer();
      widgets.add(w);

      addWidgetsForList(ul, w);
      return widgets;
    }
  }

  Map<UserList, HTML> listToMarker = new HashMap<UserList, HTML>();

  private void addWidgetsForList(UserList ul, FluidContainer w) {
    FluidRow r1 = new FluidRow();
    w.add(r1);


    //FlowPanel fp = new FlowPanel();
    //  Heading nameInfo = new Heading(2, ul.getName());
    Widget nameInfo = getUserListText(ul.getName());
    //nameInfo.addStyleName("floatLeft");
    // r1.add(fp);
    //fp.add(nameInfo);
    r1.add(new Column(6, nameInfo));
    //  itemMarker = new Heading(3, ul.getExercises().size() + " items");
    HTML itemMarker = new HTML(ul.getExercises().size() + " items");
    itemMarker.addStyleName("numItemFont");
    listToMarker.put(ul,itemMarker);
    //   itemMarker.addStyleName("floatRight");
    r1.add(new Column(3, itemMarker));
    //    fp.add(itemMarker);
/*
        r1 = new FluidRow();
        w.add(r1);
        r1.add(new Heading(3, "Description : " + ul.getDescription()));
        */
    r1 = new FluidRow();
    w.add(r1);
    r1.add(getUserListText2(ul.getDescription()));
  /*      r1 = new FluidRow();
        w.add(r1);
        r1.add(new Heading(3, "Class : " + ul.getClassMarker()));
*/
    if (!ul.getClassMarker().isEmpty()) {
      r1 = new FluidRow();
      w.add(r1);
      r1.add(getUserListText2(ul.getClassMarker()));
    }

    if (createdByYou(ul) && !ul.getName().equals(UserListManager.MY_LIST)) {
      r1 = new FluidRow();
      w.add(r1);
      r1.add(new HTML("<b>Created by you.</b>"));
    }
  }

  private Widget getUserListText(String content) {
    Widget nameInfo = new HTML(content);
    nameInfo.addStyleName("userListFont");
    return nameInfo;
  }

  private Widget getUserListText2(String content) {
    Widget nameInfo = new HTML(content);
    nameInfo.addStyleName("userListFont2");
    return nameInfo;
  }
}
