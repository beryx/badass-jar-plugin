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

import com.github.javaparser.ast.modules.ModuleDeclaration
import groovy.transform.CompileStatic
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar

class BadassJarPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.tasks.withType(Jar) { Jar jarTask ->
            if(jarTask.name == 'jar') {
                jarTask.metaClass.multiRelease = true
            }
        }

        project.afterEvaluate {
            project.tasks.withType(Jar) { Jar jarTask ->
                if(jarTask.name == 'jar') {
                    project.logger.debug "sourceCompatibility: ${project.sourceCompatibility}; targetCompatibility: ${project.targetCompatibility}"
                    if(project.sourceCompatibility <= JavaVersion.VERSION_1_8 && project.targetCompatibility <= JavaVersion.VERSION_1_8) {
                        project.sourceSets.main.java.exclude '**/module-info.java'
                        jarTask.doFirst {
                            def moduleInfoDir = project.sourceSets.main.java.srcDirs.find { new File(it, 'module-info.java').file }
                            if(!moduleInfoDir) {
                                project.logger.info "badass-jar not used because module-info.java is not present in $project.sourceSets.main.java.srcDirs"
                            } else {
                                project.logger.debug "module-info.java found in $moduleInfoDir"
                                project.logger.debug "multiRelease: $jarTask.multiRelease"
                                def moduleInfoSource = new File(moduleInfoDir, 'module-info.java').text
                                createModuleDescriptor(project, moduleInfoSource, jarTask.multiRelease, project.sourceSets.main.java.outputDir)
                            }
                        }
                    } else {
                        project.logger.info "badass-jar not used because compatibility >= 9"
                    }
                }
            }
        }
    }

    @CompileStatic
    File createModuleDescriptor(Project project, String moduleInfoSource, boolean multiRelease, File targetBaseDir) {
        def comp = new ModuleInfoCompiler()
        ModuleDeclaration module = ModuleInfoCompiler.parseModuleInfo(moduleInfoSource);
        byte[] clazz = ModuleInfoCompiler.compileModuleInfo( module, null, null);
        project.logger.debug "Module info compiled: $clazz.length bytes"
        def targetDir = multiRelease ? new File(targetBaseDir, 'META-INF/versions/9') : targetBaseDir
        targetDir.mkdirs()
        def moduleDescriptor = new File(targetDir, 'module-info.class')
        project.logger.info "Writing module descriptor into $moduleDescriptor"
        moduleDescriptor.withOutputStream { it.write(clazz) }
        moduleDescriptor
    }
}
