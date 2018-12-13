package choloc.app.streetfinder;

public class Street {

  private final StreetId streetId;
  private final double lat;
  private final double lon;
  private final int distanceInMeters;

  public Street(StreetId streetId, double lat, double lon, int distanceInMeters) {
    this.streetId = streetId;
    this.lat = lat;
    this.lon = lon;
    this.distanceInMeters = distanceInMeters;
  }

  public String getStreetName() {
    return streetId.getStreetName();
  }

  public String getPlaceName() {
    return streetId.getPlaceName();
  }

  public double getLat() {
    return lat;
  }

  public double getLon() {
    return lon;
  }

  public int getDistanceInMeters() {
    return distanceInMeters;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Street)) {
      return false;
    }
    final Street other = (Street) obj;
    return this.streetId.equals(other.streetId);
  }

  @Override
  public int hashCode() {
    return streetId.hashCode();
  }
}
