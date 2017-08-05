# rest-test

A Clojure library designed to ... well, that part is up to you.

## Usage

FIXME

## Development

### TDD

Tests are written test-first fasion using Brian Marick's [Midje].  Midje's
REPL tools are automatically loaded into the REPL's default development
namespace -- you should see a banner explaining how to get its online docs:

```
Run `(doc midje)` for Midje usage.
Run `(doc midje-repl)` for descriptions of Midje repl functions.
```

The most important REPL command is `(autotest)`.  After you run this command,
Midje will watch for file changes and compute which tests need to be re-run,
and run them with nice red or green output.

[suggest-commit] knows how to find new Midje tests and suggest their fact
names for your commit messages.  This'll speed up your inner loop a bit.

[Midje]: https://github.com/marick/Midje
[suggest-commit]: https://github.com/eraserhd/suggest-commit

## License

Copyright © 2017 Jason M. Felice
