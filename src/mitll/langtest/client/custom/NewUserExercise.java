package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.exercise.WaveformPostAudioRecordButton;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.scoring.PostAudioRecordButton;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/6/13
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class NewUserExercise<T extends ExerciseShell> extends BasicDialog {
  private static final String FOREIGN_LANGUAGE = "Foreign Language";
  private static final String CREATE = "Create";
  public static final boolean REQUIRE_ENGLISH = false;
  protected static final String ENGLISH_LABEL = "English (optional)";
  protected UserExercise newUserExercise = null;
  private final ExerciseController controller;
  protected final LangTestDatabaseAsync service;
  private final UserManager userManager;
  private final HasText itemMarker;
  protected BasicDialog.FormField english;
  protected BasicDialog.FormField foreignLang;
  protected BasicDialog.FormField translit;
  protected CreateFirstRecordAudioPanel rap;
  protected CreateFirstRecordAudioPanel rapSlow;

  /**
   * @see EditItem#getAddOrEditPanel
   * @param service
   * @param userManager
   * @param controller
   * @param itemMarker
   */
  public NewUserExercise(final LangTestDatabaseAsync service, final UserManager userManager,
                         ExerciseController controller, HasText itemMarker) {
    this.controller = controller;
    this.service = service;
    this.itemMarker = itemMarker;
    this.userManager = userManager;
  }

  private ControlGroup normalSpeedRecording;
  private UserList ul;
  private UserList originalList;
  private ListInterface<T> listInterface;
  private Panel toAddTo;

  /**
   * @see #afterValidForeignPhrase(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel)
   * @param ul
   * @param originalList
   * @param listInterface
   * @param toAddTo
   * @return
   */
  public Panel addNew(final UserList ul, UserList originalList, final ListInterface<T> listInterface, final Panel toAddTo) {
    final FluidContainer container = new FluidContainer();
    container.addStyleName("greenBackground");

    /*FormField formField =*/ makeForeignLangRow(container);

    //focusOn(formField); // TODO put this back
    makeTranslitRow(container);
    makeEnglishRow(container);

    // make audio row
    FluidRow row = new FluidRow();
    container.add(row);

    normalSpeedRecording = makeRegularAudioPanel(row);
    this.ul = ul;
    this.originalList = originalList;
    this.listInterface = listInterface;
    this.toAddTo = toAddTo;
    makeSlowAudioPanel(row);
    //rap.setOtherRAP(rapSlow.getPostAudioButton());
    //rapSlow.setOtherRAP(rap.getPostAudioButton());

    rap.setOtherRAP(rapSlow);
    rapSlow.setOtherRAP(rap);

    Panel column = getCreateButton(ul, listInterface, toAddTo, normalSpeedRecording);
    row.add(column);

    foreignLang.box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        gotBlur();
      }
    });
    translit.box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        gotBlur();
      }
    });
    english.box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        gotBlur();
      }
    });

    return container;
  }

  private void gotBlur() {
    gotBlur(english, foreignLang, rap, normalSpeedRecording, ul, listInterface, toAddTo);
  }

  protected void gotBlur(FormField english, FormField foreignLang, RecordAudioPanel rap,
                         ControlGroup normalSpeedRecording, UserList ul, ListInterface<T> pagingContainer,
                         Panel toAddTo) {}

  protected ControlGroup makeRegularAudioPanel(Panel row) {
    rap = makeRecordAudioPanel(row, english, foreignLang, true);
    return addControlGroupEntry(row, "Normal speed reference recording", rap);
  }

  protected void makeSlowAudioPanel(Panel row) {
    rapSlow = makeRecordAudioPanel(row, english, foreignLang, false);
    addControlGroupEntry(row, "Slow speed reference recording (optional)", rapSlow);
  }

  protected Panel makeEnglishRow(Panel container) {
    FluidRow row = new FluidRow();
    container.add(row);
    english = addControlFormField(row, ENGLISH_LABEL, false, 1);

    return row;
  }

  protected FormField makeForeignLangRow(Panel container) {
    FluidRow row = new FluidRow();
    container.add(row);
    foreignLang = addControlFormField(row, controller.getLanguage(),false,1);
    foreignLang.box.setDirectionEstimator(true);   // automatically detect whether text is RTL
    return foreignLang;
  }

  protected void makeTranslitRow(Panel container) {
    FluidRow row = new FluidRow();
    container.add(row);
    translit = addControlFormField(row, "Transliteration (optional)",false,0);
 //   translit.box.setDirectionEstimator(false);   // automatically detect whether text is RTL
  }
/*
  private void focusOn(final FormField form) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        form.box.setFocus(true);
      }
    });
  }*/

  protected void setFields() {}

  /**
   * @see #addNew
   * @param ul
   * @param pagingContainer
   * @param toAddTo
   * @param normalSpeedRecording
   * @return
   */
  protected Panel getCreateButton(UserList ul, ListInterface<T> pagingContainer, Panel toAddTo,
                                  ControlGroup normalSpeedRecording
  ) {
    Button submit = makeCreateButton(ul, pagingContainer, toAddTo, english, foreignLang, rap, normalSpeedRecording,
      CREATE);
    DOM.setStyleAttribute(submit.getElement(), "marginBottom", "5px");
    DOM.setStyleAttribute(submit.getElement(), "marginRight", "15px");

    Column column = new Column(2, 9, submit);
    column.addStyleName("topMargin");
    return column;
  }

  private Button makeCreateButton(final UserList ul, final ListInterface<T> pagingContainer, final Panel toAddTo,
                                  final FormField english, final FormField foreignLang,
                                  final RecordAudioPanel rap, final ControlGroup normalSpeedRecording, final String buttonName) {
    Button submit = new Button(buttonName);
    submit.setType(ButtonType.SUCCESS);
    submit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        validateThenPost(english, foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo);
      }
    });
    submit.addStyleName("rightFiveMargin");
    return submit;
  }

  protected void validateThenPost(FormField english, FormField foreignLang, RecordAudioPanel rap,
                                  ControlGroup normalSpeedRecording, UserList ul, ListInterface<T> pagingContainer,
                                  Panel toAddTo) {
    if (validateForm(english, foreignLang, rap, normalSpeedRecording)) {
      onClick(ul, pagingContainer, toAddTo);
    }
  }

  private void onClick(final UserList ul, final ListInterface<T> pagingContainer, final Panel toAddTo) {
    System.out.println("onClick : adding/changing " + newUserExercise);

    service.isValidForeignPhrase(foreignLang.box.getText(), new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Boolean result) {
        if (result) {
          newUserExercise.setEnglish(english.getText());
          newUserExercise.setForeignLanguage(foreignLang.getText());
          newUserExercise.setTransliteration(translit.getText());
          afterValidForeignPhrase(ul, pagingContainer, toAddTo);
        }
        else {
          markError(foreignLang, "The " + FOREIGN_LANGUAGE +
            " text is not in our " + controller.getLanguage() + " dictionary. Please edit.");
        }
      }
    });
  }

  /**
   * @see #onClick
   * @param ul
   * @param exerciseList
   * @param toAddTo
   */
  protected void afterValidForeignPhrase(final UserList ul,
                                         final ListInterface<T> exerciseList,
                                         final Panel toAddTo) {
    service.reallyCreateNewItem(ul.getUniqueID(), newUserExercise, new AsyncCallback<UserExercise>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(UserExercise newExercise) {
        UserExercise newUserExercisePlaceholder = ul.remove(EditItem.NEW_EXERCISE_ID);
        System.out.println("tooltip "+ newExercise.getTooltip());
        ul.addExercise(newExercise);
        originalList.addExercise(newExercise);
        ul.addExercise(newUserExercisePlaceholder); // make sure the placeholder is always at the end
        itemMarker.setText(ul.getExercises().size() + " items");

        T toMoveToEnd = exerciseList.simpleRemove(EditItem.NEW_EXERCISE_ID);
        exerciseList.addExercise((T)newExercise);  // TODO figure out better type safe way of doing this
        exerciseList.addExercise(toMoveToEnd);
        exerciseList.redraw();

        exerciseList.checkAndAskServer(toMoveToEnd.getID());

        toAddTo.clear();
        toAddTo.add(addNew(ul, originalList, exerciseList, toAddTo));
        newUserExercise = null;
      }
    });
  }

  protected CreateFirstRecordAudioPanel makeRecordAudioPanel(final Panel row, final FormField english,
                                                           final FormField foreignLang,
                                                           boolean recordRegularSpeed) {
    return new CreateFirstRecordAudioPanel(row, english, foreignLang, recordRegularSpeed);
  }

  protected class CreateFirstRecordAudioPanel extends RecordAudioPanel {
    private final FormField english;
    private final FormField foreignLang;
    boolean recordRegularSpeed = true;
    private RecordAudioPanel otherRAP;
    private WaveformPostAudioRecordButton postAudioButton;

    public CreateFirstRecordAudioPanel(Panel row, FormField english, FormField foreignLang, boolean recordRegularSpeed) {
      super(null, NewUserExercise.this.controller, row, NewUserExercise.this.service, 0, false);
      this.english = english;
      this.foreignLang = foreignLang;
      this.recordRegularSpeed = recordRegularSpeed;

      addPlayListener(new PlayListener() {
        @Override
        public void playStarted() {
          otherRAP.setEnabled(false);
        }

        @Override
        public void playStopped() {
          System.out.println("CreateFirstRecordAudioPanel.playStopped on " + getElement().getId());

          otherRAP.setEnabled(true);
        }
      });
    }

    @Override
    protected void getEachImage(int width) {
      float newWidth = Window.getClientWidth() * 0.65f;
      super.getEachImage((int) newWidth);
    }

    /**
     * Note that we want to post the audio the server, but not record in the results table (since it's not an answer
     * to an exercise...)
     * That's the final "false" on the end of the WaveformPostAudioRecordButton
     * @see mitll.langtest.client.exercise.RecordAudioPanel#makePlayAudioPanel(com.google.gwt.user.client.ui.Widget)
     * @return
     */
    @Override
    protected WaveformPostAudioRecordButton makePostAudioRecordButton() {
      postAudioButton =
        new WaveformPostAudioRecordButton(exercise, controller, exercisePanel, this, service, recordRegularSpeed ? 0:1,
          false // don't record in results table
        ) {
          @Override
          public void stopRecording() {
            otherRAP.setEnabled(true);
            showStop();
            System.out.println("WaveformPostAudioRecordButton.stopRecording with newUserExercise " + newUserExercise + " and exercise " + exercise);
            if (newUserExercise == null) {
              // first we need to create an item to attach audio to it
              NewUserExercise.this.service.createNewItem(userManager.getUser(), english.getText(), foreignLang.getText(),
                "", new AsyncCallback<UserExercise>() {
                @Override
                public void onFailure(Throwable caught) { System.out.println("onFailure : stopRecording  " + caught); }

                @Override
                public void onSuccess(UserExercise newExercise) {
                  newUserExercise = newExercise;
                  System.out.println("\tonSuccess : stopRecording with newUserExercise " + newUserExercise);

                  exercise = newExercise.toExercise();
                  otherRAP.setExercise(exercise);
                  setExercise(exercise);
                  stopRecording();
                }
              });
            } else {
              System.out.println("\t\tonSuccess : stopRecording with newUserExercise " + newUserExercise +
                " and exercise " + exercise);

              super.stopRecording();
            }
          }

          @Override
          public void startRecording() {
            super.startRecording();
            showStart();
            otherRAP.setEnabled(false);
          }

          @Override
          public void flip(boolean first) {
            super.flip(first);
            flipRecordImages(first);
          }

          @Override
          public void useResult(AudioAnswer result) {
            super.useResult(result);
            if (recordRegularSpeed) {
              newUserExercise.setRefAudio(result.path);
            } else {
              newUserExercise.setSlowRefAudio(result.path);
            }
            System.out.println("newUserExercise " + newUserExercise + " path " + result.path);
            audioPosted();
          }
      };
      postAudioButton.getElement().setId("NewUserExercise_WaveformPostAudioRecordButton");
      return postAudioButton;
    }

    /**
     * @see #addNew(mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel)
     * @param otherRAP
     */
/*    public void setOtherRAP(PostAudioRecordButton otherRAP) {
      this.otherRAP = otherRAP;
    }*/
    public void setOtherRAP(RecordAudioPanel otherRAP) {
      this.otherRAP = otherRAP;
    }
    public WaveformPostAudioRecordButton getPostAudioButton() {
      return postAudioButton;
    }
  }

  private void audioPosted() {
    gotBlur();
  }

  /**
   * Validation checks appear from top to bottom on page -- so should be consistent
   * with how the fields are added.
   *
   * @see #validateThenPost
   * @param english
   * @param foreignLang
   * @param rap
   * @param normalSpeedRecording
   * @return
   */
  private boolean validateForm(final FormField english, final FormField foreignLang, final RecordAudioPanel rap,
                               final ControlGroup normalSpeedRecording) {
    if (foreignLang.getText().isEmpty()) {
      markError(foreignLang, "Enter the foreign language phrase.");
      return false;
    } else if (REQUIRE_ENGLISH && english.getText().isEmpty()) {
      markError(english, "Enter an english word or phrase.");
      return false;
    } else if (newUserExercise == null || newUserExercise.getRefAudio() == null) {
      System.out.println("validateForm : new user ex " + newUserExercise);

      markError(normalSpeedRecording, rap.getButton(), rap.getButton(), "",
        "Record reference audio for the foreign language phrase.");
      rap.getButton().addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent event) {
          normalSpeedRecording.setType(ControlGroupType.NONE);
        }
      });
      return false;
    }
    return true;
  }
}
