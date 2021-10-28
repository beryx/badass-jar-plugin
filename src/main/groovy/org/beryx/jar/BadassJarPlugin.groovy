/*
 * Copyright 2021 the original author or authors.
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
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.jvm.tasks.Jar
import org.gradle.util.GradleVersion

class BadassJarPlugin implements Plugin<Project> {
    private static final Logger LOGGER = Logging.getLogger(Util.class);

    @Override
    void apply(Project project) {
        if(GradleVersion.current() < GradleVersion.version('6.4')) {
            throw new GradleException("This plugin requires Gradle 6.4 or newer. Update your Gradle or use version 1.2.0 of this plugin.")
        }
        project.tasks.withType(Jar) { Jar jarTask ->
            if(jarTask.name == 'jar') {
                jarTask.extensions.create("modularity", JarModularityExtension, project)
            }
        }

        project.afterEvaluate {
            project.tasks.withType(Jar) { Jar jarTask ->
                if(jarTask.name == 'jar') {
                    Util.adjustCompatibility(project)
                    JarModularityExtension jarModularity = jarTask.getExtensions().getByName("modularity")
                    String moduleInfoPath = jarModularity.moduleInfoPath.get()
                    if(project.sourceCompatibility <= JavaVersion.VERSION_1_8 && project.targetCompatibility <= JavaVersion.VERSION_1_8) {
                        LOGGER.info "badass-jar: module-info.class will be created by the plugin because compatibility <= 8"
                        project.java.modularity.inferModulePath = false
                        project.sourceSets.main.java.exclude '**/module-info.java'
                        jarTask.doFirst {
                            File moduleInfoFile = null
                            if(moduleInfoPath) {
                                moduleInfoFile = project.file(moduleInfoPath)
                                if(!moduleInfoFile.file) throw new GradleException("$moduleInfoFile.absolutePath not available")
                            } else {
                                def moduleInfoDir = project.sourceSets.main.java.srcDirs.find { new File(it, 'module-info.java').file }
                                if(!moduleInfoDir) {
                                    LOGGER.info "badass-jar not used because module-info.java is not present in $project.sourceSets.main.java.srcDirs"
                                } else {
                                    LOGGER.debug "badass-jar: module-info.java found in $moduleInfoDir"
                                    moduleInfoFile = new File(moduleInfoDir, 'module-info.java')
                                }
                            }
                            if(moduleInfoFile) {
                                createModuleDescriptor(project, moduleInfoFile.text, jarTask, project.sourceSets.main.java.destinationDirectory.asFile.get())
                            }
                        }
                    } else {
                        LOGGER.info "badass-jar: module-info.class will be created by the compiler because compatibility >= 9"
                        if(jarModularity.multiRelease.getOrElse(false)) {
                            LOGGER.warn("badass-jar: ignoring multiRelease because compatibility >= 9")
                        }
                        String moduleVersion = jarModularity.versionOrDefault
                        if(moduleVersion) {
                            def versionArgs = ['--module-version', moduleVersion]
                            LOGGER.debug "badass-jar: using versionArgs: $versionArgs"
                            project.tasks.compileJava.options.compilerArgs.addAll versionArgs
                        }
                        if(moduleInfoPath) {
                            File moduleInfoFile = project.file(moduleInfoPath)
                            if(!moduleInfoFile.file) throw new GradleException("$moduleInfoFile.absolutePath not available")
                            project.sourceSets.main.java.srcDir moduleInfoFile.parent
                        }
                    }
                }
            }
        }
    }

    File createModuleDescriptor(Project project, String moduleInfoSource, Jar jarTask, File targetBaseDir) {
        JarModularityExtension jarModularity = jarTask.getExtensions().getByName("modularity")
        ModuleDeclaration module = ModuleInfoCompiler.parseModuleInfo(moduleInfoSource);
        String version = jarModularity.versionOrDefault
        LOGGER.debug "badass-jar: Setting module version to $version"
        byte[] clazz = ModuleInfoCompiler.compileModuleInfo( module, null, version);
        LOGGER.debug "badass-jar: Module info compiled: $clazz.length bytes"
        boolean multiRelease = jarModularity.multiRelease.getOrElse(false)
        LOGGER.debug "badass-jar: multiRelease = $multiRelease"
        if(multiRelease) {
            jarTask.manifest {
                attributes('Multi-Release': true)
            }
        }
        def targetDir = multiRelease ? new File(targetBaseDir, 'META-INF/versions/9') : targetBaseDir
        targetDir.mkdirs()
        def moduleDescriptor = new File(targetDir, 'module-info.class')
        LOGGER.info "badass-jar: Writing module descriptor into $moduleDescriptor"
        moduleDescriptor.withOutputStream { it.write(clazz) }
        moduleDescriptor
    }
}
