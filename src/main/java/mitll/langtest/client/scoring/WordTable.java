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

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.incubator.Table;
import com.github.gwtbootstrap.client.ui.incubator.TableHeader;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.analysis.PhoneExampleContainer;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.gauge.SimpleColumnChart;
import mitll.langtest.client.sound.AudioControl;
import mitll.langtest.client.sound.HighlightSegment;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.client.sound.SimpleHighlightSegment;
import mitll.langtest.shared.instrumentation.SlimSegment;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/21/15.
 */
public class WordTable {
  private static final int PHONE_PADDING = 3;
  private final Logger logger = Logger.getLogger("WordTable");
  private static final int PHONE_WIDTH = 25;

  private static final String TR = "tr";
  private static final String TD = "td";

  /**
   * @param netPronImageTypeToEndTime
   * @param filter
   * @return
   * @see PhoneExampleContainer#getItemColumn
   */
  public String toHTML(Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeToEndTime, String filter) {
    Map<TranscriptSegment, List<TranscriptSegment>> wordToPhones = getWordToPhones(netPronImageTypeToEndTime);
    StringBuilder builder = new StringBuilder();
    builder.append("<table>");

    builder.append("<thead>");
    for (Map.Entry<TranscriptSegment, List<TranscriptSegment>> pair : wordToPhones.entrySet()) {
      TranscriptSegment word = pair.getKey();
      builder.append("<th style='text-align:center; background-color:" + SimpleColumnChart.getColor(word.getScore()) +
          "'>");
      builder.append(
          //   "&nbsp;"+
          word.getEvent()
          //     +"&nbsp;"
      );
      builder.append("</th>");
      //     builder.append("<th>&nbsp;</th>");
    }

    builder.append("</thead>");
    builder.append("<tr>");

    for (Map.Entry<TranscriptSegment, List<TranscriptSegment>> pair : wordToPhones.entrySet()) {
      builder.append("<td>");
      builder.append("<table>");
      builder.append("<thead>");

      addPhones(filter, builder, pair);

      builder.append("</thead>");
      builder.append("</table>");
      builder.append("</td>");
      //    builder.append("<td>&nbsp;</td>");
    }

    builder.append("</tr>");
    builder.append("</table>");
    return builder.toString();
  }

  private void addPhones(String filter, StringBuilder builder, Map.Entry<TranscriptSegment, List<TranscriptSegment>> pair) {
    for (TranscriptSegment phone : pair.getValue()) {
      String event = phone.getEvent();
      if (!event.equals("sil")) {
        String color = " background-color:" + SimpleColumnChart.getColor(phone.getScore());
        boolean match = event.equals(filter);
        if (!match) color = "";
        builder.append("<th style='text-align:center;" + color + "'>");
        builder.append(event);
        builder.append("</th>");
      }
    }
  }

  /**
   * @param netPronImageTypeToEndTime
   * @return
   * @see mitll.langtest.client.gauge.ASRHistoryPanel#makeColoredTable
   */
  public String makeColoredTable(Map<NetPronImageType, List<SlimSegment>> netPronImageTypeToEndTime) {
    StringBuilder builder = new StringBuilder();
    List<SlimSegment> words = netPronImageTypeToEndTime.get(NetPronImageType.WORD_TRANSCRIPT);

//    for (Map.Entry<TranscriptSegment, List<TranscriptSegment>> pair : getWordToPhones(netPronImageTypeToEndTime).entrySet()) {
//      builder.append(getColoredSpanForSegment(pair));
//    }

    return getHTMLForWords(builder, words);
  }

  public String makeColoredTableFull(Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeToEndTime) {
    StringBuilder builder = new StringBuilder();
    List<TranscriptSegment> words = netPronImageTypeToEndTime.get(NetPronImageType.WORD_TRANSCRIPT);

//    for (Map.Entry<TranscriptSegment, List<TranscriptSegment>> pair : getWordToPhones(netPronImageTypeToEndTime).entrySet()) {
//      builder.append(getColoredSpanForSegment(pair));
//    }

    return getHTMLForWords(builder, words);
  }

  @NotNull
  private <T extends SlimSegment> String getHTMLForWords(StringBuilder builder, List<T> words) {
    if (words == null) {
      logger.warning("no transcript?");
    } else {
      words
          .stream()
          .filter(segment -> !shouldSkipPhone(segment.getEvent()))
          .forEach(word -> builder.append(getColoredSpanForWord(word)));
    }
    return builder.toString();
  }


  private String getColoredSpanForWord(SlimSegment word) {
    String event = word.getEvent();
    if (event.equals("UNKNOWNMODEL")) event = "Low score";
    String coloredSpan = getColoredSpan(event, word.getScore());
    //  logger.info("span '" + word.getEvent() + "' " + word.getScore() + " ");
    return coloredSpan;
  }

  public String getColoredSpan(String event, float score) {
    StringBuilder builder = new StringBuilder();
    builder.append("<span style='" +
        "padding:3px; " +
        "margin-left:3px; " +
        "text-align:center; " +
        "white-space:nowrap; " +
        "background-color:" + SimpleColumnChart.getColor(score) +
        "'>");
    builder.append(event);
    builder.append("</span>");
    return builder.toString();
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

    Map<TranscriptSegment, List<TranscriptSegment>> wordToPhones = getWordToPhones(netPronImageTypeToEndTime);

    TreeMap<TranscriptSegment, IHighlightSegment> words = new TreeMap<>();
    typeToSegmentToWidget.put(NetPronImageType.WORD_TRANSCRIPT, words);

    TreeMap<TranscriptSegment, IHighlightSegment> phoneMap = new TreeMap<>();
    typeToSegmentToWidget.put(NetPronImageType.PHONE_TRANSCRIPT, phoneMap);

    int id = 0;
    Collection<Map.Entry<TranscriptSegment, List<TranscriptSegment>>> entries = wordToPhones.entrySet();

    if (isRTL) {
      List<Map.Entry<TranscriptSegment, List<TranscriptSegment>>> entries1 = new ArrayList<>(entries);
      Collections.reverse(entries1);
      entries = entries1;
    }

    for (Map.Entry<TranscriptSegment, List<TranscriptSegment>> pair : entries) {
      TranscriptSegment word = pair.getKey();

      String wordLabel = word.getEvent();
      if (!shouldSkipWord(wordLabel)) {
        DivWidget col = new DivWidget();
        col.addStyleName("wordTableWord");
        table.add(col);

        HighlightSegment header = new HighlightSegment(id++, wordLabel);
        alignCenter(header);
        header.addStyleName("floatLeft");
        header.setWidth("100%");
        header.getNorth().setWidth("100%");

        words.put(word, header);
        addClickHandler(audioControl, word, header.getClickable());

        setColorClickable(word, header);

        DivWidget hdiv = new DivWidget();
        hdiv.setWidth("100%");
        hdiv.add(header);
        col.add(hdiv);

        DivWidget phones = getPhoneDivBelowWord(audioControl, phoneMap, pair.getValue(), false, null, isRTL);
        col.add(phones);
      }
    }

    return table;
  }

  /**
   * @param audioControl so when clicked, we can play audio
   * @param phoneMap
   * @param value
   * @param simpleLayout
   * @param wordSegment
   * @param isRTL
   * @return
   */
  @NotNull
  DivWidget getPhoneDivBelowWord(AudioControl audioControl,
                                 TreeMap<TranscriptSegment, IHighlightSegment> phoneMap,
                                 List<TranscriptSegment> value,
                                 boolean simpleLayout,
                                 TranscriptSegment wordSegment, boolean isRTL) {
    DivWidget phones = new DivWidget();
    phones.addStyleName("inlineFlex");
    phones.addStyleName("phoneContainer");

    new TooltipHelper().addTooltip(phones,"Click to hear word.");

  /*  Icon playFeedback = getPlayFeedback();

    phones.addDomHandler(event -> playFeedback.setVisible(false), MouseOutEvent.getType());
    phones.addDomHandler(event -> playFeedback.setVisible(true), MouseOverEvent.getType());*/

    addPhonesBelowWord2(value, phones, audioControl, phoneMap, simpleLayout, wordSegment, isRTL);
    //phones.add(playFeedback);
    return phones;
  }

/*  @NotNull
  private Icon getPlayFeedback() {
    Icon playFeedback = new Icon(IconType.VOLUME_UP);
    playFeedback.addStyleName("leftFiveMargin");
    playFeedback.addStyleName("topFiveMargin");
    playFeedback.setVisible(false);
    Style style = playFeedback.getElement().getStyle();
    //style.setMarginLeft(-20, Style.Unit.PX);
    style.setMarginTop(-20, Style.Unit.PX);
    style.setZIndex(100);
    return playFeedback;
  }*/

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
    HTMLPanel col;
    for (TranscriptSegment phone : value) {
      String phoneLabel = phone.getEvent();
      if (!phoneLabel.equals("sil")) {
        TableHeader h = new TableHeader(phoneLabel);
        alignCenter(h);
        pTable.add(h);
        setColor(phone, h);

        //  HTML score = new HTML(" <b>" + getPercent(phone.getScore()) +"</b>");
        HTML score = showScore ? getScore(phone) : new HTML();
        alignCenter(score);
        score.getElement().getStyle().setWidth(PHONE_WIDTH, Style.Unit.PX);

        col = new HTMLPanel(TD, "");

        if (!showScore) {
          score.getElement().getStyle().setHeight(0, Style.Unit.PX);
        }
        col.add(score);

        scoreRow.add(col);
      }
    }
  }

  /**
   * @param phoneSegments
   * @param scoreRow
   * @param audioControl
   * @param phoneMap
   * @param simpleLayout
   * @param wordSegment
   * @param isRTL
   * @see #getPhoneDivBelowWord(AudioControl, TreeMap, List, boolean, TranscriptSegment, boolean)
   */
  private void addPhonesBelowWord2(List<TranscriptSegment> phoneSegments,
                                   DivWidget scoreRow,
                                   AudioControl audioControl,
                                   TreeMap<TranscriptSegment, IHighlightSegment> phoneMap,
                                   boolean simpleLayout,
                                   TranscriptSegment wordSegment,
                                   boolean isRTL) {
    if (isRTL) {
      List<TranscriptSegment> copy = new ArrayList<>(phoneSegments);
      Collections.reverse(copy);
      phoneSegments = copy;
    }
    Iterator<TranscriptSegment> iterator = phoneSegments.iterator();
    while (iterator.hasNext()) {
      TranscriptSegment phoneSegment = iterator.next();
      String phoneLabel = phoneSegment.getEvent();
      if (!shouldSkipPhone(phoneLabel)) {
        float v = phoneSegment.getStart() * 100;
        int vi = (int) v;
        SimpleHighlightSegment h = new SimpleHighlightSegment(phoneLabel, vi);
        alignCenter(h);
        addClickHandler(audioControl, wordSegment == null ? phoneSegment : wordSegment, h.getClickable());
        phoneMap.put(phoneSegment, h);

        if (simpleLayout) {
          if (iterator.hasNext()) {
            h.getElement().getStyle().setPaddingRight(PHONE_PADDING, Style.Unit.PX);
          }
          h.addStyleName("phoneColor");
        } else {
          setColorClickable(phoneSegment, h);
          h.addStyleName("phoneWidth");
        }
        scoreRow.add(h);
      }
    }
  }


  /**
   * When clicked, tell audioControl to play segment
   *
   * @param audioControl
   * @param segmentToPlay
   * @param header
   */
  private void addClickHandler(AudioControl audioControl, TranscriptSegment segmentToPlay, Label header) {
    if (audioControl != null) {
      header.addStyleName("handCursor");
    }
    header.addClickHandler(event -> {
      if (audioControl != null) {
        audioControl.repeatSegment(segmentToPlay.getStart(), segmentToPlay.getEnd());
      }
    });
  }

  private void setColor(TranscriptSegment phone, UIObject h) {
    h.getElement().getStyle().setBackgroundColor(SimpleColumnChart.getColor(phone.getScore()));
  }

  private void setColorClickable(TranscriptSegment phone, IHighlightSegment h) {
    h.setBackground(SimpleColumnChart.getColor(phone.getScore()));
  }

  private void alignCenter(UIObject header) {
    header.addStyleName("center");
  }

  @NotNull
  private HTML getScore(TranscriptSegment word) {
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
    return event.equalsIgnoreCase("sil") || event.equals("<s>") || event.equals("</s>");
  }

  private boolean shouldSkipWord(String wordLabel) {
    return wordLabel.equals("SIL");
  }

}
