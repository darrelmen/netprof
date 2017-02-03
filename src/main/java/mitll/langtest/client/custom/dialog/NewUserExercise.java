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
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.FluidContainer;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.ReloadableContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.exercise.WaveformPostAudioRecordButton;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.services.AudioService;
import mitll.langtest.client.services.AudioServiceAsync;
import mitll.langtest.client.services.ListService;
import mitll.langtest.client.services.ListServiceAsync;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.FormField;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.AnnotationExercise;
import mitll.langtest.shared.exercise.AudioRefExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.MutableAudioExercise;
import mitll.langtest.shared.exercise.MutableExercise;
import mitll.langtest.shared.exercise.Shell;

import java.util.logging.Logger;


/**
 * Created with IntelliJ IDEA.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 12/6/13
 * Time: 5:59 PM
 * To change this template use File | Settings | File Templates.
 */
class NewUserExercise extends BasicDialog {
  private final Logger logger = Logger.getLogger("NewUserExercise");

  private static final int MAX_CHARACTERS = 300;
  protected static final int TEXT_FIELD_WIDTH = 500;

  private static final String FOREIGN_LANGUAGE = "Foreign Language";
  private static final String CREATE = "Create";
  private static final String ENGLISH_LABEL = "English";// (optional)";
  private static final String ENGLISH_LABEL_2 = "Meaning (optional)";
  static final String TRANSLITERATION_OPTIONAL = "Transliteration (optional)";
  static final String NORMAL_SPEED_REFERENCE_RECORDING = "Normal speed reference recording";
  static final String SLOW_SPEED_REFERENCE_RECORDING_OPTIONAL = "Slow speed reference recording (optional)";
  private static final String ENTER_THE_FOREIGN_LANGUAGE_PHRASE = "Enter the foreign language phrase.";
  private static final String ENTER_THE_ENGLISH_PHRASE = "Enter the english equivalent.";
  private static final String ENTER_MEANING = "Enter the meaning of the english.";
  private static final String RECORD_REFERENCE_AUDIO_FOR_THE_FOREIGN_LANGUAGE_PHRASE = "Record reference audio for the foreign language phrase.";
  /**
   * @see #makeDeleteButton(UserList)
   */
  private static final String REMOVE_FROM_LIST = "Remove from list";

  private final EditItem editItem;

  final CommonExercise newUserExercise;

  final ExerciseController controller;
  private final LangTestDatabaseAsync service;

  final ListServiceAsync listService = GWT.create(ListService.class);
  final AudioServiceAsync audioServiceAsync = GWT.create(AudioService.class);
  private final HasText itemMarker;
  FormField english;
  FormField foreignLang;
  FormField translit;
  CreateFirstRecordAudioPanel rap;
  CreateFirstRecordAudioPanel rapSlow;

  ControlGroup normalSpeedRecording = null;
  ControlGroup slowSpeedRecording = null;
  UserList<CommonShell> ul;
  UserList<CommonShell> originalList;
  /**
   * TODO : What is this for???
   */
  ListInterface<CommonShell> listInterface;
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
   * @param originalList
   * @see EditItem#getAddOrEditPanel
   */
  public NewUserExercise(final LangTestDatabaseAsync service,
                         ExerciseController controller,
                         HasText itemMarker,
                         EditItem editItem,
                         CommonExercise newExercise,
                         String instance,
                         UserList<CommonShell> originalList) {
    this.controller = controller;
    this.service = service;
    this.itemMarker = itemMarker;
    this.editItem = editItem;
    this.newUserExercise = newExercise;
    this.instance = instance;
    this.originalList = originalList;

    //   logger.info("new exercise is " + newUserExercise);
  }


  /**
   * @param ul
   * @param originalList
   * @param listInterface
   * @param toAddTo
   * @return
   * @see #afterValidForeignPhrase
   */
  public Panel addNew(final UserList<CommonShell> ul,
                      UserList<CommonShell> originalList,
                      final ListInterface<CommonShell> listInterface,
                      final Panel toAddTo) {
    this.ul = ul;

    final FluidContainer container = new ResizableFluid();
    DivWidget upper = new DivWidget();
    upper.getElement().setId("addNewFieldContainer");
    Element element = container.getElement();
    element.setId("NewUserExercise_container");
    Style style = element.getStyle();
    //style.setMarginTop(5, Style.Unit.PX);
    style.setPaddingLeft(10, Style.Unit.PX);
    style.setPaddingRight(10, Style.Unit.PX);
    upper.addStyleName("buttonGroupInset4");
    container.addStyleName("greenBackground");

    addItemsAtTop(container);
    container.add(upper);

    makeForeignLangRow(upper);
    final String id1 = "" + ul.getID();

    foreignLang.box.getElement().setId("NewUserExercise_ForeignLang_entry_for_list_" + id1);
    // focusOn(formField); // Bad idea since steals the focus after search
    makeTranslitRow(upper);
    translit.box.getElement().setId("NewUserExercise_Transliteration_entry_for_list_" + id1);

    makeEnglishRow(upper);
    english.box.getElement().setId("NewUserExercise_English_entry_for_list_" + id1);

    makeOptionalRows(upper);
    // make audio row
    upper.add(makeAudioRow());

    this.toAddTo = toAddTo;
    this.originalList = originalList;
    this.listInterface = listInterface;

    container.add(getCreateButton(ul, listInterface, toAddTo, normalSpeedRecording));

    addBlurHandler(ul.getID(), foreignLang);
    addBlurHandler(ul.getID(), translit);
    addBlurHandler(ul.getID(), english);
    return container;
  }

  protected void addBlurHandler(final int id1, FormField field) {
    field.box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        gotBlur();
        controller.logEvent(field.box, "TextBox", "UserList_" + id1, "ForeignLangBox = " + field.box.getValue());
      }
    });
  }

  private class ResizableFluid extends FluidContainer implements RequiresResize {
    @Override
    public void onResize() {
      if (rap != null) {
        rap.onResize();
        rapSlow.onResize();
      }
    }
  }

  protected void makeOptionalRows(DivWidget upper) {
  }

  /**
   * @return
   * @see #addNew(mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel)
   */
  Panel makeAudioRow() {
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

  void gotBlur() {
    gotBlur(foreignLang, rap, normalSpeedRecording, ul, listInterface, toAddTo);
  }

  void gotBlur(FormField foreignLang,
               RecordAudioPanel rap,
               ControlGroup normalSpeedRecording,
               UserList<CommonShell> ul,
               ListInterface<CommonShell> pagingContainer,
               Panel toAddTo) {

    logger.info("got blur -");
    grabInfoFromFormAndStuffInfoExercise(newUserExercise.getMutable());
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
   * @param exid
   * @param uniqueID
   * @param ul
   * @param exerciseList
   * @param learnContainer
   * @see EditableExerciseDialog#makeDeleteButton(long)
   */
  void deleteItem(final int exid,
                  final long uniqueID,
                  final UserList<?> ul,
                  final PagingExerciseList<?, ?> exerciseList,
                  final ReloadableContainer learnContainer) {
    listService.deleteItemFromList(uniqueID, exid, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Boolean result) {
        if (!result) logger.warning("deleteItem huh? id " + exid + " not in list " + uniqueID);

        exerciseList.forgetExercise(exid);

        if (!ul.removeAndCheck(exid)) {
          logger.warning("deleteItem huh? didn't remove the item " + exid);
        }
        if (!originalList.removeAndCheck(exid)) {
          logger.warning("deleteItem huh? didn't remove the item " + exid + " from " + originalList);
        }
        if (learnContainer != null && learnContainer.getReloadable() != null) {
          learnContainer.getReloadable().redraw();   // TODO : or reload???
        }
      }
    });
  }

  /**
   * @param ul
   * @return
   * @see EditableExerciseDialog#makeDeleteButton(long)
   */
  Button makeDeleteButton(UserList<?> ul) {
    Button delete = new Button(REMOVE_FROM_LIST);
    delete.getElement().setId("Remove_from_list");
    delete.getElement().getStyle().setMarginRight(5, Style.Unit.PX);

    delete.setType(ButtonType.WARNING);
    delete.addStyleName("floatRight");
    controller.register(delete, "" + newUserExercise.getID(), "Remove from list " + ul.getID() + "/" + ul.getName());
    return delete;
  }

  void makeEnglishRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    english = addControlFormField(row, getEnglishLabel(), false, 1, MAX_CHARACTERS, "", TEXT_FIELD_WIDTH);
  }

  String getEnglishLabel() {
    return getLanguage().equalsIgnoreCase("english") ? ENGLISH_LABEL_2 : ENGLISH_LABEL;
  }

  void makeForeignLangRow(Panel container) {
    //  logger.info("NewUserExercise.makeForeignLangRow --->");
    Panel row = new FluidRow();
    container.add(row);
    foreignLang = addControlFormField(row, getLanguage(), false, 1, MAX_CHARACTERS, "", TEXT_FIELD_WIDTH);
    foreignLang.box.setDirectionEstimator(true);   // automatically detect whether text is RTL
  }

  private String getLanguage() {
    return controller.getLanguage();
  }

  void makeTranslitRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    translit = addControlFormField(row, TRANSLITERATION_OPTIONAL, false, 0, MAX_CHARACTERS, "", TEXT_FIELD_WIDTH);
  }

  public <S extends CommonExercise & AudioRefExercise & AnnotationExercise> void setFields(S newUserExercise) {
    logger.info("setFields : setting fields with " + newUserExercise);
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
  Panel getCreateButton(UserList<CommonShell> ul,
                        ListInterface<CommonShell> pagingContainer,
                        Panel toAddTo,
                        ControlGroup normalSpeedRecording) {
//    if (logger != null) {
//      logger.info(this.getClass() + " adding create button - new user");
//    }
    Button submit = makeCreateButton(ul, pagingContainer, toAddTo, foreignLang, rap, normalSpeedRecording);
    Style style = submit.getElement().getStyle();
    style.setMarginBottom(5, Style.Unit.PX);
    style.setMarginRight(15, Style.Unit.PX);

    Panel row = new DivWidget();
    row.addStyleName("marginBottomTen");

    configureButtonRow(row);
    row.add(submit);
    return row;
  }

  private Button makeCreateButton(final UserList<CommonShell> ul,
                                  final ListInterface<CommonShell> pagingContainer,
                                  final Panel toAddTo,
                                  final FormField foreignLang,
                                  final RecordAudioPanel rap,
                                  final ControlGroup normalSpeedRecording) {
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
   * @see #makeCreateButton(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, FormField, mitll.langtest.client.exercise.RecordAudioPanel, com.github.gwtbootstrap.client.ui.ControlGroup)
   */
  void validateThenPost(FormField foreignLang,
                        RecordAudioPanel rap,
                        ControlGroup normalSpeedRecording,
                        UserList<CommonShell> ul,
                        ListInterface<CommonShell> pagingContainer,
                        Panel toAddTo,
                        boolean onClick,
                        boolean foreignChanged) {
    if (foreignLang.getSafeText().isEmpty()) {
      markError(foreignLang, ENTER_THE_FOREIGN_LANGUAGE_PHRASE);
    } else if (english.getSafeText().isEmpty()) {
      String enterTheEnglishPhrase = isEnglish() ? ENTER_MEANING : ENTER_THE_ENGLISH_PHRASE;
      markError(english, enterTheEnglishPhrase);
    } else if (validateForm(foreignLang, rap, normalSpeedRecording, foreignChanged)) {
      isValidForeignPhrase(ul, pagingContainer, toAddTo, onClick);
    } else {
      formInvalid();
      logger.info("NewUserExercise.validateThenPost : form not valid");
    }
  }

  protected boolean isEnglish() {
    return controller.getLanguage().equalsIgnoreCase("english");
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
   * @see #validateThenPost
   */
  private void isValidForeignPhrase(final UserList<CommonShell> ul,
                                    final ListInterface<CommonShell> pagingContainer,
                                    final Panel toAddTo,
                                    final boolean onClick) {
    //  logger.info("isValidForeignPhrase : checking phrase " + foreignLang.getSafeText() + " before adding/changing " + newUserExercise);
    service.isValidForeignPhrase(foreignLang.getSafeText(), "", new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Boolean result) {
/*        logger.info("\tisValidForeignPhrase : checking phrase " + foreignLang.getSafeText() +
            " before adding/changing " + newUserExercise + " -> " + result);*/
        if (result) {
          checkIfNeedsRefAudio();
          grabInfoFromFormAndStuffInfoExercise(newUserExercise.getMutable());
          afterValidForeignPhrase(ul, pagingContainer, toAddTo, onClick);
        } else {
          markError(foreignLang, "The " + FOREIGN_LANGUAGE +
              " text is not in our " + getLanguage() + " dictionary. Please edit.");
        }
      }
    });
  }

  void grabInfoFromFormAndStuffInfoExercise(MutableExercise mutableExercise) {
    String text = english.getSafeText();

    //   logger.info("so english  field is " + text + " fl " + foreignLang.getSafeText());
    //   logger.info("so translit field is " + translit.getSafeText());
    if (isEnglish()) {
      mutableExercise.setMeaning(text);
    } else {
      mutableExercise.setEnglish(text);
    }
    mutableExercise.setForeignLanguage(foreignLang.getSafeText());
    mutableExercise.setTransliteration(translit.getSafeText());
  }

  /**
   * @see #isValidForeignPhrase(UserList, ListInterface, Panel, boolean)
   */
  void checkIfNeedsRefAudio() {
    if (newUserExercise == null/* || newUserExercise.getRefAudio() == null*/) {
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
  void afterValidForeignPhrase(final UserList<CommonShell> ul,
                               final ListInterface<CommonShell> exerciseList,
                               final Panel toAddTo,
                               boolean onClick) {
    logger.info("afterValidForeignPhrase newUserExercise is " + newUserExercise);
    audioServiceAsync.reallyCreateNewItem(ul.getID(),
        newUserExercise,
        getLanguage(),
        new AsyncCallback<CommonExercise>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(CommonExercise newExercise) {
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
   * @see #afterValidForeignPhrase(UserList, ListInterface, Panel, boolean)
   */
  private void afterItemCreated(CommonExercise newExercise,
                                UserList<CommonShell> ul,
                                ListInterface<CommonShell> exerciseList,
                                Panel toAddTo) {
//    logger.info("afterItemCreated " + newExercise + " creator " + newExercise.getCreator());
    editItem.clearNewExercise(); // success -- don't remember it

    CommonShell newUserExercisePlaceholder = ul.remove(EditItem.NEW_EXERCISE_ID);

    logger.info("afterItemCreated removed " + newUserExercisePlaceholder);

    ul.addExercise(newExercise);

    for (CommonShell exercise : ul.getExercises()) {
      logger.info("user list " + ul.getID() + " " + ul.getName() + " has " + exercise);
    }
    originalList.addExercise(newExercise);
    for (CommonShell exercise : originalList.getExercises()) {
      logger.info("originalList " + ul.getID() + " " + ul.getName() + " has " + exercise);
    }
    ul.addExercise(newUserExercisePlaceholder); // make sure the placeholder is always at the end
    itemMarker.setText(ul.getNumItems() + " items");

    Shell toMoveToEnd = moveNewExerciseToEndOfList(newExercise, exerciseList);

    exerciseList.clearCachedExercise(); // if we don't it will just use the cached exercise, if it's the current one
    int id = toMoveToEnd.getID();

    logger.info("afterItemCreated checkAndAskServer " + id);

    exerciseList.checkAndAskServer(id);

    exerciseList.report();

    toAddTo.clear();
    toAddTo.add(addNew(ul, originalList, exerciseList, toAddTo));
  }

  private Shell moveNewExerciseToEndOfList(CommonExercise newExercise, ListInterface<CommonShell> exerciseList) {
    CommonShell toMoveToEnd = exerciseList.simpleRemove(EditItem.NEW_EXERCISE_ID);

    logger.info("moveNewExerciseToEndOfList removed " + toMoveToEnd);
    exerciseList.report();

    logger.info("moveNewExerciseToEndOfList add " + newExercise);

    exerciseList.addExercise(newExercise);
    exerciseList.report();
    logger.info("moveNewExerciseToEndOfList add " + toMoveToEnd);

    exerciseList.addExercise(toMoveToEnd);
    exerciseList.report();

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
  CreateFirstRecordAudioPanel makeRecordAudioPanel(final Panel row,
                                                   boolean recordRegularSpeed,
                                                   String instance) {
    return new CreateFirstRecordAudioPanel(newUserExercise, row, recordRegularSpeed, instance);
  }

  protected class CreateFirstRecordAudioPanel extends RecordAudioPanel<CommonExercise> {
    boolean recordRegularSpeed;
    private RecordAudioPanel otherRAP;
    private WaveformPostAudioRecordButton postAudioButton;

    /**
     * @param newExercise
     * @param row
     * @param recordRegularSpeed
     * @param instance
     * @see #makeRecordAudioPanel(Panel, boolean, String)
     */
    public CreateFirstRecordAudioPanel(CommonExercise newExercise, Panel row,
                                       boolean recordRegularSpeed, String instance) {
      super(newExercise, NewUserExercise.this.controller, row, 0, false,
          recordRegularSpeed ? AudioType.REGULAR : AudioType.SLOW, instance);
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

      String id = "NewUserExercise_WaveformPostAudioRecordButton_";
      String speed = (recordRegularSpeed ? "Regular" : "Slow") + "_speed";
      getPostAudioButton().getElement().setId(id + speed);
      getPlayButton().getElement().setId(id + "Play_" + speed);
      controller.register(getPlayButton(), newExercise.getID());
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
    protected WaveformPostAudioRecordButton makePostAudioRecordButton(AudioType audioType, String recordButtonTitle) {
      postAudioButton =
          new WaveformPostAudioRecordButton(exercise.getID(), controller, exercisePanel, this,
              recordRegularSpeed ? 0 : 1,
              false // don't record in results table
              ,
              RecordButton.RECORD1,
              RecordButton.STOP1,
              audioType) {
            @Override
            public void stopRecording(long duration) {
              otherRAP.setEnabled(true);
              showStop();
              super.stopRecording(duration);
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
            protected AudioType getAudioType() {
              return recordRegularSpeed ? AudioType.REGULAR : AudioType.SLOW;
            }

            @Override
            public void useResult(AudioAnswer result) {
              super.useResult(result);

              // logger.info("useResult got back " + result.getAudioAttribute() + " for " + newUserExercise);
              if (result.getAudioAttribute() != null) {
                if (recordRegularSpeed) {
                  result.getAudioAttribute().markRegular();
                } else {
                  result.getAudioAttribute().markSlow();
                }

                // newUserExercise.getCombinedMutableUserExercise().addAudio(result.getAudioAttribute());
                newUserExercise.getMutableAudio().addAudio(result.getAudioAttribute());

              } else {
                logger.warning("no valid audio on " + result);
              }
              audioPosted();
            }

            @Override
            protected void useInvalidResult(AudioAnswer result) {
              super.useInvalidResult(result);

              MutableAudioExercise mutableAudio = newUserExercise.getMutableAudio();
              if (recordRegularSpeed) {
                mutableAudio.clearRefAudio();
              } else {
                mutableAudio.clearSlowRefAudio();
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
    if (foreignLang.getSafeText().isEmpty()) {
      markError(foreignLang, ENTER_THE_FOREIGN_LANGUAGE_PHRASE);
      return false;
    } else if (validRecordingCheck()) {
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

  private boolean validRecordingCheck() {
    return newUserExercise == null;// || newUserExercise.getRefAudio() == null;
  }
}
