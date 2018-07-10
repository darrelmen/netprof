package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.IViewContaner;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.services.DialogServiceAsync;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.dialog.IDialog.METADATA;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static mitll.langtest.shared.dialog.IDialog.METADATA.*;

/**
 * Created by go22670 on 4/5/17.
 */
public class ListenViewHelper implements ContentView {
  private final Logger logger = Logger.getLogger("ListenViewHelper");

  ExerciseController controller;

  /**
   * @param controller
   * @param myView
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  ListenViewHelper(ExerciseController controller, IViewContaner viewContainer, INavigation.VIEWS myView) {
    this.controller = controller;
    // super(controller, viewContainer, myView);
  }

  @Override
  public void showContent(Panel listContent, String instanceName, boolean fromClick) {
    SelectionState selectionState = new SelectionState();
    int dialog = selectionState.getDialog();
    DivWidget child = new DivWidget();
    child.setWidth("100%");
    listContent.add(child);
    listContent.setWidth("90%");

    DialogServiceAsync dialogService = controller.getDialogService();


    dialogService.getDialog(dialog, new AsyncCallback<IDialog>() {
      @Override
      public void onFailure(Throwable caught) {


      }

      @Override
      public void onSuccess(IDialog dialog) {
        List<ExerciseAttribute> attributes = dialog.getAttributes();

        {
          DivWidget rowOne = new DivWidget();

          rowOne.setWidth("100%");
          rowOne.getElement().getStyle().setBackgroundColor("#dff4fc");
          rowOne.addStyleName("blueRow");
          {
            Heading w = new Heading(3, getAttrValue(attributes, FLPRESENTATION), getAttrValue(attributes, PRESENTATION));
            w.setWidth("50%");
            w.addStyleName("floatLeft");
            w.getElement().getStyle().setBackgroundColor("#dff4fc");
            rowOne.add(w);
          }

          {
            Heading w1 = new Heading(3, dialog.getForeignLanguage(), dialog.getEnglish());
            w1.addStyleName("rightAlign");
            w1.addStyleName("floatRight");
            w1.setWidth("50%");
            w1.getElement().getStyle().setBackgroundColor("#dff4fc");
            rowOne.add(w1);
          }

          child.add(rowOne);
        }

        {
          DivWidget row = new DivWidget();

          row.setWidth("100%");
          {
            com.google.gwt.user.client.ui.Image flag = getFlag(dialog.getImageRef());
            flag.addStyleName("floatLeft");
            row.add(flag);
          }

          {
            Heading w1 = new Heading(3, dialog.getOrientation());
            w1.addStyleName("rightAlign");
            w1.addStyleName("floatRight");
//            w1.setWidth("50%");
  //          w1.getElement().getStyle().setBackgroundColor("#dff4fc");
            row.add(w1);
          }

          child.add(row);
        }

     /*   {
          DivWidget row2 = new DivWidget();
          row2.setWidth("100%");
          row2.getElement().getStyle().setBackgroundColor("#dff4fc");
          Heading w1 = new Heading(3, getAttrValue(attributes, PRESENTATION));
          w1.setWidth("50%");
          row2.add(w1);
          Heading w = new Heading(3, result.getEnglish());
          w.addStyleName("floatRight");
          w.setWidth("50%");
          row2.add(w);

          child.add(row2);
        }*/
      }
    });
  }


  @NotNull
  private com.google.gwt.user.client.ui.Image getFlag(String cc) {
    com.google.gwt.user.client.ui.Image image = new com.google.gwt.user.client.ui.Image(cc);
    image.setHeight("100px");
    image.setWidth("100px");
    return image;
  }
  private String getAttrValue(List<ExerciseAttribute> attributes, METADATA presentation) {
    ExerciseAttribute attr = getAttr((List<ExerciseAttribute>) attributes, (METADATA) presentation);

    return attr == null ? "" : attr.getValue();
  }

  private ExerciseAttribute getAttr(List<ExerciseAttribute> attributes, METADATA presentation) {
    List<ExerciseAttribute> collect = attributes
        .stream()
        .filter(exerciseAttribute -> {
          return exerciseAttribute.getProperty().toUpperCase().equals(presentation.toString());
        })
        .collect(Collectors.toList());
    ExerciseAttribute attribute = collect.isEmpty() ? null : collect.iterator().next();
    return attribute;
  }
}
