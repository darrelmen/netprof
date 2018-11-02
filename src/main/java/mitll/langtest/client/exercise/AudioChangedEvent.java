package mitll.langtest.client.exercise;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by go22670 on 11/18/16.
 */
public class AudioChangedEvent extends GwtEvent<AudioChangedEventHandler> {
  private static final Type<AudioChangedEventHandler> TYPE = new Type<>();
  private final String source;

  public AudioChangedEvent(String source) {
    this.source = source;
  }

  public String getSource() {
    return source;
  }

  @Override
  public Type<AudioChangedEventHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(AudioChangedEventHandler handler) {
    handler.onAudioChanged(this);
  }
}
