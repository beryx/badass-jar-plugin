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
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar

class BadassJarPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        applyModuleSystemPlugin(project)
        project.tasks.withType(Jar) { Jar jarTask ->
            if(jarTask.name == 'jar') {
                jarTask.metaClass.multiRelease = true
            }
        }

        project.afterEvaluate {
            project.tasks.withType(Jar) { Jar jarTask ->
                if(jarTask.name == 'jar') {
                    Util.adjustCompatibility(project)
                    if(project.sourceCompatibility <= JavaVersion.VERSION_1_8 && project.targetCompatibility <= JavaVersion.VERSION_1_8) {
                        project.sourceSets.main.java.exclude '**/module-info.java'
                        jarTask.doFirst {
                            def moduleInfoDir = project.sourceSets.main.java.srcDirs.find { new File(it, 'module-info.java').file }
                            if(!moduleInfoDir) {
                                project.logger.info "badass-jar not used because module-info.java is not present in $project.sourceSets.main.java.srcDirs"
                            } else {
                                project.logger.debug "module-info.java found in $moduleInfoDir"
                                String moduleInfoSource = new File(moduleInfoDir, 'module-info.java').text
                                createModuleDescriptor(project, moduleInfoSource, jarTask, project.sourceSets.main.java.outputDir)
                            }
                        }
                    } else {
                        project.logger.info "badass-jar not used because compatibility >= 9"
                    }
                }
            }
        }
    }

    File createModuleDescriptor(Project project, String moduleInfoSource, Jar jarTask, File targetBaseDir) {
        ModuleDeclaration module = ModuleInfoCompiler.parseModuleInfo(moduleInfoSource);
        byte[] clazz = ModuleInfoCompiler.compileModuleInfo( module, null, null);
        project.logger.debug "Module info compiled: $clazz.length bytes"
        project.logger.debug "multiRelease: $jarTask.multiRelease"
        if(jarTask.multiRelease) {
            jarTask.manifest {
                attributes('Multi-Release': true)
            }
        }
        def targetDir = jarTask.multiRelease ? new File(targetBaseDir, 'META-INF/versions/9') : targetBaseDir
        targetDir.mkdirs()
        def moduleDescriptor = new File(targetDir, 'module-info.class')
        project.logger.info "Writing module descriptor into $moduleDescriptor"
        moduleDescriptor.withOutputStream { it.write(clazz) }
        moduleDescriptor
    }

    private applyModuleSystemPlugin(Project project) {
        project.logger.info("Applying BadassModuleSystemPlugin to " + project.getName());
        project.afterEvaluate {
            Util.adjustCompatibility(project)
            def moduleInfoDir = project.sourceSets.main.java.srcDirs.find { new File(it, 'module-info.java').file }
            if(moduleInfoDir && (project.sourceCompatibility >= JavaVersion.VERSION_1_9 || project.targetCompatibility >= JavaVersion.VERSION_1_9)) {
                if(javaVersion < 11) {
                    throw new GradleException("You need Java 11 or newer to execute the plugin on the module-path.")
                }
                Class msPluginClass = Class.forName('org.javamodularity.moduleplugin.ModuleSystemPlugin')
                project.getPluginManager().apply(msPluginClass);
                project.logger.info("ModuleSystemPlugin enabled: moduleInfoDir = $moduleInfoDir, sourceCompatibility = $project.sourceCompatibility targetCompatibility = $project.targetCompatibility")
            } else [
                    project.logger.info('ModuleSystemPlugin disabled.')
            ]
        }

    }

    static int getJavaVersion() {
        double javaVersion = (System.properties['java.specification.version'] ?: 1.0) as double
        if(1.2 <= javaVersion && javaVersion < 2) {
            return Math.round(10 * javaVersion) % 10
        } else {
            return (int)javaVersion
        }
    }
}
