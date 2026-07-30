package org.zeylan.compiler;

import org.zeylan.compiler.cli.Sysexits;
import org.zeylan.compiler.cli.ZeylanCommand;
import picocli.CommandLine;

public class Main {

    static void main(String[] args) {
        CommandLine cmd = new CommandLine(new ZeylanCommand());
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            System.err.println(ex.getMessage());
            return Sysexits.EX_SOFTWARE;
        });
        cmd.setParameterExceptionHandler((ex, args2) -> {
            System.err.println(ex.getMessage());
            ex.getCommandLine().usage(System.err);
            return Sysexits.EX_USAGE;
        });

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

}
