package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
import mitll.langtest.shared.flashcard.CorrectAndScore;
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
import static mitll.langtest.client.scoring.ShowChoices.ALTFL;
import static mitll.langtest.client.scoring.ShowChoices.BOTH;
import static mitll.langtest.client.scoring.ShowChoices.FL;

/**
 * Created by go22670 on 3/23/17.
 */
public class TwoColumnExercisePanel<T extends CommonExercise> extends DivWidget implements AudioChangeListener,
    RefAudioGetter {
  public static final String HALF_WIDTH = "50%";
  private Logger logger = Logger.getLogger("TwoColumnExercisePanel");

  private static final String EMAIL = "Email Item";
  private static final Set<String> toIgnore = new HashSet<>(Arrays.asList("sil", "SIL", "<s>", "</s>"));

  static final int CONTEXT_INDENT = 56;

  private final List<CorrectAndScore> correctAndScores;
  private final T exercise;
  private final ExerciseController controller;

  private final AnnotationHelper annotationHelper;
  private final ClickableWords<T> clickableWords;
  private final boolean showInitially = false;
  private final UnitChapterItemHelper<CommonExercise> commonExerciseUnitChapterItemHelper;
  private final ListInterface<CommonShell, T> listContainer;
  private ChoicePlayAudioPanel playAudio;

  private Map<Integer, AlignmentOutput> alignments;

  private Map<Integer, Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>>> idToTypeToSegmentToWidget = new HashMap<>();

  private List<IHighlightSegment> altflClickables;
  /**
   * @see #getFLEntry
   */
  private List<IHighlightSegment> flclickables;
  private List<IHighlightSegment> contextClickables, altContextClickables;
  private ShowChoices choices;
  private PhonesChoices phonesChoices;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_MATCH = false;
  private boolean isRTL = false;

  /**
   * Has a left side -- the question content (Instructions and audio panel (play button, waveform)) <br></br>
   * and a right side --
   *
   * @param commonExercise for this exercise
   * @param controller
   * @param listContainer
   * @param phonesChoices
   * @param alignments
   * @paramx screenPortion
   * @paramx instance
   * @paramx allowRecording
   * @paramx includeListButtons
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel
   * @see mitll.langtest.client.banner.NewLearnHelper#getFactory
   */
  public TwoColumnExercisePanel(final T commonExercise,
                                final ExerciseController controller,
                                final ListInterface<CommonShell, T> listContainer,
                                List<CorrectAndScore> correctAndScores,
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
    this.alignments = alignments;

/*
    if (!commonExercise.getFieldToAnnotation().isEmpty()) {
      logger.info("Annos " + commonExercise.getFieldToAnnotation());
    }
    */

    annotationHelper = new AnnotationHelper(controller, commonExercise.getID());
    clickableWords = new ClickableWords<T>(listContainer, commonExercise, controller.getLanguage());
    this.isRTL = clickableWords.isRTL(exercise);
    this.correctAndScores = correctAndScores;
    commonExerciseUnitChapterItemHelper = new UnitChapterItemHelper<>(controller.getTypeOrder());
    add(getItemContent(commonExercise));

    addMouseOverHandler(event -> getRefAudio(() -> {
    }));
/*    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        getRefAudio(controller);
      }
    });*/
  }

  @Override
  public void getRefAudio(RefAudioListener listener) {
    if (playAudio != null && playAudio.getCurrentAudioAttr() != null) {
      AudioAttribute currentAudioAttr = playAudio.getCurrentAudioAttr();
      int refID = currentAudioAttr.getUniqueID();
      AudioAttribute currentAudioAttr1 = contextPlay != null ? contextPlay.getCurrentAudioAttr() : null;
      int contextRefID = currentAudioAttr1 != null ? currentAudioAttr1.getUniqueID() : -1;

      //  logger.info("getRefAudio asking for " + refID);
//    logger.info("asking for " + contextRefID);

      Set<Integer> req = new HashSet<>();
      if (refID != -1) {
        if (!alignments.containsKey(refID))
          req.add(refID);
      }

      if (contextRefID != -1) {
        // logger.info("getRefAudio asking for context " + contextRefID);
        if (!alignments.containsKey(contextRefID))
          req.add(contextRefID);
      }

      if (req.isEmpty()) {
        registerSegments(refID, currentAudioAttr, contextRefID, currentAudioAttr1);
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
                registerSegments(refID, currentAudioAttr, contextRefID, currentAudioAttr1);
                cacheOthers(listener);
              }
            });
      }
    }
  }

  private void registerSegments(int refID,
                                AudioAttribute currentAudioAttr,
                                int contextRefID,
                                AudioAttribute currentAudioAttr1) {
    if (refID != -1) {
      audioChanged(refID, currentAudioAttr.getDurationInMillis());
    }
    if (contextRefID != -1) {
      contextAudioChanged(contextRefID, currentAudioAttr1.getDurationInMillis());
    }
  }

  private void cacheOthers(RefAudioListener listener) {
    Set<Integer> req = new HashSet<>(playAudio.getAllAudioIDs());

    if (contextPlay != null) {
      req.addAll(contextPlay.getAllAudioIDs());
    }

    req.removeAll(alignments.keySet());

    if (!req.isEmpty()) {
      //   logger.info("cacheOthers Asking for audio alignments for " + req + " knownAlignments " + knownAlignments.size());
      ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
      if (projectStartupInfo != null) {
        controller.getScoringService().getAlignments(projectStartupInfo.getProjectid(),
            req, new AsyncCallback<Map<Integer, AlignmentOutput>>() {
              @Override
              public void onFailure(Throwable caught) {

              }

              @Override
              public void onSuccess(Map<Integer, AlignmentOutput> result) {
                //           logger.info("cacheOthers before knownAlignments " + alignments.keySet().size());
                alignments.putAll(result);
                //         logger.info("cacheOthers after knownAlignments " + alignments.keySet().size());
                listener.refAudioComplete();
              }
            });
      }
    }
    else {
      listener.refAudioComplete();
    }
  }

  /**
   * @param id
   * @param duration
   * @see ChoicePlayAudioPanel#addChoices
   */
  @Override
  public void audioChanged(int id, long duration) {
    AlignmentOutput alignmentOutput = alignments.get(id);
    if (alignmentOutput != null) {
      if (DEBUG) logger.info("audioChanged for ex " + exercise.getID() + " audio id " + id);
      matchSegmentsToClickables(id, duration, alignmentOutput, idToTypeToSegmentToWidget.get(id), this.flclickables, this.playAudio);
    }
  }

  /**
   * @param id
   * @param duration
   * @param alignmentOutput
   * @param typeToSegmentToWidget
   * @param flclickables
   * @param playAudio
   * @see #contextAudioChanged
   */
  private void matchSegmentsToClickables(int id,
                                         long duration,
                                         AlignmentOutput alignmentOutput,
                                         Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget,
                                         List<IHighlightSegment> flclickables,
                                         ChoicePlayAudioPanel playAudio) {
    if (typeToSegmentToWidget == null) {
      Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> netPronImageTypeTreeMapMap =
          matchSegmentToWidgetForAudio(id, duration, alignmentOutput, flclickables, playAudio);
      idToTypeToSegmentToWidget.put(id, netPronImageTypeTreeMapMap);
      typeToSegmentToWidget = netPronImageTypeTreeMapMap;
    }

    if (DEBUG) {
      logger.info("audioChanged for ex " + exercise.getID() +
          " audio id " + id + " : " +
          (typeToSegmentToWidget == null ? "missing" : typeToSegmentToWidget.size()));
    }

    if (typeToSegmentToWidget == null) {
      logger.warning("audioChanged no type to segment for " + id + " and exercise " + exercise.getID());
    } else {
      // if (DEBUG) logger.info("audioChanged segments now " + typeToSegmentToWidget.keySet());
      playAudio.setListener(new SegmentHighlightAudioControl(typeToSegmentToWidget));
    }
  }

  /**
   * TODOx : what to do about chinese?
   *
   * @param audioID
   * @param alignmentOutput
   * @see #matchSegmentsToClickables(int, long, AlignmentOutput, Map, List, ChoicePlayAudioPanel)
   */
  private Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> matchSegmentToWidgetForAudio(Integer audioID,
                                                                                                            long durationInMillis,
                                                                                                            AlignmentOutput alignmentOutput,
                                                                                                            List<IHighlightSegment> flclickables,
                                                                                                            AudioControl audioControl) {
    Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> value = new HashMap<>();

    TreeMap<TranscriptSegment, IHighlightSegment> segmentToWidget = new TreeMap<>();
    value.put(NetPronImageType.WORD_TRANSCRIPT, segmentToWidget);

    TreeMap<TranscriptSegment, IHighlightSegment> phoneMap = new TreeMap<>();
    value.put(NetPronImageType.PHONE_TRANSCRIPT, phoneMap);

    if (alignmentOutput == null) {
      logger.warning("matchSegmentToWidgetForAudio no alignment for " + audioID);
      segmentToWidget.put(new TranscriptSegment(0, (float) durationInMillis, "all", 0),
          new AllHighlight(flclickables));
    } else {
      Iterator<IHighlightSegment> iterator = flclickables.iterator();
      if (DEBUG) logger.info("matchSegmentToWidgetForAudio " + audioID + " got clickables " + flclickables.size());

      List<TranscriptSegment> wordSegments = alignmentOutput.getTypeToSegments().get(NetPronImageType.WORD_TRANSCRIPT);

      if (wordSegments == null) {
        wordSegments = Collections.emptyList();
      } else {
        wordSegments = wordSegments.stream().filter(seg -> !shouldIgnore(seg)).collect(Collectors.toList());
      }

      List<TranscriptSegment> phones = alignmentOutput.getTypeToSegments().get(NetPronImageType.PHONE_TRANSCRIPT);
      if (wordSegments == null) {
        if (DEBUG) logger.info("matchSegmentToWidgetForAudio no word segments in " + alignmentOutput);
      } else {
        if (transcriptMatches2(flclickables, wordSegments)) {

         // logger.info("these should match ---- " + wordSegments);

          // List<IHighlightSegment> compOrder = flclickables;
          if (isRTL) {
            List<IHighlightSegment> compOrder = new ArrayList<>(flclickables);
//            logger.info("transcriptMatches2 did reverse, so before  " + compOrder);
            Collections.reverse(compOrder);
            iterator = compOrder.iterator();
  //          logger.info("transcriptMatches2 did reverse, so after  " + compOrder);
          }

          for (TranscriptSegment word : wordSegments) {
            // if (!shouldIgnore(word)) {
            if (iterator.hasNext()) {
              int segmentLength = word.getEvent().length();
              List<TranscriptSegment> phonesInWord = getSegs(phones, word);
              if (isRTL) { // phones should play right to left
                Collections.reverse(phonesInWord);
              }

              if (DEBUG) logger.info("matchSegmentToWidgetForAudio got segment " + word + " length " + segmentLength);
              IHighlightSegment value1 =
                  matchEventSegmentToClickable(iterator, word, phonesInWord, audioControl, phoneMap);

              if (value1 == null) {
                logger.warning("can't find match for word " + word);
              } else {
                segmentToWidget.put(word, value1);
              }
            } else {
              logger.warning("matchSegmentToWidgetForAudio no match for " + word);
            }
          }
          // }
        } else {
          if (DEBUG_MATCH) logger.warning("no match for" +
              "\n\tsegments " + wordSegments +
              "\n\tto       " + flclickables);
        }
      }
    }
    if (DEBUG_MATCH) logger.info("matchSegmentToWidgetForAudio value is " + value);
    return value;
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

  private IHighlightSegment matchEventSegmentToClickable(Iterator<IHighlightSegment> clickables,
                                                         TranscriptSegment wordSegment,
                                                         List<TranscriptSegment> phonesInWord,

                                                         AudioControl audioControl,
                                                         TreeMap<TranscriptSegment, IHighlightSegment> phoneMap) {
    IHighlightSegment clickable = clickables.next();
    clickable = skipUnclickable(clickables, clickable);
    String segment = wordSegment.getEvent();

    //    String content = clickable.getContent();
    //boolean isMatch = content.equalsIgnoreCase(segment);
    if (DEBUG)
      logger.info("matchSegmentToWidgetForAudio compare :" +
          "\n\tsegment " + segment + //" length " + segmentLength +
          "\n\tvs      " + clickable);

    String lcSegment = removePunct(segment.toLowerCase());
    String fragment1 = removePunct(clickable.getContent().toLowerCase());

    if (lcSegment.equalsIgnoreCase(fragment1)) {
      if (phonesChoices == PhonesChoices.SHOW) {
        clickable.setSouth(new WordTable().getPhoneDivBelowWord(audioControl, phoneMap, phonesInWord, true, wordSegment, isRTL));
      }
      return clickable;
    } else {
      Collection<IHighlightSegment> bulk = getMatchingSegments(clickables, clickable, segment, lcSegment);

      if (bulk.isEmpty()) {
        return null;
      } else {
        AllHighlight allHighlight = new AllHighlight(bulk);

        allHighlight.setSouth(new WordTable().getPhoneDivBelowWord(audioControl, phoneMap, phonesInWord, true, wordSegment, isRTL));

        if (DEBUG)
          logger.info("matchSegmentToWidgetForAudio create composite from " + bulk.size() + " = " + allHighlight);
        return allHighlight;
      }
    }
  }

  @NotNull
  private Collection<IHighlightSegment> getMatchingSegments(Iterator<IHighlightSegment> clickables, IHighlightSegment clickable, String segment, String lcSegment) {
    Collection<IHighlightSegment> bulk = new ArrayList<>();

    if (DEBUG) logger.info("\tmatchSegmentToWidgetForAudio (2) compare : segment " + lcSegment +
        " vs " + clickable);

    while (!lcSegment.isEmpty()) {
      String fragment = clickable.getContent().toLowerCase();

      if (DEBUG) logger.info("\tmatchSegmentToWidgetForAudio compare : segment " + lcSegment +
          " vs fragment " + fragment);

      boolean segmentHasFragment = lcSegment.startsWith(fragment);
      if (segmentHasFragment) {
        bulk.add(clickable);
        lcSegment = lcSegment.substring(fragment.length());
        if (DEBUG) logger.info("\tmatchSegmentToWidgetForAudiosegment now " + lcSegment);

        if (!clickables.hasNext() || lcSegment.isEmpty()) {
          break;
        }
        clickable = clickables.next();
        clickable = skipUnclickable(clickables, clickable);
      } else {
        if (DEBUG) logger.info("\tmatchSegmentToWidgetForAudio compare : segment '" + lcSegment +
            "' vs fragment '" + fragment + "'");
        break;
      }
    }

    if (!lcSegment.isEmpty()) {
      logger.warning("matchSegmentToWidgetForAudio couldn't match all of segment " + segment + " this left over = " + lcSegment);
    }
    return bulk;
  }

  private boolean transcriptMatches(List<IHighlightSegment> clickables,
                                    List<TranscriptSegment> segments) {
    if (DEBUG_MATCH) logger.info("Check   " + clickables);
    if (DEBUG_MATCH) logger.info("Against " + segments);

    Iterator<TranscriptSegment> iterator = segments.iterator();
    boolean allMatch = true;

    TranscriptSegment word = null;

    if (iterator.hasNext()) {
      word = iterator.next();
    }

    if (word == null) return false;

    String lcSegment = getWordEvent(word);


    for (IHighlightSegment clickable : clickables) {
      if (DEBUG_MATCH) logger.info("Clickable " + clickable);
      if (DEBUG_MATCH) logger.info("Word      " + lcSegment);

      if (clickable.isClickable()) {
        String fragment = removePunct(clickable.getContent().toLowerCase());

        if (lcSegment.equalsIgnoreCase(fragment)) {
          if (iterator.hasNext()) {
            word = iterator.next();
            lcSegment = getWordEvent(word);
            if (DEBUG_MATCH) logger.info("now Clickable " + clickable);
            if (DEBUG_MATCH) logger.info("now Word      " + lcSegment);
          } else break;
        } else {
          if (DEBUG_MATCH) logger.info("\tmatchSegmentToWidgetForAudio compare : segment " + lcSegment +
              " vs fragment " + fragment);

          boolean segmentHasFragment = lcSegment.startsWith(fragment);
          if (segmentHasFragment) {
            lcSegment = lcSegment.substring(fragment.length());
            if (DEBUG_MATCH) logger.info("\tmatchSegmentToWidgetForAudiosegment now " + lcSegment);

            if (lcSegment.isEmpty()) {
              if (iterator.hasNext()) {
                word = iterator.next();
                lcSegment = getWordEvent(word);
              }
            }
          } else {
            if (DEBUG_MATCH) logger.info("\tmatchSegmentToWidgetForAudio compare : segment '" + lcSegment +
                "' vs fragment '" + fragment + "'");
            allMatch = false;
            break;
          }
        }
      }
    }
    return allMatch;
  }

  private boolean transcriptMatches2(List<IHighlightSegment> clickables,
                                     List<TranscriptSegment> segments) {
    StringBuilder clickableSentence = new StringBuilder();

    int i = 0;
    List<IHighlightSegment> compOrder = clickables;
    if (isRTL) {
      compOrder = new ArrayList<>(clickables);
      Collections.reverse(compOrder);
    }
    for (IHighlightSegment clickable : compOrder) {
      if (clickable.isClickable()) {
        String fragment = removePunct(clickable.getContent().toLowerCase());
        clickableSentence.append(fragment);
       // logger.info("clickable seg " + (i++) + " " + fragment);
      }
    }

    StringBuilder segSentence = new StringBuilder();
    for (TranscriptSegment segment : segments) {
      segSentence.append(removePunct(segment.getEvent().toLowerCase()));
    }

    String cl = clickableSentence.toString();
    String ss = segSentence.toString();

    if (DEBUG_MATCH) logger.info("Clickable " + cl);
    if (DEBUG_MATCH) logger.info("Segments  " + ss);
    return cl.equals(ss);
  }

  private String getWordEvent(TranscriptSegment word) {
    return removePunct(word.getEvent().toLowerCase());
  }

  protected String removePunct(String t) {
    return t
        .replaceAll(GoodwaveExercisePanel.PUNCT_REGEX, "")
        .replaceAll("[\\uFF01-\\uFF0F\\uFF1A-\\uFF1F\\u3002\\u003F\\u00BF\\u002E\\u002C\\u0021\\u20260\\u005C\\u2013]", "");
  }

/*  private TranscriptSegment skipSils(Iterator<TranscriptSegment> iterator, TranscriptSegment word) {
    TranscriptSegment val = word;
    while (shouldIgnore(word) && iterator.hasNext()) {
      logger.warning("skipSils before " + val);
      val = iterator.next();
      logger.warning("skipSils now    " + val);
    }
    return val;
  }*/

  @NotNull
  private IHighlightSegment skipUnclickable(Iterator<IHighlightSegment> iterator, IHighlightSegment clickable) {
    while (!clickable.isClickable() && iterator.hasNext()) {
      logger.info("skipUnclickable : skip " + clickable);
      clickable = iterator.next();
    }
    return clickable;
  }

  private boolean shouldIgnore(TranscriptSegment seg) {
    boolean contains = toIgnore.contains(seg.getEvent());
    // if (contains) logger.warning("match " + seg);
    return contains;
  }

  private HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
    return addDomHandler(handler, MouseOverEvent.getType());
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
      lr.setWidth(HALF_WIDTH);

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

  @NotNull
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
        "&body=Link to " + exercise.getEnglish() + "/" + exercise.getForeignLanguage() + " : " +
        encode;
  }

  private String trimURL(String url) {
    return url.split("\\?")[0].split("#")[0];
  }

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

    new UserListSupport(controller).addListOptions(dropdownContainer, exercise.getID());

    {
      NavLink share = new NavLink(EMAIL);
      dropdownContainer.add(share);
      share.setHref(getMailTo());
    }

    dropdownContainer.add(getShowComments());

    return dropdownContainer;
  }

  @NotNull
  private NavLink getShowComments() {
    NavLink widget = new NavLink("Show Comments");
    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
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
    }

    DivWidget fieldContainer = new DivWidget();
    fieldContainer.setWidth("100%");
    fieldContainer.getElement().setId("leftSideFieldContainer");
//    if (isRTL) {
//      clickableWords.setDirection(fieldContainer);
//    }

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
          getEntry(e, QCNPFExercise.MEANING, e.getMeaning(), false, false, true, showInitially,
              new ArrayList<>(), true, annotationHelper);
      addField(fieldContainer, meaningWidget);
    }

    flContainer.add(fieldContainer);
    flContainer.setWidth(HALF_WIDTH);

    rowWidget.add(flContainer);
    return recordPanel;
  }

  @NotNull
  private DivWidget getFLEntry(T e) {
    flclickables = new ArrayList<>();
    DivWidget flEntry =
        getEntry(e,
            FOREIGN_LANGUAGE,
            e.getForeignLanguage(),
            true, false, false,
            showInitially,
            flclickables,
            true, annotationHelper);
    if (!isRTL) {
      flEntry.addStyleName("floatLeft");
    }
    else {
      clickableWords.setDirection(flEntry);
    }
    flEntry.setWidth("100%");
    return flEntry;
  }

  private boolean hasAudio(T e) {
    return e.getAudioAttributePrefGender(controller.getUserManager().isMale(), true) != null;
  }

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
        context.setWidth(HALF_WIDTH);
      }

      Widget contextTransWidget = addContextTranslation(contextEx, contextTranslation, annotationHelper);

      if (contextTransWidget != null) {
        contextTransWidget.addStyleName("rightsidecolor");
        contextTransWidget.setWidth(HALF_WIDTH);
        rowWidget.add(contextTransWidget);
      }
    }
  }

  private Widget getAltContext(String flToHighlight, String altFL, AnnotationHelper annotationHelper) {
    Panel contentWidget = clickableWords.getClickableWordsHighlight(altFL, flToHighlight,
        true, false, false, altContextClickables = new ArrayList<>(), false);

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
    InlineLabel itemHeader = commonExerciseUnitChapterItemHelper.getLabel(e);
    showPopup(itemHeader, commonExerciseUnitChapterItemHelper.getUnitLessonForExercise2(e));
    itemHeader.addStyleName("floatRight");
    DivWidget itemContainer = new DivWidget();
    itemContainer.add(itemHeader);
    itemContainer.addStyleName("floatRight");
    return itemContainer;
  }

  @NotNull
  private Widget getEnglishWidget(T e, String english) {
    Widget englishWidget = getEntry(e, QCNPFExercise.ENGLISH, english, false, false, false,
        showInitially, new ArrayList<>(), true, annotationHelper);
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

  private void showPopup(InlineLabel label, String toShow) {
    label.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        new BasicDialog().showPopover(
            label,
            null,
            toShow,
            Placement.LEFT);
      }
    });
  }

  /**
   * @param e
   * @return
   * @see #getItemContent
   */
  @NotNull
  private SimpleRecordAudioPanel<T> getRecordPanel(T e) {
    return new SimpleRecordAudioPanel<T>(new BusyPanel() {
      @Override
      public boolean isBusy() {
        return false;
      }

      @Override
      public void setBusy(boolean v) {
      }
    }, controller, e, correctAndScores, listContainer);
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
    String translitSentence = e.getAltFL().trim();
    if (!translitSentence.isEmpty() && !translitSentence.equals("N/A") && !e.getForeignLanguage().trim().equals(translitSentence)) {
      altflClickables = new ArrayList<>();
      Widget entry = getEntry(e, QCNPFExercise.ALTFL, translitSentence, true, true, false,
          showInitially, altflClickables, phonesChoices == HIDE, annotationHelper);
      if (addTopMargin) entry.getElement().getStyle().setMarginTop(10, Style.Unit.PX);
      return entry;
    } else return null;
  }

  private Widget addTransliteration(T e) {
    String translitSentence = e.getTransliteration();
    if (!translitSentence.isEmpty() && !translitSentence.equals("N/A")) {
      DivWidget entry = getEntry(e, QCNPFExercise.TRANSLITERATION, translitSentence, false, true, false,
          showInitially, new ArrayList<>(), true, annotationHelper);
      return entry;
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
  private Panel getContext(CommonExercise contextExercise, String itemText, String altFL,
                           AnnotationHelper annotationHelper) {
    String context = contextExercise.getForeignLanguage();

    if (!context.isEmpty()) {
      Panel hp = new DivWidget();
      hp.addStyleName("inlineFlex");
      hp.getElement().setId("contentContainer");

      hp.add(getSpacer());
      hp.add(getContextPlay(contextExercise));

      // String noAccentFL = contextExercise.getNoAccentFL();
      Panel contentWidget = clickableWords.getClickableWordsHighlight(context, itemText,
          true, false, false, contextClickables = new ArrayList<>(), false);

      CommentBox commentBox = getCommentBox(annotationHelper);
      String context1 = FOREIGN_LANGUAGE;//QCNPFExercise.CONTEXT;
      ExerciseAnnotation annotation = contextExercise.getAnnotation(context1);

/*      logger.info("context '" + context1 +
          "' = '" + annotation +
          "'");*/

      Widget commentRow =
          commentBox
              .getEntry(context1, contentWidget,
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
          col.add(getAltContext(altFL, altFL1, annotationHelper));
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
    AlignmentOutput alignmentOutput = alignments.get(id);
    if (alignmentOutput != null) {
      if (DEBUG) {
        logger.info("contextAudioChanged audioChanged for ex " + exercise.getID() + " CONTEXT audio id " + id +
            " alignment " + alignmentOutput);
      }
      if (contextClickables == null) {
        logger.warning("huh? context not set for " + id);
      } else {
        matchSegmentsToClickables(id, duration, alignmentOutput, idToTypeToSegmentToWidget.get(id), contextClickables, contextPlay);
      }
    }
  }

  private Widget addContextTranslation(AnnotationExercise e, String contextTranslation,
                                       AnnotationHelper annotationHelper) {
    if (!contextTranslation.isEmpty()) {
      return getEntry(e,
          ENGLISH,
          contextTranslation,
          false, false, false, showInitially, new ArrayList<>(), true,
          annotationHelper);
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
                             boolean isFL,
                             boolean isTranslit,
                             boolean isMeaning,
                             boolean showInitially,
                             List<IHighlightSegment> clickables,
                             boolean addRightMargin,
                             AnnotationHelper annotationHelper) {
    ExerciseAnnotation annotation = e.getAnnotation(field);
/*    logger.info("anno for '" + field +
        "' = '" + annotation +
        "'");*/
/*    if (annotation == null) {
      boolean contains = e.getFields().contains(field);
  //    logger.info("For " + field + " " + e.getFields() + " " + contains);
    }*/

    return getEntry(field, value, annotation, isFL, isTranslit, isMeaning, showInitially, clickables, addRightMargin,
        annotationHelper);
  }

/*  private DivWidget getEntrySegments(AnnotationExercise e,
                                     final String field,
                                     List<IHighlightSegment> segments,
                                     boolean addRightMargin) {
    return getEntryFromSegments(field, e.getAnnotation(field), segments, addRightMargin, isRTL);
  }*/

  /**
   * @param field
   * @param value
   * @param annotation
   * @param showInitially
   * @param clickables
   * @param addRightMargin
   * @param annotationHelper
   * @return
   * @paramx label
   * @seex #makeFastAndSlowAudio(String)
   * @see #getEntry
   */
  private DivWidget getEntry(final String field,
                             String value,
                             ExerciseAnnotation annotation,
                             boolean isFL,
                             boolean isTranslit,
                             boolean isMeaning,
                             boolean showInitially,
                             List<IHighlightSegment> clickables,
                             boolean addRightMargin,
                             AnnotationHelper annotationHelper) {
    DivWidget contentWidget = clickableWords.getClickableWords(value, isFL, isTranslit, isMeaning, clickables, !isFL, addRightMargin);
    return getCommentBox(annotationHelper).getEntry(field, contentWidget, annotation, showInitially, isRTL);
  }

  private DivWidget getEntryFromSegments(final String field,
                                         ExerciseAnnotation annotation,
                                         List<IHighlightSegment> segments,
                                         boolean addRightMargin, boolean isRTL) {
    DivWidget contentWidget = clickableWords.getClickableDivFromSegments(segments, addRightMargin, isRTL);
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
