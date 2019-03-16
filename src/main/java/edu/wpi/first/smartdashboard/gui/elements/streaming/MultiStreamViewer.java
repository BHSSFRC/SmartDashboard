package edu.wpi.first.smartdashboard.gui.elements.streaming;

import edu.wpi.first.smartdashboard.joystick.JInputJoystick;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.properties.StringProperty;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.*;
import java.io.IOException;

public class MultiStreamViewer extends FFmpegStreamViewer {
  private final StringProperty streamOneURL = new StringProperty(this, "Stream One URL", "");
  private final StringProperty streamTwoURL = new StringProperty(this, "Stream Two URL", "");
  private JInputJoystick joystick;

  private FFmpegFrameGrabber grabberOne;
  private FFmpegFrameGrabber grabberTwo;
  private FFmpegFrameGrabber[] grabbers;

  private FFmpegFrameGrabber activeStream;

  public MultiStreamViewer() {
    super();
    this.joystick = new JInputJoystick(0);
  }

  @Override
  public void onInit() {
    this.bgThread = new BGThread();

    this.grabberOne = new FFmpegFrameGrabber(streamOneURL.getValue());
    this.activeStream = grabberOne;
    this.grabberTwo = new FFmpegFrameGrabber(streamTwoURL.getValue());
    this.grabbers = new FFmpegFrameGrabber[]{grabberOne, grabberTwo};
  }

  @Override
  protected final void paintComponent(Graphics g) {
    super.paintComponent(g);
    // HACK: paintComponent is the closest thing we have to a "main loop" without redoing the whole dashboard
    // joy input goes here
    this.joystick.pollController();
    float pov = this.joystick.getHatSwitchPosition();
    if (Float.compare(pov, 0.75f) == 0) {
      this.activeStream = grabberTwo;
      // urlProperty.setValue(streamTwoURL.getValue());
      // propertyChanged(urlProperty);
    } else if (Float.compare(pov, 0.25f) == 0) {
      this.activeStream = grabberOne;
      // urlProperty.setValue(streamOneURL.getValue());
      // propertyChanged(urlProperty);
    }
  }

  @Override
  public void onPropertyChanged(Property property) {
    super.onPropertyChanged(property);
    try {
      if (property.equals(streamOneURL)) {
        grabberOne.stop();
        grabberOne = new FFmpegFrameGrabber(streamOneURL.getValue());
        grabberOne.start();
        grabbers[0] = grabberOne;
      } else if (property.equals(streamTwoURL)) {
        grabberTwo.stop();
        grabberTwo = new FFmpegFrameGrabber(streamTwoURL.getValue());
        grabberTwo.start();
        grabbers[1] = grabberTwo;
      }
    } catch (FFmpegFrameGrabber.Exception e) {
      e.printStackTrace();
    }
  }

  public class BGThread extends StreamViewer.BGThread {

    void setup() throws FrameGrabber.Exception {
      for (FFmpegFrameGrabber grabber : grabbers) {
        grabber.setOption("fflags", "nobuffer");
        if (!ffplayDebug.getValue()) {
          grabber.setOption("loglevel", "quiet");
        }
        grabber.setOptions(ffplayOpts);
        grabber.start();
      }
    }

    @Override
    public void interrupt() {
      try {
        if (activeStream != null) {
          activeStream.close();
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
          if (grabbers != null) {
            setup();
          } else {
            // effectively await stream setup
            continue;
          }
          Java2DFrameConverter converter = new Java2DFrameConverter();
          while (!interrupted() && !isCameraChanged()) {
            Frame frame = activeStream.grab();
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
