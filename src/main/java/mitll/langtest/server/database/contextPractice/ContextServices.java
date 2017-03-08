package mitll.langtest.server.database.contextPractice;

import mitll.langtest.shared.ContextPractice;

/**
 * Created by go22670 on 3/8/17.
 */
public interface ContextServices {
  void preloadContextPractice();

  ContextPractice getContextPractice();
}
