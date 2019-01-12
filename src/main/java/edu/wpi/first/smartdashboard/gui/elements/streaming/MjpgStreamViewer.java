package edu.wpi.first.smartdashboard.gui.elements.streaming;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class MjpgStreamViewer extends StreamViewer {

  private static final int[] START_BYTES = new int[]{0xFF, 0xD8};
  private static final int[] END_BYTES = new int[]{0xFF, 0xD9};

  public abstract Stream<String> streamPossibleCameraUrls();

  @Override
  public void onInit() {
    this.STREAM_PREFIX = "mjpg:";
    bgThread = new BGThread();
  }

  public class BGThread extends StreamViewer.BGThread {

    private InputStream stream;

    public BGThread() {
      super("Camera Viewer Background");
    }

    @Override
    public void interrupt() {
      try {
        if (stream != null) {
          stream.close();
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      }

      super.interrupt();
    }

    @Override
    public void run() {
      ByteArrayOutputStream imageBuffer = new ByteArrayOutputStream();
      long lastRepaint = 0;

      while (!interrupted()) {
        stream = getCameraStream();
        try {
          while (!interrupted() && !isCameraChanged() && stream != null) {
            while (System.currentTimeMillis() - lastRepaint < 10) {
              stream.skip(stream.available());
              Thread.sleep(1);
            }
            stream.skip(stream.available());

            imageBuffer.reset();
            readUntil(stream, START_BYTES);
            Arrays.stream(START_BYTES).forEachOrdered(imageBuffer::write);
            readUntil(stream, END_BYTES, imageBuffer);

            fpsCounter++;
            bpsAccum += imageBuffer.size();
            if (System.currentTimeMillis() - lastFPSCheck > MS_TO_ACCUM_STATS) {
              lastFPSCheck = System.currentTimeMillis();
              lastFPS = fpsCounter;
              lastMbps = bpsAccum * BPS_TO_MBPS;
              fpsCounter = 0;
              bpsAccum = 0;
            }

            lastRepaint = System.currentTimeMillis();
            ByteArrayInputStream tmpStream = new ByteArrayInputStream(imageBuffer.toByteArray());
            imageToDraw = ImageIO.read(tmpStream);
            repaint();
          }

        } catch (ArrayIndexOutOfBoundsException ex) {
          // Something really bad happened but we want to recover
          ex.printStackTrace();
        } catch (IOException ex) {
          imageToDraw = null;
          repaint();
          System.out.println(ex.getMessage());
          cameraChanged();
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(ex);
        } finally {
          try {
            if (stream != null) {
              stream.close();
            }
          } catch (IOException ex) {
            ex.printStackTrace();
          }
        }
      }
    }

    private InputStream getCameraStream() {
      while (!interrupted()) {
        for (String streamUrl : streamPossibleCameraUrls()
            .filter(s -> s.startsWith(STREAM_PREFIX))
            .map(s -> s.substring(STREAM_PREFIX.length()))
            .collect(Collectors.toSet())) {
          System.out.println("Trying to connect to: " + streamUrl);
          try {
            URL url = new URL(streamUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(500);
            connection.setReadTimeout(5000);
            InputStream stream = connection.getInputStream();

            System.out.println("Connected to: " + streamUrl);
            return stream;
          } catch (IOException e) {
            imageToDraw = null;
            repaint();
            try {
              Thread.sleep(500);
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
              throw new RuntimeException(ex);
            }
          }
        }
      }
      return null;
    }

    private void readUntil(InputStream stream, int[] bytes) throws IOException {
      readUntil(stream, bytes, null);
    }

    private void readUntil(InputStream stream, int[] bytes, ByteArrayOutputStream buffer)
        throws IOException {
      for (int i = 0; i < bytes.length; ) {
        int b = stream.read();
        if (b == -1) {
          throw new IOException("End of Stream reached");
        }
        if (buffer != null) {
          buffer.write(b);
        }
        if (b == bytes[i]) {
          i++;
        } else {
          i = 0;
        }
      }
    }
  }
}
