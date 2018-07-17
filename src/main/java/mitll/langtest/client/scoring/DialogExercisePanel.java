package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.banner.IListenView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.*;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonShell;
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
public class DialogExercisePanel<T extends ClientExercise>
    extends DivWidget
    implements AudioChangeListener, RefAudioGetter, IPlayAudioControl {
  private Logger logger = Logger.getLogger("DialogExercisePanel");

  private static final Set<String> TO_IGNORE = new HashSet<>(Arrays.asList("sil", "SIL", "<s>", "</s>"));
  private static final String BLUE = "#2196F3";


  static final int CONTEXT_INDENT = 45;//50;
  private static final char FULL_WIDTH_ZERO = '\uFF10';
  private static final char FULL_WIDTH_NINE = '\uFF10' + 9;

  protected final T exercise;
  protected final ExerciseController controller;
  DivWidget flClickableRow;
  ClickableWords clickableWords;

  /**
   * @see #getFLEntry
   */
  List<IHighlightSegment> flclickables = null;

  /**
   * @see #makePlayAudio
   */
  protected HeadlessPlayAudio playAudio;


  private static final boolean DEBUG = false;
  private static final boolean DEBUG_DETAIL = false;
  private static final boolean DEBUG_MATCH = false;
  private boolean isRTL = false;

  AlignmentFetcher alignmentFetcher;

  /**
   * Mandarin has special rules for the moment so we can match simplified chinese characters to traditional ones...
   */
  private boolean isMandarin;
  private final IListenView listenView;

  /**
   * Has a left side -- the question content (Instructions and audio panel (play button, waveform)) <br></br>
   * and a right side --
   *
   * @param commonExercise for this exercise
   * @param controller
   * @param alignments
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel
   * @see mitll.langtest.client.banner.NewLearnHelper#getFactory
   * @see mitll.langtest.client.custom.content.NPFHelper#getFactory
   * @see mitll.langtest.client.custom.dialog.EditItem#setFactory
   * @see mitll.langtest.client.banner.ListenViewHelper#getTurnPanel
   */
  public DialogExercisePanel(final T commonExercise,
                             final ExerciseController controller,
                             final ListInterface<?, ?> listContainer,
                             Map<Integer, AlignmentOutput> alignments,
                             IListenView listenView) {
    this.exercise = commonExercise;
    this.controller = controller;
    this.listenView = listenView;
    isMandarin = getProjectStartupInfo() != null && getProjectStartupInfo().getLanguageInfo() == Language.MANDARIN;
    this.alignmentFetcher = new AlignmentFetcher(exercise.getID(),
        controller, listContainer,
        alignments, this, new AudioChangeListener() {
      @Override
      public void audioChanged(int id, long duration) {
        contextAudioChanged(id, duration);
      }

      @Override
      public void audioChangedWithAlignment(int id, long duration, AlignmentOutput alignmentOutputFromAudio) {
      }
    });
    getElement().getStyle().setCursor(Style.Cursor.POINTER);
  }

  public int getExID() {
    return exercise.getID();
  }

  @Override
  public void addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices) {
    ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();

    if (projectStartupInfo != null) {
      makeClickableWords(projectStartupInfo, null);
      this.isRTL = clickableWords.isRTL(exercise.getForeignLanguage());

      add(getFLEntry(exercise));
      makePlayAudio(exercise, null);
    }
  }

  protected void makePlayAudio(T e, DivWidget flContainer) {
    if (hasAudio(e)) {
      playAudio = new HeadlessPlayAudio(controller.getSoundManager(),listenView);
      alignmentFetcher.setPlayAudio(playAudio);
      if (!e.getAudioAttributes().isEmpty()) {
        AudioAttribute next = e.getAudioAttributes().iterator().next();
        playAudio.rememberAudio(next);
      //  logger.info("makePlayAudio audio for " + e.getID() + "  " + next);

        if (next.getAlignmentOutput() != null) {
          showAlignment(next.getUniqueID(), next.getDurationInMillis(), next.getAlignmentOutput());
        }
//        audioChanged(next.getUniqueID(), next.getDurationInMillis());
      } else {
        logger.warning("makePlayAudio no audio for " + e.getID());
      }
    } else {
      logger.warning("makePlayAudio no audio in " + e.getAudioAttributes());
    }
  }

  private int getVolume() {
    return listenView == null ? 100 : listenView.getVolume();
  }

  void makeClickableWords(ProjectStartupInfo projectStartupInfo, ListInterface listContainer) {
    clickableWords = new ClickableWords(listContainer, exercise.getID(), controller.getLanguage(), projectStartupInfo.getLanguageInfo().getFontSize(), BLUE);
  }

  /**
   * @param listener
   * @see mitll.langtest.client.list.FacetExerciseList#getRefAudio
   */
  @Override
  public void getRefAudio(RefAudioListener listener) {
    alignmentFetcher.getRefAudio(listener);
  }

  protected ProjectStartupInfo getProjectStartupInfo() {
    return controller == null ? null : controller.getProjectStartupInfo();
  }

  public Set<Integer> getReqAudio() {
    return alignmentFetcher.getReqAudio();
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
   * @param alignmentOutputFromAudio
   * @see TwoColumnExercisePanel#makeFirstRow
   */
  @Override
  public void audioChangedWithAlignment(int id, long duration, AlignmentOutput alignmentOutputFromAudio) {
    if (alignmentOutputFromAudio != null) {
      alignmentFetcher.rememberAlignment(id, alignmentOutputFromAudio);
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
   */
  protected void showAlignment(int id, long duration, AlignmentOutput alignmentOutput) {
    if (alignmentOutput != null) {
      if (currentAudioDisplayed != id) {
        currentAudioDisplayed = id;
        if (DEBUG)
          logger.info("showAlignment for ex " + exercise.getID() + " audio id " + id + " : " + alignmentOutput);
        //List<IHighlightSegment> flclickables = this.flclickables == null ? altflClickables : this.flclickables;
        //DivWidget flClickableRow = this.flClickableRow == null ? altFLClickableRow : this.flClickableRow;
        //DivWidget flClickablePhoneRow = this.flClickableRowPhones == null ? altFLClickableRowPhones : this.flClickableRowPhones;
        matchSegmentsToClickables(id, duration, alignmentOutput, flclickables, this.playAudio, flClickableRow, new DivWidget());
      }
    } else {
      if (DEBUG || true)
        logger.warning("showAlignment no alignment info for ex " + exercise.getID() + " " + id + " dur " + duration);
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
   * @see #audioChanged
   * @see #contextAudioChanged
   */
  protected void matchSegmentsToClickables(int id,
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
        TreeMap<TranscriptSegment, IHighlightSegment> transcriptSegmentIHighlightSegmentTreeMap =
            typeToSegmentToWidget.get(NetPronImageType.WORD_TRANSCRIPT);
        logger.info("setPlayListener segments now for ex " + exercise.getID() +
            " audio " + id + " dur " + duration +
            "\n\twords: " + transcriptSegmentIHighlightSegmentTreeMap.keySet() +
            "\n\tphone: " + typeToSegmentToWidget.get(NetPronImageType.PHONE_TRANSCRIPT).keySet()
        );

      }
      playAudio.setListener(new SegmentHighlightAudioControl(typeToSegmentToWidget));
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
    Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> value = new HashMap<>();

    TreeMap<TranscriptSegment, IHighlightSegment> segmentToWord = new TreeMap<>();
    value.put(NetPronImageType.WORD_TRANSCRIPT, segmentToWord);

    if (alignmentOutput == null) {
      logger.warning("matchSegmentToWidgetForAudio no alignment for " + audioID);
      segmentToWord.put(new TranscriptSegment(0, (float) durationInMillis, "all", 0, "all", 0),
          new AllHighlight(flclickables));
    } else {
      if (DEBUG_MATCH)
        logger.info("matchSegmentToWidgetForAudio " + audioID + " got clickables " + flclickables.size());

      List<TranscriptSegment> wordSegments = getWordSegments(alignmentOutput);
      if (wordSegments == null) {
        if (DEBUG_MATCH) logger.info("matchSegmentToWidgetForAudio no word segments in " + alignmentOutput);
      } else {
        TreeMap<TranscriptSegment, IHighlightSegment> phoneMap = new TreeMap<>();
        value.put(NetPronImageType.PHONE_TRANSCRIPT, phoneMap);
        ListIterator<IHighlightSegment> iterator = flclickables.listIterator();

        List<TranscriptSegment> phones = alignmentOutput.getTypeToSegments().get(NetPronImageType.PHONE_TRANSCRIPT);

        if (transcriptMatches(flclickables, wordSegments)) {
          doOneToOneMatch(phones, audioControl, phoneMap, segmentToWord, iterator, wordSegments, clickablePhones);
        } else {
          if (DEBUG_MATCH) logger.warning("matchSegmentToWidgetForAudio no match for" +
              "\n\tsegments " + wordSegments +
              "\n\tto       " + flclickables);

          clickableRow.clear();
          doOneToManyMatch(phones, audioControl, phoneMap, segmentToWord, iterator, wordSegments, clickableRow, clickablePhones);
        }
      }
    }
    if (DEBUG_MATCH) logger.info("matchSegmentToWidgetForAudio value is " + value);
    return value;
  }

  private void doOneToManyMatch(List<TranscriptSegment> phones,
                                AudioControl audioControl,
                                TreeMap<TranscriptSegment, IHighlightSegment> phoneMap,
                                TreeMap<TranscriptSegment, IHighlightSegment> segmentToWord,
                                ListIterator<IHighlightSegment> clickablesIterator,
                                List<TranscriptSegment> wordSegments,
                                DivWidget clickableRow,
                                DivWidget clickablePhones) {
    clickablePhones.clear();
    ListIterator<TranscriptSegment> transcriptSegmentListIterator = wordSegments.listIterator();
    while (transcriptSegmentListIterator.hasNext()) {
      TranscriptSegment wordSegment = transcriptSegmentListIterator.next();

      if (clickablesIterator.hasNext()) {
        List<TranscriptSegment> phonesInWord = getSegs(phones, wordSegment);

        if (DEBUG_MATCH)
          logger.info("doOneToManyMatch got segment " + wordSegment);// + " length " + segmentLength);

        IHighlightSegment value1 =
            matchEventSegmentToClickable(clickablesIterator, wordSegment, phonesInWord, audioControl, phoneMap, clickablePhones);

        if (value1 == null) {
          if (DEBUG_MATCH) logger.info("doOneToManyMatch can't find match for wordSegment " + wordSegment);

          // so here we have something where we need more segments for this clickable...?
          if (clickablesIterator.hasPrevious()) {  // so now we want to put segment phones underneath clickable
            IHighlightSegment current = clickablesIterator.previous();

            String lcSegment = removePunct(wordSegment.getEvent().toLowerCase());
            String fragment1 = removePunct(current.getContent().toLowerCase());

            List<TranscriptSegment> phonesInWordAll = new ArrayList<>();

            while (!fragment1.isEmpty()) {
              // boolean fragmentContainsSegment = fragment1.startsWith(lcSegment);
              boolean isFragmentInSegment = fragment1.startsWith(lcSegment) || (
                  isMandarin && fragment1.length() <= lcSegment.length()
              );
              if (isFragmentInSegment) {
                // logger.info("doOneToManyMatch OK, match for word segment " + lcSegment + " inside " + fragment1);

                fragment1 = fragment1.substring(lcSegment.length());
                phonesInWordAll.addAll(phonesInWord);

                //  logger.info("\t doOneToManyMatch now clickable segment " + fragment1 + " after removing " + lcSegment + " now " + phonesInWordAll.size() + " phones");

                if (!fragment1.isEmpty()) {
                  if (transcriptSegmentListIterator.hasNext()) {
                    wordSegment = transcriptSegmentListIterator.next();
                    lcSegment = removePunct(wordSegment.getEvent().toLowerCase());
                    phonesInWord = getSegs(phones, wordSegment);
                  }
                }
              } else {
                if (DEBUG_MATCH) logger.warning("doOneToManyMatch no match for align word '" + lcSegment +
                    "'  vs '" + fragment1 +
                    "'");
                break;
              }
            }

            if (phonesInWordAll.isEmpty()) {
              if (DEBUG) logger.warning("doOneToManyMatch no matches for " + current + " and " + lcSegment);
            } else {
              //    logger.info("doOneToManyMatch got matches for " + current + " and " + lcSegment + " fragment1 " + fragment1);

              if (shouldShowPhones()) {
                DivWidget phoneDivBelowWord = getPhoneDivBelowWord(wordSegment, phonesInWordAll, audioControl, phoneMap);
                addSouthClickable(clickablePhones, current, phoneDivBelowWord);
              }

              segmentToWord.put(wordSegment, current); // only one for now...
              clickableRow.add(current.asWidget());

              // add spacer - also required if we want to select text and copy it somewhere.
              //    clickableRow.add(new InlineHTML(" "));

              if (!isRTL) {
                addFloatLeft(current);
              }

              clickablesIterator.next(); // OK, we've done this clickable
            }

          } else {
            if (DEBUG_MATCH) logger.warning("doOneToManyMatch no match for " + wordSegment);
          }
        } else {
          // logger.warning("doOneToManyMatch no match for " + wordSegment);
          if (!isRTL) {
            addFloatLeft(value1);
          }
          segmentToWord.put(wordSegment, value1);
          clickableRow.add(value1.asWidget());
          //clickableRow.add(new InlineHTML(" "));
        }
      }
    }

    if (DEBUG_MATCH) {
      if (clickablesIterator.hasNext()) logger.info("matchSegmentToWidgetForAudio tokens left over ");
    }

    while (clickablesIterator.hasNext()) {
      IHighlightSegment next = clickablesIterator.next();
      if (DEBUG_MATCH) logger.info("matchSegmentToWidgetForAudio adding left over " + next);
      Widget w = next.asWidget();
      if (!isRTL) w.addStyleName("floatLeft");
      clickableRow.add(w);
//      InlineHTML w1 = new InlineHTML(" ");
//      if (!isRTL) w1.addStyleName("floatLeft");
//      clickableRow.add(w1);
    }
  }

  private void addFloatLeft(IHighlightSegment current) {
    current.asWidget().addStyleName("floatLeft");
  }

  /**
   * @param phones
   * @param audioControl
   * @param phoneMap
   * @param segmentToWord   remember map of transcript seg to highlight segment
   * @param iterator
   * @param wordSegments
   * @param clickablePhones
   */
  private void doOneToOneMatch(List<TranscriptSegment> phones,
                               AudioControl audioControl,
                               TreeMap<TranscriptSegment, IHighlightSegment> phoneMap,
                               TreeMap<TranscriptSegment, IHighlightSegment> segmentToWord,
                               Iterator<IHighlightSegment> iterator,
                               List<TranscriptSegment> wordSegments,
                               DivWidget clickablePhones) {
    clickablePhones.clear();
    for (TranscriptSegment wordSegment : wordSegments) {
      if (iterator.hasNext()) {
        List<TranscriptSegment> phonesInWord = getSegs(phones, wordSegment);

        if (DEBUG)
          logger.info("doOneToOneMatch got segment " + wordSegment);// + " length " + segmentLength);

        IHighlightSegment value1 =
            matchEventSegmentToClickable(iterator, wordSegment, phonesInWord, audioControl, phoneMap, clickablePhones);

        if (value1 == null) {
          if (DEBUG) logger.warning("doOneToOneMatch can't find match for wordSegment " + wordSegment);
        } else {
          segmentToWord.put(wordSegment, value1);
        }
      } else {
        if (DEBUG) logger.warning("doOneToOneMatch no match for " + wordSegment);
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

  private List<TranscriptSegment> getSegs(List<TranscriptSegment> phones, TranscriptSegment word) {
    List<TranscriptSegment> phonesInWord = new ArrayList<>();
    for (TranscriptSegment phone : phones) {
      if (phone.getStart() >= word.getStart() && phone.getEnd() <= word.getEnd()) {
        phonesInWord.add(phone);
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
   * @return
   * @see #doOneToManyMatch(List, AudioControl, TreeMap, TreeMap, ListIterator, List, DivWidget, DivWidget)
   */
  private IHighlightSegment matchEventSegmentToClickable(Iterator<IHighlightSegment> clickables,
                                                         TranscriptSegment wordSegment,
                                                         List<TranscriptSegment> phonesInWord,
                                                         AudioControl audioControl,
                                                         TreeMap<TranscriptSegment, IHighlightSegment> phoneMap,
                                                         DivWidget clickablePhones) {
    IHighlightSegment clickable = clickables.next();
    clickable = skipUnclickable(clickables, clickable);
    String segment = wordSegment.getEvent();
    if (DEBUG_DETAIL)
      logger.info("matchSegmentToWidgetForAudio compare :" +
          "\n\tsegment      " + segment + //" length " + segmentLength +
          "\n\tvs clickable " + clickable);

    String lcSegment = removePunct(segment.toLowerCase());
    String fragment1 = removePunct(clickable.getContent().toLowerCase());

    if (DEBUG_DETAIL)
      logger.info("matchSegmentToWidgetForAudio compare :" +
          "\n\tlc segment   " + lcSegment + //" length " + segmentLength +
          "\n\tvs fragment1 '" + fragment1 + "'");

    boolean showPhones = shouldShowPhones();

    if (lcSegment.equalsIgnoreCase(fragment1)) {  // easy match -
      if (showPhones) {
        DivWidget phoneDivBelowWord = getPhoneDivBelowWord(wordSegment, phonesInWord, audioControl, phoneMap);
        addSouthClickable(clickablePhones, clickable, phoneDivBelowWord);
      }

      return clickable;
    } else {
      Collection<IHighlightSegment> bulk = getMatchingSegments(clickables, clickable, lcSegment);// : Collections.EMPTY_LIST;

      if (bulk.isEmpty()) {
        return null;
      } else { // all clickables match this segment
        AllHighlight allHighlight = new AllHighlight(bulk);
        if (showPhones) {
          DivWidget phoneDivBelowWord = getPhoneDivBelowWord(wordSegment, phonesInWord, audioControl, phoneMap);

          addSouthClickable(clickablePhones, allHighlight, phoneDivBelowWord);
        }

        if (DEBUG)
          logger.info("matchSegmentToWidgetForAudio create composite from " + bulk.size() + " = " + allHighlight);

        return allHighlight;
      }
    }
  }

  protected boolean shouldShowPhones() {
    return false;
  }

  private void addSouthClickable(DivWidget clickablePhones, IHighlightSegment clickable, DivWidget phoneDivBelowWord) {
    clickable.setSouth(phoneDivBelowWord);
    clickablePhones.add(phoneDivBelowWord);
    phoneDivBelowWord.addStyleName(isRTL ? "leftFiveMargin" : "rightFiveMargin");
  }

  /**
   * @param wordSegment
   * @param phonesInWord
   * @param audioControl
   * @param phoneMap
   * @return
   */
  @NotNull
  private DivWidget getPhoneDivBelowWord(TranscriptSegment wordSegment,
                                         List<TranscriptSegment> phonesInWord,
                                         AudioControl audioControl,
                                         TreeMap<TranscriptSegment, IHighlightSegment> phoneMap) {
    return new WordTable().getPhoneDivBelowWord(audioControl, phoneMap, phonesInWord, true, wordSegment/*, isRTL*/);
  }

  /**
   * @param clickables
   * @param clickable
   * @param lcSegment
   * @return
   * @see #matchEventSegmentToClickable
   */
  @NotNull
  private Collection<IHighlightSegment> getMatchingSegments(Iterator<IHighlightSegment> clickables,
                                                            IHighlightSegment clickable,
                                                            String lcSegment) {
    Collection<IHighlightSegment> bulk = new ArrayList<>();

    if (DEBUG_MATCH) logger.info("\tgetMatchingSegments (2) compare :" +
        "\n\tsegment " + lcSegment +
        "\n\tvs      " + clickable);

    while (!lcSegment.isEmpty()) {
      String lcClickable = clickable.getContent().toLowerCase();
      String fragment = removePunct(lcClickable);
/*
      if (fragment.isEmpty()) {
        logger.info("BEFORE '" + lcClickable +
            "' after '" + fragment +
            "'");
      }
      */
      if (DEBUG_MATCH) logger.info("\tgetMatchingSegments (2) compare :" +
          "\n\tsegment     " + lcSegment + " " + lcSegment.length() +
          "\n\tvs fragment '" + fragment + "' " + fragment.length());

      boolean segmentHasFragment = lcSegment.startsWith(fragment) || (isMandarin && fragment.length() <= lcSegment.length());
      if (segmentHasFragment) {
        bulk.add(clickable);
        lcSegment = lcSegment.substring(fragment.length());
        // if (DEBUG_MATCH) logger.info("\tgetMatchingSegments segment now '" + lcSegment + "'");

        if (!clickables.hasNext() || lcSegment.isEmpty()) {
          break;
        }
        clickable = clickables.next();
        // logger.info("clickable now        " + clickable);
        clickable = skipUnclickable(clickables, clickable);
        // logger.info("after skip clickable " + clickable);
      } else {
        if (DEBUG_MATCH) {
          logger.info("\tgetMatchingSegments (2) NOPE compare :" +
              "\n\tsegment     '" + lcSegment + "'" +
              "\n\tvs fragment '" + fragment + "'");
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

  private boolean transcriptMatches(List<IHighlightSegment> clickables,
                                    List<TranscriptSegment> segments) {
    int i = 0;
    int c = 0;
    for (IHighlightSegment clickable : clickables) {
      if (clickable.isClickable()) {
        c++;
      }
    }
    boolean b = c == segments.size();
    if (!b) {
      if (DEBUG) logger.info("clickables " + c + " segments " + segments.size());
      StringBuilder builder = new StringBuilder();
      for (IHighlightSegment clickable : clickables) {
        if (clickable.isClickable()) {
          builder.append(clickable.getContent()).append(" ");
        }
      }
      if (DEBUG) logger.info("clickable : " + builder);

      StringBuilder builder2 = new StringBuilder();
      for (TranscriptSegment segment : segments) {
        builder2.append(segment.getEvent()).append(" ");
      }
      if (DEBUG) logger.info("align    : " + builder2);

    }
    return b;
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
   * @param t
   * @return
   * @see #doOneToManyMatch
   */
  private String removePunct(String t) {
    return fromFull(t
        .replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, "")
        .replaceAll("['%\\u06D4\\u060C\\u0022\\uFF01-\\uFF0F\\uFF1A-\\uFF1F\\u3001\\u3002\\u003F\\u00A1\\u00BF\\u002E\\u002C\\u002D\\u0021\\u2026\\u2019\\u005C\\u2013\\u061F\\uFF0C\\u201D]", ""));
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
  private IHighlightSegment skipUnclickable(Iterator<IHighlightSegment> iterator, IHighlightSegment clickable) {
    while (!clickable.isClickable() && iterator.hasNext()) {
      // logger.info("skipUnclickable : skip " + clickable);
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
   * @see TwoColumnExercisePanel#makeFirstRow(ClientExercise, DivWidget, boolean)
   */
  @NotNull
  protected DivWidget getFLEntry(T e) {
    flclickables = new ArrayList<>();
    flClickableRow = clickableWords.getClickableWords(getFL(e), FieldType.FL, flclickables, isRTL);
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
  protected boolean hasAudio(T e) {
    return e.hasAudioNonContext(true);
  }

  public void contextAudioChanged(int id, long duration) {
    logger.info("contextAudioChanged : audio changed for " + id + " - " + duration);
    audioChanged(id, duration);
  }

  public DivWidget getFlClickableRow() {
    return flClickableRow;
  }

  @Override
  public void doPlayPauseToggle() {
    //if (playAudio.isPlaying()) {
    playAudio.doPlayPauseToggle();
//    }
//    else {
//      audioChanged(mr.getUniqueID(), mr.getDurationInMillis());
//      playAudio(mr);
//    }
  }

  public void addPlayListener(PlayListener playListener) {
    playAudio.addPlayListener(playListener);
  }

  @Override
  public boolean doPause() {
    return playAudio.doPause();
  }

  public void clearHighlight() {

  }

  public boolean isPlaying() {
    return playAudio.isPlaying();
  }
}
