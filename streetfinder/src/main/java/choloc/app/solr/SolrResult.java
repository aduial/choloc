package choloc.app.solr;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.LinkedHashMap;
import java.util.Map;

public class SolrResult {

    private String jsonResult;

    public SolrResult(String jsonResult){
        this.jsonResult = jsonResult;
    }

    public String getJsonResult() {
        return jsonResult;
    }

    public LinkedHashMap<String, String> parseJson(){
        JsonObject jsonObject = new JsonParser().parse(getJsonResult()).getAsJsonObject();
        LinkedHashMap<String, String> results = new LinkedHashMap<>();
        String europeanaId = "";

        String numberFound = jsonObject.getAsJsonObject("response").getAsJsonPrimitive("numFound").getAsString();
        System.out.println(numberFound);
        for (Map.Entry<String, JsonElement> member : jsonObject.getAsJsonObject("highlighting").entrySet()){
            europeanaId = member.getKey();
            for (Map.Entry<String, JsonElement> hlLang : member.getValue().getAsJsonObject().entrySet()){
                if (hlLang.getValue().toString().length() > 2) {
                    results.put(europeanaId + " (" + hlLang.getKey() + ")", hlLang.getValue().getAsString());
                }
            }
        }
        return results;
    }
}
