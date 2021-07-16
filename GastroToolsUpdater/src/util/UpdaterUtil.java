package util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * A Utility Class, that provides basic Methods for all Classes in the Updater.

 * @author Haeldeus
 * @version 1.0
 */
public class UpdaterUtil {

  /**
   * Returns the current Time as a String. <br/>
   * The returned String will be "YYYY-MM-DD  HH:MM:SS.sss".

   * @return  A String with the current Time.
   */
  public static String getTime() {
    Instant instant = Instant.ofEpochMilli(System.currentTimeMillis());
    ZonedDateTime zdt = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
    String res = zdt.toString().replaceFirst("T", "  ");
    int index = res.indexOf("+");
    res = res.substring(0, index);
    return res;
  }
  
  /**
   * Prints a Log Entry into the Logfile with the given Text after the current Time.

   * @param text  The Text to be printed after the current Time.
   * @see #getTime()
   * @since 1.0
   */
  public static void log(String text) {
    System.out.println(getTime() + ": " + text);
  }
  
  /**
   * Prints a Log Entry into the ErrorLogFile with the given Text after the current Time.

   * @param text  The Text to be printed after the current Time.
   * @see #getTime()
   * @since 1.0
   */
  public static void logError(String text) {
    System.err.println(getTime() + ": " + text);
  }
}
