package mitll.langtest.client.exercise;

import com.google.gwt.event.shared.EventHandler;

/**
 * Created by go22670 on 11/18/16.
 */
public interface BusyEventHandler extends EventHandler {
  void onBusyChanged(BusyEvent authenticationEvent);
}