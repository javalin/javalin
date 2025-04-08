package io.javalin.http

/** HTTP response status codes indicating whether a specific HTTP request has been successfully completed. */
enum class HttpStatus(val code: Int, val message: String) {
    /** This interim response indicates that the client should continue the request or ignore the response if the request is already finished. */
    CONTINUE(100, "Continue"),
    /** This code is sent in response to an Upgrade request header from the client and indicates the protocol the server is switching to. */
    SWITCHING_PROTOCOLS(101, "Switching Protocols"),
    /** This code indicates that the server has received and is processing the request, but no response is available yet. */
    PROCESSING(102, "Processing"),
    /** This status code is primarily intended to be used with the Link header, letting the user agent start preloading resources while the server prepares a response or preconnect to an origin from which the page will need resources.*/
    EARLY_HINTS(103, "Early Hints"),

    /** The request succeeded. */
    OK(200, "OK"),
    /** The request succeeded, and a new resource was created as a result. This is typically the response sent after POST requests, or some PUT requests. */
    CREATED(201, "Created"),
    /** The request has been received but not yet acted upon. It is noncommittal, since there is no way in HTTP to later send an asynchronous response indicating the outcome of the request. It is intended for cases where another process or server handles the request, or for batch processing. */
    ACCEPTED(202, "Accepted"),
    /** This response code means the returned metadata is not exactly the same as is available from the origin server, but is collected from a local or a third-party copy. This is mostly used for mirrors or backups of another resource. Except for that specific case, the 200 OK response is preferred to this status. */
    NON_AUTHORITATIVE_INFORMATION(203, "Non Authoritative Information"),
    /** There is no content to send for this request, but the headers may be useful. The user agent may update its cached headers for this resource with the new ones.*/
    NO_CONTENT(204, "No Content"),
    /** Tells the user agent to reset the document which sent this request. */
    RESET_CONTENT(205, "Reset Content"),
    /** This response code is used when the Range header is sent from the client to request only part of a resource. */
    PARTIAL_CONTENT(206, "Partial Content"),
    /** Conveys information about multiple resources, for situations where multiple status codes might be appropriate. */
    MULTI_STATUS(207, "Multi-Status"),
    /** Used inside a <dav:propstat> response element to avoid repeatedly enumerating the internal members of multiple bindings to the same collection. */
    ALREADY_REPORTED(208, "Already Reported"),
    /** The server has fulfilled a GET request for the resource, and the response is a representation of the result of one or more instance-manipulations applied to the current instance. */
    IM_USED(226, "IM Used"),

    /** The request has more than one possible response. The user agent or user should choose one of them. */
    MULTIPLE_CHOICES(300, "Multiple Choices"),
    /** The URL of the requested resource has been changed permanently. The new URL is given in the response. */
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    /** This response code means that the URI of requested resource has been changed temporarily. Further changes in the URI might be made in the future. Therefore, this same URI should be used by the client in future requests. */
    FOUND(302, "Found"),
    /** The server sent this response to direct the client to get the requested resource at another URI with a GET request. */
    SEE_OTHER(303, "See Other"),
    /** This is used for caching purposes. It tells the client that the response has not been modified, so the client can continue to use the same cached version of the response. */
    NOT_MODIFIED(304, "Not Modified"),
    /** Defined in a previous version of the HTTP specification to indicate that a requested response must be accessed by a proxy. */
    @Deprecated("It has been deprecated due to security concerns regarding in-band configuration of a proxy.")
    USE_PROXY(305, "Use Proxy"),
    /** The server sends this response to direct the client to get the requested resource at another URI with the same method that was used in the prior request.  */
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),
    /** This means that the resource is now permanently located at another URI, specified by the Location: HTTP Response header. */
    PERMANENT_REDIRECT(308, "Permanent Redirect"),

    /** The server cannot or will not process the request due to something that is perceived to be a client error (e.g., malformed request syntax, invalid request message framing, or deceptive request routing). */
    BAD_REQUEST(400, "Bad Request"),
    /** Although the HTTP standard specifies "unauthorized", semantically this response means "unauthenticated". That is, the client must authenticate itself to get the requested response. */
    UNAUTHORIZED(401, "Unauthorized"),
    /** This response code is reserved for future use. The initial aim for creating this code was using it for digital payment systems, however this status code is used very rarely and no standard convention exists. */
    PAYMENT_REQUIRED(402, "Payment Required"),
    /** The client does not have access rights to the content; that is, it is unauthorized, so the server is refusing to give the requested resource. Unlike 401 Unauthorized, the client's identity is known to the server. */
    FORBIDDEN(403, "Forbidden"),
    /** The server cannot find the requested resource. In the browser, this means the URL is not recognized.  */
    NOT_FOUND(404, "Not Found"),
    /** The request method is known by the server but is not supported by the target resource. For example, an API may not allow calling DELETE to remove a resource. */
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    /** This response is sent when the web server, after performing server-driven content negotiation, doesn't find any content that conforms to the criteria given by the user agent. */
    NOT_ACCEPTABLE(406, "Not Acceptable"),
    /** This is similar to `401 Unauthorized` but authentication is needed to be done by a proxy. */
    PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
    /** This response is sent on an idle connection by some servers, even without any previous request by the client. It means that the server would like to shut down this unused connection. */
    REQUEST_TIMEOUT(408, "Request Timeout"),
    /** This response is sent when a request conflicts with the current state of the server. */
    CONFLICT(409, "Conflict"),
    /** This response is sent when the requested content has been permanently deleted from server, with no forwarding address. */
    GONE(410, "Gone"),
    /** Server rejected the request because the Content-Length header field is not defined and the server requires it. */
    LENGTH_REQUIRED(411, "Length Required"),
    /** The client has indicated preconditions in its headers which the server does not meet. */
    PRECONDITION_FAILED(412, "Precondition Failed"),
    /** Request entity is larger than limits defined by server. The server might close the connection or return an Retry-After header field.  */
    CONTENT_TOO_LARGE(413, "Content Too Large"),
    /** The URI requested by the client is longer than the server is willing to interpret. */
    URI_TOO_LONG(414, "URI Too Long"),
    /** The media format of the requested data is not supported by the server, so the server is rejecting the request. */
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    /** The range specified by the Range header field in the request cannot be fulfilled. It's possible that the range is outside the size of the target URI's data. */
    RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable"),
    /** This response code means the expectation indicated by the Expect request header field cannot be met by the server. */
    EXPECTATION_FAILED(417, "Expectation Failed"),
    /** The server refuses the attempt to brew coffee with a teapot. */
    IM_A_TEAPOT(418, "I'm a Teapot"),
    /** Unofficial status code, used in Twitter API to indicate that the client is being rate limited for making too many requests.  */
    ENHANCE_YOUR_CALM(420, "Enhance your Calm"),
    /** The request was directed at a server that is not able to produce a response. This can be sent by a server that is not configured to produce responses for the combination of scheme and authority that are included in the request URI. */
    MISDIRECTED_REQUEST(421, "Misdirected Request"),
    /** The request was well-formed but was unable to be followed due to semantic errors. */
    UNPROCESSABLE_CONTENT(422, "Unprocessable Content"),
    /** The resource that is being accessed is locked, meaning it can't be accessed. */
    LOCKED(423, "Locked"),
    /** The request failed due to failure of a previous request. */
    FAILED_DEPENDENCY(424, "Failed Dependency"),
    /** Indicates that the server is unwilling to risk processing a request that might be replayed. */
    TOO_EARLY(425, "Too Early"),
    /** The server refuses to perform the request using the current protocol but might be willing to do so after the client upgrades to a different protocol. */
    UPGRADE_REQUIRED(426, "Upgrade Required"),
    /** The origin server requires the request to be conditional. This response is intended to prevent the 'lost update' problem, where a client GETs a resource's state, modifies it and PUTs it back to the server, when meanwhile a third party has modified the state on the server, leading to a conflict. */
    PRECONDITION_REQUIRED(428, "Precondition Required"),
    /** The user has sent too many requests in a given amount of time ("rate limiting"). */
    TOO_MANY_REQUESTS(429, "Too Many Requests"),
    /** The server is unwilling to process the request because its header fields are too large. The request may be resubmitted after reducing the size of the request header fields. */
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),
    /** The user agent requested a resource that cannot legally be provided, such as a web page censored by a government. */
    UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable for Legal Reason"),
    /** The client has closed the connection prematurely, before the server could deliver a response. */
    CLIENT_CLOSED_REQUEST(499, "Client Closed Request"),

    /** The server has encountered a situation it does not know how to handle. */
    INTERNAL_SERVER_ERROR(500, "Server Error"),
    /** The request method is not supported by the server and cannot be handled. The only methods that servers are required to support (and therefore that must not return this code) are GET and HEAD. */
    NOT_IMPLEMENTED(501, "Not Implemented"),
    /** This error response means that the server, while working as a gateway to get a response needed to handle the request, got an invalid response. */
    BAD_GATEWAY(502, "Bad Gateway"),
    /** The server is not ready to handle the request. Common causes are a server that is down for maintenance or that is overloaded. */
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    /** This error response is given when the server is acting as a gateway and cannot get a response in time. */
    GATEWAY_TIMEOUT(504, "Gateway Timeout"),
    /** The HTTP version used in the request is not supported by the server. */
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported"),
    /** The method could not be performed on the resource because the server is unable to store the representation needed to successfully complete the request. */
    INSUFFICIENT_STORAGE(507, "Insufficient Storage"),
    /** The server detected an infinite loop while processing the request. */
    LOOP_DETECTED(508, "Loop Detected"),
    /** Indicates that the client needs to authenticate to gain network access. */
    NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required"),
    /** An unknown status code */
    UNKNOWN(-1, "Unknown HTTP code");

    /** Checks if the status code is informational: the request was received, continuing process */
    fun isInformational(): Boolean = code in 100..199
    /** Checks if the status code indicates success: the request was successfully received, understood, and accepted */
    fun isSuccess(): Boolean = code in 200..299
    /** Checks if the status code indicates a redirection: further action needs to be taken in order to complete the request */
    fun isRedirection(): Boolean = code in 300..399
    /** Checks if the status code indicates a client error: the request contains bad syntax or cannot be fulfilled */
    fun isClientError(): Boolean = code in 400..499
    /** Checks if the status code indicates a server error: the server failed to fulfil an apparently valid request*/
    fun isServerError(): Boolean = code in 500..599
    /** Checks if the status code indicates an error. */
    fun isError(): Boolean = isClientError() || isServerError()

    override fun toString(): String = "$code $message"

    companion object {

        private val statusMap = HttpStatus.entries.associateBy { it.code }

        @JvmStatic
        fun forStatus(status: Int): HttpStatus = statusMap[status] ?: UNKNOWN

    }

}
