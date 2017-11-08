package mitll.langtest.server.database.user;

import mitll.hlt.domino.shared.model.user.ClientUserDetail;
import mitll.hlt.domino.shared.model.user.Group;
import mitll.langtest.shared.user.User;

public interface IDominoUserDAO {
  ClientUserDetail addAndGet(ClientUserDetail user, String encodedPass);

  User getUserByID(String userID);

  ClientUserDetail toClientUserDetail(User user, String projectName, Group group);
}
