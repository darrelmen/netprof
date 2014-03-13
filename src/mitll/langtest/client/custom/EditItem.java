package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class EditItem<T extends ExerciseShell> {
  private static final String NEW_ITEM = "*New Item*";
  public static final String NEW_EXERCISE_ID = "NewExerciseID";
  private static final String EDIT_ITEM = "editItem";
  private static final String REMOVE_FROM_LIST = "Remove from list";

  protected final ExerciseController controller;
  private final NPFHelper npfHelper;
  protected final LangTestDatabaseAsync service;
  private final UserManager userManager;
  protected final ListInterface<? extends ExerciseShell> predefinedContentList;
  private UserFeedback feedback = null;
  private HasText itemMarker;
  protected PagingExerciseList<T> exerciseList;

  /**
   * @param service
   * @param userManager
   * @param controller
   * @param npfHelper
   * @see mitll.langtest.client.custom.Navigation#Navigation
   */
  public EditItem(final LangTestDatabaseAsync service, final UserManager userManager, ExerciseController controller,
                  ListInterface<? extends ExerciseShell> listInterface, UserFeedback feedback, NPFHelper npfHelper) {
    this.controller = controller;
    this.service = service;
    this.userManager = userManager;
    this.predefinedContentList = listInterface;
    this.feedback = feedback;
    this.npfHelper = npfHelper;
  }

  /**
   * @see mitll.langtest.client.custom.Navigation#showEditItem
   * @param originalList
   * @param itemMarker
   * @param includeAddItem
   * @return
   */
  public Panel editItem(UserList originalList, final HasText itemMarker, boolean includeAddItem, boolean isUserReviewer) {
    Panel hp = new HorizontalPanel();
    hp.getElement().setId("EditItem_for_"+originalList.getName());
    Panel pagerOnLeft = new SimplePanel();
    hp.add(pagerOnLeft);
    pagerOnLeft.addStyleName("rightFiveMargin");
    final Panel contentOnRight = new SimplePanel();
    contentOnRight.getElement().setId("EditItem_content");

    hp.add(contentOnRight);

    this.itemMarker = itemMarker; // TODO : something less awkward

    UserList copy = makeListOfOnlyYourItems(isUserReviewer, originalList);

    exerciseList = makeExerciseList(contentOnRight, EDIT_ITEM, copy, originalList, includeAddItem);
    pagerOnLeft.add(exerciseList.getExerciseListOnLeftSide(controller.getProps()));

    rememberAndLoadFirst(copy, exerciseList);
    return hp;
  }

  private UserList makeListOfOnlyYourItems(boolean isUserReviewer, UserList toCopy) {
    UserList copy2 = new UserList(toCopy);
    for (UserExercise ue : toCopy.getExercises()) {
      if (true) {//isUserReviewer || ue.getCreator() == controller.getUser()) {
        copy2.addExercise(ue);
      }
    }
    return copy2;
  }

  /**
   * @see #editItem(mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.HasText, boolean, boolean)
   * @param right
   * @param instanceName
   * @param ul
   * @param originalList
   * @param includeAddItem
   * @return
   */
  private PagingExerciseList<T> makeExerciseList(Panel right, String instanceName, UserList ul, UserList originalList,
                                                 final boolean includeAddItem) {
    System.out.println("EditItem.makeExerciseList - ul = " + ul.getName() + " " + includeAddItem);

    if (includeAddItem) {
      UserExercise newItem = getNewItem();
      System.out.println("makeExerciseList : Adding " + newItem + " with " + newItem.getTooltip());
      ul.addExercise(newItem);
    }

    final PagingExerciseList<T> exerciseList =
      new PagingExerciseList<T>(right, service, feedback, false, false, controller,
        true, instanceName) {
        @Override
        protected void onLastItem() {
          new ModalInfoDialog("Complete", "List complete!", new HiddenHandler() {
            @Override
            public void onHidden(HiddenEvent hiddenEvent) {
              reloadExercises();
            }
          });
        }

        @Override
        protected void askServerForExercise(String itemID) {
          if (itemID.equals(NEW_EXERCISE_ID)) {
            useExercise(getNewItem().toExercise());
          }
          else {
            super.askServerForExercise(itemID);
          }
        }

        @Override
        protected void rememberExercises(List<T> result) {
          clear();
          boolean addNewItem = includeAddItem;

          for (final T es : result) {
            addExercise(es);
            if (includeAddItem && es.getID().equals(NEW_EXERCISE_ID)) {
              addNewItem = false;
            }
          }

          if (addNewItem) {
            addExercise((T)getNewItem());  // TODO : fix this
          }
          flush();
        }
      };
    setFactory(exerciseList, ul, originalList);
    exerciseList.setUnaccountedForVertical(220);   // TODO do something better here
   // System.out.println("setting vertical on " +exerciseList.getElement().getId());
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        exerciseList.onResize();
      }
    });
    return exerciseList;
  }

  private UserExercise getNewItem() {
    return new UserExercise(-1, NEW_EXERCISE_ID, userManager.getUser(), NEW_ITEM, "", "");
  }

  private void setFactory(final PagingExerciseList<T> exerciseList, final UserList ul, final UserList originalList) {
    final PagingExerciseList<T> outer = exerciseList;
    T currentExercise = outer.getCurrentExercise();

    exerciseList.setFactory(new ExercisePanelFactory(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(Exercise e) {
        Panel panel = new SimplePanel();
        panel.getElement().setId("EditItemPanel");
        T currentExercise1 = outer.getCurrentExercise();
  //      currentExercise1.
        populatePanel(new UserExercise(e), panel, ul, originalList, itemMarker, outer);
        return panel;
      }
    }, userManager, 1);
  }

  /**
   * @see #editItem
   * @param ul
   * @param npfExerciseList
   */
  private void rememberAndLoadFirst(final UserList ul, PagingExerciseList<T> npfExerciseList) {
    npfExerciseList.setUserListID(ul.getUniqueID());
    List<T> userExercises = new ArrayList<T>();
    for (UserExercise e : ul.getExercises()) {
      userExercises.add((T) e);  // TODO something better here
    }
    npfExerciseList.rememberAndLoadFirst(userExercises);
  }

  private UserExercise newExercise;

  private void populatePanel(UserExercise exercise, final Panel right, final UserList ul, final UserList originalList, final HasText itemMarker,
                             final ListInterface<T> pagingContainer) {
    if (exercise.getID().equals(NEW_EXERCISE_ID)) {
      if (newExercise == null) {
        newExercise = createNewItem(userManager.getUser());
        addEditOrAddPanel(newExercise, itemMarker, originalList, right, ul, pagingContainer, true, false);

      } else {
        addEditOrAddPanel(newExercise, itemMarker, originalList, right, ul, pagingContainer, true, true);
      }
    } else {
      addEditOrAddPanel(exercise, itemMarker, originalList, right, ul, pagingContainer, false, true);
    }
  }

  private UserExercise createNewItem(long userid) {
    return new UserExercise(-1, UserExercise.CUSTOM_PREFIX+Long.MAX_VALUE, userid, "", "", "");
  }

  private void addEditOrAddPanel(UserExercise newExercise, HasText itemMarker, UserList originalList,
                                   Panel right, UserList ul, ListInterface<T> pagingContainer, boolean doNewExercise, boolean setFields) {
    NewUserExercise<T> editableExercise = getAddOrEditPanel(newExercise, itemMarker, originalList, doNewExercise);
    right.add(editableExercise.addNew(ul, originalList, pagingContainer, right));
    if (setFields) editableExercise.setFields(newExercise);
  }

  public void clearNewExercise() {  this.newExercise = null;  }

  /**
   * @see #populatePanel(mitll.langtest.shared.custom.UserExercise, com.google.gwt.user.client.ui.Panel, mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.HasText, mitll.langtest.client.list.ListInterface)
   * @param exercise
   * @param itemMarker
   * @param originalList
   * @param doNewExercise
   * @return
   */
  protected NewUserExercise<T> getAddOrEditPanel(UserExercise exercise, HasText itemMarker, UserList originalList, boolean doNewExercise) {
    NewUserExercise<T> editableExercise;
    if (doNewExercise) {
      editableExercise = new NewUserExercise<T>(service, controller, itemMarker, this, exercise);
    } else {
      String id = exercise.getID();
      int user = controller.getUser();

      long creator = getCreator(originalList, id);
      if (creator == user) {
        editableExercise = new EditableExercise(itemMarker, exercise, originalList);
      }
      else {
        editableExercise = new NewUserExercise<T>(service, controller, itemMarker, this, exercise) {
          @Override
          public Panel addNew(final UserList ul, final UserList originalList, ListInterface<T> listInterface, Panel toAddTo) {
            final FluidContainer container = new FluidContainer();

            this.ul = ul;
            this.originalList = originalList;
            this.listInterface = listInterface;

            Button delete = new Button(REMOVE_FROM_LIST);
            DOM.setStyleAttribute(delete.getElement(), "marginRight", "5px");
            delete.setType(ButtonType.WARNING);
            delete.addStyleName("floatRight");
            container.add(delete);

            delete.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                deleteItem(newUserExercise.getID(), originalList.getUniqueID(), ul, exerciseList, npfHelper);
              }
            });
            delete.addStyleName("topFiftyMargin");
            return container;
          }

          @Override
          protected void setFields(UserExercise newUserExercise) {

          }
        };

      }
    }
    return editableExercise;
  }

  private long getCreator(UserList originalList, String id) {
    for (UserExercise ue : originalList.getExercises()) {
      if (ue.getID().equals(id)) {
        return ue.getCreator();
      }
    }
    return -1;
  }

  protected class EditableExercise extends NewUserExercise<T> {
    private final HTML englishAnno = new HTML();
    private final HTML translitAnno = new HTML();
    private final HTML foreignAnno = new HTML();
    private final HTML fastAnno = new HTML();
    private final HTML slowAnno = new HTML();
    private String originalForeign = "";
    private String originalEnglish = "";
    protected final UserList originalList;

    /**
     *
     * @param itemMarker
     * @param changedUserExercise
     * @param originalList
     * @see #getAddOrEditPanel(mitll.langtest.shared.custom.UserExercise, com.google.gwt.user.client.ui.HasText, mitll.langtest.shared.custom.UserList, boolean)
     */
    public EditableExercise(HasText itemMarker, UserExercise changedUserExercise, UserList originalList) {
      super(EditItem.this.service, EditItem.this.controller, itemMarker, EditItem.this, changedUserExercise);
      fastAnno.addStyleName("editComment");
      slowAnno.addStyleName("editComment");
      this.originalList = originalList;
    }

    @Override
    protected void gotBlur(FormField english, FormField foreignLang, RecordAudioPanel rap,
                           ControlGroup normalSpeedRecording, UserList ul, ListInterface<T> pagingContainer,
                           Panel toAddTo) {
      boolean changed = foreignChanged();
      validateThenPost(foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo, false, changed);
    }

    protected PrevNextList<T> getPrevNext(ListInterface<T> pagingContainer) {
      final T exerciseShell = pagingContainer.byID(newUserExercise.getID());
      return new PrevNextList<T>(exerciseShell, exerciseList, shouldDisableNext());
    }

    @Override
    protected void addItemsAtTop(Panel container) {
      if (!newUserExercise.getUnitToValue().isEmpty()) {
        Panel flow = new HorizontalPanel();
        flow.getElement().setId("addItemsAtTop_unitLesson");
        flow.addStyleName("leftFiveMargin");

        for (String type : controller.getStartupInfo().getTypeOrder()) {
          Heading child = new Heading(4, type, newUserExercise.getUnitToValue().get(type));
          child.addStyleName("rightFiveMargin");
          flow.add(child);
        }

        container.add(flow);
      } else if (ul != null) {        // when could this happen???
        container.add(new Label("List " + ul.getName()));
      }
    }

    protected boolean shouldDisableNext() {  return true; }

    /**
     * Add remove from list button
     *
     * @see mitll.langtest.client.custom.NewUserExercise#addNew(mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel)
     * @param ul
     * @param pagingContainer
     * @param toAddTo
     * @param normalSpeedRecording
     * @return
     */
    @Override
    protected Panel getCreateButton(final UserList ul, ListInterface<T> pagingContainer, Panel toAddTo,
                                    ControlGroup normalSpeedRecording) {
      Panel row = new DivWidget();
      row.addStyleName("marginBottomTen");
      PrevNextList<T> prevNext = getPrevNext(pagingContainer);
      prevNext.addStyleName("floatLeft");
      prevNext.addStyleName("rightFiveMargin");
      row.add(prevNext);

      Button delete = makeDeleteButton(ul.getUniqueID());

      configureButtonRow(row);
      row.add(delete);

      return row;
    }

    private Button makeDeleteButton(final long uniqueID) {
      Button delete = new Button(REMOVE_FROM_LIST);
      DOM.setStyleAttribute(delete.getElement(), "marginRight", "5px");
      delete.setType(ButtonType.WARNING);
      delete.addStyleName("floatRight");

      delete.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          deleteItem(newUserExercise.getID(), uniqueID, ul, exerciseList, npfHelper);
        }
      });

      return delete;
    }

    /**
     * @param container
     * @return
     */
    @Override
    protected Panel makeEnglishRow(Panel container) {
      Panel row = new FluidRow();
      container.add(row);
      english = makeBoxAndAnno(row, ENGLISH_LABEL, englishAnno);
      return row;
    }

    /**
     * @param container
     */
    @Override
    protected FormField makeForeignLangRow(Panel container) {
      Panel row = new FluidRow();
      container.add(row);
      foreignLang = makeBoxAndAnno(row, controller.getLanguage(), foreignAnno);
      foreignLang.box.setDirectionEstimator(true);   // automatically detect whether text is RTL
      return foreignLang;
    }

    @Override
    protected void makeTranslitRow(Panel container) {
      Panel row = new FluidRow();
      container.add(row);
      translit = makeBoxAndAnno(row, TRANSLITERATION_OPTIONAL, translitAnno);
    }

    /**
     * @see mitll.langtest.client.custom.NewUserExercise#addNew(mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel)
     * @param row
     * @return
     */
    @Override
    protected ControlGroup makeRegularAudioPanel(Panel row) {
      rap = makeRecordAudioPanel(row, true);
      fastAnno.addStyleName("topFiveMargin");
      return addControlGroupEntrySimple(row, NORMAL_SPEED_REFERENCE_RECORDING, rap, fastAnno);
    }

    /**
     *
     * @param row
     */
    @Override
    protected ControlGroup makeSlowAudioPanel(Panel row) {
      rapSlow = makeRecordAudioPanel(row, false);
      slowAnno.addStyleName("topFiveMargin");

      return addControlGroupEntrySimple(row, SLOW_SPEED_REFERENCE_RECORDING_OPTIONAL, rapSlow, slowAnno);
    }

    private BasicDialog.FormField makeBoxAndAnno(Panel row, String label, HTML englishAnno) {
      BasicDialog.FormField formField = addControlFormField(row, label, false, 1, englishAnno);
      englishAnno.addStyleName("leftFiveMargin");
      englishAnno.addStyleName("editComment");
      return formField;
    }

    /**
     * @see #checkValidForeignPhrase
     * @param ul
     * @param exerciseList
     * @param toAddTo
     * @param onClick
     */
    @Override
    protected void afterValidForeignPhrase(final UserList ul, final ListInterface<T> exerciseList, final Panel toAddTo, boolean onClick) {
      System.out.println("EditItem.afterValidForeignPhrase : exercise id " + newUserExercise.getID());
      checkForForeignChange();

      postChangeIfDirty(exerciseList, onClick);
    }

    @Override
    protected void formInvalid() {
      postChangeIfDirty(exerciseList, false);
    }

    private void postChangeIfDirty(ListInterface<T> exerciseList, boolean onClick) {
      if (foreignChanged() || translitChanged() || englishChanged() || refAudioChanged() || slowRefAudioChanged() || onClick) {
        System.out.println("postChangeIfDirty:  change " + foreignChanged() + translitChanged() + englishChanged() + refAudioChanged() + slowRefAudioChanged());
        reallyChange(exerciseList, onClick);
      }
    }

    /**
     * So check if the audio is the original audio and the translation has changed.
     * If the translation is new but the audio isn't, ask and clear
     *
     * @paramx listener
     * @return
     */
    protected boolean checkForForeignChange() {
      if (foreignChanged()) {
        if (!refAudioChanged() && newUserExercise.getRefAudio() != null) {
          markError(normalSpeedRecording, "Consistent with " + controller.getLanguage() + "?", "Is the audio consistent with \"" + foreignLang.getText() + "\" ?");
        }
        if (!slowRefAudioChanged() && newUserExercise.getSlowAudioRef() != null) {
          markError(slowSpeedRecording, "Consistent with " + controller.getLanguage() + "?", "Is the audio consistent with \"" + foreignLang.getText() + "\" ?");
        }
        if (!translitChanged()) {
          markError(translit, "Is the transliteration consistent with \"" + foreignLang.getText() + "\" ?");
        }
        return true;
      } else return false;
    }

    private boolean englishChanged() {
      return !english.box.getText().equals(originalEnglish);
    }

    private boolean foreignChanged() {
      boolean b = !foreignLang.box.getText().equals(originalForeign);
      if (b)
        System.out.println("foreignChanged : foreign " + foreignLang.box.getText() + " != original " + originalForeign);

      return b;
    }

    private boolean translitChanged() {
      return !newUserExercise.getTransliteration().equals(originalTransliteration); }

    private boolean refAudioChanged() {
      String refAudio = newUserExercise.getRefAudio();
      return (refAudio == null && originalRefAudio != null) || (refAudio != null && !refAudio.equals(originalRefAudio));
    }

    private boolean slowRefAudioChanged() {
      String slowAudioRef = newUserExercise.getSlowAudioRef();
      return
        (slowAudioRef == null && originalSlowRefAudio != null) ||
        (slowAudioRef != null && !slowAudioRef.equals(originalSlowRefAudio));
    }

    /**
     * @see #postChangeIfDirty(mitll.langtest.client.list.ListInterface, boolean)
     * @see #audioPosted()
     * @param pagingContainer
     * @param buttonClicked
     */
    protected void reallyChange(final ListInterface<T> pagingContainer, final boolean buttonClicked) {
      newUserExercise.setCreator(controller.getUser());
      postEditItem(pagingContainer, buttonClicked);
    }

    /**
     * @see #reallyChange(mitll.langtest.client.list.ListInterface, boolean)
     * @param pagingContainer
     * @param buttonClicked
     */
    private void postEditItem(final ListInterface<T> pagingContainer, final boolean buttonClicked) {
      //System.out.println("postEditItem : edit item " + buttonClicked);

      grabInfoFromFormAndStuffInfoExercise();

      service.editItem(newUserExercise, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Void newExercise) {
          originalForeign = newUserExercise.getForeignLanguage();
          originalEnglish = newUserExercise.getEnglish();
          originalTransliteration = newUserExercise.getTransliteration();
          originalRefAudio = newUserExercise.getRefAudio();
          originalSlowRefAudio = newUserExercise.getSlowAudioRef();
         // System.out.println("postEditItem : onSuccess " + newUserExercise.getTooltip());

          doAfterEditComplete(pagingContainer, buttonClicked);
        }
      });
    }

    /**
     * Tell predefined list to update itself... since maybe a pre def item changed...
     *
     * @see #reallyChange(mitll.langtest.client.list.ListInterface, boolean)
     * @param pagingContainer
     * @param buttonClicked
     */
    protected void doAfterEditComplete(ListInterface<T> pagingContainer, boolean buttonClicked) {
      System.out.println("doAfterEditComplete : change tooltip " + buttonClicked + " id " + predefinedContentList.getCurrentExerciseID());

      changeTooltip(pagingContainer);
      predefinedContentList.reloadWith(predefinedContentList.getCurrentExerciseID());
    }

    private void changeTooltip(ListInterface<T> pagingContainer) {
      T byID = pagingContainer.byID(newUserExercise.getID());
      if (byID == null) {
        System.err.println("changeTooltip : huh? can't find exercise with id " + newUserExercise.getID());
      } else {
        String english1 = newUserExercise.getEnglish();
        byID.setTooltip(english1.isEmpty() ? newUserExercise.getForeignLanguage() : english1);
        //System.out.println("changeTooltip : for " + newUserExercise.getID() + " now " + byID.getTooltip());

        pagingContainer.redraw();   // show change to tooltip!
      }
    }

    private String originalRefAudio;
    private String originalSlowRefAudio;
    private String originalTransliteration;

    /**
     * @see #populatePanel
     * @param newUserExercise
     */
    protected void setFields(UserExercise newUserExercise) {
      //System.out.println("grabInfoFromFormAndStuffInfoExercise : setting fields with " + newUserExercise);

      // english
      english.box.setText(originalEnglish = newUserExercise.getEnglish());
      ((TextBox) english.box).setVisibleLength(newUserExercise.getEnglish().length() + 4);
      if (newUserExercise.getEnglish().length() > 20) {
        english.box.setWidth("500px");
      }
      useAnnotation(newUserExercise, "english", englishAnno);

      // foreign lang
      String foreignLanguage = newUserExercise.getForeignLanguage();
      foreignLanguage = foreignLanguage.trim();
      foreignLang.box.setText(originalForeign = foreignLanguage);
      useAnnotation(newUserExercise, "foreignLanguage", foreignAnno);

      // translit
      translit.box.setText(originalTransliteration = newUserExercise.getTransliteration());
      useAnnotation(newUserExercise, "transliteration", translitAnno);

      Exercise exercise = newUserExercise.toExercise();

      // regular speed audio
      rap.getPostAudioButton().setExercise(exercise);
      String refAudio = exercise.getRefAudio();

      if (refAudio != null) {
        ExerciseAnnotation annotation = newUserExercise.getAnnotation(refAudio);
        if (annotation == null) {
          useAnnotation(newUserExercise.getAnnotation("refAudio"), fastAnno);
        }
        else {
          useAnnotation(newUserExercise, refAudio, fastAnno);
        }
        rap.getImagesForPath(refAudio);
        originalRefAudio = refAudio;
      }

      // slow speed audio
      rapSlow.getPostAudioButton().setExercise(exercise);
      String slowAudioRef = exercise.getSlowAudioRef();

      if (slowAudioRef != null) {
        useAnnotation(newUserExercise, slowAudioRef, slowAnno);
        rapSlow.getImagesForPath(slowAudioRef);
        originalSlowRefAudio = slowAudioRef;
      }
      if (!exercise.hasRefAudio()) {
        useAnnotation(newUserExercise, "refAudio", fastAnno);
      }
    }

    private void useAnnotation(UserExercise userExercise, String field, HTML annoField) {
      ExerciseAnnotation anno = userExercise.getAnnotation(field);
      useAnnotation(anno, annoField);
    }

    private void useAnnotation(ExerciseAnnotation anno, HTML annoField) {
      boolean isIncorrect = anno != null && !anno.isCorrect();
      if (isIncorrect) {
        if (anno.comment.isEmpty()) {
          annoField.setHTML("<i>Empty Comment</i>");
        }
        else {
          annoField.setHTML("<i>\"" + anno.comment+ "\"</i>");
        }
      }
      annoField.setVisible(isIncorrect);
    }
  }
}
