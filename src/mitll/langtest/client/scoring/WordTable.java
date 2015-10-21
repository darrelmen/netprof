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

/**
 * Created by go22670 on 10/21/15.
 */
public class WordTable {

  public String toHTML(Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeToEndTime) {
    Map<TranscriptSegment, List<TranscriptSegment>> wordToPhones = getWordToPhones(netPronImageTypeToEndTime);
    StringBuilder builder = new StringBuilder();
    builder.append("<table>");
    builder.append("<thead>");
    for (Map.Entry<TranscriptSegment, List<TranscriptSegment>> pair : wordToPhones.entrySet()) {
      TranscriptSegment word = pair.getKey();
      builder.append("<th style='text-align:center; background-color:" +SimpleColumnChart.getColor(word.getScore())+
          "'>");
      builder.append(word.getEvent());
      builder.append("</th>");
    }
    builder.append("</thead>");
    builder.append("</table>");
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

      HTMLPanel row2 = new HTMLPanel("tr", "");
      pTable.add(row2);

      HTMLPanel row3 = new HTMLPanel("tr", "");
      pTable.add(row3);

      for (TranscriptSegment phone : pair.getValue()) {
        String event = phone.getEvent();
        if (!event.equals("sil")) {
          TableHeader h = new TableHeader(event);
          h.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
          pTable.add(h);
          String color1 = SimpleColumnChart.getColor(phone.getScore());
          h.getElement().getStyle().setBackgroundColor(color1);

          //  HTML widget = new HTML(" <b>" + getPercent(phone.getScore()) +"</b>");
          HTML widget = new HTML("" + getPercent(phone.getScore()));
          widget.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
          widget.getElement().getStyle().setWidth(25, Style.Unit.PX);

          col = new HTMLPanel("td", "");
          col.add(widget);
          row3.add(col);
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

  private Map<TranscriptSegment, List<TranscriptSegment>> getWordToPhones(Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeToEndTime) {
    List<TranscriptSegment> words = netPronImageTypeToEndTime.get(NetPronImageType.WORD_TRANSCRIPT);
    List<TranscriptSegment> phones = netPronImageTypeToEndTime.get(NetPronImageType.PHONE_TRANSCRIPT);

    Map<TranscriptSegment, List<TranscriptSegment>> wordToPhones = new HashMap<>();
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
    return wordToPhones;
  }
}
