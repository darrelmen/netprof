package mitll.langtest.client.scoring;

import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 12/23/13
* Time: 5:30 PM
* To change this template use File | Settings | File Templates.
*/
public class ResizableCaptionPanel extends CaptionPanel implements ProvidesResize, RequiresResize {
  public ResizableCaptionPanel(String name) {
    super(name);
  }

  public void onResize() {
    Widget contentWidget = getContentWidget();
    if (contentWidget instanceof RequiresResize) {
      ((RequiresResize) contentWidget).onResize();
    }
  }
}
