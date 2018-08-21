package mitll.langtest.client.scoring;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.instrumentation.EventContext;
import mitll.langtest.client.instrumentation.EventLogger;
import mitll.langtest.client.sound.AudioControl;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.List;

/**
 * Created by go22670 on 2/24/17.
 * @see ScoringAudioPanel
 */
class ClickableTranscript {
  private final AudioPanel.ImageAndCheck words;
  private final AudioPanel.ImageAndCheck phones;
  private PretestScore result;
  private final EventLogger eventLogger;
  private final int exerciseID;
  private final AudioControl audioControl;

  /**
   * @param words
   * @param phones
   * @param eventLogger
   * @param exerciseID
   * @param audioControl
   * @see ScoringAudioPanel#ScoringAudioPanel
   */
  ClickableTranscript(AudioPanel.ImageAndCheck words,
                      AudioPanel.ImageAndCheck phones,
                      EventLogger eventLogger,
                      int exerciseID,
                      AudioControl audioControl
  ) {
    this.words = words;
    this.phones = phones;
    this.eventLogger = eventLogger;
    this.exerciseID = exerciseID;
    this.audioControl = audioControl;
    addClickHandlers();
  }

  void setScore(PretestScore result) {
    this.result = result;
  }

  private void addClickHandlers() {
    this.phones.getImage().getElement().getStyle().setCursor(Style.Cursor.POINTER);
    this.phones.getImage().addClickHandler(new TranscriptEventClickHandler(this.phones.getImage(), NetPronImageType.PHONE_TRANSCRIPT));

    final Image image = this.words.getImage();
    image.getElement().getStyle().setCursor(Style.Cursor.POINTER);
    image.addClickHandler(new TranscriptEventClickHandler(image, NetPronImageType.WORD_TRANSCRIPT));

    // TODO come back to this
   /* final PopupPanel popupPanel = new PopupPanel(true);
    popupPanel.add(new Icon(IconType.BULLHORN));

    image.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(final MouseOverEvent event) {
        final int eventXPos = event.getX();


        popupPanel.setPopupPosition(event.getClientX()-10,event.getClientY()-10);
        popupPanel.show();

    *//*    getClickedOnSegment(eventXPos,NetPronImageType.WORD_TRANSCRIPT,new EventSegment() {
          @Override
          public void onSegmentClick(float start, float end) {

            popupPanel.setPopupPosition(event.getScreenX(),event.getScreenY());
            popupPanel.show();
          }
        });*//*
      }
    });
    image.addMouseOutHandler(new MouseOutHandler() {
      @Override
      public void onMouseOut(MouseOutEvent event) {
        popupPanel.hide();
      }
    });*/
  }

  private abstract class MyClickHandler implements ClickHandler, EventSegment {
    final NetPronImageType type;

    private MyClickHandler(NetPronImageType type) {
      this.type = type;
    }

    /**
     * The last transcript event end time is guaranteed to be = the length of the wav audio file.<br></br>
     * First normalize the click location, then scale it to the audio file length, then find which segment
     * it's in by looking for a click that falls between the end time of a candidate segment and the
     * end time of the next segment.  <br></br>
     * Then plays, the segment - note we have to adjust between the duration of a wav file and an mp3 file, which
     * will likely be different. (A little surprising to me, initially.)
     *
     * @param event
     * @see mitll.langtest.client.scoring.AudioPanel#playSegment
     */
    public void onClick(ClickEvent event) {
      if (result != null) {
        getClickedOnSegment(event.getX(), type, this);
      } else {
//        System.err.println("no result for to click against?");
      }
    }

/*    protected void getClickedOnSegment(MouseEvent<ClickHandler> event) {
      int index = 0;
      List<Float> endTimes = result.getTypeToSegments().get(type);
      float wavFileLengthInSeconds = result.getWavFileLengthInSeconds();//endTimes.get(endTimes.size() - 1);
      float horizOffset = (float) event.getX() / (float) phones.image.getWidth();
      float mouseClickTime = wavFileLengthInSeconds * horizOffset;
      if (debug) System.out.println("got client at " + event.getX() + " or " + horizOffset + " or time " + mouseClickTime +
          " duration " + wavFileLengthInSeconds + " secs or " + wavFileLengthInSeconds * 1000 + " millis");
      for (Float endTime : endTimes) {
        float next = endTimes.get(Math.min(endTimes.size() - 1, index + 1));
        if (mouseClickTime > endTime && mouseClickTime <= next) {
          if (debug) System.out.println("\t playing from " + endTime + " to " + next);

          onSegmentClick(endTime, next);
          break;
        }
        index++;
      }
    }*/

    public abstract void onSegmentClick(TranscriptSegment segment);
  }

  /**
   * Find the start-end time period corresponding to the click on either the phone or the word image and then
   * play the segment. <br></br>
   * NOTE : the duration (or length) of the wav file is usually about 0.1 sec shorter than the
   * the mp3 file (silence padding at the end).<br></br>
   * This means we have to scale the values returned from alignment so the audio times
   * line up with those in the mp3 file.
   *
   * @see #addClickHandlers
   */
  private class TranscriptEventClickHandler extends MyClickHandler {
    final UIObject widget;

    /**
     * @param type
     * @see #addClickHandlers
     */
    TranscriptEventClickHandler(UIObject widget, NetPronImageType type) {
      super(type);
      this.widget = widget;
    }

    @Override
    public void onSegmentClick(TranscriptSegment segment) {
      //   playSegment(MP3_HEADER_OFFSET+segment.getStart(), MP3_HEADER_OFFSET+segment.getEnd());
      audioControl.loadAndPlaySegment(segment.getStart(), segment.getEnd());
      eventLogger.logEvent(widget, type.toString(),
          new EventContext("" + exerciseID, "Clicked on " + segment.getDisplayEvent()));
    }
  }

  private void getClickedOnSegment(int eventXPos, NetPronImageType type, EventSegment onClick) {
    //int index = 0;
    List<TranscriptSegment> transcriptSegments = result.getTypeToSegments().get(type);
    float wavFileLengthInSeconds = result.getWavFileLengthInSeconds();//transcriptSegments.get(transcriptSegments.size() - 1);
    float horizOffset = (float) eventXPos / (float) phones.getImage().getWidth();
    float mouseClickTime = wavFileLengthInSeconds * horizOffset;
//    if (debug) System.out.println("got client at " + eventXPos + " or " + horizOffset + " or time " + mouseClickTime +
//      " duration " + wavFileLengthInSeconds + " secs or " + wavFileLengthInSeconds * 1000 + " millis");

    for (TranscriptSegment segment : transcriptSegments) {
      // TranscriptSegment next = transcriptSegments.get(Math.min(transcriptSegments.size() - 1, index + 1));
      if (mouseClickTime > segment.getStart() && mouseClickTime <= segment.getEnd()) {
//        if (debug) System.out.println("\t playing " + segment);
        //   result.getTypeToSegments();
        onClick.onSegmentClick(segment);
        break;
      }
      //index++;
    }
  }

  private interface EventSegment {
    void onSegmentClick(TranscriptSegment segment);
  }
}
