package mitll.langtest.server.domino;

import mitll.hlt.domino.shared.model.user.DBUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IDominoImport {
  //  @Override
  ImportInfo getImportFromDomino(int projID, int dominoID, String sinceInUTC, DBUser dominoAdminUser);

  @NotNull
  List<ImportProjectInfo> getImportProjectInfos(DBUser dominoAdminUser);
}
