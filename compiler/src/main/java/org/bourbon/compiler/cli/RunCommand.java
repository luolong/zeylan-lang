package org.bourbon.compiler.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/// Subcommand to run a Bourbon program
@Command(
    name = "run",
    description = "Run a Bourbon program.",
    mixinStandardHelpOptions = true
)
public final class RunCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return Sysexits.EX_TEMPFAIL;
    }
}
