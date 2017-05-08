package mitll.langtest.client.download;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by go22670 on 11/18/16.
 */
public class DownloadEvent extends GwtEvent<DownloadEventHandler> {
  public static Type<DownloadEventHandler> TYPE = new Type<>();
  //private String source;

  public DownloadEvent() {
    //this.source = source;
  }

/*
  public String getSource() {
    return source;
  }
*/

  @Override
  public Type<DownloadEventHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(DownloadEventHandler handler) {
    handler.doDownload(this);
  }
}
