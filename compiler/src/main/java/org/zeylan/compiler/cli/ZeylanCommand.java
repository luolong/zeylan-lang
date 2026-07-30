package org.zeylan.compiler.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "zeylan",
    description = "Zeylan language compiler and toolchain.",
    subcommands = {
        ReplCommand.class,
        RunCommand.class
    },
    mixinStandardHelpOptions = true,

    versionProvider = ZeylanCommand.class,

    // Exit codes
    exitCodeOnInvalidInput = Sysexits.EX_USAGE,
    exitCodeOnExecutionException = Sysexits.EX_SOFTWARE
)
public final class ZeylanCommand implements Callable<Integer>, CommandLine.IVersionProvider {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.err);
        return Sysexits.EX_OK;
    }

    @Override
    public String[] getVersion() {
        return new String[] {
                "Zeylan Language v0.1",
                "Early stage experiment"
        };
    }

}
