package io.javalin.vue

const val loadableDataScript = """

<script nonce="@internalAddNonce">
    class LoadableData {
        constructor(url, options = {cache: false, errorCallback: null}) {
            this._url = url;
            this._errorCallback = options.errorCallback ?? null;
            this.data = null;
            this.loading = true;
            this.refreshing = false;
            this.loaded = false;
            this.loadError = null;
            this._load(options.cache ?? false); // initial load
            this.addRefreshListener(); // listen for global refresh events
        }
        _load(cache = false) {
            let cacheKey = "LoadableData:" + this._url;
            if (cache) {
                this.data = JSON.parse(localStorage.getItem(cacheKey)) || null;
                this.loaded = this.data !== null;
                this.loading = this.loaded === false;
            }
            fetch(this._url).then(res => {
                if (res.ok) return res.json();
                throw { code: res.status, text: res.statusText };
            }).then(data => {
                this.data = data;
                this.loaded = true;
                if (cache) {
                    localStorage.setItem(cacheKey, JSON.stringify(data));
                }
            }).catch(error => {
                this.loadError = error;
                if (this._errorCallback !== null) {
                    this._errorCallback(error);
                }
            }).finally(() => {
                this.loading = false;
                this.refreshing = false;
            });
        }
        refresh() {
            this.refreshing = true;
            this._load(false); // refresh doesn't clear data, so no need to cache
        }
        refreshAll() {
            window.dispatchEvent(new CustomEvent("loadabledata-refresh", {detail: this._url}));
        }
        invalidateCache() {
            localStorage.removeItem("LoadableData:" + this._url);
        }
        addRefreshListener() {
            window.addEventListener("loadabledata-refresh", e => {
                if (this._url === e.detail) {
                    this.refresh();
                }
            }, false);
        }
    }
</script>"""
