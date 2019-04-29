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

package mitll.langtest.server.database.userexercise;

import mitll.langtest.server.database.exercise.DBExerciseDAO;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.SlickExerciseAttribute;
import mitll.npdata.dao.userexercise.ExerciseAttributeDAOWrapper;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.*;

public class AttributeHelper implements IAttributeDAO {
  private final ExerciseAttributeDAOWrapper attributeDAOWrapper;

  AttributeHelper(ExerciseAttributeDAOWrapper attributeDAOWrapper) {
    this.attributeDAOWrapper = attributeDAOWrapper;
  }

  public void createTable() {
    attributeDAOWrapper.createTable();
  }

  public String getName() {
    return attributeDAOWrapper.getName();
  }

  @Override
  public boolean updateProject(int oldID, int newprojid) {
    return attributeDAOWrapper.updateProject(oldID, newprojid) > 0;
  }

  /**
   * If the attribute already exists, just return it's ID.
   *
   * @param projid
   * @param now
   * @param userid
   * @param attribute
   * @param checkExists
   * @return
   * @see mitll.langtest.server.database.copy.ExerciseCopy#addAttributes
   */
  public int findOrAddAttribute(int projid,
                                long now,
                                int userid,
                                ExerciseAttribute attribute,
                                boolean checkExists) {
    if (checkExists) {
      Collection<SlickExerciseAttribute> exists = attributeDAOWrapper.exists(new SlickExerciseAttribute(-1,
          projid,
          userid,
          new Timestamp(now),
          attribute.getProperty(), attribute.getValue(), attribute.isFacet()));
      if (exists.isEmpty()) {
        return insertAttribute(projid, now, userid, attribute.getProperty(), attribute.getValue(), attribute.isFacet());
      } else {
        return exists.iterator().next().id();
      }
    } else {
      return insertAttribute(projid, now, userid, attribute.getProperty(), attribute.getValue(), attribute.isFacet());
    }
  }

  private int insertAttribute(int projid,
                              long now,
                              int userid,
                              String property,
                              String value, boolean facet) {
    return attributeDAOWrapper.insert(new SlickExerciseAttribute(-1,
        projid,
        userid,
        new Timestamp(now),
        property,
        value,
        facet));
  }

  /**
   * E.g. grammar or topic or sub-topic
   * <p>
   * Make sure the
   *
   * @param projid
   * @return
   * @see DBExerciseDAO#getAttributeTypes
   */
  @Override
  public Collection<String> getAttributeTypes(int projid) {
    Set<String> unique = new TreeSet<>();
    attributeDAOWrapper
        .allByProjectCheckFacet(projid)
        .forEach(slickExerciseAttribute -> unique.add(slickExerciseAttribute.property()));
    return unique;
  }

  /**
   * @see DBExerciseDAO#readExercises
   * @param projid
   * @return
   */
  @Override
  public Map<Integer, ExerciseAttribute> getIDToPair(int projid) {
    Map<Integer, ExerciseAttribute> pairMap = new HashMap<>();
    Map<String, ExerciseAttribute> known = new HashMap<>();
    getAllByProject(projid).forEach(p -> pairMap.put(p.id(), makeOrGet(known, p)));
    return pairMap;
  }

  @Override
  public boolean update(int id, String value) {
    return attributeDAOWrapper.update(id, value) > 0;
  }


  /**
   * @param known
   * @param p
   * @return
   * @see #getIDToPair
   */
  @NotNull
  private ExerciseAttribute makeOrGet(Map<String, ExerciseAttribute> known, SlickExerciseAttribute p) {
    String property = p.property();
    String value = p.value();

    String key = property + "-" + value;
    ExerciseAttribute attribute;
    if (known.containsKey(key)) {
      attribute = known.get(key);
    } else {
      attribute = new ExerciseAttribute(property, value, p.facet()).setId(p.id());
      known.put(key, attribute);
    }
    return attribute;
  }

  /**
   * @param projid
   * @return
   * @see #getIDToPair
   */
  private Collection<SlickExerciseAttribute> getAllByProject(int projid) {
    return attributeDAOWrapper.allByProject(projid);
  }
}
