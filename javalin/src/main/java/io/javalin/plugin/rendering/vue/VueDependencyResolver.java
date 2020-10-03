/*
 * Copyright 2020 tareq.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.javalin.plugin.rendering.vue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author tareq
 */
public class VueDependencyResolver {

    private final Map<String, String> componentIdToOwnContent; // {component-id: component-content}
    private final Map<String, String> componentIdToDependencyContent; // {component-id: required-dependencies}
    private final Pattern tagRegex = Pattern.compile("<\\s*([a-z|-]*)\\s*.*>");
    private final Pattern componentRegex = Pattern.compile("Vue.component\\(\\s*[\"|'](.*)[\"|']\\s*,.*");

    public VueDependencyResolver(final Set<Path> paths) {
        componentIdToOwnContent = new HashMap<>();
        componentIdToDependencyContent = new HashMap<>();
        paths.stream().filter(JavalinVueKt::isVueFile).forEach(path -> {
            String fileContent = JavalinVueKt.readText(path);
            Matcher matcher = componentRegex.matcher(fileContent); // check for a vue component
            while (matcher.find()) {
                componentIdToOwnContent.put(matcher.group(1), fileContent); // cache the file content, bound to the component name
            }
        });
    }

    /**
     * Build the HTML of components needed for this component
     *
     * @param componentId the component-id to build the HTMl for.
     * @return a HTML string of the components needed for this page/view if the
     * component is found, an error string otherwise.
     */
    public String resolve(final String componentId) {
        if (!componentIdToOwnContent.containsKey(componentId)) {
            throw new IllegalArgumentException(String.format("Component %s not found", componentId));
        }
        if (componentIdToDependencyContent.containsKey(componentId)) {
            return componentIdToDependencyContent.get(componentId);
        }
        Set<String> dependencies = resolveTransitiveDependencies(componentId);

        StringBuilder builder = new StringBuilder();
        dependencies.forEach(dependency -> {
            builder.append("<!-- ").append(dependency).append("-->\n");
            builder.append(componentIdToOwnContent.get(dependency));
            builder.append("\n");
        });
        String allDependencies = builder.toString();
        componentIdToDependencyContent.put(componentId, allDependencies);
        return allDependencies;
    }

    /**
     * Resolve the dependencies for a required component based on the contents
     * of its file
     *
     * @param componentId the name of the component, without tags
     * @return a Set of dependencies needed to render this component
     */
    private Set<String> resolveTransitiveDependencies(final String componentId) {
        Set<String> requiredComponents = new HashSet<>();
        requiredComponents.add(componentId);// add it to the dependency list
        Set<String> directDependencies = resolveDirectDependencies(componentId); //get its dependencies
        requiredComponents.addAll(directDependencies); //add all its dependencies  to the required components list
        directDependencies.forEach(dependency -> {
            // resolve each dependency
            requiredComponents.addAll(resolveTransitiveDependencies(dependency));
        });
        return requiredComponents;
    }

    /**
     * Resolve the direct dependencies for a component
     *
     * @param componentId the component to resolve dependencies for.
     * @return a set of dependencies.
     */
    private Set<String> resolveDirectDependencies(final String componentId) {
        Set<String> dependencies = new HashSet<>();
        String componentContent = componentIdToOwnContent.get(componentId);
        Matcher matcher = tagRegex.matcher(componentContent); //match for HTML tags
        while (matcher.find()) {
            String match = matcher.group(1);
            if (!match.equals(componentId) && componentIdToOwnContent.containsKey(match)) { // if it isn't the component itself, and its in the component map
                dependencies.add(match); //add it to the list of dependencies
            }
        }
        return dependencies;
    }

}
