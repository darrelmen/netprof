package mitll.langtest.client;

import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.user.client.ui.FlowPanel;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 6/29/12
 * Time: 12:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestSendArray extends FlowPanel {


  public native JsArrayNumber getArray() /*-{
    return $wnd.testarray;
  }-*/;

}
