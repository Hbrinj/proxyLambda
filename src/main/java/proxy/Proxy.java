package proxy;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class Proxy implements RequestStreamHandler {
    private static final String KEY_QUERY_PARAM = "queryStringParameters";
    private static final String KEY_PATH_PARAM = "pathParameters";
    private static final String KEY_PROXY_PATH = "proxy";
    private static final String KEY_HEADERS_PARAM = "headers";

    private JSONParser parser = new JSONParser();


    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
        Optional<Map> queryStringParams = Optional.empty();
        Map pathParams;
        Map event;
        Optional<Map> headers = Optional.empty();
        Optional<String> endpoint = Optional.empty();
        JSONObject json_response = new JSONObject();
        ContainerFactory jsonFactory = new JsonObjectFactory();
        LambdaLogger logger = context.getLogger();
        logger.log("Reading from inputstream");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            logger.log("Inside the resources");
            event = (Map) parser.parse(reader, jsonFactory);

            if (event.containsKey(KEY_QUERY_PARAM)) {
                logger.log("Hadd query String Params");
                queryStringParams = Optional.of((Map) event.get(KEY_QUERY_PARAM));
            }

            if (null != (pathParams = (Map) event.get(KEY_PATH_PARAM))) {
                logger.log("Hadd path String Params");
                endpoint = Optional.of((String) pathParams.get(KEY_PROXY_PATH));
            }

            if (event.containsKey(KEY_HEADERS_PARAM)) {
                logger.log("Hadd headers String Params");
                headers = Optional.of((Map) event.get(KEY_HEADERS_PARAM));
            }

            headers.ifPresent(headerMap -> {
                headerMap.put("derp", "somederpyHeader");
                json_response.put(KEY_HEADERS_PARAM, new JSONObject(headerMap));
            });
            queryStringParams.ifPresent(queryMap -> json_response.put(KEY_QUERY_PARAM, new JSONObject(queryMap)));
            endpoint.ifPresent(path -> json_response.put(KEY_PATH_PARAM, path));

            json_response.writeJSONString(writer);

            //TODO: this catch is too generic
        } catch (Exception e) {
            logger.log("ERROR: some shit went down:\n" + e.getMessage());
        }
    }

    private class JsonObjectFactory implements ContainerFactory {
        public List creatArrayContainer() {
            return new LinkedList();
        }

        public Map createObjectContainer() {
            return new LinkedHashMap();
        }
    }
}
