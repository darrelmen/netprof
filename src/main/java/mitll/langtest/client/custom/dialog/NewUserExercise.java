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

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import mitll.langtest.client.custom.ReloadableContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.exercise.WaveformPostAudioRecordButton;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.services.ListService;
import mitll.langtest.client.services.ListServiceAsync;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.FormField;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;

import java.util.Collection;
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
abstract class NewUserExercise extends BasicDialog {
  private final Logger logger = Logger.getLogger("NewUserExercise");

  public static final String CONTEXT = "context";
  public static final String CONTEXT_TRANSLATION = "context translation";
  private static final String CONTEXT_LABEL = "Context (optional)";
  private static final String CONTEXT_TRANSLATION_LABEL = "C. Translation (optional)";

  private static final int TEXT_FIELD_WIDTH = 500;
  private static final int LABEL_WIDTH = 105;

  private static final String FOREIGN_LANGUAGE = "Foreign Language";
  private static final String CREATE = "Create";
  private static final String ENGLISH_LABEL = "English (optional)";// (optional)";
  private static final String ENGLISH_LABEL_2 = "Meaning (optional)";
  private static final String TRANSLITERATION_OPTIONAL = "Transliteration (optional)";
  static final String NORMAL_SPEED_REFERENCE_RECORDING = "Normal speed reference recording";
  static final String SLOW_SPEED_REFERENCE_RECORDING_OPTIONAL = "Slow speed reference recording (optional)";
  private static final String ENTER_THE_FOREIGN_LANGUAGE_PHRASE = "Enter the foreign language phrase.";
  private static final String ENTER_THE_ENGLISH_PHRASE = "Enter the english equivalent.";
  private static final String ENTER_MEANING = "Enter the meaning of the english.";
  private static final String RECORD_REFERENCE_AUDIO_FOR_THE_FOREIGN_LANGUAGE_PHRASE = "Record reference audio for the foreign language phrase.";

  final CommonExercise newUserExercise;

  final ExerciseController controller;

  final ListServiceAsync listService = GWT.create(ListService.class);
 /**
   * @see EditableExerciseDialog#setFields
   */
  FormField english;
  FormField foreignLang;
  FormField translit;
  FormField context;
  FormField contextTrans;

  protected final HTML englishAnno = new HTML();
  protected final HTML translitAnno = new HTML();
  protected final HTML foreignAnno = new HTML();
  protected final HTML contextAnno = new HTML();
  protected final HTML contextTransAnno = new HTML();

  protected String originalContext = "";
  protected String originalContextTrans = "";

  CreateFirstRecordAudioPanel rap;
  CreateFirstRecordAudioPanel rapSlow;

  ControlGroup normalSpeedRecording = null;
  ControlGroup slowSpeedRecording = null;
  UserList<CommonShell> originalList;

  /**
   * TODO : What is this for???
   */
  ListInterface<CommonShell> listInterface;
  private Panel toAddTo;
  private boolean clickedCreate = false;
  final String instance;


  /**
   * @param controller
   * @param newExercise
   * @param instance
   * @param originalList
   * @paramx editableExerciseList
   * @paramx service
   * @paramx itemMarker
   * @see EditableExerciseList#getAddButton
   */
  public NewUserExercise(
      ExerciseController controller,
      CommonExercise newExercise,
      String instance,
      UserList<CommonShell> originalList) {
    this.controller = controller;
    this.newUserExercise = newExercise;
    this.instance = instance;
    this.originalList = originalList;
  }

  /**
   * @param listInterface
   * @param toAddTo
   * @return
   * @see EditItem#setFactory
   */
  public Panel addNew(
      final ListInterface<CommonShell> listInterface,
      final Panel toAddTo) {
    final FluidContainer container = new ResizableFluid();
    DivWidget upper = new DivWidget();
    upper.getElement().setId("addNewFieldContainer");

    container.getElement().setId("NewUserExercise_container");
    Style style = container.getElement().getStyle();
    //style.setMarginTop(5, Style.Unit.PX);
    style.setPaddingLeft(10, Style.Unit.PX);
    style.setPaddingRight(10, Style.Unit.PX);
    upper.addStyleName("buttonGroupInset4");
    container.addStyleName("greenBackground");

    addItemsAtTop(container);
    container.add(upper);

    makeForeignLangRow(upper);
    final String id1 = "" + originalList.getID();

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
    // this.originalList = originalList;
    this.listInterface = listInterface;

    Panel buttonRow = getCreateButton(originalList, listInterface, toAddTo, normalSpeedRecording);
    if (buttonRow != null) {
      container.add(buttonRow);
/*      Button widgets = makeCancelButton();
      if (widgets != null) {
        buttonRow.add(widgets);
      }*/
    }

    addBlurHandler(originalList.getID(), foreignLang);
    addBlurHandler(originalList.getID(), translit);
    addBlurHandler(originalList.getID(), english);
    return container;
  }
//
//  private Modal modal;
//
//  public void setModal(Modal modal) {
//    this.modal = modal;
//  }

  private void addBlurHandler(final int id1, FormField field) {
    field.box.addBlurHandler(event -> {
      gotBlur();
      controller.logEvent(field.box, "TextBox", "UserList_" + id1, "ForeignLangBox = " + field.box.getValue());
    });
  }

//  public boolean isCompleted() {
//    return completed;
//  }

  private class ResizableFluid extends FluidContainer implements RequiresResize {
    @Override
    public void onResize() {
      if (rap != null) {
        rap.onResize();
        rapSlow.onResize();
      }
    }
  }

  private void makeOptionalRows(DivWidget upper) {
    makeContextRow(upper);
    makeContextTransRow(upper);
  }

  private void makeContextRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    context = addContext(container, newUserExercise);
  }

  private void makeContextTransRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    contextTrans = addContextTranslation(container, newUserExercise);
  }

  /**
   * @return
   * @see #addNew
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

  abstract void addItemsAtTop(Panel container);

  private void gotBlur() {
    gotBlur(foreignLang, rap, normalSpeedRecording, listInterface, toAddTo);
  }

  void gotBlur(FormField foreignLang,
               RecordAudioPanel rap,
               ControlGroup normalSpeedRecording,
               ListInterface<CommonShell> pagingContainer,
               Panel toAddTo) {
    grabInfoFromFormAndStuffInfoExercise(newUserExercise.getMutable());
  }

  /**
   * @param row
   * @return
   * @see #addNew
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
   * @paramx exid
   * @paramx exerciseList
   * @paramx learnContainer
   * @paramx editableExerciseList
   * @paraxm button
   * @paramx uniqueID
   * @see EditableExerciseList#makeDeleteButton
   */
 /* void deleteItem(final int exid,
                  // final long uniqueID,
                  final PagingExerciseList<?, ?> exerciseList,
                  final ReloadableContainer learnContainer,
                  final EditableExerciseList editableExerciseList,
                  Button button) {
    listService.deleteItemFromList(originalList.getID(), exid, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        enableButton();
      }

      @Override
      public void onSuccess(Boolean result) {
        enableButton();
        if (!result) {
          logger.warning("deleteItem huh? id " + exid + " not in list " + originalList);
        }

        exerciseList.forgetExercise(exid);

        if (!originalList.removeAndCheck(exid)) {
          logger.warning("deleteItem huh? didn't remove the item " + exid + " from " + originalList.getID() +
              " now " + originalList.getExercises().size());
        }
        if (learnContainer != null && learnContainer.getReloadable() != null) {
          learnContainer.getReloadable().redraw();   // TODO : or reload???
        }
        logger.warning("deleteItem list size is " + exerciseList.getSize());
        editableExerciseList.enableRemove(exerciseList.getSize() > 0);
      }

      private void enableButton() {
        button.setEnabled(true);
      }
    });
  }*/

  private void makeEnglishRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    english = makeBoxAndAnno(row, getEnglishLabel(), "", englishAnno);
  }

  String getEnglishLabel() {
    return isEnglish() ? ENGLISH_LABEL_2 : ENGLISH_LABEL;
  }

  private void makeForeignLangRow(Panel container) {
    //if (DEBUG) logger.info("EditableExerciseDialog.makeForeignLangRow --->");
    Panel row = new FluidRow();
    container.add(row);

    foreignAnno.getElement().setId("foreignLanguageAnnotation");
//    if (DEBUG) logger.info("makeForeignLangRow make fl row " + foreignAnno);
    foreignLang = makeBoxAndAnno(row, controller.getLanguage(), "", foreignAnno);
    foreignLang.box.setDirectionEstimator(true);   // automatically detect whether text is RTL
    setMarginBottom(foreignLang);
  }

  private String getLanguage() {
    return controller.getLanguage();
  }

  protected void setMarginBottom(FormField foreignLang) {
    foreignLang.box.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);
  }

  private void makeTranslitRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    String subtext = "";
    translit = makeBoxAndAnno(row, getTransliterationLabel(), subtext, translitAnno);
  }

  String getTransliterationLabel() {
    return TRANSLITERATION_OPTIONAL;
  }

  private FormField addContext(Panel container, CommonExercise newUserExercise) {
    FormField formField = makeBoxAndAnno(container, CONTEXT_LABEL, "", contextAnno);

    TextBoxBase box = formField.box;
    box.setText(originalContext = newUserExercise.getContext());
    box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        gotBlur();
        logBlur("ContextBox = ", box);
      }
    });

    useAnnotation(newUserExercise, CONTEXT, contextAnno);
    return formField;
  }

  private void logBlur(String prefix, TextBoxBase box) {
    try {
      long uniqueID = originalList.getID();
      controller.logEvent(box, "TextBox", "UserList_" + uniqueID, prefix + box.getValue());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private FormField addContextTranslation(Panel container,
                                          CommonExercise newUserExercise) {
    FormField formField = makeBoxAndAnno(container, CONTEXT_TRANSLATION_LABEL, "", contextTransAnno);

    TextBoxBase box1 = formField.box;
    box1.setText(originalContextTrans = newUserExercise.getContextTranslation());
    box1.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        gotBlur();
        logBlur("ContextTransBox = ", box1);
      }
    });
    useAnnotation(newUserExercise, CONTEXT_TRANSLATION, contextTransAnno);
    return formField;
  }

  abstract void  useAnnotation(AnnotationExercise userExercise, String field, HTML annoField);

  /**
   * @param newUserExercise
   * @seex EditItem#addEditOrAddPanel
   */
  public void setFields(CommonExercise newUserExercise) {
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
        startCreatingExercise(rap, foreignLang, normalSpeedRecording, pagingContainer, toAddTo);
      }
    });
    submit.addStyleName("rightFiveMargin");
    submit.addStyleName("floatRight");

    return submit;
  }

/*  protected Button makeCancelButton() {
    final Button submit = new Button("Cancel");
    submit.setType(ButtonType.INFO);
    submit.getElement().setId("CancelButton_NewExercise_for_" + originalList.getID());
    controller.register(submit, "UserList_" + originalList.getID());
    submit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        modal.hide();
      }
    });
    submit.addStyleName("rightFiveMargin");
    submit.addStyleName("floatRight");

    return submit;
  }*/

  /**
   * @param rap
   * @param foreignLang
   * @param normalSpeedRecording
   * @param pagingContainer
   * @param toAddTo
   */
  private void startCreatingExercise(RecordAudioPanel rap,
                                     FormField foreignLang,
                                     ControlGroup normalSpeedRecording,
                                     ListInterface<CommonShell> pagingContainer,
                                     Panel toAddTo) {
    if (rap.isRecording()) {
      clickedCreate = true;
      rap.clickStop();
    } else if (rapSlow.isRecording()) {
      clickedCreate = true;
      rapSlow.clickStop();
    } else {
      validateThenPost(foreignLang, rap, normalSpeedRecording, pagingContainer, toAddTo, true, true);
    }
  }

  /**
   * @param foreignLang
   * @param rap
   * @param normalSpeedRecording
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
                        ListInterface<CommonShell> pagingContainer,
                        Panel toAddTo,
                        boolean onClick,
                        boolean foreignChanged) {
    if (foreignLang.getSafeText().isEmpty()) {
      markError(foreignLang, ENTER_THE_FOREIGN_LANGUAGE_PHRASE);
    } else if (!isEnglish() && english.getSafeText().isEmpty()) {
      String enterTheEnglishPhrase = isEnglish() ? ENTER_MEANING : ENTER_THE_ENGLISH_PHRASE;
      if (!isEnglish()) {
        markError(english, enterTheEnglishPhrase);
      }
    } else if (validateForm(foreignLang, rap, normalSpeedRecording, foreignChanged)) {
      isValidForeignPhrase(pagingContainer, toAddTo, onClick);
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
   * @param pagingContainer
   * @param toAddTo
   * @param onClick
   * @see #validateThenPost
   */
  private void isValidForeignPhrase(final ListInterface<CommonShell> pagingContainer,
                                    final Panel toAddTo,
                                    final boolean onClick) {
    //  logger.info("isValidForeignPhrase : checking phrase " + foreignLang.getSafeText() + " before adding/changing " + newUserExercise);
    controller.getScoringService().isValidForeignPhrase(foreignLang.getSafeText(), "", new AsyncCallback<Boolean>() {
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
          afterValidForeignPhrase(pagingContainer, toAddTo, onClick);
        } else {
          markError(foreignLang, "The " + FOREIGN_LANGUAGE +
              " text is not in our " + getLanguage() + " dictionary. Please edit.");
        }
      }
    });
  }

  /**
   * @param mutableExercise
   * @see EditableExerciseDialog#postEditItem
   */
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

    Collection<CommonExercise> directlyRelated = mutableExercise.getDirectlyRelated();

    if (directlyRelated.isEmpty()) {
      logger.warning("no context sentence on "+ mutableExercise.getID());
    }
    else {
      CommonExercise commonExercise = directlyRelated.iterator().next();
      MutableExercise mutable = commonExercise.getMutable();

      mutable.setForeignLanguage(context.getSafeText());

      if (isEnglish()) {
        mutable.setMeaning(contextTrans.getSafeText());
      } else {
        mutable.setEnglish(contextTrans.getSafeText());
      }

      logger.info("context now " + mutable.getDirectlyRelated().iterator().next().getForeignLanguage());
    }
  }

  /**
   * @see #isValidForeignPhrase(ListInterface, Panel, boolean)
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
   * @param row
   * @param label
   * @param subtext
   * @param annoBox
   * @return
   * @see #makeEnglishRow(Panel)
   */
  private FormField makeBoxAndAnno(Panel row, String label, String subtext, HTML annoBox) {
    FormField formField = addControlFormFieldHorizontal(row, label, subtext,
        false, 1, annoBox,
        LABEL_WIDTH, TEXT_FIELD_WIDTH);
    setMarginBottom(formField);

    annoBox.addStyleName("leftFiveMargin");
    annoBox.addStyleName("editComment");
    return formField;
  }

  /**
   * @param exerciseList
   * @param toAddTo
   * @param onClick
   * @see #isValidForeignPhrase
   */
  abstract void afterValidForeignPhrase(final ListInterface<CommonShell> exerciseList,
                               final Panel toAddTo,
                               boolean onClick);
/*  {
    //   logger.info("user list is " + ul);
    audioServiceAsync.reallyCreateNewItem(originalList.getID(), newUserExercise, getLanguage(), new AsyncCallback<CommonExercise>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(CommonExercise newExercise) {
        afterItemCreated(newExercise, exerciseList, toAddTo);
      }
    });
  }*/

  /**
   * @param row
   * @param recordRegularSpeed
   * @param instance
   * @return
   * @see #makeRegularAudioPanel
   * @see #makeSlowAudioPanel
   */
  CreateFirstRecordAudioPanel makeRecordAudioPanel(final Panel row,
                                                   boolean recordRegularSpeed,
                                                   String instance) {
    return new CreateFirstRecordAudioPanel(newUserExercise, row, recordRegularSpeed, instance);
  }

  class CreateFirstRecordAudioPanel extends RecordAudioPanel<CommonExercise> {
    final boolean recordRegularSpeed;
    private RecordAudioPanel otherRAP;
    private WaveformPostAudioRecordButton postAudioButton;

    /**
     * @param newExercise
     * @param row
     * @param recordRegularSpeed
     * @param instance
     * @see #makeRecordAudioPanel
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
      validateThenPost(foreignLang, rap, normalSpeedRecording, listInterface, toAddTo, false, true);
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
    return newUserExercise == null;
  }

/*
  public void setFocus() {
    setFocusOn(foreignLang.box);
  }*/

/*  private void setFocusOn(final FocusWidget widget) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        widget.setFocus(true);
      }
    });
  }*/
}
