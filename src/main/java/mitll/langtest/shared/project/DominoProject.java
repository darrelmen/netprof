package mitll.langtest.shared.project;

import com.google.gwt.user.client.rpc.IsSerializable;

public class DominoProject implements IsSerializable {
  protected String firstType = "";
  protected String secondType = "";
  private String name = "";
  private int dominoID;

  public DominoProject() {
  }

  public DominoProject(int dominoID, String name, String first, String secondType) {
    this.dominoID = dominoID;
    this.name = name;
    this.firstType = first;
    this.secondType = secondType;
  }

  public String getName() {
    return name;
  }

  public int getDominoID() {
    return dominoID;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDominoID(int dominoID) {
    this.dominoID = dominoID;
  }

  public String getFirstType() {
    return firstType;
  }

  public String getSecondType() {
    return secondType;
  }
}
