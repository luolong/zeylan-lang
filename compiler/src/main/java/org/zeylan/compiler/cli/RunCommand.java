package org.zeylan.compiler.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/// Subcommand to run a Zeylan program
@Command(
    name = "run",
    description = "Run a Zeylan program.",
    mixinStandardHelpOptions = true
)
public final class RunCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return Sysexits.EX_TEMPFAIL;
    }
}
