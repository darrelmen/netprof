package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
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
  protected static final String NEW_EXERCISE_ID = "NewExerciseID";
  private static final String EDIT_ITEM = "editItem";

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
   * @see mitll.langtest.client.custom.Navigation#showEditItem(mitll.langtest.shared.custom.UserList, mitll.langtest.client.custom.Navigation.TabAndContent, EditItem, boolean)
   * @param originalList
   * @param itemMarker
   * @param includeAddItem
   * @return
   */
  public Panel editItem(UserList originalList, final HasText itemMarker, boolean includeAddItem) {
    Panel hp = new HorizontalPanel();
    SimplePanel pagerOnLeft = new SimplePanel();
    hp.add(pagerOnLeft);
    pagerOnLeft.addStyleName("rightFiveMargin");
    final SimplePanel contentOnRight = new SimplePanel();
    hp.add(contentOnRight);

    this.itemMarker = itemMarker; // TODO : something less awkward

    UserList copy = new UserList(originalList);  // copy before we add to it!

    exerciseList = makeExerciseList(contentOnRight, EDIT_ITEM, copy, originalList, includeAddItem);
    pagerOnLeft.add(exerciseList.getExerciseListOnLeftSide(controller.getProps()));

    rememberAndLoadFirst(copy, exerciseList);
    return hp;
  }

  private PagingExerciseList<T> makeExerciseList(Panel right, String instanceName, UserList ul, UserList originalList,
                                                 final boolean includeAddItem) {
    System.out.println("makeExerciseList - ul = " + ul.getName() + " " + includeAddItem);

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
            UserExercise newItem = getNewItem();
            useExercise(newItem.toExercise());
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
    exerciseList.setUnaccountedForVertical(320);
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
    exerciseList.setFactory(new ExercisePanelFactory(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(Exercise e) {
        Panel panel = new SimplePanel();
        UserExercise userExerciseWrapper = new UserExercise(e);
        populatePanel(userExerciseWrapper, panel, ul, originalList, itemMarker, outer);
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
        System.out.println("EditItem.populatePanel: make new item ");

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
      editableExercise = new EditableExercise(itemMarker, exercise, originalList);
    }
    return editableExercise;
  }

  protected class EditableExercise extends NewUserExercise<T> {
    private final HTML englishAnno = new HTML();
    private final HTML translitAnno = new HTML();
    private final HTML foreignAnno = new HTML();
    private final HTML fastAnno = new HTML();
    private final HTML slowAnno = new HTML();
    private String originalForeign = "";
    private String originalEnglish = "";
    protected UserList ul;
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
      validateThenPost(foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo, false);
    }

    /**
     *
     * @param ul
     * @param originalList
     *@param pagingContainer
     * @param toAddTo
     * @return
     */
    @Override
    public Panel addNew(UserList ul, UserList originalList, ListInterface<T> pagingContainer, Panel toAddTo) {
      final Panel widgets = super.addNew(ul, originalList, pagingContainer, toAddTo);
      final T exerciseShell = pagingContainer.byID(newUserExercise.getID());
      widgets.add(new PrevNextList<T>(exerciseShell, exerciseList, shouldDisableNext()));
      this.ul = ul;
      return widgets;
    }

    @Override
    protected void addItemsAtTop(Panel container) {
      if (!newUserExercise.getUnitToValue().isEmpty()) {
        Panel flow = new HorizontalPanel();
        for (String type : controller.getStartupInfo().getTypeOrder()) {
          flow.getElement().setId("unitLesson");
          flow.addStyleName("leftFiveMargin");

          Heading child = new Heading(4, type, newUserExercise.getUnitToValue().get(type));
          child.addStyleName("rightFiveMargin");
          flow.add(child);
        }
        container.add(flow);
      }
      else if (ul != null) {
        container.add(new Label("List "+ul.getName()));
      }
    }

    protected boolean shouldDisableNext() {  return true; }

    /**
     * Add remove from list button
     *
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

      Button delete = makeDeleteButton();

      row.add(delete);

      final long uniqueID = ul.getUniqueID();
      delete.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          deleteItem(newUserExercise.getID(), uniqueID, ul);
        }
      });
      return row;
    }

    private void deleteItem(final String id, final long uniqueID, final UserList ul) {
      //System.out.println("deleteItem : item " + id + " unique " + uniqueID);

      service.deleteItemFromList(uniqueID, id, new AsyncCallback<Boolean>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Boolean result) {
          if (!result)  System.err.println("deleteItem huh? id " + id + " not in list " + uniqueID);

          exerciseList.forgetExercise(id);
          UserExercise remove = ul.remove(id);
          if (remove == null) {
            System.err.println("deleteItem huh? didn't remove the item " + id);
          }
          if (originalList.remove(id) == null) {
            System.err.println("deleteItem huh? didn't remove the item " + id + " from " + originalList);
          }
          npfHelper.reload();
        }
      });
    }

    private Button makeDeleteButton() {
      Button delete = new Button("Remove from list");
      DOM.setStyleAttribute(delete.getElement(), "marginRight", "5px");
      delete.setType(ButtonType.WARNING);
      delete.addStyleName("floatRight");
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
      translit = makeBoxAndAnno(row, "Transliteration (optional)", translitAnno);
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
      return addControlGroupEntry(row, "Normal speed reference recording", rap, fastAnno);
    }

    /**
     *
     * @param row
     */
    @Override
    protected ControlGroup makeSlowAudioPanel(Panel row) {
      rapSlow = makeRecordAudioPanel(row, false);
      slowAnno.addStyleName("topFiveMargin");

      return addControlGroupEntry(row, "Slow speed reference recording (optional)", rapSlow, slowAnno);
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

      if (foreignChanged() || translitChanged() || englishChanged() || refAudioChanged() || slowRefAudioChanged() || onClick) {
        System.out.println("\t change " + foreignChanged() + translitChanged() + englishChanged() + refAudioChanged() + slowRefAudioChanged());
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
      if (foreignChanged()/* &&
        (refAudioChanged() || slowRefAudioChanged())*/) {
/*        new DialogHelper(true).show("Invalidate audio?",
          Arrays.asList("The " + controller.getLanguage() + " has changed - should the audio be re-recorded?"), listener);*/
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

    private boolean englishChanged() { return !english.box.getText().equals(originalEnglish);  }
    private boolean foreignChanged() { return !foreignLang.box.getText().equals(originalForeign);  }
    private boolean translitChanged() { return !newUserExercise.getTransliteration().equals(originalTransliteration); }

    private boolean refAudioChanged() {
      String refAudio = newUserExercise.getRefAudio();
      return (refAudio == null && originalRefAudio != null) || (refAudio != null && !refAudio.equals(originalRefAudio));
    }

    private boolean slowRefAudioChanged() {
      String slowAudioRef = newUserExercise.getSlowAudioRef();
    //  return slowAudioRef == null || !slowAudioRef.equals(originalSlowRefAudio);
      //          System.out.println("slowRefAudioChanged " + slowAudioRef + " vs " + originalSlowRefAudio);
      return (slowAudioRef == null && originalSlowRefAudio != null) || (slowAudioRef != null && !slowAudioRef.equals(originalSlowRefAudio));

    }

    /**
     * @see NewUserExercise#afterValidForeignPhrase(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, boolean)
     * @param pagingContainer
     * @param buttonClicked
     */
    protected void reallyChange(final ListInterface<T> pagingContainer, final boolean buttonClicked) {
      //System.err.println("reallyChange : exercise id " + newUserExercise.getID());

      newUserExercise.setCreator(controller.getUser());
      postEditItem(pagingContainer, buttonClicked);
    }

    private void postEditItem(final ListInterface<T> pagingContainer, final boolean buttonClicked) {
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

          doAfterEditComplete(pagingContainer, buttonClicked);
        }
      });
    }

    /**
     * @see #reallyChange(mitll.langtest.client.list.ListInterface, boolean)
     * @param pagingContainer
     * @param buttonClicked
     */
    protected void doAfterEditComplete(ListInterface<T> pagingContainer, boolean buttonClicked) {
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
      System.out.println("setFields : setting fields with " + newUserExercise);

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
        annoField.setHTML("<i>\"" + anno.comment + "\"</i>");
      }
      annoField.setVisible(isIncorrect);
    }
  }
}
