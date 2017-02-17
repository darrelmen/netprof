package mitll.langtest.client.custom.dialog;

//import gwt.material.design.addins.client.autocomplete.base.MaterialSuggestionOracle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by go22670 on 2/16/17.
 */
public class ExerciseOracle /*extends MaterialSuggestionOracle*/ {

 /*   private List<String> contacts = new LinkedList<>();

    public ExerciseOracle() {
      contacts.add("one");
      contacts.add("two");
      contacts.add("three");
    }
    public void addContacts(List<String> users) {
      contacts.addAll(users);
    }

    @Override
    public void requestSuggestions(Request request, Callback callback) {
      Response resp = new Response();
      if(contacts.isEmpty()){
        callback.onSuggestionsReady(request, resp);
        return;
      }
      String text = request.getQuery();
      text = text.toLowerCase();

      List<Suggestion> list = new ArrayList<>();

      for(String contact : contacts){
        if(contact.toLowerCase().contains(text)){
          list.add(new Suggestion() {
            @Override
            public String getDisplayString() {
              return contact;
            }

            @Override
            public String getReplacementString() {
              return contact;
            }
          });
        }
      }

      resp.setSuggestions(list);
      callback.onSuggestionsReady(request, resp);
    }*/
}
