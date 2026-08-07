#MISE description="Build and run the compiler REPL"
bazel build //compiler:compiler_jvm
& bazel-bin/compiler/compiler_jvm.cmd repl $args
