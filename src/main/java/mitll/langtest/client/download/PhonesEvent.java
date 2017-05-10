package mitll.langtest.client.download;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by go22670 on 11/18/16.
 */
public class PhonesEvent extends GwtEvent<PhonesEventHandler> {
  public static Type<PhonesEventHandler> TYPE = new Type<>();

  public PhonesEvent() {
  }

  @Override
  public Type<PhonesEventHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(PhonesEventHandler handler) {
    handler.doPhoneEvent(this);
  }
}
