package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.DialogHelper;
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
import java.util.Arrays;
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
  private final ExerciseController controller;
  private final NPFHelper npfHelper;
  private final LangTestDatabaseAsync service;
  private final UserManager userManager;
  protected final ListInterface<? extends ExerciseShell> predefinedContentList;
  private UserFeedback feedback = null;
  private HTML itemMarker;
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
  public Panel editItem(UserList originalList, final HTML itemMarker, boolean includeAddItem) {
    Panel hp = new HorizontalPanel();
    SimplePanel pagerOnLeft = new SimplePanel();
    hp.add(pagerOnLeft);
    pagerOnLeft.addStyleName("rightFiveMargin");
    final SimplePanel contentOnRight = new SimplePanel();
    hp.add(contentOnRight);

    this.itemMarker = itemMarker; // TODO : something less awkward

    System.out.println("editItem - including add item = " + includeAddItem);
    UserList copy = new UserList(originalList);  // copy before we add to it!
    if (includeAddItem) {
      UserExercise newItem = new UserExercise(-1, NEW_EXERCISE_ID, userManager.getUser(), NEW_ITEM, "", "");
      System.out.println("editItem : Adding " + newItem + " with " + newItem.getTooltip());
      copy.addExercise(newItem);
    }

    exerciseList = makeExerciseList(contentOnRight, "editItem", copy,originalList);
    pagerOnLeft.add(exerciseList.getExerciseListOnLeftSide(controller.getProps()));

    rememberAndLoadFirst(copy, exerciseList);
    return hp;
  }

  private PagingExerciseList<T> makeExerciseList(Panel right, String instanceName, UserList ul, UserList originalList) {
    PagingExerciseList<T> exerciseList =
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
        protected void askServerForExercise(ExerciseShell exerciseShell) {
          if (exerciseShell.getID().equals(NEW_EXERCISE_ID)) {
            UserExercise newItem = new UserExercise(-1, NEW_EXERCISE_ID, userManager.getUser(), NEW_ITEM, "", "");

            useExercise(newItem.toExercise(),exerciseShell);
          }
          else {
            super.askServerForExercise(exerciseShell);
          }
        }
      };
    setFactory(exerciseList, ul, originalList);
    return exerciseList;
  }

  protected void setFactory(final PagingExerciseList<T> exerciseList, final UserList ul, final UserList originalList) {
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
   * @see #editItem(mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.HTML, boolean)
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

  private void populatePanel(UserExercise exercise, Panel right, UserList ul, UserList originalList, HTML itemMarker,
                             ListInterface<T> pagingContainer) {
    NewUserExercise<T> editableExercise = getAddOrEditPanel(exercise, itemMarker, originalList);
    right.add(editableExercise.addNew(ul, originalList, pagingContainer, right));
    editableExercise.setFields();
  }

  protected NewUserExercise<T> getAddOrEditPanel(UserExercise exercise, HTML itemMarker, UserList originalList) {
    NewUserExercise<T> editableExercise;
    if (exercise.getID().equals(NEW_EXERCISE_ID)) {
      editableExercise = new NewUserExercise<T>(service, userManager, controller, itemMarker);
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
    protected UserList ul;
    protected final UserList originalList;

    /**
     *
     * @param itemMarker
     * @param changedUserExercise
     * @param originalList
     * @seex EditItem#editItem(mitll.langtest.shared.custom.UserExercise, com.google.gwt.user.client.ui.SimplePanel, mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.HTML, mitll.langtest.shared.custom.UserExercise)
     */
    public EditableExercise(HTML itemMarker, UserExercise changedUserExercise, UserList originalList) {
      super(EditItem.this.service, EditItem.this.userManager, EditItem.this.controller, itemMarker);
      this.newUserExercise = changedUserExercise;
      fastAnno.addStyleName("editComment");
      slowAnno.addStyleName("editComment");
      this.originalList = originalList;
    }

    @Override
    protected void gotBlur(FormField english, FormField foreignLang, RecordAudioPanel rap,
                           ControlGroup normalSpeedRecording, UserList ul, ListInterface<T> pagingContainer,
                           Panel toAddTo) {
      validateThenPost(english, foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo);
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

    protected boolean shouldDisableNext() {  return true;    }

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
                                    ControlGroup normalSpeedRecording
    ) {
      Panel row = new DivWidget();
      row.addStyleName("marginBottomTen");

      Button delete = makeDeleteButton();

      row.add(delete);

      final long uniqueID = ul.getUniqueID();
      delete.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          service.deleteItemFromList(uniqueID, newUserExercise.getID(), new AsyncCallback<Boolean>() {
            @Override
            public void onFailure(Throwable caught) {}

            @Override
            public void onSuccess(Boolean result) {
              exerciseList.forgetExercise(newUserExercise.getID());
              ul.remove(newUserExercise.getID());
              npfHelper.reload();
            }
          });
        }
      });
      return row;
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
      FluidRow row = new FluidRow();
      container.add(row);
      english = makeBoxAndAnno(row, "English", englishAnno);
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

    @Override
    protected ControlGroup makeRegularAudioPanel(Panel row) {
      rap = makeRecordAudioPanel(row, english, foreignLang, true);
      fastAnno.addStyleName("topFiveMargin");
      return addControlGroupEntry(row, "Normal speed reference recording", rap, fastAnno);
    }

    @Override
    protected void makeSlowAudioPanel(Panel row) {
      rapSlow = makeRecordAudioPanel(row, english, foreignLang, false);
      slowAnno.addStyleName("topFiveMargin");

      addControlGroupEntry(row, "Slow speed reference recording (optional)", rapSlow, slowAnno);
    }

    private BasicDialog.FormField makeBoxAndAnno(Panel row, String label, HTML englishAnno) {
      BasicDialog.FormField formField = addControlFormField(row, label, false, 1, englishAnno);
      englishAnno.addStyleName("leftFiveMargin");
      englishAnno.addStyleName("editComment");
      return formField;
    }

    /**
     * @see #onClick
     * @param ul
     * @param exerciseList
     * @param toAddTo
     */
    @Override
    protected void afterValidForeignPhrase(final UserList ul, final ListInterface<T> exerciseList, final Panel toAddTo) {
      if (!checkForForeignChange(getCloseListener(exerciseList))) {
        if (foreignChanged() && translitUnchanged()) {
          markError(translit, "Is transliteration consistent with " + controller.getLanguage() + "?");
        } else {
          reallyChange(exerciseList);
        }
      }
    }

    protected DialogHelper.CloseListener getCloseListener(final ListInterface<T> pagingContainer) {
      return new DialogHelper.CloseListener() {
        @Override
        public void gotYes() {
          if (refAudioUnchanged()) {
            newUserExercise.clearRefAudio();
            rap.getWaveform().setVisible(false);
          }
          if (slowRefAudioUnchanged()) {
            newUserExercise.clearSlowRefAudio();
            rapSlow.getWaveform().setVisible(false);
          }
          if (translitUnchanged()) {
            markError(translit, "Is transliteration consistent with " + controller.getLanguage() + "?");
          }

          originalForeign = foreignLang.box.getText();
        }

        @Override
        public void gotNo() {
          reallyChange(pagingContainer);
        }
      };
    }

    /**
     * So check if the audio is the original audio and the translation has changed.
     * If the translation is new but the audio isn't, ask and clear
     *
     * @param listener
     * @return
     */
    private boolean checkForForeignChange(DialogHelper.CloseListener listener) {
      if (foreignChanged() &&
        (refAudioUnchanged() || slowRefAudioUnchanged())) {
        new DialogHelper(true).show("Invalidate audio?",
          Arrays.asList("The " + controller.getLanguage() + " has changed - should the audio be re-recorded?"), listener);
        return true;
      } else return false;
    }

    private boolean foreignChanged() { return !foreignLang.box.getText().equals(originalForeign);  }

    private boolean refAudioUnchanged() {
      String refAudio = newUserExercise.getRefAudio();
      return refAudio != null && refAudio.equals(originalRefAudio);
    }

    private boolean slowRefAudioUnchanged() {
      String slowAudioRef = newUserExercise.getSlowAudioRef();
      return slowAudioRef != null && slowAudioRef.equals(originalSlowRefAudio);
    }

    private boolean translitUnchanged() {
      return newUserExercise.getTransliteration().equals(originalTransliteration);
    }

    private void reallyChange(final ListInterface<T> pagingContainer) {
      newUserExercise.setCreator(controller.getUser());
      service.editItem(newUserExercise, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Void newExercise) {
          //  System.out.println("\treallyChange : " + newUserExercise.getID() + " button " + buttonName);
          doAfterEditComplete(pagingContainer);
        }
      });
    }

    protected void doAfterEditComplete(ListInterface<T> pagingContainer) {
      changeTooltip(pagingContainer);
      originalForeign = newUserExercise.getForeignLanguage();
      predefinedContentList.reloadWith(predefinedContentList.getCurrentExerciseID());
    }

    private void changeTooltip(ListInterface<T> pagingContainer) {
      T byID = pagingContainer.byID(newUserExercise.getID());
      if (byID == null) {
        System.err.println("changeTooltip : huh? can't find exercise with id " + newUserExercise.getID());
      } else {
        byID.setTooltip(newUserExercise.getEnglish());
        pagingContainer.redraw();   // show change to tooltip!
      }
    }

    private String originalRefAudio;
    private String originalSlowRefAudio;
    private String originalTransliteration;

    /**
     * @see EditItem#populatePanel
     * @see EditItem#populatePanel
     */
    protected void setFields() {
      System.out.println("setFields : setting fields with " + newUserExercise);

      // english
      english.box.setText(newUserExercise.getEnglish());
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
        useAnnotation(newUserExercise, refAudio, fastAnno);
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
    }

    private void useAnnotation(UserExercise userExercise, String field, HTML annoField) {
      ExerciseAnnotation anno = userExercise.getAnnotation(field);
      boolean isIncorrect = anno != null && !anno.isCorrect();
      if (isIncorrect) {
        annoField.setHTML("<i>\"" + anno.comment + "\"</i>");
      }
      annoField.setVisible(isIncorrect);
    }
  }
}
