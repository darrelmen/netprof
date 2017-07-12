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

package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.shared.amas.QAPair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 10/1/15.
 */
class ShowAnswers {
  private final Logger logger = Logger.getLogger("ShowAnswers");

  private static final String TWO_SPACES = "&nbsp;&nbsp;";
  private final String language;

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
  private Grid getGridOfAnswers(Collection<String> alternateAnswers) {
    Collection<String> copy = trim(alternateAnswers);
    int n = copy.size();
    float fn = ((float) n) / 3;
    double ceil = Math.ceil(fn);
    int cn = (int) ceil;
    int maxCols = 2 * cn;

//    logger.info("n " + n + " fn " + fn + " ceil " + ceil + " cn " + cn +
//        //" max " + maxColsOld +
//        " new " + maxCols);

    Grid grid = getGrid(n, maxCols);
    String h4Prefix = "<h4 style='margin:0px' class='" + language + "'>";
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
      grid = getOneAnswer(alternateAnswers, h4Prefix);
    }
    return grid;
  }

  private Grid getOneAnswer(Collection<String> alternateAnswers, String h4Prefix) {
    SafeHtmlBuilder safeHtmlBuilder = new SafeHtmlBuilder().appendHtmlConstant("<b>Answer" + TWO_SPACES + "</b>");
    Grid grid = new Grid(1, 2);
    grid.addStyleName(language);
    grid.setHTML(0, 0, safeHtmlBuilder.toSafeHtml());
    String onlyAnswer = alternateAnswers.iterator().next();
    grid.setHTML(0, 1, h4Prefix + onlyAnswer + "</h4>");
    return grid;
  }

  private Collection<String> trim(Collection<String> alternateAnswers) {
    List<String> copy = new ArrayList<>();
    for (String alt : alternateAnswers) {
      if (!alt.trim().isEmpty()) {
        copy.add(alt);
      } else {
        logger.warning("trim removed empty answer?");
      }
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
