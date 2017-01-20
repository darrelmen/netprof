package mitll.langtest.client.analysis;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.SafeHtmlHeader;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.sound.PlayAudioWidget;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.exercise.CommonShell;
import org.jetbrains.annotations.NotNull;

/**
 * Created by go22670 on 1/20/17.
 */
public abstract class AudioExampleContainer<T extends WordScore> extends
    SimplePagingContainer<T> {

  static final int PLAY_WIDTH = 42;
  private static final int NATIVE_WIDTH = PLAY_WIDTH;
  private static final String NATIVE = "Ref";
  private static final String PLAY = "Play";

  protected final AnalysisPlot plot;

  AudioExampleContainer(ExerciseController controller, AnalysisPlot plot) {
    super(controller);
    this.plot = plot;
  }

  protected CommonShell getShell(int id) {
    return plot.getIdToEx().get(id);
  }

  protected void addAudioColumns() {
    Column<T, SafeHtml> column = getPlayAudio();
    SafeHtmlHeader header = new SafeHtmlHeader(new SafeHtml() {
      @Override
      public String asString() {
        return "<span style=\"text-align:left;\">" + PLAY +
            "</span>";
      }
    });

    table.addColumn(column, header);
    table.setColumnWidth(column, PLAY_WIDTH + "px");
    column.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

    column = getPlayNativeAudio();
    table.addColumn(column, NATIVE);
    table.setColumnWidth(column, NATIVE_WIDTH + "px");
    column.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
  }

  /**
   * @return
   * @see SimplePagingContainer#addColumnsToTable()
   */
  private Column<T, SafeHtml> getPlayAudio() {
    return new Column<T, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(T shell) {
        CommonShell exercise = getShell(shell.getExid());
       // logger.info("getPlayAudio : Got " + shell.getId() + "  : " + shell.getFileRef());
        String title = getTitle(exercise);
        return PlayAudioWidget.getAudioTagHTML(shell.getAnswerAudio(), title);
      }
    };
  }

  @NotNull
  private String getTitle(CommonShell exercise) {
    return exercise == null ? "play" : exercise.getForeignLanguage() + "/" + exercise.getEnglish();
  }

  /**
   * @see #addColumnsToTable
   * @return
   */
  private Column<T, SafeHtml> getPlayNativeAudio() {
    return new Column<T, SafeHtml>(new SafeHtmlCell()) {
      @Override
      public SafeHtml getValue(T shell) {
        CommonShell exercise = getShell(shell.getExid());
     //   logger.info("getPlayNativeAudio : Got " +  shell.getId() + "  : " + shell.getNativeAudio());
        String title = getTitle(exercise);
        if (shell.getRefAudio() != null) {
          return PlayAudioWidget.getAudioTagHTML(shell.getRefAudio(), title);
        } else {
          //if  (exercise != null) logger.info("no native audio for " + exercise.getOldID());
          return new SafeHtmlBuilder().toSafeHtml();
        }
      }
    };
  }

  @Override
  protected CellTable.Resources chooseResources() {
    CellTable.Resources o;
    o = GWT.create(WordContainer.LocalTableResources.class);
    return o;
  }

  protected void addPlayer() {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        PlayAudioWidget.addPlayer();
      }
    });
  }
}
