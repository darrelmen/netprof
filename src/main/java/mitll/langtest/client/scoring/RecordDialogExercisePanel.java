package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import mitll.langtest.client.banner.IListenView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.scoring.AlignmentOutput;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.logging.Logger;

public class RecordDialogExercisePanel<T extends ClientExercise> extends DialogExercisePanel<T> {
  private Logger logger = Logger.getLogger("RecordDialogExercisePanel");
  NoFeedbackRecordAudioPanel<T> recordAudioPanel;

  public RecordDialogExercisePanel(final T commonExercise,
                                   final ExerciseController controller,
                                   final ListInterface<?, ?> listContainer,
                                   Map<Integer, AlignmentOutput> alignments,
                                   IListenView listenView) {
    super(commonExercise, controller, listContainer, alignments, listenView);
  }

  @Override
  public void addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices) {
    NoFeedbackRecordAudioPanel<T> recordPanel = new NoFeedbackRecordAudioPanel<>(exercise, controller);
    this.recordAudioPanel = recordPanel;

    recordPanel.addWidgets();

    DivWidget flContainer = getHorizDiv();
    if (isRight) {
      addStyleName("floatRight");
    } else flContainer.addStyleName("floatLeft");
//    addStyleName(isRight?"floatRight":"floatLeft");

    {
      DivWidget recordButtonContainer = new DivWidget();
      recordButtonContainer.addStyleName("recordingRowStyle");
      recordButtonContainer.add(recordPanel.getPostAudioRecordButton());
      flContainer.add(recordButtonContainer);
    }

    flContainer.add(recordPanel.getScoreFeedback());

    //long now = System.currentTimeMillis();
    //  logger.info("makeFirstRow for " + e.getID() + " took " + (now - then) + " to add rec");

    //   makePlayAudio(exercise, flContainer);

//    logger.info("adding widgets ");
//    recordAudioPanel = new NoFeedbackRecordAudioPanel<>(exercise, controller);
//
//    recordAudioPanel.addWidgets();
    //add(recordAudioPanel);

    add(flContainer);
    super.addWidgets(showFL, showALTFL, phonesChoices);
  }

  @NotNull
  private DivWidget getHorizDiv() {
    DivWidget flContainer = new DivWidget();
    flContainer.addStyleName("inlineFlex");
    flContainer.getElement().setId("RecordDialogExercisePanel_horiz");
    return flContainer;
  }


  protected void addMarginLeft(Style style2) {
    style2.setMarginLeft(15, Style.Unit.PX);
  }
  public void startRecording() {
    recordAudioPanel.startRecording();
  }
}
