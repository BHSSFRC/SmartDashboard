package edu.wpi.first.smartdashboard.gui.elements;

import edu.wpi.first.smartdashboard.gui.StaticWidget;
import edu.wpi.first.smartdashboard.properties.IntegerProperty;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.properties.StringProperty;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Class to make h.264 stream viewers on the SmartDashboard. Mashes up {@link MjpgStreamViewer} and {@link MjpgStreamViewerImpl}.
 *
 * @author Caleb Xavier Berger
 * @author Alex Von Hoene
 */
public class H264StreamViewer extends StaticWidget {
  // Open to changing this as needed.
  protected static final String STREAM_PREFIX = "rstp:";
  // everything to deal w/image rotation
  public final IntegerProperty rotateProperty = new IntegerProperty(this, "Degrees Rotation", 0);
  public double rotateAngleRad = 0;
  // allow user to set URL on dash
  public final StringProperty urlProperty = new StringProperty(this, "Streaming Server URL", "");
  private String url = "";
  // boolean to indicate if there's been a camera change since we last looked, used for controlling image thread
  private boolean cameraChanged = true;
  private BufferedImage imageToDraw;

  @Override
  public void init() {
    url = STREAM_PREFIX + urlProperty.getValue();

    setPreferredSize(new Dimension(160, 120));
    rotateAngleRad = Math.toRadians(rotateProperty.getValue());
  }

  @Override
  public void propertyChanged(Property property) {
    if (property == rotateProperty) {
      rotateAngleRad = Math.toRadians(rotateProperty.getValue());
    }
    if (property == urlProperty) {
      url = STREAM_PREFIX + urlProperty.getValue();
      cameraChanged();
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
  public static class BGThread extends Thread {
    @Override
    public void run() {
    }
  }
}
