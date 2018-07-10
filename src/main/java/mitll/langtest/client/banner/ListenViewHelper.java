package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.Image;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
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
import org.apache.xpath.operations.Div;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.gwt.dom.client.Style.*;
import static com.google.gwt.dom.client.Style.Unit.*;
import static mitll.langtest.shared.dialog.IDialog.METADATA.*;

/**
 * Created by go22670 on 4/5/17.
 */
public class ListenViewHelper implements ContentView {
  public static final int ROW_WIDTH = 97;
  public static final String HEIGHT = 100 +
      "px";
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

        DivWidget outer = getHeader(dialog, attributes);
        child.add(outer);

        DivWidget rowOne = getSpeakerRow(dialog);


        child.add(rowOne);

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
  private DivWidget getSpeakerRow(IDialog dialog) {
    DivWidget rowOne = new DivWidget();
    rowOne.addStyleName("cardBorderShadow");

    rowOne.setHeight("40px");
    rowOne.setWidth(97 + "%");
    rowOne.getElement().getStyle().setMarginTop(10, PX);

    List<String> speakers = dialog.getSpeakers();
    {
      String label = "<b>" + speakers.get(0) + "</b>";
      CheckBox checkBox = new CheckBox(label, true);
      checkBox.setWidth("49%");
      checkBox.addStyleName("floatLeft");
      checkBox.addStyleName("leftFiveMargin");
      checkBox.addValueChangeHandler(event -> speakerOneCheck(event.getValue()));

      rowOne.add(checkBox);
    }


    {
      String label = "<b>" + speakers.get(1) + "</b>";
      CheckBox checkBox = new CheckBox(label, true);

      checkBox.addValueChangeHandler(event -> speakerTwoCheck(event.getValue()));
      checkBox.addStyleName("rightAlign");
      checkBox.addStyleName("floatRight");
      checkBox.addStyleName("rightFiveMargin");
      // checkBox.getElement().getStyle().setMarginTop(0, PX);

      // checkBox.setWidth("49%");
      rowOne.add(checkBox);
    }

    rowOne.getElement().getStyle().setMarginBottom(10, PX);
    //  outer.add(rowOne);
    return rowOne;
  }

  private void speakerOneCheck(Boolean value) {
    logger.info("speaker one now " + value);
  }

  private void speakerTwoCheck(Boolean value) {
    logger.info("speaker two now " + value);
  }

  @NotNull
  private DivWidget getHeader(IDialog dialog, List<ExerciseAttribute> attributes) {
    DivWidget outer = new DivWidget();
//        outer.getElement().getStyle().setBorderWidth(2,PX);
//        outer.getElement().getStyle().setBorderColor("black");
//        outer.getElement().getStyle().setBorderStyle(BorderStyle.SOLID);
    {
      DivWidget rowOne = new DivWidget();
      rowOne.setHeight("40px");
      rowOne.setWidth(98 +
          "%");
      rowOne.getElement().getStyle().setBackgroundColor("#dff4fc");
      rowOne.addStyleName("blueRow");
      {
        Heading w = new Heading(3, getAttrValue(attributes, FLPRESENTATION), getAttrValue(attributes, PRESENTATION));
        w.setWidth("49%");
        w.addStyleName("floatLeft");
        w.addStyleName("leftFiveMargin");
        // w.addStyleName("bottomFiveMargin");
        w.getElement().getStyle().setMarginTop(0, PX);

        rowOne.add(w);
      }

      {
        Heading w1 = new Heading(3, dialog.getForeignLanguage(), dialog.getEnglish());

        w1.addStyleName("rightAlign");
        w1.addStyleName("floatRight");
        w1.addStyleName("rightFiveMargin");


        w1.getElement().getStyle().setMarginTop(0, PX);

        w1.setWidth("49%");
        rowOne.add(w1);
      }

      rowOne.getElement().getStyle().setMarginBottom(10, PX);
      outer.add(rowOne);
    }

    {
      DivWidget row = new DivWidget();
      row.addStyleName("cardBorderShadow");
      row.setHeight("100px");
      row.setWidth(ROW_WIDTH +
          "%");
      row.addStyleName("inlineFlex");
      {
        com.google.gwt.user.client.ui.Image flag = getFlag(dialog.getImageRef());
        flag.addStyleName("floatLeft");
        row.add(flag);
      }

      {
        Heading w1 = new Heading(5, dialog.getOrientation());
        w1.addStyleName("rightAlign");
        w1.addStyleName("floatRight");
        w1.addStyleName("rightTenMargin");

        w1.addStyleName("wrapword");

        w1.setWidth("85%");
        //          w1.getElement().getStyle().setBackgroundColor("#dff4fc");
        row.add(w1);
      }
      outer.add(row);
    }
    return outer;
  }


  @NotNull
  private com.google.gwt.user.client.ui.Image getFlag(String cc) {
    com.google.gwt.user.client.ui.Image image = new com.google.gwt.user.client.ui.Image(cc);
    image.setHeight(HEIGHT);
    image.setWidth(HEIGHT);
    return image;
  }

  private String getAttrValue(List<ExerciseAttribute> attributes, METADATA presentation) {
    ExerciseAttribute attr = getAttr(attributes, presentation);
    return attr == null ? "" : attr.getValue();
  }

  private ExerciseAttribute getAttr(List<ExerciseAttribute> attributes, METADATA presentation) {
    List<ExerciseAttribute> collect = attributes
        .stream()
        .filter(exerciseAttribute -> {
          return exerciseAttribute.getProperty().toUpperCase().equals(presentation.toString());
        })
        .collect(Collectors.toList());
    return collect.isEmpty() ? null : collect.iterator().next();
  }
}
