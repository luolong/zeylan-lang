#MISE description="Build and run the native compiler binary"
bazel build //compiler:zeylan
if ($args.Count -eq 0) {
  & bazel-bin/compiler/zeylan.exe repl
} else {
  & bazel-bin/compiler/zeylan.exe $args
}
