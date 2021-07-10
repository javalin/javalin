<template id="view-multiline-dependency">
    <dependency-1
    ></dependency-1>

    <dependency-1-foo
        :foo="baz"
        prop
    />

    <dependency-one
        :foo="baz"
        prop>
        <span>foo</span>
    </dependency-one>
</template>
<script>
    Vue.component("view-multiline-dependency",{template:"#view-multiline-dependency"})
</script>
