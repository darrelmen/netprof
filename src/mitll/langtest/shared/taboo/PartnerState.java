package mitll.langtest.shared.taboo;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/21/13
 * Time: 4:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class PartnerState implements IsSerializable {
  private boolean online;
  private Map<String, Collection<String>> typeToSelection;
  private GameInfo gameInfo;

  public PartnerState() {}

  public PartnerState(boolean online, Map<String, Collection<String>> typeToSelection, GameInfo gameInfo) {
    this(online, typeToSelection);
    this.gameInfo = gameInfo;
  }
    /**
     * @see mitll.langtest.server.database.taboo.OnlineUsers#isPartnerOnline(long, boolean)
     * @param online
     * @param typeToSelection
     */
  public PartnerState(boolean online, Map<String, Collection<String>> typeToSelection) {
    this.online = online;
    this.typeToSelection = typeToSelection;
  }

  public boolean getOnline() {
    return online;
  }

  public Map<String, Collection<String>> getTypeToSelection() {
    return typeToSelection;
  }


  public GameInfo getGameInfo() {
    return gameInfo;
  }

  public void setGameInfo(GameInfo gameInfo) {
    this.gameInfo = gameInfo;
  }
}
