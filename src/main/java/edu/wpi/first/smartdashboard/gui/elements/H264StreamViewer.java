package edu.wpi.first.smartdashboard.gui.elements;

import edu.wpi.first.smartdashboard.gui.StaticWidget;
import edu.wpi.first.smartdashboard.properties.IntegerProperty;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.properties.StringProperty;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Class to make h.264 stream viewers on the SmartDashboard. Mashes up {@link MjpgStreamViewer} and {@link MjpgStreamViewerImpl}.
 *
 * @author Caleb Xavier Berger
 * @author Alex Von Hoene
 */
public class H264StreamViewer extends StaticWidget {
  // Open to changing this as needed.
  protected static final String STREAM_PREFIX = "rstp:";
  private static final int MS_TO_ACCUM_STATS = 1000;
  private static final double BPS_TO_MBPS = 8.0 / 1024.0 / 1024.0;
  // everything to deal w/image rotation
  public final IntegerProperty rotateProperty = new IntegerProperty(this, "Degrees Rotation", 0);
  public double rotateAngleRad = 0;
  // allow user to set URL on dash
  public final StringProperty urlProperty = new StringProperty(this, "Streaming Server URL", "");
  private String url = "";
  // boolean to indicate if there's been a camera change since we last looked, used for controlling image thread
  private boolean cameraChanged = true;
  private long lastFPSCheck = 0;
  private int lastFPS = 0;
  private int fpsCounter = 0;
  private long bpsAccum = 0;
  private double lastMbps = 0;
  private BufferedImage imageToDraw;
  private BGThread bgThread = new BGThread();

  @Override
  public void init() {
    url = STREAM_PREFIX + urlProperty.getValue();

    setPreferredSize(new Dimension(160, 120));
    rotateAngleRad = Math.toRadians(rotateProperty.getValue());
    bgThread.start();
  }

  @Override
  public void propertyChanged(Property property) {
    if (property == rotateProperty) {
      rotateAngleRad = Math.toRadians(rotateProperty.getValue());
    }
    if (property == urlProperty) {
      url = STREAM_PREFIX + urlProperty.getValue();
      // cameraChanged();
    }
  }

  @Override
  public final void disconnect() {
    bgThread.interrupt();
    super.disconnect();
  }

  @Override
  protected final void paintComponent(Graphics g) {
    BufferedImage drawnImage = imageToDraw;

    if (drawnImage != null) {
      Graphics2D g2d = (Graphics2D) g;

      // get the existing Graphics transform and copy it so that we can perform scaling and rotation
      AffineTransform origXform = g2d.getTransform();
      AffineTransform newXform = (AffineTransform) (origXform.clone());

      // find the center of the original image
      int origImageWidth = drawnImage.getWidth();
      int origImageHeight = drawnImage.getHeight();
      int imageCenterX = origImageWidth / 2;
      int imageCenterY = origImageHeight / 2;

      // perform the desired scaling
      double panelWidth = getBounds().width;
      double panelHeight = getBounds().height;
      double panelCenterX = panelWidth / 2.0;
      double panelCenterY = panelHeight / 2.0;
      double rotatedImageWidth = origImageWidth * Math.abs(Math.cos(rotateAngleRad))
          + origImageHeight * Math.abs(Math.sin(rotateAngleRad));
      double rotatedImageHeight = origImageWidth * Math.abs(Math.sin(rotateAngleRad))
          + origImageHeight * Math.abs(Math.cos(rotateAngleRad));

      // compute scaling needed
      double scale = Math.min(panelWidth / rotatedImageWidth, panelHeight / rotatedImageHeight);

      // set the transform before drawing the image
      // 1 - translate the origin to the center of the panel
      // 2 - perform the desired rotation (rotation will be about origin)
      // 3 - perform the desired scaling (will scale centered about origin)
      newXform.translate(panelCenterX, panelCenterY);
      newXform.rotate(rotateAngleRad);
      newXform.scale(scale, scale);
      g2d.setTransform(newXform);

      // draw image so that the center of the image is at the "origin"; the transform will take
      // care of the rotation and scaling
      g2d.drawImage(drawnImage, -imageCenterX, -imageCenterY, null);

      // restore the original transform
      g2d.setTransform(origXform);

      // TODO: FPS and Mbps
      g.setColor(Color.PINK);
      g.drawString("FPS: " + lastFPS, 10, 10);
      g.drawString("Mbps: " + String.format("%.2f", lastMbps), 10, 25);
    } else {
      g.setColor(Color.PINK);
      g.fillRect(0, 0, getBounds().width, getBounds().height);
      g.setColor(Color.BLACK);
      g.drawString("NO CONNECTION", 10, 10);
    }
  }

  /**
   * Marks the camera (image source) as changed since we last read it for the background thread.
   */
  public void cameraChanged() {
    cameraChanged = true;
  }

  /**
   * Reads if the camera has been changed since we last read it. Also unsets the camera as changed if it has been.
   */
  private boolean isCameraChanged() {
    if (cameraChanged) {
      cameraChanged = false;
      return true;
    }
    return false;
  }

  /**
   * Background class to read images from the camera into {@code imageToDraw}.
   */
  public class BGThread extends Thread {

    private FFmpegFrameGrabber grabber;

    public BGThread() {
      super("Camera Viewer Background");
    }

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
      try {
        grabber = new FFmpegFrameGrabber("rtsp://localhost:5004/test");
        grabber.start();
        Java2DFrameConverter converter = new Java2DFrameConverter();
        while (true) {
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
      } catch (Exception e) {
        // TODO: Discover what circumstances cause this
        e.printStackTrace(System.out);
      }
      System.out.println("Exiting thread...");
    }
  }
}
