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

import com.github.gwtbootstrap.client.ui.incubator.Table;
import com.github.gwtbootstrap.client.ui.incubator.TableHeader;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.gauge.SimpleColumnChart;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/21/15.
 */
public class WordTable {
  private final Logger logger = Logger.getLogger("WordTable");

  public String toHTML(Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeToEndTime, String filter) {
    Map<TranscriptSegment, List<TranscriptSegment>> wordToPhones = getWordToPhones(netPronImageTypeToEndTime);
    StringBuilder builder = new StringBuilder();
    builder.append("<table>");
   // builder.append("<table cellspacing='5'>");

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

      for (TranscriptSegment phone : pair.getValue()) {
        String event = phone.getEvent();
        if (!event.equals("sil")) {
          String color = " background-color:" + SimpleColumnChart.getColor(phone.getScore());
          boolean match = event.equals(filter);
          if (!match) color = "";
          builder.append("<th style='text-align:center;" +
              color +
              "'>");
          builder.append(event);
          builder.append("</th>");
        }
      }

      builder.append("</thead>");
      builder.append("</table>");
      builder.append("</td>");
  //    builder.append("<td>&nbsp;</td>");

    }

    builder.append("</tr>");
    builder.append("</table>");
    return builder.toString();
  }

  public String toHTML2(Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeToEndTime) {
    Map<TranscriptSegment, List<TranscriptSegment>> wordToPhones = getWordToPhones(netPronImageTypeToEndTime);
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<TranscriptSegment, List<TranscriptSegment>> pair : wordToPhones.entrySet()) {
      TranscriptSegment word = pair.getKey();
      float score = word.getScore();
      String event = word.getEvent();
      String coloredSpan = getColoredSpan(event, score);
      builder.append(coloredSpan);
    }

    return builder.toString();
  }

  public String getColoredSpan(String event, float score) {
    StringBuilder builder = new StringBuilder();
    builder.append("<span style='padding:3px; margin-left:3px; text-align:center; background-color:" + SimpleColumnChart.getColor(score) +
        "'>");
    builder.append(event);
    builder.append("</span>");
    return builder.toString();
  }


  public Widget getWordTable(Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeToEndTime) {
    Map<TranscriptSegment, List<TranscriptSegment>> wordToPhones = getWordToPhones(netPronImageTypeToEndTime);

    Table table = new Table();
    table.getElement().setId("WordTable");
    table.getElement().getStyle().clearWidth();
    table.removeStyleName("table");

    HTMLPanel srow = new HTMLPanel("tr", "");
    table.add(srow);

    HTMLPanel row = new HTMLPanel("tr", "");
    table.add(row);

    for (Map.Entry<TranscriptSegment, List<TranscriptSegment>> pair : wordToPhones.entrySet()) {
      TranscriptSegment word = pair.getKey();

      TableHeader header = new TableHeader(word.getEvent());
      header.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
      String color = SimpleColumnChart.getColor(word.getScore());
      header.getElement().getStyle().setBackgroundColor(color);

      table.add(header);

      HTMLPanel col;
      srow.add(col = new HTMLPanel("td", ""));
      HTML wscore = new HTML("" + getPercent(word.getScore()));
      wscore.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);

      col.add(wscore);


      Table pTable = new Table();
      pTable.removeStyleName("table");

      col = new HTMLPanel("td", "");
      col.getElement().getStyle().clearBorderStyle();
      col.add(pTable);
      row.add(col);

      // TODO : remove this???
      HTMLPanel row2 = new HTMLPanel("tr", "");
      pTable.add(row2);

      HTMLPanel scoreRow = new HTMLPanel("tr", "");
      pTable.add(scoreRow);

      for (TranscriptSegment phone : pair.getValue()) {
        String event = phone.getEvent();
        if (!event.equals("sil")) {
          TableHeader h = new TableHeader(event);
          h.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
          pTable.add(h);
          String color1 = SimpleColumnChart.getColor(phone.getScore());
          h.getElement().getStyle().setBackgroundColor(color1);

          //  HTML score = new HTML(" <b>" + getPercent(phone.getScore()) +"</b>");
          HTML score = new HTML("" + getPercent(phone.getScore()));
          score.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
          score.getElement().getStyle().setWidth(25, Style.Unit.PX);

          col = new HTMLPanel("td", "");
          col.add(score);
          scoreRow.add(col);
        }
      }
    }

    return table;
  }

  private int getPercent(Float aFloat) {
    return getScore(aFloat * 100);
  }

  private int getScore(float a) {
    return Math.round(a);
  }

  private Map<TranscriptSegment, List<TranscriptSegment>> getWordToPhones(
      Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeToEndTime) {
    List<TranscriptSegment> words = netPronImageTypeToEndTime.get(NetPronImageType.WORD_TRANSCRIPT);
    List<TranscriptSegment> phones = netPronImageTypeToEndTime.get(NetPronImageType.PHONE_TRANSCRIPT);

    Map<TranscriptSegment, List<TranscriptSegment>> wordToPhones = new HashMap<>();
    if (words != null) {
      for (TranscriptSegment word : words) {
        String event = word.getEvent();
        if (event.equals("sil") || event.equals("<s>") || event.equals("</s>")) {

        } else {
          for (TranscriptSegment phone : phones) {
            if (phone.getStart() >= word.getStart() && phone.getEnd() <= word.getEnd()) {
              List<TranscriptSegment> orDefault = wordToPhones.get(word);
              if (orDefault == null) wordToPhones.put(word, orDefault = new ArrayList<TranscriptSegment>());
              orDefault.add(phone);
            }
          }
        }
      }
    }
    else {
      logger.warning("getWordToPhones no words in " + netPronImageTypeToEndTime);
    }
    return wordToPhones;
  }
}
