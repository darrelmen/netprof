package mitll.langtest.client.scoring;

/**
 * Created by go22670 on 9/8/14.
 */
public interface CommentAnnotator {
  void addIncorrectComment(String commentToPost, String field);

  void addCorrectComment(String field);
}
