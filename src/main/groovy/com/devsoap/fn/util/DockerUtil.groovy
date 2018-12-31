package com.devsoap.fn.util

import org.gradle.api.Project

import java.nio.charset.StandardCharsets

class DockerUtil {

    static String resolveContainerAddress(Project project, String container) {
        inspectContainerProperty(project, container, 'NetworkSettings.IPAddress')
    }

    static boolean isContainerRunning(Project project, String container) {
        try {
            inspectContainerProperty(project, container, 'State.Running').toBoolean()
        } catch(Exception e) {
            false
        }
    }

    private static String inspectContainerProperty(Project project, String container, String property) {
        final ByteArrayOutputStream propertyStream = new ByteArrayOutputStream()
        propertyStream.withStream {
            project.exec {
                commandLine 'docker'
                args  'inspect', '--type', 'container', '-f', "'{{.$property}}'", container
                standardOutput = propertyStream
            }.rethrowFailure()
        }
        String out = new String(propertyStream.toByteArray(), StandardCharsets.UTF_8)
        out.substring(1, out.length()-2) // Unquote
    }
}
