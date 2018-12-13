package choloc.app.streetfinder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class GeoManipulator {

  private final MathTransform rdToLatLonConversion;
  private final MathTransform latLonToRdConversion;

  GeoManipulator() throws FactoryException {
    latLonToRdConversion = CRS.findMathTransform(CRS.decode("EPSG:4326"), CRS.decode("EPSG:28992"));
    rdToLatLonConversion = CRS.findMathTransform(CRS.decode("EPSG:28992"), CRS.decode("EPSG:4326"));
  }

  protected LatLon convertToLatLon(RdPoint rdPoint) throws TransformException {
    final Coordinate coordinate = new Coordinate(rdPoint.x, rdPoint.y);
    JTS.transform(coordinate, coordinate, rdToLatLonConversion);
    return new LatLon(coordinate.x, coordinate.y);
  }

  protected RdPoint convertToRd(LatLon latLon) throws TransformException {
    final Coordinate coordinate = new Coordinate(latLon.lat, latLon.lon);
    JTS.transform(coordinate, coordinate, latLonToRdConversion);
    return new RdPoint(coordinate.x, coordinate.y);
  }

  protected static <T> List<T> obtainWfsData(URL initialUrl,
      Function<Document, List<T>> dataExtractor, UnaryOperator<String> nextUrlChecker)
      throws ParserConfigurationException, IOException, SAXException {

    // Result
    final List<T> results = new ArrayList<>();

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
      results.addAll(dataExtractor.apply(document));

      // Check whether there are more streets to find. If there aren't, we're done.
      final Optional<String> nextUrl = Optional.of(document).map(Document::getFirstChild).map(
          Node::getAttributes).map(node -> node.getNamedItem("next")).map(Node::getNodeValue);
      if (!nextUrl.isPresent()) {
        break;
      }

      // Set current URL.
      // HACK: the URL returned by the server is not correct.
      currentUrl = new URL(nextUrlChecker.apply(nextUrl.get()));
    }

    // Done
    return results;
  }

  protected static Node getChildNodeWithName(Node parent, String childName) {
    final NodeList children = parent.getChildNodes();
    return IntStream.range(0, children.getLength()).mapToObj(index -> children.item(index))
        .filter(node -> node.getNodeName().equals(childName)).findFirst().orElse(null);
  }

  public static class LatLon {

    final double lat;
    final double lon;

    public LatLon(double lat, double lon) {
      this.lat = lat;
      this.lon = lon;
    }
  }

  public static class RdPoint {

    final double x;
    final double y;

    public RdPoint(double x, double y) {
      this.x = x;
      this.y = y;
    }

    double distanceSq(RdPoint other) {
      final double diffX = other.x - x;
      final double diffY = other.y - y;
      return diffX * diffX + diffY * diffY;
    }

    double distance(RdPoint other) {
      return Math.sqrt(distanceSq(other));
    }
  }

  protected static class BoundingBox {

    private final RdPoint lowerLeft;
    private final RdPoint upperRight;

    public BoundingBox(RdPoint center, int offset) {
      lowerLeft = new RdPoint(center.x - offset, center.y - offset);
      upperRight = new RdPoint(center.x + offset, center.y + offset);
    }

    public RdPoint getLowerLeft() {
      return lowerLeft;
    }

    public RdPoint getUpperRight() {
      return upperRight;
    }
  }
}
