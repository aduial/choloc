package choloc.app.streetfinder;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.parsers.ParserConfigurationException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class AreaFinder extends GeoManipulator {

  private static final String MUNICIPALITY_URL_TEMPLATE =
      "https://geodata.nationaalgeoregister.nl/wijkenbuurten2018/wfs"
          + "?REQUEST=GetFeature"
          + "&VERSION=2.0.0"
          + "&SERVICE=WFS"
          + "&typenames=wijkenbuurten2018:gemeenten2018"
          + "&propertyname=wijkenbuurten2018:gemeentenaam"
          + "&bbox=%s,%s,%s,%s";

  private static final String DISTRICT_URL_TEMPLATE =
      "https://geodata.nationaalgeoregister.nl/wijkenbuurten2018/wfs"
          + "?REQUEST=GetFeature"
          + "&VERSION=2.0.0"
          + "&SERVICE=WFS"
          + "&typenames=wijkenbuurten2018:cbs_wijken_2018"
          + "&propertyname=wijkenbuurten2018:gemeentenaam,wijkenbuurten2018:wijknaam"
          + "&bbox=%s,%s,%s,%s";

  private static final String NEIGHBORHOOD_URL_TEMPLATE =
      "https://geodata.nationaalgeoregister.nl/wijkenbuurten2018/wfs"
          + "?REQUEST=GetFeature"
          + "&VERSION=2.0.0"
          + "&SERVICE=WFS"
          + "&typenames=wijkenbuurten2018:cbs_buurten_2018"
          + "&propertyname=wijkenbuurten2018:gemeentenaam,wijkenbuurten2018:buurtnaam"
          + "&bbox=%s,%s,%s,%s";

  private static final Pattern DISTRICT_PATTERN = Pattern.compile("^Wijk \\d+ (.*)$");

  public AreaFinder() throws FactoryException {
  }

  public Set<Area> findAreas(double lat, double lon,
      int searchSquareRadiusInMeters)
      throws TransformException, IOException, ParserConfigurationException, SAXException {

    // Compute the bounding box
    final RdPoint here = convertToRd(new LatLon(lat, lon));
    final BoundingBox boundingBox = new BoundingBox(here, searchSquareRadiusInMeters);

    // Obtain municipalities
    final URL municipalityUrl = new URL(String
        .format(MUNICIPALITY_URL_TEMPLATE, "" + boundingBox.getLowerLeft().x,
            "" + boundingBox.getLowerLeft().y, "" + boundingBox.getUpperRight().x,
            "" + boundingBox.getUpperRight().y));
    final List<Area> municipalities = obtainWfsData(municipalityUrl,
        AreaFinder::obtainMunicipalitiesFromDocument, UnaryOperator.identity());

    // Obtain districts
    final URL districtUrl = new URL(String
        .format(DISTRICT_URL_TEMPLATE, "" + boundingBox.getLowerLeft().x,
            "" + boundingBox.getLowerLeft().y, "" + boundingBox.getUpperRight().x,
            "" + boundingBox.getUpperRight().y));
    final List<Area> districts = obtainWfsData(districtUrl, AreaFinder::obtainDistrictsFromDocument,
        UnaryOperator.identity());

    // Obtain neighborhoods
    final URL neighborhoodUrl = new URL(String
        .format(NEIGHBORHOOD_URL_TEMPLATE, "" + boundingBox.getLowerLeft().x,
            "" + boundingBox.getLowerLeft().y, "" + boundingBox.getUpperRight().x,
            "" + boundingBox.getUpperRight().y));
    final List<Area> neighborhoods = obtainWfsData(neighborhoodUrl,
        AreaFinder::obtainNeighborhoodsFromDocument, UnaryOperator.identity());

    // Done
    final HashSet<Area> result = new HashSet<>(municipalities);
    result.addAll(districts);
    result.addAll(neighborhoods);
    return result;
  }

  private static List<Area> obtainMunicipalitiesFromDocument(Document document) {
    final NodeList nodeList = document.getElementsByTagName("wijkenbuurten2018:gemeenten2018");
    return IntStream.range(0, nodeList.getLength()).mapToObj(index -> nodeList.item(index))
        .map(node -> getChildNodeWithName(node, "wijkenbuurten2018:gemeentenaam"))
        .map(Node::getTextContent).map(muni -> new Area(muni, muni)).collect(Collectors.toList());
  }

  private static List<Area> obtainDistrictsFromDocument(Document document) {
    final NodeList nodeList = document.getElementsByTagName("wijkenbuurten2018:cbs_wijken_2018");
    return IntStream.range(0, nodeList.getLength()).mapToObj(index -> nodeList.item(index))
        .map(AreaFinder::createDistrict).filter(Objects::nonNull).collect(Collectors.toList());
  }

  private static List<Area> obtainNeighborhoodsFromDocument(Document document) {
    final NodeList nodeList = document.getElementsByTagName("wijkenbuurten2018:cbs_buurten_2018");
    return IntStream.range(0, nodeList.getLength()).mapToObj(index -> nodeList.item(index))
        .map(AreaFinder::createNeighborhood).filter(Objects::nonNull).collect(Collectors.toList());
  }

  private static Area createDistrict(Node node) {
    final Node municipality = getChildNodeWithName(node, "wijkenbuurten2018:gemeentenaam");
    final Node district = getChildNodeWithName(node, "wijkenbuurten2018:wijknaam");
    if (district == null) {
      return null;
    }
    final String rawDistrictName = district.getTextContent();
    final Matcher matcher  = DISTRICT_PATTERN.matcher(rawDistrictName);
    final String districtName;
    if (matcher.matches()) {
      districtName = matcher.group(1);
    } else {
      districtName = rawDistrictName;
    }
    return new Area(districtName, municipality.getTextContent());
  }

  private static Area createNeighborhood(Node node) {
    final Node municipality = getChildNodeWithName(node, "wijkenbuurten2018:gemeentenaam");
    final Node neighborhood = getChildNodeWithName(node, "wijkenbuurten2018:buurtnaam");
    if (neighborhood == null) {
      return null;
    }
    return new Area(neighborhood.getTextContent(), municipality.getTextContent());
  }
}
