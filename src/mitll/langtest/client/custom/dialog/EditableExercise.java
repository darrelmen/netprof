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
import mitll.langtest.client.custom.content.NPFHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.RecordAudioPanel;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.PagingExerciseList;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.CommonUserExercise;
import mitll.langtest.shared.ExerciseAnnotation;
import mitll.langtest.shared.custom.UserList;

/**
 * Creates a dialog that lets you edit an item
 *
* Created by GO22670 on 3/28/2014.
*/
class EditableExercise extends NewUserExercise {
  public static final int LABEL_WIDTH = 105;
  private final HTML englishAnno = new HTML();
  private final HTML translitAnno = new HTML();
  private final HTML foreignAnno = new HTML();
  private final HTML fastAnno = new HTML();
  private final HTML slowAnno = new HTML();
  private String originalForeign = "";
  private String originalEnglish = "";
  final UserList originalList;

  protected PagingExerciseList exerciseList;
  protected ListInterface predefinedContentList;
  protected NPFHelper npfHelper;

  /**
   *
   * @param itemMarker
   * @param changedUserExercise
   * @param originalList
   * @param predefinedContent - so we can tell it to update its tooltip
   * @see EditItem#getAddOrEditPanel
   */
  public EditableExercise(LangTestDatabaseAsync service,
                          ExerciseController controller,
                          EditItem editItem,
                          HasText itemMarker, CommonUserExercise changedUserExercise, UserList originalList,

                          PagingExerciseList exerciseList,
                          ListInterface predefinedContent,
                          NPFHelper npfHelper) {
    super(service, controller, itemMarker, editItem, changedUserExercise);
    fastAnno.addStyleName("editComment");
    slowAnno.addStyleName("editComment");
    this.originalList = originalList;
    this.exerciseList = exerciseList;
    this.predefinedContentList = predefinedContent;
    this.npfHelper = npfHelper;
  }

  @Override
  protected void gotBlur(FormField english, FormField foreignLang, RecordAudioPanel rap,
                         ControlGroup normalSpeedRecording, UserList ul, ListInterface pagingContainer,
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

      for (String type : controller.getStartupInfo().getTypeOrder()) {
        Heading child = new Heading(4, type, newUserExercise.getUnitToValue().get(type));
        child.addStyleName("rightFiveMargin");
        flow.add(child);
      }

      Heading child = new Heading(4, "Item", newUserExercise.getID());
      child.addStyleName("rightFiveMargin");
      flow.add(child);

      container.add(flow);
    }
  }

  boolean shouldDisableNext() {  return true; }

  /**
   * Add remove from list button
   *
   * @see NewUserExercise#addNew
   * @param ul
   * @param pagingContainer
   * @param toAddTo
   * @param normalSpeedRecording
   * @return
   */
  @Override
  protected Panel getCreateButton(final UserList ul, ListInterface pagingContainer, Panel toAddTo,
                                  ControlGroup normalSpeedRecording) {
    Panel row = new DivWidget();
    row.addStyleName("marginBottomTen");
    PrevNextList prevNext = getPrevNext(pagingContainer);
    prevNext.getElement().setId("PrevNextList");
    prevNext.addStyleName("floatLeft");
    prevNext.addStyleName("rightFiveMargin");
    row.add(prevNext);

    Button delete = makeDeleteButton(ul.getUniqueID());

    configureButtonRow(row);
    row.add(delete);

    return row;
  }

  /**
   * @see #getCreateButton(mitll.langtest.shared.custom.UserList, mitll.langtest.client.list.ListInterface, com.google.gwt.user.client.ui.Panel, com.github.gwtbootstrap.client.ui.ControlGroup)
   * @param pagingContainer
   * @return
   */
  PrevNextList getPrevNext(ListInterface pagingContainer) {
    return new PrevNextList(pagingContainer.byID(newUserExercise.getID()), exerciseList, shouldDisableNext(), controller);
  }

  private Button makeDeleteButton(final long uniqueID) {
    Button delete = makeDeleteButton(ul);

    delete.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        deleteItem(newUserExercise.getID(), uniqueID, ul, exerciseList, npfHelper.npfExerciseList);
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
  protected Panel makeEnglishRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    english = makeBoxAndAnno(row, getEnglishLabel(), "(optional)", englishAnno);
    return row;
  }

  protected String getEnglishLabel() {  return ENGLISH_LABEL;  }

  /**
   * @param container
   */
  @Override
  protected FormField makeForeignLangRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    foreignLang = makeBoxAndAnno(row, controller.getLanguage(), "", foreignAnno);
    foreignLang.box.setDirectionEstimator(true);   // automatically detect whether text is RTL
    return foreignLang;
  }

  @Override
  protected void makeTranslitRow(Panel container) {
    Panel row = new FluidRow();
    container.add(row);
    translit = makeBoxAndAnno(row, getTransliterationLabel(), "(optional)", translitAnno);
  }

  protected String getTransliterationLabel() {  return TRANSLITERATION_OPTIONAL;  }

  /**
   * @see NewUserExercise#addNew
   * @param row
   * @return
   */
  @Override
  protected ControlGroup makeRegularAudioPanel(Panel row) {
    rap = makeRecordAudioPanel(row, true);
    fastAnno.addStyleName("topFiveMargin");
    return addControlGroupEntrySimple(row, NORMAL_SPEED_REFERENCE_RECORDING, rap, fastAnno);
  }

  /**
   *
   * @param row
   */
  @Override
  protected ControlGroup makeSlowAudioPanel(Panel row) {
    rapSlow = makeRecordAudioPanel(row, false);
    slowAnno.addStyleName("topFiveMargin");

    return addControlGroupEntrySimple(row, SLOW_SPEED_REFERENCE_RECORDING_OPTIONAL, rapSlow, slowAnno);
  }

  /**
   * @see #makeEnglishRow(com.google.gwt.user.client.ui.Panel)
   * @param row
   * @param label
   * @param subtext
   * @param annoBox
   * @return
   */
  protected FormField makeBoxAndAnno(Panel row, String label, String subtext, HTML annoBox) {
    FormField formField = addControlFormFieldHorizontal(row, label, subtext, false, 1, annoBox, LABEL_WIDTH);
    annoBox.addStyleName("leftFiveMargin");
    annoBox.addStyleName("editComment");
    return formField;
  }

  /**
   * @see #isValidForeignPhrase
   * @param ul
   * @param exerciseList
   * @param toAddTo
   * @param onClick
   */
  @Override
  protected void afterValidForeignPhrase(final UserList ul, final ListInterface exerciseList, final Panel toAddTo, boolean onClick) {
    System.out.println("EditItem.afterValidForeignPhrase : exercise id " + newUserExercise.getID());
    checkForForeignChange();

    postChangeIfDirty(exerciseList, onClick);
  }

  @Override
  protected void formInvalid() {
    postChangeIfDirty(exerciseList, false);
  }

  private void postChangeIfDirty(ListInterface exerciseList, boolean onClick) {
    if (foreignChanged() || translitChanged() || englishChanged() || refAudioChanged() || slowRefAudioChanged() || onClick) {
      System.out.println("postChangeIfDirty:  change " + foreignChanged() + translitChanged() + englishChanged() + refAudioChanged() + slowRefAudioChanged());
      reallyChange(exerciseList, onClick);
    }
  }

  /**
   * So check if the audio is the original audio and the translation has changed.
   * If the translation is new but the audio isn't, ask and clear
   *
   * @paramx listener
   * @return
   */
  boolean checkForForeignChange() {
    if (foreignChanged()) {
      String header = getWarningHeader();
      if (normalSpeedRecording != null && !refAudioChanged() && newUserExercise.getRefAudio() != null) {
        markError(normalSpeedRecording, header, getWarningForFL());
      }
      if (slowSpeedRecording != null && !slowRefAudioChanged() && newUserExercise.getSlowAudioRef() != null) {
        markError(slowSpeedRecording, header, getWarningForFL());
      }
      if (!translitChanged()) {
        markError(translit, header, "Is the transliteration consistent with \"" + foreignLang.getText() + "\" ?");
      }
      return true;
    } else return false;
  }

  protected String getWarningHeader() {
    return "Consistent with " + controller.getLanguage() + "?";
  }

  protected String getWarningForFL() {
    return "Is the audio consistent with \"" + foreignLang.getText() + "\" ?";
  }

  private boolean englishChanged() {
    return !english.box.getText().equals(originalEnglish);
  }

  private boolean foreignChanged() {
    boolean b = !foreignLang.box.getText().equals(originalForeign);
    if (b)
      System.out.println("foreignChanged : foreign " + foreignLang.box.getText() + " != original " + originalForeign);

    return b;
  }

  private boolean translitChanged() {
    return !newUserExercise.getTransliteration().equals(originalTransliteration); }

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
   * @see #postChangeIfDirty(mitll.langtest.client.list.ListInterface, boolean)
   * @see #audioPosted()
   * @param pagingContainer
   * @param buttonClicked
   */
  void reallyChange(final ListInterface pagingContainer, final boolean buttonClicked) {
    newUserExercise.setCreator(controller.getUser());
    postEditItem(pagingContainer, buttonClicked);
  }

  /**
   * @see #reallyChange(mitll.langtest.client.list.ListInterface, boolean)
   * @param pagingContainer
   * @param buttonClicked
   */
  private void postEditItem(final ListInterface pagingContainer, final boolean buttonClicked) {
    System.out.println("postEditItem : edit item " + buttonClicked);

    grabInfoFromFormAndStuffInfoExercise();

    service.editItem(newUserExercise, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(Void newExercise) {
        originalForeign = newUserExercise.getForeignLanguage();
        originalEnglish = newUserExercise.getEnglish();
        originalTransliteration = newUserExercise.getTransliteration();
        originalRefAudio = newUserExercise.getRefAudio();
        originalSlowRefAudio = newUserExercise.getSlowAudioRef();
       // System.out.println("postEditItem : onSuccess " + newUserExercise.getTooltip());

        doAfterEditComplete(pagingContainer, buttonClicked);
      }
    });
  }

  /**
   * Tell predefined list to update itself... since maybe a pre def item changed...
   *
   * @see #reallyChange(mitll.langtest.client.list.ListInterface, boolean)
   * @param pagingContainer
   * @param buttonClicked
   */
  void doAfterEditComplete(ListInterface pagingContainer, boolean buttonClicked) {
    System.out.println("doAfterEditComplete : change tooltip " + buttonClicked + " id " + predefinedContentList.getCurrentExerciseID());

    changeTooltip(pagingContainer);
    predefinedContentList.reloadWith(predefinedContentList.getCurrentExerciseID());
  }

  private void changeTooltip(ListInterface pagingContainer) {
    CommonShell byID = pagingContainer.byID(newUserExercise.getID());
    if (byID == null) {
      System.err.println("changeTooltip : huh? can't find exercise with id " + newUserExercise.getID());
    } else {
      byID.setTooltip(newUserExercise.getCombinedTooltip());
      System.out.println("changeTooltip : for " + newUserExercise.getID() + " now " + byID.getTooltip());

      pagingContainer.redraw();   // show change to tooltip!
    }
  }

  private String originalRefAudio;
  private String originalSlowRefAudio;
  private String originalTransliteration;

  /**
   * @see EditItem#populatePanel
   * @param newUserExercise
   */
  @Override
  public void setFields(CommonExercise newUserExercise) {
    //System.out.println("grabInfoFromFormAndStuffInfoExercise : setting fields with " + newUserExercise);

    // english
    english.box.setText(originalEnglish = newUserExercise.getEnglish());
    ((TextBox) english.box).setVisibleLength(newUserExercise.getEnglish().length() + 4);
    if (newUserExercise.getEnglish().length() > 20) {
      english.box.setWidth("500px");
    }
    useAnnotation(newUserExercise, "english", englishAnno);

    // foreign lang
    String foreignLanguage = newUserExercise.getForeignLanguage();
    foreignLanguage = foreignLanguage.trim();
    foreignLang.box.setText(originalForeign = foreignLanguage);
    useAnnotation(newUserExercise, "foreignLanguage", foreignAnno);

    // translit
    translit.box.setText(originalTransliteration = newUserExercise.getTransliteration());
    useAnnotation(newUserExercise, "transliteration", translitAnno);

    if (rap != null) {
      // regular speed audio
      rap.getPostAudioButton().setExercise(newUserExercise);
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
      rapSlow.getPostAudioButton().setExercise(newUserExercise);
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

  private void useAnnotation(CommonExercise userExercise, String field, HTML annoField) {
    useAnnotation(userExercise.getAnnotation(field), annoField);
  }

  private void useAnnotation(ExerciseAnnotation anno, HTML annoField) {
    boolean isIncorrect = anno != null && !anno.isCorrect();
    if (isIncorrect) {
      if (anno.comment.isEmpty()) {
        annoField.setHTML("<i>Empty Comment</i>");
      }
      else {
        annoField.setHTML("<i>\"" + anno.comment+ "\"</i>");
      }
    }
    annoField.setVisible(isIncorrect);
  }
}
