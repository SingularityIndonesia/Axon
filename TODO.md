# TODO

## ContextWrapper
Execution context should wrap around intent execution, not live explicitly inside the Intent.

When the same Intent type is dispatched by multiple instances concurrently, listeners may need
a context to correctly identify which execution produced which result.

A `ContextWrapper` is the proposed solution — context surrounds the execution scope organically
rather than being carried as a field on the Intent itself.
