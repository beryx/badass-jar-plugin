package org.beryx.jar

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

class BadassJarPluginExtension {
    final Property<String> badassJarInputProperty
    final RegularFileProperty badassJarOutputProperty

    BadassJarPluginExtension(Project project) {
        badassJarInputProperty = project.objects.property(String)
        badassJarInputProperty.set('badassJarInputProperty-default-val')
        badassJarOutputProperty = project.layout.fileProperty()
        badassJarOutputProperty.set(project.file("$project.buildDir/badassJar/badassJar-out.txt"))
    }
}
