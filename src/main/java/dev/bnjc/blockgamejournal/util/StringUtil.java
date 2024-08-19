package dev.bnjc.blockgamejournal.util;

import java.nio.file.Path;

public class StringUtil {
  /**
   * Formats a Path into a string with a '/' separator.
   */
  public static String formatPath(Path path) {
    var builder = new StringBuilder();
    for (var iter = path.iterator(); iter.hasNext(); ) {
      Path name = iter.next();
      builder.append(name.getFileName());
      if (iter.hasNext()) builder.append("/");
    }
    return builder.toString();
  }

  public static String removeFormatting(String nbtString) {
    return nbtString.replaceAll("[§&][0-9a-f]", "");
  }
}
