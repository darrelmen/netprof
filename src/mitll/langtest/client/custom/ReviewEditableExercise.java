package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.TabPanel;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.scoring.ScoreListener;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.AudioAttribute;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.STATE;
import mitll.langtest.shared.ScoreAndPath;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by GO22670 on 3/28/2014.
 */
class ReviewEditableExercise extends EditableExercise {
  private static final String FIXED = "Mark Fixed";
  private static final String DUPLICATE = "Duplicate";
  private static final String DELETE = "Delete";
  private static final String DELETE_THIS_ITEM = "Delete this item.";
  private static final String ARE_YOU_SURE = "Are you sure?";
  private static final String REALLY_DELETE_ITEM = "Really delete whole item and all audio cuts?";
  private static final String COPY_THIS_ITEM = "Copy this item.";
  public static final String REGULAR_SPEED = " Regular speed";
  public static final String SLOW_SPEED = " Slow speed";
  public static final int DELAY_MILLIS = 5000;

  private PagingExerciseList exerciseList;
  private ListInterface predefinedContentList;
  private static final String WAV = ".wav";
  private static final String MP3 = "." + AudioTag.COMPRESSED_TYPE;

  /**
   * @param itemMarker
   * @param changedUserExercise
   * @param originalList
   * @param exerciseList
   * @see mitll.langtest.client.custom.ReviewItemHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
   */
  public ReviewEditableExercise(LangTestDatabaseAsync service,
                                ExerciseController controller,
                                HasText itemMarker,
                                CommonUserExercise changedUserExercise,

                                UserList originalList,
                                PagingExerciseList exerciseList,
                                ListInterface predefinedContent,
                                NPFHelper npfHelper) {
    super(service, controller,
      null,
      itemMarker, changedUserExercise, originalList, exerciseList, predefinedContent, npfHelper);
    this.exerciseList = exerciseList;
    this.predefinedContentList = predefinedContent;
  }

  private List<RememberTabAndContent> tabs;

  /**
   * @return
   * @see #addNew(mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel)
   */
  @Override
  protected Panel makeAudioRow() {
    Map<MiniUser, List<AudioAttribute>> malesMap = newUserExercise.getUserMap(true);
    Map<MiniUser, List<AudioAttribute>> femalesMap = newUserExercise.getUserMap(false);

    List<MiniUser> maleUsers = newUserExercise.getSortedUsers(malesMap);
    List<MiniUser> femaleUsers = newUserExercise.getSortedUsers(femalesMap);

    tabs = new ArrayList<RememberTabAndContent>();

    TabPanel tabPanel = new TabPanel();
    addTabsForUsers(newUserExercise, tabPanel, malesMap, maleUsers);
    addTabsForUsers(newUserExercise, tabPanel, femalesMap, femaleUsers);

    if (!maleUsers.isEmpty() || !femaleUsers.isEmpty()) tabPanel.selectTab(0);

    return tabPanel;
  }

  /**
   * Make tabs in order of users
   *
   * @param e
   * @param tabPanel
   * @param malesMap
   * @param maleUsers
   */
  private void addTabsForUsers(CommonExercise e, TabPanel tabPanel, Map<MiniUser, List<AudioAttribute>> malesMap, List<MiniUser> maleUsers) {
    for (MiniUser user : maleUsers) {
      String tabTitle = (user.getId() == QCNPFExercise.DEFAULT_USER) ? "Default Speaker" : getUserTitle(user);

      RememberTabAndContent tabAndContent = new RememberTabAndContent(IconType.QUESTION_SIGN, tabTitle);
      tabPanel.add(tabAndContent.getTab().asTabLink());
      tabs.add(tabAndContent);

      // TODO : when do we need this???
      tabAndContent.getContent().getElement().getStyle().setMarginRight(70, Style.Unit.PX);

      boolean allHaveBeenPlayed = true;

      List<AudioAttribute> audioAttributes = malesMap.get(user);
      for (AudioAttribute audio : audioAttributes) {
        if (!audio.isHasBeenPlayed()) {
          allHaveBeenPlayed = false;
        }
        Widget panelForAudio = getPanelForAudio(e, audio, tabAndContent);
        if (audioAttributes.size() == 2 && audioAttributes.indexOf(audio) == 0) {
          panelForAudio.addStyleName("bottomFiveMargin");
        }
        tabAndContent.getContent().add(panelForAudio);
        if (audio.isHasBeenPlayed()) {
          audioWasPlayed.add(panelForAudio);
        }
      }

      if (allHaveBeenPlayed) {
        tabAndContent.getTab().setIcon(IconType.CHECK_SIGN);
      }
    }
  }

  private String getUserTitle(MiniUser user) {
    return (user.isMale() ? "Male" :"Female")+
      (controller.getProps().isAdminView() ?" (" + user.getUserID() + ")" :"") +
      " age " + user.getAge();
  }

  @Override
  boolean checkForForeignChange() {
    boolean b = super.checkForForeignChange();

    if (b) {
      for (RememberTabAndContent tab : tabs) {
        setupPopover(tab.getContent(), getWarningHeader(), getWarningForFL(), Placement.TOP, DELAY_MILLIS);
      }
    }

    return b;
  }

  private Set<Widget> audioWasPlayed = new HashSet<Widget>();
  private Set<Widget> toResize = new HashSet<Widget>();

  private Widget getPanelForAudio(final CommonExercise e, final AudioAttribute audio, RememberTabAndContent tabAndContent) {
    String audioRef = audio.getAudioRef();
    if (audioRef != null) {
      audioRef = wavToMP3(audioRef);   // todo why do we have to do this?
    }
    final ASRScoringAudioPanel audioPanel = new ASRScoringAudioPanel(audioRef, e.getRefSentence(), service, controller,
      controller.getProps().showSpectrogram(), new ScoreListener() {
      @Override
      public void setClassAvg(float classAvg) {}

      @Override
      public void gotScore(PretestScore score, boolean showOnlyOneExercise, String path) {}

      @Override
      public int getOffsetWidth() { return 0; }

      @Override
      public void addScore(ScoreAndPath hydecScore) {}

      @Override
      public void showChart(boolean showOnlyOneExercise) {}

      @Override
      public void setRefAudio(String refAudio) {

      }
    }, 70, audio.isRegularSpeed() ? REGULAR_SPEED : SLOW_SPEED, e.getID()
    ) {

      @Override
      protected Widget getBeforePlayWidget() {
        Button delete = new Button("Delete Audio");
        addTooltip(delete, "Delete this audio cut.  Original recorder can re-record.");
        delete.addStyleName("leftFiveMargin");
        delete.setType(ButtonType.WARNING);
        delete.setIcon(IconType.REMOVE);

        final Widget outer = this;
        delete.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            System.out.println("marking audio defect for " +audio + " on " + e.getID());
            service.markAudioDefect(audio, e.getID(), new AsyncCallback<Void>() {    // delete comment too?
              @Override
              public void onFailure(Throwable caught) {}

              @Override
              public void onSuccess(Void result) {
                outer.getParent().setVisible(false);
                predefinedContentList.reload();
                // TODO : need to update other lists too?
              }
            });
          }
        });

        return delete;
      }
    };
    audioPanel.setShowColor(true);
    audioPanel.getElement().setId("ASRScoringAudioPanel");
    noteAudioHasBeenPlayed(e.getID(), audio, audioPanel);
    tabAndContent.addWidget(audioPanel);
    toResize.add(audioPanel);

    Panel vert = new VerticalPanel();
    vert.add(audioPanel);
    Widget commentLine = getCommentLine(e, audio);
    if (commentLine != null) {
      vert.add(commentLine);
    }

    return vert;
  }

  private void noteAudioHasBeenPlayed(final String id, final AudioAttribute audio, final ASRScoringAudioPanel audioPanel) {
    audioPanel.addPlayListener(new PlayListener() {
      @Override
      public void playStarted() {
        //System.out.println("playing audio " + audio.getAudioRef() + " has " + tabs.size() + " tabs");
        audioWasPlayed.add(audioPanel);
        //if (audioWasPlayed.size() == toResize.size()) {
          // all components played  // TODO put this back
          //      setApproveButtonState();
       // }
        for (RememberTabAndContent tabAndContent : tabs) {
          tabAndContent.checkAllPlayed(audioWasPlayed);
        }
        controller.logEvent(audioPanel, "qcPlayAudio", id, audio.getAudioRef());
      }

      @Override
      public void playStopped() {
      }
    });
  }

  private Widget getCommentLine(CommonExercise e, AudioAttribute audio) {
    ExerciseAnnotation audioAnnotation = e.getAnnotation(audio.getAudioRef());
    if (audioAnnotation != null && !audioAnnotation.isCorrect()) {
      HTML child = new HTML(audioAnnotation.comment);
      child.getElement().getStyle().setFontSize(14, Style.Unit.PX);
      child.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);
      return child;
    }
    else return null;
  }

  protected String wavToMP3(String path) {
    return (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
  }

  @Override
  protected boolean shouldDisableNext() {
    return false;
  }

  /**
   * Add a fixed button, so we know when to clear the comments and remove this item from the reviewed list.
   *
   * @param ul
   * @param pagingContainer
   * @param toAddTo
   * @param normalSpeedRecording
   * @return
   * @see #addNew
   */
  @Override
  protected Panel getCreateButton(final UserList ul, final ListInterface pagingContainer, final Panel toAddTo,
                                  final ControlGroup normalSpeedRecording) {
    Panel row = new DivWidget();
    row.addStyleName("marginBottomTen");

    PrevNextList prevNext = getPrevNext(pagingContainer);
    prevNext.addStyleName("floatLeft");
    row.add(prevNext);

    final Button fixed = makeFixedButton();

    fixed.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        validateThenPost(foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo, true, true);
      }
    });

    if (newUserExercise.checkPredef()) {   // for now, only the owner of the list can remove or add to their list
      row.add(getRemove());
      row.add(getDuplicate());
    }

    row.add(fixed);

    configureButtonRow(row);

    return row;
  }

  private Button getRemove() {
    Button remove = new Button(DELETE);
    remove.setType(ButtonType.WARNING);
    remove.addStyleName("floatRight");
    remove.addStyleName("leftFiveMargin");
    addTooltip(remove, DELETE_THIS_ITEM);

    remove.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        DialogHelper dialogHelper = new DialogHelper(true);
        dialogHelper.show(ARE_YOU_SURE, Arrays.asList(REALLY_DELETE_ITEM), new DialogHelper.CloseListener() {
          @Override
          public void gotYes() {
            service.deleteItem(newUserExercise.getID(), new AsyncCallback<Boolean>() {
              @Override
              public void onFailure(Throwable caught) {
              }

              @Override
              public void onSuccess(Boolean result) {
                exerciseList.removeExercise(newUserExercise);
                originalList.remove(newUserExercise.getID());
              }
            });
          }

          @Override
          public void gotNo() {
          }
        });
      }
    });
    return remove;
  }

  private Button getDuplicate() {
    final Button duplicate = new Button(DUPLICATE);
    duplicate.setType(ButtonType.SUCCESS);
    duplicate.addStyleName("floatRight");
    addTooltip(duplicate, COPY_THIS_ITEM);

    duplicate.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        duplicateExercise(duplicate);
      }
    });
    return duplicate;
  }

  /**
   * Disable the button initially so we don't accidentally duplicate twice.
   *
   * @param duplicate
   */
  private void duplicateExercise(final Button duplicate) {
    duplicate.setEnabled(false);
    newUserExercise.setCreator(controller.getUser());
    CommonShell commonShell = exerciseList.byID(newUserExercise.getID());
    if (commonShell != null) {
      newUserExercise.setState(commonShell.getState());
      newUserExercise.setSecondState(commonShell.getSecondState());
    }
    //System.out.println("to duplicate " + newUserExercise + " state " + newUserExercise.getState());
    service.duplicateExercise(newUserExercise, new AsyncCallback<UserExercise>() {
      @Override
      public void onFailure(Throwable caught) {
        duplicate.setEnabled(true);
      }

      @Override
      public void onSuccess(UserExercise result) {
        duplicate.setEnabled(true);

        exerciseList.addExerciseAfter(newUserExercise, result);
        exerciseList.redraw();
        originalList.addExerciseAfter(newUserExercise, result);
      }
    });
  }

  /**
   * @return
   * @see #getCreateButton(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, com.github.gwtbootstrap.client.ui.ControlGroup)
   */
  private Button makeFixedButton() {
    Button fixed = new Button(FIXED);
    fixed.setType(ButtonType.PRIMARY);

    fixed.addStyleName("leftFiveMargin");
    fixed.addStyleName("floatLeft");
    fixed.addStyleName("marginRight");

    fixed.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        checkForForeignChange();
      }
    });
    addTooltip(fixed, "Mark item as fixed, removed defective audio, and remove item from the review list.");
    return fixed;
  }

  @Override
  protected void audioPosted() {
    reallyChange(listInterface, false);
  }

  @Override
  protected void checkIfNeedsRefAudio() {
  }

  /**
   * TODOx : why do we have to do a sequence of server calls -- how about just one???
   *
   * @param pagingContainer
   * @param buttonClicked
   * @seex #doAfterEditComplete(mitll.langtest.client.list.ListInterface, boolean)
   * @see #reallyChange
   * @see #postEditItem(mitll.langtest.client.list.ListInterface, boolean)
   */
  @Override
  protected void doAfterEditComplete(ListInterface pagingContainer, boolean buttonClicked) {
    super.doAfterEditComplete(pagingContainer, buttonClicked);

    if (buttonClicked) {
      final String id = newUserExercise.getID();
      int user = controller.getUser();

      System.out.println("doAfterEditComplete : forgetting " + id + " user " + user);

      if (!ul.remove(newUserExercise)) {
        System.err.println("\ndoAfterEditComplete : error - didn't remove " + id + " from ul " + ul);
      }
      if (!originalList.remove(newUserExercise)) {
        System.err.println("\ndoAfterEditComplete : error - didn't remove " + id + " from original " + originalList);
      }

      service.setExerciseState(id, STATE.FIXED, user, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Void result) {
          predefinedContentList.reload();
          exerciseList.forgetExercise(id);
        }
      });
    } else {
      System.out.println("----> doAfterEditComplete : button not clicked ");

    }
  }
}
