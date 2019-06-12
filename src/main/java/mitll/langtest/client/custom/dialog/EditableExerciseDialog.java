/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.exercise.AnnotationExercise;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseAnnotation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;

import static mitll.langtest.client.custom.dialog.EditableExerciseList.SAFE_TEXT_REPLACEMENT;

abstract class EditableExerciseDialog<T extends CommonShell, U extends ClientExercise>
    extends NewUserExercise<T, U> {
  private final Logger logger = Logger.getLogger("EditableExerciseDialog");

  private static final String FOREIGN_LANGUAGE = "foreignLanguage";
  private static final String TRANSLITERATION = "transliteration";
  private static final String MEANING = "meaning";
  private static final String ENGLISH = "english";

  private final HTML fastAnno = new HTML();
  private final HTML slowAnno = new HTML();

  final INavigation.VIEWS instance;

  private static final boolean DEBUG = false;

  /**
   * @param changedUserExercise
   * @param originalListID
   * @paramx predefinedContent   - so we can tell it to update its tooltip
   * @see EditItem#setFactory
   */
  EditableExerciseDialog(ExerciseController controller,
                         U changedUserExercise,
                         int originalListID,
                         INavigation.VIEWS instanceName) {
    super(controller, changedUserExercise, originalListID);
    this.instance = instanceName;
    fastAnno.addStyleName("editComment");
    slowAnno.addStyleName("editComment");
  }

  boolean shouldDisableNext() {
    return true;
  }

  /**
   * Add remove from list button
   *
   * @param toAddTo
   * @param normalSpeedRecording
   * @return
   * @see NewUserExercise#addFields
   */
  @Override
  protected Panel getCreateButton(Panel toAddTo, ControlGroup normalSpeedRecording) {
    Panel row = new DivWidget();
    row.addStyleName("marginBottomTen");
    return row;
  }

  /**
   * @param row
   * @return
   * @see NewUserExercise#addFields
   */
  @Override
  protected ControlGroup makeRegularAudioPanel(Panel row) {
    // logger.info("makeRegularAudioPanel new user is " + newUserExercise);
    rap = makeRecordAudioPanel(newUserExercise, row, AudioType.REGULAR);
    rap.getButton().addClickHandler(clickEvent -> postChangeIfDirty(false));
    fastAnno.addStyleName("topFiveMargin");
    return addControlGroupEntrySimple(row, "", rap, fastAnno);
  }

  /**
   * @param row
   */
  @Override
  protected ControlGroup makeSlowAudioPanel(Panel row) {
    rapSlow = makeRecordAudioPanel(newUserExercise, row, AudioType.SLOW);
    rapSlow.getButton().addClickHandler(clickEvent -> postChangeIfDirty(false));

    slowAnno.addStyleName("topFiveMargin");
    return addControlGroupEntrySimple(row, "", rapSlow, slowAnno);
  }

//  @Override
//  protected void formInvalid() {
//    postChangeIfDirty(false);
//  }

  /**
   * So check if the audio is the original audio and the translation has changed.
   * If the translation is new but the audio isn't, ask and clear
   *
   * @return
   * @see NewUserExercise#afterValidForeignPhrase(Panel, boolean)
   */
/*  boolean checkForForeignChange() {
    boolean didChange = false;//foreignChanged();
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
  }*/

  boolean hasAudio() {
    return (
        normalSpeedRecording != null ||
            newUserExercise.getRefAudio() != null ||
            slowSpeedRecording != null ||
            newUserExercise.getSlowAudioRef() != null);
  }

  /**
   * @return
   * @seex #checkForForeignChange()
   * @seex ReviewEditableExercise#checkForForeignChange()
   */
/*
  String getWarningHeader() {
    return "Consistent with " + controller.getLanguageInfo().toDisplay()+ "?";
  }
*/

  /**
   * @return
   * @seex ReviewEditableExercise#checkForForeignChange()
   */
  String getWarningForFL() {
    return "Is the audio consistent with \"" + foreignLang.getSafeText() + "\" ?";
  }

  /**
   * @param newUserExercise
   * @seex EditItem#addEditOrAddPanel
   */
  @Override
  public void setFields(U newUserExercise) {
    if (DEBUG || true) {
      logger.info("grabInfoFromFormAndStuffInfoExercise : setting fields with " + newUserExercise);
    }

    // foreign lang
    setFL(newUserExercise);

    setFLNorm(newUserExercise);

    // translit
    setTranslit(newUserExercise);

    // english
    setEnglish(newUserExercise);

    setContext(newUserExercise);
    setContextFLNorm(newUserExercise);
    setContextTrans(newUserExercise);


    // make sure we check if recording should be allowed.
    if (rap != null) {
      addAudio(instance == INavigation.VIEWS.FIX_SENTENCES ? newUserExercise.getDirectlyRelated().iterator().next() : newUserExercise);
      rap.setEnabled();
    }

    if (rapSlow != null) {
      rapSlow.setEnabled();
    }

    if (rapContext != null) {
      rapContext.setEnabled();
    }
  }

  private void setFLNorm(U newUserExercise) {
    String normalizedFL = newUserExercise.getNormalizedFL();
    foreignLangNorm.setText(normalizedFL);
    boolean show = !normalizedFL.isEmpty() && !normalizedFL.equals(newUserExercise.getFLToShow());
    if (show)
      foreignLangNorm.getParent().getElement().getStyle().setMarginTop(-10, Style.Unit.PX);
    foreignLangNorm.setVisible(show);
  }

  private void setContextFLNorm(U newUserExercise) {
    List<ClientExercise> directlyRelated = newUserExercise.getDirectlyRelated();
    if (!directlyRelated.isEmpty()) {
      ClientExercise clientExercise = directlyRelated.get(0);
      String normalizedFL = clientExercise.getNormalizedFL();
  //    logger.info("setContextFLNorm For " + clientExercise.getID() + " " + clientExercise.getFLToShow() + " = '" + clientExercise.getNormalizedFL() + "'");
      foreignLangContextNorm.setText(normalizedFL);
      boolean visible = !normalizedFL.isEmpty() && !normalizedFL.equals(newUserExercise.getFLToShow());
      if (visible)
        foreignLangContextNorm.getParent().getElement().getStyle().setMarginTop(-10, Style.Unit.PX);
      foreignLangContextNorm.setVisible(visible);
    }
  }

  private void addAudio(ClientExercise newUserExercise) {
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

  @Override
  protected CreateFirstRecordAudioPanel makeRecordAudioPanel(U theExercise, final Panel row, AudioType audioType) {
    return new CreateFirstRecordAudioPanel(theExercise, row, audioType) {
      @Override
      protected int getImageWidth() {
        return 700;
      }

      @Override
      protected int getScaledImageHeight(String type) {
        return 40;
      }
    };
  }

  private void setTranslit(U newUserExercise) {
    TextBoxBase box = translit.box;
    String originalTransliteration;
    box.setText(originalTransliteration = newUserExercise.getTransliteration());
    if (originalTransliteration.isEmpty()) {
      box.setPlaceholder("optional");
    }
    setMarginBottom(translit);
    useAnnotation(newUserExercise, TRANSLITERATION, translitAnno);
  }

  /**
   * @param newUserExercise
   * @see #setFields(ClientExercise)
   */
  private void setFL(U newUserExercise) {
    foreignLang.box.setText(originalForeign = normalize(newUserExercise.getForeignLanguage()));
    useAnnotation(newUserExercise, FOREIGN_LANGUAGE, foreignAnno);
  }

  @NotNull
  private String normalize(String foreignLanguage) {
    return foreignLanguage.trim().replaceAll(SAFE_TEXT_REPLACEMENT, "'");
  }

  /**
   * Only the first context sentence...
   *
   * @param newUserExercise
   */
  private void setContext(U newUserExercise) {
    context.box.setText(originalContext = newUserExercise.getContext().trim());

    if (rapContext != null) {
      String text = context.box.getText();
      boolean val = !text.trim().isEmpty();
      //   logger.info("setContext Set context '" + text + "' = " + val);
      maybeEnableContext(val);
    }

    if (!useAnnotation(newUserExercise, CONTEXT, contextAnno)) {
      List<ClientExercise> directlyRelated = newUserExercise.getDirectlyRelated();

      if (!directlyRelated.isEmpty()) {
        useAnnotation(directlyRelated.get(0), FOREIGN_LANGUAGE, contextAnno);
      }
    }
  }

  private void setContextTrans(U newUserExercise) {
    contextTrans.box.setText(originalContextTrans = newUserExercise.getContextTranslation().trim());
    if (!useAnnotation(newUserExercise, CONTEXT_TRANSLATION, contextTransAnno)) {
      List<ClientExercise> directlyRelated = newUserExercise.getDirectlyRelated();

      if (!directlyRelated.isEmpty()) {
        useAnnotation(directlyRelated.get(0), ENGLISH, contextTransAnno);
      }
    }
  }

  private void setEnglish(U newUserExercise) {
    String english = normalize(isEnglish() ? getMeaning(newUserExercise) : newUserExercise.getEnglish());

    this.originalEnglish = english;
    this.english.box.setText(english);
    ((TextBox) this.english.box).setVisibleLength(english.length() + 4);
//    if (english.length() > 20) {
//      this.english.box.setWidth("500px");
//    }

    useAnnotation(newUserExercise, isEnglish() ? MEANING : ENGLISH, englishAnno);
  }

  /**
   * @param userExercise
   * @param field
   * @param annoField
   * @see #setFields
   */
  @Override
  boolean useAnnotation(AnnotationExercise userExercise, String field, HTML annoField) {
    //  logger.info("useAnnotation " + userExercise.getID() + " : " + field);
    //ExerciseAnnotation annotation = userExercise.getAnnotation(field);
    //   logger.info("useAnnotation annotation " + annotation);
    return useAnnotation(userExercise.getAnnotation(field), annoField);
  }

  /**
   * @param anno
   * @param annoField
   * @return
   */
  private boolean useAnnotation(ExerciseAnnotation anno, final HTML annoField) {
    final boolean isIncorrect = anno != null && !anno.isCorrect();
    if (DEBUG) logger.info("useAnnotation anno for " + anno + " = " + isIncorrect + " : " + annoField);

    if (isIncorrect) {
      String comment = anno.getComment();
      if (comment.isEmpty()) {
        annoField.setHTML("<i>Empty Comment</i>");
      } else {
        annoField.setHTML("<i>\"" + comment + "\"</i>");
      }
    }

    annoField.setVisible(isIncorrect);
    return isIncorrect;
  }
}
