package choloc.app.streetfinder;

import java.util.Objects;

public class Area {

  private final String areaName;
  private final String municipality;

  public Area(String areaName, String municipality) {
    this.areaName = areaName;
    this.municipality = municipality;
  }

  public String getAreaName() {
    return areaName;
  }

  public String getMunicipality() {
    return municipality;
  }

  @Override
  public final boolean equals(Object obj) {
    if (!(obj instanceof Area)) {
      return false;
    }
    final Area other = (Area) obj;
    return this.areaName.equals(other.areaName) && this.municipality.equals(other.municipality);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(areaName, municipality);
  }

}
