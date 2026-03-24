package io.javalin.plugin.bundled

import io.javalin.util.JavalinLogger
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.net.HttpURLConnection
import java.net.URI

/**
 * A servlet Filter that proxies all HTTP requests to a configurable target port on localhost.
 * Used by [DevReloadPlugin] in parent mode to forward requests to the child app process.
 *
 * Before proxying, calls [reloadCheck] to compile and restart the child if source files changed.
 * If a reload is in progress (another thread already compiling), returns a spinner page.
 * If compilation fails, shows the error output.
 */
internal class DevReloadProxy : Filter {

    @Volatile var targetPort: Int = -1
    @Volatile var reloadingFiles: List<String> = emptyList()
    @Volatile var compileError: String? = null

    /** Called on each request to check for changes and reload if needed. Set by DevReloadPlugin. */
    @Volatile var reloadCheck: (() -> Unit)? = null

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val req = request as HttpServletRequest
        val res = response as HttpServletResponse

        // Lightweight status endpoint for JS polling — never proxied to child
        if (req.requestURI == "/__dev-reload/status") {
            val files = reloadingFiles
            res.contentType = "application/json"
            if (files.isEmpty() && targetPort > 0) {
                res.status = 200
                res.writer.write("""{"ready":true}""")
            } else {
                res.status = 503
                res.writer.write("""{"ready":false}""")
            }
            return
        }

        // Check for changes and reload on demand
        reloadCheck?.invoke()

        // If a reload is in progress (triggered by this or another thread), show spinner/error
        val files = reloadingFiles
        if (files.isNotEmpty()) {
            res.status = 503
            res.contentType = "text/html; charset=utf-8"
            val error = compileError
            if (error != null) {
                res.writer.write(compileErrorPage(error, files))
            } else {
                res.writer.write(reloadingPage(files))
            }
            return
        }

        val port = targetPort
        if (port < 0) {
            res.status = 503
            res.contentType = "text/html; charset=utf-8"
            res.writer.write(reloadingPage(emptyList()))
            return
        }

        try {
            // Build target URL
            val query = req.queryString?.let { "?$it" } ?: ""
            val url = URI("http://localhost:$port${req.requestURI}$query").toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = req.method
            conn.connectTimeout = 10_000
            conn.readTimeout = 60_000
            conn.instanceFollowRedirects = false

            // Forward request headers
            for (name in req.headerNames) {
                if (name.equals("host", ignoreCase = true)) continue
                for (value in req.getHeaders(name)) {
                    conn.addRequestProperty(name, value)
                }
            }

            // Forward request body for POST/PUT/PATCH
            if (req.method in listOf("POST", "PUT", "PATCH")) {
                conn.doOutput = true
                req.inputStream.copyTo(conn.outputStream)
                conn.outputStream.close()
            }

            // Copy response status + headers + body
            res.status = conn.responseCode
            for ((key, values) in conn.headerFields) {
                if (key == null) continue
                if (key.equals("transfer-encoding", ignoreCase = true)) continue
                for (value in values) {
                    res.addHeader(key, value)
                }
            }
            val body = try { conn.inputStream } catch (_: Exception) { conn.errorStream }
            body?.copyTo(res.outputStream)
            conn.disconnect()

        } catch (e: java.net.ConnectException) {
            res.status = 503
            res.contentType = "text/html; charset=utf-8"
            res.writer.write(reloadingPage(reloadingFiles))
        } catch (e: Exception) {
            JavalinLogger.warn("DevReloadPlugin proxy error: ${e.message}")
            res.status = 502
            res.contentType = "text/plain"
            res.writer.write("DevReloadPlugin: proxy error — ${e.message}")
        }
    }

    companion object {
        private fun escapeHtml(s: String) = s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

        private fun formatFileList(files: List<String>): String {
            if (files.isEmpty()) return ""
            if (files.size > 5) return "<p class=\"files-summary\">${files.size} files changed</p>"
            val items = files.joinToString("") { "<li>${escapeHtml(it)}</li>" }
            return "<ul class=\"files\">$items</ul>"
        }

        private fun reloadingPage(files: List<String>): String {
            val fileList = formatFileList(files)

            return """<!DOCTYPE html>
<html><head><meta charset="utf-8"><title>Recompiling...</title>
<style>
  *{margin:0;padding:0;box-sizing:border-box}
  body{font-family:system-ui,-apple-system,sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;background:#111;color:#ccc}
  .wrap{text-align:center}
  .spinner{margin:0 auto 24px}
  h2{font-size:18px;font-weight:500;margin-bottom:8px;color:#fff}
  .subtitle{font-size:13px;color:#888;margin-bottom:16px}
  .files{list-style:none;font-size:13px;color:#888;font-family:ui-monospace,monospace}
  .files li{margin:4px 0}
  .files li::before{content:"→ ";color:#555}
  .files-summary{font-size:13px;color:#888}
  .dots span{color:transparent;animation:dot 1.5s infinite}
  .dots span:nth-child(2){animation-delay:.3s}
  .dots span:nth-child(3){animation-delay:.6s}
  @keyframes dot{0%,80%,100%{color:transparent}40%{color:#fff}}
</style>
</head><body>
<div class="wrap">
  $SPINNER_SVG
  <h2>Recompiling<span class="dots"><span>.</span><span>.</span><span>.</span></span></h2>
  <p class="subtitle">Changes detected in</p>
  $fileList
</div>
$POLL_SCRIPT
</body></html>"""
        }

        private fun compileErrorPage(error: String, files: List<String>): String {
            val fileList = formatFileList(files)

            return """<!DOCTYPE html>
<html><head><meta charset="utf-8"><title>Compile Error</title>
<style>
  *{margin:0;padding:0;box-sizing:border-box}
  body{font-family:system-ui,-apple-system,sans-serif;display:flex;align-items:center;justify-content:center;min-height:100vh;background:#111;color:#ccc;padding:40px}
  .wrap{max-width:800px;width:100%;text-align:center}
  h2{font-size:18px;font-weight:500;margin-bottom:12px;color:#ff6b6b}
  .files{list-style:none;font-size:13px;color:#888;font-family:ui-monospace,monospace;margin-bottom:20px}
  .files li{margin:4px 0}
  .files li::before{content:"→ ";color:#555}
  .files-summary{font-size:13px;color:#888;margin-bottom:20px}
  pre{text-align:left;background:#1a1a1a;border:1px solid #333;border-radius:8px;padding:16px;font-size:12px;line-height:1.5;overflow-x:auto;color:#f88;font-family:ui-monospace,monospace;max-height:60vh;overflow-y:auto}
  .hint{font-size:12px;color:#666;margin-top:16px}
</style>
</head><body>
<div class="wrap">
  <h2>Compile Error</h2>
  $fileList
  <pre>${escapeHtml(error)}</pre>
  <p class="hint">Fix the error and save — will auto-retry</p>
</div>
$POLL_SCRIPT
</body></html>"""
        }

        private val POLL_SCRIPT = """<script>
(function(){var i=setInterval(function(){fetch("/__dev-reload/status").then(function(r){if(r.ok){clearInterval(i);location.reload()}}).catch(function(){})},100)})()
</script>"""

        private val SPINNER_SVG = """<svg class="spinner" width="64px" height="64px" viewBox="0 0 182 182" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <linearGradient id="premium-shimmer" x1="-100%" y1="0%" x2="0%" y2="0%">
      <animate attributeName="x1" values="-100%; 200%" dur="2s" repeatCount="indefinite"/>
      <animate attributeName="x2" values="0%; 300%" dur="2s" repeatCount="indefinite"/>
      <stop offset="0%" stop-color="#ffffff" stop-opacity="0.80" />
      <stop offset="50%" stop-color="#ffffff" stop-opacity="1.0" />
      <stop offset="100%" stop-color="#ffffff" stop-opacity="0.80" />
    </linearGradient>
  </defs>
  <rect width="182" height="182" fill="#111111" rx="24" />
  <path fill-rule="evenodd" fill="url(#premium-shimmer)" d="M91 0C40.7421 0 0 40.7421 0 91C0 141.258 40.7421 182 91 182C141.258 182 182 141.258 182 91C182 40.7421 141.258 0 91 0ZM103.869 33.0878C116.307 20.6497 136.473 20.6497 148.911 33.0878C161.349 45.5259 161.349 65.6921 148.911 78.1302C136.473 90.5682 116.307 90.5682 103.869 78.1302C91.431 65.6921 91.4309 45.5259 103.869 33.0878ZM165.104 68.9574C164.992 69.294 164.881 69.6169 164.787 69.8692C162.8 75.2296 159.653 80.2575 155.346 84.5647C145.947 93.9631 133.117 97.838 120.889 96.1896C120.737 96.1691 120.558 96.1424 120.367 96.1124C119.394 95.9596 118.084 96.3151 117.259 96.9484C117.33 96.8543 117.406 96.7666 117.486 96.6865L116.983 97.1887C117.067 97.1049 117.16 97.0246 117.259 96.9484C116.651 97.7543 116.394 99.0231 116.663 99.9569C116.701 100.089 116.735 100.213 116.765 100.321C121.33 117.08 117.031 135.749 103.869 148.911C92.4317 160.348 76.8365 165.093 61.9494 163.145C61.662 163.107 61.2918 163.051 60.9114 162.991C59.8711 162.825 58.6859 163.44 58.2297 164.39C58.2363 164.374 58.2432 164.357 58.2504 164.341L58.2048 164.444C58.2128 164.426 58.2211 164.408 58.2297 164.39C57.9059 165.186 58.273 166.095 59.0699 166.434C59.611 166.664 60.1922 166.908 60.6065 167.073C90.0945 178.838 125.038 172.784 148.911 148.911C170.543 127.279 177.544 96.5583 169.916 69.0426C169.826 68.7181 169.697 68.2763 169.569 67.8464C169.332 67.0469 168.476 66.5274 167.657 66.6771C166.579 66.8969 165.452 67.9171 165.104 68.9574Z" />
</svg>"""
    }
}
