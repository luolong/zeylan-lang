package org.bourbon.compiler.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.bourbon.compiler.Diagnostic;
import org.bourbon.compiler.DiagnosticFormatter;
import org.bourbon.compiler.DiagnosticFormatter.OutputHandler;
import org.bourbon.compiler.Scanner;
import org.bourbon.compiler.Source;

import picocli.CommandLine.Command;

@Command(
    name = "repl",
    description = "Opens a new Bourbon language REPL session.",
    mixinStandardHelpOptions = true
)
public final class ReplCommand implements Callable<Integer> {
    final String prompt1 = "\u0001\u001B[30;44m\u0002 󰟆 bourbon\u0001\u001B[0;34m\u0002\uE0B0\u0001\u001B[0m\u0002 ";
    final String prompt2 = "\u0001\u001B[30;44m\u0002 󰟆    ...\u0001\u001B[0;34m\u0002\uE0B0\u0001\u001B[0m\u0002 ";

    @Override
    public Integer call() {
        DiagnosticFormatter.setUseNerdFonts(true);
        if (System.console() == null) {
            System.err.println("Error: The Bourbon REPL requires an interactive TTY environment and cannot be run in a non-interactive console.");
            System.err.println("Please run the REPL from a native terminal.");
            return Sysexits.EX_UNAVAILABLE;
        }

        try (var terminal = TerminalBuilder.builder()
                .system(true)
                .name("Bourbon")
                .build()) {

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            terminal.writer().println(welcomeBanner());
            terminal.flush();

            while (true) {
                String line;
                try {
                    line = reader.readLine(prompt1);
                } catch (UserInterruptException e) {
                    // Ctrl+C - ignore and clear the line
                    continue;
                } catch (EndOfFileException e) {
                    // Ctrl+D
                    break;
                }

                if (line == null) {
                    break;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                var source = Source.of(line);

                var diagnostics = new ArrayList<Diagnostic>();
                var scanner = new Scanner(source, diagnostics::add);
                var tokens = scanner.scanTokens();

                for (var diagnostic: diagnostics) {
                    DiagnosticFormatter.format(source, diagnostic,
                            OutputHandler.of(text -> terminal.writer().println(text)));
                }

                // Echo back for now
                for (var token : tokens) {
                    terminal.writer().println(token);
                }

                terminal.flush();
            }

        } catch (IOException e) {
            System.err.println("Error initializing REPL terminal: " + e.getMessage());
            return Sysexits.EX_IOERR;
        }

        return Sysexits.EX_OK;
    }

    private String welcomeBanner() {
        StringBuilder out = new StringBuilder("Welcome to Bourbon language REPL session\n");
        for (var versionLine : new BourbonCommand().getVersion()) {
            out.append(versionLine).append("\n");
        }
        return out.append("Start typing and witness the magic!\n").toString();
    }
}
