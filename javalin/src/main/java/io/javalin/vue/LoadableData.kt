package io.javalin.vue

const val loadableDataScript = """

<script nonce="@internalAddNonce">
    class LoadableData {
        constructor(url, options = {cache: false, errorCallback: null}) {
            this._url = url;
            this._errorCallback = options.errorCallback ?? null;
            this.refresh(options.cache ?? false); // initial load
            this.addRefreshListener(); // listen for global refresh events
        }
        refresh(cache = false) {
            this.data = null;
            this.loading = true;
            this.loaded = false;
            this.loadError = null;
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
                if (this._errorCallback !== null) { // should probably handle in UI
                    this._errorCallback(error);
                }
            }).finally(() => this.loading = false);
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
