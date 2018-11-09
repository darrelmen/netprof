package mitll.langtest.server.database.exercise;

import java.util.Comparator;

/**
 * These are fixed!
 *
 * @see DBExerciseDAO#setRootTypes
 * @see SectionHelper#reorderTypes
 */
public enum Facet implements Comparator<Facet> {
  SEMESTER("Semester", 0),
  TOPIC("Topic", 1),
  SUB_TOPIC("Sub-topic", "subtopic", 2),
  GRAMMAR("Grammar", 3),
  DIALECT("Dialect", 4),
  DIFFICULTY("Difficulty", 5);

  private final String name;
  private String alt;
  private final int order;

  Facet(String name, int order) {
    this.name = name;
    this.order = order;
  }

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

  public int getOrder() {
    return order;
  }

  @Override
  public int compare(Facet o1, Facet o2) {
    return Integer.compare(o1.order, o2.order);
  }
}
