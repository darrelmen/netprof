package mitll.langtest.server.database.userexercise;

import mitll.langtest.server.database.exercise.DBExerciseDAO;
import mitll.langtest.shared.exercise.ExerciseAttribute;
import mitll.npdata.dao.SlickExerciseAttribute;
import mitll.npdata.dao.userexercise.ExerciseAttributeDAOWrapper;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.*;

public class AttributeHelper implements IAttribute {
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
  public boolean updateProject(int oldID, int newprojid) { return attributeDAOWrapper.updateProject(oldID, newprojid) > 0; }

  /**
   * If the attribute already exists, just return it's ID.
   *
   * @see mitll.langtest.server.database.copy.ExerciseCopy#addAttributes
   * @param projid
   * @param now
   * @param userid
   * @param attribute
   * @param checkExists
   * @return
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
          attribute.getProperty(), attribute.getValue(),attribute.isFacet()));
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
   *
   * Make sure the
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

  @Override
  public Map<Integer, ExerciseAttribute> getIDToPair(int projid) {
    Map<Integer, ExerciseAttribute> pairMap = new HashMap<>();
    Map<String, ExerciseAttribute> known = new HashMap<>();
    getAllByProject(projid).forEach(p -> pairMap.put(p.id(), makeOrGet(known, p)));
    return pairMap;
  }

  @NotNull
  private ExerciseAttribute makeOrGet(Map<String, ExerciseAttribute> known, SlickExerciseAttribute p) {
    String property = p.property();
    String value = p.value();

    String key = property + "-" + value;
    ExerciseAttribute attribute;
    if (known.containsKey(key)) {
      attribute = known.get(key);
    } else {
      attribute = new ExerciseAttribute(property, value, p.facet());
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
