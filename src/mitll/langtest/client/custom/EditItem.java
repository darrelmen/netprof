package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.event.HiddenEvent;
import com.github.gwtbootstrap.client.ui.event.HiddenHandler;
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
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class EditItem {
  private final ExerciseController controller;
  private LangTestDatabaseAsync service;
  private UserManager userManager;
  //private PagingContainer<UserExercise> pagingContainer;
  private ListInterface predefinedContentList;
  private UserFeedback feedback = null;

  /**
   * @see mitll.langtest.client.custom.Navigation#Navigation
   * @param service
   * @param userManager
   * @param controller
   */
  public EditItem(final LangTestDatabaseAsync service, final UserManager userManager, ExerciseController controller,
                  ListInterface listInterface, UserFeedback feedback) {
    this.controller = controller;
    this.service = service;
    this.userManager = userManager;
    this.predefinedContentList = listInterface;
    this.feedback = feedback;
  }

  /**
   * @see mitll.langtest.client.custom.Navigation#showEditItem(mitll.langtest.shared.custom.UserList, mitll.langtest.client.custom.Navigation.TabAndContent)
   * @param ul
   * @param itemMarker
   * @return
   */
/*  public Panel editItemOld(final UserList ul, final HTML itemMarker) {
    Panel hp = new HorizontalPanel();
    SimplePanel pagerOnLeft = new SimplePanel();
    hp.add(pagerOnLeft);
    pagerOnLeft.addStyleName("rightFiveMargin");
    final SimplePanel contentOnRight = new SimplePanel();
    hp.add(contentOnRight);

    pagingContainer = new PagingContainer<UserExercise>(controller, 100) {
      @Override
      protected void gotClickOnItem(UserExercise exerciseShell) {
        showItem(exerciseShell, contentOnRight, ul, itemMarker);
      }
    };

    pagerOnLeft.add(getPager(ul));
    showInitialItem(ul, itemMarker, contentOnRight);
    return hp;
  }*/

  HTML itemMarker;
  PagingExerciseList<UserExercise> exerciseList;
  public Panel editItem(final UserList ul, final HTML itemMarker) {
    Panel hp = new HorizontalPanel();
    SimplePanel pagerOnLeft = new SimplePanel();
    hp.add(pagerOnLeft);
    pagerOnLeft.addStyleName("rightFiveMargin");
    final SimplePanel contentOnRight = new SimplePanel();
    hp.add(contentOnRight);

/*    pagingContainer = new PagingContainer<UserExercise>(controller, 100) {
      @Override
      protected void gotClickOnItem(UserExercise exerciseShell) {
        showItem(exerciseShell, contentOnRight, ul, itemMarker);
      }
    };*/

  //  pagerOnLeft.add(getPager(ul));

    this.itemMarker = itemMarker; // TODO : something less awkward
    exerciseList = makeExerciseList(contentOnRight, "editItem",ul);
    pagerOnLeft.add(exerciseList.getExerciseListOnLeftSide(controller.getProps()));

    //showInitialItem(ul, itemMarker, contentOnRight);
    rememberAndLoadFirst(ul, exerciseList);
    return hp;
  }

  private PagingExerciseList<UserExercise> makeExerciseList(Panel right, String instanceName,UserList ul) {
    PagingExerciseList<UserExercise> exerciseList = new PagingExerciseList<UserExercise>(right, service, feedback, false, false, controller,
      true, instanceName) {
      @Override
      protected void onLastItem() {
        new ModalInfoDialog("Complete","List complete!", new HiddenHandler() {
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

  protected void setFactory(final PagingExerciseList<UserExercise> exerciseList, final UserList ul) {
    final PagingExerciseList<UserExercise> outer = exerciseList;
    exerciseList.setFactory(new ExercisePanelFactory(service, feedback, controller, exerciseList) {
      @Override
      public Panel getExercisePanel(Exercise e) {
        Panel panel = new SimplePanel();
        populatePanel(new UserExercise(e),panel,ul,itemMarker,
          //e,
          outer.getPagingContainer());
        return panel;
      }
    },userManager,1);
  }

  private void rememberAndLoadFirst(final UserList ul, PagingExerciseList<UserExercise> npfExerciseList) {
    npfExerciseList.setUserListID(ul.getUniqueID());
    npfExerciseList.rememberAndLoadFirst(new ArrayList<UserExercise>(ul.getExercises()), null);
  }

/*  private Panel getPager(UserList ul) {
    Panel container = pagingContainer.getTableWithPager();
    for (UserExercise es : ul.getExercises()) {
       pagingContainer.addExerciseToList2(es);
    }
    pagingContainer.flush();
    return container;
  }*/

/*  private void showInitialItem(UserList ul, HTML itemMarker, SimplePanel contentOnRight) {
    UserExercise exerciseShell = pagingContainer.selectFirst();
    if (exerciseShell == null) {
      System.err.println("huh? nothing first?");
      showPopup("No editable items yet.",contentOnRight);
    }
    else {
      showItem(exerciseShell, contentOnRight, ul, itemMarker);
    }
  }*/

/*  private void showItem(final UserExercise exerciseShell, final SimplePanel contentOnRight, final UserList ul, final HTML itemMarker) {
    service.getExercise(exerciseShell.getID(), new AsyncCallback<Exercise>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Exercise result) {
        editItem(new UserExercise(result), contentOnRight, ul, itemMarker, exerciseShell);
      }
    });
  }*/

/*  private void editItem(final UserExercise exercise, SimplePanel right, UserList ul, HTML itemMarker,UserExercise exerciseShell) {
    right.clear();

    populatePanel(exercise, right, ul, itemMarker, exerciseShell);
  }*/

  private void populatePanel(UserExercise exercise, Panel right, UserList ul, HTML itemMarker,
                             //UserExercise exerciseShell,
                             PagingContainer<UserExercise> pagingContainer) {
    EditableExercise newUserExercise = new EditableExercise(itemMarker, exercise
    //  , exerciseShell
    );
    right.add(newUserExercise.addNew(ul, pagingContainer, right));
    newUserExercise.setFields();
  }

  private void showPopup(String toShow, Widget over) {
    final PopupPanel popupImage = new PopupPanel(true);
    popupImage.add(new HTML(toShow));
    popupImage.showRelativeTo(over);
    Timer t = new Timer() {
      @Override
      public void run() { popupImage.hide(); }
    };
    t.schedule(3000);
  }

  private class EditableExercise extends NewUserExercise<UserExercise> {
    //UserExercise exerciseFromList;
    HTML englishAnno = new HTML();
    HTML translitAnno = new HTML();
    HTML foreignAnno = new HTML();
    HTML fastAnno = new HTML();  // TODO add these
    HTML slowAnno = new HTML();
    String originalForeign = "";

    /**
     * @seex EditItem#editItem(mitll.langtest.shared.custom.UserExercise, com.google.gwt.user.client.ui.SimplePanel, mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.HTML, mitll.langtest.shared.custom.UserExercise)
     * @param itemMarker
     * @param changedUserExercise
     */
    public EditableExercise(HTML itemMarker, UserExercise changedUserExercise/*, UserExercise exerciseFromList*/) {
      super(EditItem.this.service, EditItem.this.userManager, EditItem.this.controller, itemMarker);
      this.newUserExercise = changedUserExercise;
     // this.exerciseFromList = exerciseFromList;
      fastAnno.addStyleName("editComment");
      slowAnno.addStyleName("editComment");
    }

    /**
     * TODO : consider relaxing restriction on type of pagingcontainer
     * @param ul
     * @param pagingContainer
     * @param toAddTo
     * @return
     */
    @Override
    public Panel addNew(UserList ul, PagingContainer<UserExercise> pagingContainer, Panel toAddTo) {
      Panel widgets = super.addNew(ul, pagingContainer, toAddTo);
      UserExercise byID = pagingContainer.getByID(newUserExercise.getID());
      System.out.println("addNew : Found " + byID);
      widgets.add(new PrevNextList<UserExercise>(byID, exerciseList)); // TODO : dies here when we do prefix search!
      return widgets;
    }

    /**
     *
     * @param container
     * @return
     */
    @Override
    protected Panel makeEnglishRow(Panel container) {
      FluidRow row = new FluidRow();
      container.add(row);
      english = makeBoxAndAnno(row, "English", englishAnno);
      focusOnEnglish(english);
      return row;
    }

    /**
     * @param container
     */
    @Override
    protected void makeForeignLangRow(Panel container) {
      FluidRow row = new FluidRow();
      container.add(row);
      foreignLang = makeBoxAndAnno(row, controller.getLanguage(), foreignAnno);
      foreignLang.box.setDirectionEstimator(true);   // automatically detect whether text is RTL
 /*     foreignLang.box.addBlurHandler(new BlurHandler() {
        @Override
        public void onBlur(BlurEvent event) {
          checkForForeignChange();
        }
      });*/
    }

    @Override
    protected void makeTranslitRow(Panel container) {
      FluidRow row = new FluidRow();
      container.add(row);
      translit = makeBoxAndAnno(row, "Transliteration (optional)", translitAnno);
    }

    @Override
    protected ControlGroup makeRegularAudioPanel(FluidRow row) {
      rap = makeRecordAudioPanel(row, english, foreignLang, true);
      fastAnno.addStyleName("topFiveMargin");
      return addControlGroupEntry(row, "Normal speed reference recording", rap, fastAnno);
    }

    @Override
    protected void makeSlowAudioPanel(FluidRow row) {
      rapSlow = makeRecordAudioPanel(row, english, foreignLang, false);
      slowAnno.addStyleName("topFiveMargin");

      addControlGroupEntry(row, "Slow speed reference recording (optional)", rapSlow, slowAnno);
    }

    private BasicDialog.FormField makeBoxAndAnno(FluidRow row,  String label, HTML englishAnno) {
      BasicDialog.FormField formField = addControlFormField(row, label, false, 1, englishAnno);
      englishAnno.addStyleName("leftFiveMargin");
      englishAnno.addStyleName("editComment");
      return formField;
    }

    /**
     * TODO : check to see if the foreign language is valid before commiting change!
     * @param ul
     * @param pagingContainer
     * @param toAddTo
     */
    @Override
    protected void onClick(final UserList ul, final PagingContainer<UserExercise> pagingContainer, Panel toAddTo) {
      System.out.println("onClick : editing " + newUserExercise);

      DialogHelper.CloseListener listener = new DialogHelper.CloseListener() {
        @Override
        public void gotYes() {
          System.out.println("invalidate the audio...");

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

      if (!checkForForeignChange(listener)) {
        if (foreignChanged() && translitUnchanged()) {
          markError(translit, "Is transliteration consistent with " + controller.getLanguage() + "?");
        } else {
          reallyChange(pagingContainer);
        }
      }
    }

    /**
     * So check if the audio is the original audio and the translation has changed.
     * If the translation is new but the audio isn't, ask and clear
     * @param listener
     * @return
     */
    private boolean checkForForeignChange(DialogHelper.CloseListener listener) {
      if (foreignChanged() &&
        (refAudioUnchanged() || slowRefAudioUnchanged())) {
        new DialogHelper(true).show("Invalidate audio?",
          Arrays.asList("The " + controller.getLanguage() +
          " has changed - should the audio be re-recorded?"), listener);
        return true;
      }
      else return false;
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

    private void reallyChange(final PagingContainer<UserExercise> pagingContainer) {
      newUserExercise.setCreator(controller.getUser());
      service.editItem(newUserExercise, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {}

        @Override
        public void onSuccess(Void newExercise) {
          changeTooltip(pagingContainer);
          originalForeign = newUserExercise.getForeignLanguage();
          String tooltip = newUserExercise.getTooltip();
          if (tooltip.length() > 30) tooltip = tooltip.substring(0,30)+"...";
          showPopup(tooltip + " has been updated.", submit);
          predefinedContentList.reload();
        }
      });
    }

    private void changeTooltip(PagingContainer<UserExercise> pagingContainer) {
      UserExercise byID = pagingContainer.getByID(newUserExercise.getID());
      if (byID == null) {
        System.err.println("huh? can't find exercise with id " +newUserExercise.getID());
      }
      else {
        byID.setEnglish(newUserExercise.getEnglish());
        byID.setForeignLanguage(newUserExercise.getForeignLanguage());
        pagingContainer.refresh();   // show change to tooltip!
      }
    }

    private String originalRefAudio;
    private String originalSlowRefAudio;
    private String originalTransliteration;

    /**
     * @seez mitll.langtest.client.custom.EditItem#editItem(mitll.langtest.shared.custom.UserExercise, com.google.gwt.user.client.ui.SimplePanel, mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.HTML, mitll.langtest.shared.custom.UserExercise)
     */
    private void setFields() {
      System.out.println("setFields : setting fields with " + newUserExercise);

       // english
      english.box.setText(newUserExercise.getEnglish());
      ((TextBox)english.box).setVisibleLength(newUserExercise.getEnglish().length()+4);
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
      useAnnotation(newUserExercise,"transliteration",translitAnno);

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

      submit.setText("Change");
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

/*
  protected Label getCommentLabel() {
    final Label commentLabel = new Label("comment : ");
   // DOM.setStyleAttribute(commentLabel.getElement(), "backgroundColor", "#ff0000");
    commentLabel.setVisible(true);
   // commentLabel.addStyleName("ImageOverlay");
    return commentLabel;
  }*/
}
