package mitll.langtest.server.domino;

import mitll.hlt.domino.shared.model.document.VocabularyItem;
import mitll.hlt.domino.shared.model.project.ClientPMProject;
import mitll.hlt.domino.shared.model.user.DBUser;

import java.util.Date;
import java.util.List;

public class ImportDoc {
  private final int docID;
  private final long timestamp;
  private final VocabularyItem vocabularyItem;

  /**
   * @param docID
   * @param timestamp
   * @param item
   * @see DominoImport#getChangedDocs(String, DBUser, ClientPMProject)
   */
  ImportDoc(int docID, long timestamp, VocabularyItem item) {
    this.docID = docID;
    this.timestamp = timestamp;
    this.vocabularyItem = item;
  }

  public int getDocID() {
    return docID;
  }

  public long getTimestamp() {
    return timestamp;
  }

  /**
   * @see mitll.langtest.server.database.exercise.DominoExerciseDAO#getExerciseFromImport(int, int, String, String, List)
   * @return
   */
  public VocabularyItem getVocabularyItem() {
    return vocabularyItem;
  }

  public String toString() {
    return "doc " + docID + " at " + new Date(timestamp) +
        "\n\t " + vocabularyItem.getTermVal() +
        "\n\t " + vocabularyItem.getAlternateFormVal() +
        "\n\t " + vocabularyItem.getTransliterationVal() +
        "\n\t " + vocabularyItem.getMeaningVal();
  }
}
