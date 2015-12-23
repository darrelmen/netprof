package mitll.langtest.client.amas;

import com.github.gwtbootstrap.client.ui.Label;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.custom.SimpleChapterNPFHelper;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.flashcard.CorrectAndScore;

import java.util.Collection;

/**
* Created by go22670 on 3/16/15.
*/
public class QuizScorePanel extends VerticalPanel {
  //private Logger logger = Logger.getLogger("QuizScorePanel");
  private Label selfScore = new Label("");
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
