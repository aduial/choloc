package choloc.app.solr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyReader {

    private static PropertyReader instance = null;

    private Properties props = null;
    private String solrUrl = "";
    private String streetTokenDist = "";
    private String townTokenDist = "";

    String propFileName = "config.properties";

    private PropertyReader() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName)) {
            props = new Properties();
            if (inputStream != null) props.load(inputStream);
            solrUrl = props.getProperty("solrurl");
            streetTokenDist = props.getProperty("streettokendist");
            townTokenDist = props.getProperty("towntokendist");
        } catch (IOException e) {
            System.out.println("Whoops! IOException thrown when trying to read 'solrurl' property " +
                    "from property file " + propFileName);
        }
    }

    public static synchronized PropertyReader getInstance() {
        if (instance == null)
            instance = new PropertyReader();
        return instance;
    }

    public String getSolrUrl(){
        return this.props.getProperty("solrurl");
    }
    public String getStreetTokenDist(){
        return this.props.getProperty("streettokendist");
    }
    public String getTownTokenDist(){
        return this.props.getProperty("towntokendist");
    }


}
