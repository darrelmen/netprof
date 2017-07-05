package mitll.langtest.client.custom.userlist;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.exercise.ExerciseController;

/**
 * Created by go22670 on 7/3/17.
 */
public class ListView implements ContentView {

  private final ExerciseController controller;

  public ListView(ExerciseController controller) {
    this.controller = controller;
  }

  public void showContent(Panel listContent, String instanceName) {
    //controller.getListService().get
  }

}
