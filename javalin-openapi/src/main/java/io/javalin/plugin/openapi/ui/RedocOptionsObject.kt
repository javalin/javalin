package io.javalin.plugin.openapi.ui

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

data class RedocOptionsObject(
    val disableSearch: Boolean = false,
    val expandDefaultServerVariables: Boolean = false,
    val expandResponses: String = "",
    val maxDisplayedEnumValues: Int? = null,
    val hideDownloadButton: Boolean = false,
    val hideHostname: Boolean = false,
    val hideLoading: Boolean = false,
    val hideSingleRequestSampleTab: Boolean = false,
    val expandSingleSchemaField: Boolean = false,
    val jsonSampleExpandLevel: String = "2",
    val hideSchemaTitles: Boolean = false,
    val simpleOneOfTypeLabel: Boolean = false,
    val menuToggle: Boolean = false,
    val nativeScrollbars: Boolean = false,
    val noAutoAuth: Boolean = false,
    val onlyRequiredInSamples: Boolean = false,
    val pathInMiddlePanel: Boolean = false,
    val requiredPropsFirst: Boolean = false,
    val scrollYOffset: String = "0",
    val showExtensions: Boolean = false,
    val sortPropsAlphabetically: Boolean = false,
    val suppressWarning: Boolean = false,
    val payloadSampleIdx: Int = 0,
    val untrustedSpec: Boolean = false,
    val theme: RedocOptionsTheme = RedocOptionsTheme(),
    @JsonIgnore val extras: Map<String, Any?> = emptyMap()
) {
    internal fun json(): ObjectNode {
        val mapper = ObjectMapper()
        return mapper.valueToTree<ObjectNode>(this).apply {
            extras.forEach { (key, value) -> this.replace(key, mapper.valueToTree(value)) }
        }
    }
    
    class Builder {
        private var disableSearch: Boolean = false
        private var expandDefaultServerVariables: Boolean = false
        private var expandResponses: String = ""
        private var maxDisplayedEnumValues: Int? = null
        private var hideDownloadButton: Boolean = false
        private var hideHostname: Boolean = false
        private var hideLoading: Boolean = false
        private var hideSingleRequestSampleTab: Boolean = false
        private var expandSingleSchemaField: Boolean = false
        private var jsonSampleExpandLevel: String = "2"
        private var hideSchemaTitles: Boolean = false
        private var simpleOneOfTypeLabel: Boolean = false
        private var menuToggle: Boolean = false
        private var nativeScrollbars: Boolean = false
        private var noAutoAuth: Boolean = false
        private var onlyRequiredInSamples: Boolean = false
        private var pathInMiddlePanel: Boolean = false
        private var requiredPropsFirst: Boolean = false
        private var scrollYOffset: String = "0"
        private var showExtensions: Boolean = false
        private var sortPropsAlphabetically: Boolean = false
        private var suppressWarning: Boolean = false
        private var payloadSampleIdx: Int = 0
        private var untrustedSpec: Boolean = false
        private var theme: RedocOptionsTheme = RedocOptionsTheme()
        private var extras: Map<String, Any?> = emptyMap()
        
        fun setDisableSearch(boolean: Boolean) = apply { disableSearch = boolean }
        fun setExpandDefaultServerVariables(boolean: Boolean) = apply { expandDefaultServerVariables = boolean }
        fun setExpandResponse(string: String) = apply { expandResponses = string }
        fun setMaxDisplayedEnumValues(int: Int?) = apply { maxDisplayedEnumValues = int }
        fun setHideDownloadButton(boolean: Boolean) = apply { hideDownloadButton = boolean }
        fun setHideHostname(boolean: Boolean) = apply { hideHostname = boolean }
        fun setHideLoading(boolean: Boolean) = apply { hideLoading = boolean }
        fun setHideSingleRequestSampleTab(boolean: Boolean) = apply { hideSingleRequestSampleTab = boolean }
        fun setExpandSingleSchemaField(boolean: Boolean) = apply { expandSingleSchemaField = boolean }
        fun setJsonSampleExpandLevel(string: String) = apply { jsonSampleExpandLevel = string }
        fun setHideSchemaTitles(boolean: Boolean) = apply { hideSchemaTitles = boolean }
        fun setSimpleOneOfTypeLabel(boolean: Boolean) = apply { simpleOneOfTypeLabel = boolean }
        fun setMenuToggle(boolean: Boolean) = apply { menuToggle = boolean }
        fun setNativeScrollbars(boolean: Boolean) = apply { nativeScrollbars = boolean }
        fun setNoAutoAuth(boolean: Boolean) = apply { noAutoAuth = boolean }
        fun setOnlyRequiredInSamples(boolean: Boolean) = apply { onlyRequiredInSamples = boolean}
        fun setPathInMiddlePanel(boolean: Boolean) = apply { pathInMiddlePanel = boolean }
        fun setRequiredPropsFirst(boolean: Boolean) = apply { requiredPropsFirst = boolean }
        fun setScrollYOffset(string: String) = apply { scrollYOffset = string }
        fun setShowExtensions(boolean: Boolean) = apply { showExtensions = boolean }
        fun setSortPropsAlphabetically(boolean: Boolean) = apply { sortPropsAlphabetically = boolean }
        fun setSuppressWarning(boolean: Boolean) = apply { suppressWarning = boolean }
        fun setPayloadSampleIdx(int: Int) = apply { payloadSampleIdx = int }
        fun setUntrustedSpec(boolean: Boolean) = apply { untrustedSpec = boolean }
        fun setTheme(redocOptionsTheme: RedocOptionsTheme) = apply { theme = redocOptionsTheme }
        fun setExtras(map: Map<String, Any?>) = apply { extras = map }
        
        fun build() = RedocOptionsObject(disableSearch, expandDefaultServerVariables, expandResponses, maxDisplayedEnumValues, hideDownloadButton, hideHostname, hideLoading, hideSingleRequestSampleTab, expandSingleSchemaField, jsonSampleExpandLevel, hideSchemaTitles, simpleOneOfTypeLabel, menuToggle, nativeScrollbars, noAutoAuth, onlyRequiredInSamples, pathInMiddlePanel, requiredPropsFirst, scrollYOffset, showExtensions, sortPropsAlphabetically, suppressWarning, payloadSampleIdx, untrustedSpec, theme, extras)
    }
}