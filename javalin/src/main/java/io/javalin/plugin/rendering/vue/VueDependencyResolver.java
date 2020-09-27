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
    private final Pattern tagRegex = Pattern.compile("<\\s*([a-z|-]*)\\s*.*>");
    private final Pattern componentRegex = Pattern.compile(".*Vue.component\\(\\s*[\"|'](.*)[\"|']\\s*,.*");

    public VueDependencyResolver(Set<Path> paths) {
        componentsMap = new HashMap<>();
        buildMap(paths);
        layoutCache = new HashMap<>();
    }

    /**
     * Builds a map of components and their file contents. This is done so that
     * component dependency resolution is fast
     *
     * @param paths the file paths to check for components
     */
    private void buildMap(Set<Path> paths) {
        paths.stream().filter(it -> it.toString().endsWith(".vue")) // only check vue files
                .forEach(path -> {
                    try {
                        String text = Files.readString(path); // read the entire file to memory
                        Matcher res = componentRegex.matcher(text); // check for a vue component
                        if (res.find()) {
                            String component = res.group(1);
                            if (component != null) {
                                componentsMap.put(component, text); // load the entire file, bound to the component name to memory
                            }
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(VueDependencyResolver.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
    }

    /**
     * Resolve the dependencies for a required component based on the contents
     * of its file
     *
     * @param component the name of the component
     * @param requiredComponents the set of required components to be
     * recursively pushed into
     */
    private void resolve(String component, Set<String> requiredComponents) {
        String strippedComponent = "";
        // Get the name of the component without tags if tags were passed in
        if (component.startsWith("<")) {
            Matcher res = tagRegex.matcher(component);
            if (res.find()) {
                strippedComponent = res.group(1);
            }
        } else {
            strippedComponent = component;
        }
        if (!requiredComponents.contains(component)) {// if it has not already been resolved
            requiredComponents.add(strippedComponent);// add it to the dependency list
            Set<String> dependencies = getDependencies(strippedComponent); //get its dependencies
            requiredComponents.addAll(dependencies); //add all its dependencies  to the required components list
            for (String dependency : dependencies) { // resolve each dependency
                resolve(dependency, requiredComponents);
            }
        }

    }

    /**
     * Build the HTML of components needed for this component
     *
     * @param component the component to build the HTMl for
     * @param isLocalhost if we are running in localhost
     * @return a partial HTML string of the components needed for this page/view
     * if the component is found, an error string otherwise.
     */
    public String buildHtml(String component, boolean isLocalhost) {
        if (!isLocalhost && layoutCache.containsKey(component)) {
            return layoutCache.get(component);
        }
        Set<String> components = new HashSet<>();
        if (!componentsMap.containsKey(component)) {
            return "Component Not Found.";
        }
        resolve(component, components);
        StringBuilder builder = new StringBuilder();
        components.stream()
                .forEach(requiredComponent -> {
                    builder.append("<!-- ").append(requiredComponent).append("-->\n");
                    builder.append(componentsMap.get(requiredComponent));
                    builder.append("\n");
                });
        String layout = builder.toString();
        layoutCache.put(component, layout);
        return layout;
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
