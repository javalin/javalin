package io.javalin.vue

const val loadableDataScript = """

<script nonce="@internalAddNonce">
    class LoadableData {
        constructor(url, cache = true, errorCallback = null) {
            this._url = url;
            this._errorCallback = errorCallback;
            this.refresh(cache);
            this.addRefreshListener();
        }
        refresh(cache = true) {
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
                throw JSON.stringify({code: res.status, text: res.statusText});
            }).then(data => {
                this.data = data;
                this.loaded = true;
                if (cache) {
                    localStorage.setItem(cacheKey, JSON.stringify(data));
                }
            }).catch(error => {
                this.loadError = JSON.parse(error);
                if (this._errorCallback !== null) { // should probably handle in UI
                    this._errorCallback(error);
                }
            }).finally(() => this.loading = false);
        }
        refreshAll() {
            LoadableData.refreshAll(this._url);
        }
        static refreshAll(url) {
            window.dispatchEvent(new CustomEvent("javalinvue-loadable-data-update", {detail: url}));
        }
        addRefreshListener() {
            window.addEventListener("javalinvue-loadable-data-update", e => {
                if (this._url === e.detail) {
                    this.refresh(false);
                }
            }, false);
        }
    }
</script>"""
