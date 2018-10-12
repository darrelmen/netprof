package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SingleSelectionModel;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.flashcard.MySoundFeedback;
import mitll.langtest.client.sound.CompressedAudio;
import mitll.langtest.client.sound.SoundFeedback;
import mitll.langtest.shared.analysis.WordScore;
import mitll.langtest.shared.project.ProjectStartupInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by go22670 on 1/20/17.
 */
public abstract class AudioExampleContainer<T extends WordScore> extends SimplePagingContainer<T> {
  private final Logger logger = Logger.getLogger("AudioExampleContainer");

  private static final String REFERENCE = "Reference";
  private static final String REVIEW1 = "Review";
  private static final String REVIEW = REVIEW1;
  private static final String PLAY = "Play";
  protected static final boolean DEBUG = false;

  private static final int TABLE_HEIGHT = 215;
  private static final String PAUSE = "Pause";

  private final INavigation.VIEWS jumpView;
  private final MySoundFeedback soundFeedback = new MySoundFeedback(this.controller.getSoundManager());
  private int lastPlayed = -1;
  private boolean isReview = false;
  private Button play, refAudio;
  private Button review;

  /**
   * @param controller
   * @see PhoneExampleContainer#PhoneExampleContainer
   */
  AudioExampleContainer(ExerciseController controller, INavigation.VIEWS jumpView) {
    super(controller);
    this.jumpView = jumpView;
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
    gotClickOnPlay(play, true);
  }

  private boolean isPlayingStudentAudio() {
    return !play.getText().trim().equals(PLAY);
  }

  private boolean isPlayingReferenceAudio() {
    return !refAudio.getText().trim().equals(PLAY);
  }

  private void playAudio(T wordScore, boolean playYourAudio) {
    lastPlayed = wordScore.getExid();

    if (soundFeedback == null) logger.warning("no sound feedback???");
    soundFeedback.queueSong(CompressedAudio.getPath(playYourAudio ? wordScore.getAnswerAudio() : wordScore.getRefAudio()), new SoundFeedback.EndListener() {
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
    tableC.setHeight(getTableHeight() + "px");
    tableC.getElement().getStyle().setProperty("minWidth", "502px");  // helps safari in layout
    column.add(tableC);
    column.add(getButtonRow());
  }

  int getTableHeight() {
    return TABLE_HEIGHT;
  }

  @NotNull
  private DivWidget getButtonRow() {
    DivWidget wrapper = new DivWidget();
    wrapper.addStyleName("floatRight");
    wrapper.getElement().getStyle().setMarginTop(25, Style.Unit.PX);
    {
      int width = 55;
      Button play = getPlayButton(width, true);
      wrapper.add(play);

      this.play = play;
    }

    {
      Button play = getPlayButton(80, false);
      wrapper.add(play);
      this.refAudio = play;
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

  @NotNull
  private Button getPlayButton(int width, boolean playYourAudio) {
    Button play = new Button(playYourAudio ? PLAY : REFERENCE) {
      @Override
      protected void onDetach() {
        super.onDetach();
        stopAudio();
      }
    };

    play.setWidth(width + "px");
    styleButton(play);

    play.setIcon(IconType.PLAY);
    play.addClickHandler(event -> gotClickOnPlay(play, playYourAudio));

    return play;
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

  private void gotClickOnPlay(Button play, boolean playYourAudio) {
    if (isReview) {
      stopReview();
    }

    Button otherButton = play == this.play ? refAudio : this.play;
    if (playYourAudio && isPlayingReferenceAudio() ||
        (!playYourAudio && isPlayingStudentAudio())) {
      stopPlay(otherButton, !playYourAudio);
    }

    T selected = getSelected();

    if (isPlaying(play, playYourAudio)) {
      play.setText(PAUSE);
      play.setIcon(IconType.PAUSE);

      if (selected != null) {
        playAudio(selected, playYourAudio);
      }
    } else {
      stopPlay(play, playYourAudio);

      if (selected != null) {
        if (lastPlayed != -1 && lastPlayed != selected.getExid()) {
          gotClickOnPlay(play, playYourAudio);
        }
      }
    }
  }

  private boolean isPlaying(Button play, boolean playYourAudio) {
    String text = play.getText().trim();
    logger.info("isPlaying text " + text + " playYourAudio " + playYourAudio);
    return text.equals(playYourAudio ? PLAY : REFERENCE);
  }

  private void gotClickOnReview() {
    if (isPlayingStudentAudio()) {
      reallyStopPlay();
    }
    if (isPlayingReferenceAudio()) {
      reallyStopRef();
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
            playAudio(visibleItem, true);
          }
        } else {
          if (DEBUG) logger.info("gotClickOnReview loadAndPlayOrPlayAudio " + selected);
          playAudio(selected, true);
        }
      }
    } else {
      stopReview();
    }
  }

  private void stopPlay(Button play, boolean isYourAudio) {
    stopAudio();
    play.setText(isYourAudio ? PLAY : REFERENCE);
    play.setIcon(IconType.PLAY);
  }

  private void stopReview() {
    stopAudio();
    resetReview();
  }

  protected void stopAll() {
    if (isReview) stopReview();
    else if (isPlayingStudentAudio()) reallyStopPlay();
    else if (isPlayingReferenceAudio()) reallyStopRef();
  }

  private void reallyStopRef() {
    stopPlay(refAudio, false);
  }

  private void reallyStopPlay() {
    stopPlay(play, true);
  }


  protected void stopAudio() {
    soundFeedback.destroySound();
  }

  SafeHtml getSafeHtml(String columnText) {
    return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
  }

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

  void selectFirstItem(List<T> results) {
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
      Scheduler.get().scheduleDeferred(() -> playAudio(toSelect, true));
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
            playAudio(getSelected(), true);
          }
        }
      }
    } else {
      if (isPlayingStudentAudio()) {
        play.setText(PLAY);
        play.setIcon(IconType.PLAY);
      } else if (isPlayingReferenceAudio()) {
        refAudio.setText(REFERENCE);
        refAudio.setIcon(IconType.PLAY);
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
