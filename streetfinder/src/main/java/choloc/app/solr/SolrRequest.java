package choloc.app.solr;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.apache.commons.lang3.StringUtils;

public class SolrRequest {

    private String query;
    private String fl = "provider_aggregation_edm_isShownBy, provider_aggregation_edm_isShownAt";
    private String hlfl = "fulltext.*";

    private String hl = "on";
    private String indent = "on";
    private String wt = "json";

    private String streetName = "";
    private String townName = "";

    public SolrRequest(String streetName, String townName){
        this.streetName = streetName;
        this.townName = townName;
    }

    public SolrRequest(String streetName, String townName, String fl, String hlfl){
        this(streetName, townName);
        this.fl = fl;
        this.hlfl = hlfl;
    }

    public SolrRequest(String streetName, String townName, String hl, String indent, String fl, String hlfl, String wt){
        this(streetName, townName, fl, hlfl);
        this.hl = hl;
        this.indent = indent;
        this.fl = fl;
        this.hlfl = hlfl;
        this.wt = wt;
    }

    // "Korte Voorhout"~2 AND fulltext:"Voorhout haag"~30&

    private String assembleQuery() {
        // fulltext:"brink assen"~30
        String query = "fulltext:";
        if (StringUtils.isNoneBlank(streetName, townName)){
            if (StringUtils.contains(streetName, " ")){
                String part0 = StringUtils.split(streetName, " ")[0];
                String part1 = StringUtils.split(streetName, " ")[1];
                return query + "\"" + part0 + " " + part1 + "\"~"
                        + PropertyReader.getInstance().getStreetTokenDist()
                        + " AND fulltext:\""
                        + part1 + " " + townName + "\"~"
                        + PropertyReader.getInstance().getTownTokenDist();

            } else {
                return query + "\"" + streetName + " " + townName + "\"~"
                        + PropertyReader.getInstance().getTownTokenDist();
            }
        }
        return query + "jammerdån";
    }

    private String getHl() {
        return hl;
    }

    public void setHl(String hl) {
        this.hl = hl;
    }

    private String getIndent() {
        return indent;
    }

    public void setIndent(String indent) {
        this.indent = indent;
    }

    private String getFl() {
        return fl;
    }

    public void setFl(String fl) {
        this.fl = fl;
    }

    private String getHlfl() {
        return hlfl;
    }

    public void setHlfl(String hlfl) {
        this.hlfl = hlfl;
    }

    private String getWt() {
        return wt;
    }

    public void setWt(String wt) {
        this.wt = wt;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public void setTownName(String townName) {
        this.townName = townName;
    }

    public String assembleRequestString(){
        StringBuilder sb = new StringBuilder();
        sb.append("select?");
        try {
            sb.append("fl=" + URLEncoder.encode(getFl(), "UTF-8") + "&");
            sb.append("hl.fl=" + URLEncoder.encode(getHlfl(), "UTF-8") + "&");
            sb.append("hl=" + URLEncoder.encode(getHl(), "UTF-8") + "&");
            sb.append("indent=" + URLEncoder.encode(getIndent(), "UTF-8") + "&");
            sb.append("q=" + URLEncoder.encode(assembleQuery(), "UTF-8") + "&");
            sb.append("wt=" + URLEncoder.encode(getWt(), "UTF-8") + "&");
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

}
