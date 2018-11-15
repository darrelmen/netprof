package mitll.langtest.client.download;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Created by go22670 on 11/18/16.
 */
public class DownloadEvent extends GwtEvent<DownloadEventHandler> {
  public static final Type<DownloadEventHandler> TYPE = new Type<>();

  public DownloadEvent() {
  }

  @Override
  public Type<DownloadEventHandler> getAssociatedType() {
    return TYPE;
  }

  @Override
  protected void dispatch(DownloadEventHandler handler) {
    handler.doDownload(this);
  }
}
