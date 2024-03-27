package io.javalin.vue

const val loadableDataScript = """

<script nonce="@internalAddNonce">
    class LoadableData {
    /**
     * Initializes the data from the backend.
     * @param url - The url to fetch the data from.
     * @param options - Options for the data. Currently only supports cache and errorCallback.
     */
    constructor(url, options = { cache: false, errorCallback: null }) {
        this._url = url;
        this._errorCallback = options.errorCallback ?? null;
        this.data = null;
        this.loading = false;
        this.refreshing = false;
        this.loaded = false;
        this.loadError = null;
        this.addRefreshListener();
    }

    /**
     * Initializes the data from the backend.
     * @param cache - If true, the data will be cached in local storage.
     * @returns {Promise<void>} - Resolves when the data is loaded.
     */
    async init(cache = false) {
        this.loading = true;
        try {
            let cacheKey = "LoadableData:" + this._url;
            if (cache) {
                this.data = JSON.parse(localStorage.getItem(cacheKey)) || null;
                this.loaded = this.data !== null;
                this.loading = this.loaded === false;
            }
            const response = await fetch(this._url);
            if (!response.ok) {
                throw { code: response.status, text: response.statusText };
            }
            const data = await response.json();
            this.data = data;
            this.loaded = true;
            if (cache) {
                localStorage.setItem(cacheKey, JSON.stringify(data));
            }
        } catch (error) {
            this.loadError = error;
            if (this._errorCallback !== null) {
                this._errorCallback(error);
            }
        } finally {
            this.loading = false;
            this.refreshing = false;
        }
    }

    /**
     * Refreshes the data from the backend.
     * If the data is cached, it will be used until the new data is loaded.
     * @param {boolean} boolean - If true, all instances of this data will be refreshed.
     */
    refresh(boolean = false) {
        this.refreshing = true;
        this.init(false); // refresh doesn't clear data, so no need to cache
        if(boolean) {
            this.refreshAll();
        }
    }

    /**
     * Refreshes all instances of this data.
     */
    refreshAll() {
        window.dispatchEvent(new CustomEvent("loadabledata-refresh", { detail: this._url }));
    }

    /**
     * Invalidates the cache for this data.
     */
    invalidateCache() {
        localStorage.removeItem("LoadableData:" + this._url);
    }

    addRefreshListener() {
        window.addEventListener("loadabledata-refresh", (e) => {
            if (this._url === e.detail) {
                this.refresh(false);
            }
        }, false);
    }
}
</script>"""
