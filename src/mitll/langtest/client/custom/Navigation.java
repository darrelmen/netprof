package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Tab;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
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
import mitll.langtest.client.dialog.EnterKeyButtonHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.exercise.WaveformPostAudioRecordButton;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.GoodwaveExercisePanel;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.custom.UserExercise;
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
  private static final String CHAPTERS = "Chapters";
  private static final boolean SHOW_CREATED = false;
  private static final String YOUR_LISTS = "Your Lists";
  public static final String PRACTICE = "Practice";
  private final ExerciseController controller;
  private LangTestDatabaseAsync service;
  private UserManager userManager;
  private int userListID;

  private ScrollPanel listScrollPanel;
  private UserFeedback feedback;
  private ListInterface listInterface;
  NPFHelper npfHelper;
  NPFHelper avpHelper;
  HTML itemMarker;

  public Navigation(final LangTestDatabaseAsync service, final UserManager userManager, final ExerciseController controller, final ListInterface listInterface) {
    this.service = service;
    this.userManager = userManager;
    this.controller = controller;
    this.listInterface = listInterface;
    npfHelper = new NPFHelper(service, feedback, userManager, controller);
    avpHelper = new AVPHelper(feedback,service, userManager, controller);
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

    return container;
  }

  private TabPanel tabPanel;
  private Tab yourItems;
  private Panel yourItemsContent;
  private TabAndContent browse;

  private Panel getButtonRow2(Panel secondAndThird) {
    tabPanel = new TabPanel();

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

    final TabAndContent create = makeTab(tabPanel, IconType.PLUS_SIGN, "Create");
    create.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        doCreate(create.content);
      }
    });

    browse = makeTab(tabPanel, IconType.TH_LIST, "Browse");
    browse.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        viewBrowse();
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
        String targetName = showEvent.getTarget() == null ? "" : showEvent.getTarget().toString();

        System.out.println("got shown event : '" +showEvent + "'\n target '" + targetName + "'");

        boolean wasChapters = targetName.contains(CHAPTERS);
        if (wasChapters) {
          System.out.println("got chapters! created panel : " + listInterface.getCreatedPanel() + "\n\n\n");
        }
        if (listInterface.getCreatedPanel() != null && wasChapters) {
          ((GoodwaveExercisePanel) listInterface.getCreatedPanel()).wasRevealed();
        }
      }
    });

    return tabPanel;
  }

  private void viewBrowse() {
    viewLessons(browse.content, true);
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

  private void zeroPadding(Panel createContent) {
    DOM.setStyleAttribute(createContent.getElement(), "paddingLeft", "0px");
    DOM.setStyleAttribute(createContent.getElement(), "paddingRight", "0px");
  }

  public static class TabAndContent {
    public Tab tab;
    public FluidContainer content;

    public TabAndContent(Tab tab, FluidContainer panel) {
      this.tab = tab;
      this.content = panel;
    }
  }

  private void refreshViewLessons() {
    viewLessons(yourItemsContent, false);
  }

  public void showInitialState() {
    System.out.println("show initial state! -->");

    service.getListsForUser(userManager.getUser(), false, new AsyncCallback<Collection<UserList>>() {
      @Override
      public void onFailure(Throwable caught) {
        //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void onSuccess(Collection<UserList> result) {
        if (result.size() == 1 && result.iterator().next().getExercises().isEmpty()) {
          service.getUserListsForText("", new AsyncCallback<Collection<UserList>>() {
            @Override
            public void onFailure(Throwable caught) {
              //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void onSuccess(Collection<UserList> result) {
              if (!result.isEmpty()) {
                // show site-wide browse list instead
                showBrowse();
              }
              else { // otherwise show the chapters tab
                tabPanel.selectTab(3);
              }
            }
          });
        }
        else {
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

  @Override
  public void onResize() {
    setScrollPanelWidth(listScrollPanel);
    npfHelper.onResize();
    avpHelper.onResize();
  }

  private class ButtonClickEvent extends ClickEvent {
        /*To call click() function for Programmatic equivalent of the user clicking the button.*/
  }

  private void viewLessons(final Panel contentPanel, boolean getAll) {
    System.out.println("doing viewLessons----> " + getAll);
    contentPanel.clear();
    contentPanel.getElement().setId("contentPanel");

    final Panel child = new DivWidget();
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

  /**
   * @see Navigation.UserListCallback#onSuccess(java.util.Collection)
   * @param ul
   * @param contentPanel
   */
  private void showList(final UserList ul, Panel contentPanel) {
    FluidContainer container = new FluidContainer();
    contentPanel.clear();
    contentPanel.add(container);
   // contentPanel.addStyleName("fullWidth2");

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
    itemMarker = new HTML(ul.getExercises().size() + " items");
    itemMarker.addStyleName("subtitleForeground");
    r1.add(new Column(3, itemMarker));

    boolean created = createdByYou(ul);
    if (created && SHOW_CREATED) {
      child = new FluidRow();
      container.add(child);
      child.add(new Heading(3, "<b>Created by you.</b>"));
    }
    final FluidContainer listContent = new FluidContainer();

    Panel operations = getListOperations(ul, created);
    container.add(operations);
    container.add(listContent);
  }

  private Panel getListOperations(final UserList ul, final boolean created) {
    final TabPanel tabPanel = new TabPanel();

    final TabAndContent learn = makeTab(tabPanel, IconType.LIGHTBULB, "Learn Pronunciation");
    learn.tab.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        npfHelper.showNPF(ul, learn, "learn");
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
      addItem = makeTab(tabPanel, IconType.PLUS_SIGN, "Add Item");
      final TabAndContent finalAddItem = addItem;
      addItem.tab.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          showAddItem(ul, finalAddItem);
        }
      });

      final TabAndContent edit = makeTab(tabPanel, IconType.EDIT, "Edit");
      edit.tab.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Window.alert("edit  list");
        }
      });
    }

    final TabAndContent finalAddItem = addItem;
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        if (created && !ul.isPrivate() && ul.isEmpty()) {
          tabPanel.selectTab(2);
          showAddItem(ul, finalAddItem);
        } else {
          tabPanel.selectTab(0);
          npfHelper.showNPF(ul, learn, "learn");
        }
      }
    });

    tabPanel.addShowHandler(new TabPanel.ShowEvent.Handler() {
      @Override
      public void onShow(TabPanel.ShowEvent showEvent) {
        String targetName = showEvent.getTarget() == null ? "" : showEvent.getTarget().toString();

        if (targetName.length() > 0) {
          System.out.println("got shown event : '" + showEvent + "'\n target '" + targetName + "'");
          if (targetName.contains(PRACTICE)) {
            avpHelper.addKeyHandler();
          } else {
            avpHelper.removeKeyHandler();
          }
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

    PagingContainer<ExerciseShell> pagingContainer = new PagingContainer<ExerciseShell>(controller, 100, false);
    Panel container = pagingContainer.getTableWithPager();
    left.add(container);
    for (ExerciseShell es : ul.getExercises()) {
      pagingContainer.addExerciseToList2(es);
    }
    pagingContainer.flush();
    right.add(addNew(ul, pagingContainer, right));
    return hp;
  }

  private String slowPath, fastPath;
  private UserExercise createdExercise = null;

  private Panel addNew(final UserList ul, final PagingContainer<?> pagingContainer, final Panel toAddTo) {
    final FluidContainer container = new FluidContainer();
    container.addStyleName("greenBackground");
    FluidRow row;
    slowPath = null;
    fastPath = null;
/*    if (false) {
      container.add(row);
      final Heading header = new Heading(3, "Add a new item");
      row.add(header);
    }*/

    row = new FluidRow();
    container.add(row);
    final FormField english = addControlFormField(row, "English");

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        english.box.setFocus(true);
      }
    });

    row = new FluidRow();
    container.add(row);
    final FormField foreignLang = addControlFormField(row, "Foreign Language (" + controller.getLanguage() + ")");

/*
    row = new FluidRow();
    container.add(row);
    final Heading recordPrompt = new Heading(4, "Record normal speed reference recording");
    row.add(recordPrompt);
*/
    row = new FluidRow();
    container.add(row);

    final LangTestDatabaseAsync outerService = service;
    final RecordAudioPanel rap = new RecordAudioPanel(null, controller, row, service, 0, false, controller.getAudioType()) {
      @Override
      protected void getEachImage(int width) {
        float newWidth = Window.getClientWidth() * 0.65f;
        super.getEachImage((int) newWidth);    //To change body of overridden methods use File | Settings | File Templates.
      }

      /**
       * @see mitll.langtest.client.scoring.AudioPanel#makePlayAudioPanel(com.google.gwt.user.client.ui.Widget, String)
       * @return
       */
      @Override
      protected WaveformPostAudioRecordButton makePostAudioRecordButton(String audioType) {
        return new WaveformPostAudioRecordButton(exercise, controller, exercisePanel, this, service, 0, audioType) {
          @Override
          public void stopRecording() {
            System.out.println("stopRecording with exercise " + exercise);
            if (exercise == null) {
              //feedback.rememberAudioType();
              outerService.createNewItem(userManager.getUser(), english.getText(), foreignLang.getText(), new AsyncCallback<UserExercise>() {
                @Override
                public void onFailure(Throwable caught) {
                  //To change body of implemented methods use File | Settings | File Templates.
                }

                @Override
                public void onSuccess(UserExercise newExercise) {
                  createdExercise = newExercise;
                  System.out.println("stopRecording with createdExercise " + createdExercise);

                  exercise = newExercise.toExercise();
                  setExercise(exercise);
                  stopRecording();
                }
              });
            } else {
              super.stopRecording();
            }
          }

          @Override
          public void useResult(AudioAnswer result) {
            super.useResult(result);
            System.out.println("path to audio is " + result.path);
            fastPath = result.path;
          }
        };
      }
    };
    final ControlGroup normalSpeedRecording = addControlGroupEntry(row, "Record normal speed reference recording", rap);

    Button submit = new Button("Create");
    row.addStyleName("buttonMargin2");
    submit.setType(ButtonType.SUCCESS);
    submit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        System.out.println("creating new item for " + english + " " + foreignLang);

        if (english.getText().isEmpty()) {
          markError(english, "Please enter an english word or phrase.");
        } else if (foreignLang.getText().isEmpty()) {
          markError(foreignLang, "Please enter the foreign language phrase.");
        } else if (fastPath == null) {
          markError(normalSpeedRecording, rap.getButton(), rap.getButton(), "", "Please record reference audio for the foreign language phrase.");
          rap.getButton().addMouseOverHandler(new MouseOverHandler() {
            @Override
            public void onMouseOver(MouseOverEvent event) {
              normalSpeedRecording.setType(ControlGroupType.NONE);
            }
          });
        } else {
//        /  System.out.println("really creating new item for " + english + " " + foreignLang + " created " + createdExercise);

          createdExercise.setRefAudio(fastPath);
          createdExercise.setSlowAudioRef(slowPath);
          service.reallyCreateNewItem(ul, createdExercise, new AsyncCallback<UserExercise>() {
            @Override
            public void onFailure(Throwable caught) {}

            @Override
            public void onSuccess(UserExercise newExercise) {
              ul.addExercise(newExercise);
              itemMarker.setText(ul.getExercises().size() + " items");
              pagingContainer.addAndFlush(newExercise);
              toAddTo.clear();
              toAddTo.add(addNew(ul, pagingContainer, toAddTo));
            }
          });
        }
      }
    });
    Column column = new Column(2, 9, submit);
    column.addStyleName("topMargin");
    row.add(column);

    return container;
  }

  private void setScrollPanelWidth(ScrollPanel row) {
    if (row != null) {
      row.setWidth((Window.getClientWidth() * 0.90) + "px");
      row.setHeight((Window.getClientHeight() * 0.7) + "px");
    }
  }

  /**
   * @see #getButtonRow2(com.google.gwt.user.client.ui.Panel)
   * @param thirdRow
   */
  private void doCreate(Panel thirdRow) {
    // fill in the middle panel with a form to allow you to create a list
    // post the results to the server
    thirdRow.clear();
    final EnterKeyButtonHelper enterKeyButtonHelper = new EnterKeyButtonHelper(true);
    Panel child = new DivWidget() {
      @Override
      protected void onUnload() {
        super.onUnload();
        enterKeyButtonHelper.removeKeyHandler();
      }
    };
    thirdRow.add(child);
    zeroPadding(child);
    child.addStyleName("userListContainer");

    FluidRow row = new FluidRow();
    child.add(row);
    final Heading header = new Heading(2, "Create a New List");
    row.add(header);

    row = new FluidRow();
    child.add(row);
    final FormField titleBox = addControlFormField(row, "Title");

    row = new FluidRow();
    child.add(row);
    final TextArea area = new TextArea();
    addControlGroupEntry(row, "Description", area);

    row = new FluidRow();
    child.add(row);

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
    private final Panel child;

    public UserListCallback(Panel contentPanel, Panel child) {
      this.contentPanel = contentPanel;
      this.child = child;
    }

    @Override
    public void onFailure(Throwable caught) {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onSuccess(Collection<UserList> result) {
      System.out.println("UserListCallback : Displaying " + result.size() + " items");
      // if (result.isEmpty()) System.err.println("\n\nhuh? no results for user");
      if (result.isEmpty()) {
        child.add(new Heading(3, "No lists created yet."));
      } else {
        listScrollPanel = new ScrollPanel();
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

    private Panel getDisplayRowPerList(final UserList ul) {
      final FocusPanel widgets = new FocusPanel();

      widgets.addStyleName("userListContent");
      widgets.addStyleName("userListBackground");
      widgets.addStyleName("leftTenMargin");

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
      FluidContainer w = new FluidContainer();
      widgets.add(w);

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
      itemMarker = new HTML(ul.getExercises().size() + " items");
      itemMarker.addStyleName("numItemFont");
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

      if (createdByYou(ul)) {
        r1 = new FluidRow();
        w.add(r1);
        r1.add(new HTML("<b>Created by you.</b>"));
      }
      return widgets;
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
