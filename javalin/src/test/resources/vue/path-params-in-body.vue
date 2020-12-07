<template id="path-params-in-body-component">
    <div id="test">hello</div>
</template>
<script>
Vue.component("path-params-in-body-component", {template: "#path-params-in-body-component"});
document.getElementById("test").innerText = Vue.prototype.$javalin.pathParams["zeisty"]
console.log(Vue.prototype.$javalin.pathParams["zeisty"])
</script>
