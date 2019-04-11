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

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.dialog.IListenView;
import mitll.langtest.client.dialog.ListenViewHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.*;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.scoring.AlignmentOutput;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by go22670 on 3/23/17.
 */
public class DialogExercisePanel<T extends ClientExercise> extends DivWidget
    implements AudioChangeListener, RefAudioGetter, IPlayAudioControl {
  private final Logger logger = Logger.getLogger("DialogExercisePanel");

  private static final String FLOAT_LEFT = "floatLeft";
  private static final String LANGUAGE = "LANGUAGE";
  private static final int WORD_SPACER = 7;

  private static final Set<String> TO_IGNORE = new HashSet<>(Arrays.asList("sil", "SIL", "<s>", "</s>"));
  public static final String BLUE = "#2196F3";

  private static final char FULL_WIDTH_ZERO = '\uFF10';
  private static final char FULL_WIDTH_NINE = '\uFF10' + 9;

  final T exercise;
  final ExerciseController controller;
  DivWidget flClickableRow;
  ClickableWords clickableWords;
  /**
   * @see #showAlignment
   */
  DivWidget flClickableRowPhones;

  /**
   * @see #getFLEntry
   */
  List<IHighlightSegment> flclickables = null;

  /**
   * @see #makePlayAudio
   */
  HeadlessPlayAudio playAudio;

  static final boolean DEBUG = false;
  static final boolean DEBUG_SHOW_ALIGNMENT = true;
  private static final boolean DEBUG_PLAY_PAUSE = false;
  private static final boolean DEBUG_DETAIL = false;
  private static final boolean DEBUG_MATCH = false;

  final boolean isRTL;

  final AlignmentFetcher alignmentFetcher;

  /**
   * Mandarin has special rules for the moment so we can match simplified chinese characters to traditional ones...
   */
  private final boolean isMandarin;
  final IListenView listenView;

  /**
   * Has a left side -- the question content (Instructions and audio panel (play button, waveform)) <br></br>
   * and a right side --
   *
   * @param commonExercise for this exercise
   * @param controller
   * @param alignments
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel
   * @see mitll.langtest.client.custom.content.NPFHelper#getFactory
   * @see mitll.langtest.client.custom.dialog.EditItem#setFactory
   * @see ListenViewHelper#getTurnPanel
   */
  DialogExercisePanel(final T commonExercise,
                      final ExerciseController controller,
                      final ListInterface<?, ?> listContainer,
                      Map<Integer, AlignmentOutput> alignments,
                      IListenView listenView) {
    this.exercise = commonExercise;
    this.controller = controller;
    this.listenView = listenView;
    ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();

    isMandarin = projectStartupInfo != null && projectStartupInfo.getLanguageInfo() == Language.MANDARIN;
    this.isRTL = !isEngAttr() && projectStartupInfo != null && projectStartupInfo.getLanguageInfo().isRTL();
    //  logger.info("isRTL " + isRTL);

    this.alignmentFetcher = new AlignmentFetcher(exercise.getID(),
        controller, listContainer,
        alignments, this, new AudioChangeListener() {
      @Override
      public void audioChanged(int id, long duration) {
        contextAudioChanged(id, duration);
      }

      @Override
      public void audioChangedWithAlignment(int id, long duration) {
      }
    });
    Style style = getElement().getStyle();
    style.setCursor(Style.Cursor.POINTER);
    getElement().setId("DialogExercisePanel_" + getExID());
  }

  public int getExID() {
    return exercise.getID();
  }

  @Override
  public void addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices) {
    ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();

    if (projectStartupInfo != null) {
      makeClickableWords(projectStartupInfo, null);

      {
        DivWidget wrapper = new DivWidget();
        wrapper.add(getFLEntry(exercise));
        wrapper.add(flClickableRowPhones = clickableWords.getClickableDiv(isRTL));
        stylePhoneRow(flClickableRowPhones);

        styleMe(wrapper);
        add(wrapper);
      }

      makePlayAudio(exercise, null);
    }
  }

  ProjectStartupInfo getProjectStartupInfo() {
    return controller == null ? null : controller.getProjectStartupInfo();
  }

  protected void stylePhoneRow(UIObject phoneRow) {
    if (isRTL) phoneRow.addStyleName("floatRight");
    phoneRow.addStyleName("topFiveMargin");
  }

  private boolean isEngAttr() {
    List<ExerciseAttribute> language =
        exercise.getAttributes()
            .stream()
            .filter(exerciseAttribute -> exerciseAttribute.getProperty().equalsIgnoreCase(LANGUAGE)).collect(Collectors.toList());
    return !language.isEmpty() && language.get(0).getValue().equalsIgnoreCase(Language.ENGLISH.toString());
  }

  void styleMe(DivWidget widget) {
    addMarginStyle();
  }

  void addMarginStyle() {
    Style style2 = getFlClickableRow().getElement().getStyle();
    addMarginLeft(style2);
    style2.setMarginRight(10, Style.Unit.PX);
    style2.setMarginTop(7, Style.Unit.PX);

    style2.setMarginBottom(0, Style.Unit.PX);
  }

  void addMarginLeft(Style style2) {
    style2.setMarginLeft(15, Style.Unit.PX);
  }

  /**
   * @param e
   * @param flContainer ignored here
   * @see #addWidgets
   * @see TwoColumnExercisePanel#makeFirstRow
   */
  void makePlayAudio(T e, DivWidget flContainer) {
    //if (hasAudio(e)) {
    playAudio = new HeadlessPlayAudio(controller.getSoundManager(), listenView);
    alignmentFetcher.setPlayAudio(playAudio);
    rememberAudio(getRegularSpeedIfAvailable(e));
//    } else {
//      logger.warning("makePlayAudio no audio in audio attributes " + e.getAudioAttributes() + " for exercise " + e.getID());
//    }
  }

  void rememberAudio(AudioAttribute next) {
    //  logger.info("rememberAudio audio for " + this + "  " + next);
    playAudio.rememberAudio(next);
    maybeShowAlignment(next);
  }

  /**
   * @param next
   */
  private void maybeShowAlignment(AudioAttribute next) {
//    AlignmentOutput alignmentOutput = alignmentFetcher.getAlignment(next.getUniqueID());
//    if (next != null && next.getAlignmentOutput() != null) {
//      //   logger.info("maybeShowAlignment audio for " + this + "  " + next);
//      showAlignment(next.getUniqueID(), next.getDurationInMillis(), next.getAlignmentOutput());
//    }
    if (next != null) {
      AlignmentOutput alignmentOutput = alignmentFetcher.getAlignment(next.getUniqueID());
      if (alignmentOutput != null) {
        //   logger.info("maybeShowAlignment audio for " + this + "  " + next);
        showAlignment(next.getUniqueID(), next.getDurationInMillis(), alignmentOutput);
      }
    }
  }

  AudioAttribute getRegularSpeedIfAvailable(T e) {
    AudioAttribute candidate = e.getRegularSpeed();
    return candidate == null ? e.getFirst() : candidate;
  }

  void makeClickableWords(ProjectStartupInfo projectStartupInfo, ListInterface listContainer) {
    Language languageInfo = isEngAttr() ? Language.ENGLISH : projectStartupInfo.getLanguageInfo();
    //  boolean addFloatLeft = shouldAddFloatLeft();
    // logger.info("makeClickableWords " + exercise.getID() + " " + exercise.getFLToShow() + " add float left " + addFloatLeft);

    clickableWords = new ClickableWords(listContainer, exercise.getID(),
        languageInfo, languageInfo.getFontSize(), BLUE, shouldAddFloatLeft());
  }

  protected boolean shouldAddFloatLeft() {
    return !isRTL;
  }

  /**
   * @param listener
   * @see mitll.langtest.client.list.FacetExerciseList#getRefAudio
   */
  @Override
  public void getRefAudio(RefAudioListener listener) {
    alignmentFetcher.getRefAudio(listener);
  }

  @Override
  public Set<Integer> getReqAudioIDs() {
    return alignmentFetcher.getReqAudioIDs();
  }

  Set<Integer> getReqAudio() {
    return alignmentFetcher.getAllReqAudioIDs();
  }

  @Override
  public void getAndRememberCachedAlignents(RefAudioListener listener, Set<Integer> req) {
    alignmentFetcher.getAndRememberCachedAlignents(listener, req);
  }

  /**
   * @param req
   * @see mitll.langtest.client.list.FacetExerciseList#makeExercisePanels
   */
  @Override
  public void setReq(int req) {
    alignmentFetcher.setReq(req);
  }

  @Override
  public int getReq() {
    return alignmentFetcher.getReq();
  }

  /**
   * @param id
   * @param duration
   * @see TwoColumnExercisePanel#makeFirstRow
   */
  @Override
  public void audioChangedWithAlignment(int id, long duration) {
    AlignmentOutput alignmentOutputFromAudio = alignmentFetcher.getAlignment(id);

    if (alignmentOutputFromAudio != null) {
      alignmentFetcher.rememberAlignment(id, alignmentOutputFromAudio);
    } else {
      logger.warning("audioChangedWithAlignment : no alignment info for audio #" + id);
    }
    if (DEBUG) logger.info("audioChangedWithAlignment " + id + " : " + alignmentOutputFromAudio);
    audioChanged(id, duration);
  }

  /**
   * @param id
   * @param duration
   * @see ChoicePlayAudioPanel#addChoices
   */
  @Override
  public void audioChanged(int id, long duration) {
    showAlignment(id, duration, alignmentFetcher.getAlignment(id));
  }

  int currentAudioDisplayed = -1;

  /**
   * TODO : don't do this twice!
   *
   * @param id
   * @param duration
   * @param alignmentOutput
   * @see #makePlayAudio
   * @see #audioChanged
   * @see RecordDialogExercisePanel#showScoreInfo
   * @see RecordDialogExercisePanel#showAlignment(int, long, AlignmentOutput)
   */
  TreeMap<TranscriptSegment, IHighlightSegment> showAlignment(int id, long duration, AlignmentOutput alignmentOutput) {
    if (alignmentOutput != null) {
      if (currentAudioDisplayed != id) {
        currentAudioDisplayed = id;
        if (DEBUG_SHOW_ALIGNMENT) {
          logger.info("showAlignment for ex " + exercise.getID() + " audio id " + id + " : " + alignmentOutput);
        }

        return matchSegmentsToClickables(id, duration, alignmentOutput, flclickables, this.playAudio, flClickableRow, flClickableRowPhones);
      } else {
        if (DEBUG_SHOW_ALIGNMENT) {
          logger.info("showAlignment SKIP for ex " + exercise.getID() + " audio id " + id + " vs " + currentAudioDisplayed);
        }
        return null;
      }
    } else {
      if (DEBUG_SHOW_ALIGNMENT)
        logger.warning("showAlignment no alignment info for " + this + " id " + id + " dur " + duration);
      return null;
    }
  }

  /**
   * @param id
   * @param duration
   * @param alignmentOutput
   * @param flclickables
   * @param playAudio
   * @param clickableRow
   * @param clickablePhones
   * @see #showAlignment
   * @see #contextAudioChanged
   */
  TreeMap<TranscriptSegment, IHighlightSegment> matchSegmentsToClickables(int id,
                                                                          long duration,
                                                                          AlignmentOutput alignmentOutput,
                                                                          List<IHighlightSegment> flclickables,
                                                                          HeadlessPlayAudio playAudio,
                                                                          DivWidget clickableRow,
                                                                          DivWidget clickablePhones) {
    if (DEBUG) logger.info("matchSegmentsToClickables match seg to clicable " + id + " : " + alignmentOutput);

    Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget =
        matchSegmentToWidgetForAudio(id, duration, alignmentOutput, flclickables, playAudio, clickableRow, clickablePhones);
    setPlayListener(id, duration, typeToSegmentToWidget, playAudio);

    return typeToSegmentToWidget.get(NetPronImageType.WORD_TRANSCRIPT);
  }

  private void setPlayListener(int id,
                               long duration,
                               Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget,
                               HeadlessPlayAudio playAudio) {
    if (DEBUG) {
      logger.info("setPlayListener for ex " + exercise.getID() +
          " audio id " + id + " : " +
          (typeToSegmentToWidget == null ? "missing" : typeToSegmentToWidget.size()));
    }

    if (typeToSegmentToWidget == null) {
      logger.warning("setPlayListener no type to segment for " + id + " and exercise " + exercise.getID());
    } else {
      if (DEBUG) {
        logger.info("setPlayListener segments now for ex " + exercise.getID() +
            " audio " + id + " dur " + duration +
            "\n\twords: " + typeToSegmentToWidget.get(NetPronImageType.WORD_TRANSCRIPT).keySet() +
            "\n\tphone: " + typeToSegmentToWidget.get(NetPronImageType.PHONE_TRANSCRIPT).keySet()
        );
      }

      playAudio.setListener(new SegmentHighlightAudioControl(typeToSegmentToWidget, getExID()));
    }
  }

  /**
   * @param audioID
   * @param durationInMillis
   * @param alignmentOutput
   * @param flclickables
   * @param audioControl
   * @param clickableRow
   * @param clickablePhones
   * @return
   * @see #matchSegmentsToClickables
   */
  private Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> matchSegmentToWidgetForAudio(Integer audioID,
                                                                                                            long durationInMillis,
                                                                                                            AlignmentOutput alignmentOutput,
                                                                                                            List<IHighlightSegment> flclickables,
                                                                                                            AudioControl audioControl,
                                                                                                            DivWidget clickableRow,
                                                                                                            DivWidget clickablePhones) {
    Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToTranscriptToHighlight = new HashMap<>();

    TreeMap<TranscriptSegment, IHighlightSegment> segmentToWord = new TreeMap<>();
    typeToTranscriptToHighlight.put(NetPronImageType.WORD_TRANSCRIPT, segmentToWord);

    if (alignmentOutput == null) {
      logger.warning("matchSegmentToWidgetForAudio no alignment for " + audioID);
      segmentToWord.put(new TranscriptSegment(0, (float) durationInMillis, "all", 0, "all"),
          getAllHighlight(flclickables));
    } else {
      if (DEBUG_MATCH)
        logger.info("matchSegmentToWidgetForAudio " + audioID + " got clickables " + flclickables.size());

      List<TranscriptSegment> wordSegments = getWordSegments(alignmentOutput);

      if (DEBUG_MATCH) {
        wordSegments.forEach(transcriptSegment -> logger.info("matchSegmentToWidgetForAudio : (" + wordSegments.size() + ") ex " + getExID() + " " + transcriptSegment));
      }

      if (wordSegments == null) {
        if (DEBUG_MATCH) logger.info("matchSegmentToWidgetForAudio no word segments in " + alignmentOutput);
      } else {
        TreeMap<TranscriptSegment, IHighlightSegment> phoneMap = new TreeMap<>();
        typeToTranscriptToHighlight.put(NetPronImageType.PHONE_TRANSCRIPT, phoneMap);
        ListIterator<IHighlightSegment> highlightSegments = flclickables.listIterator();

        List<TranscriptSegment> phones = alignmentOutput.getTypeToSegments().get(NetPronImageType.PHONE_TRANSCRIPT);

//        logger.info("phones " +phones.size());
//        phones.forEach(p->logger.info(p.toString()));

        int c = getNumClickable(flclickables);
        boolean sameNumSegments = c == wordSegments.size();
        boolean simpleLayout = !alignmentOutput.isShowPhoneScores();

        if (sameNumSegments) {
          doOneToOneMatch(phones, audioControl, phoneMap, segmentToWord, highlightSegments, wordSegments, clickablePhones, simpleLayout);
        } else {
          if (DEBUG_MATCH) logger.warning("matchSegmentToWidgetForAudio no match for" +
              "\n\tsegments " + wordSegments +
              "\n\tto       " + flclickables);

          clickableRow.clear();

          doOneToManyMatch(phones, audioControl, phoneMap, segmentToWord, highlightSegments,
              wordSegments, clickableRow, clickablePhones, simpleLayout);
        }
      }
    }
    if (DEBUG_MATCH)
      logger.info("matchSegmentToWidgetForAudio typeToTranscriptToHighlight is " + typeToTranscriptToHighlight);
    return typeToTranscriptToHighlight;
  }

  @NotNull
  protected AllHighlight getAllHighlight(Collection<IHighlightSegment> flclickables) {
    return new AllHighlight(flclickables, shouldAddFloatLeft());
  }

  /**
   * @param phones
   * @param audioControl
   * @param phoneMap
   * @param segmentToWord
   * @param clickablesIterator
   * @param wordSegments
   * @param clickableRow
   * @param clickablePhones
   * @param simpleLayout
   * @see #matchSegmentToWidgetForAudio
   */
  private void doOneToManyMatch(List<TranscriptSegment> phones,
                                AudioControl audioControl,
                                TreeMap<TranscriptSegment, IHighlightSegment> phoneMap,
                                TreeMap<TranscriptSegment, IHighlightSegment> segmentToWord,
                                ListIterator<IHighlightSegment> clickablesIterator,
                                List<TranscriptSegment> wordSegments,
                                DivWidget clickableRow,
                                DivWidget clickablePhones,
                                boolean simpleLayout) {
    clickablePhones.clear();
    ListIterator<TranscriptSegment> transcriptSegmentListIterator = wordSegments.listIterator();
    while (transcriptSegmentListIterator.hasNext()) {
      TranscriptSegment wordSegment = transcriptSegmentListIterator.next();

      if (clickablesIterator.hasNext()) {
        List<TranscriptSegment> phonesInWord = getSegsWithinWordTimeWindow(phones, wordSegment);

        if (DEBUG_MATCH)
          logger.info("doOneToManyMatch got segment " + wordSegment);// + " length " + segmentLength);

        List<IHighlightSegment> unclickable = new ArrayList<>();
        IHighlightSegment highlightSegment =
            matchEventSegmentToClickable(clickablesIterator, wordSegment, phonesInWord, audioControl, phoneMap,
                clickablePhones, unclickable, simpleLayout);

        if (highlightSegment == null) {
          if (DEBUG_MATCH) logger.info("doOneToManyMatch can't find match for wordSegment " + wordSegment);

          // so here we have something where we need more segments for this clickable...?
          if (clickablesIterator.hasPrevious()) {  // so now we want to put segment phones underneath clickable
            IHighlightSegment current = clickablesIterator.previous();

            String lcSegment = getLCSegment(wordSegment);
            String lcClickable = getLCClickable(current);

            List<TranscriptSegment> phonesInWordAll = new ArrayList<>();
            List<TranscriptSegment> wordSegmentsForClickable = new ArrayList<>();

            while (!lcClickable.isEmpty()) {
              boolean isFragmentInSegment = lcClickable.startsWith(lcSegment) || (
                  isMandarin && lcClickable.length() <= lcSegment.length());
              if (isFragmentInSegment) {
                if (DEBUG_MATCH) {
                  logger.info("doOneToManyMatch OK, match for word segment " + lcSegment + " inside " + lcClickable);
                }
                lcClickable = lcClickable.substring(lcSegment.length());
                phonesInWordAll.addAll(phonesInWord);

                wordSegmentsForClickable.add(wordSegment);
                //  logger.info("\t doOneToManyMatch now clickable segment " + lcClickable + " after removing " + lcSegment + " now " + phonesInWordAll.size() + " phones");

                if (!lcClickable.isEmpty()) {
                  if (transcriptSegmentListIterator.hasNext()) {
                    wordSegment = transcriptSegmentListIterator.next();
                    if (DEBUG_MATCH) {
                      logger.info("doOneToManyMatch word segment now " + wordSegment);
                    }
                    lcSegment = getLCSegment(wordSegment);
                    phonesInWord = getSegsWithinWordTimeWindow(phones, wordSegment);
                  }
                }
              } else {
                if (DEBUG_MATCH) {
                  logger.warning("doOneToManyMatch no match for align word '" + lcSegment +
                      "'  vs '" + lcClickable +
                      "'");
                }
                break;
              }
            }

            if (phonesInWordAll.isEmpty()) {
              if (DEBUG_MATCH) logger.warning("doOneToManyMatch no matches for " + current + " and " + lcSegment);
            } else {
              //    logger.info("doOneToManyMatch got matches for " + current + " and " + lcSegment + " lcClickable " + lcClickable);
              TranscriptSegment combinedTranscriptSegment = getCombinedTranscriptSegment(wordSegmentsForClickable);

              if (shouldShowPhones()) {
                if (DEBUG_MATCH) {
                  logger.info("doOneToManyMatch phones in word " + wordSegment);
                  for (TranscriptSegment transcriptSegment : wordSegmentsForClickable) {
                    logger.info("\tdoOneToManyMatch found shorter transcript segment matching " + transcriptSegment + " clickable " + current);
                  }
                }
//                String event = wordSegment.getEvent();
//                phonesInWordAll.forEach(ph -> logger.info(event + " " + ph.toString()));
                showPhones(combinedTranscriptSegment, phonesInWordAll, audioControl, phoneMap, clickablePhones, simpleLayout);
              }

              if (DEBUG_MATCH) {
                logger.info("doOneToManyMatch map " + wordSegment + " to clickable " + current);
              }
              if (wordSegmentsForClickable.size() > 1) {
                if (DEBUG_MATCH) {
                  logger.info("doOneToManyMatch combined is " + combinedTranscriptSegment + " to clickable " + current);
                }
              }

              segmentToWord.put(combinedTranscriptSegment, current); // only one for now...
              clickableRow.add(current.asWidget());

              // add spacer - also required if we want to select text and copy it somewhere.
              //    clickableRow.add(new InlineHTML(" "));

              addFloatLeft(current);

              clickablesIterator.next(); // OK, we've done this clickable
            }

          } else {
            if (DEBUG_MATCH) logger.warning("doOneToManyMatch no match for " + wordSegment);
          }
        } else {
          if (DEBUG_MATCH) logger.warning("doOneToManyMatch 2 : no match for " + wordSegment);
          boolean hasSpace = false;
          for (IHighlightSegment iHighlightSegment : unclickable) {
            if (!iHighlightSegment.getContent().trim().isEmpty()) {
              addFloatLeft(iHighlightSegment);
              clickableRow.add(iHighlightSegment.asWidget());
            } else hasSpace = true;
          }

          addFloatLeft(highlightSegment);
          if (hasSpace) {
            addSpacerStyle(highlightSegment);
          }

          segmentToWord.put(wordSegment, highlightSegment);
          clickableRow.add(highlightSegment.asWidget());
        }
      }
    }

    if (DEBUG_MATCH) {
      if (clickablesIterator.hasNext()) logger.info("matchSegmentToWidgetForAudio tokens left over ");
    }

    while (clickablesIterator.hasNext()) {
      IHighlightSegment next = clickablesIterator.next();
      if (DEBUG_MATCH) logger.info("matchSegmentToWidgetForAudio adding left over " + next);
      {
        Widget w = next.asWidget();
        addFloatLeft(w);
        clickableRow.add(w);
      }
    }
  }


  @NotNull
  private TranscriptSegment getCombinedTranscriptSegment(List<TranscriptSegment> wordSegmentsForClickable) {
    if (wordSegmentsForClickable.size() == 1) {
      return wordSegmentsForClickable.get(0);
    } else {
      StringBuilder builder = new StringBuilder();
      wordSegmentsForClickable.forEach(ws -> builder.append(ws.getEvent()));
      String combinedEvent = builder.toString();
      float totalScore = 0F;
      for (TranscriptSegment ws : wordSegmentsForClickable) {
        builder.append(ws.getEvent());
        totalScore += ws.getScore();
      }
      return new TranscriptSegment(
          wordSegmentsForClickable.get(0).getStart(),
          wordSegmentsForClickable.get(wordSegmentsForClickable.size() - 1).getEnd(),
          combinedEvent,
          totalScore / wordSegmentsForClickable.size(),
          combinedEvent
      );
    }
  }

  private void addSpacerStyle(IHighlightSegment highlightSegment) {
    Style style = highlightSegment.asWidget().getElement().getStyle();
    if (isRTL) {
      style.setMarginRight(WORD_SPACER, Style.Unit.PX);
    } else {
      style.setMarginLeft(WORD_SPACER, Style.Unit.PX);
    }
    if (DEBUG_MATCH) logger.info("Add space to " + highlightSegment.getContent());
  }

  private String getLCSegment(TranscriptSegment wordSegment) {
    return removePunct(wordSegment.getEvent().toLowerCase());
  }

  private String getLCClickable(IHighlightSegment current) {
    return removePunct(current.getContent().toLowerCase());
  }

  private void addFloatLeft(IHighlightSegment current) {
    addFloatLeft(current.asWidget());
  }

  protected void addFloatLeft(Widget w) {
    if (!isRTL) {
      //    logger.info("addFloatLeft to (" + isRTL + ") elem '" + w.getElement().getId() + "'");
      w.addStyleName(FLOAT_LEFT);
    }
  }

  /**
   * TODO : could be faster
   *
   * @param phones
   * @param audioControl
   * @param phoneMap
   * @param segmentToWord            remember map of transcript seg to highlight segment
   * @param highlightSegmentIterator
   * @param wordSegments
   * @param clickablePhones
   * @param simpleLayout
   * @see #matchSegmentToWidgetForAudio(Integer, long, AlignmentOutput, List, AudioControl, DivWidget, DivWidget)
   */
  private void doOneToOneMatch(List<TranscriptSegment> phones,
                               AudioControl audioControl,
                               TreeMap<TranscriptSegment, IHighlightSegment> phoneMap,
                               TreeMap<TranscriptSegment, IHighlightSegment> segmentToWord,
                               Iterator<IHighlightSegment> highlightSegmentIterator,
                               List<TranscriptSegment> wordSegments,
                               DivWidget clickablePhones,

                               boolean simpleLayout) {
    clickablePhones.clear();
    for (TranscriptSegment wordSegment : wordSegments) {
      if (highlightSegmentIterator.hasNext()) {
        List<TranscriptSegment> phonesInWord = getSegsWithinWordTimeWindow(phones, wordSegment);

        if (DEBUG_MATCH)
          logger.info("doOneToOneMatch got segment " + wordSegment);// + " length " + segmentLength);

        IHighlightSegment highlightSegment =
            matchEventSegmentToClickable(highlightSegmentIterator, wordSegment, phonesInWord, audioControl,
                phoneMap, clickablePhones, new ArrayList<>(), simpleLayout);

        if (highlightSegment == null) {
          if (DEBUG_MATCH) logger.warning("doOneToOneMatch can't find match for wordSegment " + wordSegment);
        } else {
          segmentToWord.put(wordSegment, highlightSegment);
        }
      } else {
        if (DEBUG_MATCH) logger.warning("doOneToOneMatch no match for " + wordSegment);
      }
    }
  }

  private List<TranscriptSegment> getWordSegments(AlignmentOutput alignmentOutput) {
    List<TranscriptSegment> wordSegments = alignmentOutput.getTypeToSegments().get(NetPronImageType.WORD_TRANSCRIPT);
    if (wordSegments == null) {
      wordSegments = Collections.emptyList();
    } else {
      wordSegments = wordSegments.stream().filter(seg -> !shouldIgnore(seg)).collect(Collectors.toList());
    }
    return wordSegments;
  }

  /**
   * could be faster w/ treeset
   *
   * @param phones
   * @param word
   * @return
   */
  private List<TranscriptSegment> getSegsWithinWordTimeWindow(List<TranscriptSegment> phones, TranscriptSegment word) {
    List<TranscriptSegment> phonesInWord = new ArrayList<>();
    float start = word.getStart();
    float end = word.getEnd();

    for (TranscriptSegment phone : phones) {
      if (phone.getStart() >= start && phone.getEnd() <= end) {
        phonesInWord.add(phone);
      }

      if (phone.getStart() > end) {
        break;
      }
    }

    return phonesInWord;
  }

  /**
   * @param clickables
   * @param wordSegment
   * @param phonesInWord
   * @param audioControl
   * @param phoneMap
   * @param clickablePhones
   * @param simpleLayout
   * @return
   * @see #doOneToManyMatch(List, AudioControl, TreeMap, TreeMap, ListIterator, List, DivWidget, DivWidget, boolean)
   */
  private IHighlightSegment matchEventSegmentToClickable(Iterator<IHighlightSegment> clickables,
                                                         TranscriptSegment wordSegment,
                                                         List<TranscriptSegment> phonesInWord,
                                                         AudioControl audioControl,
                                                         TreeMap<TranscriptSegment, IHighlightSegment> phoneMap,
                                                         DivWidget clickablePhones,
                                                         List<IHighlightSegment> unclickable,
                                                         boolean simpleLayout) {
    IHighlightSegment clickable = clickables.next();
    clickable = skipUnclickable(clickables, clickable, unclickable);

    if (DEBUG_DETAIL || DEBUG_MATCH) {
      logger.info("matchEventSegmentToClickable compare :" +
          "\n\tsegment      " + wordSegment.getEvent() + //" length " + segmentLength +
          "\n\tvs clickable " + clickable);
    }

    String lcSegment = getLCSegment(wordSegment);
    String lcClickable = getLCClickable(clickable);

    if (DEBUG_DETAIL || DEBUG_MATCH)
      logger.info("matchEventSegmentToClickable compare :" +
          "\n\tlc segment   " + lcSegment + //" length " + segmentLength +
          "\n\tvs lcClickable '" + lcClickable + "'");

    boolean showPhones = shouldShowPhones();

    if (lcSegment.equalsIgnoreCase(lcClickable)) {  // easy match -
      if (showPhones) {
        showPhones(wordSegment, phonesInWord, audioControl, phoneMap, clickablePhones, simpleLayout);
      }

      return clickable;
    } else {
      Collection<IHighlightSegment> bulk = getMatchingSegments(clickables, clickable, lcSegment, unclickable);

      if (bulk.isEmpty()) {
        return null;
      } else { // all clickables match this segment
        AllHighlight allHighlight = getAllHighlight(bulk);
        if (showPhones) {
          showPhones(wordSegment, phonesInWord, audioControl, phoneMap, clickablePhones, simpleLayout);
        }

        if (DEBUG || DEBUG_MATCH)
          logger.info("matchEventSegmentToClickable create composite from " + bulk.size() + " = " + allHighlight);

        return allHighlight;
      }
    }
  }

  private void showPhones(TranscriptSegment wordSegment,
                          List<TranscriptSegment> phonesInWord,
                          AudioControl audioControl,
                          TreeMap<TranscriptSegment, IHighlightSegment> phoneMap,
                          DivWidget clickablePhones,
                          boolean simpleLayout) {
    addSouthClickable(clickablePhones, getPhoneDivBelowWord(wordSegment, phonesInWord, audioControl, phoneMap, simpleLayout));
  }

  private void addSouthClickable(DivWidget clickablePhones, DivWidget phoneDivBelowWord) {
    clickablePhones.add(phoneDivBelowWord);
    phoneDivBelowWord.addStyleName(isRTL ? "leftFiveMargin" : "rightFiveMargin");
  }

  /**
   * @param wordSegment
   * @param phonesInWord
   * @param audioControl
   * @param phoneMap
   * @param simpleLayout
   * @return
   * @see #doOneToManyMatch
   */
  @NotNull
  protected DivWidget getPhoneDivBelowWord(TranscriptSegment wordSegment,
                                           List<TranscriptSegment> phonesInWord,
                                           AudioControl audioControl,
                                           TreeMap<TranscriptSegment, IHighlightSegment> phoneMap, boolean simpleLayout) {
    return new WordTable().getPhoneDivBelowWord(audioControl, phoneMap, phonesInWord, simpleLayout, wordSegment, true);
  }

  /**
   * Assumes transcript word segment contains the clickable segment on the screen.
   * If the other way round...?
   *
   * @param clickables
   * @param clickable
   * @param lcSegment
   * @return
   * @see #matchEventSegmentToClickable
   */
  @NotNull
  private Collection<IHighlightSegment> getMatchingSegments(Iterator<IHighlightSegment> clickables,
                                                            IHighlightSegment clickable,
                                                            String lcSegment,
                                                            List<IHighlightSegment> unclickable) {
    Collection<IHighlightSegment> bulk = new ArrayList<>();

    if (DEBUG_MATCH) logger.info("\tgetMatchingSegments (2) compare :" +
        "\n\tsegment " + lcSegment +
        "\n\tvs      " + clickable);

    while (!lcSegment.isEmpty()) {
      String lcClickable = getLCClickable(clickable);
/*
      if (lcClickable.isEmpty()) {
        logger.info("BEFORE '" + lcClickable +
            "' after '" + lcClickable +
            "'");
      }
      */
      if (DEBUG_MATCH) logger.info("\tgetMatchingSegments (2) compare :" +
          "\n\tsegment   '" + lcSegment + "' " + lcSegment.length() +
          "\n\tclickable '" + lcClickable + "' " + lcClickable.length());

      boolean segmentHasFragment = lcSegment.startsWith(lcClickable) || (isMandarin && lcClickable.length() <= lcSegment.length());
      if (segmentHasFragment) {
        bulk.add(clickable);
        lcSegment = lcSegment.substring(lcClickable.length());   // chop off clickable part from word segment
        // if (DEBUG_MATCH) logger.info("\tgetMatchingSegments segment now '" + lcSegment + "'");

        if (!clickables.hasNext() || lcSegment.isEmpty()) {
          break;
        }
        clickable = clickables.next();
        // logger.info("clickable now        " + clickable);
        clickable = skipUnclickable(clickables, clickable, unclickable);
        // logger.info("after skip clickable " + clickable);
      } else {
        if (DEBUG_MATCH) {
          logger.info("\tgetMatchingSegments (2) NOPE compare :" +
              "\n\tsegment     '" + lcSegment + "'" +
              "\n\tvs lcClickable '" + lcClickable + "'");
        }
        break;
      }
    }

    if (!lcSegment.isEmpty()) {
      if (DEBUG_MATCH) {
        logger.warning("getMatchingSegments couldn't match all of segment - this left over = " + lcSegment);
      }
    }
    return bulk;
  }

/*
  private boolean transcriptMatchesOneToOne(List<IHighlightSegment> clickables, List<TranscriptSegment> segments) {

    int c = getNumClickable(clickables);
    boolean b = c == segments.size();

    if (DEBUG_MATCH && !b) {
      dumpMatchComparison(clickables, segments, c);
    }

    return b;
  }
*/

  private void dumpMatchComparison(List<IHighlightSegment> clickables, List<TranscriptSegment> segments, int c) {
    logger.info("transcriptMatchesOneToOne  clickables " + c + " segments " + segments.size());
    StringBuilder builder = new StringBuilder();
    for (IHighlightSegment clickable : clickables) {
      if (clickable.isClickable()) {
        builder.append(clickable.getContent()).append(" ");
      }
    }
    logger.info("transcriptMatchesOneToOne clickable : " + builder);

    StringBuilder builder2 = new StringBuilder();
    for (TranscriptSegment segment : segments) {
      builder2.append(segment.getEvent()).append(" ");
    }
    logger.info("transcriptMatchesOneToOne align    : " + builder2);
  }

  private int getNumClickable(List<IHighlightSegment> clickables) {
    int c = 0;
    StringBuilder sb = new StringBuilder();
    for (IHighlightSegment clickable : clickables) {
      if (clickable.isClickable()) {
        c++;
        sb.append(clickable).append(" ");
      }
    }
    //logger.info("found " + c + " clickables " + sb);
    return c;
  }

  /**
   * Remove arabic full stop
   * Remove arabic comma
   * Remove arabic question mark...
   * exclamation point
   * chinese fill with comma
   * ideographic comma
   * right double quote
   * double quote
   * <p>
   * 2d = dash like in twenty-first
   *
   * left quote and right quote : LEFT SINGLE QUOTATION MARK and RIGHT SINGLE QUOTATION MARK
   *
   * @param t
   * @return
   * @see #doOneToManyMatch
   */
  private String removePunct(String t) {
    return fromFull(t
        .replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, "")
        .replaceAll("['%\\u06D4\\u060C\\u0022\\uFF01-\\uFF0F\\uFF1A-\\uFF1F\\u3001\\u3002\\u003F\\u00A1\\u00BF\\u002E\\u002C\\u002D\\u0021\\u2026\\u005C\\u2013\\u061F\\uFF0C\\u201D\\u2018\\u2019]", ""));
  }

  /**
   * Go from full width numbers to normal width
   *
   * @param s
   * @return
   */
  private String fromFull(String s) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c >= FULL_WIDTH_ZERO && c <= FULL_WIDTH_NINE) {
        int offset = c - FULL_WIDTH_ZERO;
        int full = '0' + offset;
        builder.append(Character.valueOf((char) full).toString());
      } else {
        builder.append(c);
      }
    }
    String s1 = builder.toString();
//    if (!s.isEmpty() && !s.equalsIgnoreCase(s1)) {
//      logger.warning("fromFull before '" +
//          s +
//          "' after '" + s1 +
//          "'");
//    }
    return s1;
  }

  @NotNull
  private IHighlightSegment skipUnclickable(Iterator<IHighlightSegment> iterator, IHighlightSegment clickable, List<IHighlightSegment> unclickable) {
    while (!clickable.isClickable() && iterator.hasNext()) {
      // logger.info("skipUnclickable : skip " + clickable);
      unclickable.add(clickable);
      clickable = iterator.next();
    }
    return clickable;
  }

  private boolean shouldIgnore(TranscriptSegment seg) {
    return TO_IGNORE.contains(seg.getEvent());
  }

  /**
   * @param e
   * @return
   * @see #addWidgets(boolean, boolean, PhonesChoices)
   * @see TwoColumnExercisePanel#makeFirstRow
   */
  @NotNull
  DivWidget getFLEntry(T e) {
    flclickables = new ArrayList<>();

    List<String> tokens = e.getTokens();
//    if (false) {
//      if (tokens == null && !e.hasEnglishAttr()) {
//        logger.info("getFLEntry : no tokens for " + e.getID() + " " + e.getEnglish() + " " + e.getForeignLanguage());
//      }
//    }

    flClickableRow = clickableWords.getClickableWords(getFL(e), FieldType.FL, flclickables, isRTL, tokens);
    return flClickableRow;
  }

  private String getFL(CommonShell e) {
    return e.getFLToShow();
  }

  /**
   * @param e
   * @return
   * @see #makePlayAudio(ClientExercise, DivWidget)
   */
  boolean hasAudio(T e) {
    return e.isContext() ? e.hasContextAudio() : e.hasAudioNonContext(true);
  }

  void contextAudioChanged(int id, long duration) {
    logger.info("contextAudioChanged : audio changed for " + id + " - " + duration);
    audioChanged(id, duration);
  }

  DivWidget getFlClickableRow() {
    return flClickableRow;
  }

  boolean shouldShowPhones() {
    return false;
  }

  /**
   * @return
   * @see ListenViewHelper#playCurrentTurn
   */
  @Override
  public boolean doPlayPauseToggle() {
    if (playAudio != null) {
      if (DEBUG_PLAY_PAUSE) logger.info("doPlayPauseToggle on " + getExID());
//
//      String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("doing play for " +getExID()));
//      logger.info("logException stack " + exceptionAsString);

      return playAudio.doPlayPauseToggle();
    } else {
      logger.warning("doPlayPauseToggle no play audio???");
      return false;
    }
  }

  /**
   * @param playListener
   * @see ListenViewHelper#getTurnPanel
   */
  public void addPlayListener(PlayListener playListener) {
    if (playAudio != null) {
      playAudio.addPlayListener(playListener);
    }
  }

  @Override
  public boolean doPause() {
    return playAudio.doPause();
  }

  public boolean isPlaying() {
    return playAudio.isPlaying();
  }

  public void resetAudio() {
    playAudio.reinitialize();
  }

  @Override
  protected void onUnload() {
    if (playAudio != null) {
      playAudio.destroySound();
    }
  }

  /**
   * @see ListenViewHelper#clearHighlightAndRemoveMark
   */
  public void clearHighlight() {
    flclickables.forEach(IHighlightSegment::clearHighlight);
  }

  public String toString() {
    return "turn ex #" + exercise.getID() + " '" + exercise.getForeignLanguage() + "'";
  }
}
