package edu.wpi.first.smartdashboard.gui.elements.streaming;

import edu.wpi.first.smartdashboard.properties.Property;

import java.util.stream.Stream;

public class MjpgStreamViewerImpl extends MjpgStreamViewer {

  public static final String NAME = "MJPG Stream Viewer";

  @Override
  public void onPropertyChanged(Property property) {
  }

  @Override
  public Stream<String> streamPossibleCameraUrls() {
    return Stream.of(url);
  }
}
