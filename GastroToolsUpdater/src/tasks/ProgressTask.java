package tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import updater.Updater;

/**
 * The Task, that will start the CheckerTask to check for Updates. This Task will also update the 
 * ProgressIndicator of the Updater Class.
 * @author Haeldeus
 * @version 1.0
 */
public class ProgressTask extends Task<Void> {
  
  /**
   * The Label, that will display update Message to the User. This Label is the same as 
   * {@code Updater.updaterLabel}.
   */
  private Label updates;
  
  /**
   * The Updater, which called this Task.
   */
  private Updater primary;
  
  /**
   * The latest published Version of the Launcher as a String.
   */
  private String publishedVersion;
  
  /**
   * All older published Versions of the Launcher as an ArrayList of Strings.
   */
  private ArrayList<String> olderVersions;
  
  /**
   * The maximal value of the ProgressIndicator.
   */
  private int max;
  
  /**
   * The current index of steps done.
   */
  private int index;
  
  /**
   * The Constructor for this Task. Sets all immediately needed Fields to the given Values.
   * @param primary The Updater, which called this Task.
   * @param updates The Label, that will display update Message to the User.
   * @since 1.0
   */
  public ProgressTask(Updater primary, Label updates) {
    this.updates = updates;
    this.primary = primary;
    max = 10;
    index = 1;
  }
  
  @Override
  protected Void call() {
    /*
     * Updates the Indicator with 1 and a Message, that the Files needed are loading. Increments 
     * index afterwards.
     */
    updateIndicator(index, "Lade Dateien...");
    index++;
    /*
     * Loads the version File. If the File doesn't exist, the following try-Block will cause an 
     * FileNotFoundException, which will be caught and the User gets informed about this Error.
     */
    File version = new File(System.getProperty("user.dir") + "/app/Version.txt");
    try {
      /*
       * Updates the Indicator with 2 and a Message, that the currently installed Version is 
       * checked. Increments index afterwards.
       */
      updateIndicator(index, "Überprüfe installierte Version...");
      index++;
      /*
       * A String to store the currently installed Version.
       */
      String vers;
      try {
        /*
         * Creates a Reader for the Version-File, reads the first Line, where the Version is 
         * written to and closes the Reader afterwards.
         */
        BufferedReader br = new BufferedReader(new FileReader(version.toString()));
        vers = br.readLine();
        br.close();
      } catch (FileNotFoundException e) {
        /*
         * If the Version-File doesn't exist, vers will be set to "" to show that the File is 
         * missing and an Update is recommended.
         */
        vers = "";    
      }
      /*
       * Updates the Indicator with 3 and a Message, that the latest published Version is checked.
       * Increments index afterwards.
       */
      updateIndicator(index, "Überprüfe aktuellste Version...");
      index++;
      /*
       * Creates a new CheckerTask and starts it. This Task will check for the latest Version and 
       * handles the updating-Progress.
       */
      CheckerTask task = new CheckerTask(updates, primary, this, index);
      new Thread(task).start();
      new Thread(() -> {
        try {
          Thread.sleep(5000);  
        } catch (InterruptedException e) {
          //Testing Purposes, shouldn't be called.
          e.printStackTrace();
        }
        task.cancel();
      }).start();
      
      /*
       * While olderVersions is null, the CheckerTask hasn't finished yet and this Thread will wait 
       * for it to finish.
       */
      while (olderVersions == null) {
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          //Nothing to do.
        }
      }
      
      /*
       * If publishedVersion was set to FAILED, an Error occurred while trying to check for 
       * possible updates and the User will be informed. Else, the latestVersion will be set in the 
       * primary Updater.
       */
      if (publishedVersion.equals("FAILED")) {
        primary.showUpdateFailed();
        return null;
      } else {
        primary.setLatestVersion(publishedVersion);
      }
      
      /*
       * If the currently installed Version is equal to the latest published Version, no update is 
       * needed. Else, if vers is equal to "", olderVersions contains the currently installed 
       * Version or in any other case, an Update is recommended to the User.
       */
      if (vers.equals(publishedVersion)) {
        /*
         * Updates the indicator with the maximum value and a Message, that no updates are needed. 
         * Afterwards, it calls primary.startWithoutUpdate to start the Launcher.
         */
        updateIndicator(max, "Keine Updates nötig!");
        primary.startWithoutUpdate();;
      } else if (vers.equals("")) {
        /*
         * Updates the Message Label, to inform the User that no Version-File was found and an 
         * Update is recommended to maintain stability of the Application.
         */
        updateLabel("Keine Versions-Datei gefunden. Update zur nächsten Version "
                + "empfohlen!");
        primary.showUpdateNeeded();
      } else if (olderVersions.contains(vers)) {
        /*
         * Updates the Message Label, to inform the User that a new Version was found and an Update 
         * is recommended.
         */
        updateLabel("Neue Version gefunden!");
        primary.showUpdateNeeded();
      } else {
        /*
         * Updates the Message Label, to inform the User that there was an Error when trying to 
         * check for an Update and an Update is recommended to maintain stability.
         */
        System.out.println(olderVersions.toString() + " " + vers);
        updateLabel("Fehler beim Update!");
        primary.showUpdateNeeded();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    /*
     * Updates the Progress with it's maximum value and stops the Task afterwards.
     */
    updateProgress(max, max);
    return null;
  }
  
  /**
   * Updates the ProgressIndicator with the given value and Text to be displayed.
   * @param value The Value, the Indicator will be updated with. The Percentage displayed will be 
   *      {@code (value/max)}
   * @param text  The Text, that will be displayed in the updates-Label below the Indicator.
   * @see #max
   * @see #updates
   * @since 1.0
   */
  protected void updateIndicator(int value, String text) {
    /*
     * Updates the index to maintain concurrency.
     */
    index = value;
    /*
     * Displays the given Text to the User and updates the Progress with the given value.
     */
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        updates.setText(text);
        updateProgress(value, max);
      }      
    });
  }
  
  /**
   * Updates the Message Label with the given Text.
   * @param text  The Text to be shown to the User.
   * @since 1.0
   */
  private void updateLabel(String text) {
    /*
     * Sets the Text of the updates-Label to the given text.
     */
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        updates.setText(text);
      }      
    });
  }
  
  /**
   * Sets the published Version String to the given String.
   * @param version The String, that will be set as publishedVersion.
   * @since 1.0
   */
  protected void setPublishedVersion(String version) {
    this.publishedVersion = version;
  }
  
  /**
   * Sets the older Version ArrayList to the given ArrayList.
   * @param olderVersions All older Version found as an ArrayList of Strings.
   * @since 1.0
   */
  protected void setOlderVersions(ArrayList<String> olderVersions) {
    this.olderVersions = olderVersions;
  }
}
