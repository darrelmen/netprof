package mitll.langtest.client.custom.userlist;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.TextArea;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Collection;

/**
 * Created by go22670 on 7/7/17.
 */
public class ImportBulk {
  private static final int IMPORT_WIDTH = 500;
  private static final int VISIBLE_LINES = 20;
  private static final int CHARACTER_WIDTH = 150;
  private TextArea textArea;

  public DivWidget showImportItem(ExerciseController controller) {
    DivWidget inner = new DivWidget();
    DivWidget upper = new DivWidget();
    String width = IMPORT_WIDTH + "px";
    upper.setWidth(width);
    textArea = new TextArea();
    textArea.setWidth(width);

    upper.add(textArea);
    inner.add(upper);
    textArea.setVisibleLines(VISIBLE_LINES);
    textArea.setCharacterWidth(CHARACTER_WIDTH);

    inner.add(new Heading(4, "Copy and paste tab separated lines with pairs of " + controller.getLanguage() + " item and its translation."));
    inner.add(new Heading(4, "(Quizlet export format.)"));
//    Button anImport = new Button("Import");
//    anImport.addClickHandler(new ClickHandler() {
//      @Override
//      public void onClick(ClickEvent event) {
//        doBulk(controller, ul);
//      }
//    });
    DivWidget bottom = new DivWidget();
//    bottom.add(anImport);
    bottom.addStyleName("topFiveMargin");
    inner.add(bottom);
    return inner;
  }

  public void doBulk(ExerciseController controller, UserList<CommonShell> ul) {
    controller.getListService().reallyCreateNewItems(ul.getID(), sanitize(textArea.getText()),
        new AsyncCallback<Collection<CommonExercise>>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Collection<CommonExercise> newExercise) {
            //logger.info("before " + ul.getExercises().size());
            //   for (CommonExercise exercise : newExercise) {
            //    ul.addExercise(exercise);
            // }
            //   tabPanel.selectTab(0);
            ///     editItem.reload();
//                showEditItem();
            //logger.info("after  " + ul.getExercises().size());
            //     reallyShowLearnTab(tabPanel, learnTab, ul, instanceName);
          }
        });
  }

  private String sanitize(String text) {
    return SimpleHtmlSanitizer.sanitizeHtml(text).asString();
  }
}
