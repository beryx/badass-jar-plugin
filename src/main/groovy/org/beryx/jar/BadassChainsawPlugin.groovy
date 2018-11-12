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

import com.zyxist.chainsaw.ChainsawPlugin
import com.zyxist.chainsaw.JavaModule
import com.zyxist.chainsaw.algorithms.ModuleNameDetector
import com.zyxist.chainsaw.tasks.VerifyModuleNameTask
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginConvention

class BadassChainsawPlugin extends ChainsawPlugin {
    @Override
    void apply(Project project) {
        project.logger.info("Applying BadassChainsawPlugin to " + project.getName());
        Configuration cfg = project.getConfigurations().create(PATCH_CONFIGURATION_NAME);
        cfg.setDescription("Dependencies that break Java Module System rules and need to be added as patches to other modules.");
        project.getExtensions().create('javaModule', JavaModule.class);
        project.afterEvaluate {
            Util.adjustCompatibility(project)
            def moduleInfoDir = project.sourceSets.main.java.srcDirs.find { new File(it, 'module-info.java').file }
            if(moduleInfoDir && (project.sourceCompatibility >= JavaVersion.VERSION_1_9 || project.targetCompatibility >= JavaVersion.VERSION_1_9)) {
                project.logger.info("ChainsawPlugin enabled: moduleInfoDir = $moduleInfoDir, sourceCompatibility = $project.sourceCompatibility targetCompatibility = $project.targetCompatibility")
                JavaPluginConvention javaConfig = project.getConvention().getPlugin(JavaPluginConvention.class);
                ModuleNameDetector detector = new ModuleNameDetector(javaConfig.getSourceCompatibility());

                VerifyModuleNameTask vmnTask = project.getTasks().create('verifyModuleName', VerifyModuleNameTask.class);
                vmnTask.dependsOn("compileJava");

                ChainsawPlugin.metaClass.getMetaMethod('removePatchedDependencies',project, cfg).invoke(this,project, cfg)
                ChainsawPlugin.metaClass.getMetaMethod('configureJavaTasks', project, detector).invoke(this, project, detector)
            } else [
                project.logger.info('ChainsawPlugin disabled.')
            ]
        }
    }
}
