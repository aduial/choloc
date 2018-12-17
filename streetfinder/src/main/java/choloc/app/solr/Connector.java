package choloc.app.solr;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

// NOTE the SolrRequest class has a couple of default values, eg for the Streetname and Town token proximities
// check there please

public class Connector {

  private final String USER_AGENT = "Mozilla/5.0";

  public static void main(String[] args) throws Exception {
    Connector conn = new Connector();
    conn.testPrintResults("Korte Voorhout", "Gravenhage");
    conn.testPrintResults("Brink", "Assen");
    conn.testPrintResults("Steenstraat", "Arnhem");
  }

  public LinkedHashMap<String, String> getSolrFTHighlights(String street, String municipality) {
    SolrRequest req = new SolrRequest(street, municipality);
    try {
      return doSolrQuery(req).parseJson();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private SolrResult doSolrQuery(SolrRequest solrRequest) throws Exception {

    String solrUrl = PropertyReader.getInstance().getSolrUrl();
    String requestUrl = solrUrl + solrRequest.assembleRequestString();

    HttpClient client = HttpClientBuilder.create().build();
    HttpGet request = new HttpGet(requestUrl);
    HttpResponse response = client.execute(request);
    // add request header
    request.addHeader("User-Agent", USER_AGENT);

    System.out.println("\nSending 'GET' request to URL : " + requestUrl);
    System.out.println("Response Code : " +
        response.getStatusLine().getStatusCode());

    BufferedReader rd = new BufferedReader(
        new InputStreamReader(response.getEntity().getContent()));

    StringBuffer result = new StringBuffer();
    String line;
    while ((line = rd.readLine()) != null) {
      result.append(line);
    }
    return new SolrResult(result.toString());
  }

  private void testPrintResults(String street, String town) {
    LinkedHashMap<String, String> results = getSolrFTHighlights(street, town);
    System.out.println("+++ Results for street: " + street + ", in " + town);
    for (Map.Entry<String, String> entry : results.entrySet()) {
      System.out.println("Found fulltext for Europeana ID: " + entry.getKey());
      System.out.println(entry.getValue() + "\n\n");
    }
  }

}
