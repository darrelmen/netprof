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
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.exercise.WaveformPostAudioRecordButton;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.recorder.RecordButton;
import mitll.langtest.client.scoring.UnitChapterItemHelper;
import mitll.langtest.client.services.AudioServiceAsync;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.FormField;
import mitll.langtest.server.database.audio.IAudioDAO;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
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
abstract class NewUserExercise<T extends CommonShell, U extends ClientExercise> extends BasicDialog {
  private final Logger logger = Logger.getLogger("NewUserExercise");

  private static final int MARGIN_BOTTOM = 4;

  private static final String CONTEXT_BOX = "ContextBox = ";
  private static final String CLICK_HERE_TO_GO_TO_DOMINO = "Click here to go to domino.";

  public static final String CONTEXT = "context";
  static final String CONTEXT_TRANSLATION = "context translation";

  private static final int WIDE_TEXT_FIELD_WIDTH = 750;
  private static final int LABEL_WIDTH = 105;

  private static final String ENGLISH_LABEL = "English";
  private static final String ENGLISH_LABEL_2 = "Meaning";
  private static final String TRANSLITERATION_OPTIONAL = "Transliteration";

//  static final String NORMAL_SPEED_REFERENCE_RECORDING = "Normal speed reference recording";
//  static final String SLOW_SPEED_REFERENCE_RECORDING_OPTIONAL = "Slow speed reference recording (optional)";
  //private static final String ENTER_THE_FOREIGN_LANGUAGE_PHRASE = "Enter the foreign language phrase.";
  // private static final String RECORD_REFERENCE_AUDIO_FOR_THE_FOREIGN_LANGUAGE_PHRASE = "Record reference audio for the foreign language phrase.";

  final U newUserExercise;

  final ExerciseController controller;

  String originalForeign = "";
  String originalEnglish = "";
  private String originalTransliteration;

  String originalRefAudio;
  String originalSlowRefAudio;
  private String originalContextAudio = "";

  /**
   * @see EditableExerciseDialog#setFields
   */
  FormField english;
  FormField foreignLang;
  FormField translit;
  FormField context;
  FormField contextTrans;

  final HTML englishAnno = new HTML();
  final HTML translitAnno = new HTML();
  final HTML foreignAnno = new HTML();
  final HTML contextAnno = new HTML();
  final HTML contextTransAnno = new HTML();

  String originalContext = "";
  String originalContextTrans = "";

  /**
   * @see #makeRegularAudioPanel
   */
  CreateFirstRecordAudioPanel rap, rapSlow;
  /**
   * @see #makeContextAudioPanel
   */
  CreateFirstRecordAudioPanel rapContext;

  ControlGroup normalSpeedRecording = null;
  ControlGroup slowSpeedRecording = null;

  private final int listID;
  private Panel toAddTo;

  private ListInterface<T, U> listInterface;

  private static final boolean DEBUG = false;


  /**
   * @param controller
   * @param newExercise
   * @param listID
   * @paramx instance
   * @see EditableExerciseDialog#EditableExerciseDialog
   */
  NewUserExercise(ExerciseController controller, U newExercise, int listID) {
    this.controller = controller;
    this.newUserExercise = newExercise;
    this.listID = listID;
  }

  /**
   * @param listInterface
   * @param toAddTo
   * @return
   * @see EditItem#setFactory
   */
  public Panel addFields(final ListInterface<T, U> listInterface, final Panel toAddTo) {
    this.listInterface = listInterface;
    final FluidContainer container = new ResizableFluid();

    DivWidget upper = new DivWidget();
    upper.getElement().setId("addNewFieldContainer");
    upper.addStyleName("buttonGroupInset4");
    upper.getElement().getStyle().setPaddingBottom(1, Style.Unit.PX);

    {
      container.getElement().setId("NewUserExercise_container");
      Style style = container.getElement().getStyle();
      style.setPaddingLeft(10, Style.Unit.PX);
      style.setPaddingRight(10, Style.Unit.PX);
      container.addStyleName("greenBackground");
    }

    addItemsAtTop(container);
    container.add(upper);

    DivWidget dominoEditInfo = getDominoEditInfo();
    if (dominoEditInfo != null) {
      upper.add(dominoEditInfo);
    }

    makeForeignLangRow(upper);

    {
      //final String id1 = "" + listID;
      // foreignLang.box.getElement().setId("NewUserExercise_ForeignLang_entry_for_list_" + id1);
      // focusOn(formField); // Bad idea since steals the focus after search
      makeTranslitRow(upper);
      // translit.box.getElement().setId("NewUserExercise_Transliteration_entry_for_list_" + id1);

      makeEnglishRow(upper);
      // english.box.getElement().setId("NewUserExercise_English_entry_for_list_" + id1);
    }

    makeOptionalRows(upper);
    // make audio row
    upper.add(makeAudioRow());

    this.toAddTo = toAddTo;

    Panel buttonRow = getCreateButton(toAddTo, normalSpeedRecording);
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

  /**
   * @param upper
   * @see #addFields
   */
  private void makeOptionalRows(DivWidget upper) {
    makeContextRow(upper);
    makeContextTransRow(upper);
  }

  /**
   * Tie to the context audio record box
   *
   * @param container
   */
  private void makeContextRow(Panel container) {
    Panel row = new DivWidget();
    container.add(row);
    context = addContext(container, newUserExercise);
    context.box.addKeyUpHandler(keyUpEvent -> {
      boolean hasText = !context.box.getText().trim().isEmpty();
      //   logger.info("makeContextRow Got key up " + hasText);
      maybeEnableContext(hasText);
    });
  }

  protected void maybeEnableContext(boolean hasText) {
    rapContext.setEnabled(hasText && hasRecordPermission());
  }

  private void makeContextTransRow(Panel container) {
    Panel row = new DivWidget();
    container.add(row);
    contextTrans = addContextTranslation(container, newUserExercise);
  }

  @NotNull
  protected DivWidget getDominoEditInfo() {
    DivWidget h = new DivWidget();
    h.addStyleName("leftFiveMargin");
    h.addStyleName("bottomFiveMargin");
    h.addStyleName("inlineFlex");
    HTML child1 = new HTML("To edit text, go into domino, edit the item, and then re-import.");
    h.add(child1);
    Anchor child = new Anchor(CLICK_HERE_TO_GO_TO_DOMINO);
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
    DivWidget row = new DivWidget();

    normalSpeedRecording = makeRegularAudioPanel(row);
    normalSpeedRecording.addStyleName("buttonGroupInset5");

    slowSpeedRecording = makeSlowAudioPanel(row);
    slowSpeedRecording.addStyleName("buttonGroupInset5");

    ControlGroup contextRecording = makeContextAudioPanel(row);
    contextRecording.addStyleName("buttonGroupInset5");

    List<RecordAudioPanel> raps = new ArrayList<>();

    raps.add(rapSlow);
    raps.add(rapContext);
    rap.setOtherRAPs(raps);

    raps = new ArrayList<>();
    raps.add(rap);
    raps.add(rapContext);
    rapSlow.setOtherRAPs(raps);

    raps = new ArrayList<>();
    raps.add(rap);
    raps.add(rapSlow);
    rapContext.setOtherRAPs(raps);

    // TODO : add notif for context
    return row;
  }

  /**
   * @param container
   * @see #addFields
   */
//  @Override
  protected void addItemsAtTop(Panel container) {
    new UnitChapterItemHelper<U>(controller.getProjectStartupInfo().getTypeOrder()).addUnitChapterItem(newUserExercise, container);
  }

  private void gotBlur() {
    //  logger.info("gotBlur");
    gotBlur(rap, normalSpeedRecording, toAddTo);
  }

  private void gotBlur(RecordAudioPanel rap, ControlGroup normalSpeedRecording, Panel toAddTo) {
    validateThenPost(rap, normalSpeedRecording, toAddTo, false);
  }

  /**
   * @param row
   * @return
   * @see #addFields
   */
  ControlGroup makeRegularAudioPanel(Panel row) {
    rap = makeRecordAudioPanel(newUserExercise, row, AudioType.REGULAR);
    rap.getButton().addClickHandler(clickEvent -> postChangeIfDirty(false));
    return addControlGroupEntrySimple(row, "", rap);
  }

  /**
   * TODO: do this without the cast!
   *
   * @param row
   * @return
   * @see #makeAudioRow
   */
  ControlGroup makeContextAudioPanel(Panel row) {
    List<ClientExercise> directlyRelated = newUserExercise.getDirectlyRelated();

    U next;
    if (directlyRelated.isEmpty()) {   // this should not happen...
      next = newUserExercise;
    } else {
      next = (U) directlyRelated.iterator().next();
    }

    if (DEBUG) logger.info("makeContextAudioPanel make context from " + next.getID() + " " + next.getEnglish() + " " +next.getForeignLanguage());

    rapContext = makeRecordAudioPanel(next, row, AudioType.CONTEXT_REGULAR);
    rapContext.getButton().addClickHandler(clickEvent -> postChangeIfDirty(false));

    rapContext.setEnabled(false);

//    logger.info("rap context " + rapContext.getPlayButton().isVisible());
    return addControlGroupEntrySimple(row, "", rapContext);
  }

  ControlGroup makeSlowAudioPanel(Panel row) {
    rapSlow = makeRecordAudioPanel(newUserExercise, row, AudioType.SLOW);
    return addControlGroupEntrySimple(row, "", rapSlow);
  }

  void configureButtonRow(Panel row) {
    row.addStyleName("buttonGroupInset");
  }

  private void makeEnglishRow(Panel container) {
    Panel row = new DivWidget();
    container.add(row);
    english = makeBoxAndAnno(row, "", englishAnno);
    markPlaceholder(english.box, isEnglish() ? newUserExercise.getMeaning() : newUserExercise.getEnglish(), "Translation (optional)");
  }

  String getEnglishLabel() {
    return isEnglish() ? ENGLISH_LABEL_2 : ENGLISH_LABEL;
  }

  /**
   * @param container
   * @return
   */
  private void makeForeignLangRow(Panel container) {
    //if (DEBUG) logger.info("EditableExerciseDialog.makeForeignLangRow --->");
    Panel row = new DivWidget();
    container.add(row);

    foreignAnno.getElement().setId("foreignLanguageAnnotation");
//    if (DEBUG) logger.info("makeForeignLangRow make fl row " + foreignAnno);
    foreignLang = makeBoxAndAnno(row, "", foreignAnno);
    if (getLanguage().equalsIgnoreCase("urdu")) {
      foreignLang.getWidget().getElement().getStyle().setProperty("fontFamily", "'MyUrduWebFont'");
    }
    foreignLang.box.setDirectionEstimator(true);   // automatically detect whether text is RTL
    setFontSize(foreignLang);
    setMarginBottom(foreignLang);
  }

  private void setFontSize(FormField foreignLang) {
    foreignLang.box.getElement().getStyle().setFontSize(24, Style.Unit.PX);
  }

  private String getLanguage() {
    return controller.getLanguage();
  }

  protected void setMarginBottom(FormField foreignLang) {
    foreignLang.box.getElement().getStyle().setMarginBottom(MARGIN_BOTTOM, Style.Unit.PX);
  }

  private void makeTranslitRow(Panel container) {
    Panel row = new DivWidget();
    container.add(row);
    String subtext = "";
    translit = makeBoxAndAnno(row, subtext, translitAnno);
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
  private FormField addContext(Panel container, U newUserExercise) {
    FormField formField = makeBoxAndAnnoArea(container, "", contextAnno);
    formField.box.getElement().getStyle().setLineHeight(28, Style.Unit.PX);
    setFontSize(formField);

    TextBoxBase box = formField.box;
    box.setDirectionEstimator(true);   // automatically detect whether text is RTL

    box.setText(newUserExercise.getContext());
    markPlaceholder(box, newUserExercise.getContext(), "Context Sentence (optional)");
    addOnBlur(box, CONTEXT_BOX);
    box.addBlurHandler(blurEvent -> gotContextBlur(box.getText()));

    useAnnotation(newUserExercise, CONTEXT, contextAnno);
    return formField;
  }

  /**
   * So if we have a match, we go ahead and attach a copy of the audio to the context exercise
   * If not, we remove any current audio.
   *
   * @param text
   * @see mitll.langtest.server.services.AudioServiceImpl#getTranscriptMatch
   * @see IAudioDAO#getTranscriptMatch
   */
  private void gotContextBlur(String text) {
    if (DEBUG) logger.info("gotContextBlur '" + text + "'");

    int exid = -1;
    int audioID = -1;
    if (!newUserExercise.getDirectlyRelated().isEmpty()) {
      ClientExercise next = newUserExercise.getDirectlyRelated().iterator().next();
      exid = next.getID();
      AudioAttribute first = next.getFirst();
      if (first != null) {
        audioID = first.getUniqueID();
      }
    }

    getAudioService().getTranscriptMatch(controller.getProjectStartupInfo().getProjectid(),
        exid, audioID, true, text, new AsyncCallback<AudioAttribute>() {
          @Override
          public void onFailure(Throwable throwable) {

          }

          @Override
          public void onSuccess(AudioAttribute audioAttribute) {
            if (audioAttribute != null) {
              AudioAnswer audioAnswer = new AudioAnswer();
              audioAnswer.setAudioAttribute(audioAttribute);
              audioAnswer.setPath(audioAttribute.getAudioRef());

              rapContext.getPostAudioButton().useResult(audioAnswer);
              if (DEBUG) logger.info("gotContextBlur got existing audio attribute : " + audioAttribute);
              addToContext(audioAttribute);
            } else {
              if (DEBUG) logger.info("gotContextBlur got NO existing audio attribute for " + text);
              rapContext.getPostAudioButton().useInvalidResult(newUserExercise.getID(), Validity.OK, 0D);
              clearContext();
            }
          }
        });
  }

  private void markPlaceholder(TextBoxBase box, String content, String hint) {
    if (content.isEmpty()) {
      box.setPlaceholder(hint);
    }
  }

  private FormField addContextTranslation(Panel container, U newUserExercise) {
    FormField formField = makeBoxAndAnnoArea(container, "", contextTransAnno);

    TextBoxBase box1 = formField.box;
    box1.setText(newUserExercise.getContextTranslation());
    markPlaceholder(box1, newUserExercise.getContextTranslation(), "Context Sentence Translation (optional)");

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
    }
  }

  protected String getMeaning(U newUserExercise) {
    return newUserExercise.getMeaning().isEmpty() ? newUserExercise.getEnglish() : newUserExercise.getMeaning();
  }

  boolean useAnnotation(AnnotationExercise userExercise, String field, HTML annoField) {
    return false;
  }

  /**
   * @param buttonClicked
   * @param keepAudio
   * @see #reallyChange
   */
  private void editItem(final boolean buttonClicked, boolean keepAudio) {
    getAudioService().editItem(newUserExercise, keepAudio, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("changin an exercise", caught);
      }

      @Override
      public void onSuccess(Void newExercise) {
        originalForeign = newUserExercise.getForeignLanguage();
        originalEnglish = newUserExercise.getEnglish();
        originalTransliteration = newUserExercise.getTransliteration();
        originalRefAudio = newUserExercise.getRefAudio();
        originalSlowRefAudio = newUserExercise.getSlowAudioRef();


        //  if (DEBUG) logger.info("postEditItem : onSuccess ");// + newUserExercise.getTooltip());
        doAfterEditComplete(buttonClicked);

        Scheduler.get().scheduleDeferred((Command) () -> refreshEx());
      }
    });
  }

  private void refreshEx() {
    controller.getExerciseService().refreshExercise(controller.getProjectStartupInfo().getProjectid(),
        newUserExercise.getID(), new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(Void result) {
//            logger.info("OK, updated exercise " + newUserExercise.getID());
          }
        });
  }

  private AudioServiceAsync getAudioService() {
    return controller.getAudioService();
  }

  /**
   * Tell predefined list to update itself... since maybe a pre def item changed...
   *
   * @see #reallyChange(boolean, boolean)
   */
  protected void doAfterEditComplete(boolean buttomClicked) {
    changeTooltip(listInterface);
  }

  /**
   * TODOx : get rid of this
   * Update the exercise shell in the exercise list with the changes to it's english/fl fields.
   *
   * @param pagingContainer
   * @see #doAfterEditComplete
   */

  private void changeTooltip(ListInterface<T, ?> pagingContainer) {
    T byID = pagingContainer.byID(newUserExercise.getID());
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
   * @param toAddTo
   * @param onClick
   * @see #validateThenPost(RecordAudioPanel, ControlGroup, Panel, boolean)
   */
  void afterValidForeignPhrase(final Panel toAddTo, boolean onClick) {
    postChangeIfDirty(onClick);
  }

  /**
   * Don't post anything to server unless text actually changed - could get lots of blur events that should be ignored.
   *
   * @param onClick
   * @see #afterValidForeignPhrase
   */
  void postChangeIfDirty(boolean onClick) {
    if (anyFieldsDirty() || onClick) {
      if (DEBUG) {
        logger.info("postChangeIfDirty:  change" +
            "\n\tref        " + refAudioChanged() +
            "\n\tslow       " + slowRefAudioChanged() +
            "\n\tcontext    " + contextAudioChanged()
        );
      }
      //logger.info("postChangeIfDirty keep audio = " + getKeepAudio());
      reallyChange(onClick, getKeepAudio());
    } else {
      //logger.info("ignore change");
    }
  }

  private boolean anyFieldsDirty() {
    return
        foreignChanged() ||
            translitChanged() ||
            englishChanged() ||

            contextChanged() ||
            contextTransChanged() ||

            refAudioChanged() ||
            contextAudioChanged() ||
            slowRefAudioChanged();
  }

  private boolean contextChanged() {
    return !originalContext.equals(context.getSafeText());
  }

  private boolean contextTransChanged() {
    return !originalContextTrans.equals(contextTrans.getSafeText());
  }

  protected boolean getKeepAudio() {
    return false;
  }

  private boolean englishChanged() {
    return !english.box.getText().equals(originalEnglish);
  }

  private boolean foreignChanged() {
    return !foreignLang.box.getText().equals(originalForeign);
  }

  /**
   * @return
   * @seex #checkForForeignChange
   * @see #postChangeIfDirty(boolean)
   */
  private boolean translitChanged() {
    String transliteration = newUserExercise.getTransliteration();
    String originalTransliteration = this.originalTransliteration;
    //  logger.info("translitChanged : translit '" + transliteration + "' vs original '" + originalTransliteration + "' changed  = " + changed);
    return !transliteration.equals(originalTransliteration);
  }

  private boolean refAudioChanged() {
    return compareRefs(newUserExercise.getRefAudio(), this.originalRefAudio);
  }

  private boolean slowRefAudioChanged() {
    return compareRefs(newUserExercise.getSlowAudioRef(), this.originalSlowRefAudio);
  }

  private boolean contextAudioChanged() {
    if (!newUserExercise.getDirectlyRelated().isEmpty()) {
      ClientExercise next = newUserExercise.getDirectlyRelated().iterator().next();
      AudioAttribute first = next.getFirst();
      if (first == null) {
        return false;
      } else {
        String audioRef = first.getAudioRef();
        boolean b = compareRefs(audioRef, this.originalContextAudio);
        if (b) {
          logger.info("contextAudioChanged : Current context " + audioRef + " vs " + originalContextAudio);
        }
        return b;
      }
    } else {
      return false;
    }
  }

  private boolean compareRefs(String refAudio, String originalRefAudio) {
    return
        (refAudio == null && originalRefAudio != null) ||
            (refAudio != null && originalRefAudio == null) ||
            (refAudio != null && !refAudio.equals(originalRefAudio));
  }


  /**
   * @param newUserExercise
   * @seex EditItem#addEditOrAddPanel
   */
  public abstract void setFields(U newUserExercise);

  /**
   * @param toAddTo
   * @param normalSpeedRecording
   * @return
   * @see #addFields
   */
  abstract Panel getCreateButton(Panel toAddTo, ControlGroup normalSpeedRecording);

  /**
   * @param rap
   * @param normalSpeedRecording
   * @param toAddTo
   * @param onClick
   * @seex #makeCreateButton
   * @see #audioPosted
   */
  void validateThenPost(RecordAudioPanel rap,
                        ControlGroup normalSpeedRecording,
                        Panel toAddTo,
                        boolean onClick) {
    //if (validateForm(rap, normalSpeedRecording)) {
//   logger.info("NewUserExercise.validateThenPost : form is valid");
    isValidForeignPhrase(toAddTo, onClick);
//    } else {
//      formInvalid();
//      logger.info("NewUserExercise.validateThenPost : form not valid");
//    }
  }

  protected boolean isEnglish() {
    return controller.getLanguage().equalsIgnoreCase("english");
  }

  void formInvalid() {
  }

  /**
   * Ask the server if the foreign lang text is in our dictionary and can be run through hydec.
   *
   * @param toAddTo
   * @param onClick
   * @paramx pagingContainer
   * @see #validateThenPost
   */
  private void isValidForeignPhrase(final Panel toAddTo, final boolean onClick) {
    if (DEBUG) {
      logger.info("isValidForeignPhrase : checking " +
          "\n\tphrase " + foreignLang.getSafeText() + " before adding/changing " + newUserExercise);
    }

    final FormField foreignLang = this.foreignLang;

    String safeText = foreignLang.getSafeText().trim();

    controller.getScoringService().isValidForeignPhrase(safeText, "", new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("is valid exercise", caught);
      }

      @Override
      public void onSuccess(Boolean result) {
        if (result || safeText.isEmpty()) {
          String safeText1 = context.getSafeText().trim();
          controller.getScoringService().isValidForeignPhrase(safeText1, "", new AsyncCallback<Boolean>() {
            @Override
            public void onFailure(Throwable caught) {
              controller.handleNonFatalError("is valid exercise", caught);
            }

            @Override
            public void onSuccess(Boolean result) {
              if (result || safeText1.isEmpty()) {
                afterValidForeignPhrase(toAddTo, onClick);
              } else {
                markWarn(context, "The " + controller.getLanguage() +
                    " text is not in our " + getLanguage() + " dictionary. Please edit.", Placement.BOTTOM);
              }
            }
          });

        } else {
          markWarn(foreignLang, "The " + controller.getLanguage() +
              " text is not in our " + getLanguage() + " dictionary. Please edit.", Placement.BOTTOM);
        }
      }
    });

  }


  /**
   * Why would we want to change the creator of an exercise?
   *
   * @param markFixedClicked
   * @param keepAudio
   * @see #postChangeIfDirty(boolean)
   * @see #audioPosted
   */
  void reallyChange(final boolean markFixedClicked, boolean keepAudio) {
    //   logger.info("reallyChange - grab fields!");
    ClientExercise clientExercise = grabInfoFromFormAndStuffInfoExercise(newUserExercise);
    editItem(markFixedClicked, keepAudio);

    if (DEBUG) {
      logger.info("reallyChange : edit item " + clientExercise.getID() +
          "\n\teng " + clientExercise.getEnglish() + " " + clientExercise.getForeignLanguage());
    }

    if (contextChanged() || contextTransChanged()) {

      getAudioService().editItem(clientExercise, keepAudio, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
          controller.handleNonFatalError("changing the context exercise", caught);
        }

        @Override
        public void onSuccess(Void newExercise) {
          originalContext = clientExercise.getForeignLanguage();
          originalContextTrans = clientExercise.getEnglish();

          Scheduler.get().scheduleDeferred((Command) () -> refreshEx());
        }
      });
    } else {

    }

  }

  /**
   * @param clientExercise
   * @return context exercise
   * @see #isValidForeignPhrase
   * @see #reallyChange(boolean, boolean)
   */
  private ClientExercise grabInfoFromFormAndStuffInfoExercise(ClientExercise clientExercise) {
    String text = english.getSafeText();

    MutableShell mutableShell = clientExercise.getMutableShell();
    //   logger.info("so english  field is " + text + " fl " + foreignLang.getSafeText());
    //   logger.info("so translit field is " + translit.getSafeText());
    if (isEnglish()) {
      mutableShell.setMeaning(text);
    } else {
      mutableShell.setEnglish(text);
    }
    mutableShell.setForeignLanguage(foreignLang.getSafeText());
    return updateContextExercise(clientExercise);
  }

  /**
   * @param clientExercise
   * @return context sentence exercise
   * @see #grabInfoFromFormAndStuffInfoExercise(ClientExercise)
   */
  private ClientExercise updateContextExercise(ClientExercise clientExercise) {
    List<ClientExercise> directlyRelated = clientExercise.getDirectlyRelated();

    ClientExercise contextSentenceExercise = directlyRelated.iterator().next();
    MutableShell mutableContext = contextSentenceExercise.getMutableShell();

    mutableContext.setForeignLanguage(context.getSafeText());

    if (isEnglish()) {
      mutableContext.setMeaning(contextTrans.getSafeText());
    } else {
      mutableContext.setEnglish(contextTrans.getSafeText());
    }

    return contextSentenceExercise;
  }

  /**
   * @param row
   * @param subtext
   * @param annoBox
   * @return
   * @paramx label
   * @see #makeEnglishRow
   */
  private FormField makeBoxAndAnno(Panel row, String subtext, HTML annoBox) {
    FormField formField = addControlFormFieldHorizontal(row, "", subtext,
        false, 1, annoBox,
        LABEL_WIDTH, WIDE_TEXT_FIELD_WIDTH);
    styleBox(annoBox, formField);
    return formField;
  }

  private FormField makeBoxAndAnnoArea(Panel row, String subtext, HTML annoBox) {
    TextArea textBox = new TextArea();
    textBox.setVisibleLines(2);
    FormField formField = getFormField(
        row,
        "",
        subtext,
        1,
        annoBox,
        LABEL_WIDTH,
        WIDE_TEXT_FIELD_WIDTH,
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
   * @param theExercise
   * @param row
   * @param audioType
   * @return
   * @see #makeRegularAudioPanel
   * @see #makeSlowAudioPanel
   */
  CreateFirstRecordAudioPanel makeRecordAudioPanel(U theExercise, Panel row, AudioType audioType) {
    return new CreateFirstRecordAudioPanel(theExercise, row, audioType);
  }

  class CreateFirstRecordAudioPanel extends RecordAudioPanel<U> {
    final AudioType audioType;
    private List<RecordAudioPanel> otherRAPs;
    private WaveformPostAudioRecordButton postAudioButton;

    /**
     * @param newExercise
     * @param row
     * @param audioType
     * @see #makeRecordAudioPanel
     */
    CreateFirstRecordAudioPanel(U newExercise, Panel row, AudioType audioType) {
      super(newExercise, NewUserExercise.this.controller, row, 0, false, audioType);

      this.audioType = audioType;
      setExercise(newExercise);

      addPlayListener(new PlayListener() {
        @Override
        public void playStarted() {
          disableOthers(false);
        }

        @Override
        public void playStopped() {
          disableOthers(true);
        }
      });

      setEnabled(hasRecordPermission());
      controller.register(getPlayButton(), newExercise.getID());
    }


    private void disableOthers(boolean b) {
//     logger.info("disable others " +b);
      otherRAPs.forEach(otherRAP -> otherRAP.setEnabled(b));
    }

    /**
     * Note that we want to post the audio the server, but not record in the results table (since it's not an answer
     * to an exercise...)
     * That's the final "false" on the end of the WaveformPostAudioRecordButton
     *
     * @return
     * @see mitll.langtest.client.scoring.AudioPanel#makePlayAudioPanel
     * @see RecordAudioPanel#makePlayAudioPanel
     */
    @Override
    protected WaveformPostAudioRecordButton makePostAudioRecordButton(AudioType audioType, String recordButtonTitle) {
      postAudioButton =
          new WaveformPostAudioRecordButton(exercise.getID(), controller, exercisePanel, this,
              0,
              // don't record in results table????
              audioType == AudioType.REGULAR ? "Regular" : audioType == AudioType.CONTEXT_REGULAR ? "Context" : "Slow",
              RecordButton.STOP1,
              audioType) {
            @Override
            public boolean stopRecording(long duration, boolean abort) {
              disableOthers(true);
              showStop();
              return super.stopRecording(duration, abort);
            }

            @Override
            public void startRecording() {
              super.startRecording();
              showStart();
              disableOthers(false);
            }

            @Override
            protected AudioType getAudioType() {
              return audioType;
            }

            /**
             * @see NewUserExercise.CreateFirstRecordAudioPanel#makePostAudioRecordButton
             * @param result
             */
            @Override
            public void useResult(AudioAnswer result) {
              super.useResult(result);

              if (DEBUG) {
                logger.info("useResult got back " + result.getAudioAttribute() +
                    "\n\tfor " + newUserExercise);
              }

              useAudioAttribute(result);
              audioPosted();
            }

            private void useAudioAttribute(AudioAnswer result) {
              AudioAttribute audioAttribute = result.getAudioAttribute();

              if (audioAttribute != null) {
//                logger.info("useAudioAttribute audio type " + audioAttribute.getAudioType());
                useAudioAttribute2(audioAttribute);
              } else {
                logger.warning("useAudioAttribute no valid audio on " + result);
              }
            }

            @Override
            public void useInvalidResult(int exid, Validity validity, double dynamicRange) {
              super.useInvalidResult(exid, validity, dynamicRange);

              MutableAudioExercise mutableAudio = newUserExercise.getMutableAudio();
              if (audioType == AudioType.REGULAR || audioType == AudioType.CONTEXT_REGULAR) {
                mutableAudio.clearRefAudio();
              } else {
                mutableAudio.clearSlowRefAudio();
              }

              audioPosted();
            }
          };
      //postAudioButton.getElement().setId(WIDGET_ID + (recordRegularSpeed ? "Regular" : "Slow") + "_speed");
      return postAudioButton;
    }


    void setOtherRAPs(List<RecordAudioPanel> otherRAPs) {
      this.otherRAPs = otherRAPs;
    }

    WaveformPostAudioRecordButton getPostAudioButton() {
      return postAudioButton;
    }
  }

  private void useAudioAttribute2(AudioAttribute audioAttribute) {
    if (DEBUG) {
      logger.info("useAudioAttribute2 " +
          "\n\taudio id " + audioAttribute.getUniqueID() +
          "\n\tex       " + audioAttribute.getExid() +
          "\n\ttrans    " + audioAttribute.getTranscript());
    }

    if (audioAttribute.getAudioType() == AudioType.CONTEXT_REGULAR) {
      addToContext(audioAttribute);

      rapContext.setAudioPathFromAttribute(audioAttribute);
    } else {
      newUserExercise.getMutableAudio().addAudio(audioAttribute);
    }
  }

  private void addToContext(AudioAttribute audioAttribute) {
    List<ClientExercise> directlyRelated = newUserExercise.getDirectlyRelated();
    if (directlyRelated.isEmpty()) {
      logger.warning("addToContext no context sentence?");
    } else {
      ClientExercise clientExercise = directlyRelated.get(0);
      MutableAudioExercise mutableAudio = clientExercise.getMutableAudio();
      if (clientExercise.hasRefAudio()) {
        if (!mutableAudio.clearRefAudio()) {
          logger.info("addToContext didn't clear audio? " + clientExercise.getAudioAttributes());
        }
      }
      mutableAudio.addAudio(audioAttribute);
      originalContextAudio = audioAttribute.getAudioRef();
    }
  }

  private void clearContext() {
    List<ClientExercise> directlyRelated = newUserExercise.getDirectlyRelated();
    if (directlyRelated.isEmpty()) {
      logger.info("no context sentence?");
    } else {
      ClientExercise clientExercise = directlyRelated.get(0);
      if (!clientExercise.getMutableAudio().clearRefAudio()) {
        logger.info("clearContext Didn't clear context ref audio?");
      } else {
        originalContextAudio = "";
      }
    }
  }

  void audioPosted() {
//    if (clickedCreate) {
//      clickedCreate = false;
    validateThenPost(rap, normalSpeedRecording, toAddTo, false);
//    }

    gotBlur();
  }

  /**
   * Validation checks appear from top to bottom on page -- so should be consistent
   * with how the fields are added.
   *
   * @param rap
   * @param normalSpeedRecording
   * @return
   * @see #validateThenPost
   */
/*  private boolean validateForm(final RecordAudioPanel rap,
                               final ControlGroup normalSpeedRecording) {

    return true;
  }*/
/*  private boolean validRecordingCheck() {
    return newUserExercise == null;
  }*/

  private boolean hasRecordPermission() {
    User current = controller.getUserManager().getCurrent();
    //  logger.info("kind = " +current.getUserKind());
    return !current.isStudent() || !current.getPermissions().isEmpty();
  }
}
