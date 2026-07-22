# Lessons

## Lombok on JDK 23+ (this project: Java 25)
JDK 23+ disables implicit annotation processing — Lombok on the classpath does
NOTHING unless maven-compiler-plugin sets `<proc>full</proc>` (or
annotationProcessorPaths). Symptom: `cannot find symbol getX()` for annotated
fields. This pom now has `<proc>full</proc>`; don't remove it.
Also: every mvn run bumps `<revision>` in pom.xml (antrun) — pom diffs churn,
that's expected, never revert them.

## Extracting constants for duplicate literals (java:S1192)
When replacing all occurrences of a literal with a new constant, a blind
`replace_all` ALSO rewrites the constant's own definition line:
`private static final String X = X;` → `self-reference in initializer` compile error.

**How to avoid:** replace USAGES only, leaving the definition holding the raw
literal. Or add the constant first, then replace, then verify the def line still
reads `= "literal";`. ALWAYS run `mvn test-compile` after a batch of such edits —
per-agent self-checks missed 3 of 66 files here.

## Pipeline exit codes lie
`mvn ... | tail -20; echo EXIT=$?` reports tail's exit (always 0), not maven's.
A broken build looked green. **Use `${PIPESTATUS[0]}` or grep for `BUILD SUCCESS`.**

## Concurrent sessions mutate the tree
Another Claude session edited/reverted/committed this working tree mid-task —
my uncommitted edits vanished, then reappeared inside the other session's commit.
**Before verifying or diagnosing "mystery" failures: re-run `git status`/`git log`
to confirm tree state still matches assumptions. Keep edit scripts idempotent and
re-runnable so re-applying after a clobber is one command.**
