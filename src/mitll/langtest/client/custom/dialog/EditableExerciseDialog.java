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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.ReloadableContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.list.Reloadable;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.*;

import java.util.logging.Logger;

/**
 * Creates a dialog that lets you edit an item
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/28/2014.
 */
class EditableExerciseDialog extends NewUserExercise {
  private final Logger logger = Logger.getLogger("EditableExerciseDialog");

  private static final int LABEL_WIDTH = 105;
  private final HTML englishAnno = new HTML();
  private final HTML translitAnno = new HTML();
  private final HTML foreignAnno = new HTML();
  private final HTML fastAnno = new HTML();
  private final HTML slowAnno = new HTML();
  private String originalForeign = "";
  private String originalEnglish = "";

  private final PagingExerciseList<CommonShell, CommonExercise> exerciseList;
  final ReloadableContainer predefinedContentList;

  private String originalRefAudio;
  private String originalSlowRefAudio;
  private String originalTransliteration;

  private static final boolean DEBUG = false;

  /**
   * @param itemMarker
   * @param changedUserExercise
   * @param originalList
   * @paramx predefinedContent   - so we can tell it to update its tooltip
   * @see EditItem#getAddOrEditPanel
   */
  public EditableExerciseDialog(LangTestDatabaseAsync service,
                                ExerciseController controller,
                                EditItem editItem,
                                HasText itemMarker,
                                CommonExercise changedUserExercise,
                                UserList<CommonShell> originalList,

                                PagingExerciseList<CommonShell, CommonExercise> exerciseList,
                                ReloadableContainer predefinedContent,
                                String instanceName) {
    super(service, controller, itemMarker, editItem, changedUserExercise, instanceName, originalList);
    fastAnno.addStyleName("editComment");
    slowAnno.addStyleName("editComment");
    this.originalList = originalList;
    this.exerciseList = exerciseList;
    this.predefinedContentList = predefinedContent;
  }

  @Override
  protected void gotBlur(FormField foreignLang,
                         RecordAudioPanel rap,
                         ControlGroup normalSpeedRecording,
                         UserList<CommonShell> ul,
                         ListInterface<CommonShell> pagingContainer,
                         Panel toAddTo) {
    boolean changed = foreignChanged();
    validateThenPost(foreignLang, rap, normalSpeedRecording, ul, pagingContainer, toAddTo, false, changed);
  }

  @Override
  protected void addItemsAtTop(Panel container) {
    if (!newUserExercise.getUnitToValue().isEmpty()) {
      Panel flow = new HorizontalPanel();
      flow.getElement().setId("addItemsAtTop_unitLesson");
      flow.addStyleName("leftFiveMargin");

      for (String type : controller.getProjectStartupInfo().getTypeOrder()) {
        Heading child = new Heading(4, type, newUserExercise.getUnitToValue().get(type));
        child.addStyleName("rightFiveMargin");
        flow.add(child);
      }

      Heading child = new Heading(4, "Item", ""+newUserExercise.getID());
      child.addStyleName("rightFiveMargin");
      flow.add(child);

      container.add(flow);
    }
  }

  boolean shouldDisableNext() {
    return true;
  }

  /**
   * Add remove from list button
   *
   * @param ul
   * @param pagingContainer
   * @param toAddTo
   * @param normalSpeedRecording
   * @return
   * @see NewUserExercise#addNew
   */
  @Override
  protected Panel getCreateButton(UserList<CommonShell> ul,
                                  ListInterface<CommonShell> pagingContainer,
                                  Panel toAddTo,
                                  ControlGroup normalSpeedRecording) {
//
//    if (logger != null) {
//      logger.info(this.getClass() + " adding create button - editable.");
//    }

    Panel row = new DivWidget();
    row.addStyleName("marginBottomTen");
    PrevNextList prevNext = getPrevNext(pagingContainer);
    prevNext.getElement().setId("PrevNextList");
    prevNext.addStyleName("floatLeft");
    prevNext.addStyleName("rightFiveMargin");
    row.add(prevNext);

    Button delete = makeDeleteButton(ul.getID());

    configureButtonRow(row);
    row.add(delete);

    return row;
  }

  /**
   * @param pagingContainer
   * @return
   * @see #getCreateButton(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, com.github.gwtbootstrap.client.ui.ControlGroup)
   */
  PrevNextList<CommonShell> getPrevNext(ListInterface<CommonShell> pagingContainer) {
    CommonShell shell = pagingContainer.byID(newUserExercise.getID());
    return new PrevNextList<>(shell, exerciseList, shouldDisableNext(), controller);
  }

  /**
   * @see #getCreateButton(UserList, ListInterface, Panel, ControlGroup)
   * @param uniqueID
   * @return
   */
  private Button makeDeleteButton(final long uniqueID) {
    Button delete = makeDeleteButton(ul);

    delete.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        delete.setEnabled(false);
       // logger.info("makeDeleteButton got click to delete " +id);
        deleteItem(newUserExercise.getID(), uniqueID, ul, exerciseList, predefinedContentList);
      }
    });

    return delete;
  }

  /**
   * @param container
   * @return
   * @see #addNew(mitll.langtest.shared.custom.UserList, mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel)
   */
  @Override
  protected void makeEnglishRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    english = makeBoxAndAnno(row, getEnglishLabel(), "", englishAnno);
  }

  /**
   * @param container
   * @see #addNew(UserList, UserList, ListInterface, Panel)
   */
  @Override
  protected void makeForeignLangRow(Panel container) {
    if (DEBUG) logger.info("EditableExerciseDialog.makeForeignLangRow --->");

    Panel row = new FluidRow();
    container.add(row);

    foreignAnno.getElement().setId("foreignLanguageAnnotation");

//    if (DEBUG) logger.info("makeForeignLangRow make fl row " + foreignAnno);

    foreignLang = makeBoxAndAnno(row, controller.getLanguage(), "", foreignAnno);
    foreignLang.box.setDirectionEstimator(true);   // automatically detect whether text is RTL
    // return foreignLang;
  }

  @Override
  protected void makeTranslitRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    String subtext = "";
    translit = makeBoxAndAnno(row, getTransliterationLabel(), subtext, translitAnno);
  }

  String getTransliterationLabel() {
    return TRANSLITERATION_OPTIONAL;
  }

  /**
   * @param row
   * @return
   * @see NewUserExercise#addNew
   */
  @Override
  protected ControlGroup makeRegularAudioPanel(Panel row) {
    rap = makeRecordAudioPanel(row, true, instance);
    fastAnno.addStyleName("topFiveMargin");
    return addControlGroupEntrySimple(row, NORMAL_SPEED_REFERENCE_RECORDING, rap, fastAnno);
  }

  /**
   * @param row
   */
  @Override
  protected ControlGroup makeSlowAudioPanel(Panel row) {
    rapSlow = makeRecordAudioPanel(row, false, instance);
    slowAnno.addStyleName("topFiveMargin");
    return addControlGroupEntrySimple(row, SLOW_SPEED_REFERENCE_RECORDING_OPTIONAL, rapSlow, slowAnno);
  }

  /**
   * @param row
   * @param label
   * @param subtext
   * @param annoBox
   * @return
   * @see #makeEnglishRow(com.google.gwt.user.client.ui.Panel)
   */
  FormField makeBoxAndAnno(Panel row, String label, String subtext, HTML annoBox) {
    FormField formField = addControlFormFieldHorizontal(row, label, subtext, false, 1, annoBox, LABEL_WIDTH);
    annoBox.addStyleName("leftFiveMargin");
    annoBox.addStyleName("editComment");
    return formField;
  }

  /**
   * @param ul
   * @param exerciseList
   * @param toAddTo
   * @param onClick
   * @see #isValidForeignPhrase
   */
  @Override
  void afterValidForeignPhrase(final UserList<CommonShell> ul,
                               final ListInterface<CommonShell> exerciseList,
                               final Panel toAddTo,
                               boolean onClick) {
//    if (DEBUG) logger.info("EditItem.afterValidForeignPhrase : exercise id " + newUserExercise.getOldID());
    checkForForeignChange();
    postChangeIfDirty(exerciseList, onClick);
  }

  @Override
  protected void formInvalid() {
    postChangeIfDirty(exerciseList, false);
  }

  private void postChangeIfDirty(ListInterface<CommonShell> exerciseList, boolean onClick) {
    if (foreignChanged() || translitChanged() || englishChanged() || refAudioChanged() || slowRefAudioChanged() || onClick) {
      //  if (DEBUG) logger.info("postChangeIfDirty:  change " + foreignChanged() + translitChanged() + englishChanged() + refAudioChanged() + slowRefAudioChanged());
      reallyChange(exerciseList, onClick);
    }
  }

  /**
   * So check if the audio is the original audio and the translation has changed.
   * If the translation is new but the audio isn't, ask and clear
   *
   * @return
   * @see #afterValidForeignPhrase(UserList, ListInterface, Panel, boolean)
   */
  boolean checkForForeignChange() {
    boolean didChange = foreignChanged();
    if (DEBUG) logger.info("checkForForeignChange didChange " + didChange);
    if (didChange) {
      String header = getWarningHeader();

      if (DEBUG)
        logger.info("checkForForeignChange normal speed : '" + normalSpeedRecording + "' ref changed '" + refAudioChanged() + "' new ref audio ref '" + newUserExercise.getRefAudio() + "'");

      if (normalSpeedRecording != null && !refAudioChanged() && newUserExercise.getRefAudio() != null) {
        if (DEBUG)
          logger.info("\tcheckForForeignChange show warning : normal speed : '" + (normalSpeedRecording != null) + "' ref changed '" + !refAudioChanged() + "' new ref audio ref '" + (newUserExercise.getRefAudio() != null) + "'");

        markError(normalSpeedRecording, header, getWarningForFL());
      }
      if (slowSpeedRecording != null && !slowRefAudioChanged() && newUserExercise.getSlowAudioRef() != null) {
        markError(slowSpeedRecording, header, getWarningForFL());
      }
      if (!translitChanged() && !translit.isEmpty()) {
        markError(translit, header, "Is the transliteration consistent with \"" + foreignLang.getText() + "\" ?");
      }
    }
    return didChange;
  }

  boolean hasAudio() {
    return (
        normalSpeedRecording != null ||
            newUserExercise.getRefAudio() != null ||
            slowSpeedRecording != null ||
            newUserExercise.getSlowAudioRef() != null);
  }

  /**
   * @return
   * @see #checkForForeignChange()
   * @see ReviewEditableExercise#checkForForeignChange()
   */
  String getWarningHeader() {
    return "Consistent with " + controller.getLanguage() + "?";
  }

  /**
   * @return
   * @see ReviewEditableExercise#checkForForeignChange()
   */
  String getWarningForFL() {
    return "Is the audio consistent with \"" + foreignLang.getText() + "\" ?";
  }

  private boolean englishChanged() {
    return !english.box.getText().equals(originalEnglish);
  }

  private boolean foreignChanged() {
    //    if (b)
//      logger.info("foreignChanged : foreign '" + foreignLang.box.getText() + "' != original '" + originalForeign + "'");
    return !foreignLang.box.getText().equals(originalForeign);
  }

  /**
   * @return
   * @see #checkForForeignChange()
   * @see #postChangeIfDirty(ListInterface, boolean)
   */
  private boolean translitChanged() {
    String transliteration = newUserExercise.getTransliteration();
    String originalTransliteration = this.originalTransliteration;

    //  logger.info("translitChanged : translit '" + transliteration + "' vs original '" + originalTransliteration + "' changed  = " + changed);
    return !transliteration.equals(originalTransliteration);
  }

  private boolean refAudioChanged() {
    String refAudio = newUserExercise.getRefAudio();
    return (refAudio == null && originalRefAudio != null) || (refAudio != null && !refAudio.equals(originalRefAudio));
  }

  private boolean slowRefAudioChanged() {
    String slowAudioRef = newUserExercise.getSlowAudioRef();
    return
        (slowAudioRef == null && originalSlowRefAudio != null) ||
            (slowAudioRef != null && !slowAudioRef.equals(originalSlowRefAudio));
  }

  /**
   * @param pagingContainer
   * @param buttonClicked
   * @see #postChangeIfDirty(mitll.langtest.client.list.ListInterface, boolean)
   * @see #audioPosted()
   */
  void reallyChange(final ListInterface<CommonShell> pagingContainer, final boolean buttonClicked) {
    newUserExercise.getMutable().setCreator(controller.getUser());
    postEditItem(pagingContainer, buttonClicked);
  }

  /**
   * Wait for edit to succeed before altering original fields.
   *
   * @param pagingContainer
   * @param buttonClicked
   * @see #reallyChange(mitll.langtest.client.list.ListInterface, boolean)
   */
  private void postEditItem(final ListInterface<CommonShell> pagingContainer, final boolean buttonClicked) {
    if (DEBUG) logger.info("postEditItem : edit item " + buttonClicked);

    grabInfoFromFormAndStuffInfoExercise(newUserExercise.getMutable());

    listService.editItem(newUserExercise, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void newExercise) {
        originalForeign = newUserExercise.getForeignLanguage();
        originalEnglish = newUserExercise.getEnglish();
        originalTransliteration = newUserExercise.getTransliteration();
        originalRefAudio     = newUserExercise.getRefAudio();
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
   * @see #reallyChange(mitll.langtest.client.list.ListInterface, boolean)
   */
  void doAfterEditComplete(ListInterface<CommonShell> pagingContainer, boolean buttonClicked) {
    changeTooltip(pagingContainer);
    if (predefinedContentList != null) {
      if (DEBUG)
        logger.info("doAfterEditComplete : predef content list not null");// + " id " + predefinedContentList.getCurrentExerciseID());

      Reloadable reloadable = predefinedContentList.getReloadable();

      if (reloadable == null) {
        logger.warning("doAfterEditComplete : reloadable null????");// + " id " + predefinedContentList.getCurrentExerciseID());
      } else {
        reloadable.reloadWithCurrent();
      }
    }
    //else {
      //   if (DEBUG || true) logger.warning("doAfterEditComplete : no predef content " + buttonClicked);// + " id " + predefinedContentList.getCurrentExerciseID());

    //}
  }

  /**
   * @param pagingContainer
   * @see #doAfterEditComplete(ListInterface, boolean)
   */
  private void changeTooltip(ListInterface<CommonShell> pagingContainer) {
    CommonShell byID = pagingContainer.byID(newUserExercise.getID());
    if (DEBUG) logger.info("changeTooltip " + byID);
    if (byID == null) {
      logger.warning("changeTooltip : huh? can't find exercise with id " + newUserExercise.getID());
    } else {
      MutableShell mutableShell = byID.getMutableShell();
      mutableShell.setEnglish(newUserExercise.getEnglish());
      mutableShell.setForeignLanguage(newUserExercise.getForeignLanguage());
//      if (DEBUG || true) logger.info("\tchangeTooltip : for " + newUserExercise.getID() + " now " + newUserExercise);
      pagingContainer.redraw();   // show change to tooltip!
    }
  }

  /**
   * @param newUserExercise
   * @see EditItem#addEditOrAddPanel
   */
  @Override
  public <S extends CommonShell & AudioRefExercise & AnnotationExercise> void setFields(S newUserExercise) {
    //if (DEBUG) logger.info("grabInfoFromFormAndStuffInfoExercise : setting fields with " + newUserExercise);
    // english
    {
      english.box.setText(originalEnglish = newUserExercise.getEnglish());
      ((TextBox) english.box).setVisibleLength(newUserExercise.getEnglish().length() + 4);
      if (newUserExercise.getEnglish().length() > 20) {
        english.box.setWidth("500px");
      }
      useAnnotation(newUserExercise, "english", englishAnno);
    }

    // foreign lang
    {
      foreignLang.box.setText(originalForeign = newUserExercise.getForeignLanguage().trim());
      useAnnotation(newUserExercise, "foreignLanguage", foreignAnno);
    }

    // translit
    translit.box.setText(originalTransliteration = newUserExercise.getTransliteration());
    useAnnotation(newUserExercise, "transliteration", translitAnno);

    if (rap != null) {
      // regular speed audio
      int id = newUserExercise.getID();
      rap.getPostAudioButton().setExercise(id);
      String refAudio = newUserExercise.getRefAudio();

      if (refAudio != null) {
        ExerciseAnnotation annotation = newUserExercise.getAnnotation(refAudio);
        if (annotation == null) {
          useAnnotation(newUserExercise.getAnnotation("refAudio"), fastAnno);
        } else {
          useAnnotation(newUserExercise, refAudio, fastAnno);
        }
        rap.getImagesForPath(refAudio);
        originalRefAudio = refAudio;
      }

      // slow speed audio
      rapSlow.getPostAudioButton().setExercise(id);
      String slowAudioRef = newUserExercise.getSlowAudioRef();

      if (slowAudioRef != null) {
        useAnnotation(newUserExercise, slowAudioRef, slowAnno);
        rapSlow.getImagesForPath(slowAudioRef);
        originalSlowRefAudio = slowAudioRef;
      }
      if (!newUserExercise.hasRefAudio()) {
        useAnnotation(newUserExercise, "refAudio", fastAnno);
      }
    }
  }

  /**
   * @param userExercise
   * @param field
   * @param annoField
   * @see #setFields(CommonShell)
   */
  void useAnnotation(AnnotationExercise userExercise, String field, HTML annoField) {
   // ExerciseAnnotation annotation = ;
    // if (DEBUG) logger.info("useAnnotation anno for " + field + " = " + annotation);
    useAnnotation(userExercise.getAnnotation(field), annoField);
  }

  private void useAnnotation(ExerciseAnnotation anno, final HTML annoField) {
    final boolean isIncorrect = anno != null && !anno.isCorrect();
    // if (DEBUG) logger.info("useAnnotation anno for " + anno + " = " + isIncorrect + " : " + annoField);

    if (isIncorrect) {
      if (anno.getComment().isEmpty()) {
        annoField.setHTML("<i>Empty Comment</i>");
      } else {
        annoField.setHTML("<i>\"" + anno.getComment() + "\"</i>");
      }
    }

    annoField.setVisible(isIncorrect);
  }
}
