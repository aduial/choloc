package choloc.app.streetfinder;

import java.awt.geom.Point2D.Double;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class StreetFinder extends GeoManipulator {

  private static final String URL_TEMPLATE = "https://geodata.nationaalgeoregister.nl/nwbwegen/wfs"
      + "?REQUEST=GetFeature"
      + "&VERSION=2.0.0"
      + "&SERVICE=WFS"
      + "&typenames=nwbwegen:wegvakken"
      + "&propertyname=stt_naam,gme_naam,geom"
      + "&count=200"
      + "&bbox=%s,%s,%s,%s";

  public StreetFinder() throws FactoryException {
  }

  public List<Street> findStreetsSortedByDistance(double lat, double lon,
      int searchSquareSideInMeters)
      throws TransformException, IOException, ParserConfigurationException, SAXException {

    // Compute the bounding box
    final Double here = convertToRd(new LatLon(lat, lon));
    final int offset = searchSquareSideInMeters;
    final Double lowerLeft = new Double(here.x - offset, here.y - offset);
    final Double upperRight = new Double(here.x + offset, here.y + offset);

    // Determine the URL
    final URL url = new URL(String
        .format(URL_TEMPLATE, "" + lowerLeft.x, "" + lowerLeft.y, "" + upperRight.x,
            "" + upperRight.y));

    // Obtain the street information.
    final List<ParsedStreet> parsedStreets = obtainStreets(url);
    System.out.println("" + parsedStreets.size() + " streets found.");

    // Collect the street segments into one street
    final Map<StreetId, List<ParsedStreet>> streetsById = parsedStreets.stream()
        .collect(Collectors.groupingBy(ParsedStreet::getStreetId));

    // Compute the nearest point.
    final List<Street> result = new ArrayList<>();
    for (Entry<StreetId, List<ParsedStreet>> entry : streetsById.entrySet()) {
      final List<List<Double>> polygons = entry.getValue().stream().map(ParsedStreet::getPolygon)
          .collect(
              Collectors.toList());
      final Double nearestPoint = computeNearestPoint(polygons, here);
      final double distance = nearestPoint.distance(here);
      final LatLon reference = convertToLatLon(nearestPoint);
      result.add(
          new Street(entry.getKey(), reference.lat, reference.lon, (int) Math.round(distance)));
    }

    // Sort results and done.
    System.out.println("" + result.size() + " unique streets found.");
    Collections.sort(result, Comparator.comparing(Street::getDistanceInMeters));
    return result;
  }

  private List<ParsedStreet> obtainStreets(URL initialUrl)
      throws ParserConfigurationException, IOException, SAXException {

    // Result
    final List<ParsedStreet> results = new ArrayList<>();

    // Do this while we have a next batch
    URL currentUrl = initialUrl;
    while (true) {

      // Obtain document
      System.out.println("Sending request: " + currentUrl);
      final Document document;
      try (final InputStream inputStream = currentUrl.openStream()) {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.parse(inputStream);
      }

      // Extract the streets
      final NodeList nodeList = document.getElementsByTagName("nwbwegen:wegvakken");
      results.addAll(
          IntStream.range(0, nodeList.getLength()).mapToObj(index -> nodeList.item(index))
              .map(StreetFinder::extractStreet).collect(Collectors.toList()));

      // Check whether there are more streets to find. If there aren't, we're done.
      final Optional<String> nextUrl = Optional.of(document).map(Document::getFirstChild).map(
          Node::getAttributes).map(node -> node.getNamedItem("next")).map(Node::getNodeValue);
      if (!nextUrl.isPresent()) {
        break;
      }

      // Set current URL.
      // HACK: the URL returned by the server is not correct.
      currentUrl = new URL(nextUrl.get().replace(":/cgi-bin/mapserv.fcgi", "/nwbwegen/wfs"));
    }

    // Done
    return results;
  }

  private static ParsedStreet extractStreet(Node node) {

    // Extract polygon
    final Node geomNode = getChildNodeWithName(node, "nwbwegen:geom");
    final Node lineStringNode = getChildNodeWithName(geomNode, "gml:LineString");
    final String posListString = getChildNodeWithName(lineStringNode, "gml:posList")
        .getTextContent();
    final String[] splitPositions = posListString.split(" ");
    final List<Double> polygon = new ArrayList<>();
    for (int i = 0; i < splitPositions.length; i += 2) {
      final double x = java.lang.Double.parseDouble(splitPositions[i]);
      final double y = java.lang.Double.parseDouble(splitPositions[i + 1]);
      polygon.add(new Double(x, y));
    }

    // Compose result
    final String street = getChildNodeWithName(node, "nwbwegen:stt_naam").getTextContent();
    final String place = getChildNodeWithName(node, "nwbwegen:gme_naam").getTextContent();
    return new ParsedStreet(street, place, polygon);
  }

  private static Node getChildNodeWithName(Node parent, String childName) {
    final NodeList children = parent.getChildNodes();
    return IntStream.range(0, children.getLength()).mapToObj(index -> children.item(index))
        .filter(node -> node.getNodeName().equals(childName)).findFirst().orElse(null);
  }

  private static Double computeNearestPoint(List<List<Double>> polygons, Double here) {
    Double result = null;
    double distance = java.lang.Double.MAX_VALUE;
    for (List<Double> polygon : polygons) {
      final Double currentPoint = computeNearestPointForSinglePolygon(polygon, here);
      final double currentDistance = here.distance(currentPoint);
      if (currentDistance < distance) {
        result = currentPoint;
      }
    }
    return result;
  }

  private static Double computeNearestPointForSinglePolygon(List<Double> polygon, Double here) {

    // If there is just a single point, we have no choice.
    if (polygon.size() == 1) {
      return polygon.get(0);
    }

    // So we can assume that there are line segments.
    final List<Double> candidates = new ArrayList<>();
    for (int i = 1; i < polygon.size(); i++) {
      candidates.add(getNearestPointForLineSegment(polygon.get(i - 1), polygon.get(i), here));
    }

    // Choose the closest
    Double result = null;
    double distance = java.lang.Double.MAX_VALUE;
    for (Double candidate : candidates) {
      final double currentDistance = here.distance(candidate);
      if (currentDistance < distance) {
        result = candidate;
      }
    }
    return result;
  }

  private static Double getNearestPointForLineSegment(Double point1, Double point2, Double here) {

    // In case the segment has length 0, we don't have a direction.
    final double segmentLengthSquared = point1.distanceSq(point2);
    if (segmentLengthSquared == 0.0) {
      return point1;
    }

    // Use scalar projection rule to project vector (point1 - here) on vector (point1 - point2).
    final Double segmentVector = new Double(point2.x - point1.x, point2.y - point1.y);
    final Double hereVector = new Double(here.x - point1.x, here.y - point1.y);
    double dotProduct = segmentVector.x * hereVector.x + segmentVector.y * hereVector.y;
    double scalar = dotProduct / segmentLengthSquared;

    // If 0 <= scalar <= 1, the projection is on the line segment, otherwise, it's outside.
    double adjustedScalar = Math.max(0, Math.min(1, scalar));

    // Find the point corresponding to the scalar value.
    return new Double(point1.x + adjustedScalar * segmentVector.x,
        point1.y + adjustedScalar * segmentVector.y);
  }

  private static class ParsedStreet {

    private final StreetId streetId;
    private final List<Double> polygon;

    public ParsedStreet(String street, String place, List<Double> polygon) {
      if (street == null || street.trim().isEmpty()) {
        throw new IllegalArgumentException("Street is not valid.");
      }
      if (place == null || place.trim().isEmpty()) {
        throw new IllegalArgumentException("Place is not valid.");
      }
      if (polygon.isEmpty()) {
        throw new IllegalArgumentException("Polygon is not valid.");
      }
      this.streetId = new StreetId(street, place);
      this.polygon = polygon;
    }

    public StreetId getStreetId() {
      return streetId;
    }

    public List<Double> getPolygon() {
      return polygon;
    }
  }
}
