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

import groovy.transform.CompileStatic
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.slf4j.Marker

/**
 * A logger whose level can be overridden using the system property {@code badass-jar-log-level}.
 *
 * <br>Instances can be creating using {@code PluginLogger.of(clazz)}.
 */
@CompileStatic
class PluginLogger implements Logger {
    private final Logger INTERNAL_LOGGER = Logging.getLogger(PluginLogger)

    @Delegate final Logger logger

    static final String LOG_LEVEL_PROPERTY_NAME = "badass-jar-log-level"
    static final String LOG_PREFIX = "badass-jar: "

    final Level logLevel = pluginLogLevel

    static enum Level { trace, debug, info, warn, error, inherited}

    static PluginLogger of(Class clazz) {
        new PluginLogger(Logging.getLogger(clazz))
    }

    Level getPluginLogLevel() {
        String pluginLevel = System.properties[LOG_LEVEL_PROPERTY_NAME]
        Level.values().find {it.name() == pluginLevel } ?: Level.inherited
    }

    PluginLogger(Logger logger) {
        this.logger = logger
        INTERNAL_LOGGER.info("Log level of $logger.name set to $logLevel")
    }

    @Override boolean isTraceEnabled() { logLevel == Level.inherited ? logger.isTraceEnabled() : logLevel <= Level.trace }
    @Override boolean isDebugEnabled() { logLevel == Level.inherited ? logger.isDebugEnabled() : logLevel <= Level.debug }
    @Override boolean isInfoEnabled() { logLevel == Level.inherited ? logger.isInfoEnabled() : logLevel <= Level.info }
    @Override boolean isWarnEnabled() { logLevel == Level.inherited ? logger.isWarnEnabled() : logLevel <= Level.warn }
    @Override boolean isErrorEnabled() { logLevel == Level.inherited ? logger.isErrorEnabled() : logLevel <= Level.error }


    @Override void trace(String msg) { if(traceEnabled) logger.lifecycle(LOG_PREFIX + msg) }
    @Override void trace(String format, Object arg) { if(traceEnabled) logger.lifecycle(LOG_PREFIX + format, arg) }
    @Override void trace(String format, Object arg1, Object arg2) { if(traceEnabled) logger.lifecycle(LOG_PREFIX + format, arg1, arg2) }
    @Override void trace(String format, Object... arguments) { if(traceEnabled) logger.lifecycle(LOG_PREFIX + format, arguments) }
    @Override void trace(String msg, Throwable t) { if(traceEnabled) logger.lifecycle(LOG_PREFIX + msg, t) }

    @Override boolean isTraceEnabled(Marker marker) { traceEnabled }
    @Override void trace(Marker marker, String msg) { if(traceEnabled) logger.lifecycle(LOG_PREFIX + msg) }
    @Override void trace(Marker marker, String format, Object arg) { if(traceEnabled) logger.lifecycle(LOG_PREFIX + format, arg) }
    @Override void trace(Marker marker, String format, Object arg1, Object arg2) { if(traceEnabled) logger.lifecycle(LOG_PREFIX + format, arg1, arg2) }
    @Override void trace(Marker marker, String format, Object... argArray) { if(traceEnabled) logger.lifecycle(LOG_PREFIX + format, argArray) }
    @Override void trace(Marker marker, String msg, Throwable t) { if(traceEnabled) logger.lifecycle(LOG_PREFIX + msg, t) }

    @Override void debug(String msg) { if(debugEnabled) logger.lifecycle(LOG_PREFIX + msg) }
    @Override void debug(String format, Object arg) { if(debugEnabled) logger.lifecycle(LOG_PREFIX + format, arg) }
    @Override void debug(String format, Object arg1, Object arg2) { if(debugEnabled) logger.lifecycle(LOG_PREFIX + format, arg1, arg2) }
    @Override void debug(String format, Object... arguments) { if(debugEnabled) logger.lifecycle(LOG_PREFIX + format, arguments) }
    @Override void debug(String msg, Throwable t) { if(debugEnabled) logger.lifecycle(LOG_PREFIX + msg, t) }

    @Override boolean isDebugEnabled(Marker marker) { debugEnabled }
    @Override void debug(Marker marker, String msg) { if(debugEnabled) logger.lifecycle(LOG_PREFIX + msg) }
    @Override void debug(Marker marker, String format, Object arg) { if(debugEnabled) logger.lifecycle(LOG_PREFIX + format, arg) }
    @Override void debug(Marker marker, String format, Object arg1, Object arg2) { if(debugEnabled) logger.lifecycle(LOG_PREFIX + format, arg1, arg2) }
    @Override void debug(Marker marker, String format, Object... argArray) { if(debugEnabled) logger.lifecycle(LOG_PREFIX + format, argArray) }
    @Override void debug(Marker marker, String msg, Throwable t) { if(debugEnabled) logger.lifecycle(LOG_PREFIX + msg, t) }

    @Override void info(String msg) { if(infoEnabled) logger.lifecycle(LOG_PREFIX + msg) }
    @Override void info(String format, Object arg) { if(infoEnabled) logger.lifecycle(LOG_PREFIX + format, arg) }
    @Override void info(String format, Object arg1, Object arg2) { if(infoEnabled) logger.lifecycle(LOG_PREFIX + format, arg1, arg2) }
    @Override void info(String format, Object... arguments) { if(infoEnabled) logger.lifecycle(LOG_PREFIX + format, arguments) }
    @Override void info(String msg, Throwable t) { if(infoEnabled) logger.lifecycle(LOG_PREFIX + msg, t) }

    @Override boolean isInfoEnabled(Marker marker) { infoEnabled }
    @Override void info(Marker marker, String msg) { if(infoEnabled) logger.lifecycle(LOG_PREFIX + msg) }
    @Override void info(Marker marker, String format, Object arg) { if(infoEnabled) logger.lifecycle(LOG_PREFIX + format, arg) }
    @Override void info(Marker marker, String format, Object arg1, Object arg2) { if(infoEnabled) logger.lifecycle(LOG_PREFIX + format, arg1, arg2) }
    @Override void info(Marker marker, String format, Object... argArray) { if(infoEnabled) logger.lifecycle(LOG_PREFIX + format, argArray) }
    @Override void info(Marker marker, String msg, Throwable t) { if(infoEnabled) logger.lifecycle(LOG_PREFIX + msg, t) }

    @Override void warn(String msg) { if(warnEnabled) logger.lifecycle(LOG_PREFIX + msg) }
    @Override void warn(String format, Object arg) { if(warnEnabled) logger.lifecycle(LOG_PREFIX + format, arg) }
    @Override void warn(String format, Object arg1, Object arg2) { if(warnEnabled) logger.lifecycle(LOG_PREFIX + format, arg1, arg2) }
    @Override void warn(String format, Object... arguments) { if(warnEnabled) logger.lifecycle(LOG_PREFIX + format, arguments) }
    @Override void warn(String msg, Throwable t) { if(warnEnabled) logger.lifecycle(LOG_PREFIX + msg, t) }

    @Override boolean isWarnEnabled(Marker marker) { warnEnabled }
    @Override void warn(Marker marker, String msg) { if(warnEnabled) logger.lifecycle(LOG_PREFIX + msg) }
    @Override void warn(Marker marker, String format, Object arg) { if(warnEnabled) logger.lifecycle(LOG_PREFIX + format, arg) }
    @Override void warn(Marker marker, String format, Object arg1, Object arg2) { if(warnEnabled) logger.lifecycle(LOG_PREFIX + format, arg1, arg2) }
    @Override void warn(Marker marker, String format, Object... argArray) { if(warnEnabled) logger.lifecycle(LOG_PREFIX + format, argArray) }
    @Override void warn(Marker marker, String msg, Throwable t) { if(warnEnabled) logger.lifecycle(LOG_PREFIX + msg, t) }

    @Override void error(String msg) { if(errorEnabled) logger.lifecycle(LOG_PREFIX + msg) }
    @Override void error(String format, Object arg) { if(errorEnabled) logger.lifecycle(LOG_PREFIX + format, arg) }
    @Override void error(String format, Object arg1, Object arg2) { if(errorEnabled) logger.lifecycle(LOG_PREFIX + format, arg1, arg2) }
    @Override void error(String format, Object... arguments) { if(errorEnabled) logger.lifecycle(LOG_PREFIX + format, arguments) }
    @Override void error(String msg, Throwable t) { if(errorEnabled) logger.lifecycle(LOG_PREFIX + msg, t) }

    @Override boolean isErrorEnabled(Marker marker) { errorEnabled }
    @Override void error(Marker marker, String msg) { if(errorEnabled) logger.lifecycle(LOG_PREFIX + msg) }
    @Override void error(Marker marker, String format, Object arg) { if(errorEnabled) logger.lifecycle(LOG_PREFIX + format, arg) }
    @Override void error(Marker marker, String format, Object arg1, Object arg2) { if(errorEnabled) logger.lifecycle(LOG_PREFIX + format, arg1, arg2) }
    @Override void error(Marker marker, String format, Object... argArray) { if(errorEnabled) logger.lifecycle(LOG_PREFIX + format, argArray) }
    @Override void error(Marker marker, String msg, Throwable t) { if(errorEnabled) logger.lifecycle(LOG_PREFIX + msg, t) }
}
