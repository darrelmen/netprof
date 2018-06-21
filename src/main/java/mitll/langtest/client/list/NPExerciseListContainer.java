package mitll.langtest.client.list;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import mitll.langtest.client.analysis.AudioExampleContainer;
import mitll.langtest.client.exercise.PagingContainer;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.project.ProjectStartupInfo;

class NPExerciseListContainer extends PagingContainer<CommonShell> {
//  private final Logger logger = Logger.getLogger("NPExerciseListContainer");

  private final NPExerciseList exerciseList;
  private final PagingExerciseList<CommonShell, CommonExercise> outer;

  NPExerciseListContainer(NPExerciseList exerciseList, boolean isRecorder,
                          boolean showFirstNotCompleted, PagingExerciseList<CommonShell, CommonExercise> outer) {
    super(exerciseList.controller, exerciseList.getVerticalUnaccountedFor(), isRecorder, showFirstNotCompleted);
    this.exerciseList = exerciseList;
    this.outer = outer;
  }

  @Override
  protected int getNumTableRowsGivenScreenHeight() {
    int pageSize = exerciseList.getPageSize();
    return (pageSize == -1) ? super.getNumTableRowsGivenScreenHeight() : pageSize;
  }

  @Override
  public void gotClickOnItem(CommonShell e) {
    outer.gotClickOnItem(e);
  }

  @Override
  protected CellTable.Resources chooseResources() {
    ProjectStartupInfo projectStartupInfo = controller.getProjectStartupInfo();
    boolean isRTL = projectStartupInfo != null && projectStartupInfo.getLanguageInfo().isRTL();

    CellTable.Resources o;
    if (isRTL) {   // so when we truncate long entries, the ... appears on the correct end
      // logger.info("simplePaging : chooseResources RTL - content");
      if (controller.getLanguage().equalsIgnoreCase("urdu")) {
        o = GWT.create(AudioExampleContainer.UrduTableResources.class);
      } else {
        o = GWT.create(NPExerciseListContainer.RTLTableResources.class);
      }
    } else {
      // logger.info("simplePaging : chooseResources LTR - content");
      o = GWT.create(NPExerciseListContainer.TableResources.class);
    }
    return o;
  }

  public interface TableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "ExerciseCellTableStyleSheet.css"})
    NPExerciseListContainer.TableResources.TableStyle cellTableStyle();
  }

  public interface RTLTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "RTLExerciseCellTableStyleSheet.css"})
    NPExerciseListContainer.RTLTableResources.TableStyle cellTableStyle();
  }

  public interface UrduTableResources extends CellTable.Resources {
    /**
     * The styles applied to the table.
     */
    interface TableStyle extends CellTable.Style {
    }

    @Override
    @Source({CellTable.Style.DEFAULT_CSS, "UrduExerciseCellTableStyleSheet.css"})
    NPExerciseListContainer.RTLTableResources.TableStyle cellTableStyle();
  }
}
