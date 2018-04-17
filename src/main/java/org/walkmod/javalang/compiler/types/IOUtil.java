package org.walkmod.javalang.compiler.types;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class IOUtil {

  public static byte[] readStream(InputStream inputStream, boolean close) throws IOException {
    if(inputStream == null) {
      throw new IOException("Stream not found");
    }
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      byte[] data = new byte[4096];

      int bytesRead;
      while((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
        outputStream.write(data, 0, bytesRead);
      }

      outputStream.flush();
      return outputStream.toByteArray();
    } finally {
      if(close) {
        inputStream.close();
      }
    }
  }
}
