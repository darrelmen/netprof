package mitll.langtest.client.analysis;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.SafeHtmlHeader;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.flashcard.MySoundFeedback;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.PlayAudioWidget;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Created by go22670 on 1/20/17.
 */
public abstract class AudioExampleContainer<T extends WordScore> extends SimplePagingContainer<T> {
  // private final Logger logger = Logger.getLogger("AudioExampleContainer");
  private static final int PLAY_WIDTH = 42;
  private static final int NATIVE_WIDTH = PLAY_WIDTH;
  private static final String NATIVE = "Ref";
  private static final String PLAY = "Play";

  protected final AnalysisPlot plot;
  private final MySoundFeedback soundFeedback = new MySoundFeedback(this.controller.getSoundManager());

  /**
   * @param controller
   * @param plot
   * @see PhoneExampleContainer#PhoneExampleContainer
   */
  AudioExampleContainer(ExerciseController controller, AnalysisPlot plot) {
    super(controller);
    this.plot = plot;
  }

  void addAudioColumns() {
    Column<T, SafeHtml> column = getPlayAudio();

    {
      SafeHtmlHeader header = new SafeHtmlHeader((SafeHtml) () -> "<span style=\"text-align:left;\">" + PLAY + "</span>");

      table.addColumn(column, header);
      table.setColumnWidth(column, PLAY_WIDTH + "px");
      column.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    }

    {
      column = getPlayNativeAudio();
      table.addColumn(column, NATIVE);
      table.setColumnWidth(column, NATIVE_WIDTH + "px");
      column.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    }
  }

  /**
   * TODO : buggy - make sure the exercise is there before asking for it...
   *
   * @return
   * @see #addColumnsToTable
   */
  private Column<T, SafeHtml> getPlayAudio() {
    return new Column<T, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(T shell) {
        if (shell == null) return getSafeHtml("");
        CommonShell exercise = getShell(shell.getExid());
        // logger.info("getPlayAudio : Got " + shell.getId() + "  : " + shell.getFileRef());
        return PlayAudioWidget.getAudioTagHTML(shell.getAnswerAudio(), getTitle(exercise));
      }
    };
  }


  /**
   * @return
   * @see #addColumnsToTable
   */
  private Column<T, SafeHtml> getPlayNativeAudio() {
    return new Column<T, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(T shell) {
        if (shell == null) return getSafeHtml("");

        //   logger.info("getPlayNativeAudio : Got " +  shell.getId() + "  : " + shell.getNativeAudio());
        if (shell.getRefAudio() != null) {
          CommonShell exercise = getShell(shell.getExid());
          return PlayAudioWidget.getAudioTagHTML(shell.getRefAudio(), getTitle(exercise));
        } else {
          //if  (exercise != null) logger.info("no native audio for " + exercise.getOldID());
          return new SafeHtmlBuilder().toSafeHtml();
        }
      }
    };
  }

  @NotNull
  private String getTitle(CommonShell exercise) {
    return exercise == null ? "play" : exercise.getForeignLanguage() + "/" + exercise.getEnglish();
  }

  /**
   * @param id
   * @return
   * @see #getPlayAudio
   */
  protected CommonShell getShell(int id) {
    return plot.getShell(id);
  }

  void addPlayer() {
    Scheduler.get().scheduleDeferred(PlayAudioWidget::addPlayer);
  }

  /**
   * Choose different direction depending on language
   *
   * @return
   */
  @Override
  protected CellTable.Resources chooseResources() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    boolean isRTL = projectStartupInfo != null && projectStartupInfo.getLanguageInfo().isRTL();

    CellTable.Resources o;
    if (isRTL) {   // so when we truncate long entries, the ... appears on the correct end
      // logger.info("simplePaging : chooseResources RTL - content");
      o = GWT.create(RTLTableResources.class);
    } else {
      // logger.info("simplePaging : chooseResources LTR - content");
      o = GWT.create(TableResources.class);
    }
    return o;
  }

  void gotClick(T object, NativeEvent event) {
    if (BrowserEvents.CLICK.equals(event.getType())) {
      gotClickOnItem(object);
    }
  }

  protected void gotClickOnItem(final T wordScore) {
    playAudio(wordScore);
  }

  protected void playAudio(T wordScore) {
    soundFeedback.queueSong(CompressedAudio.getPath(wordScore.getAnswerAudio()), new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {

      }

      @Override
      public void songEnded() {
        studentAudioEnded();
      }
    });
  }

  protected void studentAudioEnded() {

  }

  SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

  public interface TableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "ExerciseCellTableStyleSheet.css"})
    AudioExampleContainer.TableResources.TableStyle cellTableStyle();
  }

  public interface RTLTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "RTLExerciseCellTableStyleSheet.css"})
    AudioExampleContainer.RTLTableResources.TableStyle cellTableStyle();
  }
}
