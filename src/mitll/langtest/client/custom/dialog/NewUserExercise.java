/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.exercise.WaveformPostAudioRecordButton;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.custom.UserExercise;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;

import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/6/13
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class NewUserExercise extends BasicDialog {
  private final Logger logger = Logger.getLogger("NewUserExercise");

  private static final String FOREIGN_LANGUAGE = "Foreign Language";
  private static final String CREATE = "Create";
  static final String ENGLISH_LABEL = "English (optional)";
  static final String ENGLISH_LABEL_2 = "Meaning (optional)";
  static final String TRANSLITERATION_OPTIONAL = "Transliteration (optional)";
  static final String NORMAL_SPEED_REFERENCE_RECORDING = "Normal speed reference recording";
  static final String SLOW_SPEED_REFERENCE_RECORDING_OPTIONAL = "Slow speed reference recording (optional)";
  private static final String ENTER_THE_FOREIGN_LANGUAGE_PHRASE = "Enter the foreign language phrase.";
  private static final String RECORD_REFERENCE_AUDIO_FOR_THE_FOREIGN_LANGUAGE_PHRASE = "Record reference audio for the foreign language phrase.";
  private static final String REMOVE_FROM_LIST = "Remove from list";

  private final EditItem editItem;
  final UserExercise newUserExercise;
  final ExerciseController controller;
  final LangTestDatabaseAsync service;
  private final HasText itemMarker;
  BasicDialog.FormField english;
  BasicDialog.FormField foreignLang;
  BasicDialog.FormField translit;
  CreateFirstRecordAudioPanel rap;
  CreateFirstRecordAudioPanel rapSlow;

  ControlGroup normalSpeedRecording;
  ControlGroup slowSpeedRecording;
  UserList<Shell> ul;
  UserList<Shell> originalList;
  ListInterface<Shell> listInterface;
  private Panel toAddTo;
  private boolean clickedCreate = false;
  final String instance;

  /**
   * @param service
   * @param controller
   * @param itemMarker
   * @param editItem
   * @param newExercise
   * @param instance
   * @see EditItem#getAddOrEditPanel
   */
  public NewUserExercise(final LangTestDatabaseAsync service,
                         ExerciseController controller, HasText itemMarker, EditItem editItem,
                         CommonUserExercise newExercise,
                         String instance) {
    this.controller = controller;
    this.service = service;
    this.itemMarker = itemMarker;
    this.editItem = editItem;
    this.newUserExercise = newExercise.toUserExercise();
    this.instance = instance;
  }

  /**
   * @param ul
   * @param originalList
   * @param listInterface
   * @param toAddTo
   * @return
   * @see #afterValidForeignPhrase
   */
  public Panel addNew(final UserList<Shell> ul,
                      UserList<Shell> originalList,
                      final ListInterface<Shell> listInterface,
                      final Panel toAddTo) {
    this.ul = ul;

    final FluidContainer container = new FluidContainer();
    DivWidget upper = new DivWidget();

    container.getElement().setId("NewUserExercise_container");
    container.getElement().getStyle().setPaddingLeft(10, Style.Unit.PX);
    container.getElement().getStyle().setPaddingRight(10, Style.Unit.PX);
    upper.addStyleName("buttonGroupInset4");
    container.addStyleName("greenBackground");

    addItemsAtTop(container);
    container.add(upper);
    /*FormField formField =*/
    makeForeignLangRow(upper);
    String id = "NewUserExercise_ForeignLang_entry_for_list_" + ul.getID();
    foreignLang.box.getElement().setId(id);

    // focusOn(formField); // Bad idea since steals the focus after search
    makeTranslitRow(upper);
    translit.box.getElement().setId("NewUserExercise_Transliteration_entry_for_list_" + ul.getID());

    makeEnglishRow(upper);
    english.box.getElement().setId("NewUserExercise_English_entry_for_list_" + ul.getID());

    // make audio row
    upper.add(makeAudioRow());

    this.toAddTo = toAddTo;
    this.originalList = originalList;
    this.listInterface = listInterface;

    container.add(getCreateButton(ul, listInterface, toAddTo, normalSpeedRecording));

    foreignLang.box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        gotBlur();
        controller.logEvent(foreignLang.box, "TextBox", "UserList_" + ul.getID(), "ForeignLangBox = " + foreignLang.box.getValue());
      }
    });
    translit.box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        gotBlur();
        controller.logEvent(translit.box, "TextBox", "UserList_" + ul.getID(), "TranslitBox = " + translit.box.getValue());
      }
    });
    english.box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        gotBlur();
        controller.logEvent(english.box, "TextBox", "UserList_" + ul.getID(), "EnglishBox = " + english.box.getValue());
      }
    });

    return container;
  }

  /**
   * @return
   * @see #addNew(mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel)
   */
  protected Panel makeAudioRow() {
    FluidRow row = new FluidRow();

    normalSpeedRecording = makeRegularAudioPanel(row);
    normalSpeedRecording.addStyleName("buttonGroupInset3");

    slowSpeedRecording = makeSlowAudioPanel(row);
    slowSpeedRecording.addStyleName("buttonGroupInset5");

    rap.setOtherRAP(rapSlow);
    rapSlow.setOtherRAP(rap);
    return row;
  }

  void addItemsAtTop(Panel container) {
  }

  private void gotBlur() {
    gotBlur(english, foreignLang, rap, normalSpeedRecording, ul, listInterface, toAddTo);
  }

  void gotBlur(FormField english, FormField foreignLang, RecordAudioPanel rap,
               ControlGroup normalSpeedRecording,
               UserList<Shell> ul,
               ListInterface<Shell> pagingContainer,
               Panel toAddTo) {
    grabInfoFromFormAndStuffInfoExercise();
  }

  /**
   * @param row
   * @return
   * @see #addNew(mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel)
   */
  ControlGroup makeRegularAudioPanel(Panel row) {
    rap = makeRecordAudioPanel(row, true, instance);
    return addControlGroupEntrySimple(row, NORMAL_SPEED_REFERENCE_RECORDING, rap);
  }

  ControlGroup makeSlowAudioPanel(Panel row) {
    rapSlow = makeRecordAudioPanel(row, false, instance);
    return addControlGroupEntrySimple(row, SLOW_SPEED_REFERENCE_RECORDING_OPTIONAL, rapSlow);
  }

  void configureButtonRow(Panel row) {
    row.addStyleName("buttonGroupInset");
  }

  /**
   * Removes from 4 lists!
   *
   * @param id
   * @param uniqueID
   * @param ul
   * @param exerciseList
   * @param npfExerciseList
   */
  void deleteItem(final String id, final long uniqueID,
                  final UserList<? extends Shell> ul,
                  final PagingExerciseList exerciseList,
                  final PagingExerciseList npfExerciseList) {
    service.deleteItemFromList(uniqueID, id, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Boolean result) {
        if (!result) logger.warning("deleteItem huh? id " + id + " not in list " + uniqueID);

        exerciseList.forgetExercise(id);
        Shell remove = ul.remove(id);
        if (remove == null) {
          logger.warning("deleteItem huh? didn't remove the item " + id);
        }
        if (originalList.remove(id) == null) {
          logger.warning("deleteItem huh? didn't remove the item " + id + " from " + originalList);
        }
        if (npfExerciseList != null) {
          npfExerciseList.redraw();
        }
      }
    });
  }

  protected Button makeDeleteButton(UserList<? extends Shell> ul) {
    Button delete = new Button(REMOVE_FROM_LIST);
    delete.getElement().setId("Remove_from_list");
    delete.getElement().getStyle().setMarginRight(5, Style.Unit.PX);

    delete.setType(ButtonType.WARNING);
    delete.addStyleName("floatRight");
    controller.register(delete, newUserExercise.getID(), "Remove from list " + ul.getID() + "/" + ul.getName());
    return delete;
  }

  Panel makeEnglishRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    english = addControlFormField(row, getEnglishLabel(), false, 1, 100, "");

    return row;
  }

  protected String getEnglishLabel() {
    return getLanguage().equalsIgnoreCase("english") ? ENGLISH_LABEL_2 : ENGLISH_LABEL;
  }

  FormField makeForeignLangRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    foreignLang = addControlFormField(row, getLanguage(), false, 1, 150, "");
    foreignLang.box.setDirectionEstimator(true);   // automatically detect whether text is RTL

    return foreignLang;
  }

  private String getLanguage() {
    return controller.getLanguage();
  }

  void makeTranslitRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    translit = addControlFormField(row, TRANSLITERATION_OPTIONAL, false, 0, 150, "");
  }

/*
  private void focusOn(final FormField form) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        form.box.setFocus(true);
      }
    });
  }
*/

  public <S extends CommonShell & AudioRefExercise & AnnotationExercise> void setFields(S newUserExercise) {
    logger.info("grabInfoFromFormAndStuffInfoExercise : setting fields with " + newUserExercise);

    // english
    String english = newUserExercise.getEnglish();
    this.english.box.setText(english);
    ((TextBox) this.english.box).setVisibleLength(english.length() + 4);
    if (english.length() > 20) {
      this.english.box.setWidth("400px");
    }

    // foreign lang
    String foreignLanguage = newUserExercise.getForeignLanguage();
    foreignLanguage = foreignLanguage.trim();
    foreignLang.box.setText(foreignLanguage);

    // translit
    translit.box.setText(newUserExercise.getTransliteration());

    // regular speed audio
    rap.getPostAudioButton().setExercise(newUserExercise.getID());
    String refAudio = newUserExercise.getRefAudio();

    if (refAudio != null) {
      rap.getImagesForPath(refAudio);
    }

    // slow speed audio
    rapSlow.getPostAudioButton().setExercise(newUserExercise.getID());
    String slowAudioRef = newUserExercise.getSlowAudioRef();

    if (slowAudioRef != null) {
      rapSlow.getImagesForPath(slowAudioRef);
    }
  }

  /**
   * @param ul
   * @param pagingContainer
   * @param toAddTo
   * @param normalSpeedRecording
   * @return
   * @see #addNew
   */
  Panel getCreateButton(UserList<Shell> ul,
                        ListInterface<Shell> pagingContainer, Panel toAddTo,
                        ControlGroup normalSpeedRecording) {
    Button submit = makeCreateButton(ul, pagingContainer, toAddTo, foreignLang, rap, normalSpeedRecording);
    submit.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);
    submit.getElement().getStyle().setMarginRight(15, Style.Unit.PX);

    Panel row = new DivWidget();
    row.addStyleName("marginBottomTen");

    configureButtonRow(row);
    row.add(submit);
    return row;
  }

  private Button makeCreateButton(final UserList<Shell> ul,
                                  final ListInterface<Shell> pagingContainer, final Panel toAddTo,
                                  final FormField foreignLang,
                                  final RecordAudioPanel rap, final ControlGroup normalSpeedRecording) {
    final Button submit = new Button(NewUserExercise.CREATE);
    submit.setType(ButtonType.SUCCESS);
    submit.getElement().setId("CreateButton_NewExercise_for_" + ul.getID());
    controller.register(submit, "UserList_" + ul.getID());
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
          validateThenPost(foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo, true, true);
        }
      }
    });
    submit.addStyleName("rightFiveMargin");
    submit.addStyleName("floatRight");

    return submit;
  }

  /**
   * @param foreignLang
   * @param rap
   * @param normalSpeedRecording
   * @param ul
   * @param pagingContainer
   * @param toAddTo
   * @param onClick
   * @param foreignChanged
   * @see #audioPosted()
   * @see #makeCreateButton(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.user.BasicDialog.FormField, mitll.langtest.client.exercise.RecordAudioPanel, com.github.gwtbootstrap.client.ui.ControlGroup)
   */
  void validateThenPost(FormField foreignLang, RecordAudioPanel rap,
                        ControlGroup normalSpeedRecording,
                        UserList<Shell> ul,
                        ListInterface<Shell> pagingContainer,
                        Panel toAddTo, boolean onClick, boolean foreignChanged) {
    if (foreignLang.getText().isEmpty()) {
      markError(foreignLang, ENTER_THE_FOREIGN_LANGUAGE_PHRASE);
    } else if (validateForm(foreignLang, rap, normalSpeedRecording, foreignChanged)) {
      isValidForeignPhrase(ul, pagingContainer, toAddTo, onClick);
    } else {
      formInvalid();
      logger.info("NewUserExercise.validateThenPost : form not valid");
    }
  }

  void formInvalid() {
  }

  /**
   * Ask the server if the foreign lang text is in our dictionary and can be run through hydec.
   *
   * @param ul
   * @param pagingContainer
   * @param toAddTo
   * @param onClick
   * @see #validateThenPost(mitll.langtest.client.user.BasicDialog.FormField, mitll.langtest.client.exercise.RecordAudioPanel, com.github.gwtbootstrap.client.ui.ControlGroup, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, boolean, boolean)
   */
  private void isValidForeignPhrase(final UserList<Shell> ul,
                                    final ListInterface<Shell> pagingContainer, final Panel toAddTo,
                                    final boolean onClick) {
    //String foreignLangText = foreignLang.getText();
/*    logger.info("isValidForeignPhrase : checking phrase " + foreignLangText +
      " before adding/changing " + newUserExercise);*/

    service.isValidForeignPhrase(foreignLang.getText(), new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Boolean result) {
        if (result) {
          checkIfNeedsRefAudio();
          grabInfoFromFormAndStuffInfoExercise();
          afterValidForeignPhrase(ul, pagingContainer, toAddTo, onClick);
        } else {
          markError(foreignLang, "The " + FOREIGN_LANGUAGE +
              " text is not in our " + getLanguage() + " dictionary. Please edit.");
        }
      }
    });
  }

  void grabInfoFromFormAndStuffInfoExercise() {
    newUserExercise.setEnglish(english.getText());
    newUserExercise.setForeignLanguage(foreignLang.getText());
    newUserExercise.setTransliteration(translit.getText());
  }

  void checkIfNeedsRefAudio() {
    if (newUserExercise == null || newUserExercise.getRefAudio() == null) {
      //logger.info("checkIfNeedsRefAudio : new user ex " + newUserExercise);

      Button recordButton = rap.getButton();
      markError(normalSpeedRecording, recordButton, recordButton, "", RECORD_REFERENCE_AUDIO_FOR_THE_FOREIGN_LANGUAGE_PHRASE, Placement.RIGHT);
      recordButton.addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent event) {
          normalSpeedRecording.setType(ControlGroupType.NONE);
        }
      });
    }
  }

  /**
   * @param ul
   * @param exerciseList
   * @param toAddTo
   * @param onClick
   * @see #isValidForeignPhrase
   */
  void afterValidForeignPhrase(final UserList<Shell> ul,
                               final ListInterface<Shell> exerciseList,
                               final Panel toAddTo, boolean onClick) {
    service.reallyCreateNewItem(ul.getUniqueID(), newUserExercise, new AsyncCallback<UserExercise>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(UserExercise newExercise) {
        afterItemCreated(newExercise, ul, exerciseList, toAddTo);
      }
    });
  }

  /**
   * Add to the original list, out copy, and the pagin exercise list --- complicated!
   * <p>
   * After list manipulation, remove the old panel and put in a new copy.
   *
   * @param newExercise
   * @param ul
   * @param exerciseList
   * @param toAddTo
   */
  private void afterItemCreated(Shell newExercise, UserList<Shell> ul, ListInterface<Shell> exerciseList, Panel toAddTo) {
    //logger.info("afterItemCreated " + newExercise);
    editItem.clearNewExercise(); // success -- don't remember it

    Shell newUserExercisePlaceholder = ul.remove(EditItem.NEW_EXERCISE_ID);
    ul.addExercise(newExercise);
    originalList.addExercise(newExercise);
    ul.addExercise(newUserExercisePlaceholder); // make sure the placeholder is always at the end
    itemMarker.setText(ul.getExercises().size() + " items");

    Shell toMoveToEnd = moveNewExerciseToEndOfList(newExercise, exerciseList);

    exerciseList.checkAndAskServer(toMoveToEnd.getID());

    toAddTo.clear();
    toAddTo.add(addNew(ul, originalList, exerciseList, toAddTo));
  }

  private Shell moveNewExerciseToEndOfList(Shell newExercise, ListInterface<Shell> exerciseList) {
    Shell toMoveToEnd = exerciseList.simpleRemove(EditItem.NEW_EXERCISE_ID);
    exerciseList.addExercise(newExercise);  // TODO figure out better type safe way of doing this
    exerciseList.addExercise(toMoveToEnd);
    exerciseList.redraw();
    return toMoveToEnd;
  }

  /**
   * @param row
   * @param recordRegularSpeed
   * @param instance
   * @return
   * @see #makeRegularAudioPanel(com.google.gwt.user.client.ui.Panel)
   */
  CreateFirstRecordAudioPanel<CommonUserExercise> makeRecordAudioPanel(final Panel row,
                                                                       boolean recordRegularSpeed,
                                                                       String instance) {
    return new CreateFirstRecordAudioPanel<>(newUserExercise, row, recordRegularSpeed, instance);
  }

  class CreateFirstRecordAudioPanel<T extends Shell & AudioRefExercise> extends RecordAudioPanel<T> {
    boolean recordRegularSpeed = true;
    private RecordAudioPanel otherRAP;
    private WaveformPostAudioRecordButton postAudioButton;

    public CreateFirstRecordAudioPanel(T newExercise, Panel row,
                                       boolean recordRegularSpeed, String instance) {
      super(newExercise, NewUserExercise.this.controller, row, NewUserExercise.this.service, 0, false,
          NewUserExercise.this.controller.getAudioType(), instance);
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

      String newUserExercise_waveformPostAudioRecordButton = "NewUserExercise_WaveformPostAudioRecordButton_";
      String speed = (recordRegularSpeed ? "Regular" : "Slow") + "_speed";
      getPostAudioButton().getElement().setId(newUserExercise_waveformPostAudioRecordButton + speed);
      getPlayButton().getElement().setId(newUserExercise_waveformPostAudioRecordButton + "Play_" + speed);
      controller.register(getPlayButton(), newExercise.getID());
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
     *
     * @return
     * @see mitll.langtest.client.scoring.AudioPanel#makePlayAudioPanel
     */
    @Override
    protected WaveformPostAudioRecordButton makePostAudioRecordButton(String audioType, String recordButtonTitle) {
      postAudioButton =
          new WaveformPostAudioRecordButton(exercise.getID(), controller, exercisePanel, this, service, recordRegularSpeed ? 0 : 1,
              false // don't record in results table
              ,
              RecordButton.RECORD1,
              RecordButton.STOP1,
              audioType) {
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
            protected String getAudioType() {
              return recordRegularSpeed ? Result.AUDIO_TYPE_REGULAR : Result.AUDIO_TYPE_SLOW;
            }

            @Override
            public void useResult(AudioAnswer result) {
              super.useResult(result);

              //logger.info("got back " + result.getAudioAttribute() + " for " + newUserExercise);
              if (result.getAudioAttribute() != null) {

                if (recordRegularSpeed) {
                  result.getAudioAttribute().markRegular();
                } else {
                  result.getAudioAttribute().markSlow();
                }

                newUserExercise.addAudio(result.getAudioAttribute());

              } else {
                logger.warning("no valid audio on " + result);
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
      postAudioButton.getElement().setId("NewUserExercise_WaveformPostAudioRecordButton_" + (recordRegularSpeed ? "Regular" : "Slow") + "_speed");
      return postAudioButton;
    }

    public void setOtherRAP(RecordAudioPanel otherRAP) {
      this.otherRAP = otherRAP;
    }

    public WaveformPostAudioRecordButton getPostAudioButton() {
      return postAudioButton;
    }
  }

  void audioPosted() {
    if (clickedCreate) {
      clickedCreate = false;
      validateThenPost(foreignLang, rap, normalSpeedRecording, ul, listInterface, toAddTo, false, true);
    }

    gotBlur();
  }

  /**
   * Validation checks appear from top to bottom on page -- so should be consistent
   * with how the fields are added.
   *
   * @param foreignLang
   * @param rap
   * @param normalSpeedRecording
   * @param foreignChanged
   * @return
   * @see #validateThenPost
   */
  private boolean validateForm(final FormField foreignLang, final RecordAudioPanel rap,
                               final ControlGroup normalSpeedRecording, boolean foreignChanged) {
    if (foreignLang.getText().isEmpty()) {
      markError(foreignLang, ENTER_THE_FOREIGN_LANGUAGE_PHRASE);
      return false;
    } else if (newUserExercise == null || newUserExercise.getRefAudio() == null) {
      logger.info("validateForm : new user ex " + newUserExercise);

      if (foreignChanged && rap != null) {
        Button recordButton = rap.getButton();
        markError(normalSpeedRecording, recordButton, recordButton, "",
            RECORD_REFERENCE_AUDIO_FOR_THE_FOREIGN_LANGUAGE_PHRASE);
        recordButton.addMouseOverHandler(new MouseOverHandler() {
          @Override
          public void onMouseOver(MouseOverEvent event) {
            normalSpeedRecording.setType(ControlGroupType.NONE);
          }
        });
        return false;
      } else {
        return true;
      }
    }
    return true;
  }
}
