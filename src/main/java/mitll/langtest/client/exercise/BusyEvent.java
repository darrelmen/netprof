package mitll.langtest.client.exercise;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by go22670 on 11/18/16.
 */
public class BusyEvent extends GwtEvent<BusyEventHandler> {
  public static final Type<BusyEventHandler> TYPE = new Type<>();

  @Override
  public Type<BusyEventHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(BusyEventHandler handler) {
    handler.onBusyChanged(this);
  }
}
