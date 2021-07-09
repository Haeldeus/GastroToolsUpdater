package updater;

import java.io.File;
import java.io.IOException;
//import java.io.InputStream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import tasks.DownloadTask;
import tasks.ProgressTask;

/**
 * The Updater MainClass. Here, the Frame will be configured and all other Tasks will be started 
 * from this Thread.
 * @author Haeldeus
 * @version 1.0
 */
public class Updater extends Application {

  /**
   * The primary Stage, this Application is running on.
   */
  private Stage primary;
  
  /**
   * The Label, where all Updates will be shown to the User.
   */
  private Label updaterLabel;
  
  /**
   * The BorderPane, which contains all other Nodes.
   */
  private BorderPane bp;
  
  /**
   * The ProgressIndicator, that shows the Progress to the User.
   */
  private ProgressIndicator pi;
  
  /**
   * The File, where the Launcher is located at.
   */
  private File file;
  
  /**
   * The latest published Version of the Launcher.
   */
  private String latestVersion;
  
  @Override
  public void start(Stage primaryStage) throws Exception {
    /*
     * Sets all immediately needed Fields to their default values.
     */
    file = new File(System.getProperty("user.dir") + "/app/Launcher.jar");
    this.primary = primaryStage;
    this.bp = new BorderPane();
    this.pi = new ProgressIndicator();
    /*
     * Starts the UpdateTask to check for new Updates for the Launcher.
     */
    startUpdateTask();
    /*
     * Sets the Size of the Scene, it's restrictions and the Stylesheet. Afterwards, it displays 
     * the primaryStage to the User.
     */
    Scene scene = new Scene(this.bp, 600, 250);
    //TODO: Add Stylesheet
    //scene.getStylesheets().add(Util.getControlStyle());
    primary.setScene(scene);
    primary.setMinHeight(270);
    primary.setMinWidth(620);
    primary.setTitle("GastroTools Updater");
    primary.show();
  }

  /**
   * Starts the ProgressTask. That Task will check for Updates via a new CheckerTask and update the 
   * progressIndicator and updaterLabel.
   * @see ProgressTask
   * @since 1.0
   */
  private void startUpdateTask() {
    /*
     * Sets the ProgressIndicator as the Center Node of the BorderPane.
     */
    this.bp.setCenter(this.pi);
    
    /*
     * Creates a new Label to display Update Messages and adds it to the Bottom of the Pane.
     */
    this.updaterLabel = new Label();
    this.bp.setBottom(this.updaterLabel);
    
    /*
     * Creates a new ProgressTask, binds the Indicator to it and starts it afterwards.
     */
    ProgressTask pt = new ProgressTask(this, this.updaterLabel);
    this.pi.progressProperty().bind(pt.progressProperty());
    new Thread(pt).start();
  }
  
  /**
   * Displays a Message to the User, that the Update has failed. This might happen if: <br>
   * - The Version File was not found. <br>
   * - The Connection to the Website with the version Number couldn't be established. <br>
   * - The Task was interrupted (either User-Side or by timing out).
   * @since 1.0
   */
  public void showUpdateFailed() {
    /*
     * Since all following instructions alter the Stage of the Application, this has to be done via 
     * a new Runnable.
     */
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        /*
         * Sets the Bottom Node of the BorderPane to null and creates a new GridPane, that will be 
         * displayed in the Center.
         */
        bp.setBottom(null);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));
        /*
         * Adds the Updater Label to the Grid.
         */
        grid.add(updaterLabel, 0, 0, 2, 1);
        /*
         * Creates a new Button to retry updating and adds it to the Grid.
         */
        Button btRetry = new Button("Wiederholen" + System.lineSeparator() + "(Empfohlen)");
        btRetry.setOnAction(new EventHandler<ActionEvent>() {
          @Override
          public void handle(ActionEvent arg0) {
            /*
             * Simply restarts the updateTask, since this resets the Stage and retries to update.
             */
            startUpdateTask();
          }          
        });
        grid.add(btRetry, 0, 1);
        
        /*
         * Creates a new Button to ignore the problem and starts the Launcher without updating.
         * This Button is added to the Grid as well.
         */
        Button btStart = new Button("Ohne Update starten" + System.lineSeparator() 
            + "(Nicht Empfohlen)");
        btStart.setOnAction(new EventHandler<ActionEvent>() {
          @Override
          public void handle(ActionEvent ae) {
            /*
             * Simply calls the Method to start without updating.
             */
            startWithoutUpdate();
          }
        });
        grid.add(btStart, 1, 1);
        grid.setAlignment(Pos.CENTER);
        bp.setCenter(grid);
      }      
    });
  }
  
  /**
   * Displays a Message to the User, that an Update is needed and adds Buttons to start updating or 
   * ignoring the Update.
   * @since 1.0
   */
  public void showUpdateNeeded() {
    /*
     * Since all following instructions alter the Stage of the Application, this has to be done via 
     * a new Runnable.
     */
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        /*
         * Sets the Bottom Node of the BorderPane to null and creates a new GridPane, that will be 
         * displayed in the Center.
         */
        bp.setBottom(null);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 10, 10, 10));
        /*
         * Adds the Updater Label to the Grid.
         */
        grid.add(updaterLabel, 0, 0, 2, 1);
        /*
         * Creates a new Button to start updating and adds it to the Grid.
         */
        Button btUpdate = new Button("Neue Version herunterladen" 
              + System.lineSeparator() + "(Empfohlen)");
        btUpdate.setOnAction(new EventHandler<ActionEvent>() {
          @Override
          public void handle(ActionEvent arg0) {
            /*
             * Starts to update via the designated Method.
             */
            startUpdate();
          }          
        });
        grid.add(btUpdate, 0, 1);
        
        /*
         * Creates a new Button to ignore the problem and starts the Launcher without updating.
         * This Button is added to the Grid as well.
         */
        Button btStart = new Button("Ohne Update starten" + System.lineSeparator() 
            + "(Nicht Empfohlen)");
        btStart.setOnAction(new EventHandler<ActionEvent>() {
          @Override
          public void handle(ActionEvent ae) {
            /*
             * Simply starts the Application without updating via the designated Method.
             */
            startWithoutUpdate();
          }
        });
        grid.add(btStart, 1, 1);
        grid.setAlignment(Pos.CENTER);
        bp.setCenter(grid);
      }      
    });
  }
  
  /**
   * Starts to update the Launcher via DownloadTask. Displays some Labels to the User, which 
   * contain some information about the Performance.
   * @since 1.0
   */
  private void startUpdate() {
    /*
     * Resets the BorderPane, since all former Nodes aren't needed in this step.
     */
    bp.setCenter(null);
    bp.setBottom(null);

    /*
     * Creates a new GridPane, that will display all future Nodes to the User.
     */
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(10, 10, 10, 10));
    
    /*
     * Creates the Labels, that will display the information to the User.
     */
    Label updates = new Label();
    Label length = new Label();
    
    /*
     * Creates the DownloadTask with the needed Parameters.
     */
    DownloadTask task = 
        new DownloadTask("https://github.com/Haeldeus/GastroToolsLauncher/releases/download/v" 
        + latestVersion + "/Launcher.jar", file, updates, length, latestVersion);
    
    /*
     * Creates a new ProgressBar, that will display the progress of the Download/Update to the User 
     * and binds it to the DownloadTask.
     */
    ProgressBar pb = new ProgressBar();
    pb.progressProperty().bind(task.progressProperty());
    
    /*
     * Adds all Nodes to the Grid.
     */
    grid.add(pb, 0, 0, 3, 1);
    grid.add(updates, 0, 1, 3, 1);
    grid.add(length, 0, 2, 3, 1);
    
    grid.setAlignment(Pos.CENTER);
    /*
     * Starts the DownloadTask.
     */
    new Thread(task).start();
    /*
     * Adds a new EventHandler to the onCloseRequest to cancel the Update, when the User closes the 
     * Application.
     */
    primary.setOnCloseRequest(new EventHandler<WindowEvent>() {
      @Override
      public void handle(WindowEvent event) {
          task.cancel();
      }
    });
    /*
     * Adds the Grid to the Center of the BorderPane.
     */
    bp.setCenter(grid);
  }
  
  /**
   * Starts the Launcher without updating it.
   * @since 1.0
   */
  public void startWithoutUpdate() {
    try {
      /*
       *  Run a java application in a separate system process
       */
      //Process proc = Runtime.getRuntime().exec("java -jar " + file.getPath());
      System.out.println("working dir: " + file.getPath().substring(0, 
          file.getPath().lastIndexOf(File.separator)));
      Runtime.getRuntime().exec("java -jar " + file.getPath(), null, 
          new File(file.getPath().substring(0, file.getPath().lastIndexOf(File.separator))));
      
      /*
       *  Then retrieve the process output
       */
      //InputStream in = proc.getInputStream();
      //InputStream err = proc.getErrorStream();
      System.exit(0);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Sets the {@link #latestVersion}-Field to the given String.
   * @param version The latest found Version. If no version was found, this has to be set to 
   * {@code "FAILED"}.
   * @since 1.0
   */
  public void setLatestVersion(String version) {
    this.latestVersion = version;
  }
  
  /**
   * The Main-Method to start this Application.
   * @param args  Unused.
   * @since 1.0
   */
  public static void main(String[] args) {
    Updater.launch(args);
  }
}
