package choloc.app.streetfinder;

import java.util.Objects;

public class StreetId {

  private final String streetName;
  private final String placeName;

  public StreetId(String streetName, String placeName) {
    this.streetName = streetName;
    this.placeName = placeName;
  }

  public String getStreetName() {
    return streetName;
  }

  public String getPlaceName() {
    return placeName;
  }

  @Override
  public final boolean equals(Object obj) {
    if (!(obj instanceof StreetId)) {
      return false;
    }
    final StreetId other = (StreetId) obj;
    return this.streetName.equals(other.streetName) && this.placeName.equals(other.placeName);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(streetName, placeName);
  }
}
