package mitll.langtest.client.custom.userlist;

import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Collection;

/**
 * Created by go22670 on 7/3/17.
 */
public class ListView implements ContentView {

  private final ExerciseController controller;

  public ListView(ExerciseController controller) {
    this.controller = controller;
  }


  public void showContent(Panel listContent, String instanceName) {

    SafeUri animated = UriUtils.fromSafeConstant(LangTest.LANGTEST_IMAGES + "animated_progress28.gif");

    com.github.gwtbootstrap.client.ui.Image waitCursor = new com.github.gwtbootstrap.client.ui.Image(animated);

    listContent.clear();
    listContent.add(waitCursor);
/*    controller.getListService().getNumLists(new AsyncCallback<Integer>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(Integer result) {
        populate(listContent, result);
      }
    });*/

    controller.getListService().getLists(new AsyncCallback<Collection<UserList<CommonShell>>>() {
      @Override
      public void onFailure(Throwable caught) {

      }

      @Override
      public void onSuccess(Collection<UserList<CommonShell>> result) {
        listContent.clear();
        listContent.add(new ListContainer(controller).getTableWithPager(result));
      }
    });
  }

//  private void populate(Panel listContent, Integer result) {
//
//    listContent.add(new ListContainer(controller).getTableWithPager());
//  }

}
