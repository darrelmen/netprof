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

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.client.user.FormField;
import mitll.langtest.shared.exercise.ExerciseAnnotation;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.AnnotationExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Creates a dialog that lets you edit an item
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/28/2014.
 * <T extends CommonShell & AudioRefExercise & CombinedMutableUserExercise, UL extends UserList<?>>
 */
class EditableExerciseDialog extends NewUserExercise {
  private final Logger logger = Logger.getLogger("EditableExerciseDialog");

  private final HTML fastAnno = new HTML();
  private final HTML slowAnno = new HTML();

  private final PagingExerciseList<CommonShell, CommonExercise> exerciseList;

  private static final boolean DEBUG = false;

  /**
   * @param changedUserExercise
   * @param originalList
   * @paramx predefinedContent   - so we can tell it to update its tooltip
   * @see EditItem#setFactory
   */
  public EditableExerciseDialog(ExerciseController controller,
                                CommonExercise changedUserExercise,
                                UserList<CommonShell> originalList,

                                PagingExerciseList<CommonShell, CommonExercise> exerciseList,
                                String instanceName) {
    super(controller, changedUserExercise, instanceName, originalList);
    fastAnno.addStyleName("editComment");
    slowAnno.addStyleName("editComment");
    this.originalList = originalList;
    this.exerciseList = exerciseList;
  }

  /**
   * @see
   * @param foreignLang
   * @param rap
   * @param normalSpeedRecording
   * @param pagingContainer
   * @param toAddTo
   */
  @Override
  protected void gotBlur(FormField foreignLang,
                         RecordAudioPanel rap,
                         ControlGroup normalSpeedRecording,
                         ListInterface<CommonShell,CommonExercise> pagingContainer,
                         Panel toAddTo) {
    validateThenPost(foreignLang, rap, normalSpeedRecording, pagingContainer, toAddTo, false, foreignChanged());
  }

  /**
   * @param container
   * @see #addFields
   */
  @Override
  protected void addItemsAtTop(Panel container) {
    Map<String, String> unitToValue = newUserExercise.getUnitToValue();
    if (!unitToValue.isEmpty()) {
      Panel flow = new HorizontalPanel();
      flow.getElement().setId("addItemsAtTop_unitLesson");
      flow.addStyleName("leftFiveMargin");

      for (String type : controller.getProjectStartupInfo().getTypeOrder()) {
        String subtext = unitToValue.get(type);
        if (subtext != null && !subtext.isEmpty()) {
          Heading child = new Heading(4, type, subtext);
          child.addStyleName("rightFiveMargin");
          flow.add(child);
        }
      }

      Heading child = new Heading(4, "Item", "" + newUserExercise.getID());
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
   * @see NewUserExercise#addFields
   */
  @Override
  protected Panel getCreateButton(UserList<CommonShell> ul,
                                  ListInterface<CommonShell,CommonExercise> pagingContainer,
                                  Panel toAddTo,
                                  ControlGroup normalSpeedRecording) {
    Panel row = new DivWidget();
    row.addStyleName("marginBottomTen");
    PrevNextList prevNext = getPrevNext(pagingContainer);
    prevNext.getElement().setId("PrevNextList");
    prevNext.addStyleName("floatLeftAndClear");
    prevNext.addStyleName("rightFiveMargin");
    row.add(prevNext);

    configureButtonRow(row);

    return row;
  }

  /**
   * @param pagingContainer
   * @return
   * @see #getCreateButton(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, com.github.gwtbootstrap.client.ui.ControlGroup)
   */
  PrevNextList<CommonShell> getPrevNext(ListInterface<CommonShell, CommonExercise> pagingContainer) {
    CommonShell shell = pagingContainer.byID(newUserExercise.getID());
    return new PrevNextList<>(shell, exerciseList, shouldDisableNext(), controller);
  }

  /**
   * @param row
   * @return
   * @see NewUserExercise#addFields
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
   * @param exerciseList
   * @param toAddTo
   * @param onClick
   * @see #isValidForeignPhrase
   */
  @Override
  void afterValidForeignPhrase(final ListInterface<CommonShell, CommonExercise> exerciseList,
                               final Panel toAddTo,
                               boolean onClick) {
    //  if (DEBUG) logger.info("EditableExerciseDialog.afterValidForeignPhrase : exercise id " + newUserExercise.getID());
    checkForForeignChange();
    postChangeIfDirty(exerciseList, onClick);
  }

  @Override
  protected void formInvalid() {
    postChangeIfDirty(exerciseList, false);
  }

  /**
   * So check if the audio is the original audio and the translation has changed.
   * If the translation is new but the audio isn't, ask and clear
   *
   * @return
   * @see NewUserExercise#afterValidForeignPhrase(ListInterface, Panel, boolean)
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
        markError(translit, header, "Is the transliteration consistent with \"" + foreignLang.getSafeText() + "\" ?");
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
    return "Is the audio consistent with \"" + foreignLang.getSafeText() + "\" ?";
  }

  /**
   * @param newUserExercise
   * @seex EditItem#addEditOrAddPanel
   */
  @Override
  public void setFields(CommonExercise newUserExercise) {
    if (DEBUG) logger.info("grabInfoFromFormAndStuffInfoExercise : setting fields with " + newUserExercise);

    // foreign lang
    setFL(newUserExercise);

    // translit
    setTranslit(newUserExercise);

    // english
    setEnglish(newUserExercise);

    setContext(newUserExercise);

    setContextTrans(newUserExercise);

    if (rap != null) {
      // regular speed audio
      int id = newUserExercise.getID();
      rap.getPostAudioButton().setExerciseID(id);
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
      rapSlow.getPostAudioButton().setExerciseID(id);
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

  private void setTranslit(CommonExercise newUserExercise) {
    TextBoxBase box = translit.box;
    box.setText(originalTransliteration = newUserExercise.getTransliteration());
    if (originalTransliteration.isEmpty()) {
      box.setPlaceholder("optional");
    }
    setMarginBottom(translit);
    useAnnotation(newUserExercise, "transliteration", translitAnno);
  }

  private void setFL(CommonExercise newUserExercise) {
    foreignLang.box.setText(originalForeign = newUserExercise.getForeignLanguage().trim());
    useAnnotation(newUserExercise, "foreignLanguage", foreignAnno);
  }

  private void setContext(CommonExercise newUserExercise) {
    context.box.setText(originalContext = newUserExercise.getContext().trim());
    useAnnotation(newUserExercise, CONTEXT, contextAnno);
  }

  private void setContextTrans(CommonExercise newUserExercise) {
    contextTrans.box.setText(originalContextTrans = newUserExercise.getContextTranslation().trim());
    useAnnotation(newUserExercise, CONTEXT_TRANSLATION, contextTransAnno);
  }

  private void setEnglish(CommonExercise newUserExercise) {
    String english = isEnglish() ? getMeaning(newUserExercise) : newUserExercise.getEnglish();
    this.english.box.setText(originalEnglish = english);
    ((TextBox) this.english.box).setVisibleLength(english.length() + 4);
    if (english.length() > 20) {
      this.english.box.setWidth("500px");
    }
    String field = isEnglish() ? "meaning" : "english";
    useAnnotation(newUserExercise, field, englishAnno);
  }

  /**
   * @param userExercise
   * @param field
   * @param annoField
   * @see #setFields
   */
  void useAnnotation(AnnotationExercise userExercise, String field, HTML annoField) {
    useAnnotation(userExercise.getAnnotation(field), annoField);
  }

  private void useAnnotation(ExerciseAnnotation anno, final HTML annoField) {
    final boolean isIncorrect = anno != null && !anno.isCorrect();
    if (DEBUG) logger.info("useAnnotation anno for " + anno + " = " + isIncorrect + " : " + annoField);

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
