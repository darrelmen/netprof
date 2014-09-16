package mitll.langtest.client.exercise;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import mitll.langtest.shared.CommonShell;
import mitll.langtest.shared.STATE;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 10/2/13
 * Time: 7:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class PagingContainer extends SimplePagingContainer<CommonShell> {
  private static final int MAX_LENGTH_ID = 35;
  private static final boolean DEBUG = false;
  private final Map<String,CommonShell> idToExercise = new HashMap<String, CommonShell>();

  public PagingContainer(ExerciseController controller) {
    super(controller);
  }
    /**
     * @see mitll.langtest.client.list.PagingExerciseList#makePagingContainer()
     * @param controller
     * @param verticalUnaccountedFor
     */
  public PagingContainer(ExerciseController controller, int verticalUnaccountedFor) {
    this(controller);
    this.verticalUnaccountedFor = verticalUnaccountedFor;
  }

  public void redraw() {  table.redraw();  }

  private CommonShell getByID(String id) {
    for (CommonShell t : getList()) {
      if (t.getID().equals(id)) {
        return t;
      }
    }
    return null;
  }

  /**
   * @see mitll.langtest.client.list.PagingExerciseList#simpleRemove(String)
   * @param es
   */
  public void forgetExercise(CommonShell es) {
    List<CommonShell> list = getList();
    int before = getList().size();
    //System.out.println("PagingContainer.forgetExercise, before size = " + before + " : "+ es);

    if (!list.remove(es)) {
      if (!list.remove(getByID(es.getID()))) {
        System.err.println("forgetExercise couldn't remove " + es);
        for (CommonShell t : list) {
          System.out.println("\tnow has " + t.getID());
        }
      }
      else {
        idToExercise.remove(es.getID());
      }
    }
    else {
      if (list.size() == before -1) {
        //System.out.println("\tPagingContainer : now has " + list.size()+ " items");
      }
      else {
        System.err.println("\tPagingContainer.forgetExercise : now has " + list.size() + " items vs " +before);
      }
      idToExercise.remove(es.getID());
    }
    redraw();
  }

  public void setUnaccountedForVertical(int v) {  verticalUnaccountedFor = v;  }

  public List<CommonShell> getExercises() {  return getList();  }
  public int getSize() { return getList().size(); }
  public boolean isEmpty() { return getList().isEmpty();  }
  public CommonShell getFirst() {  return getAt(0);  }
  public int getIndex(CommonShell t) {  return getList().indexOf(t); }
  public CommonShell getAt(int i) { return getList().get(i);  }
/*
  public int getMouseX() {
    return mouseX;
  }

  public int getMouseY() {
    return mouseY;
  }

  public String getClickedExerciseID() {
    return clickedExerciseID;
  }*/

  /*  private List<Image> getImgTags(Node child) {
    NodeList<Node> childNodes = child.getChildNodes();
    List<Image> images = new ArrayList<Image>();
    for (int j = 0; j < childNodes.getLength(); j++) {
      Node item = childNodes.getItem(j);
      System.out.println("\tchild " + j+ " " + item.getNodeName() + " " + item.getNodeValue() + " " +item.getNodeType());
      if (item.getNodeName().equalsIgnoreCase("img")) {

   *//*     Element as = ImageElement.as(item);
        images.add(Image.wrap(as));*//*

        images.add(new MyImage(ImageElement.as(item)));
      }
      else {
        images.addAll(getImgTags(item));
      }
    }
    System.out.println("got  " + images);

    return images;
  }*/

/*  private static class MyImage extends Image {
    public MyImage(Element element) {
      super(element);
    }
  }*/

  /*  public com.github.gwtbootstrap.client.ui.CellTable<CommonShell> makeBootstrapCellTable(com.github.gwtbootstrap.client.ui.CellTable.Resources resources) {
    com.github.gwtbootstrap.client.ui.CellTable<CommonShell> bootstrapCellTable = createBootstrapCellTable(resources);
    this.table = bootstrapCellTable;
    configureTable();

    return bootstrapCellTable;
  }*/

  public CommonShell getCurrentSelection() { return selectionModel.getSelectedObject(); }

  /*  private com.github.gwtbootstrap.client.ui.CellTable<CommonShell> createBootstrapCellTable(com.github.gwtbootstrap.client.ui.CellTable.Resources o) {
     int pageSize = PAGE_SIZE;
    return new com.github.gwtbootstrap.client.ui.CellTable<CommonShell>(pageSize, o);
  }*/

  protected void addColumnsToTable() {
    //System.out.println("addColumnsToTable : completed " + controller.showCompleted() +  " now " + getCompleted().size());

    Column<CommonShell, SafeHtml> id2 = getExerciseIdColumn2(true);
    addColumn(id2);

    // this would be better, but want to consume clicks
  /*  TextColumn<ExerciseShell> id2 = new TextColumn<ExerciseShell>() {
      @Override
      public String getValue(ExerciseShell exerciseShell) {
        String columnText =  exerciseShell.getTooltip();
        if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0,MAX_LENGTH_ID-3)+"...";

        return columnText;
      }
    };*/
  }

  private Column<CommonShell, SafeHtml> getExerciseIdColumn2(final boolean consumeClicks) {

    return new Column<CommonShell, SafeHtml>(new MySafeHtmlCell(consumeClicks)) {

      @Override
      public void onBrowserEvent(Cell.Context context, Element elem, CommonShell object, NativeEvent event) {
        super.onBrowserEvent(context, elem, object, event);
        if (BrowserEvents.CLICK.equals(event.getType())) {
          //System.out.println("getExerciseIdColumn.onBrowserEvent : got click " + event);
          //mouseX = event.getClientX();
          //mouseY = event.getClientY();
          //clickedExerciseID = object.getID();
          gotClickOnItem(object);
        }
      }

      @Override
      public SafeHtml getValue(CommonShell shell) {
        if (!controller.showCompleted()) {
          return getColumnToolTip(shell.getTooltip());
        } else {
          String columnText = shell.getTooltip();
          String html = shell.getID();
          if (columnText != null) {
            if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
            STATE state = shell.getState();

            boolean isDefect = state == STATE.DEFECT;
            boolean isFixed  = state == STATE.FIXED;
            boolean isLL     = shell.getSecondState() == STATE.ATTN_LL;
            boolean isRerecord = shell.getSecondState() == STATE.RECORDED;

            boolean hasSecondState = isLL || isRerecord;
            boolean recorded = state == STATE.RECORDED;
            boolean approved = state == STATE.APPROVED || recorded;

            boolean isSet = isDefect || isFixed || approved;
    /*        if (controller.getAudioType().equals(Result.AUDIO_TYPE_RECORDER)) {
              isSet = recorded;
            }
*/
/*            if (isSet) {
              System.out.println(table.getParent().getParent().getElement().getId()+" shell " + shell.getID() + " state " + state + "/" + shell.getSecondState()+
                " defect " +isDefect +
                " fixed " + isFixed + " recorded " + recorded);
           }*/

            String icon =
              approved ? "icon-check" :
                isDefect ? "icon-bug" :
                  isFixed ? "icon-thumbs-up" :
                      "";

            html = (isSet ?
              "<i " +
                (isDefect ? "style='color:red'" :
                  isFixed ? "style='color:green'" :
                    "") +
                " class='" +
                icon +
                "'></i>" +

                "&nbsp;" : "") + columnText + (hasSecondState ?
              "&nbsp;<i " +
                (isLL ? "style='color:gold'" : "") +
                " class='" +
                (isLL ? "icon-warning-sign" : "icon-microphone") +
                "'></i>" : "");

          }
          return new SafeHtmlBuilder().appendHtmlConstant(html).toSafeHtml();
        }
      }

      private SafeHtml getColumnToolTip(String columnText) {
        if (columnText.length() > MAX_LENGTH_ID) columnText = columnText.substring(0, MAX_LENGTH_ID - 3) + "...";
        return new SafeHtmlBuilder().appendHtmlConstant(columnText).toSafeHtml();
      }
    };
  }

  protected void gotClickOnItem(final CommonShell e) {}

  @Override
  public void clear() {
    super.clear();
    idToExercise.clear();
  }

  public CommonShell byID(String id) { return idToExercise.get(id); }

  public void addExercise(CommonShell exercise) {
    idToExercise.put(exercise.getID(), exercise);
    getList().add(exercise);
    //System.out.println("data now has "+list.size() + " after adding " + exercise.getID());
  }

  public void addExerciseAfter(CommonShell afterThisOne, CommonShell exercise) {
    //System.out.println("addExercise adding " + exercise);

    List<CommonShell> list = getList();
    int before= list.size();
    String id = exercise.getID();
    idToExercise.put(id, exercise);
    int i = list.indexOf(afterThisOne);
    list.add(i + 1, exercise);
    int after = list.size();
   // System.out.println("data now has "+ after + " after adding " + exercise.getID());
    if (before +1!=after) System.err.println("didn't add " + exercise.getID());
  }

  public Set<String> getKeys() { return idToExercise.keySet(); }

  protected void markCurrent(CommonShell currentExercise) {
    if (currentExercise != null) {
      markCurrentExercise(currentExercise.getID());
    }
  }

  protected float adjustVerticalRatio(float ratio) {
    if (dataProvider != null && getList() != null && !getList().isEmpty()) {
      CommonShell toLoad = getList().get(0);

      if (toLoad.getID().length() > ID_LINE_WRAP_LENGTH) {
        ratio /= 2; // hack for long ids
      }
    }

    return ratio;
  }


  public void markCurrentExercise(String itemID) {
    if (getList() == null || getList().isEmpty()) return;

    CommonShell t = idToExercise.get(itemID);
    markCurrent(getList().indexOf(t), t);
  }

  private void markCurrent(int i, CommonShell itemToSelect) {
    if (DEBUG) System.out.println(new Date() + " markCurrentExercise : Comparing selected " + itemToSelect.getID());
    table.getSelectionModel().setSelected(itemToSelect, true);
    if (DEBUG) {
      int pageEnd = table.getPageStart() + table.getPageSize();
      System.out.println("marking " + i + " out of " + table.getRowCount() + " page start " + table.getPageStart() +
        " end " + pageEnd);
    }

    int pageNum = i / table.getPageSize();
    int newIndex = pageNum * table.getPageSize();
    if (i < table.getPageStart()) {
      int newStart = Math.max(0, newIndex);//table.getPageStart() - table.getPageSize());
      if (DEBUG) System.out.println("new start of prev page " + newStart + " vs current " + table.getVisibleRange());
      table.setVisibleRange(newStart, table.getPageSize());
    } else {
      int pageEnd = table.getPageStart() + table.getPageSize();
      if (i >= pageEnd) {
        int newStart = Math.max(0, Math.min(table.getRowCount() - table.getPageSize(), newIndex));   // not sure how this happens, but need Math.max(0,...)
        if (DEBUG) System.out.println("new start of next newIndex " + newStart + "/" + newIndex + "/page = " + pageNum +
          " vs current " + table.getVisibleRange());
        table.setVisibleRange(newStart, table.getPageSize());
      }
    }
    table.redraw();
  }

  /**
   * @see mitll.langtest.client.list.PagingExerciseList#onResize()
   * @param currentExercise
   */
  public void onResize(CommonShell currentExercise) {
    //System.out.println("PagingContainer : onResize");

    int numRows = getNumTableRowsGivenScreenHeight();
    if (table.getPageSize() != numRows) {
      table.setPageSize(numRows);
      table.redraw();
      markCurrent(currentExercise);
    }
  }

  private static class MySafeHtmlCell extends SafeHtmlCell {
    private final boolean consumeClicks;

    public MySafeHtmlCell(boolean consumeClicks) {
      this.consumeClicks = consumeClicks;
    }

    @Override
    public Set<String> getConsumedEvents() {
      Set<String> events = new HashSet<String>();
      if (consumeClicks) events.add(BrowserEvents.CLICK);
      return events;
    }
  }
}
