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
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.exercise.WaveformPostAudioRecordButton;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.FormField;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.*;
import org.jetbrains.annotations.NotNull;

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
  public static final String CONTEXT_BOX = "ContextBox = ";
  private final Logger logger = Logger.getLogger("NewUserExercise");

  private static final String OPTIONAL = "optional";
  private static final String WIDGET_ID = "NewUserExercise_WaveformPostAudioRecordButton_";

  public static final String CONTEXT = "context";
  static final String CONTEXT_TRANSLATION = "context translation";
  /**
   * @see #addContext
   */
  private static final String CONTEXT_LABEL = "Context";
  private static final String CONTEXT_TRANSLATION_LABEL = "C. Translation";

  private static final int TEXT_FIELD_WIDTH = 500;
  private static final int LABEL_WIDTH = 105;

  private static final String FOREIGN_LANGUAGE = "Foreign Language";
  private static final String ENGLISH_LABEL = "English";// (optional)";
  private static final String ENGLISH_LABEL_2 = "Meaning";
  private static final String TRANSLITERATION_OPTIONAL = "Transliteration";
  static final String NORMAL_SPEED_REFERENCE_RECORDING = "Normal speed reference recording";
  static final String SLOW_SPEED_REFERENCE_RECORDING_OPTIONAL = "Slow speed reference recording (optional)";
  private static final String ENTER_THE_FOREIGN_LANGUAGE_PHRASE = "Enter the foreign language phrase.";

  private static final String RECORD_REFERENCE_AUDIO_FOR_THE_FOREIGN_LANGUAGE_PHRASE = "Record reference audio for the foreign language phrase.";

  final CommonExercise newUserExercise;

  final ExerciseController controller;

  protected String originalForeign = "";
  protected String originalEnglish = "";
  protected String originalRefAudio;
  protected String originalSlowRefAudio;
  protected String originalTransliteration;
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

  private final int listID;
  /**
   * TODO : What is this for???
   */
  ListInterface<CommonShell, CommonExercise> listInterface;
  private Panel toAddTo;
  private boolean clickedCreate = false;
  final String instance;

  private static final boolean DEBUG = false;

  /**
   * @param controller
   * @param newExercise
   * @param instance
   * @param listID
   * @see EditableExerciseDialog#EditableExerciseDialog
   */
  public NewUserExercise(
      ExerciseController controller,
      CommonExercise newExercise,
      String instance,
       int listID) {
    this.controller = controller;
    this.newUserExercise = newExercise;
    this.instance = instance;
    this.listID = listID;
  }

  /**
   * @param listInterface
   * @param toAddTo
   * @return
   * @see EditItem#setFactory
   */
  public Panel addFields(final ListInterface<CommonShell, CommonExercise> listInterface, final Panel toAddTo) {
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

    upper.add(getDominoEditInfo());

    makeForeignLangRow(upper);
//    int listID = originalList.getID();
    final String id1 = "" + listID;

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
    this.listInterface = listInterface;

    Panel buttonRow = getCreateButton(listInterface, toAddTo, normalSpeedRecording);
    if (buttonRow != null) {
      container.add(buttonRow);
   }

    addBlurHandler(listID, foreignLang);
    addBlurHandler(listID, translit);
    addBlurHandler(listID, english);
    return container;
  }

  private void addBlurHandler(final int id1, FormField field) {
    field.box.addBlurHandler(event -> {
      gotBlur();
      controller.logEvent(field.box, "TextBox", "UserList_" + id1, "ForeignLangBox = " + field.box.getValue());
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

  @NotNull
  protected DivWidget getDominoEditInfo() {
    DivWidget h= new DivWidget();
    h.addStyleName("leftFiveMargin");
    h.addStyleName("bottomFiveMargin");
    h.addStyleName("inlineFlex");
    HTML child1 = new HTML("To edit text, go into domino, edit the item, and then re-import.");
    h.add(child1);
    Anchor child = new Anchor("Click here to go to domino.");
    child.addStyleName("leftFiveMargin");
    child.setTarget("_blank");
    child.setHref(controller.getProps().getDominoURL());
    h.add(child);
    return h;
  }

  /**
   * @return
   * @see #addFields
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

  abstract void gotBlur(FormField foreignLang,
                        RecordAudioPanel rap,
                        ControlGroup normalSpeedRecording,
                        ListInterface<CommonShell,CommonExercise> pagingContainer,
                        Panel toAddTo);

  /**
   * @param row
   * @return
   * @see #addFields
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

  private void makeEnglishRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    english = makeBoxAndAnno(row, getEnglishLabel(), "", englishAnno);
    markPlaceholder(english.box, isEnglish() ? newUserExercise.getMeaning() : newUserExercise.getEnglish());
  }

  String getEnglishLabel() {
    return isEnglish() ? ENGLISH_LABEL_2 : ENGLISH_LABEL;
  }

  protected FormField makeForeignLangRow(Panel container) {
    //if (DEBUG) logger.info("EditableExerciseDialog.makeForeignLangRow --->");
    Panel row = new FluidRow();
    container.add(row);

    foreignAnno.getElement().setId("foreignLanguageAnnotation");
//    if (DEBUG) logger.info("makeForeignLangRow make fl row " + foreignAnno);
    foreignLang = makeBoxAndAnno(row, controller.getLanguage(), "", foreignAnno);
    foreignLang.box.setDirectionEstimator(true);   // automatically detect whether text is RTL
    setMarginBottom(foreignLang);
    return foreignLang;
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

  /**
   * @param container
   * @param newUserExercise
   * @return
   * @see #makeContextRow
   */
  private FormField addContext(Panel container, CommonExercise newUserExercise) {
    FormField formField = makeBoxAndAnnoArea(container, CONTEXT_LABEL, "", contextAnno);

    TextBoxBase box = formField.box;
    box.setText(originalContext = newUserExercise.getContext());
    markPlaceholder(box, originalContext);
    addOnBlur(box, CONTEXT_BOX);

    useAnnotation(newUserExercise, CONTEXT, contextAnno);
    return formField;
  }

  private void markPlaceholder(TextBoxBase box, String content) {
    if (content.isEmpty()) box.setPlaceholder(OPTIONAL);
  }

  private FormField addContextTranslation(Panel container,
                                          CommonExercise newUserExercise) {
    FormField formField = makeBoxAndAnnoArea(container, CONTEXT_TRANSLATION_LABEL, "", contextTransAnno);

    TextBoxBase box1 = formField.box;
    box1.setText(originalContextTrans = newUserExercise.getContextTranslation());
    markPlaceholder(box1, originalContextTrans);

    addOnBlur(box1, "ContextTransBox = ");
    useAnnotation(newUserExercise, CONTEXT_TRANSLATION, contextTransAnno);
    return formField;
  }

  private void addOnBlur(final TextBoxBase box, final String prefix) {
    box.addBlurHandler(event -> {
      gotBlur();
      logBlur(prefix, box);
    });
  }

  private void logBlur(String prefix, TextBoxBase box) {
    try {
      controller.logEvent(box, "TextBox", "UserList_" + listID, prefix + box.getValue());
    } catch (Exception e) {
      logger.warning("got exception " + e);
      //e.printStackTrace();
    }
  }

  protected String getMeaning(CommonExercise newUserExercise) {
    return newUserExercise.getMeaning().isEmpty() ? newUserExercise.getEnglish() : newUserExercise.getMeaning();
  }

  abstract void useAnnotation(AnnotationExercise userExercise, String field, HTML annoField);

  /**
   * @param pagingContainer
   * @param buttonClicked
   * @param keepAudio
   * @see #reallyChange
   */
  private void editItem(final ListInterface<CommonShell,CommonExercise> pagingContainer, final boolean buttonClicked, boolean keepAudio) {
    controller.getListService().editItem(newUserExercise, keepAudio, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void newExercise) {
        originalForeign = newUserExercise.getForeignLanguage();
        originalEnglish = newUserExercise.getEnglish();
        originalTransliteration = newUserExercise.getTransliteration();
        originalRefAudio = newUserExercise.getRefAudio();
        originalSlowRefAudio = newUserExercise.getSlowAudioRef();
        // if (DEBUG) logger.info("postEditItem : onSuccess " + newUserExercise.getTooltip());
        doAfterEditComplete(pagingContainer, buttonClicked);
      }
    });
  }

  /**
   * Tell predefined list to update itself... since maybe a pre def item changed...
   *
   * @param pagingContainer
   * @param buttonClicked
   * @see #reallyChange(ListInterface, boolean, boolean)
   */
  void doAfterEditComplete(ListInterface<CommonShell,CommonExercise> pagingContainer, boolean buttonClicked) {
    changeTooltip(pagingContainer);
  }

  /**
   * Update the exercise shell in the exercise list with the changes to it's english/fl fields.
   *
   * @param pagingContainer
   * @see #doAfterEditComplete(ListInterface, boolean)
   */
  private void changeTooltip(ListInterface<CommonShell,CommonExercise> pagingContainer) {
    CommonShell byID = pagingContainer.byID(newUserExercise.getID());
    if (DEBUG) logger.info("changeTooltip " + byID);
    if (byID == null) {
      logger.warning("changeTooltip : huh? can't find exercise with id " + newUserExercise.getID());
    } else {
      MutableShell mutableShell = byID.getMutableShell();
      mutableShell.setEnglish(newUserExercise.getEnglish());
      mutableShell.setForeignLanguage(newUserExercise.getForeignLanguage());
      mutableShell.setMeaning(getMeaning(newUserExercise));
//      if (DEBUG || true) logger.info("\tchangeTooltip : for " + newUserExercise.getID() + " now " + newUserExercise);
      pagingContainer.redraw();   // show change to tooltip!
    }
  }

  /**
   * Don't post anything to server unless text actually changed - could get lots of blur events that should be ignored.
   *
   * @param exerciseList
   * @param onClick
   * @see #afterValidForeignPhrase
   */
  void postChangeIfDirty(ListInterface<CommonShell,CommonExercise> exerciseList, boolean onClick) {
    if (anyFieldsDirty() || onClick) {
      if (DEBUG) {
        logger.info("postChangeIfDirty:  change" +
            "\n\tfl         " + foreignChanged() +
            "\n\ttransliter " + translitChanged() +
            "\n\tenglish    " + englishChanged() +
            "\n\tcontext    " + contextChanged() +
            "\n\tc english  " + contextTransChanged() +
            "\n\tref        " + refAudioChanged() +
            "\n\tslow       " + slowRefAudioChanged()
        );
      }
      //    logger.info("postChangeIfDirty keep audio = " + getKeepAudio());
      reallyChange(exerciseList, onClick, getKeepAudio());
    }
  }

  private boolean anyFieldsDirty() {
    return foreignChanged() ||
        translitChanged() ||
        englishChanged() ||
        refAudioChanged() ||
        slowRefAudioChanged() ||
        contextChanged() ||
        contextTransChanged();
  }

  private boolean contextTransChanged() {
    return !originalContextTrans.equals(contextTrans.getSafeText());
  }

  private boolean contextChanged() {
    return !originalContext.equals(context.getSafeText());
  }

  protected boolean getKeepAudio() {
    return false;
  }

  private boolean englishChanged() {
    return !english.box.getText().equals(originalEnglish);
  }

  protected boolean foreignChanged() {
    return !foreignLang.box.getText().equals(originalForeign);
  }

  /**
   * @return
   * @seex #checkForForeignChange
   * @see #postChangeIfDirty(ListInterface, boolean)
   */
  protected boolean translitChanged() {
    String transliteration = newUserExercise.getTransliteration();
    String originalTransliteration = this.originalTransliteration;
    //  logger.info("translitChanged : translit '" + transliteration + "' vs original '" + originalTransliteration + "' changed  = " + changed);
    return !transliteration.equals(originalTransliteration);
  }

  protected boolean refAudioChanged() {
    String refAudio = newUserExercise.getRefAudio();
    return (refAudio == null && originalRefAudio != null) || (refAudio != null && !refAudio.equals(originalRefAudio));
  }

  protected boolean slowRefAudioChanged() {
    String slowAudioRef = newUserExercise.getSlowAudioRef();
    return
        (slowAudioRef == null && originalSlowRefAudio != null) ||
            (slowAudioRef != null && !slowAudioRef.equals(originalSlowRefAudio));
  }

  /**
   * @param pagingContainer
   * @param markFixedClicked
   * @param keepAudio
   * @see #postChangeIfDirty(ListInterface, boolean)
   * @see #audioPosted
   */
  void reallyChange(final ListInterface<CommonShell,CommonExercise> pagingContainer, final boolean markFixedClicked, boolean keepAudio) {
    newUserExercise.getMutable().setCreator(controller.getUserState().getUser());
    grabInfoFromFormAndStuffInfoExercise(newUserExercise.getMutable());
    editItem(pagingContainer, markFixedClicked, keepAudio);
  }

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
    rap.getPostAudioButton().setExerciseID(newUserExercise.getID());
    String refAudio = newUserExercise.getRefAudio();

    if (refAudio != null) {
      rap.getImagesForPath(refAudio);
    }

    // slow speed audio
    rapSlow.getPostAudioButton().setExerciseID(newUserExercise.getID());
    String slowAudioRef = newUserExercise.getSlowAudioRef();

    if (slowAudioRef != null) {
      rapSlow.getImagesForPath(slowAudioRef);
    }
  }

  /**
   * @param pagingContainer
   * @param toAddTo
   * @param normalSpeedRecording
   * @return
   * @see #addFields
   */
  abstract Panel getCreateButton(ListInterface<CommonShell, CommonExercise> pagingContainer,
                                 Panel toAddTo,
                                 ControlGroup normalSpeedRecording);

  /**
   * @param foreignLang
   * @param rap
   * @param normalSpeedRecording
   * @param pagingContainer
   * @param toAddTo
   * @param onClick
   * @param foreignChanged
   * @seex #makeCreateButton
   * @see #audioPosted()
   */
  void validateThenPost(FormField foreignLang,
                        RecordAudioPanel rap,
                        ControlGroup normalSpeedRecording,
                        ListInterface<CommonShell, CommonExercise> pagingContainer,
                        Panel toAddTo,
                        boolean onClick,
                        boolean foreignChanged) {
    if (foreignLang.getSafeText().isEmpty()) {
      markError(foreignLang, ENTER_THE_FOREIGN_LANGUAGE_PHRASE);
//    } else if (!isEnglish() && english.getSafeText().isEmpty()) {
      //     String enterTheEnglishPhrase = isEnglish() ? ENTER_MEANING : ENTER_THE_ENGLISH_PHRASE;
//      if (!isEnglish()) {
//        markError(english, enterTheEnglishPhrase);
//      }
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
  private void isValidForeignPhrase(final ListInterface<CommonShell,CommonExercise> pagingContainer,
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
   * @see #isValidForeignPhrase(ListInterface, Panel, boolean)
   * @see #reallyChange(ListInterface, boolean, boolean)
   */
  private void grabInfoFromFormAndStuffInfoExercise(MutableExercise mutableExercise) {
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

//    Collection<CommonExercise> directlyRelated = mutableExercise.getDirectlyRelated();

 /*
    if (directlyRelated.isEmpty()) {
      if (!context.getSafeText().isEmpty() ||
          !contextTrans.getSafeText().isEmpty()
          ) {
        logger.info("grabInfoFromFormAndStuffInfoExercise no context sentence on " + mutableExercise.getID());
        addContext(controller.getUser(), mutableExercise, projectID);
      }
    }
    */
    updateContextExercise(mutableExercise);
  }

  private void updateContextExercise(MutableExercise mutableExercise) {
    Collection<CommonExercise> directlyRelated = mutableExercise.getDirectlyRelated();

    if (!directlyRelated.isEmpty()) {
      CommonExercise contextSentenceExercise = directlyRelated.iterator().next();
      MutableExercise mutableContext = contextSentenceExercise.getMutable();

      mutableContext.setForeignLanguage(context.getSafeText());

      if (isEnglish()) {
        mutableContext.setMeaning(contextTrans.getSafeText());
      } else {
        mutableContext.setEnglish(contextTrans.getSafeText());
      }

      Collection<CommonExercise> directlyRelated1 = mutableExercise.getDirectlyRelated();
      logger.info("grabInfoFromFormAndStuffInfoExercise context now " + directlyRelated1.iterator().next().getForeignLanguage());
    }
  }

  /**
   * @see #isValidForeignPhrase(ListInterface, Panel, boolean)
   */
  protected void checkIfNeedsRefAudio() {
    if (newUserExercise == null) {
      //logger.info("checkIfNeedsRefAudio : new user ex " + newUserExercise);
      Button recordButton = rap.getButton();
      markError(normalSpeedRecording, recordButton, recordButton, "", RECORD_REFERENCE_AUDIO_FOR_THE_FOREIGN_LANGUAGE_PHRASE, Placement.RIGHT);
      recordButton.addMouseOverHandler(event -> normalSpeedRecording.setType(ControlGroupType.NONE));
    }
  }

  /**
   * @param row
   * @param label
   * @param subtext
   * @param annoBox
   * @return
   * @see #makeEnglishRow
   */
  private FormField makeBoxAndAnno(Panel row, String label, String subtext, HTML annoBox) {
    FormField formField = addControlFormFieldHorizontal(row, label, subtext,
        false, 1, annoBox,
        LABEL_WIDTH, TEXT_FIELD_WIDTH);
    styleBox(annoBox, formField);
    return formField;
  }

  private FormField makeBoxAndAnnoArea(Panel row, String label, String subtext, HTML annoBox) {
    TextArea textBox = new TextArea();
    textBox.setVisibleLines(2);
    FormField formField = getFormField(
        row,
        label,
        subtext,
        1,
        annoBox,
        LABEL_WIDTH,
        TEXT_FIELD_WIDTH,
        textBox);
    styleBox(annoBox, formField);
    return formField;
  }

  private void styleBox(HTML annoBox, FormField formField) {
    setMarginBottom(formField);

    annoBox.addStyleName("leftFiveMargin");
    annoBox.addStyleName("editComment");
  }

  /**
   * @param exerciseList
   * @param toAddTo
   * @param onClick
   * @see #isValidForeignPhrase
   */
  abstract void afterValidForeignPhrase(final ListInterface<CommonShell,CommonExercise> exerciseList,
                                        final Panel toAddTo,
                                        boolean onClick);

  /**
   * @param row
   * @param recordRegularSpeed
   * @param instance
   * @return
   * @see #makeRegularAudioPanel
   * @see #makeSlowAudioPanel
   */
  protected CreateFirstRecordAudioPanel makeRecordAudioPanel(final Panel row,
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

      String speed = (recordRegularSpeed ? "Regular" : "Slow") + "_speed";
      getPostAudioButton().getElement().setId(WIDGET_ID + speed);
      getPlayButton().getElement().setId(WIDGET_ID + "Play_" + speed);
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
              false, // don't record in results table????
              RecordButton.RECORD1,
              RecordButton.STOP1,
              audioType) {
            @Override
            public boolean stopRecording(long duration) {
              otherRAP.setEnabled(true);
              showStop();
              return super.stopRecording(duration);
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
      postAudioButton.getElement().setId(WIDGET_ID + (recordRegularSpeed ? "Regular" : "Slow") + "_speed");
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
        recordButton.addMouseOverHandler(event -> normalSpeedRecording.setType(ControlGroupType.NONE));
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
