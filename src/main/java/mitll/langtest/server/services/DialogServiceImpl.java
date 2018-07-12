/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.services;

import mitll.langtest.client.services.DialogService;
import mitll.langtest.server.database.exercise.ISection;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.scoring.AlignmentHelper;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import mitll.langtest.shared.exercise.FilterRequest;
import mitll.langtest.shared.exercise.FilterResponse;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Probably going to need to parameterize by exercises?
 *
 * @param <T>
 */
@SuppressWarnings("serial")
public class DialogServiceImpl<T extends IDialog> extends MyRemoteServiceServlet implements DialogService {
  private static final Logger logger = LogManager.getLogger(DialogServiceImpl.class);


  private static final String ANY = "Any";

  /**
   * @param request
   * @return
   * @see mitll.langtest.client.list.FacetExerciseList#getTypeToValues
   */
  public FilterResponse getTypeToValues(FilterRequest request) throws DominoSessionException {
    ISection<IDialog> sectionHelper = getDialogSectionHelper();
    if (sectionHelper == null) {
      logger.info("getTypeToValues no reponse...");// + "\n\ttype->selection" + typeToSelection);
      return new FilterResponse();
    } else {
      FilterResponse response = sectionHelper.getTypeToValues(request, false);

      User userFromSession = getUserFromSession();

      if (userFromSession != null) {
//        logger.info("getTypeToValues got " + userFromSession);
        //       logger.info("getTypeToValues isRecordRequest " + request.isRecordRequest());
        //  int userFromSessionID = userFromSession.getID();
        //  int projectID = getProjectIDFromUser(userFromSessionID);

        Map<String, Collection<String>> typeToSelection = new HashMap<>();
        request.getTypeToSelection().forEach(pair -> {
          String value1 = pair.getValue();
          if (!value1.equalsIgnoreCase(ANY)) {
            typeToSelection.put(pair.getProperty(), Collections.singleton(value1));
          }
        });


      }

      return response;
    }
    //}
  }

  @Override
  public ExerciseListWrapper<IDialog> getDialogs(ExerciseListRequest request) throws DominoSessionException {
    ISection<IDialog> sectionHelper = getDialogSectionHelper();
    if (sectionHelper == null) {
      logger.info("getTypeToValues no reponse...");// + "\n\ttype->selection" + typeToSelection);
      return new ExerciseListWrapper<>();
    } else {

      int userIDFromSessionOrDB = getUserIDFromSessionOrDB();


      if (userIDFromSessionOrDB != -1) {
        List<IDialog> dialogList = getDialogs(request, sectionHelper, userIDFromSessionOrDB);

        return new ExerciseListWrapper<>(request.getReqID(),
            dialogList,
            null, new HashMap<>()
        );

      } else {
        return new ExerciseListWrapper<>();
      }
    }
  }

  @Override
  public IDialog getDialog(int id) throws DominoSessionException {
    List<IDialog> iDialogs = getDialogs(getUserIDFromSessionOrDB());
    List<IDialog> collect = iDialogs.stream().filter(iDialog -> iDialog.getID() == id).collect(Collectors.toList());
    IDialog iDialog = collect.isEmpty() ? iDialogs.iterator().next() : collect.iterator().next();

    logger.info("get dialog " + id + "\n\treturns " + iDialog);

    int projid = iDialog.getProjid();
    Project project = db.getProject(projid);
    String language = project.getLanguage();

    iDialog.getExercises().forEach(clientExercise ->
        db.getAudioDAO().attachAudioToExercise(clientExercise, language, new HashMap<>())
    );

    new AlignmentHelper(serverProps, db.getRefResultDAO()).addAlignmentOutput(projid, project, iDialog.getExercises());

    return iDialog;
  }

  private List<IDialog> getDialogs(ExerciseListRequest request, ISection<IDialog> sectionHelper, int userIDFromSessionOrDB) {
    return (request.getTypeToSelection().isEmpty()) ?
        getDialogs(userIDFromSessionOrDB) :
        new ArrayList<>(sectionHelper.getExercisesForSelectionState(request.getTypeToSelection()));
  }

  private List<IDialog> getDialogs(int userIDFromSessionOrDB) {
    List<IDialog> dialogList = new ArrayList<>();
    {
      int projectIDFromUser = getProjectIDFromUser(userIDFromSessionOrDB);
      if (projectIDFromUser != -1) {
        dialogList = getProject(projectIDFromUser).getDialogs();
      }
    }
    return dialogList;
  }
}