package tasks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import tool.LoggingTool;

/**
 * The Task, that will control the Download of the Launcher, if it should be updated or freshly 
 * downloaded.

 * @author Haeldeus
 * @version {@value updater.Updater#version}
 */
public class DownloadTask extends Task<Void> {

  /**
   * The File, that will contain the downloaded Data.
   */
  private File outputFile;
  
  /**
   * The URL, where this Task will download the Data from.
   */
  private String downloadUrl;
  
  /**
   * The size of the File to be downloaded.
   */
  private long downloadLength;
  
  /**
   * The Size of the File at the Start of the Download. This is used, to ensure the estimated 
   * time to download the File is correct.
   */
  private long startingLength;
  
  /**
   * The Label, that will display update Messages to the User.
   */
  private Label updates;
  
  /**
   * The Label, that will display message about the length of this download to the User.
   */
  private Label length;
  
  /**
   * The data, that determines, when this Task started the Download. Used to estimate remaining 
   * time.
   */
  private long start;
  
  /**
   * The String, that defines the version-Number. This is used to ensure, that the file-Download 
   * can be resumed after canceling it.
   */
  private String version;
  
  /**
   * The Constructor for this Task. Sets all Fields to the given Parameters.

   * @param downloadUrl The URL to download the Launcher from.
   * @param file The File, where the Download will be stored in.
   * @param updates The Label, that will display Messages to the User.
   * @param length The Label, that will display Messages about the remaining time to the User.
   * @param version The Version-String, that defines the version to be downloaded.
   * @since 1.0
   */
  public DownloadTask(String downloadUrl, File file, Label updates, Label length, String version) {
    this.outputFile = file;
    this.downloadUrl = downloadUrl;
    this.updates = updates;
    this.length = length;
    this.version = version;
  }
  
  @Override
  protected Void call() throws Exception {

    /*
     * Saves the Path of the File in a new String. This path is saved with the direct Folder, the 
     * File is in as last part of the String ("dirA/dirB/file.end" will be saved as "dirA/dirB/").
     */
    String p = outputFile.getAbsolutePath().substring(0, 
        outputFile.getAbsolutePath().lastIndexOf(File.separator) + 1);
    /*
     * Checking, if the Path exists. If not, it will be created.
     */
    File path = new File(p);
    if (!path.exists()) {
      LoggingTool.log("Creating Folder " + p.substring(p.substring(0, 
          p.length() - 1).lastIndexOf(File.separator), p.length()));
      path.mkdirs();
    }
    LoggingTool.log("File located in Folder " + p);
    
    /*
     * Saves the name of the Application to be downloaded as a String.
     */
    String name = outputFile.getAbsolutePath().substring(p.length(), 
        outputFile.getAbsolutePath().length());
    LoggingTool.log("App Name: " + name);
    
    /*
     * Creates a new File, that will be used to create a temporary File, which will replace the old 
     * File after downloading. If a jar-File already exists at the given path of outputFile, this 
     * will create a new temporary File, if no File exists, then newFile only references outputFile.
     */
    File tmpFile;
    /*
     * Creates a temporary Text-File at the saved path to make resuming the Download possible.
     */
    File f = new File(p + "tmp.txt");
    LoggingTool.log("Creating temporary Text File");
    if (outputFile.exists()) {
      if (!f.exists()) {
        LoggingTool.log("Creating new temporary File");
        tmpFile = new File(p + name.replace(".jar", "(tmp).jar"));
      } else {
        LoggingTool.log("No new temporary File has to be created since download was cancelled");
        tmpFile = outputFile;
      }
    } else {
      LoggingTool.log("No temporary File has to be created");
      tmpFile = outputFile;
    }
    /*
     * Boolean Value, that will determine, if the current File has to be deleted or not.
     */
    boolean deleteFile;
    /*
     * Checks, if a Text-File with the given path of f already exists. In this case, the content 
     * has to be compared to the version to be downloaded to ensure that the already existing part 
     * of the download is up to date.
     */
    LoggingTool.log("Checking, if Text File already exists...");
    if (f.exists()) {
      try {
        /*
         * Creates a BufferedReader, that will read the single line in the File. Afterwards, it 
         * compares this Line to the given version-String to determine, if the File has to be 
         * deleted (both are equal means no deletion, different means deletion of the File).
         */
        BufferedReader br = new BufferedReader(new FileReader(f));
        String readVers = br.readLine();
        LoggingTool.log("TextFile exists, read Version is " + readVers);
        deleteFile = !readVers.equals(version);
        br.close();
      } catch (IOException e) {
        /*
         * If an error was caused when reading the File, this Task will default back to deleting 
         * the File for safety measures.
         */
        deleteFile = true;
        e.printStackTrace();
      }
    } else {
      try {
        /*
         * If there is no existing File, the Data has to be deleted by default, since there is no 
         * possible check for compatibility in this case. Also, a new File will be created, where 
         * the version of the download is stored in.
         */
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        LoggingTool.log("No TextFile exists, creating new File with Version " + version);
        deleteFile = true;
        bw.write(version);
        bw.close();
      } catch (IOException e) {
        /*
         * If an error was caused when writing the File, this Task will default back to deleting 
         * the File for safety measures.
         */
        deleteFile = true;
        e.printStackTrace();
      }
    }

    LoggingTool.log("temporary File has to be deleted? " + deleteFile);
    if (deleteFile) {
      LoggingTool.log("Deleting temporary File...");
      tmpFile.delete();
    }
    /*
     * Sets start to the current time to be able to determine remaining time later on.
     */
    start = System.currentTimeMillis();
    
    /*
     * Opens a URLConnection to the given URL, adds functionality to resume the download and 
     * downloads the Data from the URL.
     */
    //Change to outputFile if not working, for both methods!
    LoggingTool.log("Starting Download!");
    URLConnection downloadFileConnection = addFileResumeFunctionality(downloadUrl, tmpFile);
    try {
      transferDataAndGetBytesDownloaded(downloadFileConnection, tmpFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    /*
     * Checks if this Task was cancelled by the User (by closing the Application or other means) 
     * and returns null in that case to stop the further execution of the Task.
     */
    if (isCancelled()) {
      return null;
    }

    /*
     * Tries to start the downloaded Launcher and exits this Application. If this Start fails, an 
     * Error Message is thrown.
     */
    try {
      /*
       * Deletes the temporary File, where the version was stored, since it's not needed after 
       * completing the Download.
       */
      LoggingTool.log("Deleting temporary Text File");
      f.delete();
      /*
       * Renames the downloaded File, to replace the older executable.
       */
      if (outputFile != tmpFile) {
        LoggingTool.log("Replacing older File");
        outputFile.delete();
        tmpFile.renameTo(outputFile);
      }
      // Run a java application in a separate system process
      //Process proc = Runtime.getRuntime().exec("java -jar " + outputFile.getPath());
      LoggingTool.log("Starting the Launcher...");
      Runtime.getRuntime().exec("java -jar " + outputFile.getPath(), null, 
          new File(outputFile.getPath().substring(0, outputFile.getPath()
              .lastIndexOf(File.separator))));
      // Then retrieve the process output
      /*
        InputStream in = proc.getInputStream();
        InputStream err = proc.getErrorStream();
        OutputStream out = proc.getOutputStream();
      */
      System.exit(0);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Downloads the File from the given URLConnection to the given outputFile. 
   * Also updates the Progress while doing so to be able to show this progress to the User.

   * @param downloadFileConnection  The URLConnection, the Data will be downloaded from.
   * @param outputFile  The File, where the Data will be stored in.
   * @return  The Size of the downloaded Data in bytes as a long.
   * @throws IOException  If there was an Error in getting the I/O-Streams from the URLConnection 
   *      or writing the Data to the File.
   * @since 1.0
   */
  private long transferDataAndGetBytesDownloaded(URLConnection downloadFileConnection, 
      File outputFile) throws IOException {
    /*
     * Gets the path of the outputFile and checks the size of the File to enable resuming the 
     * Download.
     */
    LoggingTool.log("Download File to " + outputFile.getAbsolutePath());
    Path p = Paths.get(outputFile.getAbsolutePath());
    long bytesDownloaded;
    if (Files.exists(p)) {
      bytesDownloaded = Files.size(p);
    } else {
      bytesDownloaded = 0;
    }
    LoggingTool.log("bytesDownloaded is " + bytesDownloaded);
    startingLength = bytesDownloaded;
    /*
     * Updates the Progress (which is 0 / downloadLength). This is more or less just initializing 
     * the ProgressBar with it's Max value.
     * In case there were some data Download prior by an other Instance of the Updater, the 
     * Progress is updated with the downloaded Data's Size / downloadLength.
     */
    updateProgress(bytesDownloaded, downloadLength);
    /*
     * Tries to get the Input-Stream from the URLConnection and a new FileOutputStream to the 
     * OuputFile. Throws an IOException, if these can't be obtained/created.
     * Since these Streams has to be closed after this block has finished, a try-with-resources 
     * statement is used.
     */
    try (InputStream is = downloadFileConnection.getInputStream(); 
        OutputStream os = new FileOutputStream(outputFile, true)) {

      /*
       * Creates a new Byte-Array, which will store the Data read in each Download-Cycle.
       */
      byte[] buffer = new byte[1024];

      /*
       * Creates a new Integer, which will store the amount of downloaded bytes in this Cycle.
       */
      int bytesCount;
      /*
       * While there is still Data to read in is, this Loop will continue. The size of the data 
       * read will be stored in bytesCount for each iteration.
       */
      while ((bytesCount = is.read(buffer)) > 0) {
        /*
         * Checks, if this Task was cancelled since the last iteration.
         */
        if (!isCancelled()) {
          /*
           * Writes the content of buffer into the FileOutputStream.
           */
          os.write(buffer, 0, bytesCount);
          /*
           * Adds the size of the Data read this cycle to bytesDownloaded.
           */
          bytesDownloaded += bytesCount;
          /*
           * Updates the Progress with the new data.
           */
          updateProgress(bytesDownloaded, downloadLength);
          /*
           * Updates the User about the estimated remaining time.
           */
          update(bytesDownloaded);
        } else {
          /*
           * If the Task was cancelled, this Loop will break and thus the Task will finish.
           */
          LoggingTool.log("DownloadTask was cancelled!");
          break;
        }
      }
    }
    return bytesDownloaded;
  }

  /**
   * Updates the User about the estimated time remaining to download the Data.

   * @param bytesDownloaded The size of the Data downloaded so far.
   * @since 1.0
   */
  private void update(long bytesDownloaded) {
    /*
     * Since the download might start with an existing File, the size of this existing File must be 
     * subtraced from the current File size, that is bytesDownloaded to ensure correct values for 
     * performance.
     */
    bytesDownloaded -= startingLength;
    /*
     * Calculates the difference between the current Time and the Time, this Task started.
     */
    long diff = System.currentTimeMillis() - start;
    /*
     * Calculates the performance of this Download. The double Value describes bytes downloaded per 
     * ms.
     */
    double perf = bytesDownloaded / diff;
    /*
     * Calculates the estimated time left to download the rest of the data based on the current 
     * performance.
     */
    double needed = (downloadLength - bytesDownloaded) / perf;
    /*
     * Stores the perf-Value as a String, that describes it in kbit/s instead of B/s.
     */
    String performance = "" + (7.8125 * perf);
    /*
     * Stores the bytesDownloaded-Value as a String, that describes it in MB instead of bytes.
     */
    String downloaded = "" + ((bytesDownloaded + startingLength) / 1048576.0);
    /*
     * Stores the downloadLength-Value as a String, that describes it in MB instead of bytes.
     */
    String max = "" + (downloadLength / 1048576.0);
    /*
     * Stores the needed-Value as a String, that descibes it in seconds instead of ms.
     */
    String rest = "" + (needed / 1000);
    /*
     * Stores the index of the '.' in the Strings. This is used to cut off the numbers displayed 
     * after two decimals to reduce information overflow.
     */
    int indexPerformance = performance.indexOf('.');
    int indexMax = max.indexOf('.');
    int indexDownloaded = downloaded.indexOf('.');
    int indexRest = rest.indexOf('.');
    /*
     * Sets the Labels to display the newly calculated data.
     */
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        updates.setText("Heruntergeladen: " + downloaded.substring(0, indexDownloaded + 2) + "/" 
            + max.substring(0, indexMax + 2) + "MB (" + performance.substring(0, indexPerformance 
                + 2) + " kBit/s)");
        length.setText("Voraussichtliche Restzeit: " + rest.substring(0, indexRest + 2) + "s");
      }
    });
  }
  //Stored, since this might be needed in future implementations.
  /*
  private long downloadFile(String downloadUrl, String saveAsFileName) 
      throws IOException, URISyntaxException {
    File outputFile = new File(saveAsFileName);

    URLConnection downloadFileConnection = addFileResumeFunctionality(downloadUrl, outputFile);
    return transferDataAndGetBytesDownloaded(downloadFileConnection, outputFile);
  } */

  /**
   * Adds the Functionality to resume the Download to the File and calculates the Size of the 
   * Download.

   * @param downloadUrl The URL, that will be downloaded as a String.
   * @param outputFile  The File, the data will be saved in.
   * @return  A new URLConnection to the given URL with the ability to resume the download.
   * @throws IOException  If the Connection to the URL couldn't be established correctly or 
   *      the downloadURL doesn't support the 'GET'-Request.
   * @throws URISyntaxException If the downloadURL was malformed, so no new URI could be created 
   *      from it.
   * @throws ProtocolException  If the downloadURL doesn't support the 'GET'-Request. Since it's 
   *      not possible to determine, which Exception will be called first from the 
   *      requestMethod-Method, both Exceptions are needed (IO and Protocol).
   * @see URI
   * @since 1.0
   */
  private URLConnection addFileResumeFunctionality(String downloadUrl, File outputFile) 
      throws IOException, URISyntaxException, ProtocolException {
    
    /*
     * Creates a new URLConnection to the given downloadUrl.
     */
    URLConnection downloadFileConnection = new URI(downloadUrl).toURL()
        .openConnection();

    /*
     * Stores the Path of the directory, the outputFile will be saved in.
     */
    String pathS = outputFile.getPath().substring(0, 
        outputFile.getPath().lastIndexOf(File.separator) + 1);
    LoggingTool.log("outputFile will be saved in " + pathS);
    
    /*
     * Creates a new File, that describes the Path to the outputFile's Directory.
     */
    File path = new File(pathS);
    
    /*
     * If the Path doesn't exist, it will be created.
     */
    if (!path.exists()) {
      path.mkdirs();
    }
    
    /*
     * Creates a HTTP URL Connection to the given downloadUrl, to request the property to resume 
     * the download.
     */
    HttpURLConnection httpFileConnection = (HttpURLConnection) downloadFileConnection;
    LoggingTool.log("HttpUrlConnection established");
    /*
     * Creates a temporary HTTP URL Connection, that is needed to get the total Size of the Data.
     */
    HttpURLConnection tmpFileConn = (HttpURLConnection) new URI(downloadUrl).toURL()
        .openConnection();
    LoggingTool.log("TmpFileConnection established");
    /*
     * Sets the RequestMethod of the temporary Connection and gets the Length of it's Content as a 
     * long.
     */
    tmpFileConn.setRequestMethod("GET");
    long fileLength = tmpFileConn.getContentLengthLong();
    LoggingTool.log("FileLength to be downloaded is " + fileLength + "B");
    /*
     * Stores the Length of the Content in downloadLength and the length of the OutputFile in a new 
     * long to compare these two values.
     */
    downloadLength = fileLength;
    long existingFileSize = outputFile.length();

    LoggingTool.log("Existing File Size is " + existingFileSize + "B");
    /*
     * Checks, if the existing File's size is smaller than the Length of the File to be downloaded.
     * If yes, it requests the Download Range from there on, if not it updates the Progress to 
     * display the finished state.
     */
    if (existingFileSize < fileLength) {
      httpFileConnection.setRequestProperty("Range", "bytes=" + existingFileSize + "-" 
          + fileLength);
    } else {
      updateProgress(fileLength, fileLength);
      httpFileConnection.setRequestProperty("Range", "bytes=" + existingFileSize + "-" 
          + fileLength);
    }
    /*
     * Returns the newly created Connection with the Property to resume the Download.
     */
    return downloadFileConnection;
  }
}
