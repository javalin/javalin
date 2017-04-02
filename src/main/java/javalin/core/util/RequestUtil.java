// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import javalin.Request;
import javalin.core.HandlerMatch;

public class RequestUtil {

    public static Request create(HttpServletRequest httpRequest) {
        return new Request(httpRequest, new HashMap<>(), new ArrayList<>());
    }

    public static Request create(HttpServletRequest httpRequest, HandlerMatch handlerMatch) {
        List<String> requestList = Util.pathToList(handlerMatch.requestUri);
        List<String> matchedList = Util.pathToList(handlerMatch.handlerUri);
        return new Request(
            httpRequest,
            getParams(requestList, matchedList),
            getSplat(requestList, matchedList)
        );
    }

    public static List<String> getSplat(List<String> request, List<String> matched) {
        int numRequestParts = request.size();
        int numMatchedParts = matched.size();
        boolean sameLength = (numRequestParts == numMatchedParts);
        List<String> splat = new ArrayList<>();
        for (int i = 0; (i < numRequestParts) && (i < numMatchedParts); i++) {
            String matchedPart = matched.get(i);
            if (isSplat(matchedPart)) {
                StringBuilder splatParam = new StringBuilder(request.get(i));
                if (!sameLength && (i == (numMatchedParts - 1))) {
                    for (int j = i + 1; j < numRequestParts; j++) {
                        splatParam.append("/");
                        splatParam.append(request.get(j));
                    }
                }
                splat.add(urlDecode(splatParam.toString()));
            }
        }
        return Collections.unmodifiableList(splat);
    }

    public static Map<String, String> getParams(List<String> requestPaths, List<String> handlerPaths) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; (i < requestPaths.size()) && (i < handlerPaths.size()); i++) {
            String matchedPart = handlerPaths.get(i);
            if (isParam(matchedPart)) {
                params.put(matchedPart.toLowerCase(), urlDecode(requestPaths.get(i)));
            }
        }
        return Collections.unmodifiableMap(params);
    }

    public static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s.replace("+", "%2B"), "UTF-8").replace("%2B", "+");
        } catch (UnsupportedEncodingException ignored) {
            return "";
        }
    }

    public static boolean isParam(String pathPart) {
        return pathPart.startsWith(":");
    }

    public static boolean isSplat(String pathPart) {
        return pathPart.equals("*");
    }

    public static String byteArrayToString(byte[] bytes, String encoding) {
        String string;
        if (encoding != null && Charset.isSupported(encoding)) {
            try {
                string = new String(bytes, encoding);
            } catch (UnsupportedEncodingException e) {
                string = new String(bytes);
            }
        } else {
            string = new String(bytes);
        }
        return string;
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] byteBuffer = new byte[1024];
        for (int b = input.read(byteBuffer); b != -1; b = input.read(byteBuffer)) {
            baos.write(byteBuffer, 0, b);
        }
        return baos.toByteArray();
    }
}
