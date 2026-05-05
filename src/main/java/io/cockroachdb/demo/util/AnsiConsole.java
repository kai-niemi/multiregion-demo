package io.cockroachdb.demo.util;

import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jline.terminal.Terminal;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.util.Assert;

public class AnsiConsole {
    private final Lock lock = new ReentrantLock();

    private final Terminal terminal;

    public AnsiConsole(Terminal terminal) {
        Assert.notNull(terminal, "terminal is null");
        this.terminal = terminal;
    }

    public AnsiConsole header(String format, Object... args) {
        return printf(AnsiColor.BRIGHT_MAGENTA, format, args);
    }

    public AnsiConsole debug(String format, Object... args) {
        return printf(AnsiColor.BRIGHT_BLUE, format, args);
    }

    public AnsiConsole info(String format, Object... args) {
        return printf(AnsiColor.BRIGHT_GREEN, format, args);
    }

    public AnsiConsole warn(String format, Object... args) {
        return printf(AnsiColor.BRIGHT_YELLOW, format, args);
    }

    public AnsiConsole error(String format, Object... args) {
        return printf(AnsiColor.BRIGHT_RED, format, args);
    }

    public AnsiConsole printf(AnsiColor color, String format, Object... args) {
        return println(color, String.format(Locale.US, format, args));
    }

    public AnsiConsole println(AnsiColor color, String text) {
        try {
            lock.lock();
            terminal.writer().println(AnsiOutput.toString(color, text, AnsiColor.DEFAULT));
            terminal.writer().flush();
            return this;
        } finally {
            lock.unlock();
        }
    }
}

