package mitll.langtest.client.list;

import java.util.List;

/**
 * Created by go22670 on 2/11/14.
 */
public interface ListChangeListener<T> {
  public void listChanged(List<T> items);
}
