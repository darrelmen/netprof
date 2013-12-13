package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseFormatter;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/12/13
 * Time: 5:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class QCNPFExercise extends NPFExercise {
  public QCNPFExercise(Exercise e, ExerciseController controller, ListInterface listContainer,
                       float screenPortion, boolean addKeyHandler, String instance) {
    super(e, controller, listContainer, screenPortion, addKeyHandler, instance);
  }

  /**
   * @param e
   * @param content
   * @return
   */
  @Override
  protected Widget getQuestionContent(Exercise e, String content) {
    FlowPanel column = new FlowPanel();
    column.addStyleName("blockStyle");

    column.add(getEntry(e, "foreignLanguage" , ExerciseFormatter.FOREIGN_LANGUAGE_PROMPT, e.getRefSentence()));
    column.add(getEntry(e, "english", ExerciseFormatter.ENGLISH_PROMPT, e.getEnglishSentence()));

    column.getElement().setId("QuestionContent");
    column.addStyleName("floatLeft");
    return column;
  }

  /**
   * TODO add annotation info to exercise
   * TODO fill in incorrect fields with annotation info (if in qc or in edit mode?)
   * TODO post annnotation to server
   * TODO after edit, clear annotation
   *
   * @param e
   * @param field
   * @param label
   * @param value
   * @return
   */
  private Widget getEntry(Exercise e, final String field, final String label, String value) {
    Panel row = new HorizontalPanel();
    FlowPanel qcCol = new FlowPanel();
    qcCol.addStyleName("blockStyle");

    String group = "QC_" + e.getID() + "_" + label;
    RadioButton correct = new RadioButton(group, "Correct");
    qcCol.add(correct);
    final FlowPanel commentRow = new FlowPanel();
         commentRow.setVisible(false);
    final Label comment = new Label("comment?");
    final TextBox commentEntry = new TextBox();

    correct.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        commentRow.setVisible(false);
        // send to server
        System.out.println("post to server " + exercise.getID() + " field " + field + " is correct");
        //service.postAnnotation();
      }
    });
    correct.setValue(true);
    RadioButton incorrect = new RadioButton(group, "Incorrect");
    qcCol.add(incorrect);

    incorrect.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        commentRow.setVisible(true);
        commentEntry.setFocus(true);
       // comment.setText("comment?");
        // send to server
      }
    });
    row.add(qcCol);
    qcCol.addStyleName("qcRightBorder");
    qcCol.addStyleName("rightFiveMargin");

    FlowPanel nameValueComment = new FlowPanel();
    nameValueComment.addStyleName("blockStyle");

    Panel nameValueRow = new FlowPanel();
    nameValueRow.getElement().setId("nameValueRow_" + label);
    nameValueRow.addStyleName("Instruction");

    InlineHTML foreignPhrase = new InlineHTML(label);
    foreignPhrase.addStyleName("Instruction-title");
    nameValueRow.add(foreignPhrase);

    InlineHTML englishPhrase = new InlineHTML(value);
    englishPhrase.addStyleName("Instruction-data");
    nameValueRow.add(englishPhrase);
    englishPhrase.addStyleName("leftFiveMargin");

    nameValueComment.add(nameValueRow);

    DOM.setStyleAttribute(comment.getElement(), "backgroundColor", "#ff0000");
    comment.setVisible(true);
    comment.addStyleName("ImageOverlay");

    commentRow.addStyleName("trueInlineStyle");
    commentRow.add(comment);

    commentEntry.addStyleName("leftFiveMargin");
    commentRow.add(commentEntry);

    commentEntry.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
       System.out.println("post to server " + exercise.getID() + " field " + field + " comment " + commentEntry.getText() + " is incorrect");
      }
    });
    nameValueComment.add(commentRow);
    row.add(nameValueComment);
    return row;
  }
}
