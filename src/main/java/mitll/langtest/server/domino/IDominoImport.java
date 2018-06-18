package mitll.langtest.server.domino;

import mitll.hlt.domino.shared.model.user.DBUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public interface IDominoImport {
  ImportInfo getImportFromDomino(int projID, int dominoID, String sinceInUTC, DBUser dominoAdminUser, boolean shouldSwap);

  @NotNull
  List<ImportProjectInfo> getImportProjectInfos(DBUser dominoAdminUser);

  String getDominoProjectName(int id);

  Map<String, Integer> getNPIDToDominoID(int projid);
}
