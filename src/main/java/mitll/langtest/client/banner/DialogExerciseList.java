package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.FacetExerciseList;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DialogExerciseList<T extends CommonShell & ScoredExercise> extends FacetExerciseList<T, IDialog> {
  public static final int MAX_LENGTH_ID = 55;//35;// 23;
  private final Logger logger = Logger.getLogger("DialogExerciseList");
  private static final int CHOICE_WIDTH = 170;//180;//190;//195;
  private static final int NORMAL_MIN_HEIGHT = 67;
  private static final int LANGUAGE_SIZE = 6;

  public DialogExerciseList(Panel topRow, Panel currentExercisePanel, String instanceName, DivWidget listHeader,
                            ExerciseController controller) {
    super(topRow, currentExercisePanel, controller, new ListOptions(instanceName), listHeader, false);
  }

  protected void getTypeToValues(Map<String, String> typeToSelection, int userListID) {
    if (!isThereALoggedInUser()) return;

    //List<Pair> pairs = getPairs(typeToSelection);
    //  logger.info("getTypeToValues request " + pairs + " list " + userListID);

    final long then = System.currentTimeMillis();

    controller.getDialogService().getTypeToValues(getFilterRequest(userListID, getPairs(typeToSelection)),
        new AsyncCallback<FilterResponse>() {
          @Override
          public void onFailure(Throwable caught) {
            if (caught instanceof DominoSessionException) {
              logger.info("getTypeToValues : got " + caught);
            }
            controller.handleNonFatalError(GETTING_TYPE_VALUES, caught);
          }

          /**
           * fixes downstream selections that no longer make sense.
           * @param response
           */
          @Override
          public void onSuccess(FilterResponse response) {
            gotFilterResponse(response, then, typeToSelection);
          }
        });
  }

  protected void getExerciseIDs(Map<String, Collection<String>> typeToSection,
                                String prefix,
                                int exerciseID,
                                ExerciseListRequest request) {
    waitCursorHelper.scheduleWaitTimer();

    logger.info("getExerciseIDs " +
        "\n\trequest " + request +
        "\n\t ex     " + exerciseID + " type " + typeToSection);

    if (controller.getUser() > 0) {
      controller.getDialogService().getDialogs(request,
          new AsyncCallback<ExerciseListWrapper<IDialog>>() {
            @Override
            public void onFailure(Throwable caught) {
              waitCursorHelper.showFinished();
              logger.info("getExerciseIDs got FAILURE " + caught);

            }

            @Override
            public void onSuccess(ExerciseListWrapper<IDialog> result) {
              waitCursorHelper.showFinished();
              logger.info("getExerciseIDs got back " + result.getExercises().size());

              //rememberExercises(result.getExercises());

              showExerciesForCurrentReq(result.getExercises(), incrReq());
            }
          });
    }

//    if (controller.getUser() > 0) {
//      // final long then = System.currentTimeMillis();
//      service.getExerciseIds(
//          request,
//          new SetExercisesCallback(userListID + "_" + typeToSection.toString(), prefix, exerciseID, request));
//    }
  }

  @Override
  protected void getFullExercises(Collection<Integer> visibleIDs, int currentReq, Collection<Integer> requested, List<IDialog> alreadyFetched) {
    logger.info("getFullExercises " + visibleIDs);
    controller.getDialogService().getDialogs(new ExerciseListRequest(),
        new AsyncCallback<ExerciseListWrapper<IDialog>>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(ExerciseListWrapper<IDialog> result) {

          }
        });
  }

  @Override
  protected void goGetNextPage() {

  }


  protected void populatePanels(Collection<IDialog> result, int reqID, DivWidget exerciseContainer) {
    long then = System.currentTimeMillis();
    exerciseContainer.add(showProjectChoices(result));
    long now = System.currentTimeMillis();


  }

  private Section showProjectChoices(Collection<IDialog> result) {
    // logger.info("showProjectChoices choices # = " + result.size() + " : nest level " + nest);
    final Section section = new Section("section");
    section.getElement().getStyle().setOverflow(Style.Overflow.SCROLL);
    section.setHeight("100%");
    //section.add(getHeader(result, nest));

    {
      final Container flags = new Container();
      flags.setWidth("970px");
      //final DivWidget flags = new DivWidget();
      flags.add(addFlags(result));
      section.add(flags);
    }

    return section;
  }

  private Thumbnails addFlags(Collection<IDialog> result) {
    Thumbnails current = new Thumbnails();
    current.getElement().getStyle().setMarginBottom(70, Style.Unit.PX);

    result
        .forEach(project -> {
          Panel langIcon = getImageAnchor(project.getEnglish(), project);
          if (langIcon != null) {
            current.add(langIcon);
          }
        });

    return current;
  }

  private Panel getImageAnchor(final String name, IDialog dialog) {
    Thumbnail thumbnail = new Thumbnail();
    thumbnail.setWidth(CHOICE_WIDTH + "px");
    thumbnail.setSize(2);

    String imageRef = dialog.getImageRef();

    // logger.info("show image " + imageRef);
    PushButton button = new PushButton(getFlag(imageRef));
    final int projid = dialog.getID();
    button.addClickHandler(clickEvent -> gotClickOnDialog(name, dialog, projid, 1));
    thumbnail.add(button);

    DivWidget horiz = new DivWidget();
    horiz.getElement().getStyle().setProperty("minHeight", NORMAL_MIN_HEIGHT + "px"); // so they wrap nicely
    thumbnail.add(horiz);

    horiz.add(getContainerWithButtons(name));

    return thumbnail;
  }

  @NotNull
  private com.google.gwt.user.client.ui.Image getFlag(String cc) {
    Image image = new Image(cc);
    image.setHeight("150px");
    image.setWidth("150px");
    return image;
  }

  @NotNull
  private DivWidget getContainerWithButtons(String name) {
    DivWidget container = new DivWidget();
    Heading label;

    container.add(label = getLabel(truncate(name, MAX_LENGTH_ID), ""));
    container.setWidth("100%");
    container.addStyleName("floatLeft");


    return container;
  }

  @NotNull
  private Heading getLabel(String name, String statusText) {
    Heading label = new Heading(LANGUAGE_SIZE, name);
    label.addStyleName("floatLeft");
    label.setWidth("100%");
    label.getElement().getStyle().setLineHeight(25, Style.Unit.PX);

    {
      Widget subtitle = label.getWidget(0);
      subtitle.addStyleName("floatLeft");
      subtitle.setWidth("100%");
      subtitle.addStyleName("topFiveMargin");
    }

    //showProjectStatus(status, label);
    label.setSubtext(statusText);

    label.addStyleName("floatLeft");
    return label;
  }

  @NotNull
  private String truncate(String columnText, int maxLengthId) {
    if (columnText.length() > maxLengthId) columnText = columnText.substring(0, maxLengthId - 3) + "...";
    return columnText;
  }

  /**
   * Will push be bad? Will other parts wake up?
   * Maybe replace item?
   *
   * @param name
   * @param dialog
   * @param projid
   * @param nest
   */
  private void gotClickOnDialog(String name, IDialog dialog, int projid, int nest) {
    logger.info("got click on " + name);
//    History.replaceItem(SelectionState.DIALOG + "=" + dialog.getID(), false);
    controller.getNavigation().showDialogIn(dialog.getID(), INavigation.VIEWS.LISTEN);
  }
}
