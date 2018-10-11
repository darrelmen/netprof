package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
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
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.custom.INavigation;
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

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 1/20/17.
 */
public abstract class AudioExampleContainer<T extends WordScore> extends SimplePagingContainer<T> {
  private final Logger logger = Logger.getLogger("AudioExampleContainer");


  private static final String REVIEW1 = "Review";
  private static final String REVIEW = REVIEW1;
  private static final String PLAY = "Play";
  protected static final boolean DEBUG = false;
  private static final int PLAY_WIDTH = 42;
  private static final int NATIVE_WIDTH = PLAY_WIDTH;
  private static final String NATIVE = "Ref";
  //private static final String PLAY = "Play";
  private static final int TABLE_HEIGHT = 215;
  private static final String PAUSE = "Pause";

  private final ExerciseLookup<CommonShell> plot;
  private final INavigation.VIEWS jumpView;
  private final MySoundFeedback soundFeedback = new MySoundFeedback(this.controller.getSoundManager());
  private int lastPlayed = -1;
  private boolean isReview = false;
  private Button play;
  private Button review;

  /**
   * @param controller
   * @param plot
   * @see PhoneExampleContainer#PhoneExampleContainer
   */
  AudioExampleContainer(ExerciseController controller, ExerciseLookup<CommonShell> plot, INavigation.VIEWS jumpView) {
    super(controller);
    this.plot = plot;
    this.jumpView = jumpView;
  }

  void addAudioColumns() {
/*
    Column<T, SafeHtml> column = getPlayAudio();
*/

/*    {
      SafeHtmlHeader header = new SafeHtmlHeader((SafeHtml) () -> "<span style=\"text-align:left;\">" + PLAY + "</span>");

      table.addColumn(column, header);
      table.setColumnWidth(column, PLAY_WIDTH + "px");
      column.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    }*/

    {
      Column<T, SafeHtml> column = getPlayNativeAudio();
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
/*
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
*/


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
    return exercise == null ? "play" : exercise.getFLToShow() + "/" + exercise.getEnglish();
  }

  /**
   * @param id
   * @return
   * @see #getPlayAudio
   */
  private CommonShell getShell(int id) {
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
      if (controller.getLanguage().equalsIgnoreCase("urdu")) {
        o = GWT.create(UrduTableResources.class);
      } else {
        o = GWT.create(RTLTableResources.class);
      }
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

  private void gotClickOnItem(final T wordScore) {
    gotClickOnPlay(play);
  }

  private boolean isPlayingStudentAudio() {
    return !play.getText().equals(PLAY);
  }


  private void playAudio(T wordScore) {
    lastPlayed = wordScore.getExid();

    soundFeedback.queueSong(CompressedAudio.getPath(wordScore.getAnswerAudio()), new SoundFeedback.EndListener() {
      @Override
      public void songStarted() {
//        logger.info("started " + CompressedAudio.getPath(wordScore.getAnswerAudio()));
      }

      @Override
      public void songEnded() {
        studentAudioEnded();
      }
    });
  }

  @Override
  protected void addTable(Panel column) {
    DivWidget tableC = new DivWidget();
    tableC.add(table);
    tableC.setHeight(TABLE_HEIGHT + "px");
    tableC.getElement().getStyle().setProperty("minWidth", "502px");  // helps safari in layout
    column.add(tableC);
    column.add(getButtonRow());
  }

  @NotNull
  private DivWidget getButtonRow() {
    DivWidget wrapper = new DivWidget();
    wrapper.addStyleName("floatRight");

    {
      Button play = new Button(PLAY) {
        @Override
        protected void onDetach() {
          super.onDetach();
          stopAudio();
        }

        ;
      };
      play.setWidth("55px");
      styleButton(play);

      play.setIcon(IconType.PLAY);
      play.addClickHandler(event -> gotClickOnPlay(play));
      wrapper.add(play);
      this.play = play;
    }

    {
      review = new Button(REVIEW) {
        @Override
        protected void onDetach() {
          super.onDetach();
          stopAudio();
        }
      };

      review.setWidth("61px");
      styleButton(review);
      review.setIcon(IconType.PLAY);
      review.addClickHandler(event -> gotClickOnReview());
      wrapper.add(review);
    }

    {
      Button learn = new Button(jumpView.toString());
      learn.addStyleName("topFiveMargin");
      learn.addStyleName("leftTenMargin");
      learn.setType(ButtonType.SUCCESS);

      learn.addClickHandler(event -> gotClickOnLearn());
      wrapper.add(learn);
    }

    DivWidget child = new DivWidget();
    child.add(wrapper);
    return child;
  }

  private void styleButton(Button review) {
    review.addStyleName("topFiveMargin");
    review.addStyleName("leftFiveMargin");
    review.setType(ButtonType.SUCCESS);
  }

  private void gotClickOnLearn() {
    if (getSelected() != null) {
      int exid = getSelected().getExid();
      controller.getShowTab(jumpView).showLearnAndItem(exid);
    }
  }

  private void gotClickOnPlay(Button play) {
    if (isReview) {
      gotClickOnReview();
    }

    String text = play.getText().trim();
    logger.info("text " + text);
    boolean isPlaying = text.equals(PLAY);
    T selected = getSelected();

    if (isPlaying) {
      play.setText(PAUSE);
      play.setIcon(IconType.PAUSE);

      if (selected != null) {
        playAudio(selected);
      }
    } else {

      stopPlay(play);
      if (selected != null) {
        if (lastPlayed != -1 && lastPlayed != selected.getExid()) {
          gotClickOnPlay(play);
        }
      }
    }
  }

  private void stopPlay(Button play) {
    stopAudio();

    play.setText(PLAY);
    play.setIcon(IconType.PLAY);
  }

  private void gotClickOnReview() {
    if (isPlayingStudentAudio()) {
      stopPlay(play);
    }

    isReview = !isReview;
    if (isReview) {
      review.setText(PAUSE);
      review.setIcon(IconType.PAUSE);

      T selected = getSelected();
      if (selected == null) {
        logger.warning("gotClickOnReview no selection?");
      } else {
        if (onLast() && table.getRowCount() > 1) {
          if (DEBUG) {
            logger.info("scrollToVisible first row - selected = " + selected + " table.getRowCount() " + table.getRowCount());
          }

          boolean didScroll = scrollToVisible(0);
          if (!didScroll) {
            T visibleItem = table.getVisibleItem(0);
            setSelected(visibleItem);
            playAudio(visibleItem);
          }
        } else {
          if (DEBUG) logger.info("gotClickOnReview loadAndPlayOrPlayAudio " + selected);
          playAudio(selected);
        }
      }
    } else {
      stopAudio();
      resetReview();
    }
  }


  private void stopAudio() {
    soundFeedback.destroySound();
  }

  SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

  /**
   * @paramx wordScore
   */
//  @Override
//  protected void playAudio(WordScore wordScore) {
//    lastPlayed = wordScore.getExid();
//    playAudio(wordScore);
//  }
  private boolean onLast() {
    int visibleItemCount = table.getVisibleItemCount();
    if (visibleItemCount == 0) return true;
    else {
      WordScore lastVisible = table.getVisibleItem(visibleItemCount - 1);
      return (lastVisible.getExid() == lastPlayed);
    }
  }

  /**
   *
   */
  private void resetReview() {
    review.setText(REVIEW1);
    review.setIcon(IconType.PLAY);
    isReview = false;
  }

  protected void selectFirstItem(List<T> results) {
    int next = -1;

    for (int i = 0; i < results.size(); i++) {
      T wordScore = results.get(i);
      if (wordScore.getExid() == lastPlayed) {
        if (DEBUG) {
          logger.info("selectFirst found last " + wordScore.getExid() + " word " + wordScore.getTranscript());
        }

        next = i;
      }
    }

    next = Math.min(results.size() - 1, next + 1);
    T toSelect = results.get(next);
    //logger.info("Select " + next + " " + toSelect.getExid());
    setSelected(toSelect);
    if (isReview) {
      Scheduler.get().scheduleDeferred(() -> playAudio(toSelect));
    }
  }

  /**
   *
   */
  private void studentAudioEnded() {
    //  logger.info("studentAudioEnded ");
    if (isReview) {
      T selected = getSelected();
      if (selected == null) {
        logger.warning("studentAudioEnded no selection?");
      } else {
        if (DEBUG) logger.info("studentAudioEnded selected " + selected);
        List<T> visibleItems = table.getVisibleItems();

        int i = visibleItems == null ? -1 : visibleItems.indexOf(selected);

        if (i > -1) {
          if (DEBUG) logger.info("studentAudioEnded index " + i + " in " + visibleItems.size());
          if (i == visibleItems.size() - 1) {
            Range visibleRange = table.getVisibleRange();
            int i1 = visibleRange.getStart() + visibleRange.getLength();
            int rowCount = table.getRowCount();
            if (DEBUG) logger.info("studentAudioEnded next page " + i1 + " row " + rowCount);

            boolean b = i1 > rowCount;
            if (b) {
              resetReview();
            } else {
              if (i1 == rowCount) {
                resetReview();
              } else {
                if (DEBUG) logger.info("studentAudioEnded scrollToVisible " + i1 + " row " + rowCount);

                scrollToVisible(i1);
              }
            }
          } else {
            if (DEBUG) logger.info("studentAudioEnded next " + (i + 1));
            T wordScore = visibleItems.get(i + 1);
            setSelected(wordScore);
            playAudio(getSelected());
          }
        }
      }
    } else {
      if (isPlayingStudentAudio()) {
        play.setText(PLAY);
        play.setIcon(IconType.PLAY);
      } else {
        resetReview();
      }
    }
  }

  @Override
  protected void addSelectionModel() {
    selectionModel = new SingleSelectionModel<>();
    table.setSelectionModel(selectionModel);
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

  public interface UrduTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "UrduExerciseCellTableStyleSheet.css"})
    AudioExampleContainer.RTLTableResources.TableStyle cellTableStyle();
  }
}
