package net.minecraftforge.gradle.base.util

import org.jetbrains.annotations.NotNull
import org.slf4j.Logger

class LoggerWriter extends Writer {

    private final Logger logger;
    private final Level level;

    LoggerWriter(Logger logger, Level level) {
        this.logger = logger
        this.level = level
    }

    @Override
    void write(@NotNull char[] cbuf, int off, int len) throws IOException {
        switch (level) {
            case Level.DEBUG:
                logger.debug(new String(cbuf, off, len))
                break
            case Level.INFO:
                logger.info(new String(cbuf, off, len))
                break
            case Level.WARN:
                logger.warn(new String(cbuf, off, len))
                break
            case Level.ERROR:
                logger.error(new String(cbuf, off, len))
                break
        }
    }

    @Override
    void flush() throws IOException {
    }

    @Override
    void close() throws IOException {
    }

    static enum Level {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
}
