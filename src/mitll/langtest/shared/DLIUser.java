package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/10/13
 * Time: 2:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class DLIUser implements IsSerializable, Demographics {
  private long userID;  // foreign key

  private int weeksOfExperience;
  private ILRLevel reading;
  private ILRLevel listening;
  private ILRLevel speaking;
  private ILRLevel writing;

  public DLIUser() {
  }

  /**
   * @see mitll.langtest.client.user.StudentDialog#addUser(int, String, int, String, java.util.Collection)
   * @param userID
   * @param weeksOfExperience
   * @param reading
   * @param listening
   * @param speaking
   * @param writing
   */
  public DLIUser(long userID, int weeksOfExperience,
                 ILRLevel reading, ILRLevel listening, ILRLevel speaking, ILRLevel writing) {
    this.userID = userID;
    this.weeksOfExperience = weeksOfExperience;
    this.reading = reading;
    this.listening = listening;
    this.speaking = speaking;
    this.writing = writing;
  }

  public int getWeeksOfExperience() {
    return weeksOfExperience;
  }

  public ILRLevel getReading() {
    return reading;
  }

  public ILRLevel getListening() {
    return listening;
  }

  public ILRLevel getSpeaking() {
    return speaking;
  }

  public ILRLevel getWriting() {
    return writing;
  }

  @Override
  public long getUserID() {
    return userID;
  }

  public String toString() {
    return "user id " + userID + " has " +weeksOfExperience + " weeks exp, ILR (r "+ reading + ", l "+ listening + ", s "+ speaking + ", w "+ writing + ")";
  }

  public static class ILRLevel implements IsSerializable {
    private String ilrLevel;
    private boolean estimating;

    public ILRLevel() {
    }

    public ILRLevel(String ilrLevel, boolean estimating) {
      this.ilrLevel = ilrLevel;
      this.estimating = estimating;
    }

    public String getIlrLevel() {
      return ilrLevel;
    }

    public boolean isEstimating() {
      return estimating;
    }
    public String toString() { return ilrLevel + (isEstimating() ? "(e)" : ""); }
  }
}
