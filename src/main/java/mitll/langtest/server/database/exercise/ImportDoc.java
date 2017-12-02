package mitll.langtest.server.database.exercise;

import mitll.hlt.domino.shared.model.document.VocabularyItem;

import java.util.Date;

public class ImportDoc {
  private int docID;
  private long timestamp;
  private VocabularyItem vocabularyItem;

  public ImportDoc(int docID, long timestamp, VocabularyItem item){
    this.docID = docID;
    this.timestamp = timestamp;
    this.vocabularyItem = item;
  }

  public String toString() { return "doc " + docID + " at " + new Date(timestamp) +
      "\n\t " + vocabularyItem.getTermVal() +
      "\n\t " + vocabularyItem.getAlternateFormVal() +
      "\n\t " + vocabularyItem.getTransliterationVal() +
      "\n\t " + vocabularyItem.getMeaningVal()
      ;}

  public int getDocID() {
    return docID;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public VocabularyItem getVocabularyItem() {
    return vocabularyItem;
  }
}
