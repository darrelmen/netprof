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

import com.github.gwtbootstrap.client.ui.Label;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.services.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.Collection;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 3/16/15.
*/
class QuizScorePanel extends VerticalPanel {
  //private Logger logger = Logger.getLogger("QuizScorePanel");
  private final Label selfScore = new Label("");
  //private Label autoScore = new Label("");

  /**
   * @see AutoCRTChapterNPFHelper#getMyListLayout(LangTestDatabaseAsync, UserFeedback, UserManager, ExerciseController, SimpleChapterNPFHelper)
   */
  public QuizScorePanel() {
    Label w1 = new Label("Self-Score");
    setColor(w1,"blue");
    add(w1);
    style(w1);

    add(selfScore);
    setColor(selfScore,"black");
    style(selfScore);

/*    Label w = new Label("Auto-Score");
    style(w);
    setColor(w, "green");

    add(w);
    w.getElement().getStyle().setMarginTop(20, Style.Unit.PX);

    add(autoScore);
    style(autoScore);
    setColor(autoScore,"black");*/
  }

  private void setColor(Label w1, String blue) {
    w1.getElement().getStyle().setBackgroundColor("white");
    w1.getElement().getStyle().setColor(blue);
  }

  private void style(Widget w) {
    w.getElement().getStyle().setFontSize(32, Style.Unit.PX);
    w.getElement().getStyle().setLineHeight(30, Style.Unit.PX);
  }

  public void setScores(Collection<CorrectAndScore> scores) {
    int selfTotal = 0;
//    int autoTotal = scores.size();
    int selfCorrect = 0;
  //  int autoCorrect = 0;
    for (CorrectAndScore correctAndScore : scores) {
     // logger.info("Got " + correctAndScore);
      if (correctAndScore.getUserScore() > -1) {
        selfTotal++;
      }
      if (correctAndScore.getUserScore() > 0.99f) selfCorrect++;
   //   else selfIncorrect++;
    //  if (correctAndScore.isCorrect()) autoCorrect++;
    }
    //logger.info("self " +selfCorrect + " vs " + selfIncorrect + " = " + selfTotal);

    selfScore.setText(selfCorrect +" of "+selfTotal + " - " +getPercent(selfCorrect,selfTotal));
    //autoScore.setText(autoCorrect +" of "+autoTotal + " - " +getPercent(autoCorrect,autoTotal));
  }

  private String getPercent(int numer, int denom) {
    return (int)(100f*(float)numer/(float)denom) + "%";
  }
}
