package org.beryx.jar

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.beryx.jar.impl.BadassJarTaskImpl

class BadassJarTask extends DefaultTask {
    @Input @Optional
    Property<String> badassJarInputProperty

    @OutputFile @Optional
    RegularFileProperty badassJarOutputProperty

    @TaskAction
    void badassJarTaskAction() {
        def input = badassJarInputProperty.get()
        def output = badassJarOutputProperty.get().asFile
        new BadassJarTaskImpl(input, output).execute()
    }
}
