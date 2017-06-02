/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.core.util

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

import javax.servlet.http.HttpServletRequest

import io.javalin.Request
import io.javalin.core.HandlerMatch

object RequestUtil {

    fun create(httpRequest: HttpServletRequest): Request {
        return Request(httpRequest, HashMap<String, String>(), ArrayList<String>())
    }

    fun create(httpRequest: HttpServletRequest, handlerMatch: HandlerMatch): Request {
        val requestList = Util.pathToList(handlerMatch.requestUri)
        val matchedList = Util.pathToList(handlerMatch.handlerUri)
        return Request(
                httpRequest,
                getParams(requestList, matchedList),
                getSplat(requestList, matchedList)
        )
    }

    fun getSplat(request: List<String>, matched: List<String>): List<String> {
        val numRequestParts = request.size
        val numMatchedParts = matched.size
        val sameLength = numRequestParts == numMatchedParts
        val splat = ArrayList<String>()
        var i = 0
        while (i < numRequestParts && i < numMatchedParts) {
            val matchedPart = matched[i]
            if (isSplat(matchedPart)) {
                val splatParam = StringBuilder(request[i])
                if (!sameLength && i == numMatchedParts - 1) {
                    for (j in i + 1..numRequestParts - 1) {
                        splatParam.append("/")
                        splatParam.append(request[j])
                    }
                }
                splat.add(urlDecode(splatParam.toString()))
            }
            i++
        }
        return Collections.unmodifiableList(splat)
    }

    fun getParams(requestPaths: List<String>, handlerPaths: List<String>): Map<String, String> {
        val params = HashMap<String, String>()
        var i = 0
        while (i < requestPaths.size && i < handlerPaths.size) {
            val matchedPart = handlerPaths[i]
            if (isParam(matchedPart)) {
                params.put(matchedPart.toLowerCase(), urlDecode(requestPaths[i]))
            }
            i++
        }
        return Collections.unmodifiableMap(params)
    }

    fun urlDecode(s: String): String {
        try {
            return URLDecoder.decode(s.replace("+", "%2B"), "UTF-8").replace("%2B", "+")
        } catch (ignored: UnsupportedEncodingException) {
            return ""
        }

    }

    fun isParam(pathPart: String): Boolean {
        return pathPart.startsWith(":")
    }

    fun isSplat(pathPart: String): Boolean {
        return pathPart == "*"
    }

    fun byteArrayToString(bytes: ByteArray, encoding: String?): String {
        var string: String
        if (encoding != null && Charset.isSupported(encoding)) {
            try {
                string = String(bytes, Charset.forName(encoding))
            } catch (e: UnsupportedEncodingException) {
                string = String(bytes)
            }

        } else {
            string = String(bytes)
        }
        return string
    }

    @Throws(IOException::class)
    fun toByteArray(input: InputStream): ByteArray {
        val baos = ByteArrayOutputStream()
        val byteBuffer = ByteArray(1024)
        var b = input.read(byteBuffer)
        while (b != -1) {
            baos.write(byteBuffer, 0, b)
            b = input.read(byteBuffer)
        }
        return baos.toByteArray()
    }
}
