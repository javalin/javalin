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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 *
 * @author tareq
 */
public class VueDependencyResolver {

    private final Map<String, String> componentsMap;
    private final Map<String, String> layoutCache;
    private String completeLayout;
    private final Pattern tagRegex = Pattern.compile("<\\s*([a-z|-]*)\\s*.*>");
    private final Pattern componentRegex = Pattern.compile("Vue.component\\(\\s*[\"|'](.*)[\"|']\\s*,.*");

    public VueDependencyResolver(Set<Path> paths) {
        componentsMap = new HashMap<>();
        buildMap(paths);
        layoutCache = new HashMap<>();
    }

    /**
     * Builds a map of components and their file contents, in addition to the
     * completeLayout. This is done so that component dependency resolution is
     * fast
     *
     * @param paths the file paths to check for components
     */
    private void buildMap(Set<Path> paths) {
        if (paths == null) {
            throw new IllegalArgumentException("Paths Passed in Are null");
        }
        StringBuilder builder = new StringBuilder();
        paths.stream().filter(it -> it.toString().endsWith(".vue")) // only check vue files
                .forEach(path -> {
                    try {
                        String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8); // read the entire file to memory
                        builder.append("\n<!-- ").append(path.getFileName()).append(" -->\n");
                        builder.append(text);
                        builder.append("\n");
                        Matcher res = componentRegex.matcher(text); // check for a vue component
                        while (res.find()) {
                            String component = res.group(1);
                            if (component != null) {
                                componentsMap.put(component, text); // load the entire file, bound to the component name to memory
                            }
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(VueDependencyResolver.class.getName()).log(Level.SEVERE, null, ex);
                        throw new RuntimeException(ex);
                    }
                });
        completeLayout = builder.toString();
    }

    /**
     * Resolve the dependencies for a required component based on the contents
     * of its file
     *
     * @param component the name of the component, without tags
     * @param requiredComponents the set of required components to be
     * recursively pushed into
     */
    private void resolve(String component, Set<String> requiredComponents) {
        requiredComponents.add(component);// add it to the dependency list
        Set<String> dependencies = getDependencies(component); //get its dependencies
        requiredComponents.addAll(dependencies); //add all its dependencies  to the required components list
        for (String dependency : dependencies) {
            // resolve each dependency
            resolve(dependency, requiredComponents);
        }

    }

    /**
     * Build the HTML of components needed for this component
     *
     * @param component the component to build the HTMl for .Should not have
     * tags, only the name
     * @param resolveDependencies whether to resolve dependencies or not
     * @return a partial HTML string of the components needed for this page/view
     * if the component is found, an error string otherwise.
     */
    public String buildHtml(String component, boolean resolveDependencies) {
        if (resolveDependencies) {
            if (component == null || component.length() < 1) {
                throw new IllegalArgumentException("Component Passed is null");
            }
            if (component.startsWith("<")) {
                throw new IllegalArgumentException(String.format("Component %s should be passed without brackets(name only)", component));
            }
            if (!componentsMap.containsKey(component)) {
                throw new IllegalArgumentException(String.format("Component %s not found", component));
            }
            if (layoutCache.containsKey(component)) {
                return layoutCache.get(component);
            }
            Set<String> components = new HashSet<>();
            resolve(component, components);
            StringBuilder builder = new StringBuilder();
            for (String requiredComponent : components) {
                builder.append("<!-- ").append(requiredComponent).append("-->\n");
                builder.append(componentsMap.get(requiredComponent));
                builder.append("\n");
            }
            String layout = builder.toString();
            layoutCache.put(component, layout);
            return layout;
        } else {
            return completeLayout;
        }
    }

    /**
     * Resolve the direct dependencies for a component
     *
     * @param component the component to resolve dependencies for. Should not
     * have tags, only the name
     * @return a set of dependencies.
     */
    private Set<String> getDependencies(String component) {
        Set<String> dependencies = new HashSet<>();
        Matcher res = tagRegex.matcher(componentsMap.get(component));//match for HTML tags
        while (res.find()) {
            String found = res.group(1);
            if (found != null && !found.equals(component) && componentsMap.containsKey(found)) { // if it isnt the component and its in the component graph
                dependencies.add(found);//add it to the list of dependencies
            }
        }
        return dependencies;
    }

}
