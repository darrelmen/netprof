package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Column;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.TextBox;
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
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.exercise.WaveformPostAudioRecordButton;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;
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
  protected static final String ENGLISH_LABEL = "English (optional)";
  private final EditItem editItem;
  protected final UserExercise newUserExercise;
  protected final ExerciseController controller;
  protected final LangTestDatabaseAsync service;
  private final HasText itemMarker;
  protected BasicDialog.FormField english;
  protected BasicDialog.FormField foreignLang;
  protected BasicDialog.FormField translit;
  protected CreateFirstRecordAudioPanel rap;
  protected CreateFirstRecordAudioPanel rapSlow;

  protected ControlGroup normalSpeedRecording, slowSpeedRecording;
  private UserList ul;
  private UserList originalList;
  protected ListInterface<T> listInterface;
  private Panel toAddTo;
  private boolean clickedCreate = false;

  /**
   * @see EditItem#getAddOrEditPanel
   * @param service
   * @param controller
   * @param itemMarker
   * @param editItem
   * @param newExercise
   */
  public NewUserExercise(final LangTestDatabaseAsync service,
                         ExerciseController controller, HasText itemMarker, EditItem editItem, UserExercise newExercise) {
    this.controller = controller;
    this.service = service;
    this.itemMarker = itemMarker;
    this.editItem = editItem;
    this.newUserExercise = newExercise;
  }


  /**
   * @see #afterValidForeignPhrase(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, boolean)
   * @see EditItem#populatePanel(mitll.langtest.shared.custom.UserExercise, com.google.gwt.user.client.ui.Panel, mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList, com.google.gwt.user.client.ui.HasText, mitll.langtest.client.list.ListInterface)
   *
   * @param ul
   * @param originalList
   * @param listInterface
   * @param toAddTo
   * @return
   */
  public Panel addNew(final UserList ul, UserList originalList, final ListInterface<T> listInterface, final Panel toAddTo) {
    final FluidContainer container = new FluidContainer();
    container.addStyleName("greenBackground");

    addItemsAtTop(container);
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
    slowSpeedRecording = makeSlowAudioPanel(row);

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

  protected void addItemsAtTop(Panel container) {

  }

  private void gotBlur() {
    gotBlur(english, foreignLang, rap, normalSpeedRecording, ul, listInterface, toAddTo);
  }

  protected void gotBlur(FormField english, FormField foreignLang, RecordAudioPanel rap,
                         ControlGroup normalSpeedRecording, UserList ul, ListInterface<T> pagingContainer,
                         Panel toAddTo) {
    newUserExercise.setTransliteration(translit.getText());
    newUserExercise.setForeignLanguage(foreignLang.getText());
    newUserExercise.setEnglish(english.getText());
  }

  /**
   * @see #addNew(mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel)
   * @param row
   * @return
   */
  protected ControlGroup makeRegularAudioPanel(Panel row) {
    rap = makeRecordAudioPanel(row, true);
    return addControlGroupEntry(row, "Normal speed reference recording", rap);
  }

  protected ControlGroup makeSlowAudioPanel(Panel row) {
    rapSlow = makeRecordAudioPanel(row, false);
    return addControlGroupEntry(row, "Slow speed reference recording (optional)", rapSlow);
  }

  protected Panel makeEnglishRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    english = addControlFormField(row, ENGLISH_LABEL, false, 1);

    return row;
  }

  protected FormField makeForeignLangRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    foreignLang = addControlFormField(row, controller.getLanguage(), false, 1);
    foreignLang.box.setDirectionEstimator(true);   // automatically detect whether text is RTL
    return foreignLang;
  }

  protected void makeTranslitRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    translit = addControlFormField(row, "Transliteration (optional)",false,0);
  }

/*
  private void focusOn(final FormField form) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        form.box.setFocus(true);
      }
    });
  }*/

  protected void setFields(UserExercise newUserExercise) {
    System.out.println("setFields : setting fields with " + newUserExercise);

    // english
    english.box.setText(newUserExercise.getEnglish());
    ((TextBox) english.box).setVisibleLength(newUserExercise.getEnglish().length() + 4);
    if (newUserExercise.getEnglish().length() > 20) {
      english.box.setWidth("400px");
    }

    // foreign lang
    String foreignLanguage = newUserExercise.getForeignLanguage();
    foreignLanguage = foreignLanguage.trim();
    foreignLang.box.setText(foreignLanguage);

    // translit
    translit.box.setText(newUserExercise.getTransliteration());

    Exercise exercise = newUserExercise.toExercise();

    // regular speed audio
    rap.getPostAudioButton().setExercise(exercise);
    String refAudio = exercise.getRefAudio();


    if (refAudio != null) {
      rap.getImagesForPath(refAudio);
    }

    // slow speed audio
    rapSlow.getPostAudioButton().setExercise(exercise);
    String slowAudioRef = exercise.getSlowAudioRef();

    if (slowAudioRef != null) {
      rapSlow.getImagesForPath(slowAudioRef);
    }
  }

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
    Button submit = makeCreateButton(ul, pagingContainer, toAddTo, foreignLang, rap, normalSpeedRecording,
      CREATE);
    DOM.setStyleAttribute(submit.getElement(), "marginBottom", "5px");
    DOM.setStyleAttribute(submit.getElement(), "marginRight", "15px");

    Column column = new Column(2, 9, submit);
    column.addStyleName("topMargin");
    return column;
  }

  private Button makeCreateButton(final UserList ul, final ListInterface<T> pagingContainer, final Panel toAddTo,
                                  final FormField foreignLang,
                                  final RecordAudioPanel rap, final ControlGroup normalSpeedRecording, final String buttonName) {
    Button submit = new Button(buttonName);
    submit.setType(ButtonType.SUCCESS);
    submit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (rap.isRecording()) {
          clickedCreate = true;
          rap.clickStop();
        } else if (rapSlow.isRecording()) {
          clickedCreate = true;
          rapSlow.clickStop();
        } else {
          validateThenPost(foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo, true);
        }
      }
    });
    submit.addStyleName("rightFiveMargin");
    return submit;
  }

  /**
   * @see #audioPosted()
   * @see #makeCreateButton(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.user.BasicDialog.FormField, mitll.langtest.client.exercise.RecordAudioPanel, com.github.gwtbootstrap.client.ui.ControlGroup, String)
   * @param foreignLang
   * @param rap
   * @param normalSpeedRecording
   * @param ul
   * @param pagingContainer
   * @param toAddTo
   * @param onClick
   */
  protected void validateThenPost(FormField foreignLang, RecordAudioPanel rap,
                                  ControlGroup normalSpeedRecording, UserList ul, ListInterface<T> pagingContainer,
                                  Panel toAddTo, boolean onClick) {
    if (foreignLang.getText().isEmpty()) {
      markError(foreignLang, "Enter the foreign language phrase.");
    }
    else if (validateForm(foreignLang, rap, normalSpeedRecording)) {
      checkValidForeignPhrase(ul, pagingContainer, toAddTo, onClick);
    }
    else {
      System.out.println("NewUserExercise.validateThenPost : form not valid");
    }
  }

  private void checkValidForeignPhrase(final UserList ul, final ListInterface<T> pagingContainer, final Panel toAddTo,
                                       final boolean onClick) {
    String foreignLangText = foreignLang.getText();
    System.out.println("checkValidForeignPhrase : checking phrase " +foreignLangText+
      " before adding/changing " + newUserExercise);

    service.isValidForeignPhrase(foreignLangText, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Boolean result) {
        if (result) {
          checkIfNeedsRefAudio();
          newUserExercise.setEnglish(english.getText());
          newUserExercise.setForeignLanguage(foreignLang.getText());
          newUserExercise.setTransliteration(translit.getText());
          afterValidForeignPhrase(ul, pagingContainer, toAddTo, onClick);
        } else {
          markError(foreignLang, "The " + FOREIGN_LANGUAGE +
            " text is not in our " + controller.getLanguage() + " dictionary. Please edit.");
        }
      }
    });
  }

  protected void checkIfNeedsRefAudio() {
    if (newUserExercise == null || newUserExercise.getRefAudio() == null) {
      System.out.println("checkIfNeedsRefAudio : new user ex " + newUserExercise);

      Button recordButton = rap.getButton();
      markError(normalSpeedRecording, recordButton, recordButton, "",
        "Record reference audio for the foreign language phrase.");
      recordButton.addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent event) {
          normalSpeedRecording.setType(ControlGroupType.NONE);
        }
      });
    }
  }

  /**
   * @see #checkValidForeignPhrase
   * @param ul
   * @param exerciseList
   * @param toAddTo
   * @param onClick
   */
  protected void afterValidForeignPhrase(final UserList ul,
                                         final ListInterface<T> exerciseList,
                                         final Panel toAddTo, boolean onClick) {
    service.reallyCreateNewItem(ul.getUniqueID(), newUserExercise, new AsyncCallback<UserExercise>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(UserExercise newExercise) {
        afterItemCreated(newExercise, ul, exerciseList, toAddTo);
      }
    });
  }

  protected void afterItemCreated(UserExercise newExercise, UserList ul, ListInterface<T> exerciseList, Panel toAddTo) {
    editItem.clearNewExercise(); // success -- don't remember it

    UserExercise newUserExercisePlaceholder = ul.remove(EditItem.NEW_EXERCISE_ID);
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
  }

  /**
   * @see #makeRegularAudioPanel(com.google.gwt.user.client.ui.Panel)
   * @param row
   * @param recordRegularSpeed
   * @return
   */
  protected CreateFirstRecordAudioPanel makeRecordAudioPanel(final Panel row, boolean recordRegularSpeed) {
    return new CreateFirstRecordAudioPanel(newUserExercise.toExercise(), row,  recordRegularSpeed);
  }

  protected class CreateFirstRecordAudioPanel extends RecordAudioPanel {
    boolean recordRegularSpeed = true;
    private RecordAudioPanel otherRAP;
    private WaveformPostAudioRecordButton postAudioButton;

    public CreateFirstRecordAudioPanel(Exercise newExercise, Panel row,
                                       boolean recordRegularSpeed) {
      super(newExercise, NewUserExercise.this.controller, row, NewUserExercise.this.service, 0, false);
      this.recordRegularSpeed = recordRegularSpeed;
      setExercise(newExercise);

      addPlayListener(new PlayListener() {
        @Override
        public void playStarted() {
          otherRAP.setEnabled(false);
        }

        @Override
        public void playStopped() {
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

            super.stopRecording();

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
            audioPosted();
          }

          @Override
          protected void useInvalidResult(AudioAnswer result) {
            super.useInvalidResult(result);

            if (recordRegularSpeed) {
              newUserExercise.clearRefAudio();
            } else {
              newUserExercise.clearSlowRefAudio();
            }

            audioPosted();
          }
        };
      postAudioButton.getElement().setId("NewUserExercise_WaveformPostAudioRecordButton");
      return postAudioButton;
    }

    public void setOtherRAP(RecordAudioPanel otherRAP) {
      this.otherRAP = otherRAP;
    }
    public WaveformPostAudioRecordButton getPostAudioButton() {
      return postAudioButton;
    }
  }

  protected void audioPosted() {
    if (clickedCreate) {
      clickedCreate = false;
      validateThenPost(foreignLang, rap, normalSpeedRecording, ul, listInterface, toAddTo, false);
    }

    gotBlur();
  }

  /**
   * Validation checks appear from top to bottom on page -- so should be consistent
   * with how the fields are added.
   *
   * @see #validateThenPost
   * @param foreignLang
   * @param rap
   * @param normalSpeedRecording
   * @return
   */
  private boolean validateForm(final FormField foreignLang, final RecordAudioPanel rap,
                               final ControlGroup normalSpeedRecording) {
    if (foreignLang.getText().isEmpty()) {
      markError(foreignLang, "Enter the foreign language phrase.");
      return false;
    } else if (newUserExercise == null || newUserExercise.getRefAudio() == null) {
      System.out.println("validateForm : new user ex " + newUserExercise);

      Button recordButton = rap.getButton();
      markError(normalSpeedRecording, recordButton, recordButton, "",
        "Record reference audio for the foreign language phrase.");
      recordButton.addMouseOverHandler(new MouseOverHandler() {
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
