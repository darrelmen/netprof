package mitll.langtest.client.list;

import mitll.langtest.client.custom.INavigation;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

import static mitll.langtest.client.custom.INavigation.VIEWS.*;

public class ViewParser {
  private final Logger logger = Logger.getLogger("SelectionState");

  private static final String DRILL = "Drill";
  private static final String PRACTICE = "Practice";

  @NotNull
  public INavigation.VIEWS getView(String instance) {
    try {
      String rawInstance = instance;

      if (rawInstance.equalsIgnoreCase(CORE_REHEARSE.toString())) {
        return INavigation.VIEWS.CORE_REHEARSE;
      } else if (rawInstance.equalsIgnoreCase(REHEARSE.toString())) {
        return INavigation.VIEWS.REHEARSE;
      } else if (rawInstance.equalsIgnoreCase(PERFORM_PRESS_AND_HOLD.toString())) {
        return INavigation.VIEWS.PERFORM_PRESS_AND_HOLD;
      } else if (rawInstance.equalsIgnoreCase(PERFORM.toString())) {
        return PERFORM;
      } else {
        instance = instance.replaceAll(" ", "_");

        if (instance.equalsIgnoreCase(DRILL)) instance = PRACTICE.toUpperCase();
        if (instance.equalsIgnoreCase(DIALOG.toString())) instance = INavigation.VIEWS.DIALOG.name();

        if (rawInstance.equalsIgnoreCase(CORE_REHEARSE.toString())) {
          return INavigation.VIEWS.REHEARSE;
        } else if (rawInstance.equalsIgnoreCase(PERFORM.toString())) {
          return PERFORM;
        } else {
          return instance.isEmpty() ? INavigation.VIEWS.NONE : INavigation.VIEWS.valueOf(instance.toUpperCase());
        }
      }

    } catch (IllegalArgumentException e) {
      logger.warning("getView : hmm, couldn't parse " + instance);
      return INavigation.VIEWS.NONE;
    }
  }
}
