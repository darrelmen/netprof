package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.dialog.ModalInfoDialog;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanelFactory;
import mitll.langtest.client.exercise.PagingContainer;
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
  public static final String CHANGE = "Change";
  private final ExerciseController controller;
  private final NPFHelper npfHelper;
  private LangTestDatabaseAsync service;
  private UserManager userManager;
  protected ListInterface<? extends ExerciseShell> predefinedContentList;
  private UserFeedback feedback = null;

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

  private HTML itemMarker;
  protected PagingExerciseList<T> exerciseList;

  public Panel editItem(final UserList ul, final HTML itemMarker) {
    Panel hp = new HorizontalPanel();
    SimplePanel pagerOnLeft = new SimplePanel();
    hp.add(pagerOnLeft);
    pagerOnLeft.addStyleName("rightFiveMargin");
    final SimplePanel contentOnRight = new SimplePanel();
    hp.add(contentOnRight);

    this.itemMarker = itemMarker; // TODO : something less awkward
    exerciseList = makeExerciseList(contentOnRight, "editItem", ul);
    pagerOnLeft.add(exerciseList.getExerciseListOnLeftSide(controller.getProps()));

    rememberAndLoadFirst(ul, exerciseList);
    return hp;
  }

  private PagingExerciseList<T> makeExerciseList(Panel right, String instanceName, UserList ul) {
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
      };
    setFactory(exerciseList, ul);
    return exerciseList;
  }

  protected void setFactory(final PagingExerciseList<T> exerciseList, final UserList ul) {
    final PagingExerciseList<T> outer = exerciseList;
    exerciseList.setFactory(new ExercisePanelFactory(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(Exercise e) {
        Panel panel = new SimplePanel();
        populatePanel(new UserExercise(e), panel, ul, itemMarker, outer.getPagingContainer());
        return panel;
      }
    }, userManager, 1);
  }

  private void rememberAndLoadFirst(final UserList ul, PagingExerciseList<T> npfExerciseList) {
    npfExerciseList.setUserListID(ul.getUniqueID());
    List<T> userExercises = new ArrayList<T>();
    for (UserExercise e : ul.getExercises()) {
      userExercises.add((T) e);  // TODO something better here
    }
    npfExerciseList.rememberAndLoadFirst(userExercises, null);
  }

  protected void populatePanel(UserExercise exercise, Panel right, UserList ul, HTML itemMarker,
                               PagingContainer<T> pagingContainer) {
    EditableExercise newUserExercise = new EditableExercise(itemMarker, exercise);
    right.add(newUserExercise.addNew(ul, pagingContainer, right));
    newUserExercise.setFields();
  }

/*  private void showPopup(String toShow, Widget over) {
    final PopupPanel popupImage = new PopupPanel(true);
    popupImage.add(new HTML(toShow));
    popupImage.showRelativeTo(over);
    Timer t = new Timer() {
      @Override
      public void run() {
        popupImage.hide();
      }
    };
    t.schedule(3000);
  }*/

  protected class EditableExercise extends NewUserExercise<T> {
    HTML englishAnno = new HTML();
    HTML translitAnno = new HTML();
    HTML foreignAnno = new HTML();
    HTML fastAnno = new HTML();
    HTML slowAnno = new HTML();
    String originalForeign = "";
    UserList ul;

    /**
     * @param itemMarker
     * @param changedUserExercise
     * @seex EditItem#editItem(mitll.langtest.shared.custom.UserExercise, com.google.gwt.user.client.ui.SimplePanel, mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.HTML, mitll.langtest.shared.custom.UserExercise)
     */
    public EditableExercise(HTML itemMarker, UserExercise changedUserExercise) {
      super(EditItem.this.service, EditItem.this.userManager, EditItem.this.controller, itemMarker);
      this.newUserExercise = changedUserExercise;
      fastAnno.addStyleName("editComment");
      slowAnno.addStyleName("editComment");
    }

    @Override
    protected void gotBlur(FormField english, FormField foreignLang, RecordAudioPanel rap,
                           ControlGroup normalSpeedRecording, UserList ul, PagingContainer<T> pagingContainer, Panel toAddTo, String buttonName) {
      validateThenPost(english, foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo, buttonName);
    }

    @Override
    protected String getButtonName() {
      return CHANGE;
    }

    /**
     * @param ul
     * @param pagingContainer
     * @param toAddTo
     * @return
     */
    @Override
    public Panel addNew(UserList ul, PagingContainer<T> pagingContainer, Panel toAddTo) {
      final Panel widgets = super.addNew(ul, pagingContainer, toAddTo);
      final T exerciseShell = pagingContainer.getByID(newUserExercise.getID());
      widgets.add(new PrevNextList<T>(exerciseShell, exerciseList, shouldDisableNext()));
      this.ul = ul;
      return widgets;
    }

    protected boolean shouldDisableNext() {  return true;    }

    /**
     * Add remove from list button
     * @param ul
     * @param pagingContainer
     * @param toAddTo
     * @param normalSpeedRecording
     * @param buttonName
     * @return
     */
    @Override
    protected Panel getCreateButton(final UserList ul, PagingContainer<T> pagingContainer, Panel toAddTo,
                                    ControlGroup normalSpeedRecording, String buttonName
    ) {
      //Button submit = makeCreateButton(ul, pagingContainer, toAddTo, english, foreignLang, rap, normalSpeedRecording, buttonName);

      Panel row = new DivWidget();
      row.addStyleName("marginBottomTen");
      //submit.addStyleName("floatRight");

      Button delete = new Button("Remove from list");
      DOM.setStyleAttribute(delete.getElement(), "marginRight", "5px");
      delete.setType(ButtonType.WARNING);
      delete.addStyleName("floatRight");

      row.add(delete);
      //row.add(submit);

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
/*      foreignLang.box.addBlurHandler(new BlurHandler() {
        @Override
        public void onBlur(BlurEvent event) {
          validateThenPost(english, foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo, buttonName);
        }
      });*/
      return null;
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
     * @see #onClick(mitll.langtest.shared.custom.UserList, mitll.langtest.client.exercise.PagingContainer, com.google.gwt.user.client.ui.Panel, String)
     * @param ul
     * @param pagingContainer
     * @param toAddTo
     * @param buttonName
     */
    @Override
    protected void afterValidForeignPhrase(final UserList ul, final PagingContainer<T> pagingContainer, final Panel toAddTo,
                                           final String buttonName) {
      if (!checkForForeignChange(getCloseListener(pagingContainer, buttonName))) {
        if (foreignChanged() && translitUnchanged()) {
          markError(translit, "Is transliteration consistent with " + controller.getLanguage() + "?");
        } else {
          reallyChange(pagingContainer, buttonName);
        }
      }
    }

    protected DialogHelper.CloseListener getCloseListener(final PagingContainer<T> pagingContainer, final String buttonName) {
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
          reallyChange(pagingContainer, buttonName);
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

    private boolean foreignChanged() {
      return !foreignLang.box.getText().equals(originalForeign);
    }

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

    private void reallyChange(final PagingContainer<T> pagingContainer, final String buttonName) {
      newUserExercise.setCreator(controller.getUser());
      //System.out.println("reallyChange : " + newUserExercise.getID());

      service.editItem(newUserExercise, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Void newExercise) {
          //  System.out.println("\treallyChange : " + newUserExercise.getID() + " button " + buttonName);

          doAfterEditComplete(pagingContainer, buttonName);
        }
      });
    }

    protected void doAfterEditComplete(PagingContainer<T> pagingContainer, String buttonName) {
      changeTooltip(pagingContainer);
      originalForeign = newUserExercise.getForeignLanguage();

/*      String tooltip = newUserExercise.getTooltip();
      if (tooltip.length() > 30) tooltip = tooltip.substring(0, 30) + "...";
      showPopup(tooltip + " has been updated.", submit);*/
      predefinedContentList.reload();
    }

    private void changeTooltip(PagingContainer<T> pagingContainer) {
      T byID = pagingContainer.getByID(newUserExercise.getID());
      if (byID == null) {
        System.err.println("changeTooltip : huh? can't find exercise with id " + newUserExercise.getID());
      } else {
        byID.setTooltip(newUserExercise.getEnglish());
        pagingContainer.refresh();   // show change to tooltip!
      }
    }

    private String originalRefAudio;
    private String originalSlowRefAudio;
    private String originalTransliteration;

    /**
     * @seez mitll.langtest.client.custom.EditItem#editItem(mitll.langtest.shared.custom.UserExercise, com.google.gwt.user.client.ui.SimplePanel, mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.HTML, mitll.langtest.shared.custom.UserExercise)
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

      //submit.setText(CHANGE);
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
