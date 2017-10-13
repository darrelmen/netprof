package mitll.langtest.client.scoring;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by go22670 on 11/18/16.
 */
public class ListChangedEvent extends GwtEvent<ListChangedEventHandler> {
  public static final Type<ListChangedEventHandler> TYPE = new Type<>();

  public ListChangedEvent() {
  }

  @Override
  public Type<ListChangedEventHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(ListChangedEventHandler handler) {
    handler.onAudioChanged(this);
  }
}
