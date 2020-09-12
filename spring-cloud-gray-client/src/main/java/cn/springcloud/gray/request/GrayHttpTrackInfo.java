package cn.springcloud.gray.request;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GrayHttpTrackInfo extends GrayTrackInfo {

    public static final String ATTRIBUTE_HTTP_METHOD = "method";
    public static final String ATTRIBUTE_HTTP_URI = "uri";
    public static final String ATTRIBUTE_HTTP_HEADER = "header";
    public static final String ATTRIBUTE_HTTP_PARAMETER = "param";


    public static final String GRAY_TRACK_HEADER_PREFIX = GRAY_TRACK_PREFIX + ATTRIBUTE_HTTP_HEADER;

    public static final String GRAY_TRACK_METHOD = GRAY_TRACK_PREFIX + ATTRIBUTE_HTTP_METHOD;

    public static final String GRAY_TRACK_PARAMETER_PREFIX = GRAY_TRACK_PREFIX + ATTRIBUTE_HTTP_PARAMETER;

    public static final String GRAY_TRACK_URI = GRAY_TRACK_PREFIX + ATTRIBUTE_HTTP_URI;

    private HttpHeaders headers = new HttpHeaders();
    private MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

    public void addHeader(String name, String value) {
        headers.add(name.toLowerCase(), value);
    }

    public void setHeader(String name, List<String> values) {
        headers.put(name.toLowerCase(), values);
    }

    public List<String> getHeader(String name) {
        return headers.get(name.toLowerCase());
    }


    public void addParameter(String name, String value) {
        parameters.add(name.toLowerCase(), value);
    }

    public void setParameters(String name, List<String> value) {
        parameters.put(name.toLowerCase(), value);
    }

    public List<String> getParameter(String name) {
        return parameters.get(name.toLowerCase());
    }

    public Set<String> headerNames() {
        return headers.keySet();
    }

    public Set<String> parameterNames() {
        return parameters.keySet();
    }

    public Map<String, List<String>> getHeaders() {
        return MapUtils.unmodifiableMap(headers);
    }

    public Map<String, List<String>> getParameters() {
        return MapUtils.unmodifiableMap(parameters);
    }

    public void setUri(String url) {
        setAttribute(ATTRIBUTE_HTTP_URI, url);
    }

    public String getUri() {
        return StringUtils.defaultString(getAttribute(ATTRIBUTE_HTTP_URI));
    }

    public void setMethod(String method) {
        setAttribute(ATTRIBUTE_HTTP_METHOD, method);
    }

    public String getMethod() {
        return StringUtils.defaultString(getAttribute(ATTRIBUTE_HTTP_METHOD));
    }


    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put(ATTRIBUTE_HTTP_HEADER, getHeaders());
        map.put(ATTRIBUTE_HTTP_PARAMETER, getParameters());
        return map;
    }
}
