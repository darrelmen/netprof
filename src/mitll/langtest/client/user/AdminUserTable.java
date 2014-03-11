package mitll.langtest.client.user;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.shared.User;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/14/13
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class AdminUserTable extends UserTable {
  private boolean showEnabled = false;

  public AdminUserTable(PropertyHandler props) {
    super(props);
  }

  @Override
  public void showUsers(final LangTestDatabaseAsync service, int userid) {
    service.isAdminUser(userid, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Boolean result) {
        showEnabled = result;
        showDialog(service);
      }
    });
  }

  @Override
  protected void addUserIDColumns(final LangTestDatabaseAsync service, CellTable<User> table) {
    super.addUserIDColumns(service, table);

    if (showEnabled) {
      CheckboxCell checkboxCell = new CheckboxCell(false, false);

      Column<User, Boolean> checkColumn = new Column<User, Boolean>(checkboxCell) {
        @Override
        public Boolean getValue(User object) {
          return object.enabled;
        }

        @Override
        public void setFieldUpdater(FieldUpdater<User, Boolean> fieldUpdater) {
          super.setFieldUpdater(fieldUpdater);
        }
      };

      checkColumn.setFieldUpdater(new FieldUpdater<User, Boolean>() {
        @Override
        public void update(int index, User object, Boolean value) {
          System.out.println("got " + index +  " " + object + " value " + value);
          service.setUserEnabled(object.id, value, new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
              Window.alert("setUserEnabled couldn't contact server.");
            }

            @Override
            public void onSuccess(Void result) {

            }
          });
        }
      });

      table.addColumn(checkColumn, "Enabled");
      table.setColumnWidth(checkColumn, 40, Style.Unit.PX);
    }
  }
}
