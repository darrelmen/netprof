package mitll.langtest.client.download;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by go22670 on 11/18/16.
 */
public class ShowEvent extends GwtEvent<ShowEventHandler> {
  public static Type<ShowEventHandler> TYPE = new Type<>();
  //private String source;

  public ShowEvent() {
    //this.source = source;
  }

/*
  public String getSource() {
    return source;
  }
*/

  @Override
  public Type<ShowEventHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(ShowEventHandler handler) {
    handler.doShowEvent(this);
  }
}
