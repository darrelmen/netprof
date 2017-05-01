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

package mitll.langtest.server.autocrt.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Just for AutoCRT
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/30/15.
*/
public class ExerciseExport {
  /**
   * Exercise id
   */
  public final String id;
  /**
   * Answer key answers
   */
  public List<String> key = new ArrayList<String>();
  /**
   * List of response-grade pairs
   */
  public final List<ResponseAndGrade> rgs = new ArrayList<ResponseAndGrade>();

  /**
   * @see mitll.langtest.server.database.Export#populateIdToExportMap
   * @param id
   * @param key
   */
  public ExerciseExport(String id, String key) {
    this.id = id;
    List<String> c = Arrays.asList(key.split("\\|\\|"));
    for (String answer : c) {
      String trim = answer.trim();
      if (!trim.isEmpty()) {
        this.key.add(trim);
      }
    }
  }

  /**
   * @param response
   * @param grade
   * @param maxGrade
   * @see mitll.langtest.server.database.Export#addPredefinedAnswers
   * @see mitll.langtest.server.database.Export#getExports
   */
  public ResponseAndGrade addRG(String response, int grade, int maxGrade) {
    ResponseAndGrade e = new ResponseAndGrade(response, grade, maxGrade);
    rgs.add(e);
    return e;
  }

  public String toString() {
    return "id " + id + " " + key.size() + " keys : " + "" +
        new HashSet<String>(key) +
        " and " + rgs.size() + " responses "
        + new HashSet<ResponseAndGrade>(rgs);
  }
}
