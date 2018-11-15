package mitll.langtest.server.domino;

import mitll.langtest.shared.exercise.DominoUpdateResponse;
import mitll.langtest.shared.project.DominoProject;
import mitll.langtest.shared.project.Language;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IProjectSync {
  DominoUpdateResponse addPending(int projectid, int importUser, boolean doChange);

  @NotNull
  DominoUpdateResponse getDominoUpdateResponse(int projectid, int importUser, boolean doChange, ImportInfo importFromDomino);

  List<DominoProject> getDominoForLanguage(Language lang);
}
