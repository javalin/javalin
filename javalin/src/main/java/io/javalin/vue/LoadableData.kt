package io.javalin.vue

const val loadableDataScript = """

<script nonce="@internalAddNonce">
    class LoadableData {
        constructor(url, options = {cache: false, errorCallback: null}) {
            this.isVue2 = Vue.version.startsWith("2");
            this._url = url;
            this._errorCallback = options.errorCallback ?? null;
            if (this.isVue2) {
              this.data = null;
              this.loading = true;
              this.refreshing = false;
              this.loaded = false;
              this.loadError = null;
            } else {
              this.data = Vue.ref(null);
              this.loading = Vue.ref(true);
              this.refreshing = Vue.ref(false);
              this.loaded = Vue.ref(false);
              this.loadError = Vue.ref(null);
            }
        
            this._load(options.cache ?? false); // initial load
            this.addRefreshListener(); // listen for global refresh events
          }
          _load(cache = false) {
            let cacheKey = "LoadableData:" + this._url;
            if (cache) {
              this.data = JSON.parse(localStorage.getItem(cacheKey)) || null;
              if (this.isVue2) {
                this.loaded = this.data !== null;
                this.loading = this.loaded === false;
              } else {
                this.loaded.value = this.data.value !== null;
                this.loading.value = !this.loaded.value;
              }
            }
            fetch(this._url).then(res => {
                if (res.ok) return res.json();
                throw { code: res.status, text: res.statusText };
              }).then(data => {
                if (this.isVue2) {
                  this.data = data;
                  this.loaded = true;
                } else {
                  this.data.value = data;
                  this.loaded.value = true;
                }
                if (cache) {
                  localStorage.setItem(cacheKey, JSON.stringify(data));
                }
              }).catch(error => {
                if (this.isVue2) {
                  this.loadError = error;
                } else {
                  this.loadError.value = error;
                }
                if (this._errorCallback !== null) {
                  this._errorCallback(error);
                }
              }).finally(() => {
                if (this.isVue2) {
                  this.loading = false;
                  this.refreshing = false;
                } else {
                  this.loading.value = false;
                  this.refreshing.value = false;
                }
              });
          }
          refresh() {
            if (this.isVue2) {
              this.refreshing = true;
            } else {
              this.refreshing.value = true;
            }
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
