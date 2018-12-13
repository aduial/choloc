package choloc.app.streetfinder;

import java.util.Objects;

public class StreetId {

  private final String streetName;
  private final String placeName;
  private final String municipalityName;

  public StreetId(String streetName, String placeName, String municipalityName) {
    this.streetName = streetName;
    this.placeName = placeName;
    this.municipalityName = municipalityName;
  }

  public String getStreetName() {
    return streetName;
  }

  public String getPlaceName() {
    return placeName;
  }

  public String getMunicipalityName() {
    return municipalityName;
  }

  @Override
  public final boolean equals(Object obj) {
    if (!(obj instanceof StreetId)) {
      return false;
    }
    final StreetId other = (StreetId) obj;
    return this.streetName.equals(other.streetName) && this.placeName.equals(other.placeName)
        && this.municipalityName.equals(other.municipalityName);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(streetName, placeName, municipalityName);
  }
}
