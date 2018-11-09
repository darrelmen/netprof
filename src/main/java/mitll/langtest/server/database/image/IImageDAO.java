package mitll.langtest.server.database.image;

import mitll.langtest.server.database.IDAO;
import mitll.npdata.dao.SlickImage;

import java.util.List;

public interface IImageDAO extends IDAO {

  int insert(SlickImage image);

  List<SlickImage> getAll(int projid);

  List<SlickImage> getAllNoExistsCheck(int projid);

  int ensureDefault(int defProjectID);

  void makeSureImagesAreThere(int projectID, String language, boolean validateAll);

  int getDefault();

}
