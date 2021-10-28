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
import groovy.transform.CompileStatic
import org.beryx.jar.JarModularityExtension.JarModularityData
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.util.GradleVersion

@CompileStatic
class BadassJarPlugin implements Plugin<Project> {
    private static final Logger LOGGER = PluginLogger.of(BadassJarPlugin.class)

    @Override
    void apply(Project project) {
        if(GradleVersion.current() < GradleVersion.version('6.7')) {
            throw new GradleException("This plugin requires Gradle 6.7 or newer. Update your Gradle or use version 1.2.0 of this plugin.")
        }
        Jar jarTask = (Jar)project.tasks.findByName(JavaPlugin.JAR_TASK_NAME)
        jarTask?.extensions?.create("modularity", JarModularityExtension, project)

        project.afterEvaluate {
            if(jarTask) {
                if(jarTask.name == JavaPlugin.JAR_TASK_NAME) {
                    JavaCompile compileJava = (JavaCompile)project.tasks.getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME)
                    int toolchainVersion = compileJava.javaCompiler.get().metadata.languageVersion.asInt()
                    LOGGER.debug("toolchainVersion: $toolchainVersion")
                    def data = ((JarModularityExtension)jarTask.getExtensions().getByName("modularity")).data
                    if(toolchainVersion > 8) {
                        configureJpmsToolchain(jarTask, data)
                    } else {
                        configureNonJpmsToolchain(jarTask, data)
                    }
                }
            }
        }
    }

    static void configureJpmsToolchain(Jar jarTask, JarModularityData jarModularityData) {
        def project = jarTask.project
        JavaCompile compileJava = (JavaCompile)project.tasks.getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME)

        LOGGER.info "module-info.class will be created by the compiler because compatibility >= 9"
        if(jarModularityData.multiRelease) {
            LOGGER.warn("ignoring multiRelease because compatibility >= 9")
        }
        String moduleVersion = jarModularityData.version
        if(moduleVersion) {
            def versionArgs = ['--module-version', moduleVersion]
            LOGGER.debug "using versionArgs: $versionArgs"
            compileJava.options.compilerArgs.addAll versionArgs
        }
        if(jarModularityData.moduleInfoPath) {
            File moduleInfoFile = project.file(jarModularityData.moduleInfoPath)
            if(!moduleInfoFile.file) throw new GradleException("$moduleInfoFile.absolutePath not available")
            def javaPluginExtension = project.extensions.getByType(JavaPluginExtension)
            SourceSet mainSourceSet = javaPluginExtension.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            mainSourceSet.java.srcDir(moduleInfoFile.parent)
        }
    }

    static void configureNonJpmsToolchain(Jar jarTask, JarModularityData jarModularityData) {
        LOGGER.info "module-info.class will be created using the ASM library because your toolchain version is <= 8"
        def project = jarTask.project

        def javaPluginExtension = project.extensions.getByType(JavaPluginExtension)
        javaPluginExtension.modularity.inferModulePath.set(false)
        project.tasks.withType(JavaCompile) { JavaCompile task -> task.modularity.inferModulePath.set(false) }
        project.tasks.withType(JavaExec) { JavaExec task -> task.modularity.inferModulePath.set(false) }

        SourceSet mainSourceSet = javaPluginExtension.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        mainSourceSet.java.exclude('**/module-info.java')

        jarTask.doFirst {
            File moduleInfoFile = null
            if (jarModularityData.moduleInfoPath) {
                moduleInfoFile = project.file(jarModularityData.moduleInfoPath)
                if (!moduleInfoFile.file) throw new GradleException("$moduleInfoFile.absolutePath not available")
            } else {
                def moduleInfoDir = mainSourceSet.java.srcDirs.find { new File(it, 'module-info.java').file }
                if (!moduleInfoDir) {
                    LOGGER.info "badass-jar not used because module-info.java is not present in $mainSourceSet.java.srcDirs"
                } else {
                    LOGGER.debug "module-info.java found in $moduleInfoDir"
                    moduleInfoFile = new File(moduleInfoDir, 'module-info.java')
                }
            }
            if (moduleInfoFile) {
                def targetBaseDir = mainSourceSet.java.destinationDirectory.asFile.get()
                createModuleDescriptor(jarModularityData, moduleInfoFile.text, jarTask, targetBaseDir)
            }
        }
    }

    static File createModuleDescriptor(JarModularityData jarModularityData, String moduleInfoSource, Jar jarTask, File targetBaseDir) {
        ModuleDeclaration module = ModuleInfoCompiler.parseModuleInfo(moduleInfoSource)
        String version = jarModularityData.version
        LOGGER.debug "Setting module version to $version"
        byte[] clazz = ModuleInfoCompiler.compileModuleInfo( module, null, version)
        LOGGER.debug "Module info compiled: $clazz.length bytes"
        boolean multiRelease = jarModularityData.multiRelease
        LOGGER.debug "multiRelease = $multiRelease"
        if(multiRelease) {
            jarTask.manifest.attributes('Multi-Release': true)
        }
        def targetDir = multiRelease ? new File(targetBaseDir, 'META-INF/versions/9') : targetBaseDir
        targetDir.mkdirs()
        def moduleDescriptor = new File(targetDir, 'module-info.class')
        LOGGER.info "Writing module descriptor into $moduleDescriptor"
        moduleDescriptor.withOutputStream { it.write(clazz) }
        moduleDescriptor
    }
}
