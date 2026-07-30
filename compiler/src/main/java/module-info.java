module zeylan.compiler {

    requires org.jspecify;
    requires info.picocli;
    requires org.jline.terminal;
    requires org.jline.reader;

    exports org.zeylan.compiler;
    exports org.zeylan.compiler.cli;

    opens org.zeylan.compiler.cli to info.picocli;
}
