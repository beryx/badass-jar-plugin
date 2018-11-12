/*
 * Copyright 2018 the original author or authors.
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
package org.beryx.jar

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project

class Util {
    static void adjustCompatibility(Project project) {
        if(project.hasProperty('javaCompatibility')) {
            def javaVersion = JavaVersion.toVersion(project.javaCompatibility)
            if(!javaVersion) throw new GradleException("Invalid value for javaCompatibility: $project.javaCompatibility")
            project.sourceCompatibility = javaVersion
            project.targetCompatibility = javaVersion
            project.logger.info "javaCompatibility: $javaVersion"
        } else {
            project.logger.debug "javaCompatibility not set"
        }
        project.logger.info "sourceCompatibility: ${project.sourceCompatibility}; targetCompatibility: ${project.targetCompatibility}"
    }
}
