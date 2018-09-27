package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.exercise.CommentBox;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.sound.AllHighlight;
import mitll.langtest.client.sound.AudioControl;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.client.sound.SegmentHighlightAudioControl;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.scoring.AlignmentOutput;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static mitll.langtest.client.qc.QCNPFExercise.ENGLISH;
import static mitll.langtest.client.qc.QCNPFExercise.FOREIGN_LANGUAGE;
import static mitll.langtest.client.scoring.PhonesChoices.SHOW;

/**
 * Created by go22670 on 3/23/17.
 */
public class TwoColumnExercisePanel<T extends CommonExercise> extends DivWidget implements AudioChangeListener, RefAudioGetter {
  private Logger logger = Logger.getLogger("TwoColumnExercisePanel");


  private static final boolean HIDE_UNSAFE = false;

 public enum FieldType {FL, TRANSLIT, MEANING, EN}

  private static final String N_A = "N/A";

  private static final String LEFT_WIDTH = "60%";
  private static final int LEFT_WIDTH_NO_ENGLISH_VALUE = 85;
  private static final String LEFT_WIDTH_NO_ENGLISH = LEFT_WIDTH_NO_ENGLISH_VALUE + "%";

  /**
   *
   */
  private static final String RIGHT_WIDTH = "40%";
  private static final String RIGHT_WIDTH_NO_ENGLISH = (100 - LEFT_WIDTH_NO_ENGLISH_VALUE) + "%";


  private static final Set<String> TO_IGNORE = new HashSet<>(Arrays.asList("sil", "SIL", "<s>", "</s>"));

  static final int CONTEXT_INDENT = 45;//50;
  private static final char FULL_WIDTH_ZERO = '\uFF10';
  private static final char FULL_WIDTH_NINE = '\uFF10' + 9;

  private final T exercise;
  private final ExerciseController controller;

  private final CommentAnnotator annotationHelper;
  private ClickableWords<T> clickableWords;
  private static final boolean showInitially = false;
  private UnitChapterItemHelper<CommonExercise> commonExerciseUnitChapterItemHelper;
  private final ListInterface<CommonShell, T> listContainer;
  private ChoicePlayAudioPanel playAudio, contextPlay;
  private List<IHighlightSegment> altflClickables = null;
  /**
   * @see #getFLEntry
   */
  private List<IHighlightSegment> flclickables = null;
  private List<IHighlightSegment> contextClickables;//, altContextClickables;

  private DivWidget flClickableRow, altFLClickableRow;
  private DivWidget contextClickableRow;

  /**
   *
   */
  private DivWidget flClickableRowPhones, altFLClickableRowPhones;
  /**
   * @see #contextAudioChanged
   * @see #getContext
   */
  private DivWidget contextClickableRowPhones;

  private boolean showFL;
  private boolean showALTFL;
  /**
   *
   */
  private PhonesChoices phonesChoices;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_DETAIL = false;
  private static final boolean DEBUG_MATCH = false;
  private boolean isRTL = false;

  private AlignmentFetcher alignmentFetcher;
  private ItemMenu itemMenu;
  private boolean addPlayer;

  /**
   * Mandarin has special rules for the moment so we can match simplified chinese characters to traditional ones...
   */
  private boolean isMandarin;

  /**
   * Has a left side -- the question content (Instructions and audio panel (play button, waveform)) <br></br>
   * and a right side --
   *
   * @param commonExercise for this exercise
   * @param controller
   * @param listContainer
   * @param alignments
   * @param addPlayer
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel
   * @see mitll.langtest.client.banner.NewLearnHelper#getFactory
   * @see mitll.langtest.client.custom.content.NPFHelper#getFactory
   * @see mitll.langtest.client.custom.dialog.EditItem#setFactory
   */
  public TwoColumnExercisePanel(final T commonExercise,
                                final ExerciseController controller,
                                final ListInterface<CommonShell, T> listContainer,
                                Map<Integer, AlignmentOutput> alignments,
                                boolean addPlayer) {
    this.exercise = commonExercise;
    this.controller = controller;
    this.listContainer = listContainer;
    this.addPlayer = addPlayer;
    addStyleName("twoColumnStyle");
    isMandarin = getProjectStartupInfo() != null && getProjectStartupInfo().getLanguageInfo() == Language.MANDARIN;
    annotationHelper = controller.getCommentAnnotator();
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
    this.itemMenu = new ItemMenu(controller, commonExercise);
  }

  @Override
  public void addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices) {
    this.showFL = showFL;
    this.showALTFL = showALTFL;

    this.phonesChoices = phonesChoices;

    ProjectStartupInfo projectStartupInfo = getProjectStartupInfo();

    if (projectStartupInfo != null) {
      int fontSize = projectStartupInfo.getLanguageInfo().getFontSize();
      clickableWords = new ClickableWords<>(listContainer, exercise.getID(), controller.getLanguage(), fontSize);
      this.isRTL = clickableWords.isRTL(exercise);

      commonExerciseUnitChapterItemHelper = new UnitChapterItemHelper<>(controller.getTypeOrder());
      add(getItemContent(exercise));
    } else {
      //  logger.warning("addWidgets no project startup info?");
      clickableWords = null;
      commonExerciseUnitChapterItemHelper = null;
    }
  }

  @Override
  public void getRefAudio(RefAudioListener listener) {
    alignmentFetcher.getRefAudio(listener);
  }


  private ProjectStartupInfo getProjectStartupInfo() {
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

  @Override
  public void audioChangedWithAlignment(int id, long duration, AlignmentOutput alignmentOutputFromAudio) {
    if (alignmentOutputFromAudio != null) {
//      alignments.put(id, alignmentOutputFromAudio);
      alignmentFetcher.rememberAlignment(id, alignmentOutputFromAudio);
    }
    //logger.info("audioChangedWithAlignment " + id + " : " + alignmentOutputFromAudio);
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

  private int currentAudioDisplayed = -1;

  /**
   * TODO : don't do this twice!
   *
   * @param id
   * @param duration
   * @param alignmentOutput
   */
  private void showAlignment(int id, long duration, AlignmentOutput alignmentOutput) {
    if (alignmentOutput != null) {
      if (currentAudioDisplayed != id) {
        currentAudioDisplayed = id;
        if (DEBUG)
          logger.info("showAlignment for ex " + exercise.getID() + " audio id " + id + " : " + alignmentOutput);
        List<IHighlightSegment> flclickables = this.flclickables == null ? altflClickables : this.flclickables;
        DivWidget flClickableRow = this.flClickableRow == null ? altFLClickableRow : this.flClickableRow;
        DivWidget flClickablePhoneRow = this.flClickableRowPhones == null ? altFLClickableRowPhones : this.flClickableRowPhones;
        matchSegmentsToClickables(id, duration, alignmentOutput, flclickables, this.playAudio, flClickableRow, flClickablePhoneRow);
      }
    } else {
      if (DEBUG)
        logger.info("showAlignment no alignment info for ex " + exercise.getID() + " " + id + " dur " + duration);
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
  private void matchSegmentsToClickables(int id,
                                         long duration,
                                         AlignmentOutput alignmentOutput,
                                         List<IHighlightSegment> flclickables,
                                         ChoicePlayAudioPanel playAudio,
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
                               ChoicePlayAudioPanel playAudio) {
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
   * Row 1: FL - ENGLISH
   * Row 2: AltFL
   * Row 3: Transliteration
   * Row 4: Meaning
   * Row 5: context sentence fl - eng
   *
   * @return
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel#getQuestionContent
   */
  private Widget getItemContent(final T e) {
    long then = System.currentTimeMillis();
    Panel card = new DivWidget();
    card.setWidth("100%");

    boolean isEnglish = isEnglish();
    boolean useMeaningInsteadOfEnglish = isEnglish && isMeaningValid(e);
    String english = useMeaningInsteadOfEnglish ? e.getMeaning() : e.getEnglish();

    DivWidget rowWidget = getRowWidget();
    //rowWidget.getElement().setId("firstRow");

    boolean hasEnglish = isValid(english);
    Widget recordPanel = makeFirstRow(e, rowWidget, hasEnglish);
    // logger.info("safe " + e.getID() + " " + e.isSafeToDecode());
    if (HIDE_UNSAFE) recordPanel.setVisible(e.isSafeToDecode());
    card.add(rowWidget);

    //long now = System.currentTimeMillis();
    //  logger.info("getItemContent for " + e.getID() + " took " + (now - then) + " to add first row");

    {
      DivWidget lr = getHorizDiv();
      addFloatLeft(lr);
      lr.setWidth(hasEnglish ? RIGHT_WIDTH : RIGHT_WIDTH_NO_ENGLISH);
      if (hasEnglish) {
        lr.getElement().getStyle().setProperty("minWidth", "345px");
      }
      if (hasEnglish) lr.add(getEnglishWidget(e, english));
      lr.add(getItemWidget(e));

      DivWidget dropC = new DivWidget();
      dropC.add(itemMenu.getDropdown());
      lr.add(dropC);

      rowWidget.add(lr);
    }

    rowWidget = getRowWidget();
    card.add(rowWidget);

    //rowWidget.getElement().setId("scoringRow");
    rowWidget.add(recordPanel);

    if (e.hasContext()) {
      addContext(e, card);
    }

    if (DEBUG) {
      long now = System.currentTimeMillis();
      logger.info("getItemContent for " + e.getID() + " took " + (now - then));
    }
    return card;
  }

  private boolean isEnglish() {
    return controller.getLanguage().equalsIgnoreCase("english");
  }


  /**
   * @param e
   * @param rowWidget
   * @return
   * @see #getItemContent
   */
  @NotNull
  private SimpleRecordAudioPanel<T> makeFirstRow(T e, DivWidget rowWidget, boolean hasEnglish) {
    //  long then = System.currentTimeMillis();
    SimpleRecordAudioPanel<T> recordPanel = getRecordPanel(e);

    DivWidget flContainer = getHorizDiv();
    //flContainer.getElement().setId("flWidget");

    {
      DivWidget recordButtonContainer = new DivWidget();
      recordButtonContainer.addStyleName("recordingRowStyle");
      recordButtonContainer.add(recordPanel.getPostAudioRecordButton());
      if (HIDE_UNSAFE) recordButtonContainer.setVisible(e.isSafeToDecode());
      flContainer.add(recordButtonContainer);
    }

    long now = System.currentTimeMillis();
    //  logger.info("makeFirstRow for " + e.getID() + " took " + (now - then) + " to add rec");

    if (hasAudio(e)) {
      flContainer.add(playAudio = getPlayAudioPanel());
      alignmentFetcher.setPlayAudio(playAudio);
    }
    //else {
    // logger.info("makeFirstRow no audio in " + e.getAudioAttributes());
    //}

    DivWidget fieldContainer = new DivWidget();
    fieldContainer.setWidth("100%");
    fieldContainer.getElement().setId("leftSideFieldContainer");

    String trim = getAltFL(e).trim();

    now = System.currentTimeMillis();
    // logger.info("makeFirstRow for " + e.getID() + " took " + (now - then) + " to add rec and play");

    if (showFL || getFL(e).trim().equals(trim) || trim.isEmpty()) {
      fieldContainer.add(getFLEntry(e));
      fieldContainer.add(flClickableRowPhones = clickableWords.getClickableDiv(isRTL));
      flClickableRowPhones.getElement().setId("flClickableRowPhones");
      stylePhoneRow(flClickableRowPhones);

      if (playAudio != null && playAudio.getCurrentAudioAttr() != null) {
        AudioAttribute currentAudioAttr = playAudio.getCurrentAudioAttr();

        if (DEBUG) logger.info("audioChangedWithAlignment audio " + currentAudioAttr.getUniqueID());

        audioChangedWithAlignment(currentAudioAttr.getUniqueID(),
            currentAudioAttr.getDurationInMillis(),
            currentAudioAttr.getAlignmentOutput());
      }
    }

//    now = System.currentTimeMillis();
    // logger.info("makeFirstRow for " + e.getID() + " took " + (now - then) + " to fl row");

    if (showALTFL) {
      addField(fieldContainer, addAltFL(e, showFL));
      altFLClickableRowPhones = clickableWords.getClickableDiv(isRTL);
      altFLClickableRowPhones.getElement().setId("altFLClickableRowPhones");
      stylePhoneRow(altFLClickableRowPhones);

      addField(fieldContainer, altFLClickableRowPhones);
    }

    addField(fieldContainer, addTransliteration(e));

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
    }

    flContainer.add(fieldContainer);
    flContainer.setWidth(hasEnglish ? LEFT_WIDTH : LEFT_WIDTH_NO_ENGLISH);

    rowWidget.add(flContainer);
    return recordPanel;
  }

  private void stylePhoneRow(UIObject phoneRow) {
    //  phoneRow.addStyleName("inlineFlex");
    if (isRTL) phoneRow.addStyleName("floatRight");
  }

  /**
   * @param e
   * @return
   * @see #makeFirstRow
   */
  @NotNull
  private DivWidget getFLEntry(T e) {
    flclickables = new ArrayList<>();

    DivWidget contentWidget = clickableWords.getClickableWords(getFL(e),
        FieldType.FL,
        flclickables, false, isRTL);

    flClickableRow = contentWidget;

    DivWidget flEntry = getCommentEntry(FOREIGN_LANGUAGE,
        e.getAnnotation(FOREIGN_LANGUAGE),
        false,
        showInitially,
        annotationHelper, isRTL, contentWidget, e.getID());

    if (isRTL) {
      clickableWords.setDirection(flEntry);
    } else {
      addFloatLeft(flEntry);
    }
    flEntry.setWidth("100%");
    return flEntry;
  }

  private String getFL(CommonShell e) {
    String flToShow = e.getFLToShow();
    //logger.info("for " + e.getID() + " " + flToShow + " should swap " +e.shouldSwap());
    return flToShow;
  }

  private String getAltFL(CommonExercise exercise) {
    return exercise.getAltFLToShow();
  }

  /**
   * @param e
   * @return
   * @see #makeFirstRow
   */
  private boolean hasAudio(T e) {
    return e.hasAudioNonContext(true);
  }

  /**
   * @param parentExercise
   * @param card
   * @paramx rowWidget
   * @see #getItemContent
   */
  private void addContext(T parentExercise, Panel card) {
    //  int c = 0;
    String foreignLanguage = getFL(parentExercise);
    String altFL = getAltFL(parentExercise);
    Collection<CommonExercise> directlyRelated = parentExercise.getDirectlyRelated();
    for (CommonExercise contextEx : directlyRelated) {
      DivWidget rowWidget = getRowWidget();
      card.add(rowWidget);
      addContextFields(rowWidget, foreignLanguage, altFL, contextEx);
/*
      c++;
      if (c < directlyRelated.size()) {
        card.add(rowWidget = getRowWidget());
      }*/
    }
  }

  /**
   * @see #addContext(CommonExercise, Panel)
   * @param rowWidget
   * @param foreignLanguage
   * @param altFL
   * @param contextEx
   */
  private void addContextFields(DivWidget rowWidget,
                                String foreignLanguage,
                                String altFL,
                                CommonExercise contextEx) {
    AnnotationHelper annotationHelper = new AnnotationHelper(controller, controller.getMessageHelper());

    Panel context = getContext(contextEx, foreignLanguage, altFL, annotationHelper);
    if (context != null) {
      rowWidget.add(context);
      context.setWidth("100%");
    }

    {
      String contextTranslation = contextEx.getEnglish();

      boolean same = getFL((T) contextEx).equals(contextTranslation);
      if (!same) {
        if (context != null && !contextTranslation.isEmpty()) {
          context.setWidth(LEFT_WIDTH);
        }

        Widget contextTransWidget = addContextTranslation(contextEx, contextTranslation, annotationHelper);

        if (contextTransWidget != null) {
          contextTransWidget.addStyleName("rightsidecolor");
          contextTransWidget.addStyleName("leftFiveMargin");
          contextTransWidget.setWidth(RIGHT_WIDTH);
          rowWidget.add(contextTransWidget);
        }
      }
    }
  }

  private Widget getAltContext(String flToHighlight, String altFL, AnnotationHelper annotationHelper, int exid) {
    Panel contentWidget = clickableWords.getClickableWordsHighlight(altFL, flToHighlight,
        FieldType.FL, new ArrayList<>(), true);

    CommentBox commentBox = getCommentBox(annotationHelper, exid);
    return commentBox
        .getEntry(QCNPFExercise.ALTCONTEXT, contentWidget,
            exercise.getAnnotation(QCNPFExercise.ALTCONTEXT), showInitially, isRTL);
  }

  /**
   * @return
   * @see #makeFirstRow
   */
  @NotNull
  private ChoicePlayAudioPanel getPlayAudioPanel() {
    return new ChoicePlayAudioPanel(controller.getSoundManager(), exercise, controller, false, this);
  }

  /**
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
    Widget englishWidget = getEntry(e, QCNPFExercise.ENGLISH, english,
        FieldType.EN,
        showInitially, new ArrayList<>(), true, annotationHelper, false);
    englishWidget.addStyleName("rightsidecolor");
    englishWidget.getElement().setId("englishWidget");
    englishWidget.addStyleName("floatLeft");
    englishWidget.addStyleName("leftFiveMargin");
    // englishWidget.setWidth("90%");
    return englishWidget;
  }

  private void addField(Panel grid, Widget widget) {
    if (widget != null) {
      widget.addStyleName("topFiveMargin");
      grid.add(widget);
    }
  }

  /**
   * TODO: do we need this???
   *
   * @param e
   * @return
   * @see #getItemContent
   */
  @NotNull
  private SimpleRecordAudioPanel<T> getRecordPanel(T e) {
    return new SimpleRecordAudioPanel<>(controller, e, listContainer, addPlayer);
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
    flContainer.addStyleName("scoringRowStyle");
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

      DivWidget contentWidget = clickableWords.getClickableWords(altFL,
          FieldType.FL,
          altflClickables,
          false,
          //true,
          isRTL);

      altFLClickableRow = contentWidget;

      DivWidget flEntry = getCommentEntry(QCNPFExercise.ALTFL, e.getAnnotation(QCNPFExercise.ALTFL), false,
          showInitially, annotationHelper, isRTL, contentWidget, e.getID());

      if (addTopMargin) {
        contentWidget.getElement().getStyle().setMarginTop(5, Style.Unit.PX);
      }
      if (!isRTL) {
        addFloatLeft(flEntry);
      } else {
        clickableWords.setDirection(flEntry);
      }
      flEntry.setWidth("100%");
      return flEntry;

    } else return null;
  }

  private void addFloatLeft(DivWidget flEntry) {
    flEntry.addStyleName("floatLeft");
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
   * @return
   * @see #addContextFields(DivWidget, String, String, CommonExercise)
   */
  private Panel getContext(CommonExercise contextExercise,
                           String itemText,
                           String altFL,
                           AnnotationHelper annotationHelper) {
    String context = getFL(contextExercise);

    if (!context.isEmpty()) {
      Panel hp = new DivWidget();
      {
        hp.addStyleName("inlineFlex");
        hp.addStyleName("leftFiveMargin");
        hp.getElement().setId("contentContainer");
        hp.add(getSpacer());
      }
      ChoicePlayAudioPanel contextPlay = getContextPlay(contextExercise);
      hp.add(contextPlay);

      DivWidget contentWidget = clickableWords.getClickableWordsHighlight(context, itemText,
          FieldType.FL, contextClickables = new ArrayList<>(), true);

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
          Widget altContext = getAltContext(altFL, altFL1, annotationHelper, contextExercise.getID());
          if (showFL && showALTFL) altContext.addStyleName("topFiveMargin");
          col.add(altContext);
        }
      }

      if (contextPlay != null && contextPlay.getCurrentAudioAttr() != null) {
        AudioAttribute currentAudioAttr = contextPlay.getCurrentAudioAttr();
        contextAudioChangedWithAlignment(currentAudioAttr.getUniqueID(),
            currentAudioAttr.getDurationInMillis(),
            currentAudioAttr.getAlignmentOutput());
      }

      return hp;
    } else {
      return null;
    }
  }


  @NotNull
  private DivWidget getSpacer() {
    DivWidget spacer = new DivWidget();
    spacer.getElement().getStyle().setProperty("minWidth", CONTEXT_INDENT + "px");
    return spacer;
  }

  /**
   * @see #getContext
   * @param contextExercise
   * @return
   */
  private ChoicePlayAudioPanel getContextPlay(CommonAudioExercise contextExercise) {
    AudioChangeListener contextAudioChanged = new AudioChangeListener() {
      @Override
      public void audioChanged(int id, long duration) {
        if (DEBUG) {
          logger.info("getContextPlay audioChanged for ex " + exercise.getID() + "/" + contextExercise.getID() +
              " CONTEXT audio id "
          );
        }

        contextAudioChanged(id, duration);
      }

      @Override
      public void audioChangedWithAlignment(int id, long duration, AlignmentOutput alignmentOutputFromAudio) {
        if (DEBUG) {
          logger.info("getContextPlay audioChangedWithAlignment for ex " + exercise.getID() + "/" + contextExercise.getID() +
              " CONTEXT audio id "
          );
        }
        contextAudioChangedWithAlignment(id, duration, alignmentOutputFromAudio);
      }
    };
    contextPlay
        = new ChoicePlayAudioPanel(controller.getSoundManager(), contextExercise, controller, true, contextAudioChanged);
    AudioAttribute audioAttrPrefGender = contextExercise.getAudioAttrPrefGender(controller.getUserManager().isMale());
    contextPlay.setEnabled(audioAttrPrefGender != null);
    alignmentFetcher.setContextPlay(contextPlay);

    return contextPlay;
  }

  private void contextAudioChangedWithAlignment(int id, long duration, AlignmentOutput alignmentOutputFromAudio) {
    if (DEBUG) {
      logger.info("contextAudioChangedWithAlignment audioChanged for ex " + exercise.getID() + " CONTEXT audio id " + id +
          " alignment " + alignmentOutputFromAudio);
    }

    if (alignmentOutputFromAudio != null) {
      alignmentFetcher.rememberAlignment(id, alignmentOutputFromAudio);
    }
    contextAudioChanged(id, duration);
  }

  public void contextAudioChanged(int id, long duration) {
    AlignmentOutput alignmentOutput = alignmentFetcher.getAlignment(id);
    if (alignmentOutput != null) {
      if (DEBUG) {
        logger.info("contextAudioChanged audioChanged for ex " + exercise.getID() + " CONTEXT audio id " + id +
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

  private boolean shouldShowPhones() {
    return phonesChoices == SHOW;
  }

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
        fieldType != FieldType.FL, isRTL);
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
