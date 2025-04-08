/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.http.HttpStatus.ACCEPTED
import io.javalin.http.HttpStatus.ALREADY_REPORTED
import io.javalin.http.HttpStatus.BAD_GATEWAY
import io.javalin.http.HttpStatus.BAD_REQUEST
import io.javalin.http.HttpStatus.CLIENT_CLOSED_REQUEST
import io.javalin.http.HttpStatus.CONFLICT
import io.javalin.http.HttpStatus.CONTENT_TOO_LARGE
import io.javalin.http.HttpStatus.CONTINUE
import io.javalin.http.HttpStatus.CREATED
import io.javalin.http.HttpStatus.EARLY_HINTS
import io.javalin.http.HttpStatus.ENHANCE_YOUR_CALM
import io.javalin.http.HttpStatus.EXPECTATION_FAILED
import io.javalin.http.HttpStatus.FAILED_DEPENDENCY
import io.javalin.http.HttpStatus.FORBIDDEN
import io.javalin.http.HttpStatus.FOUND
import io.javalin.http.HttpStatus.GATEWAY_TIMEOUT
import io.javalin.http.HttpStatus.GONE
import io.javalin.http.HttpStatus.HTTP_VERSION_NOT_SUPPORTED
import io.javalin.http.HttpStatus.IM_A_TEAPOT
import io.javalin.http.HttpStatus.IM_USED
import io.javalin.http.HttpStatus.INSUFFICIENT_STORAGE
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.HttpStatus.LENGTH_REQUIRED
import io.javalin.http.HttpStatus.LOCKED
import io.javalin.http.HttpStatus.LOOP_DETECTED
import io.javalin.http.HttpStatus.METHOD_NOT_ALLOWED
import io.javalin.http.HttpStatus.MISDIRECTED_REQUEST
import io.javalin.http.HttpStatus.MOVED_PERMANENTLY
import io.javalin.http.HttpStatus.MULTIPLE_CHOICES
import io.javalin.http.HttpStatus.MULTI_STATUS
import io.javalin.http.HttpStatus.NETWORK_AUTHENTICATION_REQUIRED
import io.javalin.http.HttpStatus.NON_AUTHORITATIVE_INFORMATION
import io.javalin.http.HttpStatus.NOT_ACCEPTABLE
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.NOT_IMPLEMENTED
import io.javalin.http.HttpStatus.NOT_MODIFIED
import io.javalin.http.HttpStatus.NO_CONTENT
import io.javalin.http.HttpStatus.OK
import io.javalin.http.HttpStatus.PARTIAL_CONTENT
import io.javalin.http.HttpStatus.PAYMENT_REQUIRED
import io.javalin.http.HttpStatus.PERMANENT_REDIRECT
import io.javalin.http.HttpStatus.PRECONDITION_FAILED
import io.javalin.http.HttpStatus.PRECONDITION_REQUIRED
import io.javalin.http.HttpStatus.PROCESSING
import io.javalin.http.HttpStatus.PROXY_AUTHENTICATION_REQUIRED
import io.javalin.http.HttpStatus.RANGE_NOT_SATISFIABLE
import io.javalin.http.HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE
import io.javalin.http.HttpStatus.REQUEST_TIMEOUT
import io.javalin.http.HttpStatus.RESET_CONTENT
import io.javalin.http.HttpStatus.SEE_OTHER
import io.javalin.http.HttpStatus.SERVICE_UNAVAILABLE
import io.javalin.http.HttpStatus.SWITCHING_PROTOCOLS
import io.javalin.http.HttpStatus.TEMPORARY_REDIRECT
import io.javalin.http.HttpStatus.TOO_EARLY
import io.javalin.http.HttpStatus.TOO_MANY_REQUESTS
import io.javalin.http.HttpStatus.UNAUTHORIZED
import io.javalin.http.HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS
import io.javalin.http.HttpStatus.UNPROCESSABLE_CONTENT
import io.javalin.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE
import io.javalin.http.HttpStatus.UPGRADE_REQUIRED
import io.javalin.http.HttpStatus.URI_TOO_LONG
import io.javalin.http.HttpStatus.USE_PROXY

open class HttpResponseException @JvmOverloads constructor(
    val status: Int,
    message: String = "",
    val details: Map<String, String> = mapOf()
) : RuntimeException(message) {
    constructor(
        status: HttpStatus,
        message: String = status.message,
        details: Map<String, String> = mapOf()
    ) : this(status.code, message, details)
}

open class ContinueResponse @JvmOverloads constructor(
    message: String = CONTINUE.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(CONTINUE, message, details)

open class SwitchingProtocolsResponse @JvmOverloads constructor(
    message: String = SWITCHING_PROTOCOLS.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(SWITCHING_PROTOCOLS, message, details)

open class ProcessingResponse @JvmOverloads constructor(
    message: String = PROCESSING.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(PROCESSING, message, details)

open class EarlyHintsResponse @JvmOverloads constructor(
    message: String = EARLY_HINTS.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(EARLY_HINTS, message, details)

open class OkResponse @JvmOverloads constructor(
    message: String = OK.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(OK, message, details)

open class CreatedResponse @JvmOverloads constructor(
    message: String = CREATED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(CREATED, message, details)

open class AcceptedResponse @JvmOverloads constructor(
    message: String = ACCEPTED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(ACCEPTED, message, details)

open class NonAuthoritativeInformationResponse @JvmOverloads constructor(
    message: String = NON_AUTHORITATIVE_INFORMATION.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(NON_AUTHORITATIVE_INFORMATION, message, details)

open class NoContentResponse @JvmOverloads constructor(
    message: String = NO_CONTENT.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(NO_CONTENT, message, details)

open class ResetContentResponse @JvmOverloads constructor(
    message: String = RESET_CONTENT.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(RESET_CONTENT, message, details)

open class PartialContentResponse @JvmOverloads constructor(
    message: String = PARTIAL_CONTENT.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(PARTIAL_CONTENT, message, details)

open class MultiStatusResponse @JvmOverloads constructor(
    message: String = MULTI_STATUS.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(MULTI_STATUS, message, details)

open class AlreadyReportedResponse @JvmOverloads constructor(
    message: String = ALREADY_REPORTED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(ALREADY_REPORTED, message, details)

open class ImUsedResponse @JvmOverloads constructor(
    message: String = IM_USED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(IM_USED, message, details)

open class RedirectResponse @JvmOverloads constructor(
    status: HttpStatus = FOUND,
    message: String = status.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(status, message, details)

open class MultipleChoicesResponse @JvmOverloads constructor(
    message: String = MULTIPLE_CHOICES.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(MULTIPLE_CHOICES, message, details)

open class MovedPermanentlyResponse @JvmOverloads constructor(
    message: String = MOVED_PERMANENTLY.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(MOVED_PERMANENTLY, message, details)

open class FoundResponse @JvmOverloads constructor(
    message: String = FOUND.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(FOUND, message, details)

open class SeeOtherResponse @JvmOverloads constructor(
    message: String = SEE_OTHER.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(SEE_OTHER, message, details)

open class NotModifiedResponse @JvmOverloads constructor(
    message: String = NOT_MODIFIED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(NOT_MODIFIED, message, details)

open class UseProxyResponse @JvmOverloads constructor(
    message: String = USE_PROXY.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(USE_PROXY, message, details)

open class TemporaryRedirectResponse @JvmOverloads constructor(
    message: String = TEMPORARY_REDIRECT.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(TEMPORARY_REDIRECT, message, details)

open class PermanentRedirectResponse @JvmOverloads constructor(
    message: String = PERMANENT_REDIRECT.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(PERMANENT_REDIRECT, message, details)

open class BadRequestResponse @JvmOverloads constructor(
    message: String = BAD_REQUEST.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(BAD_REQUEST, message, details)

open class UnauthorizedResponse @JvmOverloads constructor(
    message: String = UNAUTHORIZED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(UNAUTHORIZED, message, details)

open class PaymentRequiredResponse @JvmOverloads constructor(
    message: String = PAYMENT_REQUIRED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(PAYMENT_REQUIRED, message, details)

open class ForbiddenResponse @JvmOverloads constructor(
    message: String = FORBIDDEN.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(FORBIDDEN, message, details)

open class NotFoundResponse @JvmOverloads constructor(
    message: String = NOT_FOUND.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(NOT_FOUND, message, details)

open class MethodNotAllowedResponse @JvmOverloads constructor(
    message: String = METHOD_NOT_ALLOWED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(METHOD_NOT_ALLOWED, message, details)

open class NotAcceptableResponse @JvmOverloads constructor(
    message: String = NOT_ACCEPTABLE.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(NOT_ACCEPTABLE, message, details)

open class ProxyAuthenticationRequiredResponse @JvmOverloads constructor(
    message: String = PROXY_AUTHENTICATION_REQUIRED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(PROXY_AUTHENTICATION_REQUIRED, message, details)

open class RequestTimeoutResponse @JvmOverloads constructor(
    message: String = REQUEST_TIMEOUT.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(REQUEST_TIMEOUT, message, details)

open class ConflictResponse @JvmOverloads constructor(
    message: String = CONFLICT.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(CONFLICT, message, details)

open class GoneResponse @JvmOverloads constructor(
    message: String = GONE.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(GONE, message, details)

open class LengthRequiredResponse @JvmOverloads constructor(
    message: String = LENGTH_REQUIRED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(LENGTH_REQUIRED, message, details)

open class PreconditionFailedResponse @JvmOverloads constructor(
    message: String = PRECONDITION_FAILED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(PRECONDITION_FAILED, message, details)

open class ContentTooLargeResponse @JvmOverloads constructor(
    message: String = CONTENT_TOO_LARGE.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(CONTENT_TOO_LARGE, message, details)

open class UriTooLongResponse @JvmOverloads constructor(
    message: String = URI_TOO_LONG.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(URI_TOO_LONG, message, details)

open class UnsupportedMediaTypeResponse @JvmOverloads constructor(
    message: String = UNSUPPORTED_MEDIA_TYPE.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(UNSUPPORTED_MEDIA_TYPE, message, details)

open class RangeNotSatisfiableResponse @JvmOverloads constructor(
    message: String = RANGE_NOT_SATISFIABLE.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(RANGE_NOT_SATISFIABLE, message, details)

open class ExpectationFailedResponse @JvmOverloads constructor(
    message: String = EXPECTATION_FAILED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(EXPECTATION_FAILED, message, details)

open class ImATeapotResponse @JvmOverloads constructor(
    message: String = IM_A_TEAPOT.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(IM_A_TEAPOT, message, details)

open class EnhanceYourCalmResponse @JvmOverloads constructor(
    message: String = ENHANCE_YOUR_CALM.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(ENHANCE_YOUR_CALM, message, details)

open class MisdirectedRequestResponse @JvmOverloads constructor(
    message: String = MISDIRECTED_REQUEST.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(MISDIRECTED_REQUEST, message, details)

open class UnprocessableContentResponse @JvmOverloads constructor(
    message: String = UNPROCESSABLE_CONTENT.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(UNPROCESSABLE_CONTENT, message, details)

open class LockedResponse @JvmOverloads constructor(
    message: String = LOCKED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(LOCKED, message, details)

open class FailedDependencyResponse @JvmOverloads constructor(
    message: String = FAILED_DEPENDENCY.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(FAILED_DEPENDENCY, message, details)

open class TooEarlyResponse @JvmOverloads constructor(
    message: String = TOO_EARLY.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(TOO_EARLY, message, details)

open class UpgradeRequiredResponse @JvmOverloads constructor(
    message: String = UPGRADE_REQUIRED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(UPGRADE_REQUIRED, message, details)

open class PreconditionRequiredResponse @JvmOverloads constructor(
    message: String = PRECONDITION_REQUIRED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(PRECONDITION_REQUIRED, message, details)

open class TooManyRequestsResponse @JvmOverloads constructor(
    message: String = TOO_MANY_REQUESTS.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(TOO_MANY_REQUESTS, message, details)

open class RequestHeaderFieldsTooLargeResponse @JvmOverloads constructor(
    message: String = REQUEST_HEADER_FIELDS_TOO_LARGE.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(REQUEST_HEADER_FIELDS_TOO_LARGE, message, details)

open class UnavailableForLegalReasonsResponse @JvmOverloads constructor(
    message: String = UNAVAILABLE_FOR_LEGAL_REASONS.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(UNAVAILABLE_FOR_LEGAL_REASONS, message, details)

open class ClientClosedRequestResponse @JvmOverloads constructor(
    message: String = CLIENT_CLOSED_REQUEST.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(CLIENT_CLOSED_REQUEST, message, details)

open class InternalServerErrorResponse @JvmOverloads constructor(
    message: String = INTERNAL_SERVER_ERROR.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(INTERNAL_SERVER_ERROR, message, details)

open class NotImplementedResponse @JvmOverloads constructor(
    message: String = NOT_IMPLEMENTED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(NOT_IMPLEMENTED, message, details)

open class BadGatewayResponse @JvmOverloads constructor(
    message: String = BAD_GATEWAY.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(BAD_GATEWAY, message, details)

open class ServiceUnavailableResponse @JvmOverloads constructor(
    message: String = SERVICE_UNAVAILABLE.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(SERVICE_UNAVAILABLE, message, details)

open class GatewayTimeoutResponse @JvmOverloads constructor(
    message: String = GATEWAY_TIMEOUT.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(GATEWAY_TIMEOUT, message, details)

open class HttpVersionNotSupportedResponse @JvmOverloads constructor(
    message: String = HTTP_VERSION_NOT_SUPPORTED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(HTTP_VERSION_NOT_SUPPORTED, message, details)

open class InsufficientStorageResponse @JvmOverloads constructor(
    message: String = INSUFFICIENT_STORAGE.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(INSUFFICIENT_STORAGE, message, details)

open class LoopDetectedResponse @JvmOverloads constructor(
    message: String = LOOP_DETECTED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(LOOP_DETECTED, message, details)

open class NetworkAuthenticationRequiredResponse @JvmOverloads constructor(
    message: String = NETWORK_AUTHENTICATION_REQUIRED.message,
    details: Map<String, String> = mapOf()
) : HttpResponseException(NETWORK_AUTHENTICATION_REQUIRED, message, details)
