package edu.wpi.first.smartdashboard.gui.elements.streaming;

import edu.wpi.first.smartdashboard.properties.BooleanProperty;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.properties.StringProperty;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.io.IOException;
import java.util.HashMap;

/**
 * Class to make FFmpeg-based stream viewers on the SmartDashboard.
 *
 * @author Caleb Xavier Berger
 * @author Alex Von Hoene
 */
public class FFmpegStreamViewer extends StreamViewer {

  public final StringProperty ffplayOptions = new StringProperty(this, "Extra ffplay options", "");
  protected HashMap<String, String> ffplayOpts = new HashMap<>();
  final BooleanProperty ffplayDebug = new BooleanProperty(this, "FFPlay expanded debug", false);

  @Override
  public void onInit() {
    bgThread = new BGThread();
  }

  @Override
  public void onPropertyChanged(Property property) {
    if (property.equals(ffplayOptions)) {
      String opts = ffplayOptions.getValue();
      String[] pairs = opts.split(",");
      for (String pair : pairs) {
        String[] kv = pair.split(":");
        ffplayOpts.put(kv[0], kv[1]);
      }
      cameraChanged();
    }
  }

  public class BGThread extends StreamViewer.BGThread {

    private FFmpegFrameGrabber grabber;

    @Override
    public void interrupt() {
      try {
        if (grabber != null) {
          grabber.close();
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      super.interrupt();
    }

    @Override
    public void run() {
      while (!interrupted()) {
        try {
          grabber = new FFmpegFrameGrabber(url);
          grabber.setOption("fflags", "nobuffer");
          if (!ffplayDebug.getValue()) {
            grabber.setOption("loglevel", "quiet");
          }
          grabber.setOptions(ffplayOpts);
          grabber.start();
          Java2DFrameConverter converter = new Java2DFrameConverter();
          while (!interrupted() && !isCameraChanged()) {
            Frame frame = grabber.grab();
            if (frame != null) {
              fpsCounter++;
              if (System.currentTimeMillis() - lastFPSCheck > MS_TO_ACCUM_STATS) {
                lastFPSCheck = System.currentTimeMillis();
                lastFPS = fpsCounter;
                // lastMbps = bpsAccum * BPS_TO_MBPS;
                fpsCounter = 0;
                bpsAccum = 0;
              }
              imageToDraw = converter.convert(frame);
              repaint();
              continue;
            }
            // doesn't matter, painting methods handle it
            imageToDraw = null;
            repaint();
          }
        } catch (FrameGrabber.Exception e) {
          // something went wrong probably in connecting
          cameraChanged();
        } catch (Exception e) {
          // TODO: Discover what circumstances cause this
          e.printStackTrace(System.out);
        }
      }
      System.out.println("Exiting thread...");
    }
  }
}
