package com.tiendaagil.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Punto común para obtener loggers de la aplicación. */
public final class LogConfig {
    private LogConfig() {}
    public static Logger logger(Class<?> type) { return LoggerFactory.getLogger(type); }
}
