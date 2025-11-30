package io.javalin.config

import jakarta.servlet.MultipartConfigElement

/**
 * This class contains the configuration for handling multipart file uploads
 *
 * @property cacheDirectory : the directory where files which exceed the maximum in memory size should be cached
 * @property maxFileSize : the maximum size allowed (in bytes) for an individual uploaded file
 * @property maxTotalRequestSize : the maximum size allowed (in bytes) for the entire multipart request
 * @property maxInMemoryFileSize : the maximum size allowed (in bytes) before uploads are cached to disk
 */
class MultipartConfig {
    private var cacheDirectory = System.getProperty("java.io.tmpdir")
    private var maxFileSize: Long = -1
    private var maxTotalRequestSize: Long = -1
    private var maxInMemoryFileSize: Int = 1

    /**
     * Sets the location of the cache directory used to write file uploads
     *
     * @param path : the path of the cache directory used to write file uploads > maxInMemoryFileSize
     */
    fun cacheDirectory(path: String) {
        this.cacheDirectory = path
    }

    /**
     * Sets the maximum file size for an individual file upload
     *
     * @param size : the maximum size of the file
     * @param sizeUnit : the units that this size is measured in
     */
    fun maxFileSize(size: Long, sizeUnit: SizeUnit) {
        this.maxFileSize = size * sizeUnit.multiplier
    }

    /**
     * Sets the maximum size for a single file before it will be cached to disk rather than read in memory
     *
     * @param size : the maximum size of the file
     * @param sizeUnit : the units that this size is measured in
     */
    fun maxInMemoryFileSize(size: Int, sizeUnit: SizeUnit) {
        this.maxInMemoryFileSize = size * sizeUnit.multiplier
    }

    /**
     * Sets the maximum size for the entire multipart request
     *
     * @param size : the maximum size of the file
     * @param sizeUnit : the units that this size is measured in
     */
    fun maxTotalRequestSize(size: Long, sizeUnit: SizeUnit) {
        this.maxTotalRequestSize = size * sizeUnit.multiplier
    }

    /**
     * builds a multipart configuration element from the current file upload settings
     */
    internal fun multipartConfigElement(): MultipartConfigElement {
        return MultipartConfigElement(cacheDirectory, maxFileSize, maxTotalRequestSize, maxInMemoryFileSize)
    }
}

/**
 * This class represents the potential file size descriptors to avoid the use of hard-coded multipliers
 */
enum class SizeUnit(val multiplier: Int) {
    BYTES(1),
    KB(1024),
    MB(1024 * 1024),
    GB(1024 * 1024 * 1024)
}
