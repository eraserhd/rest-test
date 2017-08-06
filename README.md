# rest-test

An example REST service.

## Usage

You can start the server locally, assuming you have [leiningen] installed,
with the command `lein ring server`.  This will automatically open a browser
window to `localhost:3000`, although the root URL is not handled so you
will see the message "Not found!"

You can add records using the following curl command:
```
$ curl --data-binary @file.txt http://localhost:3000/records
```

[leiningen] https://github.com/technomancy/leiningen

## API

### POST /records

POST requests to `/records` should have the "Content-Type" header set to
`text/csv` or `text/plain`.

The POST body should be one line per record, with each field separated by
delimiters.  The delimiters can be `,`, `|`, or ` ` (a space).  Delimiters
may not be doubled.

End-of-line characters should be single newline (`\n`) characters.  A newline
at the end of the file is accepted.

### The GET endpoints

There are three GET endpoints:

- `/records/birthdate`
- `/records/gender`
- `/records/name`

All three return the same format response, except for the sort order of the
records.  Here's an example response:

```json
{
  "records": [
    {
      "lastName": "Smith",
      "firstName": "Olive",
      "gender": "enby",
      "favoriteColor": "white",
      "birthdate": "9/2/1896"
    },
    {
      "lastName": "Xmith",
      "firstName": "Olive",
      "gender": "enby",
      "favoriteColor": "white",
      "birthdate": "9/2/1896"
    }
  ]
}
```

The sort order is:

| Endpoint             | Sort Order                                 |
|----------------------|--------------------------------------------|
| `/records/birthdate` | Date of birth, ascending                   |
| `/records/gender`    | Gender ascending, then last name ascending |
| `/records/name`      | Last name, descending                      |

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
