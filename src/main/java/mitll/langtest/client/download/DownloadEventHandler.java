package mitll.langtest.client.download;

import com.google.gwt.event.shared.EventHandler;

/**
 * Created by go22670 on 11/18/16.
 */
public interface DownloadEventHandler extends EventHandler {
  void doDownload(DownloadEvent authenticationEvent);
}