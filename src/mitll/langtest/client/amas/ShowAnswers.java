package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.shared.amas.QAPair;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 10/1/15.
 */
public class ShowAnswers {
  private Logger logger = Logger.getLogger("ShowAnswers");
  private static final String TWO_SPACES = "&nbsp;&nbsp;";
  private String language;
  /**
   * @see AmasExercisePanel#showAnswers(QAPair, HasWidgets, String, String)
   */
  public ShowAnswers(String language) {
   this.language = language;
  }

  /**
   * @param qaPair
   * @param toAddTo
   * @param prefix
   * @param question
   * @return
   * @see mitll.langtest.client.recorder.FeedbackRecordPanel#getAnswerWidget
   * @see AmasExercisePanel#showAnswers(QAPair, HasWidgets, String, String)
   */
  public Widget showAnswers(QAPair qaPair, HasWidgets toAddTo, String prefix, String question) {
    if (!prefix.isEmpty() || !question.isEmpty()) {
      Heading widget = new Heading(4, "<b>" + prefix + "</b>" + question);
      widget.addStyleName(language);
      toAddTo.add(widget);
    }
    Grid grid = getGridOfAnswers(qaPair.getAlternateAnswers());
    toAddTo.add(grid);
    return grid;
  }

  /**
   * @param alternateAnswers
   * @return
   * @see mitll.langtest.client.result.ResultManager#getAsyncTable(int, Widget)
   */
  public Grid getGridOfAnswers(List<String> alternateAnswers) {
    List<String> copy = trim(alternateAnswers);
    int n = copy.size();
    float fn = ((float) n) / 3;
    double ceil = Math.ceil(fn);
    int cn = (int) ceil;
    int maxCols = 2 * cn;

    // logger.info("n " + n + " fn " + fn + " ceil " + ceil + " cn " + cn + " max " + maxColsOld + " new " + maxCols);

    Grid grid = getGrid(n, maxCols);
    String h4Prefix = "<h4 style='margin:0px' class='" + language +
        "'>";
    if (n > 1) {
      SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder().appendHtmlConstant("<b>Possible answers</b>");
      grid.setHTML(0, 0, "");
      grid.setHTML(0, 1, safeHtmlBuilder.toSafeHtml());

      int j = 0;
      int i = 0;
      for (String alternate : copy) {
        char toUse= (char) ('a' + j++);
        safeHtmlBuilder = new SafeHtmlBuilder().appendHtmlConstant("<b>(" + toUse + ")"+
            TWO_SPACES +
            "</b>");
        int row1 = (i % 3) + 1; // header row - +1
        int column = 2 * (i / 3);

        //        logger.info("i " + i + " row " + row1 + " col " +column + " maxc " + maxCols + " n " +n);
        grid.setHTML(row1, column, safeHtmlBuilder.toSafeHtml());

        if (column + 1 < maxCols) {
          grid.setHTML(row1, column + 1, h4Prefix + alternate + "</h4>");
          grid.getCellFormatter().setStyleName(row1, column + 1, "tableCell-even");
        }
        i++;
      }
    } else {
      SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder().appendHtmlConstant("<b>Answer" +
          TWO_SPACES +
          "</b>");
      grid = new Grid(1, 2);
//      if (isRTL) grid.addStyleName("arabicFont");
      grid.addStyleName(language);

      grid.setHTML(0, 0, safeHtmlBuilder.toSafeHtml());
      String onlyAnswer = alternateAnswers.get(0);

      grid.setHTML(0, 1, h4Prefix + onlyAnswer + "</h4>");
    }
    return grid;
  }

  private List<String> trim(List<String> alternateAnswers) {
    List<String> copy = new ArrayList<String>();
    for (String alt : alternateAnswers) {
      if (!alt.trim().isEmpty()) {
        copy.add(alt);
      } else logger.warning("removed empty answer?");
    }
    return copy;
  }

  private Grid getGrid(int n, int maxCols) {
    int maxRows = Math.min(n + 1, Math.max(3, n + 1));
    if (maxRows > 4) maxRows = 4;
    Grid grid = new Grid(maxRows, maxCols);
   // if (isRTL) grid.addStyleName("arabicFont");
    grid.addStyleName(language);
    return grid;
  }
}
