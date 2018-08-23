package mitll.langtest.client.banner;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PushButton;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.FacetExerciseList;
import mitll.langtest.client.list.ListOptions;
import mitll.langtest.client.project.ThumbnailChoices;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import mitll.langtest.shared.exercise.FilterResponse;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DialogExerciseList extends FacetExerciseList<IDialog, IDialog> {
  private final Logger logger = Logger.getLogger("DialogExerciseList");

  private static final int CHOICES_WIDTH = 970;

  private static final int MAX_LENGTH_ID = 19;
  private static final int NORMAL_MIN_HEIGHT = 67;
  private static final int LANGUAGE_SIZE = 6;

  private ThumbnailChoices thumbnailChoices = new ThumbnailChoices();

  DialogExerciseList(Panel topRow, Panel currentExercisePanel, INavigation.VIEWS instanceName, DivWidget listHeader,
                     ExerciseController controller) {
    super(topRow, currentExercisePanel, controller, new ListOptions(instanceName), listHeader, false);
  }

  @Override protected int getFirstPageSize() {  return 10;  }

  protected void getTypeToValues(Map<String, String> typeToSelection, int userListID) {
    if (!isThereALoggedInUser()) return;
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
/*
    logger.info("getExerciseIDs " +
        "\n\trequest " + request +
        "\n\t ex     " + exerciseID + " type " + typeToSection);*/

    if (controller.getUser() > 0) {
      controller.getDialogService().getDialogs(request,
          new SetExercisesCallback("" + "_" + typeToSection.toString(), prefix, exerciseID, request));
    }
  }

  @Override
  protected void getFullExercises(Collection<Integer> visibleIDs, int currentReq, Collection<Integer> requested, List<IDialog> alreadyFetched) {
    //  logger.info("getFullExercises " + visibleIDs);
    controller.getDialogService().getDialogs(new ExerciseListRequest(),
        new AsyncCallback<ExerciseListWrapper<IDialog>>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(ExerciseListWrapper<IDialog> result) {
            List<IDialog> toShow = result.getExercises().stream().filter(iDialog -> visibleIDs.contains(iDialog.getID())).collect(Collectors.toList());

            List<Integer> ordered = new ArrayList<>(visibleIDs);
            toShow.sort((o1, o2) -> {
              int i = ordered.indexOf(o1.getID());
              int j = ordered.indexOf(o2.getID());
              return Integer.compare(i, j);
            });
            showExerciesForCurrentReq(toShow, incrReq());
          }
        });
  }

  @Override
  protected void goGetNextPage() {
  }

  /**
   * @param result
   * @param reqID
   * @param exerciseContainer
   * @see #reallyShowExercises(Collection, int)
   */
  protected void populatePanels(Collection<IDialog> result, int reqID, DivWidget exerciseContainer) {
    //  long then = System.currentTimeMillis();
    exerciseContainer.add(showProjectChoices(result));
    //  long now = System.currentTimeMillis();
  }

  private Section showProjectChoices(Collection<IDialog> result) {
    // logger.info("showProjectChoices choices # = " + result.size() + " : nest level " + nest);
    final Section section = thumbnailChoices.getScrollingSection();

    {
      final Container flags = new Container();
      flags.setWidth(CHOICES_WIDTH + "px");
      flags.add(addFlags(result));
      section.add(flags);
    }

    return section;
  }

  private Thumbnails addFlags(Collection<IDialog> dialogs) {
    Thumbnails current = new Thumbnails();
    current.getElement().getStyle().setMarginBottom(70, Style.Unit.PX);

    dialogs
        .forEach(dialog -> {

//          logger.info("Got " + dialog.getID() + " " + dialog.getEnglish() + " " + dialog.getAttributes());
          Panel langIcon = getImageAnchor(dialog);
          if (langIcon != null) {
            current.add(langIcon);
          }
        });

    return current;
  }

  private Panel getImageAnchor(IDialog dialog) {
    Thumbnail thumbnail = thumbnailChoices.getThumbnail();

    // logger.info("show image " + imageRef);
    PushButton button = new PushButton(getFlag(dialog.getImageRef()));
    button.addClickHandler(clickEvent -> gotClickOnDialog(dialog));
    thumbnail.add(button);

    {
      Map<String, String> props = new HashMap<>(dialog.getUnitToValue());
      dialog.getAttributes().forEach(attr ->
      {
        props.put(getProperty(attr), attr.getValue());
        if (isTitle(attr)) {
          props.put("english", dialog.getEnglish());
        }
      });

      thumbnailChoices.addPopover(button, props, Placement.BOTTOM);
    }

    {
      DivWidget horiz = new DivWidget();
      horiz.getElement().getStyle().setProperty("minHeight", NORMAL_MIN_HEIGHT + "px"); // so they wrap nicely
      horiz.add(getContainerWithButtons(dialog));

      thumbnail.add(horiz);
    }
    return thumbnail;
  }

  private String getProperty(ExerciseAttribute attr) {
    return isTitle(attr) ? "title" : attr.getProperty();
  }

  private boolean isTitle(ExerciseAttribute attr) {
    return attr.getProperty().equals(IDialog.METADATA.FLTITLE.toString().toLowerCase());
  }

  @NotNull
  private com.google.gwt.user.client.ui.Image getFlag(String cc) {
    Image image = new Image(cc);
    image.setHeight("150px");
    image.setWidth("150px");
    return image;
  }

  @NotNull
  private DivWidget getContainerWithButtons(IDialog dialog) {
    DivWidget container = new DivWidget();

    container.add(getLabel(thumbnailChoices.truncate(dialog.getForeignLanguage(), MAX_LENGTH_ID), dialog.getEnglish()));
    container.setWidth("100%");
    container.addStyleName("floatLeft");

    return container;
  }

  @NotNull
  private Heading getLabel(String name, String statusText) {
    Heading label = thumbnailChoices.getChoiceLabel(LANGUAGE_SIZE, name);
    label.setSubtext(statusText);
    return label;
  }

  /**
   * Will push be bad? Will other parts wake up?
   * Maybe replace item?
   *
   * @param dialog
   */
  private void gotClickOnDialog(IDialog dialog) {
    controller.getNavigation().showDialogIn(dialog.getID(), INavigation.VIEWS.LISTEN);
  }
}
