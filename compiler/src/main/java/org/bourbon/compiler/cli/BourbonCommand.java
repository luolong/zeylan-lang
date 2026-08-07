package org.bourbon.compiler.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "bourbon",
    description = "Bourbon language compiler and toolchain.",
    subcommands = {
        ReplCommand.class,
        RunCommand.class
    },
    mixinStandardHelpOptions = true,

    versionProvider = BourbonCommand.class,

    // Exit codes
    exitCodeOnInvalidInput = Sysexits.EX_USAGE,
    exitCodeOnExecutionException = Sysexits.EX_SOFTWARE
)
public final class BourbonCommand implements Callable<Integer>, CommandLine.IVersionProvider {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.err);
        return Sysexits.EX_OK;
    }

    @Override
    public String[] getVersion() {
        return new String[] {
                "Bourbon Language v0.1",
                "Early stage experiment"
        };
    }

}
