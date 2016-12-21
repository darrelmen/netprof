/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.*;
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
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.ReloadableContainer;
import mitll.langtest.client.custom.tabs.RememberTabAndContent;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.exercise.WaveformPostAudioRecordButton;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.list.Reloadable;
import mitll.langtest.client.qc.UserTitle;
import mitll.langtest.client.scoring.ASRScoringAudioPanel;
import mitll.langtest.client.scoring.EmptyScoreListener;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.user.MiniUser;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/28/2014.
 */
public class ReviewEditableExercise extends EditableExerciseDialog {
  private final Logger logger = Logger.getLogger("ReviewEditableExercise");

  private static final String MARK_FIXED_TOOLTIP = "Mark item as fixed, removed defective audio, and remove item from the review list.";

  /**
   * @see #makeFixedButton
   */
  private static final String FIXED = "Mark Fixed";
  private static final String DUPLICATE = "Duplicate";
  private static final String DELETE = "Delete";
  private static final String DELETE_THIS_ITEM = "Delete this item.";
  private static final String ARE_YOU_SURE = "Are you sure?";
  private static final String REALLY_DELETE_ITEM = "Really delete whole item and all audio cuts?";
  private static final List<String> MSGS = Collections.singletonList(REALLY_DELETE_ITEM);
  private static final String COPY_THIS_ITEM = "Copy this item.";
  private static final String REGULAR_SPEED = " Regular speed";
  private static final String SLOW_SPEED = " Slow speed";
  private static final int DELAY_MILLIS = 5000;

  /**
   * @see #makeAudioRow
   */
  private static final String ADD_AUDIO = "Add audio";

  private final Set<Widget> audioWasPlayed = new HashSet<>();
  private final PagingExerciseList<CommonShell, CommonExercise> exerciseList;

  private BasicDialog.FormField context;
  private BasicDialog.FormField contextTrans;
  private final HTML contextAnno = new HTML();
  private final HTML contextTransAnno = new HTML();
  private String originalContext = "";
  private String originalContextTrans = "";
  private List<RememberTabAndContent> tabs;
  CheckBox keepAudio = new CheckBox("Keep Audio even if text changes");

  /**
   * @param itemMarker
   * @param changedUserExercise
   * @param originalList
   * @param exerciseList
   * @paramx predefinedContent   - this should be a reference to the Learn tab exercise list, but it's not getting set.
   * @see mitll.langtest.client.custom.content.ReviewItemHelper#doInternalLayout(mitll.langtest.shared.custom.UserList, String)
   */
  public ReviewEditableExercise(LangTestDatabaseAsync service,
                                ExerciseController controller,
                                HasText itemMarker,
                                CommonExercise changedUserExercise,

                                UserList<CommonShell> originalList,
                                PagingExerciseList<CommonShell, CommonExercise> exerciseList,
                                ReloadableContainer predefinedContent,
                                String instanceName) {
    super(service, controller,
        null,
        itemMarker, changedUserExercise, originalList, exerciseList,
        predefinedContent,
        instanceName);
    this.exerciseList = exerciseList;
  }

  /**
   * TODO should move this stuff into a class that handles these basic operations
   *
   * @param newUserExercise
   * @param <S>
   */
  @Override
  public <S extends CommonShell & AudioRefExercise & AnnotationExercise> void setFields(S newUserExercise) {
    super.setFields(newUserExercise);

    final com.github.gwtbootstrap.client.ui.base.TextBoxBase box = context.box;

    // TODO : put this back!!!


/*    box.setText(originalContext = newUserExercise.getContext());

    box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        gotBlur();
        try {
          long uniqueID = originalList.getID();
          controller.logEvent(box, "TextBox", "UserList_" + uniqueID, "ContextBox = " + box.getValue());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    useAnnotation(newUserExercise, "context", contextAnno);
    useAnnotation(newUserExercise, "context translation", contextTransAnno);

    TextBoxBase box1 = contextTrans.box;
    box1.setText(originalContextTrans = newUserExercise.getContextTranslation());
    box1.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        gotBlur();
        try {
          long uniqueID = originalList.getID();
          controller.logEvent(box1, "TextBox", "UserList_" + uniqueID, "ContextTransBox = " + box1.getValue());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });*/
  }

  void grabInfoFromFormAndStuffInfoExercise(MutableExercise mutableExercise) {
    super.grabInfoFromFormAndStuffInfoExercise(mutableExercise);

    // TODO : put this back!
    /*    mutableExercise.setContext(context.getText());
    mutableExercise.setContextTranslation(contextTrans.getText());*/
  }

  protected void makeOptionalRows(DivWidget upper) {
    makeContextRow(upper);
    makeContextTransRow(upper);
  }

  protected void makeContextRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    context = makeBoxAndAnno(row, "Context", "", contextAnno);
  }

  protected void makeContextTransRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    contextTrans = makeBoxAndAnno(row, "Context Translation", "", contextTransAnno);
  }


  /**
   * @return
   * @see #addNew(mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel)
   */
  @Override
  protected Panel makeAudioRow() {
    AudioAttributeExercise audioAttributeExercise = newUserExercise;

    Map<MiniUser, List<AudioAttribute>> malesMap = audioAttributeExercise.getUserMap(true);
    Map<MiniUser, List<AudioAttribute>> femalesMap = audioAttributeExercise.getUserMap(false);

    List<MiniUser> maleUsers = audioAttributeExercise.getSortedUsers(malesMap);
    List<MiniUser> femaleUsers = audioAttributeExercise.getSortedUsers(femalesMap);

    tabs = new ArrayList<>();

    TabPanel tabPanel = new TabPanel();
    addTabsForUsers(newUserExercise, tabPanel, malesMap, maleUsers);
    addTabsForUsers(newUserExercise, tabPanel, femalesMap, femaleUsers);

    RememberTabAndContent tabAndContent = getRememberTabAndContent(tabPanel, ADD_AUDIO, false, false);

    tabAndContent.getContent().add(getRecordingWidget());
    tabAndContent.getTab().setIcon(IconType.PLUS);

    tabPanel.selectTab(0);
    return tabPanel;
  }

  private DivWidget getRecordingWidget() {
    DivWidget widget = new DivWidget();
    widget.add(getRecordAudioWithAnno(widget, AudioType.REGULAR));
    widget.add(getRecordAudioWithAnno(widget, AudioType.SLOW));

    return widget;
  }

  private Panel getRecordAudioWithAnno(DivWidget widget, AudioType audioTypeRegular) {
    MyRecordAudioPanel w = new MyRecordAudioPanel(widget, audioTypeRegular, instance);

    if (w.getAudioAttribute() == null) {
      return w;
    } else {
      Panel vert = new VerticalPanel();
      vert.add(w);
      Widget commentLine = getCommentLine(newUserExercise, w.getAudioAttribute());
      w.setComment(commentLine);
      if (commentLine != null) {
        vert.add(commentLine);
      }
      return vert;
    }
  }

  /**
   * Make tabs in order of users
   *
   * @param commonExercise
   * @param tabPanel
   * @param userToAudio
   * @param users
   * @see #makeAudioRow()
   */
  private <X extends CommonShell & AnnotationExercise> void addTabsForUsers(X commonExercise,
                                                                            TabPanel tabPanel,
                                                                            Map<MiniUser, List<AudioAttribute>> userToAudio,
                                                                            List<MiniUser> users) {
    int me = controller.getUser();
    UserTitle userTitle = new UserTitle();
    for (MiniUser user : users) {

      boolean byMe = (user.getID() == me);
      if (!byMe) {
        String tabTitle = userTitle.getUserTitle(me, user);

        RememberTabAndContent tabAndContent = getRememberTabAndContent(tabPanel, tabTitle, true, true);

        boolean allHaveBeenPlayed = true;

        List<AudioAttribute> audioAttributes = userToAudio.get(user);
        for (AudioAttribute audio : audioAttributes) {
          if (!audio.isHasBeenPlayed()) {
            allHaveBeenPlayed = false;
          }
          Widget panelForAudio = getPanelForAudio(commonExercise, audio, tabAndContent);
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
  }

  private RememberTabAndContent getRememberTabAndContent(TabPanel tabPanel, String tabTitle, boolean addRightMargin, boolean isCheckable) {
    RememberTabAndContent tabAndContent = new RememberTabAndContent(IconType.QUESTION_SIGN, tabTitle, isCheckable);
    tabPanel.add(tabAndContent.getTab().asTabLink());
    tabs.add(tabAndContent);

    // TODO : when do we need this???
    if (addRightMargin) tabAndContent.getContent().getElement().getStyle().setMarginRight(70, Style.Unit.PX);
    return tabAndContent;
  }

/*  private String getUserTitle(int me, MiniUser user) {
    return (user.isDefault()) ? GoodwaveExercisePanel.DEFAULT_SPEAKER : (user.getExID() == me) ? "by You (" + user.getUserID() + ")" : getUserTitle(user);
  }*/

/*
  private String getUserTitle(int me, MiniUser user) {
    long id = user.getID();
    if (id == UserDAO.DEFAULT_USER_ID) return GoodwaveExercisePanel.DEFAULT_SPEAKER;
    else if (id == UserDAO.DEFAULT_MALE_ID) return "Default Male";
    else if (id == UserDAO.DEFAULT_FEMALE_ID) return "Default Female";
    else return
          (user.getID() == me) ? "by You (" + user.getUserID() + ")" : getUserTitle(user);
  }
*/

/*
  private String getUserTitle(MiniUser user) {
    String suffix = user.getAge() < 99 ? " age " + user.getAge() : "";
    String userid = true//user.isAdmin()
        ? " (" + user.getUserID() + ")" : "";
    return (user.isMale() ? MALE : FEMALE) +
        userid +
        suffix;
  }
*/

  /**
   * Don't warn user to check if audio is consistent if there isn't any.
   *
   * @return
   */
  @Override
  boolean checkForForeignChange() {
    boolean didChange = super.checkForForeignChange();

    if (didChange) {
      if (hasAudio()) {
/*        for (RememberTabAndContent tab : tabs) {
          if (tab.isCheckable()) {
            setupPopover(tab.getContent(), getWarningHeader(), getWarningForFL(), Placement.TOP, DELAY_MILLIS, false);
          }
        }*/
        setupPopover(keepAudio, getWarningHeader(), getWarningForFL(), Placement.TOP, DELAY_MILLIS, false);
      }
    }

    return didChange;
  }

//  private final Set<Widget> audioWasPlayed = new HashSet<>();
  // private final Set<Widget> toResize = new HashSet<>();

/*
  private String getPath(String path) {
    return CompressedAudio.getPath(path);
  }
*/

  private <X extends CommonShell & AnnotationExercise> Widget getPanelForAudio(final X exercise,
                                                                               final AudioAttribute audio,
                                                                               RememberTabAndContent tabAndContent) {
    String audioRef = audio.getAudioRef();
    if (audioRef != null) {
      audioRef = CompressedAudio.getPathNoSlashChange(audioRef);   // todo why do we have to do this?
    }
    final ASRScoringAudioPanel audioPanel = new ASRScoringAudioPanel<X>(audioRef, exercise.getForeignLanguage(), controller,
        controller.getProps().showSpectrogram(), new EmptyScoreListener(), 70, audio.isRegularSpeed() ? REGULAR_SPEED : SLOW_SPEED,
        exercise, instance
    ) {
      /**
       * @see mitll.langtest.client.scoring.AudioPanel#addWidgets(String, String)
       * @return
       */
      @Override
      protected Widget getAfterPlayWidget() {
        return getDeleteButton(this, audio, exercise, "Delete this audio cut.  Original recorder can re-record.");
      }
    };
    audioPanel.setShowColor(true);
    audioPanel.getElement().setId("ASRScoringAudioPanel");
    noteAudioHasBeenPlayed(exercise, audio, audioPanel);
    tabAndContent.addWidget(audioPanel);
    //  toResize.add(audioPanel);

    Panel vert = new VerticalPanel();
    vert.add(audioPanel);
    Widget commentLine = getCommentLine(exercise, audio);
    if (commentLine != null) {
      vert.add(commentLine);
    }

    return vert;
  }

  /**
   * @param widgets
   * @param audio
   * @param exerciseID
   * @param tip
   * @return
   * @see #getPanelForAudio
   */
  private Widget getDeleteButton(final Panel widgets, final AudioAttribute audio, final HasID exercise, String tip) {
    ClickHandler handler = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        logger.info("marking audio defect for " + audio + " on " + exercise.getID());
        controller.getQCService().markAudioDefect(audio, exercise, new AsyncCallback<Void>() {    // delete comment too?
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Void result) {
            widgets.getParent().setVisible(false);
            reloadLearnList();
            // TODO : need to update other lists too?
          }
        });
      }
    };

    return getDeleteButton(tip, handler);
  }

  /**
   * @see #doAfterEditComplete(ListInterface, boolean)
   */
  private void reloadLearnList() {
    Reloadable exerciseList = predefinedContentList.getReloadable();
    if (exerciseList != null) {
      exerciseList.clearCachedExercise();
      exerciseList.reload();
    } else {
      logger.warning("reloadLearnList : no exercise list ref ");
    }
  }

  // String tip = "Delete this audio cut.  Original recorder can re-record.";
  private Button getDeleteButton(String tip, ClickHandler handler) {
    Button delete = new Button("Delete Audio");
    addTooltip(delete, tip);
    delete.addStyleName("leftFiveMargin");
    delete.setType(ButtonType.WARNING);
    delete.setIcon(IconType.REMOVE);
    delete.addClickHandler(handler);
    return delete;
  }

  /**
   * @param id
   * @param audio
   * @param audioPanel
   * @see #getPanelForAudio
   */
  private void noteAudioHasBeenPlayed(final HasID id, final AudioAttribute audio, final ASRScoringAudioPanel audioPanel) {
    audioPanel.addPlayListener(new PlayListener() {
      @Override
      public void playStarted() {
        audioWasPlayed.add(audioPanel);
        logger.info("now audio was played is size " + audioWasPlayed.size());
        for (RememberTabAndContent tabAndContent : tabs) {
          tabAndContent.checkAllPlayed(audioWasPlayed);
        }
        controller.logEvent(audioPanel, "qcPlayAudio", id.getID(), audio.getAudioRef());
      }

      @Override
      public void playStopped() {
      }
    });
  }

  /**
   * @param e
   * @param audio
   * @return
   * @see #getPanelForAudio
   */
  private <E extends AnnotationExercise> Widget getCommentLine(E e, AudioAttribute audio) {
    ExerciseAnnotation audioAnnotation = e.getAnnotation(audio.getAudioRef());

//    logger.info("annotation for " + audio.getAudioRef() + " is " + audioAnnotation);

    if (audioAnnotation != null && !audioAnnotation.isCorrect()) {
      HTML child = new HTML(audioAnnotation.getComment().isEmpty() ? "EMPTY COMMENT" : audioAnnotation.getComment());
      child.getElement().getStyle().setFontSize(14, Style.Unit.PX);
      child.getElement().getStyle().setFontWeight(Style.FontWeight.BOLD);
      return child;
    } else {
      Map<String, ExerciseAnnotation> fieldToAnnotation = e.getFieldToAnnotation();

//      for (Map.Entry<String, ExerciseAnnotation> pair : fieldToAnnotation.entrySet()) {
//        logger.info("found " + pair.getKey() + " : " + pair.getValue());
//      }
      return null;
    }
  }

  @Override
  protected boolean shouldDisableNext() {
    return false;
  }

  /**
   * Three buttons - previous, next, mark fixed.
   * <p>
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
  protected Panel getCreateButton(final UserList<CommonShell> ul,
                                  final ListInterface<CommonShell> pagingContainer,
                                  final Panel toAddTo,
                                  final ControlGroup normalSpeedRecording) {
    Panel row = new DivWidget();
    row.addStyleName("marginBottomTen");

    PrevNextList prevNext = getPrevNext(pagingContainer);
    prevNext.addStyleName("floatLeft");
    row.add(prevNext);

    if (newUserExercise.isPredefined()) {//getCombinedMutableUserExercise().checkPredef()) {   // for now, only the owner of the list can remove or add to their list
      row.add(getRemove());
      row.add(getDuplicate());
    }

    row.add(getFixedButton(ul, pagingContainer, toAddTo, normalSpeedRecording));
    boolean keepAudioSelection = getKeepAudioSelection();

 //   logger.info("value is  " + keepAudioSelection);
    keepAudio.setValue(keepAudioSelection);
    keepAudio.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent clickEvent) {
        storeKeepAudio(keepAudio.getValue());
      }
    });
    keepAudio.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
    row.add(keepAudio);
    configureButtonRow(row);

    return row;
  }

  private Button getFixedButton(final UserList<CommonShell> ul,
                                final ListInterface<CommonShell> pagingContainer,
                                final Panel toAddTo,
                                final ControlGroup normalSpeedRecording) {
    final Button fixed = makeFixedButton();
    fixed.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        validateThenPost(foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo, true, true);
      }
    });
    return fixed;
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
        confirmThenDeleteItem();
      }
    });
    return remove;
  }

  private void confirmThenDeleteItem() {
    DialogHelper dialogHelper = new DialogHelper(true);
    dialogHelper.show(ARE_YOU_SURE, MSGS, new DialogHelper.CloseListener() {
      @Override
      public void gotYes() {
        controller.getQCService().deleteItem(newUserExercise.getID(), new AsyncCallback<Boolean>() {
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

  /**
   * @return
   */
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
    newUserExercise.getMutable().setCreator(controller.getUser());
    CommonShell commonShell = exerciseList.byHasID(newUserExercise);
    if (commonShell != null) {
      newUserExercise.setState(commonShell.getState());
      newUserExercise.setSecondState(commonShell.getSecondState());
    }
    //logger.info("to duplicate " + newUserExercise + " state " + newUserExercise.getState());
    listService.duplicateExercise(newUserExercise, new AsyncCallback<CommonExercise>() {
      @Override
      public void onFailure(Throwable caught) {
        duplicate.setEnabled(true);
      }

      @Override
      public void onSuccess(CommonExercise result) {
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
    addTooltip(fixed, MARK_FIXED_TOOLTIP);
    return fixed;
  }

  /**
   * @see mitll.langtest.client.custom.dialog.NewUserExercise.CreateFirstRecordAudioPanel#makePostAudioRecordButton
   */
  @Override
  protected void audioPosted() {
    Boolean value = getKeepAudio();
  //  logger.info("did audioPosted keep audio = " + value);
    reallyChange(listInterface, false, value);
  }

  protected boolean getKeepAudio() {
    Boolean value = keepAudio.getValue();
  //  logger.info(this.getClass() + " : did getKeepAudio keep audio = " + value);
    return value;
  }

  /**
   * @see #isValidForeignPhrase(UserList, ListInterface, Panel, boolean)
   */
  @Override
  protected void checkIfNeedsRefAudio() {
  }

  private boolean getKeepAudioSelection() {
    return getKeepAudioSelection(getSelectedUserKey(controller, ""));
  }

  private boolean getKeepAudioSelection(String selectedUserKey) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      String item = localStorageIfSupported.getItem(selectedUserKey);
     // logger.info("value for " + selectedUserKey + "='" + item+ "'");
      if (item != null) {
        return item.toLowerCase().equals("true");
      }
      else {
        storeKeepAudio(true);
        return true;
      }
    }
    // else {
    return false;
    // }
  }

  private String getSelectedUserKey(ExerciseController controller, String appTitle) {
    return getStoragePrefix(controller, appTitle) + "keepAudio";
  }

  private String getStoragePrefix(ExerciseController controller, String appTitle) {
    return appTitle + ":" + controller.getUser() + ":";
  }

  private void storeKeepAudio(boolean shouldKeepAudio) {
    if (Storage.isLocalStorageSupported()) {
      Storage localStorageIfSupported = Storage.getLocalStorageIfSupported();
      localStorageIfSupported.setItem(getSelectedUserKey(controller, ""), "" + shouldKeepAudio);
    }
  }

  /**
   * TODOx : why do we have to do a sequence of server calls -- how about just one???
   *
   * @param pagingContainer
   * @param buttonClicked
   * @seex #doAfterEditComplete(mitll.langtest.client.list.ListInterface, boolean)
   * @see #reallyChange
   * @see #postEditItem
   */
  @Override
  protected void doAfterEditComplete(ListInterface<CommonShell> pagingContainer, boolean buttonClicked) {
    //  super.doAfterEditComplete(pagingContainer, buttonClicked);
    changeTooltip(pagingContainer);
    if (buttonClicked) {
      userSaidExerciseIsFixed();
    }
  }

  private void userSaidExerciseIsFixed() {
    final int id = newUserExercise.getID();
    int user = controller.getUser();

//    logger.info("doAfterEditComplete : forgetting exercise " + id + " current user " + user + " before list had " + ul.getExercises().size());

      controller.getQCService().markState(id, STATE.FIXED, user, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Void result) {
          logger.info("doAfterEditComplete : forgetting exercise " + id);
          exerciseList.forgetExercise(id);

          reloadLearnList();
        }
      });
     //else {
    //logger.info("----> doAfterEditComplete : button not clicked ");
    // }
  }

  @Override
  protected String getEnglishLabel() {
    return controller.getLanguage().equalsIgnoreCase("english") ? "Meaning<br/>" : "English<br/>";
  }

  @Override
  protected String getTransliterationLabel() {
    return "Transliteration";
  }

  private class MyRecordAudioPanel extends RecordAudioPanel {
    private Button deleteButton;
    private Widget comment;

    public MyRecordAudioPanel(DivWidget widget, AudioType audioType, String instance) {
      super(ReviewEditableExercise.this.newUserExercise, ReviewEditableExercise.this.controller, widget,
          0, false, audioType, instance);
      this.audioType = audioType;
    }

    public void setComment(Widget comment) {
      this.comment = comment;
    }

    protected WaveformPostAudioRecordButton makePostAudioRecordButton(AudioType audioType, final String recordButtonTitle) {
      return new MyWaveformPostAudioRecordButton(audioType, recordButtonTitle) {
        @Override
        public void useResult(AudioAnswer result) {
          super.useResult(result);
          if (result.isValid()) {
            newUserExercise.getMutableAudio().addAudio(result.getAudioAttribute());
            deleteButton.setEnabled(true);
          }
        }
      };
    }

    /**
     * @return
     * @see mitll.langtest.client.scoring.AudioPanel#addWidgets(String, String)
     */
    @Override
    protected Widget getAfterPlayWidget() {
      ClickHandler handler = new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          deleteButton.setEnabled(false);
          int id = exercise.getID();
          logger.info("marking audio defect for " + getAudioAttribute() + " on " + id);
          controller.getQCService().markAudioDefect(getAudioAttribute(), exercise, new AsyncCallback<Void>() {    // delete comment too?
            @Override
            public void onFailure(Throwable caught) {
            }

            @Override
            public void onSuccess(Void result) {
              // widgets.getParent().setVisible(false);
              getWaveform().setVisible(false);
              getPlayButton().setEnabled(false);
              if (comment != null) comment.setVisible(false);

              reloadLearnList();
            }
          });
        }
      };

      deleteButton = getDeleteButton("Delete this audio cut.", handler);
      if (getAudioAttribute() == null) deleteButton.setEnabled(false);
      return deleteButton;
    }

    @Override
    protected float getWaveformHeight() {
      return 60;
    }
  }
}
