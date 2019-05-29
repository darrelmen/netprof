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

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.SessionManager;
import mitll.langtest.client.custom.exercise.CommentBox;
import mitll.langtest.client.dialog.IListenView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.scoring.AlignmentOutput;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

import static com.google.gwt.dom.client.Style.Unit.PX;
import static mitll.langtest.client.qc.QCNPFExercise.ENGLISH;
import static mitll.langtest.client.qc.QCNPFExercise.FOREIGN_LANGUAGE;
import static mitll.langtest.client.scoring.PhonesChoices.SHOW;

/**
 * Created by go22670 on 3/23/17.
 */
public class TwoColumnExercisePanel<T extends ClientExercise> extends DialogExercisePanel<T> {
  private Logger logger = Logger.getLogger("TwoColumnExercisePanel");

  /**
   * @see #getRowWidget
   */
  private static final String SCORING_ROW_STYLE = "scoringRowStyle";

  private static final String FLOAT_LEFT = "floatLeft";
  private static final String N_A = "N/A";

  /**
   * @see #getItemContent(ClientExercise)
   */
  private static final String LEFT_WIDTH = "60%";
  private static final int LEFT_WIDTH_NO_ENGLISH_VALUE = 82;
  /**
   * @see #getItemContent
   */
  private static final String LEFT_WIDTH_NO_ENGLISH = LEFT_WIDTH_NO_ENGLISH_VALUE + "%";

  /**
   *
   */
  private static final String RIGHT_WIDTH = "40%";
  /**
   * @see #getUnitChapterAndDropdown(ClientExercise, String, boolean)
   */
  private static final String RIGHT_WIDTH_NO_ENGLISH = (100 - LEFT_WIDTH_NO_ENGLISH_VALUE) + "%";

  static final int CONTEXT_INDENT = 45;//50;

  private final CommentAnnotator annotationHelper;
  /**
   * @see RefAudioGetter#addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
  private static final boolean showInitially = false;
  private UnitChapterItemHelper<ClientExercise> commonExerciseUnitChapterItemHelper;
  private final ListInterface<?, ?> listContainer;
  private ChoicePlayAudioPanel<ClientExercise> contextPlay;
  private List<IHighlightSegment> altflClickables = null;
  private List<IHighlightSegment> contextClickables;//, altContextClickables;

  private DivWidget altFLClickableRow;
  private DivWidget contextClickableRow;

  /**
   * @see #makeFirstRow
   */
  private DivWidget altFLClickableRowPhones;
  /**
   * @see #contextAudioChanged
   * @see #getContext
   */
  private DivWidget contextClickableRowPhones;

  private boolean showFL;
  private boolean showALTFL;
  /**
   * @see #shouldShowPhones
   */
  private PhonesChoices phonesChoices;
  private EnglishDisplayChoices englishDisplayChoices;


  private final ItemMenu itemMenu;
  private final boolean addPlayer;

  private final SessionManager sessionManager;

  private boolean showingEnglish = true;
  private boolean showingEnglishContext = true;

  private Dropdown dropdownContainer;

  private static final boolean DEBUG = false;

  /**
   * Has a left side -- the question content (Instructions and audio panel (play button, waveform)) <br></br>
   * and a right side --
   *
   * @param commonExercise for this exercise
   * @param controller
   * @param listContainer
   * @param alignments
   * @param addPlayer
   * @param listenView
   * @param sessionManager
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel
   * @see mitll.langtest.client.banner.LearnHelper#getFactory
   * @see mitll.langtest.client.custom.content.NPFHelper#getFactory
   * @see mitll.langtest.client.custom.dialog.EditItem#setFactory
   */
  public TwoColumnExercisePanel(final T commonExercise,
                                final ExerciseController<T> controller,
                                final ListInterface<?, ?> listContainer,
                                Map<Integer, AlignmentOutput> alignments,
                                boolean addPlayer,
                                IListenView listenView,
                                SessionManager sessionManager) {
    super(commonExercise, controller, listContainer, alignments, listenView);

    this.sessionManager = sessionManager;
    this.listContainer = listContainer;
    this.addPlayer = addPlayer;
    //  logger.info("Adding isContext " + isContext + " ex " + commonExercise.getID() + " " + commonExercise.getEnglish() + " " + commonExercise.getForeignLanguage());

    addStyleName("twoColumnStyle");
    annotationHelper = controller.getCommentAnnotator();
    this.itemMenu = new ItemMenu(controller, commonExercise);
  }

  /**
   * @param showFL
   * @param showALTFL
   * @param phonesChoices
   * @param englishDisplayChoices
   * @see mitll.langtest.client.list.FacetExerciseList#makeExercisePanels
   * @return
   */
  @Override
  public DivWidget addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices, EnglishDisplayChoices englishDisplayChoices) {
    this.showFL = showFL;
    this.showALTFL = showALTFL;

    this.phonesChoices = phonesChoices;
    this.englishDisplayChoices = englishDisplayChoices;

    ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();

    DivWidget widgets = null;
    if (projectStartupInfo != null) {
      makeClickableWords(projectStartupInfo, listContainer);
      commonExerciseUnitChapterItemHelper = new UnitChapterItemHelper<>(controller.getTypeOrder());
      add(widgets=getItemContent(exercise));
    } else {
      //  logger.warning("addWidgets no project startup info?");
      clickableWords = null;
      commonExerciseUnitChapterItemHelper = null;
    }
    return widgets;
  }

  /**
   * TODO : don't do this twice!
   *
   * @param id
   * @param duration
   * @param alignmentOutput
   */
  protected TreeMap<TranscriptSegment, IHighlightSegment> showAlignment(int id, long duration, AlignmentOutput alignmentOutput) {
    if (alignmentOutput != null) {
      if (currentAudioDisplayed != id) {
        currentAudioDisplayed = id;

        if (DEBUG_SHOW_ALIGNMENT) {
          logger.info("showAlignment for ex " + exercise.getID() + " audio id " + id + " : " + alignmentOutput);
        }

        List<IHighlightSegment> flclickables = this.flclickables == null ? altflClickables : this.flclickables;
        DivWidget flClickableRow = this.getFlClickableRow() == null ? altFLClickableRow : this.getFlClickableRow();
        DivWidget flClickablePhoneRow = this.flClickableRowPhones == null ? altFLClickableRowPhones : this.flClickableRowPhones;
        return matchSegmentsToClickables(id, duration, alignmentOutput, flclickables, this.getPlayAudio(), flClickableRow, flClickablePhoneRow);
      } else {
        return null;
      }
    } else {
      if (DEBUG_SHOW_ALIGNMENT) {
        logger.info("showAlignment no alignment info for ex " + exercise.getID() + " " + id + " dur " + duration);
      }
      return null;
    }
  }

  private SimpleRecordAudioPanel recordPanel;
  private Widget meaningWidget, flEntry, firstRow;

  /**
   * Row 1: FL - ENGLISH
   * Row 2: AltFL
   * Row 3: Transliteration
   * Row 4: Meaning
   * Row 5: context sentence fl - eng
   *
   * @return
   * @see RefAudioGetter#addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#getQuestionContent
   */
  private DivWidget getItemContent(final T e) {
    long then = System.currentTimeMillis();

    DivWidget card = new DivWidget();
    card.setWidth("100%");

    //  logger.info("For "  +e.getID() + " meaning " + e.getMeaning() + " " + e.getEnglish() + " " + english);
    SimpleRecordAudioPanel<T> recordPanel =
        new SimpleRecordAudioPanel<>(controller, e, listContainer, addPlayer, listenView, sessionManager);
    this.recordPanel = recordPanel;
    // add the first row
    {
      DivWidget rowWidget = getRowWidget();
      rowWidget.getElement().setId("firstRow");
      card.add(rowWidget);

      String english = getEnglishToShow(e);
      boolean hasEnglish = isValid(english);
      {
        DivWidget flContainer = makeFirstRow(e, recordPanel);
//        flContainer.addStyleName("floatLeft");

        flContainer.setWidth(hasEnglish ? LEFT_WIDTH : LEFT_WIDTH_NO_ENGLISH);
        this.firstRow = flContainer;
//        DivWidget w = new DivWidget();
//        w.getElement().setId("middle");
//        flContainer.add(w);
        rowWidget.add(flContainer);
      }

      //long now = System.currentTimeMillis();
      //  logger.info("getItemContent for " + e.getID() + " took " + (now - then) + " to add first row");

      rowWidget.add(getUnitChapterAndDropdown(e, english, hasEnglish));
    }

    // second row shows score and history for item
    card.add(getScoringRow(recordPanel));

    // finally, third row, has context
    if (e.hasContext()) {
      addContext(e, card);
    }

    if (DEBUG) {
      long now = System.currentTimeMillis();
      logger.info("getItemContent for " + e.getID() + " took " + (now - then));
    }
    return card;
  }

  private String getEnglishToShow(T e) {
    return isEnglish() && isMeaningValid(e) ? e.getMeaning() : e.getEnglish();
  }

  @NotNull
  private DivWidget getScoringRow(Widget recordPanel) {
    DivWidget rowWidget = getRowWidget();
    rowWidget.getElement().setId("scoringRow");
    rowWidget.add(recordPanel);
    return rowWidget;
  }


  /**
   * @param e
   * @param english
   * @param hasEnglish
   * @return
   * @see #getItemContent
   */
  @NotNull
  private DivWidget getUnitChapterAndDropdown(T e, String english, boolean hasEnglish) {
    DivWidget lr = getHorizDiv();
    addFloatLeft(lr);
    lr.setWidth(hasEnglish ? RIGHT_WIDTH : RIGHT_WIDTH_NO_ENGLISH);
//    lr.addStyleName("floatRight");

    if (hasEnglish) {
      lr.getElement().getStyle().setProperty("minWidth", "345px");

      DivWidget englishContainer = new DivWidget();
      englishContainer.setWidth("100%");
//      englishContainer.getElement().getStyle().setProperty("minWidth", "345px");

      boolean showEng = englishDisplayChoices == EnglishDisplayChoices.SHOW;
      String toUse = showEng ? english : replaceWithDashes(english);

      showingEnglish = showEng;
      englishContainer.add(meaningWidget = getEnglishWidget(e, toUse));
      lr.add(englishContainer);

      if (!showEng) {
        addMouseOverHandler(englishContainer, mouseOverEvent -> {
          if (!showingEnglish) {
            showingEnglish = true;
//            logger.info("mouse over...");
            englishContainer.clear();
            englishContainer.add(getEnglishWidget(e, english));
          }
        });
        addMouseOutHandler(englishContainer, mouseOutEvent -> {
          if (showingEnglish) {
            showingEnglish = false;
            //          logger.info("mouse out...");
            englishContainer.clear();
            englishContainer.add(getEnglishWidget(e, toUse));
          }
        });
      }
    }

    lr.add(getItemWidget(e));

    {
      DivWidget dropC = new DivWidget();
      dropC.add(dropdownContainer = itemMenu.getDropdown());
      lr.add(dropC);
    }
    return lr;
  }

  private boolean isEnglish() {
    return controller.getLanguageInfo() == Language.ENGLISH;
  }

  /**
   * Cheesy hack to make it just show the fl term in OOVViewHelper
   *
   * @see mitll.langtest.client.banner.OOVViewHelper
   */
  public void hideRecordButton() {
    recordPanel.setVisible(false);
    recordPanel.getPostAudioRecordButton().setVisible(false);
    dropdownContainer.setVisible(false);
    flEntry.setWidth("90%");
    if (meaningWidget != null) {
      meaningWidget.setVisible(false);
      //  logger.info("hide " + meaningWidget.getElement().getId());
      firstRow.setWidth("90%");
      meaningWidget.getParent().getParent().setWidth("1px");
      meaningWidget.getParent().getParent().getElement().getStyle().clearProperty("minWidth");
    }
    //  logger.info("hide " + recordPanel.getElement().getId());
  }

  /**
   * Left to right, in the first row, we have the record button, the play audio widget, and the fl text
   *
   * @param e
   * @return
   * @paramx rowWidget
   * @see #getItemContent
   */
  @NotNull
  private DivWidget makeFirstRow(T e, SimpleRecordAudioPanel<T> recordPanel) {
    DivWidget flContainer = getHorizDiv();

    { // #1 : add record button
      flContainer.add(getRecordButtonContainer(recordPanel));

      recordPanel.getPostAudioRecordButton().setEnabled(exercise.isSafeToDecode());

      //long now = System.currentTimeMillis();
      //  logger.info("makeFirstRow for " + e.getID() + " took " + (now - then) + " to add rec");

      // #2 : add play audio button
      makePlayAudio(e, flContainer);
    }

    {
      DivWidget fieldContainer = new DivWidget();
      fieldContainer.setWidth("100%");
      fieldContainer.getElement().setId("leftSideFieldContainer");

      String trimmedAltFL = getAltFL(e).trim();

//    long now = System.currentTimeMillis();
      // logger.info("makeFirstRow for " + e.getID() + " took " + (now - then) + " to add rec and play");

      // add FL row
      if (showFL || getFL(e).trim().equals(trimmedAltFL) || trimmedAltFL.isEmpty()) {
        fieldContainer.add(flEntry = getFLEntry(e));
        fieldContainer.add(flClickableRowPhones = clickableWords.getClickableDiv(isRTL));
        flClickableRowPhones.getElement().setId("flClickableRowPhones");
        stylePhoneRow(flClickableRowPhones);

        if (getPlayAudio() != null && getPlayAudio().getCurrentAudioAttr() != null) {
          AudioAttribute currentAudioAttr = getPlayAudio().getCurrentAudioAttr();

          if (DEBUG) logger.info("audioChangedWithAlignment audio " + currentAudioAttr.getUniqueID());

          //   AlignmentOutput alignmentOutput;
          audioChangedWithAlignment(
              currentAudioAttr.getUniqueID(),
              currentAudioAttr.getDurationInMillis()
          );
        }
      }

//    now = System.currentTimeMillis();
      // logger.info("makeFirstRow for " + e.getID() + " took " + (now - then) + " to fl row");

      // add alt fl
      if (showALTFL) {
        addField(fieldContainer, addAltFL(e, showFL));
        altFLClickableRowPhones = clickableWords.getClickableDiv(isRTL);
        altFLClickableRowPhones.getElement().setId("altFLClickableRowPhones");
        stylePhoneRow(altFLClickableRowPhones);

        addField(fieldContainer, altFLClickableRowPhones);
      }

      // add translit
      addField(fieldContainer, addTransliteration(e));

      // add meaning
      {
        boolean meaningValid = isMeaningValid(e);
        boolean useMeaningInsteadOfEnglish = meaningValid && isEnglish();

        if (!useMeaningInsteadOfEnglish && meaningValid) {
          Widget meaningWidget =
              getEntry(e,
                  QCNPFExercise.MEANING,
                  e.getMeaning(),
                  FieldType.MEANING,
                  showInitially,
                  new ArrayList<>(), true, annotationHelper, false);
          addField(fieldContainer, meaningWidget);
          this.meaningWidget = meaningWidget;
        }
      }

      flContainer.add(fieldContainer);
    }

    return flContainer;
  }

  @NotNull
  private DivWidget getRecordButtonContainer(SimpleRecordAudioPanel<?> recordPanel) {
    DivWidget recordButtonContainer = new DivWidget();
    recordButtonContainer.addStyleName("recordingRowStyle");
    PostAudioRecordButton postAudioRecordButton = recordPanel.getPostAudioRecordButton();
    recordButtonContainer.add(postAudioRecordButton);
    return recordButtonContainer;
  }

  protected void makePlayAudio(T e, DivWidget flContainer) {
    flContainer.add(setPlayAudio(getPlayAudioPanel()));
    alignmentFetcher.setPlayAudio(getPlayAudio());
  }

  protected void stylePhoneRow(UIObject phoneRow) {
    if (isRTL) {
      phoneRow.addStyleName("floatRight");
      phoneRow.getElement().getStyle().setMarginTop(7, PX);
    }
  }

  /**
   * @param e
   * @return
   * @see #makeFirstRow
   */
  @NotNull
  protected DivWidget getFLEntry(T e) {
    flclickables = new ArrayList<>();

    DivWidget contentWidget = clickableWords.getClickableWords(getFL(e),
        FieldType.FL,
        flclickables, isRTL, e.getTokens());

    flClickableRow = contentWidget;
    if (isRTL) flClickableRow.addStyleName("rightTenMargin");

    DivWidget flEntry = getCommentEntry(FOREIGN_LANGUAGE,
        e.getAnnotation(FOREIGN_LANGUAGE),
        false,
        showInitially,
        annotationHelper, isRTL, contentWidget, e.getID());

    if (isRTL) {
      clickableWords.setDirectionToRTL(flEntry);
    } else {
      addFloatLeft(flEntry);
    }
    flEntry.setWidth("100%");
    return flEntry;
  }

  private String getFL(CommonShell e) {
    return e.getFLToShow();
  }

  private String getAltFL(ClientExercise exercise) {
    return exercise.getAltFLToShow();
  }

  /**
   * @param e
   * @param card
   * @see #getItemContent
   */
  private void addContext(T e, Panel card) {
    String foreignLanguage = getFL(e);
    String altFL = getAltFL(e);
    Collection<ClientExercise> directlyRelated = e.getDirectlyRelated();
    for (ClientExercise contextEx : directlyRelated) {
      DivWidget rowWidget = getRowWidget();
      card.add(rowWidget);

      // logger.info("Add context " +contextEx.getID() + " " + contextEx);
      SimpleRecordAudioPanel<ClientExercise> recordPanel = addContextFields(rowWidget, foreignLanguage, altFL, contextEx);
      if (recordPanel != null) {
        card.add(getScoringRow(recordPanel));
      }
      //else {
      // logger.warning("can't add record panel?");
      //}
    }
  }

  /**
   * Left - right row - on left we have the context sentence, on right, the translation
   *
   * @param rowWidget
   * @param foreignLanguage
   * @param altFL
   * @param contextEx
   * @see #addContext
   */
  private SimpleRecordAudioPanel<ClientExercise> addContextFields(DivWidget rowWidget,
                                                                  String foreignLanguage,
                                                                  String altFL,
                                                                  ClientExercise contextEx) {
    AnnotationHelper annotationHelper = new AnnotationHelper(controller, controller.getMessageHelper());
    SimpleRecordAudioPanel<ClientExercise> recordPanel =
        new SimpleRecordAudioPanel<>(controller, contextEx, listContainer, addPlayer, listenView, sessionManager);
    Panel context = getContext(contextEx, foreignLanguage, altFL, annotationHelper, getRecordButtonContainer(recordPanel),
        exercise.getTokens());

    recordPanel.getPostAudioRecordButton().setEnabled(contextEx.isSafeToDecode());

    if (context != null) {
      rowWidget.add(context);
      context.setWidth("100%");
    }

    {
      String contextTranslation = contextEx.getEnglish();

      boolean same = getFL(contextEx).equals(contextTranslation);
      if (!same) {
        if (context != null && !contextTranslation.isEmpty()) {
          context.setWidth(LEFT_WIDTH);
        }

        boolean showEng = englishDisplayChoices == EnglishDisplayChoices.SHOW;
        String toUse = showEng ? contextTranslation : replaceWithDashes(contextTranslation);

        Widget contextTransWidget = addContextTranslation(contextEx, toUse, annotationHelper);


        if (contextTransWidget != null) {


          DivWidget englishContainer = new DivWidget();

          contextTransWidget.addStyleName("rightsidecolor");
          contextTransWidget.addStyleName("leftFiveMargin");

          rowWidget.add(englishContainer);

          englishContainer.add(contextTransWidget);
          englishContainer.setWidth(RIGHT_WIDTH);

          showingEnglishContext = showEng;

          if (!showEng) {
            addMouseOverHandler(englishContainer, mouseOverEvent -> {
              if (!showingEnglishContext) {
                showingEnglishContext = true;

//            logger.info("mouse over...");
                englishContainer.clear();
                englishContainer.add(addContextTranslation(contextEx, contextTranslation, annotationHelper));
              }
            });
            addMouseOutHandler(englishContainer, mouseOutEvent -> {
              if (showingEnglishContext) {
                showingEnglishContext = false;

                //          logger.info("mouse out...");
                englishContainer.clear();
                englishContainer.add(addContextTranslation(contextEx, toUse, annotationHelper));
              }
            });
          }

          //rowWidget.add(contextTransWidget);
        }
      }
    }

    return (context != null) ? recordPanel : null;
  }

  private Widget getAltContext(String flToHighlight, String altFL, AnnotationHelper annotationHelper, int exid,
                               List<String> contextTokens, List<String> highlightTokens) {
    Panel contentWidget = clickableWords.getClickableWordsHighlight(altFL, flToHighlight,
        FieldType.FL, new ArrayList<>(), true, contextTokens, highlightTokens, controller.getLanguageInfo().isRTL());

    return getCommentBox(annotationHelper, exid)
        .getEntry(QCNPFExercise.ALTCONTEXT, contentWidget,
            exercise.getAnnotation(QCNPFExercise.ALTCONTEXT), showInitially, isRTL);
  }

  /**
   * @return
   * @see #makePlayAudio
   */
  @NotNull
  private ChoicePlayAudioPanel<T> getPlayAudioPanel() {
    return new ChoicePlayAudioPanel<T>(exercise, controller, exercise.isContext(), this);
  }

  /**
   * Has mouse over showing details about the item.
   *
   * @param e
   * @return
   */
  @NotNull
  private DivWidget getItemWidget(T e) {
    InlineLabel itemHeader = commonExerciseUnitChapterItemHelper.showPopup(e);
    itemHeader.addStyleName("floatRight");
    DivWidget itemContainer = new DivWidget();
    itemContainer.add(itemHeader);
    itemContainer.addStyleName("floatRight");
    return itemContainer;
  }


  @NotNull
  private Widget getEnglishWidget(T e, String english) {
//    if (replaceWithDashes) {
//      english = replaceWithDashes(english);
//      logger.info("now " + english);
//    }

    Widget englishWidget = getEntry(e, QCNPFExercise.ENGLISH, english,
        FieldType.EN,
        showInitially, new ArrayList<>(), true, annotationHelper, false);


    englishWidget.addStyleName("rightsidecolor");
    englishWidget.getElement().setId("englishWidget");
    englishWidget.addStyleName(FLOAT_LEFT);
    return englishWidget;
  }

  @NotNull
  private String replaceWithDashes(String english) {
    english = english.replaceAll("[^\\s]", "-");
    return english;
  }


  private HandlerRegistration addMouseOutHandler(Widget divWidget, MouseOutHandler handler) {
    return divWidget.addDomHandler(handler, MouseOutEvent.getType());
  }

  private HandlerRegistration addMouseOverHandler(Widget divWidget, MouseOverHandler handler) {
    return divWidget.addDomHandler(handler, MouseOverEvent.getType());
  }

  private void addField(Panel grid, Widget widget) {
    if (widget != null) {
      widget.addStyleName("topFiveMargin");
      grid.add(widget);
    }
  }

  @NotNull
  private DivWidget getHorizDiv() {
    DivWidget flContainer = new DivWidget();
    flContainer.addStyleName("inlineFlex");
    return flContainer;
  }

  @NotNull
  private DivWidget getRowWidget() {
    DivWidget flContainer = new DivWidget();
    flContainer.addStyleName(SCORING_ROW_STYLE);
    return flContainer;
  }

  /**
   * @param e
   * @param addTopMargin
   * @return
   * @see #makeFirstRow
   */
  private Widget addAltFL(T e, boolean addTopMargin) {
    String altFL = getAltFL(e).trim();
    if (!altFL.isEmpty() && !altFL.equals(N_A) && !getFL(e).trim().equals(altFL)) {
      altflClickables = new ArrayList<>();

      DivWidget contentWidget = clickableWords.getClickableWords(altFL, FieldType.FL, altflClickables, isRTL, null);

      altFLClickableRow = contentWidget;

      DivWidget flEntry = getCommentEntry(QCNPFExercise.ALTFL, e.getAnnotation(QCNPFExercise.ALTFL), false,
          showInitially, annotationHelper, isRTL, contentWidget, e.getID());

      if (addTopMargin) {
        contentWidget.getElement().getStyle().setMarginTop(5, PX);
      }
      if (isRTL) {
        clickableWords.setDirectionToRTL(flEntry);
      } else {
        addFloatLeft(flEntry);
      }
      flEntry.setWidth("100%");
      return flEntry;

    } else return null;
  }

  private void addFloatLeft(DivWidget flEntry) {
    flEntry.addStyleName(FLOAT_LEFT);
  }

  private Widget addTransliteration(T e) {
    String translitSentence = e.getTransliteration();
    if (!translitSentence.isEmpty() && !translitSentence.equals(N_A)) {
      return getEntry(e, QCNPFExercise.TRANSLITERATION, translitSentence, FieldType.TRANSLIT,
          showInitially, new ArrayList<>(), true, annotationHelper, false);
    }
    return null;
  }

  private boolean isMeaningValid(T e) {
    return isValid(e.getMeaning());
  }

  private boolean isValid(String meaning) {
    return meaning != null && !meaning.trim().isEmpty() && !meaning.equals(N_A);
  }

  /**
   * @param contextExercise
   * @param highlightTokens
   * @return
   * @see #addContextFields(DivWidget, String, String, ClientExercise)
   */
  private Panel getContext(ClientExercise contextExercise,
                           String itemText,
                           String altFL,
                           AnnotationHelper annotationHelper,
                           Widget recordWidget,
                           List<String> highlightTokens) {
    String context = getFL(contextExercise);

    if (!context.isEmpty()) {
      Panel hp = new DivWidget();
      {
        hp.addStyleName("inlineFlex");
        hp.getElement().setId("contentContainer");
        hp.add(recordWidget);
      }
      ChoicePlayAudioPanel contextPlay = getContextPlay(contextExercise);
      hp.add(contextPlay);

      DivWidget contentWidget = clickableWords.getClickableWordsHighlight(context, itemText,
          FieldType.FL, contextClickables = new ArrayList<>(), true,
          contextExercise.getTokens(),
          highlightTokens, controller.getLanguageInfo().isRTL());

      contextClickableRow = contentWidget;
      contextClickableRowPhones = clickableWords.getClickableDiv(isRTL);
      contextClickableRowPhones.getElement().setId("contextClickableRowPhones");
      stylePhoneRow(contextClickableRowPhones);

      Widget commentRow =
          getCommentBox(annotationHelper, contextExercise.getID())
              .getEntry(FOREIGN_LANGUAGE, contentWidget,
                  contextExercise.getAnnotation(FOREIGN_LANGUAGE), showInitially, isRTL);

      commentRow.setWidth(100 + "%");

      DivWidget col = new DivWidget();
      col.setWidth("100%");

      //   col.setWidth(CONTEXT_WIDTH + "%");
      hp.add(col);

      String altFL1 = getAltFL(contextExercise);
      if (showFL || altFL1.isEmpty()) {
        col.add(commentRow);
        col.add(contextClickableRowPhones);
      }

      if (showALTFL) {
        if (!altFL1.isEmpty() && !context.equals(altFL1)) {
          // TODO : what to do with alt context tokens
          Widget altContext = getAltContext(altFL, altFL1, annotationHelper, contextExercise.getID(),
              contextExercise.getTokens(),
              highlightTokens);
          if (showFL && showALTFL) altContext.addStyleName("topFiveMargin");
          col.add(altContext);
        }
      }

      if (contextPlay != null && contextPlay.getCurrentAudioAttr() != null) {
        AudioAttribute currentAudioAttr = contextPlay.getCurrentAudioAttr();

        int uniqueID = currentAudioAttr.getUniqueID();
        //AlignmentOutput alignmentOutput = alignmentFetcher.getAlignment(uniqueID);

        contextAudioChangedWithAlignment(uniqueID,
            currentAudioAttr.getDurationInMillis()//,
            //           alignmentFetcher.getAlignment(uniqueID)
        );
      }

      return hp;
    } else {
      //  logger.info("note that the context fl is empty");
      return null;
    }
  }

  /**
   * @param contextExercise
   * @return
   * @see #getContext
   */
  private ChoicePlayAudioPanel getContextPlay(ClientExercise contextExercise) {
    AudioChangeListener contextAudioChanged = new AudioChangeListener() {
      @Override
      public void audioChanged(int id, long duration) {
        if (DEBUG) {
          logger.info("getContextPlay audioChanged for ex " + exercise.getID() + "/" + contextExercise.getID() +
              " RECORD_CONTEXT audio id "
          );
        }
        contextAudioChanged(id, duration);
      }

      @Override
      public void audioChangedWithAlignment(int id, long duration) {
        if (DEBUG) {
          logger.info("getContextPlay audioChangedWithAlignment for ex " + exercise.getID() + "/" + contextExercise.getID() +
              " RECORD_CONTEXT audio id "
          );
        }
        contextAudioChangedWithAlignment(id, duration);//, alignmentOutputFromAudio);
      }
    };
    contextPlay = new ChoicePlayAudioPanel<>(contextExercise, controller, true, contextAudioChanged);
    AudioAttribute audioAttrPrefGender = contextExercise.getAudioAttrPrefGender(controller.getUserManager().isMale());
    contextPlay.setEnabled(audioAttrPrefGender != null);
    alignmentFetcher.setContextPlay(contextPlay);

    return contextPlay;
  }

  private void contextAudioChangedWithAlignment(int id, long duration) {//}, AlignmentOutput alignmentOutputFromAudio) {
    if (DEBUG) {
      logger.info("contextAudioChangedWithAlignment audioChanged for ex " + exercise.getID() + " RECORD_CONTEXT audio id " + id);// +
      //        " alignment " + alignmentOutputFromAudio);
    }

//
//    if (alignmentOutputFromAudio != null) {
//      alignmentFetcher.rememberAlignment(id, alignmentOutputFromAudio);
//    }
    contextAudioChanged(id, duration);
  }

  public void contextAudioChanged(int id, long duration) {
    if (DEBUG) {
      logger.info("contextAudioChanged  id " + id);
    }

    AlignmentOutput alignmentOutput = alignmentFetcher.getAlignment(id);
    if (alignmentOutput != null) {
      if (DEBUG) {
        logger.info("contextAudioChanged audioChanged for ex " + exercise.getID() + " RECORD_CONTEXT audio id " + id +
            " alignment " + alignmentOutput);
      }
      if (contextClickables == null) {
        logger.warning("contextAudioChanged : huh? context not set for " + id);
      } else {
        matchSegmentsToClickables(id, duration, alignmentOutput, contextClickables, contextPlay, contextClickableRow, contextClickableRowPhones);
      }
    }
    //else {
    //logger.warning("contextAudioChanged : no alignment output for " + id);
    //}
  }

  /**
   * @return
   * @see #doOneToManyMatch
   * @see #matchEventSegmentToClickable
   */
  @Override
  protected boolean shouldShowPhones() {
    return phonesChoices == SHOW;
  }

  /**
   * @param e
   * @param contextTranslation
   * @param annotationHelper
   * @return null if there isn't any context translation
   */
  private Widget addContextTranslation(AnnotationExercise e,
                                       String contextTranslation,
                                       AnnotationHelper annotationHelper) {
    if (!contextTranslation.isEmpty()) {
      return getEntry(e,
          ENGLISH,
          contextTranslation,
          FieldType.EN, showInitially, new ArrayList<>(), true,
          annotationHelper, false);
    } else {
      return null;
    }
  }

  /**
   * @param e
   * @param field
   * @param value
   * @param showInitially
   * @param clickables
   * @param addRightMargin
   * @param annotationHelper
   * @param isRTL
   * @return
   * @see #getFLEntry
   * @see #addAltFL
   * @see #addContextTranslation
   * @see #addTransliteration
   */
  private DivWidget getEntry(AnnotationExercise e,
                             final String field,
                             String value,
                             FieldType fieldType,
                             boolean showInitially,
                             List<IHighlightSegment> clickables,
                             boolean addRightMargin,
                             CommentAnnotator annotationHelper,
                             boolean isRTL) {
    return getEntry(field, value, e.getAnnotation(field), fieldType, showInitially, clickables, addRightMargin,
        annotationHelper, isRTL, e.getID());
  }

  /**
   * @param field
   * @param value
   * @param annotation
   * @param showInitially
   * @param clickables
   * @param addRightMargin
   * @param annotationHelper
   * @param isRTL
   * @param exid
   * @return
   * @paramx label
   * @see #getEntry
   */
  private DivWidget getEntry(final String field,
                             String value,
                             ExerciseAnnotation annotation,
                             FieldType fieldType,
                             boolean showInitially,
                             List<IHighlightSegment> clickables,
                             boolean addRightMargin,
                             CommentAnnotator annotationHelper,
                             boolean isRTL, int exid) {
    DivWidget contentWidget = clickableWords.getClickableWords(value, fieldType, clickables,
        isRTL, null);
    // logger.info("value " + value + " translit " + isTranslit + " is fl " + isFL);
    return getCommentEntry(field, annotation, fieldType == FieldType.TRANSLIT, showInitially,
        annotationHelper, isRTL, contentWidget, exid);
  }

  private DivWidget getCommentEntry(String field,
                                    ExerciseAnnotation annotation,
                                    boolean isTranslit,
                                    boolean showInitially,
                                    CommentAnnotator annotationHelper,
                                    boolean isRTL,
                                    DivWidget contentWidget,
                                    int exid) {
    if (isTranslit && isRTL) {
      // logger.info("- float right value " + value + " translit " + isTranslit + " is fl " + isFL);
      contentWidget.addStyleName("floatRight");
    }
    return
        getCommentBox(annotationHelper, exid)
            .getEntry(field, contentWidget, annotation, showInitially, isRTL);
  }

  /**
   * @return
   * @seex x#getEntry(String, String, String, ExerciseAnnotation)
   * @seex #makeFastAndSlowAudio(String)
   */
  private CommentBox getCommentBox(CommentAnnotator annotationHelper, int exid) {
    if (logger == null) {
      logger = Logger.getLogger("TwoColumnExercisePanel");
    }
    T exercise = this.exercise;
    CommentBox commentBox = new CommentBox(exid, controller,
        annotationHelper, exercise.getMutableAnnotation(), true);
    itemMenu.addCommentBox(commentBox);
    return commentBox;
  }
}
