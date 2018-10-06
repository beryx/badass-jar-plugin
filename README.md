## badass-jar ##

A Gradle plugin that provides a `badassJar` task, which can be configured using the `badassJar` extension.

The current task implementation reads the value of `badassJarInputProperty` and writes it into the file specified by `badassJarOutputProperty`.

Example `build.gradle` configuration:

```
plugins {
    id 'org.beryx.jar' version '1.0.0'
}

badassJar {
    badassJarInputProperty = 'my-input-value'
    badassJarOutputProperty = file('my-output-file.txt')
}
```
