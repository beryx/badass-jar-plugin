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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class BadassJarPluginSpec extends Specification {
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()

    private static String createBuildContent(Boolean multiRelease, String sourceCompatibility) {
        String content = """
            plugins {
                id 'java'
                id 'org.beryx.jar'
            }
            sourceCompatibility = $sourceCompatibility
        """.stripIndent()
        def multiReleaseCfg = (multiRelease == null) ? '' : """
                jar {
                    multiRelease = $multiRelease
                }""".stripIndent()
        return content + multiReleaseCfg
    }

    private static String getLocation(File jarFile, String fileName) {
        def zipFile = new ZipFile(jarFile)
        ZipEntry entry = zipFile.entries().find() { it.name == fileName || it.name.endsWith("/$fileName")}
        return entry ? entry.name - fileName : null
    }

    def setUpBuild(Boolean multiRelease, String sourceCompatibility, boolean hasModuleInfo) {
        createJavaMain()
        if(hasModuleInfo) createJavaModuleInfo()

        File buildFile = testProjectDir.newFile("build.gradle")
        buildFile.text = createBuildContent(multiRelease, sourceCompatibility)
        println "Executing build script:\n${buildFile.text}"
    }

    private void createJavaMain() {
        File srcDir = new File(testProjectDir.root, 'src/main/java/org/example/hello')
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

    private void createJavaModuleInfo() {
        File srcDir = new File(testProjectDir.root, 'src/main/java')
        srcDir.mkdirs()
        new File(srcDir, 'module-info.java').withPrintWriter {
            it.print '''
                module org.example.hello {
                    exports org.example.hello;
                }
            '''.stripIndent()
        }
    }

    @Unroll
    def "create jar with multiRelease=#multiRelease, sourceCompatibility=#sourceCompatibility, hasModuleInfo=#hasModuleInfo"() {
        when:
        setUpBuild(multiRelease, sourceCompatibility, hasModuleInfo)
        def runner = GradleRunner.create()
        BuildResult result = runner
                .withProjectDir(testProjectDir.root)
                .withPluginClasspath()
                .withArguments('jar', "-is")
                .build();
        def jarFile = new File("$runner.projectDir/build/libs").listFiles()[0]
        def output = result.output
        def notUsed = output.contains('badass-jar not used')

        then:
        result.task(":jar").outcome == TaskOutcome.SUCCESS
        getLocation(jarFile, 'module-info.class') == expectedModuleInfoLocation
        notUsed == !badassJarUsed

        where:
        multiRelease | sourceCompatibility | hasModuleInfo || badassJarUsed | expectedModuleInfoLocation
        null         | '1.7'               | true          || true          | 'META-INF/versions/9/'
        true         | '1.8'               | true          || true          | 'META-INF/versions/9/'
        false        | '1.8'               | true          || true          | ''
        null         | '9'                 | true          || false         | ''
        true         | '10'                | true          || false         | ''
        false        | '11'                | true          || false         | ''
        null         | '1.7'               | false         || false         | null
        true         | '1.8'               | false         || false         | null
        false        | '1.8'               | false         || false         | null
        null         | '9'                 | false         || false         | null
        true         | '10'                | false         || false         | null
        false        | '11'                | false         || false         | null
    }
}
