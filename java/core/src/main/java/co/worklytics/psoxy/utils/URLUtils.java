package co.worklytics.psoxy.utils;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class URLUtils {

    public static String relativeURL(URL url) {
        // The returned file portion will be the same as getPath(), plus the concatenation of the value of getQuery()
        return url.getFile();
    }

    @SneakyThrows
    public static String relativeURL(String urlAsString) {
        return relativeURL(new URL(urlAsString));
    }

    public static List<String> queryParamNames(URL url) {
        return URLEncodedUtils.parse(url.getQuery(), StandardCharsets.UTF_8).stream()
            .map(NameValuePair::getName)
            .collect(Collectors.toList());
    }

    public static Map<String, String> parseQueryParams(URL url) {
        return URLEncodedUtils.parse(url.getQuery(), StandardCharsets.UTF_8).stream()
            .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
    }

    public static String asQueryString(Map<String, String> params) {
        return URLEncodedUtils.format(params.entrySet().stream()
            .map(entry -> new BasicNameValuePair(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList()), StandardCharsets.UTF_8);
    }
}
