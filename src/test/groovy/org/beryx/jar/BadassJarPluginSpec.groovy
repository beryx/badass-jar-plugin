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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class BadassJarPluginSpec extends Specification {
    @TempDir Path testProjectDir

    enum ModuleInfoType {
        NONE,
        STANDARD,
        EXTERNAL,
    }
    private static final ModuleInfoType NONE = ModuleInfoType.NONE
    private static final ModuleInfoType STANDARD = ModuleInfoType.STANDARD
    private static final ModuleInfoType EXTERNAL = ModuleInfoType.EXTERNAL

    private static String createBuildContent(Boolean multiRelease, ModuleInfoType moduleInfoType, String sourceCompatibility) {
        String content = """
            plugins {
                id 'java'
                id 'org.beryx.jar'
            }
            sourceCompatibility = $sourceCompatibility
        """.stripIndent()
        def multiReleaseCfg = (multiRelease == null) ? '' : "multiRelease = $multiRelease"
        def moduleInfoPathCfg = (moduleInfoType != EXTERNAL) ? '' : "moduleInfoPath = 'src/main/module/module-info.java'"
        def jarCfg = (!multiReleaseCfg && !moduleInfoPathCfg) ? '' : """
                jar {
                    $multiReleaseCfg
                    $moduleInfoPathCfg
                }""".stripIndent()
        return content + jarCfg
    }

    private static String getLocation(File jarFile, String fileName) {
        def zipFile = new ZipFile(jarFile)
        ZipEntry entry = zipFile.entries().find() { it.name == fileName || it.name.endsWith("/$fileName")}
        return entry ? entry.name - fileName : null
    }

    private static String getManifest(File jarFile) {
        def zstream = new ZipInputStream(jarFile.newInputStream())
        ZipEntry entry
        while(entry = zstream.nextEntry) {
            if(entry.name == 'META-INF/MANIFEST.MF') {
                return zstream.text
            }
        }
        assert false: "MANIFEST.MF not found in $jarFile"
    }

    def setUpBuild(Boolean multiRelease, ModuleInfoType moduleInfoType, String sourceCompatibility) {
        createJavaMain()
        createJavaModuleInfo(moduleInfoType)

        File buildFile = new File(testProjectDir.toFile(), "build.gradle")
        buildFile.text = createBuildContent(multiRelease, moduleInfoType, sourceCompatibility)
        println "Executing build script:\n${buildFile.text}"
    }

    private void createJavaMain() {
        File srcDir = new File(testProjectDir.toFile(), 'src/main/java/org/example/hello')
        srcDir.mkdirs()
        new File(srcDir, 'Hello.java').withPrintWriter {
            it.print '''
                package org.example.hello;
                
                public class Hello {
                    public static void main(String[] args) {
                        System.out.println("Hello!");
                    }
                }
            '''.stripIndent()
        }
    }

    private void createJavaModuleInfo(ModuleInfoType moduleInfoType) {
        if(moduleInfoType == NONE) return
        String subdirName = (moduleInfoType == STANDARD) ? 'java' : 'module'
        File moduleInfoDir = new File(testProjectDir.toFile(), "src/main/$subdirName")
        moduleInfoDir.mkdirs()
        new File(moduleInfoDir, 'module-info.java').withPrintWriter {
            it.print '''
                module org.example.hello {
                    exports org.example.hello;
                }
            '''.stripIndent()
        }
    }

    @Unroll
    def "create jar with multiRelease=#multiRelease, sourceCompatibility=#sourceCompatibility, javaCompatibility=#javaCompatibility, moduleInfoType=#moduleInfoType"() {
        when:
        setUpBuild(multiRelease, moduleInfoType, sourceCompatibility)
        def runner = GradleRunner.create()
        BuildResult result = runner
                .forwardOutput()
                .withDebug(true)
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments(['jar', '-is'] + (javaCompatibility ? ["-PjavaCompatibility=$javaCompatibility" as String] : []))
                .build();
        def jarFile = new File("$runner.projectDir/build/libs").listFiles()[0]
        def output = result.output
        def notUsed = output.contains('badass-jar not used')

        then:
        result.task(":jar").outcome == TaskOutcome.SUCCESS
        getLocation(jarFile, 'module-info.class') == expectedModuleInfoLocation
        notUsed == !badassJarUsed
        getManifest(jarFile).contains('Multi-Release: true') == multiReleaseManifest

        where:
        multiRelease | sourceCompatibility | javaCompatibility | moduleInfoType || badassJarUsed | expectedModuleInfoLocation | multiReleaseManifest
        null         | '1.7'               | null              | STANDARD       || true          | 'META-INF/versions/9/'     | true
        true         | '1.8'               | null              | EXTERNAL       || true          | 'META-INF/versions/9/'     | true
        false        | '1.8'               | null              | STANDARD       || true          | ''                         | false
        null         | '9'                 | null              | EXTERNAL       || false         | null                         | false
        true         | '10'                | null              | STANDARD       || false         | ''                         | false
        false        | '11'                | null              | EXTERNAL       || false         | null                         | false
        null         | '1.7'               | null              | NONE           || false         | null                       | false
        true         | '1.8'               | null              | NONE           || false         | null                       | false
        false        | '1.8'               | null              | NONE           || false         | null                       | false
        null         | '9'                 | null              | NONE           || false         | null                       | false
        true         | '10'                | null              | NONE           || false         | null                       | false
        false        | '11'                | null              | NONE           || false         | null                       | false
        null         | '1.7'               | '11'              | STANDARD       || false         | ''                         | false
        true         | '1.8'               | '1.10'            | EXTERNAL       || false         | null                         | false
        false        | '1.8'               | '1.9'             | STANDARD       || false         | ''                         | false
        null         | '9'                 | '1.7'             | EXTERNAL       || true          | 'META-INF/versions/9/'     | true
        true         | '10'                | '1.8'             | STANDARD       || true          | 'META-INF/versions/9/'     | true
        false        | '11'                | '1.8'             | EXTERNAL       || true          | ''                         | false
    }
}
