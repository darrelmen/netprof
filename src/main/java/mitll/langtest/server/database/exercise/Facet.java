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

import java.util.Comparator;

/**
 * These are fixed!
 *
 * @see DBExerciseDAO#setRootTypes
 * @see ISection#reorderTypes
 */
public enum Facet implements Comparator<Facet> {
  SEMESTER("Semester", 0),//, true),
  TOPIC("Topic", 1),
  SUB_TOPIC("Sub-topic", "subtopic", 2),
  GRAMMAR("Grammar", 3),
  DIALECT("Dialect", 4),
  DIFFICULTY("Difficulty", 5);

  private final String name;
  private String alt;
  private final int order;
  private boolean alsoProjectType = false;

  Facet(String name, int order) {
    this.name = name;
    this.order = order;
  }

//  Facet(String name, int order, boolean alsoProjectType) {
//    this.name = name;
//    this.order = order;
//    this.alsoProjectType = alsoProjectType;
//  }

  Facet(String name, String alt, int order) {
    this.name = name;
    this.alt = alt;
    this.order = order;
  }

  public String toString() {
    return name;
  }

  public String getName() {
    return name;
  }

  public String getAlt() {
    return alt;
  }

/*
  public int getOrder() {
    return order;
  }
*/

  @Override
  public int compare(Facet o1, Facet o2) {
    return Integer.compare(o1.order, o2.order);
  }

//  public boolean isAlsoProjectType() {
//    return alsoProjectType;
//  }
}
