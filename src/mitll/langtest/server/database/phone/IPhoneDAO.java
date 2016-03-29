package mitll.langtest.server.database.phone;

/**
 * Created by go22670 on 3/29/16.
 */
public interface IPhoneDAO<T> {
  boolean addPhone(T phone);
}
