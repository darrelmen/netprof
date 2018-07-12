package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.ContentView;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.IViewContaner;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.SelectionState;
import mitll.langtest.client.scoring.*;
import mitll.langtest.client.services.DialogServiceAsync;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.dialog.IDialog.METADATA;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.scoring.AlignmentOutput;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.gwt.dom.client.Style.Unit.PX;
import static mitll.langtest.shared.dialog.IDialog.METADATA.FLPRESENTATION;
import static mitll.langtest.shared.dialog.IDialog.METADATA.PRESENTATION;

/**
 * Created by go22670 on 4/5/17.
 */
public class ListenViewHelper implements ContentView {
  public static final int ROW_WIDTH = 97;
  public static final String HEIGHT = 100 + "px";
  public static final String RIGHT_BKG_COLOR = "#4aa8ee";
  public static final String LEFT_COLOR = "#e7e6ec";
  private final Logger logger = Logger.getLogger("ListenViewHelper");

  ExerciseController controller;
//  private boolean isRTL = false;
  // private ClickableWords<ClientExercise> clickableWords;

  /**
   * @param controller
   * @param myView
   * @see NewContentChooser#NewContentChooser(ExerciseController, IBanner)
   */
  ListenViewHelper(ExerciseController controller, IViewContaner viewContainer, INavigation.VIEWS myView) {
    this.controller = controller;
    // ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    // isRTL = projectStartupInfo != null && projectStartupInfo.getLanguageInfo().isRTL();
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
        //List<ExerciseAttribute> attributes = dialog.getAttributes();

        //child.add(getControls());
        child.add(getHeader(dialog));
        child.add(getSpeakerRow(dialog));
        child.add(getTurns(dialog));

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
    //rowOne.addStyleName("inlineFlex");

    rowOne.setHeight("40px");
    rowOne.setWidth(97 + "%");
    rowOne.getElement().getStyle().setMarginTop(10, PX);

    List<String> speakers = dialog.getSpeakers();
    {
      String label = speakers.get(0);
      CheckBox checkBox = new CheckBox(label, true);
      checkBox.setValue(true);
      //checkBox.setWidth("49%");
      checkBox.addStyleName("floatLeft");
      checkBox.addStyleName("leftFiveMargin");
      Style style = checkBox.getElement().getStyle();
      // style.setMarginLeft(-40, PX);
      //style.setFontSize(32, PX);
      //checkBox.setHeight("30px");

      checkBox.addStyleName("leftSpeaker");
      checkBox.getElement().getStyle().setBackgroundColor(LEFT_COLOR);

      checkBox.addValueChangeHandler(event -> speakerOneCheck(event.getValue()));

      rowOne.add(checkBox);
    }

    rowOne.add(getControls());
    {
      String label = speakers.get(1);
      CheckBox checkBox = new CheckBox(label, true);

      checkBox.setValue(true);
      Style style = checkBox.getElement().getStyle();
      style.setBackgroundColor(RIGHT_BKG_COLOR);
      style.setColor("white");
      //checkBox.setHeight("30px");
      checkBox.addStyleName("rightSpeaker");

      checkBox.addValueChangeHandler(event -> speakerTwoCheck(event.getValue()));
      //style.setMarginLeft(-40, PX);
      // style.setFontSize(32, PX);
      ;
      checkBox.addStyleName("rightAlign");
      checkBox.addStyleName("floatRight");
      checkBox.addStyleName("rightFiveMargin");

      rowOne.add(checkBox);
    }

    rowOne.getElement().getStyle().setMarginBottom(10, PX);
    return rowOne;
  }

  private void speakerOneCheck(Boolean value) {
    logger.info("speaker one now " + value);
  }

  private void speakerTwoCheck(Boolean value) {
    logger.info("speaker two now " + value);
  }

  @NotNull
  private DivWidget getHeader(IDialog dialog) {
    DivWidget outer = new DivWidget();
//        outer.getElement().getStyle().setBorderWidth(2,PX);
//        outer.getElement().getStyle().setBorderColor("black");
//        outer.getElement().getStyle().setBorderStyle(BorderStyle.SOLID);
/*    {
      DivWidget rowOne = new DivWidget();
      rowOne.setHeight("40px");
      rowOne.setWidth(98 + "%");
      rowOne.getElement().getStyle().setBackgroundColor("#dff4fc");
      rowOne.addStyleName("blueRow");
      //addPresentation(attributes, rowOne);

      rowOne.add(getTitle(dialog));
      rowOne.getElement().getStyle().setMarginBottom(10, PX);
      outer.add(rowOne);
    }*/

    {
      DivWidget row = new DivWidget();
      row.addStyleName("cardBorderShadow");
      row.setHeight("100px");
      row.setWidth(ROW_WIDTH + "%");
      row.addStyleName("inlineFlex");
      {
        com.google.gwt.user.client.ui.Image flag = getFlag(dialog.getImageRef());
        flag.addStyleName("floatLeft");
        row.add(flag);
      }

      DivWidget vert = new DivWidget();
      vert.getElement().setId("vert");
      row.add(vert);
      vert.addStyleName("leftTenMargin");

      {
        DivWidget titleDiv = new DivWidget();
        // Style style = titleDiv.getElement().getStyle();
//        style.setBackgroundColor("#dff4fc");
//        style.setMarginBottom(0,PX);
//        style.setBorderColor("black");
//        style.setBorderStyle(Style.BorderStyle.SOLID);
//        style.setBorderWidth(1,PX);
        titleDiv.addStyleName("titleBlue");

        //  titleDiv.getElement().getStyle().setClear(Style.Clear.RIGHT);
        titleDiv.add(getFLTitle(dialog));
        vert.add(titleDiv);
      }

      {
        DivWidget titleDiv = new DivWidget();
        titleDiv.getElement().getStyle().setBackgroundColor("#dff4fc");
        //titleDiv.getElement().getStyle().setClear(Style.Clear.RIGHT);
        titleDiv.add(getHeading(5, dialog.getEnglish()));
        vert.add(titleDiv);
      }

      // row.add(getHeader(dialog));
      {
        DivWidget oreintDiv = new DivWidget();
        Heading w1 = new Heading(5, dialog.getOrientation());
        //   w1.addStyleName("rightAlign");
        //   w1.addStyleName("floatRight");
        //  w1.addStyleName("rightTenMargin");
        w1.addStyleName("wrapword");

        oreintDiv.add(w1);
        //   w1.setWidth("85%");
        //          w1.getElement().getStyle().setBackgroundColor("#dff4fc");
        vert.add(oreintDiv);
      }
      outer.add(row);
    }
    return outer;
  }

  @NotNull
  private Heading getFLTitle(IDialog dialog) {
    return getHeading(3, dialog.getForeignLanguage());
  }

  @NotNull
  private Heading getHeading(int size, String foreignLanguage) {
    Heading w1 = new Heading(size, foreignLanguage);//, dialog.getEnglish());
    w1.getElement().getStyle().setMarginTop(0, PX);
    w1.getElement().getStyle().setMarginBottom(5, PX);
    return w1;
  }

  private void addPresentation(List<ExerciseAttribute> attributes, DivWidget rowOne) {
    Heading w = new Heading(3, getAttrValue(attributes, FLPRESENTATION), getAttrValue(attributes, PRESENTATION));
    w.setWidth("49%");
    w.addStyleName("floatLeft");
    w.addStyleName("leftFiveMargin");
    // w.addStyleName("bottomFiveMargin");
    w.getElement().getStyle().setMarginTop(0, PX);

    rowOne.add(w);
  }


  @NotNull
  private com.google.gwt.user.client.ui.Image getFlag(String cc) {
    com.google.gwt.user.client.ui.Image image = new com.google.gwt.user.client.ui.Image(cc);
    image.setHeight(HEIGHT);
    image.setWidth(HEIGHT);
    return image;
  }

  private final Map<Integer, AlignmentOutput> alignments = new HashMap<>();

  @NotNull
  private DivWidget getTurns(IDialog dialog) {
    DivWidget rowOne = new DivWidget();
//    rowOne.addStyleName("cardBorderShadow");

    rowOne.setWidth(97 + "%");
    rowOne.getElement().getStyle().setMarginTop(10, PX);

    int size = dialog.getExercises().size();
    logger.info("dialog " + dialog);
    logger.info("size   " + size);

    List<String> speakers = dialog.getSpeakers();

    Map<String, List<ClientExercise>> speakerToEx = dialog.groupBySpeaker();
    String left = speakers.get(0);
    String right = speakers.get(1);
    List<ClientExercise> leftTurns = speakerToEx.get(left);
    List<ClientExercise> rightTurns = speakerToEx.get(right);

    logger.info("speakerToEx " + speakerToEx.keySet());
    //logger.info("right " + right + " rightTurns " + rightTurns);


    dialog.getExercises().forEach(clientExercise -> {
      // List<ExerciseAttribute> attributes = clientExercise.getAttributes();
      // logger.info("id " +clientExercise.getID() + " has " + attributes.size());
      // attributes.forEach(exerciseAttribute -> logger.info("Got " + exerciseAttribute));

      DialogExercisePanel<ClientExercise> widgets = new DialogExercisePanel<>(clientExercise, controller, null, alignments);
      widgets.addWidgets(true, false, PhonesChoices.HIDE);
      Style style = widgets.getElement().getStyle();
      if (rightTurns != null && rightTurns.contains(clientExercise)) {
        style.setFloat(Style.Float.RIGHT);
        style.setTextAlign(Style.TextAlign.RIGHT);
        style.setBackgroundColor(RIGHT_BKG_COLOR);
        style.setColor("white");
      } else {
        style.setFloat(Style.Float.LEFT);
        style.setTextAlign(Style.TextAlign.LEFT);
        style.setBackgroundColor(LEFT_COLOR);
      }
      style.setClear(Style.Clear.BOTH);

      //else {
      // widgets.addStyleName("leftspeech");
      widgets.addStyleName("bubble");
      {
        Style style2 = widgets.getFlClickableRow().getElement().getStyle();
        style2.setMarginLeft(15, Style.Unit.PX);
        style2.setMarginTop(7, Style.Unit.PX);
        //style2.setMarginBottom(7, Style.Unit.PX);
      }
      turns.add(widgets);
      //}
      rowOne.add(widgets);
//      widgets.setWidth("80%");
    });

    if (!turns.isEmpty()) {
      turns.get(0).getElement().getStyle().setBorderColor("green");
    }

    rowOne.getElement().getStyle().setMarginBottom(10, PX);
    return rowOne;
  }

  private List<DialogExercisePanel> turns = new ArrayList<>();

  @NotNull
  private DivWidget getControls() {
    DivWidget rowOne = new DivWidget();
//    rowOne.addStyleName("cardBorderShadow");

    rowOne.getElement().getStyle().setTextAlign(Style.TextAlign.CENTER);
    //rowOne.setWidth(97 + "%");
   // rowOne.getElement().getStyle().setMarginTop(10, PX);

    {
      Button widgets = new Button("", IconType.ARROW_LEFT, event -> gotGoBack());
      widgets.addStyleName("leftFiveMargin");
      widgets.addStyleName("rightTenMargin");
      rowOne.add(widgets);
    }

    {
      Button widgets = new Button("", IconType.BACKWARD, event -> gotBackward());
      widgets.addStyleName("leftFiveMargin");
      rowOne.add(widgets);
    }
    {
      Button widgets1 = new Button("", IconType.PLAY, event -> gotPlay());
      widgets1.addStyleName("leftFiveMargin");
      rowOne.add(widgets1);
    }
    {
      Button widgets2 = new Button("", IconType.FORWARD, event -> gotForward());
      widgets2.addStyleName("leftFiveMargin");
      rowOne.add(widgets2);
    }
  //  rowOne.getElement().getStyle().setMarginBottom(10, PX);
    return rowOne;
  }

  private void gotGoBack() {
    logger.info("got go back");
  }

  private void gotBackward() {
    logger.info("got backward");
  }

  private void gotPlay() {
    logger.info("got play");
  }

  private void gotForward() {
    logger.info("got forward");
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
