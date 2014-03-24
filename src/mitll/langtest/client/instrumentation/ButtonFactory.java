package mitll.langtest.client.instrumentation;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;

/**
 * Created by GO22670 on 3/24/2014.
 */
public class ButtonFactory {
  private final LangTestDatabaseAsync service;


  public ButtonFactory(LangTestDatabaseAsync service, PropertyHandler props) {
    this.service = service;

  }

  public void registerButton(final Button button, final String exid,final String context,final long userid) {
    button.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        service.logEvent(button.getElement().getId(),exid,context,userid,new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(Void result) {

          }
        });
      }
    });
  }
}
