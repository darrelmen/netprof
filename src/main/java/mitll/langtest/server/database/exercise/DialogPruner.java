/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.project.Project;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.HasID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class DialogPruner {
  private static final Logger logger = LogManager.getLogger(DialogPruner.class);
  private static final boolean DEBUG = false;

  public List<CommonExercise> getNoDialogExercises(List<CommonExercise> toFilter,
                                                   DatabaseServices databaseServices, int projectID) {
    return getNoDialogExercises(toFilter, getDialogExercises(databaseServices, projectID));
  }

  @NotNull
  private <T extends HasID> List<CommonExercise> getNoDialogExercises(List<CommonExercise> toFilter,
                                                                      List<T> dialogExercises) {
    long then = System.currentTimeMillis();
    Set<Integer> exidFromDialogs = getDialogExerciseIDs(dialogExercises);

    if (DEBUG) {
      Set<Integer> willRemove = new TreeSet<>();

      toFilter.forEach(commonExercise -> {
        if (exidFromDialogs.contains(commonExercise.getID())) {
          willRemove.add(commonExercise.getID());
        }
      });

      logger.info("getNoDialogExercises : remove " + willRemove.size() + " exids given " + exidFromDialogs.size());
    }

    int before = toFilter.size();
    toFilter = toFilter.stream().filter(commonExercise -> !exidFromDialogs.contains(commonExercise.getID())).collect(Collectors.toList());
    int after = toFilter.size();
    // if (after != before) {
    if (DEBUG) logger.info("getNoDialogExercises removed " + (before - after) + " dialog exercises from " + before);
    // }
    long now = System.currentTimeMillis();
    if (now - then > 20) logger.info("getNoDialogExercises took " + (now - then) + " to filter out " + (before - after));
    return toFilter;
  }

  @NotNull
  private <T extends HasID> Set<Integer> getDialogExerciseIDs(List<T> dialogExercises) {
    Set<Integer> exidFromDialogs = new HashSet<>();

    dialogExercises.forEach(clientExercise -> exidFromDialogs.add(clientExercise.getID()));
    return exidFromDialogs;
  }

  @NotNull
  private List<CommonExercise> getDialogExercises(DatabaseServices databaseServices, int projectID) {
    List<CommonExercise> filtered = new ArrayList<>();
    Collection<IDialog> dialogs = getDialogs(databaseServices, projectID);
    dialogs.forEach(iDialog -> filtered.addAll(toCommon(iDialog.getBothExercisesAndCoreNoEmpty())));
    if (dialogs.isEmpty()) {
      logger.info("getDialogExercises no dialogs in " + projectID);
    } else {
      logger.info("getDialogExercises ok found " + filtered.size() + " for " + projectID + " and " + dialogs.size() + " dialogs");
    }

    return filtered;
  }

  private Collection<IDialog> getDialogs(DatabaseServices databaseServices, int projectID) {
    Project project = databaseServices.getProject(projectID);
    return project == null ? new ArrayList<>() : project.getDialogs();
  }

  @NotNull
  private List<CommonExercise> toCommon(Collection<ClientExercise> exercises) {
    List<CommonExercise> commonExercises = new ArrayList<>(exercises.size());
    exercises.forEach(clientExercise -> commonExercises.add(clientExercise.asCommon()));
    return commonExercises;
  }
}
