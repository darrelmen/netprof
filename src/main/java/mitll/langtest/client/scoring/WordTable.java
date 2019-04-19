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
import com.github.gwtbootstrap.client.ui.incubator.Table;
import com.github.gwtbootstrap.client.ui.incubator.TableHeader;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.analysis.PhoneExampleContainer;
import mitll.langtest.client.analysis.WordContainerAsync;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.gauge.SimpleColumnChart;
import mitll.langtest.client.sound.AudioControl;
import mitll.langtest.client.sound.HighlightSegment;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.client.sound.SimpleHighlightSegment;
import mitll.langtest.client.table.PagerTable;
import mitll.langtest.shared.instrumentation.SlimSegment;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WordTable {
  private final Logger logger = Logger.getLogger("WordTable");

  private static final String WHITE_SPACE_NOWRAP = PagerTable.WHITE_SPACE_NOWRAP;

  private static final String TABLE = "<table>";
  private static final String TABLEEND = "</table>";

  private static final String UNKNOWNMODEL = "UNKNOWNMODEL";
  private static final String TEXT_ALIGN_CENTER = "text-align:center;";
  private static final String COLORED_SPAN_STYLE = "padding:3px; " +
      "margin-left:3px; " +
      TEXT_ALIGN_CENTER + " " +
      "font-family:sans-serif; " +
      WHITE_SPACE_NOWRAP + " ";
  private static final String FONT_SIZE = "font-size:14pt;";
  private static final String BACKGROUND_COLOR = "background-color";
  private static final String BLUE = "#2196F3";
  /**
   * Fix for japanese word wrap issue.
   */
  private static final String HEADER = "<th style='" + TEXT_ALIGN_CENTER + PagerTable.WHITE_SPACE_NOWRAP + BACKGROUND_COLOR + ":";

  private static final int PHONE_WIDTH = 25;

  private static final String TR = "tr";
  private static final String TD = "td";
  private static final String LOW_SCORE = "Low score";
  private static final String CLICK_TO_HEAR_WORD = "Click to hear.";
  private static final String SIL = "sil";
  private static final String THEAD = "<thead>";
  private static final String NBSP = "&nbsp;";
  private static final boolean DEBUG = false;

  /**
   * @param netPronImageTypeToEndTime
   * @param filter
   * @param first
   * @return
   * @see PhoneExampleContainer#getItemColumn
   */
  public String toHTML(Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeToEndTime, String filter, String bigram, boolean first) {
    Map<TranscriptSegment, List<TranscriptSegment>> wordToPhones = getWordToPhones(netPronImageTypeToEndTime);
    StringBuilder builder = new StringBuilder();

    builder.append(TABLE);

    {
      builder.append(THEAD);
      addWordColHeaders(builder, wordToPhones.keySet());
      builder.append("</thead>");
    }

    {
      builder.append("<tr>");

      //    logger.info("show " + filter + " and " + bigram + " phones " + wordToPhones.values());

      for (List<TranscriptSegment> phones : wordToPhones.values()) {
        builder.append("<td>");
        builder.append(TABLE);
        builder.append(THEAD);

        addPhones(filter, bigram, builder, phones, first);

        builder.append("</thead>");
        builder.append(TABLEEND);
        builder.append("</td>");
      }

      builder.append("</tr>");
    }

    builder.append(TABLEEND);
    return builder.toString();
  }

  private void addWordColHeaders(StringBuilder builder, Collection<? extends SlimSegment> transcriptSegments) {
    transcriptSegments.forEach(word -> {
      // float score = word.getScore();
      float score = word.getScore();
      String color = getColor(score);
      //   logger.warning("addWordColHeaders : word " + word.getEvent() + " score " + score + " = " + color);
      builder.append(HEADER)
          .append(color)
          .append(";")
          .append(getForeground(score))
          .append("'>");
      builder.append(word.getEvent());
      builder.append("</th>");
    });
  }

  /**
   * Only color one phone.
   *
   * if prev phone matches and next phone matches
   *
   * @param filter
   * @param contextPhone
   * @param builder
   * @param first
   */
  private void addPhones(String filter, String contextPhone, StringBuilder builder, List<TranscriptSegment> phones, boolean first) {
    Set<TranscriptSegment> toColor = new HashSet<>();
    Set<TranscriptSegment> toMark = new HashSet<>();

    boolean isUnder = contextPhone.equalsIgnoreCase("_");

    for (int i = 0; i < phones.size(); i++) {
      TranscriptSegment prev = phones.get(i);
      TranscriptSegment next = i + 1 < phones.size() ? phones.get(i + 1) : new TranscriptSegment();

      String prevCandidate = prev.getEvent();
      String nextCandidate = next.getEvent();

      // logger.info("addPhones at " + i + " prev " + prevCandidate + " next " + nextCandidate);

      if (!first && (prevCandidate.equalsIgnoreCase(contextPhone)) &&
          nextCandidate.equalsIgnoreCase(filter)
      ) {
        toMark.add(prev);
        toColor.add(next);
      } else {
        boolean prevMatchFilter = prevCandidate.equalsIgnoreCase(filter);
        if (isUnder) {
          // first phone
          if (i == 0 && prevMatchFilter) {
            toColor.add(prev);
          }
        } else if (
            (first && prevMatchFilter) &&
                (nextCandidate.equalsIgnoreCase(contextPhone))
        ) {
          toColor.add(prev);
          toMark.add(next);
        }
      }
    }

    boolean found = !toColor.isEmpty();
    if (found) {
      for (TranscriptSegment phone : phones) {
        String event = phone.getEvent();
        boolean match = toColor.contains(phone);
        boolean contextMatch = toMark.contains(phone);

        if (!event.equals(SIL)) {
          {
            builder
                .append("<th style='" + TEXT_ALIGN_CENTER)
                .append(FONT_SIZE);

            if (match) {
              builder.append(getForeground(phone.getScore()));
            }

            {
              String color = match ?
                  (" " + BACKGROUND_COLOR + ":" + getColor(phone)) :
                  contextMatch ? (" " + BACKGROUND_COLOR + ":" + "#C0C0C0") :
                      "";
              builder.append(color)
                  .append("'>");
            }
          }

          builder.append(event);
          builder.append("</th>");
        }
      }
    }
  }

  /**
   * From words
   *
   * @param netPronImageTypeToEndTime
   * @return
   * @see WordContainerAsync#getItemColumn
   */
  public String makeColoredTableReally(Map<NetPronImageType, List<SlimSegment>> netPronImageTypeToEndTime) {
    //List<SlimSegment> words = netPronImageTypeToEndTime.get(NetPronImageType.WORD_TRANSCRIPT);
    List<SlimSegment> filtered = netPronImageTypeToEndTime
        .get(NetPronImageType.WORD_TRANSCRIPT)
        .stream()
        .filter(slimSegment -> !shouldSkipPhone(slimSegment.getEvent())).collect(Collectors.toList());
    StringBuilder builder = new StringBuilder();
    builder.append(TABLE);

    {
      builder.append(THEAD);
      addWordColHeaders(builder, filtered);
      builder.append("</thead>");
    }

    builder.append(TABLEEND);
    return builder.toString();
  }

  /**
   * @param netPronImageTypeToEndTime
   * @return
   * @seex mitll.langtest.client.gauge.ASRHistoryPanel#makeColoredTable
   */
/*  public String makeColoredTable(Map<NetPronImageType, List<SlimSegment>> netPronImageTypeToEndTime) {
    StringBuilder builder = new StringBuilder();
    List<SlimSegment> words = netPronImageTypeToEndTime.get(NetPronImageType.WORD_TRANSCRIPT);
    return getHTMLForWords(builder, words);
  }*/
  public String makeColoredTableFull(Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeToEndTime) {
    StringBuilder builder = new StringBuilder();
    List<TranscriptSegment> words = netPronImageTypeToEndTime.get(NetPronImageType.WORD_TRANSCRIPT);
    return getHTMLForWords(builder, words);
  }

  @NotNull
  private <T extends SlimSegment> String getHTMLForWords(StringBuilder builder, List<T> words) {
    if (words == null) {
      //   logger.warning("no transcript?");
      // could happen for low score...
    } else {
      words
          .stream()
          .filter(segment -> !shouldSkipPhone(segment.getEvent()))
          .forEach(word -> builder.append(getColoredSpanForWord(word)));
    }
    return builder.toString();
  }


  private String getColoredSpanForWord(SlimSegment word) {
    String event = word.getEvent().equals(UNKNOWNMODEL) ? LOW_SCORE : word.getEvent();
    // if (event.equals("UNKNOWNMODEL")) event = LOW_SCORE;
    String coloredSpan = getColoredSpan(event, word.getScore());
    // logger.info("getColoredSpanForWord : span '" + word.getEvent() + "' " + word.getScore() + " = " + getColor(word.getScore()));
    return coloredSpan;
  }

  /**
   * TODO : don't put css here.
   *
   * @param event
   * @param score
   * @return
   */
  public String getColoredSpan(String event, float score) {
    StringBuilder builder = new StringBuilder();
    String foreground = getForeground(score);
    builder.append("<span " +
        //"class='scoringStyle'" +
        "style='" +
        COLORED_SPAN_STYLE +
        foreground +
        BACKGROUND_COLOR + ":" + getColor(score) +
        "'>");
    builder.append(event);
    // logger.info("getColoredSpan Return " + builder + " for " + event + " at " + score);
    builder.append("</span>");
    return builder.toString();
  }

  @NotNull
  private String getForeground(float score) {
    return score < 0.4F ? "color:white; " : "";
  }

  /**
   * @param netPronImageTypeToEndTime
   * @param audioControl
   * @param isRTL
   * @return
   * @see ScoreFeedbackDiv#getWordTableContainer
   */
  Widget getDivWord(Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeToEndTime,
                    AudioControl audioControl,
                    Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget,
                    boolean isRTL) {
    DivWidget table = new DivWidget();
    table.addStyleName("topFiveMargin");
    table.addStyleName("leftFiveMargin");
    table.addStyleName("floatLeftAndClear");
    table.setWidth("100%");

    TreeMap<TranscriptSegment, IHighlightSegment> words = new TreeMap<>();
    typeToSegmentToWidget.put(NetPronImageType.WORD_TRANSCRIPT, words);

    TreeMap<TranscriptSegment, IHighlightSegment> phoneMap = new TreeMap<>();
    typeToSegmentToWidget.put(NetPronImageType.PHONE_TRANSCRIPT, phoneMap);

    int id = 0;
    Collection<Map.Entry<TranscriptSegment, List<TranscriptSegment>>> entries =
        getWordToPhones(netPronImageTypeToEndTime).entrySet();

    if (isRTL) {
      List<Map.Entry<TranscriptSegment, List<TranscriptSegment>>> entries1 = new ArrayList<>(entries);
      Collections.reverse(entries1);
      entries = entries1;
    }

    for (Map.Entry<TranscriptSegment, List<TranscriptSegment>> pair : entries) {
      TranscriptSegment word = pair.getKey();

      if (!shouldSkipWord(word.getEvent())) {
        table.add(getDivForWord(audioControl, words, phoneMap, id, pair.getValue(), word, words.get(word)));
        id++;
      }
    }

    return table;
  }

  /**
   * inline flex on phones expands to fill space.
   *
   * @param audioControl
   * @param words
   * @param phoneMap
   * @param id
   * @param phonesForWord
   * @param word
   * @param wordHighlight
   * @return
   * @see #getDivWord
   */
  private Widget getDivForWord(AudioControl audioControl,
                               TreeMap<TranscriptSegment, IHighlightSegment> words,
                               TreeMap<TranscriptSegment, IHighlightSegment> phoneMap,
                               int id,
                               List<TranscriptSegment> phonesForWord,
                               TranscriptSegment word, IHighlightSegment wordHighlight) {
    HighlightSegment header = getWordLabel(id, word.getEvent());

    words.put(word, header);
    addClickHandler(audioControl, word, header.getClickable());

    // String color =
    setColorClickable(word, header);
    setForegroundColor(word, header);
    //   logger.info("getDivForWord : color for " + word.getEvent() + " score " + word.getScore() + " = " + color);

    new TooltipHelper().addTooltip(header, CLICK_TO_HEAR_WORD);

    {
      DivWidget phones = getPhoneDivBelowWord(audioControl, phoneMap, phonesForWord, false, null, /*isRTL*/true, wordHighlight);
      phones.addStyleName("inlineFlex");

      header.setSouthScore(phones);
    }
    return header;
  }

  private void setForegroundColor(TranscriptSegment word, UIObject header) {
    if (word.getScore() < 0.4F) {
      header.getElement().getStyle().setColor("white");
    }
  }

  /**
   * @param id
   * @param wordLabel
   * @return
   * @see #getDivForWord
   */
  @NotNull
  private HighlightSegment getWordLabel(int id, String wordLabel) {
    HighlightSegment header = new HighlightSegment(id, wordLabel);
    alignCenter(header);
    header.addStyleName("wordTableWord");
    header.addStyleName("floatLeft");

    header.getNorth().setWidth("100%");
    header.getNorth().getElement().getStyle().setCursor(Style.Cursor.POINTER);
    return header;
  }

  /**
   * @param audioControl  so when clicked, we can play audio
   * @param phoneMap
   * @param phoneSegments
   * @param simpleLayout
   * @param wordSegment
   * @param doFloatLeft
   * @param wordHighlight
   * @return
   * @paramz isRTL
   * @see TwoColumnExercisePanel#getPhoneDivBelowWord
   */
  @NotNull
  DivWidget getPhoneDivBelowWord(AudioControl audioControl,
                                 TreeMap<TranscriptSegment, IHighlightSegment> phoneMap,
                                 List<TranscriptSegment> phoneSegments,
                                 boolean simpleLayout,
                                 TranscriptSegment wordSegment,
                                 boolean doFloatLeft, IHighlightSegment wordHighlight) {
    DivWidget phones = new DivWidget();
    if (doFloatLeft) {
      phones.addStyleName("phoneContainer");
    } else {
      phones.addStyleName("simplePhoneContainer");
    }

    addPhonesBelowWord2(phoneSegments, phones, audioControl, phoneMap, simpleLayout, wordSegment,wordHighlight);
    return phones;
  }

  /**
   * @param netPronImageTypeToEndTime
   * @param showScore
   * @return
   * @see ReviewScoringPanel#getWordTable
   */
  Widget getWordTable(Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeToEndTime, boolean showScore) {
    Map<TranscriptSegment, List<TranscriptSegment>> wordToPhones = getWordToPhones(netPronImageTypeToEndTime);

    Table table = new Table();
    table.getElement().setId("WordTable");
    table.getElement().getStyle().clearWidth();
    table.removeStyleName("table");

    HTMLPanel srow = new HTMLPanel(TR, "");
    table.add(srow);

    HTMLPanel row = new HTMLPanel(TR, "");
    table.add(row);

    for (Map.Entry<TranscriptSegment, List<TranscriptSegment>> pair : wordToPhones.entrySet()) {
      TranscriptSegment word = pair.getKey();

      String wordLabel = word.getEvent();
      if (!shouldSkipWord(wordLabel)) {
        TableHeader header = new TableHeader(wordLabel);
        alignCenter(header);
        setColor(word, header);
        table.add(header);

        HTMLPanel col;
        srow.add(col = new HTMLPanel(TD, ""));

        HTML wscore = showScore ? getScore(word) : new HTML();
        alignCenter(wscore);
        col.add(wscore);

        if (!showScore) {
          wscore.getElement().getStyle().setHeight(0, Style.Unit.PX);
        }

        Table pTable = new Table();
        pTable.removeStyleName("table");

        col = new HTMLPanel(TD, "");
        col.getElement().getStyle().clearBorderStyle();
        col.add(pTable);
        row.add(col);

        // TODO : remove this???
        HTMLPanel row2 = new HTMLPanel(TR, "");
        pTable.add(row2);

        HTMLPanel scoreRow = new HTMLPanel(TR, "");
        pTable.add(scoreRow);

        if (!showScore) {
          scoreRow.getElement().getStyle().setHeight(0, Style.Unit.PX);
        }

        addPhonesBelowWord(showScore, pair.getValue(), pTable, scoreRow);
      }
    }

    return table;
  }

  private void addPhonesBelowWord(boolean showScore, List<TranscriptSegment> value, Table pTable, HTMLPanel scoreRow) {
    for (TranscriptSegment phone : value) {
      String phoneLabel = getPhoneEvent(phone);
      if (!phoneLabel.equals(SIL)) {
        TableHeader h = new TableHeader(phoneLabel);
        alignCenter(h);
        pTable.add(h);
        setColor(phone, h);

        HTML score = showScore ? getScore(phone) : new HTML();
        alignCenter(score);
        score.getElement().getStyle().setWidth(PHONE_WIDTH, Style.Unit.PX);

        HTMLPanel col = new HTMLPanel(TD, "");

        if (!showScore) {
          score.getElement().getStyle().setHeight(0, Style.Unit.PX);
        }
        col.add(score);

        scoreRow.add(col);
      }
    }
  }

  private String getPhoneEvent(TranscriptSegment phone) {
    return phone.getEvent();
  }

  /**
   * Feedback from DLI instructors is that phones should go left to right, even for RTL languages
   * like arabic.
   *
   * @param phoneSegments
   * @param scoreRow
   * @param audioControl
   * @param phoneMap
   * @param simpleLayout
   * @param wordSegment
   * @param wordHighlight
   * @paramx isRTL
   * @see #getPhoneDivBelowWord
   */
  private void addPhonesBelowWord2(List<TranscriptSegment> phoneSegments,
                                   DivWidget scoreRow,
                                   AudioControl audioControl,
                                   TreeMap<TranscriptSegment, IHighlightSegment> phoneMap,
                                   boolean simpleLayout,
                                   TranscriptSegment wordSegment,
                                   IHighlightSegment wordHighlight) {
    if (DEBUG) logger.info("addPhonesBelowWord2 add phones below " +
        "\n\tword " + wordSegment +
        "\n\tsegs " + phoneSegments.size());

//    String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("word " +wordSegment));
//    logger.info("logException stack " + exceptionAsString);

    Iterator<TranscriptSegment> iterator = phoneSegments.iterator();
    boolean hasAudioControl = audioControl != null;

//   if (simpleLayout) logger.info("addPhonesBelowWord2 simple layout for " +wordSegment + " and " + phoneSegments);
    while (iterator.hasNext()) {
      TranscriptSegment phoneSegment = iterator.next();
      String phoneLabel = getPhoneEvent(phoneSegment);
      if (!shouldSkipPhone(phoneLabel)) {
        boolean hasNext = iterator.hasNext();
        String phoneLabel1 = getPhoneLabel(phoneSegment, phoneLabel);
        SimpleHighlightSegment h = new SimpleHighlightSegment(phoneLabel1 + (hasNext ? NBSP : ""), BLUE);
        //  logger.info("\taddPhonesBelowWord2 word " + wordSegment + " phone " + phoneLabel + " : " + h.getContent());
//        if (phoneSegment.isIn(wordSegment)) {
        {
          HTML clickable = h.getClickable();

          addClickHandler(audioControl, wordSegment == null ? phoneSegment : wordSegment, clickable);

          if (wordHighlight != null) {
            clickable.addMouseOverHandler(event -> wordHighlight.asWidget().addStyleName("underline"));
            clickable.addMouseOutHandler(event -> wordHighlight.asWidget().removeStyleName("underline"));
          }
        }

        //      }
        IHighlightSegment put = phoneMap.put(phoneSegment, h);
        if (put != null) logger.info("prev for " + phoneSegment + " was " + put);

        if (simpleLayout) {
          if (hasNext) {
            h.addStyleName("phoneStyle");
          } else {
            h.addStyleName("lastPhoneStyle");
          }
        } else {
          if (hasAudioControl) addHandStyle(h);
          alignCenter(h);
          setColorClickable(phoneSegment, h);
          setForegroundColor(phoneSegment, h);

          h.addStyleName("phoneWidth");
        }
        scoreRow.add(h);
      }
    }
  }

  private String getPhoneLabel(TranscriptSegment phoneSegment, String phoneLabel) {
    String phoneLabel1 = phoneLabel;
    String displayEvent = phoneSegment.getDisplayEvent();
    if (!phoneLabel1.equals(displayEvent)) {
      phoneLabel1 = displayEvent;
    }
    return phoneLabel1;
  }

  /**
   * When clicked, tell audioControl to play segment
   *
   * @param audioControl
   * @param segmentToPlay
   * @param header
   * @see #addPhonesBelowWord2
   * @see #getDivWord(Map, AudioControl, Map, boolean)
   */
  private void addClickHandler(AudioControl audioControl, TranscriptSegment segmentToPlay, Label header) {
    if (audioControl != null) {
    //  if (false) logger.info("addClickHandler add handler for " + segmentToPlay + " when click on " + header.getText());
      header.addClickHandler(event -> {

      //  logger.info("addClickHandler click on " + segmentToPlay + " header " + header.getText());

        audioControl.loadAndPlaySegment(segmentToPlay.getStart(), segmentToPlay.getEnd());
      });
    }
  }

  private void addHandStyle(Label header) {
    header.addStyleName("handCursor");
  }

  private void setColor(SlimSegment phone, UIObject h) {
    h.getElement().getStyle().setBackgroundColor(getColor(phone));
  }

  private void setColorClickable(SlimSegment phone, IHighlightSegment h) {
    h.setBackground(getColor(phone));
  }

  @NotNull
  private String getColor(SlimSegment phone) {
    return getColor(phone.getScore());
  }

  @NotNull
  private String getColor(float score) {
    return SimpleColumnChart.getColor(score);
  }

  private void alignCenter(UIObject header) {
    header.addStyleName("center");
  }

  @NotNull
  private HTML getScore(SlimSegment word) {
    return new HTML("" + getPercent(word.getScore()));
  }

  private int getPercent(Float aFloat) {
    return getScore(aFloat * 100);
  }

  private int getScore(float a) {
    return Math.round(a);
  }

  /**
   * Slower than needs to be.
   *
   * @param netPronImageTypeToEndTime
   * @return
   * @see #getDivWord
   */
  private Map<TranscriptSegment, List<TranscriptSegment>> getWordToPhones(
      Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeToEndTime) {
    List<TranscriptSegment> words = netPronImageTypeToEndTime.get(NetPronImageType.WORD_TRANSCRIPT);
    List<TranscriptSegment> phones = netPronImageTypeToEndTime.get(NetPronImageType.PHONE_TRANSCRIPT);

    Map<TranscriptSegment, List<TranscriptSegment>> wordToPhones = new HashMap<>();
    if (words != null) {
      for (TranscriptSegment word : words) {
        String event = word.getEvent();
        if (!shouldSkipPhone(event)) {
          for (TranscriptSegment phone : phones) {
            if (phone.getStart() >= word.getStart() && phone.getEnd() <= word.getEnd()) {
              List<TranscriptSegment> orDefault = wordToPhones.computeIfAbsent(word, k -> new ArrayList<>());
              orDefault.add(phone);
            }
          }
        }
      }
    } else {
      //logger.warning("getWordToPhones no words in " + netPronImageTypeToEndTime);
    }
    return wordToPhones;
  }

  private boolean shouldSkipPhone(String event) {
    return event.equalsIgnoreCase(SIL) || event.equals("<s>") || event.equals("</s>");
  }

  private boolean shouldSkipWord(String wordLabel) {
    return wordLabel.equals("SIL");
  }
}
