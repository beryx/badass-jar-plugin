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
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.beryx.jar.ModuleConfigExtension.ModuleData
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar

@CompileStatic
class JarTaskConfigurer {
    private final Logger LOGGER = PluginLogger.of(JarTaskConfigurer)

    public static final String COMPILE_NON_JPMS_TASK_NAME = "compileNonJpms"

    final Project project
    final Jar jarTask
    final JavaCompile compileJava
    final ModuleConfigExtension moduleConfigExtension
    final ModuleData moduleData
    final JavaPluginExtension javaPluginExtension
    final SourceSet mainSourceSet
    final File moduleInfoJava
    final File moduleInfoTargetDir

    JarTaskConfigurer(Jar jarTask, ModuleConfigExtension moduleConfigExtension) {
        this.project = jarTask.project
        this.jarTask = jarTask
        this.compileJava = (JavaCompile)project.tasks.getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME)
        this.moduleConfigExtension = moduleConfigExtension
        this.moduleData = moduleConfigExtension.data
        this.javaPluginExtension = project.extensions.getByType(JavaPluginExtension)
        this.mainSourceSet = javaPluginExtension.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
        def targetBaseDir = mainSourceSet.java.destinationDirectory.asFile.get()
        this.moduleInfoTargetDir = moduleData.multiRelease
                ? new File(targetBaseDir, "META-INF/versions/$moduleData.moduleInfoCompatibility")
                : targetBaseDir
        this.moduleInfoJava = findModuleInfoJava()
    }

    void configure() {
        if(moduleInfoJava) {
            int toolchainVersion = compilerVersion.majorVersion as int
            LOGGER.debug("toolchainVersion: $toolchainVersion")
            if(toolchainVersion > 8) {
                configureJpmsToolchain()
            } else {
                configureNonJpmsToolchain()
            }
        }
    }

    @CompileDynamic
    JavaVersion getCompilerVersion() {
        compileJava.javaVersion
    }

    void configureJpmsToolchain() {
        LOGGER.info "module-info.class will be created by the Java compiler"
        def moduleInfoFile = moduleInfoJava
        LOGGER.debug("moduleInfoFile: $moduleInfoFile")

        if(moduleData.version) {
            def versionArgs = ['--module-version', moduleData.version]
            compileJava.options.compilerArgs.addAll(versionArgs)
        }

        if(moduleData.moduleInfoPath) {
            mainSourceSet.java.srcDirs(project.file(moduleInfoFile.parent))
        }

        def sourceCompatibilityJavaVersion = JavaVersion.toVersion(compileJava.sourceCompatibility)
        LOGGER.debug("sourceCompatibilityJavaVersion: $sourceCompatibilityJavaVersion")
        if(sourceCompatibilityJavaVersion <= JavaVersion.VERSION_1_8) {
            JavaCompile compileNonJpms = project.tasks.maybeCreate(COMPILE_NON_JPMS_TASK_NAME, JavaCompile)
            compileNonJpms.options.release.set(sourceCompatibilityJavaVersion.majorVersion as int)
            compileNonJpms.exclude("**/module-info.java")
            compileNonJpms.modularity.inferModulePath.set(false)
            compileNonJpms.setClasspath(project.files(compileJava.classpath))
            compileNonJpms.setSource(project.files(compileJava.source.asFileTree.files))
            compileNonJpms.getDestinationDirectory().set(compileJava.destinationDirectory.getAsFile().get())

            compileJava.include("**/module-info.java")
            compileJava.getDestinationDirectory().set(moduleInfoTargetDir)
            compileJava.options.release.set(moduleData.moduleInfoCompatibility)

            LOGGER.debug("mainSourceSet: $mainSourceSet.java.srcDirs")
            LOGGER.debug("compileJava release: ${compileJava.options.release.getOrNull()}")
            LOGGER.debug("compileJava source: $compileJava.source.asFileTree.files")
            LOGGER.debug("compileJava sourcepath: ${compileJava.options?.sourcepath?.files}")
            LOGGER.debug("compileJava classpath: $compileJava.classpath.files")

            LOGGER.debug("compileNonJpms release: ${compileNonJpms.options.release.getOrNull()}")
            LOGGER.debug("compileNonJpms source: $compileNonJpms.source.asFileTree.files")
            LOGGER.debug("compileNonJpms sourcepath: ${compileNonJpms.options?.sourcepath?.files}")
            LOGGER.debug("compileNonJpms classpath: ${compileNonJpms.classpath?.files}")

            compileNonJpms.dependsOn(compileJava)
            def classesTask = project.tasks.getByName(JavaPlugin.CLASSES_TASK_NAME)
            classesTask.dependsOn(compileNonJpms)
        } else {
            if(moduleConfigExtension.multiRelease.isPresent() && moduleData.multiRelease) {
                LOGGER.info("multiRelease ignored because classes are compiled using Java $sourceCompatibilityJavaVersion")
            }
            if(moduleConfigExtension.moduleInfoCompatibility.isPresent() && JavaVersion.toVersion(moduleData.moduleInfoCompatibility) != sourceCompatibilityJavaVersion) {
                LOGGER.info("moduleInfoCompatibility ignored because classes are compiled using Java $sourceCompatibilityJavaVersion")
            }
        }
    }

    private File findModuleInfoJava() {
        File moduleInfoFile = null
        if (moduleData.moduleInfoPath) {
            moduleInfoFile = project.file(moduleData.moduleInfoPath)
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
        return moduleInfoFile
    }

    void configureNonJpmsToolchain() {
        LOGGER.info "module-info.class will be created using the ASM library"

        javaPluginExtension.modularity.inferModulePath.set(false)
        project.tasks.withType(JavaCompile) { JavaCompile task -> task.modularity.inferModulePath.set(false) }
        project.tasks.withType(JavaExec) { JavaExec task -> task.modularity.inferModulePath.set(false) }

        mainSourceSet.java.exclude('**/module-info.java')

        compileJava.doFirst {
            moduleInfoTargetDir.mkdirs()
            if(!moduleInfoTargetDir.directory) throw new GradleException("Cannot create directory $moduleInfoTargetDir")
            LOGGER.debug("Directory $moduleInfoTargetDir created.")
        }

        jarTask.doFirst {
            createModuleDescriptor(moduleInfoJava.text)
        }
    }

    File createModuleDescriptor(String moduleInfoSource) {
        ModuleDeclaration module = ModuleInfoCompiler.parseModuleInfo(moduleInfoSource)
        LOGGER.debug "Setting module version to $moduleData.version"
        byte[] clazz = ModuleInfoCompiler.compileModuleInfo( module, null, moduleData.version)
        LOGGER.debug "Module info compiled: $clazz.length bytes"
        LOGGER.debug "multiRelease = $moduleData.multiRelease"
        if(moduleData.multiRelease) {
            jarTask.manifest.attributes('Multi-Release': true)
        }
        def moduleDescriptor = new File(moduleInfoTargetDir, 'module-info.class')
        LOGGER.info "Writing module descriptor into $moduleDescriptor"
        moduleDescriptor.withOutputStream { it.write(clazz) }
        LOGGER.debug("Written ${moduleDescriptor.length()} bytes into $moduleDescriptor")
        moduleDescriptor
    }
}
