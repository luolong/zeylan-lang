package org.bourbon.compiler;

import org.bourbon.compiler.cli.Sysexits;
import org.bourbon.compiler.cli.BourbonCommand;
import picocli.CommandLine;

public class Main {

    static void main(String[] args) {
        CommandLine cmd = new CommandLine(new BourbonCommand());
        cmd.setExecutionExceptionHandler((ex, _, _) -> {
            System.err.println(ex.getMessage());
            return Sysexits.EX_SOFTWARE;
        });
        cmd.setParameterExceptionHandler((ex, _) -> {
            System.err.println(ex.getMessage());
            ex.getCommandLine().usage(System.err);
            return Sysexits.EX_USAGE;
        });

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

}
