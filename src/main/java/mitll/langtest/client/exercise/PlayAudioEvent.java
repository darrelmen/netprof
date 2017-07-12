package mitll.langtest.client.exercise;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by go22670 on 11/18/16.
 */
public class PlayAudioEvent extends GwtEvent<PlayAudioEventHandler> {
  public static final Type<PlayAudioEventHandler> TYPE = new Type<>();
  private final int id;

  public PlayAudioEvent(int id) {
    this.id = id;
  }

  @Override
  public Type<PlayAudioEventHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(PlayAudioEventHandler handler) {
    handler.onPlayAudio(this);
  }

  public int getId() {
    return id;
  }
}
