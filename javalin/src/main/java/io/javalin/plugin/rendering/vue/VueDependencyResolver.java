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

import static com.sun.tools.javac.util.Convert.shortName;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import kotlin.sequences.Sequence;
import kotlin.text.MatchResult;
import kotlin.text.Regex;
import static org.graalvm.compiler.hotspot.nodes.GetObjectAddressNode.get;

/**
 *
 * @author tareq
 */
public class VueDependencyResolver {

    private final Map<String, String> componentsMap;

    public VueDependencyResolver(Set<Path> paths) {
        componentsMap = buildMap(paths);
    }

    private Map<String, String> buildMap(Set<Path> paths) {
        Map<String, String> componentsMap = new HashMap<>();
        Regex regex = new Regex("Vue.component\\(\\s*[\"|'](.*)[\"|']\\s*,.*\\)");
        paths.stream().filter(it -> it.toString().endsWith(".vue"))
                .forEach(path -> {

                    try {
                        String text = Files.readString(path);
                        if (regex.matches(text)) {
                            MatchResult res = regex.find(text, 0);
                            String component = res.getGroupValues().get(1);
                            if (component != null) {
                                componentsMap.put(component, text);
                            }
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(VueDependencyResolver.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
        return componentsMap;
    }

    public void resolve(String component, Set<String> requiredComponents) {
        requiredComponents.add(component);
        Set<String> dependencies = getDependencies(component);
        requiredComponents.addAll(dependencies);
        for (String dependency : dependencies) {
            resolve(dependency, requiredComponents);
        }

    }

    public String buildHtml(String component) {
        Set<String> components = new HashSet<>();
        resolve(component, components);
        StringBuilder builder = new StringBuilder();
        components.stream()
                .forEach(requiredComponent -> builder.append(componentsMap.get(requiredComponent)));

        return builder.toString();
    }

    Set<String> getDependencies(String component) {
        Set<String> dependencies = new HashSet<>();
        Regex regex = new Regex("<\\s*(.*)\\s*></.*>");
        Sequence<MatchResult> res = regex.findAll(componentsMap.get(component), 0);
        res.iterator().forEachRemaining(matchResult -> {
            String found = matchResult.getGroupValues().get(1);
            if (found != null && !found.equals(component)) {
                dependencies.add(found);
            }
        });
        return dependencies;
    }

}
