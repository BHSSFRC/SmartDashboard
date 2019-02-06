package edu.wpi.first.smartdashboard.gui.elements.streaming;

import edu.wpi.first.smartdashboard.properties.MultiProperty;
import edu.wpi.first.smartdashboard.properties.Property;
import edu.wpi.first.smartdashboard.properties.StringProperty;

public class MultiStreamViewer extends FFmpegStreamViewer {
  final StringProperty streamOneURL = new StringProperty(this, "Stream One URL", "");
  final StringProperty streamTwoURL = new StringProperty(this, "Stream Two URL", "");
  final MultiProperty selectedStream = new MultiProperty(this, "Selected stream");

  public MultiStreamViewer() {
    super();
    selectedStream.add("Stream one", streamOneURL);
    selectedStream.add("Stream two", streamTwoURL);
  }

  @Override
  public void onPropertyChanged(Property property) {
    super.onPropertyChanged(property);
    if (property.equals(selectedStream)) {
      // dangerous
      url = ((StringProperty) selectedStream.getValue()).getValue();
      // awful
      urlProperty.setValue(url);
      cameraChanged();
    }
  }
}
