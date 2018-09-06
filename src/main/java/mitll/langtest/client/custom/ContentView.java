package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.Panel;

/**
 * Created by go22670 on 7/5/17.
 */
public interface ContentView {
  /**
   * @see INavigation#showView
   * @param listContent
   * @param instanceName
   */
  void showContent(Panel listContent, INavigation.VIEWS instanceName);
}
