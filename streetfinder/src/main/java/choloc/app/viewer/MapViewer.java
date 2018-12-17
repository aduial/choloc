package choloc.app.viewer;

import choloc.app.solr.Connector;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.xml.sax.SAXException;

import choloc.app.streetfinder.Street;
import choloc.app.streetfinder.StreetFinder;

public class MapViewer extends JMapViewer {

  private static final long serialVersionUID = 8678292754866282345L;

  private transient MapMarkerDot myPositionMarker = null;
  private transient List<ContentPosition> contentPositions = Collections.emptyList();

  private final transient Consumer<String> resultSetter;

  public MapViewer(Consumer<String> resultSetter) {

    this.resultSetter = resultSetter;

    // Create a click event for clicking on the map
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        setPosition(e.getPoint());
        super.mouseClicked(e);
      }
    });
  }

  private void setPosition(Point clicked) {

    // Find clicked position (last position in list is top)
    ContentPosition clickedPosition = null;
    for (ContentPosition candidate : this.contentPositions) {
      if (candidate.isInMarker(clicked, this::getMapPosition)) {
        clickedPosition = candidate;
      }
    }

    // Handle clicked position
    if (clickedPosition != null) {
      // Set the text of the clicked position.
      retrieveSolrInfo(clickedPosition);
    } else {
      // Change my position.
      final ICoordinate coordinate = getPosition(clicked);
      if (myPositionMarker != null) {
        removeMapMarker(myPositionMarker);
      }
      myPositionMarker = new MapMarkerDot(null, "My position", clone(coordinate));
      myPositionMarker.setBackColor(Color.RED);
      addMapMarker(myPositionMarker);
    }
  }

  public ICoordinate getCurrentPosition() {
    return clone(this.myPositionMarker.getCoordinate());
  }

  private void retrieveSolrInfo(ContentPosition position) {

    // Start HTML document
    final StringBuilder resultBuilder = new StringBuilder("<!DOCTYPE html><html><body>");
    resultBuilder.append("<h1>").append(position.getStreet().getStreetName()).append(" (")
        .append(position.getStreet().getPlaceName()).append(")</h1>");

    // Contact the Solr.
    final Map<String, String> searchResults = new Connector()
        .getSolrFTHighlights(position.getStreet().getStreetName(),
            position.getStreet().getPlaceName());

    // Set the results.
    if (searchResults == null || searchResults.isEmpty()) {
      resultBuilder.append("<br><br>No results found.");
    } else {
      for (Entry<String, String> entry : searchResults.entrySet()) {
        resultBuilder.append("<br><br><table style=\"border-collapse: collapse\">");
        resultBuilder.append("<tr><td style=\"border: 1px solid black\"><b>").append(entry.getKey())
            .append("</b></td></tr>");
        final String textValue = entry.getValue().replace("<em>", "<b>").replace("</em>", "</b>");
        resultBuilder.append("<tr><td>").append(textValue).append("</td></tr>").append("</table>");
      }
    }

    // Finish HTML document and save the text.
    resultBuilder.append("</body></html>");
    this.resultSetter.accept(resultBuilder.toString());
  }

  void scoutSurroundings() {

    // Maybe no position has been set.
    if (myPositionMarker == null) {
      return;
    }

    // Obtain data
    final Coordinate here = clone(myPositionMarker.getCoordinate());
    final List<Street> streetResults;
    try {
      streetResults = new StreetFinder()
          .findStreetsSortedByDistance(here.getLat(), here.getLon(), 500);
    } catch (TransformException | IOException | ParserConfigurationException | SAXException | FactoryException e) {
      e.printStackTrace();
      return;
    }

    // Create list of content positions.
    contentPositions = streetResults.stream().map(ContentPosition::new)
        .collect(Collectors.toList());

    // Remove old positions
    removeAllMapMarkers();
    this.resultSetter.accept("");

    // Set my current marker to green
    myPositionMarker.setBackColor(Color.GREEN);

    // Add new markers
    final List<MapMarker> markers = Stream
        .concat(Stream.of(myPositionMarker),
            contentPositions.stream().map(ContentPosition::getPosition))
        .collect(Collectors.toList());
    setMapMarkerList(markers);
  }

  static final Coordinate clone(ICoordinate coordinate) {
    return new Coordinate(coordinate.getLat(), coordinate.getLon());
  }

  private static class ContentPosition {

    private final MapMarkerDot position;
    private final Street street;

    public ContentPosition(Street street) {
      this.position = new MapMarkerDot(null, street.getStreetName(),
          new Coordinate(street.getLat(), street.getLon()));
      this.position.setBackColor(Color.YELLOW);
      this.street = street;
    }

    public Street getStreet() {
      return street;
    }

    public MapMarkerDot getPosition() {
      return position;
    }

    public boolean isInMarker(Point point, Function<Coordinate, Point> toMapPointConverter) {
      final Point markerCenter = toMapPointConverter.apply(position.getCoordinate());
      return markerCenter != null && markerCenter.distance(point) < position.getRadius();
    }
  }
}