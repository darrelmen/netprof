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

package mitll.langtest.server.database.postgres;

import mitll.langtest.server.database.BaseTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

public class PhoneTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(PhoneTest.class);

  @Test
  public void testWorst() {
  //  DatabaseImpl spanish = getDatabase("spanish");

/*    {
      IPhoneDAO dao = spanish.getPhoneDAO();

      Result first = spanish.getResultDAO().getResultByID(3754);

      logger.info("For " + first);
      Set<String> singleton = Collections.singleton(first.getExerciseID());
      JSONObject worstPhonesJson = dao.getWorstPhonesJson(first.getUserid(), singleton, new HashMap<>());
      logger.info("got\n" + worstPhonesJson);
      logger.info("len " + worstPhonesJson.toString().length());

      if (false) {
        Set<Integer> singleton1 = Collections.singleton(first.getUniqueID());
        PhoneReport worstPhonesForResults = dao.getWorstPhonesForResults(first.getUserid(), singleton1, new HashMap<>());
        logger.info("got " + worstPhonesForResults);
      }
    }*/
/*    {
      PhoneDAO phoneDAO = new PhoneDAO(spanish);

      ResultDAO resultDAO = new ResultDAO(spanish);
      //  List<Result> results = resultDAO.getResults();

      Result truth = resultDAO.getResultByID(41303);
      logger.info("truth " + truth);

      List<String> ts = Collections.singletonList(truth.getExerciseID());
      JSONObject worstPhonesJsonTruth = phoneDAO.getWorstPhonesJson(truth.getUserid(), ts, new HashMap<>());
      logger.info("truth json " + worstPhonesJsonTruth);
      logger.info("len " + worstPhonesJsonTruth.toString().length());

      if (false) {
        logger.info("truth report " + phoneDAO.getWorstPhonesForResults(truth.getUserid(), Arrays.asList(truth.getUniqueID()), new HashMap<>()));
      }
    }*/
  }

/*  @Test
  public void testWorst2() {
    DatabaseImpl spanish = getDatabase("spanish");

    {
      IPhoneDAO dao = spanish.getPhoneDAO();

      Result first = spanish.getResultDAO().getResultByID(3754);

      logger.info("For " + first);


      Set<Integer> singleton1 = Collections.singleton(first.getUniqueID());
      PhoneReport worstPhonesForResults = dao.getWorstPhonesForResults(first.getUserid(), singleton1, new HashMap<>(), language);
      logger.info("got " + worstPhonesForResults);
    }
    {
      PhoneDAO phoneDAO = new PhoneDAO(spanish);

      ResultDAO resultDAO = new ResultDAO(spanish);

      Result truth = resultDAO.getResultByID(41303);
      logger.info("truth " + truth);
      logger.info("truth report " + phoneDAO.getWorstPhonesForResults(truth.getUserid(), Arrays.asList(truth.getUniqueID()), new HashMap<>(), language));
    }
  }*/
}
