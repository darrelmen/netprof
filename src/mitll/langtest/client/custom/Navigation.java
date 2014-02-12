package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.InlineLabel;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 8:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class Navigation extends TabContainer implements RequiresResize {
  private static final String CHAPTERS = "Chapters";
  private static final String YOUR_LISTS = "Your Lists";
  public static final String REVIEW = "review";
  public static final String COMMENT = "comment";
  private static final String ADD__OR_EDIT_ITEM = "Add/Edit Item";
  public static final String ITEMS_TO_REVIEW = "Items to review";
  public static final String ITEMS_WITH_COMMENTS = "Items with comments";
  private final ExerciseController controller;
  private LangTestDatabaseAsync service;
  private UserManager userManager;

  private ScrollPanel listScrollPanel;
  private ListInterface<? extends ExerciseShell> listInterface;
  private NPFHelper npfHelper;
  private NPFHelper avpHelper;
  private EditItem<? extends ExerciseShell> editItem;
  private EditItem<? extends ExerciseShell> reviewItem;

  public Navigation(final LangTestDatabaseAsync service, final UserManager userManager,
                    final ExerciseController controller, final ListInterface<? extends ExerciseShell> listInterface,
                    UserFeedback feedback) {
    this.service = service;
    this.userManager = userManager;
    this.controller = controller;
    this.listInterface = listInterface;
    npfHelper = new NPFHelper(service, feedback, userManager, controller);
    avpHelper = new AVPHelper(service, feedback, userManager, controller);
    editItem = new EditItem<UserExercise>(service, userManager, controller, listInterface, feedback, npfHelper);
    reviewItem = new ReviewEditItem<UserExercise>(service, userManager, controller, listInterface, feedback, npfHelper);
  }

  //private TabPanel tabPanel;
  private Tab yourItems;
  private Panel yourItemsContent;
  private TabAndContent browse;
  private TabAndContent review, commented;

  /**
   * @see #getNav(com.google.gwt.user.client.ui.Panel)
   * @param secondAndThird
   * @return
   */
  protected /*<T extends ExerciseShell>*/ Panel getButtonRow2(Panel secondAndThird) {
    tabPanel = new TabPanel();

    boolean combinedMode = controller.getProps().isCombinedMode();

    // your list tab
    if (!combinedMode) {
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
      final CreateListDialog createListDialog = new CreateListDialog(this, service, userManager);
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
    }

    // chapter tab
    final TabAndContent chapters = makeTab(tabPanel, combinedMode ? IconType.LIGHTBULB : IconType.TH_LIST, !combinedMode ? CHAPTERS : LEARN_PRONUNCIATION);
    chapters.content.add(secondAndThird);

    if (controller.isReviewMode()) {
      review = makeTab(tabPanel, IconType.EDIT, "Review");
      review.tab.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          viewReview(review.content);
        }
      });

      commented = makeTab(tabPanel, IconType.COMMENT, "Comments");
      commented.tab.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          viewComments(commented.content);
        }
      });
    }

    // so we can know when chapters is revealed and tell it to update it's lists
    tabPanel.addShowHandler(new TabPanel.ShowEvent.Handler() {
      @Override
      public void onShow(TabPanel.ShowEvent showEvent) {
       /* System.out.println("got shown event : '" +showEvent + "'\n" +
            "\ntarget " + showEvent.getTarget()+
            " ' target name '" + showEvent.getTarget().getName() + "'");*/
        String targetName = showEvent.getTarget() == null ? "" : showEvent.getTarget().toString();

        //System.out.println("getButtonRow2 : got shown event : '" +showEvent + "' target '" + targetName + "'");

        boolean wasChapters = targetName.contains(CHAPTERS);
        Panel createdPanel = listInterface.getCreatedPanel();
        boolean hasCreated = createdPanel != null;
        if (hasCreated && wasChapters) {
          System.out.println("\tgot chapters! created panel :  has created " + hasCreated + " was revealed  " + createdPanel.getClass());
          ((GoodwaveExercisePanel) createdPanel).wasRevealed();
        }
      }
    });

    return tabPanel;    // TODO - consider how to tell panels when they are hidden by tab changes
  }

  /**
   * @see #getButtonRow2(com.google.gwt.user.client.ui.Panel)
   * @see #showMyLists()
   */
  private void refreshViewLessons() { viewLessons(yourItemsContent, false);  }

  @Override
  public void showInitialState() {
/*    System.out.println("showInitialState show initial state for " + userManager.getUser() +
      " : getting user lists " + controller.isReviewMode());*/

    service.getListsForUser(userManager.getUser(), false, true, new AsyncCallback<Collection<UserList>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Collection<UserList> result) {
        if (result.size() == 1 && result.iterator().next().getExercises().isEmpty()) {
          service.getUserListsForText("", new AsyncCallback<Collection<UserList>>() {
            @Override
            public void onFailure(Throwable caught) {}

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
          //System.out.println("\tshowInitialState show initial state for " + userManager.getUser() + " found " + result.size() + " lists");

          showMyLists();
        }
      }
    });
  }

  private void showMyLists() {
    int tabToSelect =/* controller.isReviewMode() ? 4:*/ 0;
    tabPanel.selectTab(tabToSelect);
    yourItems.fireEvent(new ButtonClickEvent());
    refreshViewLessons();
  }

  private void showBrowse() {
    tabPanel.selectTab(2);
    browse.tab.fireEvent(new ButtonClickEvent());
    viewBrowse();
  }

  /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  private class ButtonClickEvent extends ClickEvent {}

  private void viewBrowse() { viewLessons(browse.content, true); }

  private void viewLessons(final Panel contentPanel, boolean getAll) {
    contentPanel.clear();
    contentPanel.getElement().setId("contentPanel");

    final Panel child = new DivWidget();
    contentPanel.add(child);
    child.addStyleName("exerciseBackground");
    listScrollPanel = new ScrollPanel();

    if (getAll) {
     // System.out.println("viewLessons----> getAll = " + getAll);
      service.getUserListsForText("", new UserListCallback(contentPanel, child,listScrollPanel, "lessons"));
    } else {
     // System.out.println("viewLessons for " + userManager.getUser());
      service.getListsForUser(userManager.getUser(), false, true, new UserListCallback(contentPanel, child,listScrollPanel, "lessons"));
    }
  }

  /**
   * @see #getButtonRow2(com.google.gwt.user.client.ui.Panel)
   * @param contentPanel
   */
  private void viewReview(final Panel contentPanel) {
    contentPanel.clear();
    contentPanel.getElement().setId("viewReview_contentPanel");

    final Panel child = new DivWidget();
    contentPanel.add(child);
    child.addStyleName("exerciseBackground");
     System.out.println("\tviewReview : reviewLessons for " + userManager.getUser());
    service.getReviewList(new AsyncCallback<UserList>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(UserList result) {
        System.out.println("\tviewReview : reviewLessons for " + userManager.getUser() + " got " + result);

        new UserListCallback(contentPanel, child, new ScrollPanel(), REVIEW).onSuccess(Collections.singleton(result));
      }
    });
  }

  private void viewComments(final Panel contentPanel) {
    contentPanel.clear();
    contentPanel.getElement().setId("commentReview_contentPanel");

    final Panel child = new DivWidget();
    contentPanel.add(child);
    child.addStyleName("exerciseBackground");
    service.getCommentedList(new AsyncCallback<UserList>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(UserList result) {
        System.out.println("\tviewComments : commented for " + userManager.getUser() + " got " + result);

        new UserListCallback(contentPanel, child, new ScrollPanel(), COMMENT).onSuccess(Collections.singleton(result));
      }
    });
  }

  @Override
  public void onResize() {
    setScrollPanelWidth(listScrollPanel);
    npfHelper.onResize();
    avpHelper.onResize();
  }

  private boolean createdByYou(UserList ul) {
    return ul.getCreator().id == userManager.getUser();
  }

  /**
   * @see Navigation.UserListCallback#getDisplayRowPerList(mitll.langtest.shared.custom.UserList, boolean)
   * @param ul
   * @param contentPanel
   */
  private TabPanel showList(final UserList ul, Panel contentPanel, final String instanceName) {
    System.out.println("showList " +ul + " instance " + instanceName);

    FluidContainer container = new FluidContainer();
    contentPanel.clear();
    contentPanel.add(container);
    container.getElement().setId("showListContainer");
    DOM.setStyleAttribute(container.getElement(), "paddingLeft", "2px");
    DOM.setStyleAttribute(container.getElement(), "paddingRight", "2px");

    FluidRow child = new FluidRow();
    container.add(child);

    FluidRow r1 = new FluidRow();
    child.add(r1);
    child.addStyleName("userListDarkerBlueColor");

    String subtext = ul.getDescription() + " " + ul.getClassMarker();
    Heading widgets = new Heading(1, ul.getName(), subtext);    // TODO : better color for subtext h1->small

    r1.add(widgets);
    widgets.addStyleName("floatLeft");
    widgets.addStyleName("leftFiveMargin");
    HTML itemMarker = new HTML(ul.getExercises().size() + " items");
    listToMarker.put(ul,itemMarker);

    boolean created = createdByYou(ul) || instanceName.equals(REVIEW) || instanceName.equals(COMMENT);

    TabPanel listOperations = getListOperations(ul, created, instanceName);
    container.add(listOperations);

    addVisitor(ul);

    return listOperations;
  }

  private void addVisitor(UserList ul) {
    service.addVisitor(ul, (long)controller.getUser(), new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {}
      @Override
      public void onSuccess(Void result) {}
    });
  }

  /**
   * @see #showList(mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.Panel, String)
   * @param ul
   * @param created
   * @param instanceName
   * @return
   */
  private TabPanel getListOperations(final UserList ul, final boolean created, final String instanceName) {
    final TabPanel tabPanel = new TabPanel();
    System.out.println("getListOperations : '" + instanceName + " for list " +ul);
    final boolean isReview = instanceName.equals(REVIEW);
    final boolean isComment = instanceName.equals(COMMENT);
    final String instanceName1 = isReview ? REVIEW : isComment ? COMMENT : "learn";

    // add learn tab
    String learnTitle = isReview ? ITEMS_TO_REVIEW : isComment ? ITEMS_WITH_COMMENTS : LEARN_PRONUNCIATION;
    final TabAndContent learn = makeTab(tabPanel, isReview ? IconType.EDIT_SIGN : IconType.LIGHTBULB, learnTitle);
    learn.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        npfHelper.showNPF(ul, learn, instanceName1);
      }
    });

    // add practice tab
    final boolean isNormalList = !isReview && !isComment;
    if (isNormalList) {
      final TabAndContent practice = makeTab(tabPanel, IconType.CHECK, PRACTICE);
      practice.tab.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          avpHelper.showNPF(ul, practice, "practice");
        }
      });
    }
    // add add item and edit tabs (conditionally)
    TabAndContent editItem = null;
    if (created && !ul.isPrivate()) {
      final TabAndContent edit = makeTab(tabPanel, IconType.EDIT, ADD__OR_EDIT_ITEM);
      editItem = edit;
      edit.tab.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          showEditItem(ul, edit, (isReview || isComment) ? reviewItem : Navigation.this.editItem, isNormalList);
        }
      });
    }

    // select the initial tab -- either add if an empty
    final TabAndContent finalEditItem = editItem;
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        if (created && !ul.isPrivate() && ul.isEmpty() && finalEditItem != null) {
          tabPanel.selectTab(2);    // 2 = add/edit item
          showEditItem(ul, finalEditItem, (isReview || isComment) ? reviewItem : Navigation.this.editItem, isNormalList);
        } else {
          tabPanel.selectTab(0);
          npfHelper.showNPF(ul, learn, instanceName1);
        }
      }
    });

    return tabPanel;
  }

  /**
   * @see #getListOperations(mitll.langtest.shared.custom.UserList, boolean, String)
   * @param ul
   * @param addItem
   */
  private void showEditItem(UserList ul, TabAndContent addItem, EditItem<? extends ExerciseShell> editItem, boolean includeAddItem) {
    System.out.println("\n\n\nshowEditItem --- " + ul);
    addItem.content.clear();
    Widget widgets = editItem.editItem(ul, listToMarker.get(ul), includeAddItem);
    addItem.content.add(widgets);
  }

  private void setScrollPanelWidth(ScrollPanel row) {
    if (row != null) {
      //row.setWidth((Window.getClientWidth() * 0.95) + "px");
      row.setHeight((Window.getClientHeight() * 0.7) + "px");
    }
  }

  TabPanel innerTabs;
  private class UserListCallback implements AsyncCallback<Collection<UserList>> {
    private final Panel contentPanel;
    private final Panel child;
    private final ScrollPanel listScrollPanel;
    private String instanceName;

    /**
     * @see #viewComments(com.google.gwt.user.client.ui.Panel)
     * @see #viewLessons(com.google.gwt.user.client.ui.Panel, boolean)
     * @see #viewReview(com.google.gwt.user.client.ui.Panel)
     * @param contentPanel
     * @param child
     * @param listScrollPanel
     * @param instanceName
     */
    public UserListCallback(Panel contentPanel, Panel child, ScrollPanel listScrollPanel, String instanceName) {
      this.contentPanel = contentPanel;
      this.child = child;
      this.listScrollPanel = listScrollPanel;
      this.instanceName = instanceName;
    }

    @Override
    public void onFailure(Throwable caught) {}

    @Override
    public void onSuccess(Collection<UserList> result) {
      System.out.println("\tUserListCallback : Displaying " + result.size() + " user lists for " + instanceName);
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
        Map<String,List<UserList>> nameToLists = new HashMap<String, List<UserList>>();

        for (final UserList ul : result) {
          List<UserList> userLists = nameToLists.get(ul.getName());
          if (userLists == null) nameToLists.put(ul.getName(), userLists = new ArrayList<UserList>());
          userLists.add(ul);
        }
        for (final UserList ul : result) {
          List<UserList> collisions = nameToLists.get(ul.getName());
          boolean showMore = false;
          if (collisions.size() > 1) {
            if (collisions.indexOf(ul) > 0) showMore = true;
          }
          if (!ul.isEmpty() || (!ul.isPrivate()/* && !ul.isEmpty()*/) ) {
            anyAdded = true;
            insideScroll.add(getDisplayRowPerList(ul, showMore));
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

    private Panel getDisplayRowPerList(final UserList ul, boolean showMore) {
      final FocusPanel widgets = new FocusPanel();

      widgets.addStyleName("userListContent");
      widgets.addStyleName("userListBackground");
      widgets.addStyleName("leftTenMargin");
      widgets.addStyleName("rightTenMargin");

      widgets.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          innerTabs = showList(ul, contentPanel, instanceName);
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

      addWidgetsForList(ul, showMore, w);
      return widgets;
    }
  }

  private Map<UserList, HasText> listToMarker = new HashMap<UserList, HasText>();

  /**
   * @see mitll.langtest.client.custom.Navigation.UserListCallback#getDisplayRowPerList(mitll.langtest.shared.custom.UserList, boolean)
   * @param ul
   * @param showMore
   * @param container
   */
  private void addWidgetsForList(final UserList ul, boolean showMore, final FluidContainer container) {
    Panel r1 = new FlowPanel();
    r1.addStyleName("trueInlineStyle");
    String name = ul.getName();
    Widget child = makeItemMarker2(ul);
    child.addStyleName("leftFiveMargin");

    Heading h4 = new Heading(4,name,ul.getExercises().size() + " items");
    h4.addStyleName("floatLeft");
  //  if (!ul.isFavorite()) h4.addStyleName("niceBlue");
  //  if (yourList) h4.addStyleName("niceBlue");

    r1.add(h4);

    h4 = new Heading(4,"",ul.getDescription()+",");
    h4.addStyleName("floatLeft");
    h4.addStyleName("leftFiveMargin");
    h4.getElement().setId("desc");
    //h4.addStyleName("listSubtitle");

    //Widget widget = h4.getWidget(0);
   // System.out.println("small is " + widget);
   // DOM.setStyleAttribute(widget.getElement(), "color", "#333333");

    //  if (!ul.isFavorite()) h4.addStyleName("niceBlue");
    //  if (yourList) h4.addStyleName("niceBlue");

    r1.add(h4);
    h4 = new Heading(4,"",ul.getClassMarker());
    h4.addStyleName("floatLeft");
    h4.addStyleName("leftFiveMargin");
    h4.getElement().setId("course");

    //h4.addStyleName("listSubtitle");

    //  if (!ul.isFavorite()) h4.addStyleName("niceBlue");
    //  if (yourList) h4.addStyleName("niceBlue");

    r1.add(h4);


    boolean yourList = isYourList(ul);
    if (yourList) {
      Button deleteButton = makeDeleteButton(ul);
      deleteButton.addStyleName("floatRight");
      deleteButton.addStyleName("leftFiveMargin");
      r1.add(deleteButton);
    }

    if (!ul.isFavorite()) {
      String html1 = "by " +  ul.getCreator().userID;
      Heading h4Again;
      if (yourList) {
        h4Again = new Heading(5,html1);
      }
      else {
        h4Again = new Heading(4,"",html1);
      }

      h4Again.addStyleName("floatRight");
      r1.add(h4Again);
    }

    container.add(r1);

    if (showMore && !ul.getDescription().isEmpty()) {
      Panel r2 = new FluidRow();
      container.add(r2);
      r2.add(getUserListText2(ul.getDescription()));
    }
  }

  private Widget makeItemMarker2(UserList ul) {
    InlineLabel itemMarker = new InlineLabel(ul.getExercises().size() + " items");
    itemMarker.addStyleName("numItemFont");
    listToMarker.put(ul, itemMarker);
    return itemMarker;
  }

  private Button makeDeleteButton(final UserList ul) {
    Button delete = new Button("Delete");
    delete.addStyleName("topMargin");
    DOM.setStyleAttribute(delete.getElement(), "marginBottom", "5px");

    delete.setType(ButtonType.WARNING);
    delete.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        service.deleteList(ul.getUniqueID(), new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {}

          @Override
          public void onSuccess(Boolean result) {
            if (result) {
              refreshViewLessons();
            }
            else {
              System.err.println("---> did not do deleteList " + ul.getUniqueID());
            }
          }
        });
      }
    });
    return delete;
  }

  private boolean isYourList(UserList ul) {
    return createdByYou(ul) && !ul.getName().equals(UserList.MY_LIST);
  }

/*
  private Widget getUserListText(String content) {
    Widget nameInfo = new HTML(content);
    nameInfo.addStyleName("userListFontBlue");
    return nameInfo;
  }

  private Widget getFavoriteUserListText(String content) {
    Widget nameInfo = new HTML(content);
    nameInfo.addStyleName("userListFont");
    return nameInfo;
  }
*/

  private Widget getUserListText2(String content) {
    Widget nameInfo = new HTML(content);
    nameInfo.addStyleName("userListFont2");
    return nameInfo;
  }
}
