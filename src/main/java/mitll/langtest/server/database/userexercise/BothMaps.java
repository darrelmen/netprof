package mitll.langtest.server.database.userexercise;

import java.util.Map;

public class BothMaps {
  private final Map<String, Integer> oldToNew;
  //  private final Map<Integer, Integer> dominoToNew;

  BothMaps(Map<String, Integer> oldToNew) {//}, Map<Integer, Integer> dominoToNew) {
    this.oldToNew = oldToNew;
    //  this.dominoToNew = dominoToNew;
  }

  public Map<String, Integer> getOldToNew() {
    return oldToNew;
  }

/*    public Map<Integer, Integer> getDominoToNew() {
    return dominoToNew;
  }*/
}
