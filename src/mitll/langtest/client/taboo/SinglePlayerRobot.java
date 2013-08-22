package mitll.langtest.client.taboo;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.PropertyHandler;
import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.ExerciseShell;
import mitll.langtest.shared.taboo.StimulusAnswerPair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 8/15/13
 * Time: 12:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class SinglePlayerRobot {
  private final LangTestDatabaseAsync service;
//  private List<SectionNode> nodes;
  private List<ExerciseShell> exercisesRemaining = null;
  private Exercise currentExercise = null;
  private List<String> synonymSentences = Collections.emptyList();
 // int nodeIndex, exerciseIndex, stimIndex;
  private PropertyHandler propertyHandler;
  //private Collection<ExerciseShell> exerciseShells;

  /**
   * @see mitll.langtest.client.LangTest#setTabooFactory(long, boolean, boolean)
   * @param service
   * @param propertyHandler
   */
  public SinglePlayerRobot(LangTestDatabaseAsync service, PropertyHandler propertyHandler) {
    this.service = service;
    this.propertyHandler = propertyHandler;
  }

  /**
   * Get the chapters, exercises, run through each (randomly choose exercises),
   * then randomly choose a stimulus.
   * All client side?
   *
   *
   * @paramx fuserid
   */
/*  public void doSinglePlayer() {
    if (nodes == null || nodes.isEmpty()) {
      service.getSectionNodes(new AsyncCallback<List<SectionNode>>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("getSectionNodes can't contact server. got " + caught);
        }

        @Override
        public void onSuccess(List<SectionNode> result) {
          nodes = result;
         // System.out.println("getSectionNodes.onSuccess : got nodes " + nodes);
        }
      });
    }
  }*/

  /**
   * @see ReceiverExerciseFactory.ReceiverPanel#checkForStimulus
   * @param userid
   * @param async
   */
  public void checkForStimulus(long userid, AsyncCallback<StimulusAnswerPair> async) {
    if (exercisesRemaining == null) {
      StimulusAnswerPair stimulusAnswerPair = new StimulusAnswerPair();
      stimulusAnswerPair.setNoStimYet(true);
      async.onSuccess(stimulusAnswerPair); // async query not complete yet
    }
    else if (exercisesRemaining.isEmpty() && synonymSentences.isEmpty()) {
    /*  if (nodes == null) {
        StimulusAnswerPair stimulusAnswerPair = new StimulusAnswerPair();
        stimulusAnswerPair.setNoStimYet(true);
        async.onSuccess(stimulusAnswerPair); // async query not complete yet
      }
      else if (nodes.isEmpty()) {
        System.out.println("checkForStimulus : no chapters left");
*/
        async.onSuccess(null); // no more chapters, no more exercises, we're done -- TODO : start over?
    /*  }
      else {
        SectionNode chapter = nodes.remove(0);
        System.out.println("checkForStimulus.getExercisesForNextChapter : next chapter " + chapter);
        getExercisesForNextChapter(userid, chapter, async);
      }*/
    }
    else {
      getNextExercise(async);
    }
  }

/*  private void getExercisesForNextChapter(final long fuserid, final SectionNode chapter, final AsyncCallback<StimulusAnswerPair> async) {
    Map<String, Collection<String>> typeToSection = new HashMap<String, Collection<String>>();
    typeToSection.put(chapter.getType(), Arrays.asList(chapter.getName()));
    System.out.println("getExercisesForNextChapter requesting for " + typeToSection);

    service.getExercisesForSelectionState(0, typeToSection, fuserid, new AsyncCallback<ExerciseListWrapper>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert("Couldn't contact server.");
      }

      @Override
      public void onSuccess(ExerciseListWrapper result) {
        System.out.println("getExercisesForSelectionState.onSuccess got " + result.exercises.size() + " exercises.");
        exercisesRemaining = result.exercises;
        Random rand = new Random();
        shuffle(exercisesRemaining, rand);

        if (exercisesRemaining.isEmpty()) {
          System.err.println("huh? no exercises for " + chapter + "???");
        } else {
          getNextExercise(async);
        }
      }
    });
  }*/

  /**
   * @see #checkForStimulus(long, com.google.gwt.user.client.rpc.AsyncCallback)
   * @param async
   */
  private void getNextExercise(final AsyncCallback<StimulusAnswerPair> async) {
    if (synonymSentences.isEmpty()) {
      //System.out.println("getNextExercise exercisesRemaining = " + exercisesRemaining.size());

      ExerciseShell nextExerciseShell = exercisesRemaining.get(0);
      //System.out.println("getNextExercise for = " + nextExerciseShell.getID());

      service.getExercise(nextExerciseShell.getID(), new AsyncCallback<Exercise>() {
        @Override
        public void onFailure(Throwable caught) {
          Window.alert("Couldn't contact server.");
        }

        @Override
        public void onSuccess(Exercise result) {
          //System.out.println("getNextExercise.onSuccess got " + result);

          currentExercise = result;
          exercisesRemaining.remove(0);

          synonymSentences = currentExercise.getSynonymSentences();

          final String refSentence = getRefSentence();

          if (synonymSentences.isEmpty()) {
            System.err.println("huh? no stim sentences for " + currentExercise);
            async.onSuccess(new StimulusAnswerPair(result.getID(), "Data error on server, please report.", refSentence, false, false));
          } else {
            String rawStim = synonymSentences.remove(0);
            boolean empty = synonymSentences.isEmpty();
           // System.out.println("getNextExercise stim left " + synonymSentences.size() + " empty " + empty);
            async.onSuccess(new StimulusAnswerPair(result.getID(), getObfuscated(rawStim, refSentence), refSentence,
              empty, false));
          }
        }
      });
    } else {
      String rawStim = synonymSentences.remove(0);
      final String refSentence = getRefSentence();
      boolean empty = synonymSentences.isEmpty();
     // System.out.println("stim left " + synonymSentences.size() + " empty " + empty);

      async.onSuccess(new StimulusAnswerPair(currentExercise.getID(), getObfuscated(rawStim,refSentence), refSentence,
        empty, false));
    }
  }

  private String getRefSentence() {
    return propertyHandler.doTabooEnglish() ? currentExercise.getEnglishSentence().trim() : currentExercise.getRefSentence().trim();
  }

  private String getObfuscated(String exampleToSend, String refSentence) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < refSentence.length(); i++) builder.append('_');
/*    if (!exampleToSend.contains(refSentence)) {
      System.err.println("huh? '" + exampleToSend + "' doesn't contain '" + refSentence + "'");
    }*/
    return exampleToSend.replaceAll(refSentence, builder.toString());
  }


  private void shuffle(List<ExerciseShell> list, Random rnd) {
    int size = list.size();
    ExerciseShell arr[] = list.toArray(new ExerciseShell[size]);

    if (size < 5) {
      for (int i=size; i>1; i--)
        swap(arr, i-1, rnd.nextInt(i));
    } else {
      // Shuffle array
      for (int i=size; i>1; i--)
        swap(arr, i-1, rnd.nextInt(i));

      // Dump array back into list
      ListIterator<ExerciseShell> it = list.listIterator();
      for (ExerciseShell anArr : arr) {
        it.next();
        it.set(anArr);
      }
    }
  }

  /**
   * Swaps the two specified elements in the specified array.
   */
  private void swap(ExerciseShell[] arr, int i, int j) {
    ExerciseShell tmp = arr[i];
    arr[i] = arr[j];
    arr[j] = tmp;
  }

  public void registerAnswer(boolean correct) {
    if (correct) synonymSentences = Collections.emptyList();
    //System.out.println("register answer " + correct);
  }

  public void setExerciseShells(Collection<ExerciseShell> exerciseShells) {
   // this.exerciseShells = exerciseShells;
    //System.out.println("setExerciseShells : got " + exerciseShells.size() + " exercises");
    exercisesRemaining = new ArrayList<ExerciseShell>(exerciseShells);
    Random rand = new Random();

    shuffle(exercisesRemaining, rand);
  }

 /* public Collection<ExerciseShell> getExerciseShells() {
    return exerciseShells;
  }*/
}
