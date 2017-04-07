package mitll.langtest.client.scoring;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by go22670 on 11/18/16.
 */
public class AudioSelectedEvent extends GwtEvent<AudioSelectedEventHandler> {
  public static Type<AudioSelectedEventHandler> TYPE = new Type<>();
  private int exid;

  public AudioSelectedEvent(int exid) {
    this.exid = exid;
  }

  public int getExid() {
    return exid;
  }

  @Override
  public Type<AudioSelectedEventHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(AudioSelectedEventHandler handler) {
    handler.onAudioChanged(this);
  }
}
