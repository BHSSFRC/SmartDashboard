package edu.wpi.first.smartdashboard.gui.elements.streaming;

import edu.wpi.first.smartdashboard.gui.StaticWidget;
import edu.wpi.first.smartdashboard.properties.IntegerProperty;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.properties.StringProperty;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public abstract class StreamViewer extends StaticWidget {

  protected String STREAM_PREFIX = "";
  public static final int MS_TO_ACCUM_STATS = 1000;
  public static final double BPS_TO_MBPS = 8.0 / 1024.0 / 1024.0;

  public final IntegerProperty rotateProperty = new IntegerProperty(this, "Degrees Rotation", 0);
  private double rotateAngleRad = 0;

  public final StringProperty urlProperty = new StringProperty(this, "Server URL", "");
  protected String url = "";

  protected long lastFPSCheck = 0;
  protected int lastFPS = 0;
  protected int fpsCounter = 0;
  protected long bpsAccum = 0;
  protected double lastMbps = 0;
  protected BufferedImage imageToDraw;
  protected BGThread bgThread;
  private boolean cameraChanged = true;

  public void cameraChanged() {
    cameraChanged = true;
  }

  protected boolean isCameraChanged() {
    if (cameraChanged) {
      cameraChanged = false;
      return true;
    }
    return false;
  }

  /**
   * Called before the widget paints for the first time. Any code you would place in a regular widget's
   * {@link StaticWidget#init init} method goes here.
   * <p>
   * INSTANTIATE your {@link StreamViewer#bgThread} here.
   */
  public abstract void onInit();

  public abstract void onPropertyChanged(Property property);

  @Override
  public final void init() {
    onInit();

    url = STREAM_PREFIX + urlProperty.getValue();
    setPreferredSize(new Dimension(160, 120));
    rotateAngleRad = Math.toRadians(rotateProperty.getValue());
    bgThread.start();
    revalidate();
    repaint();
  }

  @Override
  public final void propertyChanged(final Property property) {
    if (property == rotateProperty) {
      rotateAngleRad = Math.toRadians(rotateProperty.getValue());
    }
    if (property == urlProperty) {
      url = urlProperty.getValue();
      cameraChanged();
    }
    onPropertyChanged(property);
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
      // cast the Graphics context into a Graphics2D
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

  public abstract class BGThread extends Thread {
    public BGThread() {
      this("Camera Viewer Background");
    }

    public BGThread(String name) {
      super(name);
    }

    /**
     * This method must, by some means or another, do the following:
     * 1. Connect to the video source at {@link StreamViewer#url}
     * 2. Loop the following directions:
     * 1. Grab one (1) frame of video.
     * 2. Convert the frame to a {@link BufferedImage}.
     * 3. Set {@link StreamViewer#imageToDraw} to the converted frame.
     * 4. Call {@code repaint()} and continue looping.
     * Additionally, it should make attempts to recover lost connections, and respect calls to {@link BGThread#interrupt()}.
     * See {@link FFmpegStreamViewer.BGThread#run()} for a decent reference impl.
     */
    @Override
    public abstract void run();
  }
}
