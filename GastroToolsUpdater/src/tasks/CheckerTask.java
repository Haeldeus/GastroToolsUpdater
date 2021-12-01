package tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import loggingtool.LoggingTool;
import updater.Updater;

/**
 * The Task, that will check, if a new Version of the Launcher was released.

 * @author Haeldeus
 * @version {@value updater.Updater#version}
 */
public class CheckerTask extends Task<Void> {

  /**
   * The Label, that will display Messages to the User to enable him to keep track of the Process.
   */
  private Label updates;
  
  /**
   * The primary Application, this Task was indirectly called from. Since the Application calls a 
   * ProgressTask, which in turn will call a CheckerTask, both have to be stored.
   */
  private Updater primary;
  
  /**
   * The ProgressTask, that directly called this Task.
   */
  private ProgressTask prt;
  
  /**
   * The current index of operations done. This is stored to update the ProgressBar in the 
   * Application's Frame.
   */
  private int index;
  
  /**
   * The Constructor for this Task. This will set all Fields to the given Parameters.

   * @param updates The Label, where Messages about the Process are shown to the User.
   * @param primary The Application, this Task was indirectly called from.
   * @param prt The ProgressTask, which directly called this Task.
   * @param index The current index of operations done.
   * @since 1.0
   */
  public CheckerTask(Label updates, Updater primary, ProgressTask prt, int index) {
    this.updates = updates;
    this.primary = primary;
    this.prt = prt;
    this.index = index;
  }
  
  @Override
  protected Void call() {
    /*
     * Creates a bufferedReader, which will read the Version File in the Repository, if a 
     * Connection can be established. If no connection is found, this Reader will be null.
     */
    BufferedReader br;
    try {
      /*
       * Since the Launcher's Version File will always be at the same location, this is a static 
       * check for that File.
       */
      URL url = new URL("https://github.com/Haeldeus/CashAssetsLauncher/blob/main/version.txt");
      LoggingTool.log(getClass(), LoggingTool.getLineNumber(), "Reading text from: " 
          + url.toString());
      /*
       *  Get the input stream through URL Connection. If no connection can be established, an 
       *  IOException will be thrown, which is caught by the catch-Block below.
       */
      URLConnection con = url.openConnection();
      con.connect();
      InputStream is = con.getInputStream();
      /*
       * Creates a BufferedReader for the InputStream.
       */
      br = new BufferedReader(new InputStreamReader(is));
      /*
       * Updates the Frame, to show the Progress.
       */
      prt.updateIndicator(++index, "Verbindung hergestellt!");
      LoggingTool.log(getClass(), LoggingTool.getLineNumber(), 
          "Connection to MainServer established");
    } catch (IOException e) {
      /*
       * Updates the User, that no connection was detected.
       */
      Platform.runLater(new Runnable() {
        @Override
        public void run() {
          updates.setText("Keine Verbindung zum Server möglich. Bitte überprüfen Sie Ihre "
              + "Internetverbindung.");
          LoggingTool.log(getClass(), LoggingTool.getLineNumber(),
              "No Connection to the Server could be established!");
          LoggingTool.logError(getClass(), LoggingTool.getLineNumber(),
              "No Connection to the Server could be established!");
        }        
      });
      /*
       * Sets the Published and older Versions Fields in the parent Task to the Failed-States.
       */
      prt.setPublishedVersion("FAILED");
      prt.setOlderVersions(new ArrayList<String>());
      /*
       * Sets the Reader to null for consistency Issues and returns null to exit the Task.
       */
      br = new BufferedReader(null);
      return null;
    }
    /*
     * Creates an empty String to store version numbers.
     */
    String s = "";
    
    /*
     * A String, that saves the line, that was last read. Will be updated after each br.readLine().
     */
    String line = null;
    
    /*
     * A boolean to determine, if the current part of the InputStream contains 
     * information about the Version.
     */
    boolean version = false;
    
    /*
     * Go through each line of the InputStream to search for version information.
     */
    try {
      while ((line = br.readLine()) != null) {
        /*
         * Checks, if the Task was cancelled. If yes, sets older and published Versions to their 
         * Failed-States and exits the Task.
         */
        if (isCancelled()) {
          Platform.runLater(new Runnable() {
            @Override
            public void run() {
              LoggingTool.log(getClass(), LoggingTool.getLineNumber(),
                  "CheckerTask was cancelled due to timeout!");
              LoggingTool.logError(getClass(), LoggingTool.getLineNumber(), 
                  "CheckerTask was cancelled due to timeout!");
              prt.setPublishedVersion("FAILED");
              updates.setText("Zeitüberschreitung!");
              primary.showUpdateFailed();
            }           
          });
          return null;
        }
        /*
         * This String is always at the end of the version information.
         */
        if (line.contains("#End Version File")) {
          version = false;
          prt.updateIndicator(++index, "Versionsbeschreibung abgefragt.");
        }
        
        /*
         * We are now at the information part of the File. Here begins the allocation of 
         * all versions that were released.
         */
        if (version) {
          /*
           * Remove HTML-Code from the String.
           */
          while (line.contains(">")) {
            /*
             * Checks, if the Task was cancelled. If yes, sets older and published Versions to 
             * their Failed-States and exits the Task.
             */
            if (isCancelled()) {
              Platform.runLater(new Runnable() {
                @Override
                public void run() {
                  LoggingTool.log(getClass(), LoggingTool.getLineNumber(), 
                      "CheckerTask was cancelled due to timeout!");
                  LoggingTool.logError(getClass(), LoggingTool.getLineNumber(), 
                      "CheckerTask was cancelled due to timeout!");
                  prt.setPublishedVersion("FAILED");
                  updates.setText("Zeitüberschreitung!");
                  primary.showUpdateFailed();
                }           
              });
              return null;
            }
            line = line.replaceFirst(line.substring(line.indexOf("<"), 
                line.indexOf(">") + 1), "");
          }
          /*
           * If the Line still contains Information after removing all HTML-Code, then there 
           * are version numbers stored in it. It is added to the String defined before.
           */
          if (line.trim().length() != 0) {
            s = s.concat(line.trim() + System.lineSeparator());
          }
        }
        /*
         * This Line is always at the start of the Information about versions. This is checked 
         * here to remove a line from s.
         */
        if (line.contains("#Begin Version File")) {
          version = true;
          prt.updateIndicator(++index, "Versionsbeschreibung gefunden!");
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    LoggingTool.log(getClass(), LoggingTool.getLineNumber(), 
        "Published Versions found! Splitting Info into suitable Strings...");
    /*
     * Updates the User, that the Version Details will be separated in the next step. 
     */
    prt.updateIndicator(++index, "Aufteilen der Versionsbeschreibung...");
    /*
     * Separates the Version-String into single Version-Tokens. Each Token describes a single 
     * Version-Number.
     */

    StringTokenizer st = new StringTokenizer(s, System.lineSeparator());
    /*
     * The First Token always contains "Current Version:", which isn't a Version itself and can 
     * be deleted safely.
     */
    st.nextToken();
    /*
     * The second Token always contains the current version and thus will be stored.
     */
    prt.setPublishedVersion(st.nextToken());
    /*
     * The third Token always contains "Older Versions:", which isn't a Version itself and can 
     * be deleted safely.
     */
    st.nextToken();
    /*
     * Creates a new ArrayList, where all oldVersions will be stored in. This is done via a 
     * while-loop, that checks, if there are Tokens left in the Tokenizer and adds these Tokens 
     * to the List.
     */
    ArrayList<String> oldVersions = new ArrayList<String>();
    while (st.hasMoreTokens()) {
      oldVersions.add(st.nextToken());
    }
    /*
     * Updates the User, stores the created List in the parent Task and exits this Task.
     */
    LoggingTool.log(getClass(), LoggingTool.getLineNumber(),
        "Check for published Versions successful!");
    prt.updateIndicator(++index, "Version überprüft.");
    prt.setOlderVersions(oldVersions);
    return null;
  }
  
}
