package mitll.langtest.server.database.image;

import mitll.langtest.server.database.IDAO;
import mitll.npdata.dao.SlickImage;

import java.util.List;

public interface IImageDAO extends IDAO {

  List<SlickImage> getAll(int projid);

  List<SlickImage> getAllNoExistsCheck(int projid);

  void makeSureImagesAreThere(int projectID, String language, boolean validateAll);

}
