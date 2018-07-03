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
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.FilterRequest;
import mitll.langtest.shared.exercise.FilterResponse;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Probably going to need to parameterize by exercises?
 * @param <T>
 */
@SuppressWarnings("serial")
public class DialogServiceImpl<T extends IDialog> extends MyRemoteServiceServlet implements DialogService  {
  private static final Logger logger = LogManager.getLogger(DialogServiceImpl.class);

  private static final int SLOW_EXERCISE_EMAIL = 2000;
  private static final int SLOW_MILLIS = 50;
  private static final int WARN_DUR = 100;

  private static final int MIN_DEBUG_DURATION = 30;
  private static final int MIN_WARN_DURATION = 1000;
  private static final String LISTS = "Lists";

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_ID_LOOKUP = false;

  private static final boolean USE_PHONE_TO_DISPLAY = true;
  private static final boolean WARN_MISSING_REF_RESULT = false;
  private static final String RECORDED1 = "Recorded";
  private static final String RECORDED = RECORDED1;
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
        int userFromSessionID = userFromSession.getID();
        int projectID = getProjectIDFromUser(userFromSessionID);

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
}