package choloc.app.streetfinder;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

public abstract class GeoManipulator {

  private final MathTransform rdToLatLonConversion;
  private final MathTransform latLonToRdConversion;

  GeoManipulator() throws FactoryException {
    latLonToRdConversion = CRS.findMathTransform(CRS.decode("EPSG:4326"), CRS.decode("EPSG:28992"));
    rdToLatLonConversion = CRS.findMathTransform(CRS.decode("EPSG:28992"), CRS.decode("EPSG:4326"));
  }

  protected LatLon convertToLatLon(Double rdPoint) throws TransformException {
    final Coordinate coordinate = new Coordinate(rdPoint.x, rdPoint.y);
    JTS.transform(coordinate, coordinate, rdToLatLonConversion);
    return new LatLon(coordinate.x, coordinate.y);
  }

  protected Double convertToRd(LatLon latLon) throws TransformException {
    final Coordinate coordinate = new Coordinate(latLon.lat, latLon.lon);
    JTS.transform(coordinate, coordinate, latLonToRdConversion);
    return new Double(coordinate.x, coordinate.y);
  }

  public static class LatLon {

    final double lat;
    final double lon;

    public LatLon(double lat, double lon) {
      this.lat = lat;
      this.lon = lon;
    }
  }
}
