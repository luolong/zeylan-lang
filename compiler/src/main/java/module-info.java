module bourbon.compiler {

    requires org.jspecify;
    requires info.picocli;
    requires org.jline.terminal;
    requires org.jline.reader; requires transitive org.junit.jupiter.api;

    exports org.bourbon.compiler;
    exports org.bourbon.compiler.cli;

    opens org.bourbon.compiler.cli to info.picocli;
}
