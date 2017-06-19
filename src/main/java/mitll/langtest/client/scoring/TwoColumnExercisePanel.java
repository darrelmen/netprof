package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.exercise.CommentBox;
import mitll.langtest.client.exercise.BusyPanel;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.qc.QCNPFExercise;
import mitll.langtest.client.sound.AllHighlight;
import mitll.langtest.client.sound.AudioControl;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.client.sound.SegmentHighlightAudioControl;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.shared.exercise.*;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.scoring.AlignmentOutput;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static mitll.langtest.client.qc.QCNPFExercise.ENGLISH;
import static mitll.langtest.client.qc.QCNPFExercise.FOREIGN_LANGUAGE;
import static mitll.langtest.client.scoring.PhonesChoices.HIDE;
import static mitll.langtest.client.scoring.PhonesChoices.SHOW;
import static mitll.langtest.client.scoring.ShowChoices.*;

/**
 * Created by go22670 on 3/23/17.
 */
public class TwoColumnExercisePanel<T extends CommonExercise> extends DivWidget implements AudioChangeListener, RefAudioGetter {
  private Logger logger = Logger.getLogger("TwoColumnExercisePanel");

  private static final String LEFT_WIDTH = "60%";
  private static final String RIGHT_WIDTH = "40%";

  /**
   *
   */
  private static final String EMAIL = "Email Item";
  private static final Set<String> TO_IGNORE = new HashSet<>(Arrays.asList("sil", "SIL", "<s>", "</s>"));

  static final int CONTEXT_INDENT = 56;
  private static final char FULL_WIDTH_ZERO = '\uFF10';
  private static final char FULL_WIDTH_NINE = '\uFF10' + 9;

  private final T exercise;
  private final ExerciseController controller;

  private final AnnotationHelper annotationHelper;
  private final ClickableWords<T> clickableWords;
  private final boolean showInitially = false;
  private final UnitChapterItemHelper<CommonExercise> commonExerciseUnitChapterItemHelper;
  private final ListInterface<CommonShell, T> listContainer;
  private ChoicePlayAudioPanel playAudio;

  /**
   *
   */
  private final Map<Integer, AlignmentOutput> alignments;

  private List<IHighlightSegment> altflClickables = null;
  /**
   * @see #getFLEntry
   */
  private List<IHighlightSegment> flclickables = null;
  private List<IHighlightSegment> contextClickables, altContextClickables;

  private DivWidget flClickableRow, altFLClickableRow;

  private final ShowChoices choices;
  private final PhonesChoices phonesChoices;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_MATCH = false;
  private boolean isRTL = false;
  private DivWidget contextClickableRow;

  /**
   * Has a left side -- the question content (Instructions and audio panel (play button, waveform)) <br></br>
   * and a right side --
   *
   * @param commonExercise for this exercise
   * @param controller
   * @param listContainer
   * @param phonesChoices
   * @param alignments
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel
   * @see mitll.langtest.client.banner.NewLearnHelper#getFactory
   */
  public TwoColumnExercisePanel(final T commonExercise,
                                final ExerciseController controller,
                                final ListInterface<CommonShell, T> listContainer,
                                ShowChoices choices,
                                PhonesChoices phonesChoices,
                                Map<Integer, AlignmentOutput> alignments) {
    this.exercise = commonExercise;
    this.controller = controller;
    this.listContainer = listContainer;

    getElement().setId("TwoColumnExercisePanel");
    addStyleName("cardBorderShadow");
    addStyleName("bottomFiveMargin");
    addStyleName("floatLeftAndClear");
    setWidth("100%");

    this.choices = choices;
    this.phonesChoices = phonesChoices;

    logger.info("phone display " + phonesChoices);

    this.alignments = alignments;

    annotationHelper = new AnnotationHelper(controller, commonExercise.getID());
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();

    if (projectStartupInfo != null) {
      int fontSize = projectStartupInfo.getLanguageInfo().getFontSize();
      clickableWords = new ClickableWords<T>(listContainer, commonExercise, controller.getLanguage(), fontSize, phonesChoices == SHOW);
      this.isRTL = clickableWords.isRTL(exercise);

      commonExerciseUnitChapterItemHelper = new UnitChapterItemHelper<>(controller.getTypeOrder());
      add(getItemContent(commonExercise));

      getRefAudio();
    } else {
      clickableWords = null;
      commonExerciseUnitChapterItemHelper = null;
    }
  }

  /**
   * @param listener
   * @see mitll.langtest.client.list.FacetExerciseList#getRefAudio(Iterator)
   */
  @Override
  public void getRefAudio(RefAudioListener listener) {
    AudioAttribute currentAudioAttr = playAudio == null ? null : playAudio.getCurrentAudioAttr();
    int refID = currentAudioAttr == null ? -1 : currentAudioAttr.getUniqueID();

    AudioAttribute contextAudioAttr = contextPlay != null ? contextPlay.getCurrentAudioAttr() : null;
    int contextRefID = contextAudioAttr != null ? contextAudioAttr.getUniqueID() : -1;

    Set<Integer> req = new HashSet<>();
    if (refID != -1) {
      if (DEBUG) {
        logger.info("getRefAudio asking for" +
                "\n\texercise " + exercise.getID() +
                "\n\taudio #" + refID //+
            //    "\n\tspeed  " + currentAudioAttr.getSpeed() +
            //    "\n\tisMale " + currentAudioAttr.getUser().isMale()
        );
      }
      if (addToRequest(currentAudioAttr, refID)) req.add(refID);
    } else {
      //  logger.warning("getRefAudio huh? how can audio id be -1??? " + currentAudioAttr);
    }

    if (contextRefID != -1) {
      // logger.info("getRefAudio asking for context " + contextRefID);
      if (DEBUG) {
        logger.info("getRefAudio asking for context" +
            "\n\texercise " + exercise.getID() +
            "\n\taudio #" + contextRefID +
            "\n\tspeed  " + contextAudioAttr.getSpeed() +
            "\n\tisMale " + contextAudioAttr.getUser().isMale()
        );
      }
      if (addToRequest(contextAudioAttr, contextRefID)) req.add(contextRefID);
    } else {
      //logger.warning("no context audio for " + exercise.getID());
    }

    if (req.isEmpty()) {
      registerSegments(refID, currentAudioAttr, contextRefID, contextAudioAttr);
      listener.refAudioComplete();
      cacheOthers(listener);
    } else {
      controller.getScoringService().getAlignments(
          controller.getProjectStartupInfo().getProjectid(),
          req, new AsyncCallback<Map<Integer, AlignmentOutput>>() {
            @Override
            public void onFailure(Throwable caught) {

            }

            @Override
            public void onSuccess(Map<Integer, AlignmentOutput> result) {
              alignments.putAll(result);
              registerSegments(refID, currentAudioAttr, contextRefID, contextAudioAttr);
              cacheOthers(listener);
            }
          });
    }
    // {
    //   logger.info("no current audio for " + exercise.getID());
    //  listener.refAudioComplete();
    // }
  }

  public int getRefAudio() {
    AudioAttribute currentAudioAttr = playAudio == null ? null : playAudio.getCurrentAudioAttr();
    return currentAudioAttr == null ? -1 : currentAudioAttr.getUniqueID();
  }

  public int getContextRefAudio() {
    AudioAttribute contextAudioAttr = contextPlay != null ? contextPlay.getCurrentAudioAttr() : null;
    return contextAudioAttr != null ? contextAudioAttr.getUniqueID() : -1;
  }

  private boolean addToRequest(AudioAttribute currentAudioAttr, int refID) {
    if (!alignments.containsKey(refID)) {
      AlignmentOutput alignmentOutput = currentAudioAttr.getAlignmentOutput();
      if (alignmentOutput == null) {
        return true;
      } else {
        // logger.info("addToRequest remember " + refID + " " + alignmentOutput);
        alignments.put(refID, alignmentOutput);
        return false;
      }
    } else {
      return false;
    }
  }

  private void registerSegments(int refID,
                                AudioAttribute currentAudioAttr,
                                int contextRefID,
                                AudioAttribute currentAudioAttr1) {
    if (refID != -1) {
      audioChanged(refID, currentAudioAttr.getDurationInMillis());
    } else {
      //logger.warning("registerSegments huh? register " + refID);
    }
    if (contextRefID != -1) {
      contextAudioChanged(contextRefID, currentAudioAttr1.getDurationInMillis());
    }
  }

  private void cacheOthers(RefAudioListener listener) {
    Set<Integer> req = getReqAudio();

    if (!req.isEmpty()) {
      //  logger.info("cacheOthers (" + exercise.getID() + ") Asking for audio alignments for " + req + " knownAlignments " + alignments.size());
      ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
      if (projectStartupInfo != null) {
        controller.getScoringService().getAlignments(projectStartupInfo.getProjectid(),
            req, new AsyncCallback<Map<Integer, AlignmentOutput>>() {
              @Override
              public void onFailure(Throwable caught) {

              }

              @Override
              public void onSuccess(Map<Integer, AlignmentOutput> result) {
                alignments.putAll(result);
                listener.refAudioComplete();
              }
            });
      }
    } else {
      listener.refAudioComplete();
    }
  }

  public Set<Integer> getReqAudio() {
    Set<Integer> req = playAudio == null ? new HashSet<>() : new HashSet<>(playAudio.getAllAudioIDs());

    if (contextPlay != null) {
      req.addAll(contextPlay.getAllAudioIDs());
    }
    req.removeAll(alignments.keySet());

    return req;
  }

  /**
   * @param id
   * @param duration
   * @see ChoicePlayAudioPanel#addChoices
   */
  @Override
  public void audioChanged(int id, long duration) {
    if (phonesChoices == SHOW) {
      AlignmentOutput alignmentOutput = alignments.get(id);
      if (alignmentOutput != null) {
        if (DEBUG) logger.info("audioChanged for ex " + exercise.getID() + " audio id " + id);
        List<IHighlightSegment> flclickables = this.flclickables == null ? altflClickables : this.flclickables;
        DivWidget flClickableRow = this.flClickableRow == null ? altFLClickableRow : this.flClickableRow;
        matchSegmentsToClickables(id, duration, alignmentOutput, flclickables, this.playAudio, flClickableRow);
      } else {
        if (DEBUG)
          logger.info("audioChanged no alignment info for ex " + exercise.getID() + " " + id + " dur " + duration);
      }
    }
  }

  /**
   * @param id
   * @param duration
   * @param alignmentOutput
   * @param flclickables
   * @param playAudio
   * @param clickableRow
   * @paramz typeToSegmentToWidget
   * @see #audioChanged
   * @see #contextAudioChanged
   */
  private void matchSegmentsToClickables(int id,
                                         long duration,
                                         AlignmentOutput alignmentOutput,
                                         List<IHighlightSegment> flclickables,
                                         ChoicePlayAudioPanel playAudio,
                                         DivWidget clickableRow) {
    Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget =
        matchSegmentToWidgetForAudio(id, duration, alignmentOutput, flclickables, playAudio, clickableRow);
    setPlayListener(id, duration, typeToSegmentToWidget, playAudio);
  }

  private void setPlayListener(int id,
                               long duration,
                               Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget,
                               ChoicePlayAudioPanel playAudio) {
    if (DEBUG) {
      logger.info("audioChanged for ex " + exercise.getID() +
          " audio id " + id + " : " +
          (typeToSegmentToWidget == null ? "missing" : typeToSegmentToWidget.size()));
    }

    if (typeToSegmentToWidget == null) {
      logger.warning("audioChanged no type to segment for " + id + " and exercise " + exercise.getID());
    } else {
      if (DEBUG) {
        TreeMap<TranscriptSegment, IHighlightSegment> transcriptSegmentIHighlightSegmentTreeMap = typeToSegmentToWidget.get(NetPronImageType.WORD_TRANSCRIPT);
        logger.info("audioChanged segments now for ex " + exercise.getID() +
            " audio " + id + " dur " + duration +
            "\n\twords: " + transcriptSegmentIHighlightSegmentTreeMap.keySet() +
            "\n\tphone: " + typeToSegmentToWidget.get(NetPronImageType.PHONE_TRANSCRIPT).keySet()
        );

      }
      playAudio.setListener(new SegmentHighlightAudioControl(typeToSegmentToWidget));
    }
  }

  /**
   * TODOx : what to do about chinese?
   *
   * @param audioID
   * @param alignmentOutput
   * @see #matchSegmentsToClickables
   */
  private Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> matchSegmentToWidgetForAudio(Integer audioID,
                                                                                                            long durationInMillis,
                                                                                                            AlignmentOutput alignmentOutput,
                                                                                                            List<IHighlightSegment> flclickables,
                                                                                                            AudioControl audioControl,
                                                                                                            DivWidget clickableRow) {
    Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> value = new HashMap<>();

    TreeMap<TranscriptSegment, IHighlightSegment> segmentToWord = new TreeMap<>();
    value.put(NetPronImageType.WORD_TRANSCRIPT, segmentToWord);

    if (alignmentOutput == null) {
      logger.warning("matchSegmentToWidgetForAudio no alignment for " + audioID);
      segmentToWord.put(new TranscriptSegment(0, (float) durationInMillis, "all", 0),
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
          doOneToOneMatch(phones, audioControl, phoneMap, segmentToWord, iterator, wordSegments);
        } else {
          if (DEBUG_MATCH) logger.warning("matchSegmentToWidgetForAudio no match for" +
              "\n\tsegments " + wordSegments +
              "\n\tto       " + flclickables);

          clickableRow.clear();
          doOneToManyMatch(phones,
              audioControl, phoneMap, segmentToWord, iterator, wordSegments, clickableRow);
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
                                DivWidget clickableRow) {
    ListIterator<TranscriptSegment> transcriptSegmentListIterator = wordSegments.listIterator();
    while (transcriptSegmentListIterator.hasNext()) {
      TranscriptSegment wordSegment = transcriptSegmentListIterator.next();

      if (clickablesIterator.hasNext()) {
        List<TranscriptSegment> phonesInWord = getPhonesInWord(phones, wordSegment);

        if (DEBUG)
          logger.info("doOneToManyMatch got segment " + wordSegment);// + " length " + segmentLength);

        IHighlightSegment value1 =
            matchEventSegmentToClickable(clickablesIterator, wordSegment, phonesInWord, audioControl, phoneMap);

        if (value1 == null) {
          logger.info("doOneToManyMatch can't find match for wordSegment " + wordSegment);

          // so here we have something where we need more segments for this clickable...?
          if (clickablesIterator.hasPrevious()) {  // so now we want to put segment phones underneath clickable
            IHighlightSegment current = clickablesIterator.previous();

            String lcSegment = removePunct(wordSegment.getEvent().toLowerCase());
            String fragment1 = removePunct(current.getContent().toLowerCase());

            List<TranscriptSegment> phonesInWordAll = new ArrayList<>();

            while (!fragment1.isEmpty()) {
              boolean fragmentContainsSegment = fragment1.startsWith(lcSegment);

              if (fragmentContainsSegment) {
                logger.info("doOneToManyMatch OK, match for word segment " + lcSegment + " inside " + fragment1);

                fragment1 = fragment1.substring(lcSegment.length());
                phonesInWordAll.addAll(phonesInWord);

                logger.info("\t doOneToManyMatch now clickable segment " + fragment1 + " after removing " + lcSegment + " now " + phonesInWordAll.size() + " phones");

                if (!fragment1.isEmpty()) {
                  if (transcriptSegmentListIterator.hasNext()) {
                    wordSegment = transcriptSegmentListIterator.next();
                    lcSegment = removePunct(wordSegment.getEvent().toLowerCase());
                    phonesInWord = getPhonesInWord(phones, wordSegment);
                  }
                }
              } else {
                logger.warning("doOneToManyMatch no match for align word '" + lcSegment +
                    "'  vs '" + fragment1 +
                    "'");
                break;
              }
            }

            if (phonesInWordAll.isEmpty()) {
              logger.warning("doOneToManyMatch no matches for " + current + " and " + lcSegment);
            } else {
              current.setSouth(getPhoneDivBelowWord(wordSegment, phonesInWordAll, audioControl, phoneMap));
              segmentToWord.put(wordSegment, current); // only one for now...
              clickableRow.add(current.asWidget());

              if (!isRTL) {
                current.asWidget().addStyleName("floatLeft");
              }

              clickablesIterator.next(); // OK, we've done this clickable
            }

          } else {
//            value1.asWidget().addStyleName("floatLeft");
//            segmentToWord.put(wordSegment, value1);
//            clickableRow.add(value1.asWidget());

            logger.warning("doOneToManyMatch no match for " + wordSegment);
          }
        } else {
          // logger.warning("doOneToManyMatch no match for " + wordSegment);

          if (!isRTL) {
            value1.asWidget().addStyleName("floatLeft");
          }
          segmentToWord.put(wordSegment, value1);
          clickableRow.add(value1.asWidget());
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
    }
  }

  private void doOneToOneMatch(List<TranscriptSegment> phones,
                               AudioControl audioControl,
                               TreeMap<TranscriptSegment, IHighlightSegment> phoneMap,
                               TreeMap<TranscriptSegment, IHighlightSegment> segmentToWord,
                               Iterator<IHighlightSegment> iterator, List<TranscriptSegment> wordSegments) {
    for (TranscriptSegment wordSegment : wordSegments) {
      if (iterator.hasNext()) {
        List<TranscriptSegment> phonesInWord = getPhonesInWord(phones, wordSegment);

        if (DEBUG)
          logger.info("doOneToOneMatch got segment " + wordSegment);// + " length " + segmentLength);

        IHighlightSegment value1 =
            matchEventSegmentToClickable(iterator, wordSegment, phonesInWord, audioControl, phoneMap);

        if (value1 == null) {
          logger.warning("doOneToOneMatch can't find match for wordSegment " + wordSegment);
        } else {
          segmentToWord.put(wordSegment, value1);
        }
      } else {
        logger.warning("doOneToOneMatch no match for " + wordSegment);
      }
    }
  }

  private List<TranscriptSegment> getPhonesInWord(List<TranscriptSegment> phones, TranscriptSegment wordSegment) {
    List<TranscriptSegment> phonesInWord = getSegs(phones, wordSegment);
    if (isRTL) { // phones should play right to left
      Collections.reverse(phonesInWord);
    }
    return phonesInWord;
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
   * @return
   */
  private IHighlightSegment matchEventSegmentToClickable(Iterator<IHighlightSegment> clickables,
                                                         TranscriptSegment wordSegment,
                                                         List<TranscriptSegment> phonesInWord,
                                                         AudioControl audioControl,
                                                         TreeMap<TranscriptSegment, IHighlightSegment> phoneMap) {
    IHighlightSegment clickable = clickables.next();
    clickable = skipUnclickable(clickables, clickable);
    String segment = wordSegment.getEvent();
    if (DEBUG)
      logger.info("matchSegmentToWidgetForAudio compare :" +
          "\n\tsegment       " + segment + //" length " + segmentLength +
          "\n\tvs clickable '" + clickable + "'");

    String lcSegment = removePunct(segment.toLowerCase());
    String fragment1 = removePunct(clickable.getContent().toLowerCase());

    if (DEBUG)
      logger.info("matchSegmentToWidgetForAudio compare :" +
          "\n\tsegment       " + lcSegment + //" length " + segmentLength +
          "\n\tvs clickable '" + fragment1 + "'");

    boolean showPhones = phonesChoices == PhonesChoices.SHOW;

    if (lcSegment.equalsIgnoreCase(fragment1)) {  // easy match -
      if (showPhones) {
        clickable.setSouth(getPhoneDivBelowWord(wordSegment, phonesInWord, audioControl, phoneMap));
      }

      return clickable;
    } else {
      Collection<IHighlightSegment> bulk = showPhones ? getMatchingSegments(clickables, clickable, lcSegment) : Collections.EMPTY_LIST;

      if (bulk.isEmpty()) {
        return null;
      } else { // all clickables match this segment
        AllHighlight allHighlight = new AllHighlight(bulk);
        allHighlight.setSouth(getPhoneDivBelowWord(wordSegment, phonesInWord, audioControl, phoneMap));

        if (DEBUG)
          logger.info("matchSegmentToWidgetForAudio create composite from " + bulk.size() + " = " + allHighlight);

        return allHighlight;
      }
    }
  }

  @NotNull
  private DivWidget getPhoneDivBelowWord(TranscriptSegment wordSegment,
                                         List<TranscriptSegment> phonesInWord,
                                         AudioControl audioControl,
                                         TreeMap<TranscriptSegment, IHighlightSegment> phoneMap) {
    return new WordTable().getPhoneDivBelowWord(audioControl, phoneMap, phonesInWord, true, wordSegment, isRTL);
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
      String t = clickable.getContent().toLowerCase();
      String fragment = removePunct(t);
      if (fragment.isEmpty()) {
        logger.info("BEFORE '" + t +
            "' after '" + fragment +
            "'");
      }
      if (DEBUG_MATCH) logger.info("\tgetMatchingSegments compare :" +
          "\n\tsegment     " + lcSegment +
          "\n\tvs fragment '" + fragment + "'");

      boolean segmentHasFragment = lcSegment.startsWith(fragment);
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
          logger.info("\tgetMatchingSegments compare :" +
              "\n\tsegment     '" + lcSegment + "'" +
              "\n\tvs fragment '" + fragment + "'");
        }
        break;
      }
    }

    if (!lcSegment.isEmpty()) {
      logger.warning("getMatchingSegments couldn't match all of segment - this left over = " + lcSegment);
    }
    return bulk;
  }

  private boolean transcriptMatches(List<IHighlightSegment> clickables,
                                    List<TranscriptSegment> segments) {
    int i = 0;
    //List<IHighlightSegment> compOrder = clickables;

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
   *
   * @param t
   * @return
   * @see #doOneToManyMatch
   */
  private String removePunct(String t) {
    return fromFull(t
        .replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, "")
        .replaceAll("['%\\u06D4\\u060C\\u0022\\uFF01-\\uFF0F\\uFF1A-\\uFF1F\\u3001\\u3002\\u003F\\u00A1\\u00BF\\u002E\\u002C\\u0021\\u2026\\u2019\\u005C\\u2013\\u061F\\uFF0C\\u201D]", ""));
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
      logger.info("skipUnclickable : skip " + clickable);
      clickable = iterator.next();
    }
    return clickable;
  }

  private boolean shouldIgnore(TranscriptSegment seg) {
    return TO_IGNORE.contains(seg.getEvent());
  }

//  private HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
//    return addDomHandler(handler, MouseOverEvent.getType());
//  }

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
    Panel card = new DivWidget();
    card.getElement().setId("CommentNPFExercise_QuestionContent");
    card.setWidth("100%");

    boolean meaningValid = isMeaningValid(e);
    boolean isEnglish = controller.getLanguage().equalsIgnoreCase("english");
    boolean useMeaningInsteadOfEnglish = isEnglish && meaningValid;
    String english = useMeaningInsteadOfEnglish ? e.getMeaning() : e.getEnglish();

    DivWidget rowWidget = getRowWidget();
    rowWidget.getElement().setId("firstRow");

    SimpleRecordAudioPanel<T> recordPanel = makeFirstRow(e, rowWidget);
    card.add(rowWidget);

    if (isValid(english)) {
      DivWidget lr = getHorizDiv();
      lr.addStyleName("floatLeft");
      lr.setWidth(RIGHT_WIDTH);

      lr.add(getEnglishWidget(e, english));
      lr.add(getItemWidget(e));
      lr.add(getDropdown());

      rowWidget.add(lr);
    }

    rowWidget = getRowWidget();
    card.add(rowWidget);

    rowWidget.getElement().setId("scoringRow");
    rowWidget.add(recordPanel);

    rowWidget = getRowWidget();
    card.add(rowWidget);
    rowWidget.getElement().setId("contextRow");

    addContext(e, card, rowWidget);

    return card;
  }

/*  @NotNull
  private String getMailTo() {
    String s1 = trimURL(Window.Location.getHref());

    String s = s1 +
        "#" +
        SelectionState.SECTION_SEPARATOR + "search=" + exercise.getID() +
        SelectionState.SECTION_SEPARATOR + "project=" + controller.getProjectStartupInfo().getProjectid();

    String encode = URL.encode(s);
    return "mailto:" +
        "?" +
        "Subject=Share netprof item " + exercise.getEnglish() +
        "&body=Hi,<br/>    Here's a link to " + exercise.getEnglish() + "/" + exercise.getForeignLanguage() + " : " +
        encode + "<br/>Thanks,"+ controller.getUserManager().getCurrent().getFullName();
  }*/
/*

  private String trimURL(String url) {
    return url.split("\\?")[0].split("#")[0];
  }
*/

  private boolean showingComments = false;

  @NotNull
  private Dropdown getDropdown() {
    Dropdown dropdownContainer = new Dropdown("");
    dropdownContainer.setIcon(IconType.REORDER);
    dropdownContainer.setRightDropdown(true);
    dropdownContainer.getMenuWiget().getElement().getStyle().setTop(10, Style.Unit.PCT);

    dropdownContainer.addStyleName("leftThirtyMargin");
    dropdownContainer.getElement().getStyle().setListStyleType(Style.ListStyleType.NONE);
    dropdownContainer.getTriggerWidget().setCaret(false);

    UserListSupport userListSupport = new UserListSupport(controller);
    userListSupport.addListOptions(dropdownContainer, exercise.getID());

    {
      NavLink share = new NavLink(EMAIL);
      dropdownContainer.add(share);
      share.setHref(userListSupport.getMailToExercise(exercise));
    }
    userListSupport.addSendLinkWhatYouSee(dropdownContainer);

    dropdownContainer.add(getShowComments());

    return dropdownContainer;
  }

  @NotNull
  private NavLink getShowComments() {
    NavLink widget = new NavLink("Show Comments");
    widget.addClickHandler(event -> {
      for (CommentBox box : comments) {
        if (showingComments) {
          box.hideButtons();
        } else {
          box.showButtons();
        }
      }
      showingComments = !showingComments;
      if (showingComments) {
        widget.setText("Hide Comments");
      } else {
        widget.setText("Show Comments");
      }
    });
    return widget;
  }


  @NotNull
  private SimpleRecordAudioPanel<T> makeFirstRow(T e, DivWidget rowWidget) {
    SimpleRecordAudioPanel<T> recordPanel = getRecordPanel(e);

    DivWidget flContainer = getHorizDiv();
    flContainer.getElement().setId("flWidget");

    DivWidget recordButtonContainer = new DivWidget();
    recordButtonContainer.add(recordPanel.getPostAudioRecordButton());
    flContainer.add(recordButtonContainer);

    if (hasAudio(e)) {
      flContainer.add(playAudio = getPlayAudioPanel());
    } else {
      // logger.info("makeFirstRow no audio in " + e.getAudioAttributes());
    }

    DivWidget fieldContainer = new DivWidget();
    fieldContainer.setWidth("100%");
    fieldContainer.getElement().setId("leftSideFieldContainer");

    String trim = e.getAltFL().trim();
    if (choices == BOTH || choices == FL || e.getForeignLanguage().trim().equals(trim) || trim.isEmpty()) {
      fieldContainer.add(getFLEntry(e));
    }

    if (choices == BOTH || choices == ALTFL) {
      addField(fieldContainer, addAltFL(e, choices == BOTH));
    }

    addField(fieldContainer, addTransliteration(e));

    boolean meaningValid = isMeaningValid(e);
    boolean isEnglish = controller.getLanguage().equalsIgnoreCase("english");
    boolean useMeaningInsteadOfEnglish = isEnglish && meaningValid;

    if (!useMeaningInsteadOfEnglish && meaningValid) {
      Widget meaningWidget =
          getEntry(e, QCNPFExercise.MEANING,
              e.getMeaning(),
              FieldType.MEANING,
              //false, false, true,
              showInitially,
              new ArrayList<>(), true, annotationHelper, false);
      addField(fieldContainer, meaningWidget);
    }

    flContainer.add(fieldContainer);
    flContainer.setWidth(LEFT_WIDTH);

    rowWidget.add(flContainer);
    return recordPanel;
  }

  enum FieldType {FL, TRANSLIT, MEANING, EN}

  /**
   * @param e
   * @return
   * @see #makeFirstRow
   */
  @NotNull
  private DivWidget getFLEntry(T e) {
    flclickables = new ArrayList<>();

    DivWidget contentWidget = clickableWords.getClickableWords(e.getForeignLanguage(),
        FieldType.FL,
        flclickables, false, true, isRTL);

    flClickableRow = contentWidget;

    DivWidget flEntry = getCommentEntry(FOREIGN_LANGUAGE, e.getAnnotation(FOREIGN_LANGUAGE), false,
        showInitially, annotationHelper, isRTL, contentWidget);

    if (isRTL) {
      clickableWords.setDirection(flEntry);
    } else {
      flEntry.addStyleName("floatLeft");
    }
    flEntry.setWidth("100%");
    return flEntry;
  }

  /**
   * @param e
   * @return
   * @see #makeFirstRow(CommonExercise, DivWidget)
   */
  private boolean hasAudio(T e) {
    return e.hasAudio(true);
  }

  /**
   * @param e
   * @param card
   * @param rowWidget
   * @see #getItemContent(CommonExercise)
   */

  private void addContext(T e, Panel card, DivWidget rowWidget) {
    int c = 0;
    String foreignLanguage = e.getForeignLanguage();//e.getNoAccentFL();
    String altFL = e.getAltFL();
    for (CommonExercise contextEx : e.getDirectlyRelated()) {
      addContextFields(rowWidget, foreignLanguage, altFL, contextEx);

      c++;
      if (c < e.getDirectlyRelated().size()) {
        rowWidget = getRowWidget();
        card.add(rowWidget);
        rowWidget.getElement().setId("contextRow_again");
      }
    }
  }

  private void addContextFields(DivWidget rowWidget,
                                String foreignLanguage,
                                String altFL,
                                CommonExercise contextEx) {
    AnnotationHelper annotationHelper = new AnnotationHelper(controller, contextEx.getID());
    Panel context = getContext(contextEx, foreignLanguage, altFL, annotationHelper);
    if (context != null) {
      rowWidget.add(context);
      context.setWidth("100%");
    }

    String contextTranslation = contextEx.getEnglish();

    boolean same = contextEx.getForeignLanguage().equals(contextTranslation);
    if (!same) {
      if (context != null && !contextTranslation.isEmpty()) {
        context.setWidth(LEFT_WIDTH);
      }

      Widget contextTransWidget = addContextTranslation(contextEx, contextTranslation, annotationHelper);

      if (contextTransWidget != null) {
        contextTransWidget.addStyleName("rightsidecolor");
        contextTransWidget.setWidth(RIGHT_WIDTH);
        rowWidget.add(contextTransWidget);
      }
    }
  }

  private Widget getAltContext(String flToHighlight, String altFL, AnnotationHelper annotationHelper) {
    Panel contentWidget = clickableWords.getClickableWordsHighlight(altFL, flToHighlight,
        FieldType.FL, altContextClickables = new ArrayList<>(), false);

    CommentBox commentBox = getCommentBox(annotationHelper);
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
    //showPopup(itemHeader, commonExerciseUnitChapterItemHelper.getUnitLessonForExercise2(e));
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
    englishWidget.setWidth("90%");
    return englishWidget;
  }

  private void addField(Panel grid, Widget widget) {
    if (widget != null) {
      widget.addStyleName("topFiveMargin");
      grid.add(widget);
    }
  }

/*  private void showPopup(InlineLabel label, String toShow) {
    label.addMouseOverHandler(event -> new BasicDialog().showPopover(
        label,
        null,
        toShow,
        Placement.LEFT));
  }*/

  /**
   * @param e
   * @return
   * @see #getItemContent
   */
  @NotNull
  private SimpleRecordAudioPanel<T> getRecordPanel(T e) {
    //  logger.info("exercise " + e.getID() + " " + e.getScores().size() + " scores");
    return new SimpleRecordAudioPanel<T>(new BusyPanel() {
      @Override
      public boolean isBusy() {
        return false;
      }

      @Override
      public void setBusy(boolean v) {
      }
    }, controller, e, listContainer);
  }

  @NotNull
  private DivWidget getHorizDiv() {
    DivWidget flContainer = new DivWidget();
    flContainer.addStyleName("inlineFlex");
    return flContainer;
  }

  @NotNull
  private DivWidget getRowWidget() {
    DivWidget rowWidget = getHorizDiv();
    rowWidget.addStyleName("bottomFiveMargin");
    rowWidget.addStyleName("floatLeft");
    rowWidget.setWidth("100%");
    return rowWidget;
  }


  private Widget addAltFL(T e, boolean addTopMargin) {
    String altFL = e.getAltFL().trim();
    if (!altFL.isEmpty() && !altFL.equals("N/A") && !e.getForeignLanguage().trim().equals(altFL)) {
      altflClickables = new ArrayList<>();

      DivWidget contentWidget = clickableWords.getClickableWords(altFL,
          FieldType.FL,
          altflClickables, false, phonesChoices == HIDE, isRTL);

      altFLClickableRow = contentWidget;

      DivWidget flEntry = getCommentEntry(QCNPFExercise.ALTFL, e.getAnnotation(QCNPFExercise.ALTFL), false,
          showInitially, annotationHelper, isRTL, contentWidget);

      if (addTopMargin) {
        contentWidget.getElement().getStyle().setMarginTop(5, Style.Unit.PX);
      }
      if (!isRTL) {
        flEntry.addStyleName("floatLeft");
      } else {
        clickableWords.setDirection(flEntry);
      }
      flEntry.setWidth("100%");
      return flEntry;

    } else return null;
  }

  private Widget addTransliteration(T e) {
    String translitSentence = e.getTransliteration();
    if (!translitSentence.isEmpty() && !translitSentence.equals("N/A")) {
      return getEntry(e, QCNPFExercise.TRANSLITERATION, translitSentence, FieldType.TRANSLIT,
          showInitially, new ArrayList<>(), true, annotationHelper, false);
    }
    return null;
  }

  private boolean isMeaningValid(T e) {
    String meaning = e.getMeaning();
    return isValid(meaning);
  }

  private boolean isValid(String meaning) {
    return meaning != null && !meaning.trim().isEmpty() && !meaning.equals("N/A");
  }

  private final List<CommentBox> comments = new ArrayList<>();
  private ChoicePlayAudioPanel contextPlay;

  /**
   * @param contextExercise
   * @return
   * @see #addContextFields(DivWidget, String, String, CommonExercise)
   */
  private Panel getContext(CommonExercise contextExercise,
                           String itemText,
                           String altFL,
                           AnnotationHelper annotationHelper) {
    String context = contextExercise.getForeignLanguage();

    if (!context.isEmpty()) {
      Panel hp = new DivWidget();
      hp.addStyleName("inlineFlex");
      hp.getElement().setId("contentContainer");

      hp.add(getSpacer());
      hp.add(getContextPlay(contextExercise));

      DivWidget contentWidget = clickableWords.getClickableWordsHighlight(context, itemText,
          FieldType.FL, contextClickables = new ArrayList<>(), false);

      contextClickableRow = contentWidget;

      CommentBox commentBox = getCommentBox(annotationHelper);
      ExerciseAnnotation annotation = contextExercise.getAnnotation(FOREIGN_LANGUAGE);

/*      logger.info("context '" + context1 +
          "' = '" + annotation +
          "'");*/

      Widget commentRow =
          commentBox
              .getEntry(FOREIGN_LANGUAGE, contentWidget,
                  annotation, showInitially, isRTL);

      commentRow.setWidth("100%");

      DivWidget col = new DivWidget();
      col.setWidth("100%");
      hp.add(col);

      String altFL1 = contextExercise.getAltFL();
      if (choices == BOTH || choices == FL || altFL1.isEmpty()) {
        col.add(commentRow);
      }

      if (choices == BOTH || choices == ALTFL) {
        if (!altFL1.isEmpty() && !context.equals(altFL1)) {
          Widget altContext = getAltContext(altFL, altFL1, annotationHelper);
          if (choices == BOTH) altContext.addStyleName("topFiveMargin");
          col.add(altContext);
        }
      }

      return hp;
    } else {
      return null;
    }
  }

  @NotNull
  private DivWidget getSpacer() {
    DivWidget spacer = new DivWidget();
    spacer.getElement().setId("spacer");
    spacer.getElement().getStyle().setProperty("minWidth", CONTEXT_INDENT + "px");
    return spacer;
  }

  private Widget getContextPlay(CommonExercise contextExercise) {
    contextPlay
        = new ChoicePlayAudioPanel(controller.getSoundManager(), contextExercise, controller, true, this::contextAudioChanged);
    AudioAttribute audioAttrPrefGender = contextExercise.getAudioAttrPrefGender(controller.getUserManager().isMale());
    contextPlay.setEnabled(audioAttrPrefGender != null);

    return contextPlay;
  }

  private void contextAudioChanged(int id, long duration) {
    if (phonesChoices == SHOW) {
      AlignmentOutput alignmentOutput = alignments.get(id);
      if (alignmentOutput != null) {
        if (DEBUG) {
          logger.info("contextAudioChanged audioChanged for ex " + exercise.getID() + " CONTEXT audio id " + id +
              " alignment " + alignmentOutput);
        }
        if (contextClickables == null) {
          logger.warning("huh? context not set for " + id);
        } else {
          matchSegmentsToClickables(id, duration, alignmentOutput, contextClickables, contextPlay, contextClickableRow);
        }
      }
    }
  }

  private Widget addContextTranslation(AnnotationExercise e, String contextTranslation,
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
   * @paramx label
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
                             AnnotationHelper annotationHelper,
                             boolean isRTL) {
    return getEntry(field, value, e.getAnnotation(field), fieldType, showInitially, clickables, addRightMargin,
        annotationHelper, isRTL);
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
   * @return
   * @paramx label
   * @seex #makeFastAndSlowAudio(String)
   * @see #getEntry
   */
  private DivWidget getEntry(final String field,
                             String value,
                             ExerciseAnnotation annotation,
                             FieldType fieldType,
                             boolean showInitially,
                             List<IHighlightSegment> clickables,
                             boolean addRightMargin,
                             AnnotationHelper annotationHelper,
                             boolean isRTL) {
    DivWidget contentWidget = clickableWords.getClickableWords(value, fieldType, clickables,
        fieldType != FieldType.FL, addRightMargin, isRTL);
    // logger.info("value " + value + " translit " + isTranslit + " is fl " + isFL);
    return getCommentEntry(field, annotation, fieldType == FieldType.TRANSLIT, showInitially,
        annotationHelper, isRTL, contentWidget);
  }

  private DivWidget getCommentEntry(String field,
                                    ExerciseAnnotation annotation,
                                    boolean isTranslit,
                                    boolean showInitially,
                                    AnnotationHelper annotationHelper,
                                    boolean isRTL,
                                    DivWidget contentWidget) {
    if (isTranslit && isRTL) {
      // logger.info("- float right value " + value + " translit " + isTranslit + " is fl " + isFL);
      contentWidget.addStyleName("floatRight");
    }
    return getCommentBox(annotationHelper).getEntry(field, contentWidget, annotation, showInitially, isRTL);
  }

  /**
   * @return
   * @seex x#getEntry(String, String, String, ExerciseAnnotation)
   * @seex #makeFastAndSlowAudio(String)
   */
  private CommentBox getCommentBox(AnnotationHelper annotationHelper) {
    if (logger == null) {
      logger = Logger.getLogger("CommentNPFExercise");
    }
    T exercise = this.exercise;
    CommentBox commentBox =
        new CommentBox(this.exercise.getID(), controller,
            annotationHelper, exercise.getMutableAnnotation(), true);
    comments.add(commentBox);
    return commentBox;
  }
}
