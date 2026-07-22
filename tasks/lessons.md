# Lessons

## Extracting constants for duplicate literals (java:S1192)
When replacing all occurrences of a literal with a new constant, a blind
`replace_all` ALSO rewrites the constant's own definition line:
`private static final String X = X;` → `self-reference in initializer` compile error.

**How to avoid:** replace USAGES only, leaving the definition holding the raw
literal. Or add the constant first, then replace, then verify the def line still
reads `= "literal";`. ALWAYS run `mvn test-compile` after a batch of such edits —
per-agent self-checks missed 3 of 66 files here.
