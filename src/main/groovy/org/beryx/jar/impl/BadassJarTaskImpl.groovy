package org.beryx.jar.impl

class BadassJarTaskImpl {
    String badassJarInputProperty
    File badassJarOutputProperty

    BadassJarTaskImpl(String sampleInputProperty, File badassJarOutputProperty) {
        this.badassJarInputProperty = sampleInputProperty
        this.badassJarOutputProperty = badassJarOutputProperty
    }

    void execute() {
        badassJarOutputProperty.text = "badassJar: badassJarInputProperty = $badassJarInputProperty"
    }
}
