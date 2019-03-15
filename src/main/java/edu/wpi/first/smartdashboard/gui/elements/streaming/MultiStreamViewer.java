package edu.wpi.first.smartdashboard.gui.elements.streaming;

import edu.wpi.first.smartdashboard.joystick.JInputJoystick;
import edu.wpi.first.smartdashboard.properties.StringProperty;

import java.awt.*;

public class MultiStreamViewer extends FFmpegStreamViewer {
  private final StringProperty streamOneURL = new StringProperty(this, "Stream One URL", "");
  private final StringProperty streamTwoURL = new StringProperty(this, "Stream Two URL", "");
  private JInputJoystick joystick;

  public MultiStreamViewer() {
    super();
    this.joystick = new JInputJoystick(0);
  }

  @Override
  protected final void paintComponent(Graphics g) {
    super.paintComponent(g);
    // HACK: paintComponent is the closest thing we have to a "main loop" without redoing the whole dashboard
    // joy input goes here
    this.joystick.pollController();
    float pov = this.joystick.getHatSwitchPosition();
    if (Float.compare(pov, 0.75f) == 0) {
      urlProperty.setValue(streamTwoURL.getValue());
      propertyChanged(urlProperty);
    } else if (Float.compare(pov, 0.25f) == 0) {
      urlProperty.setValue(streamOneURL.getValue());
      propertyChanged(urlProperty);
    }
  }
}
