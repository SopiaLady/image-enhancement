package com.ganwork.util;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import java.util.Map;

public class JsonPathExtractor {
    private static final Configuration config = Configuration.defaultConfiguration()
            .addOptions(Option.SUPPRESS_EXCEPTIONS);

    public static Object extract(Object document, String path) {
        if (document == null || path == null || path.isBlank()) {
            return null;
        }

        try {
            // 处理Map类型
            if (document instanceof Map) {
                return JsonPath.using(config).parse(document).read(path);
            }
            // 处理JSON字符串
            else if (document instanceof String) {
                return JsonPath.using(config).parse((String) document).read(path);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}