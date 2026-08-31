> If you like dotenv, you will probably love [dotenvx](https://github.com/dotenvx/dotenvx) – for encrypting `.env` files. Thank you for using dotenv. 🙏

# dotenv [![Maven Central](https://img.shields.io/maven-central/v/io.github.motdotla/dotenv.svg?style=flat-square)](https://central.sonatype.com/artifact/io.github.motdotla/dotenv)

<img src="https://raw.githubusercontent.com/motdotla/dotenv/master/dotenv.svg" alt="dotenv" align="right" width="200" />

Dotenv is a zero-dependency library that loads environment variables from a `.env` file into your application's environment. Storing configuration in the environment separate from code is based on [The Twelve-Factor App](https://12factor.net/config) methodology.

&nbsp;

## Usage

Install it.

```xml
<dependency>
  <groupId>io.github.motdotla</groupId>
  <artifactId>dotenv</artifactId>
  <version>17.4.2</version>
</dependency>
```

Create a `.env` file in the root of your project:

```ini
# .env
HELLO="Dotenv"
OPENAI_API_KEY="your-api-key-goes-here"
```

As early as possible in your application, configure dotenv:

```java
// Main.java
import io.github.motdotla.dotenv.Dotenv;

public class Main {
  public static void main(String[] args) {
    Dotenv.config();

    System.out.println("Hello " + Dotenv.processEnv().get("HELLO"));
  }
}
```
```sh
$ java -jar app.jar
◇ injected env (2) from .env
Hello Dotenv
```

That's it. `Dotenv.processEnv()` now has the keys and values you defined in your `.env` file.

&nbsp;

### A note on `processEnv`

A JVM cannot modify its own environment, so dotenv cannot write into `System.getenv()` the way it wrote into Node's `process.env`. Instead, `Dotenv.processEnv()` is a mutable map seeded from `System.getenv()` at startup, and that is where loaded values land. Read your configuration from it:

```java
Dotenv.processEnv().get("HELLO");   // from .env, or from the real environment
System.getenv("HELLO");             // only the real environment — not what .env loaded
```

Values loaded into it are passed on to child processes started by `dotenv run`, so a command run under the CLI sees them as real environment variables.

&nbsp;

## Advanced

<details><summary>Gradle</summary><br>

```groovy
implementation 'io.github.motdotla:dotenv:17.4.2'
```

`DOTENV_CONFIG_ENCODING`, `DOTENV_CONFIG_PATH`, `DOTENV_CONFIG_QUIET`, `DOTENV_CONFIG_DEBUG`, `DOTENV_CONFIG_OVERRIDE`, `DOTENV_CONFIG_SECURE`, and `DOTENV_CONFIG_FAST` provide defaults for `config()` and `dotenv run`. Options/flags passed directly take precedence.

</details>
<details><summary>Loading before application code</summary><br>

Touching `Config` loads your `.env` from its static initializer, before anything else in your application runs. It is the counterpart of `import 'dotenv/config'`, and is configured entirely through the `DOTENV_CONFIG_*` environment variables.

```java
import io.github.motdotla.dotenv.Config;

public class Main {
  static {
    Config.load();
  }

  public static void main(String[] args) {
    // configuration is already loaded here
  }
}
```

</details>
<details><summary>Multi-module builds</summary><br>

For a multi-module build with a structure like `apps/backend`, put the `.env` file in the root of the folder where your process runs.

```ini
# apps/backend/.env
S3_BUCKET="YOURS3BUCKET"
SECRET_KEY="YOURSECRETKEYGOESHERE"
```

</details>
<details><summary>Multiline Values</summary><br>

If you need multiline variables, for example private keys, those are supported with line breaks:

```ini
PRIVATE_KEY="-----BEGIN RSA PRIVATE KEY-----
...
Kh9NV...
...
-----END RSA PRIVATE KEY-----"
```

Alternatively, you can double quote strings and use the `\n` character:

```ini
PRIVATE_KEY="-----BEGIN RSA PRIVATE KEY-----\nKh9NV...\n-----END RSA PRIVATE KEY-----\n"
```

</details>
<details><summary>Comments</summary><br>

Comments may be added to your file on their own line or inline:

```ini
# This is a comment
SECRET_KEY=YOURSECRETKEYGOESHERE # comment
SECRET_HASH="something-with-a-#-hash"
```

Comments begin where a `#` exists, so if your value contains a `#` please wrap it in quotes.

</details>
<details><summary>Parsing</summary><br>

The engine which parses the contents of your file containing environment variables is available to use. It accepts a `String` or a `byte[]` and returns a `Map` with the parsed keys and values.

```java
import io.github.motdotla.dotenv.Dotenv;
import java.util.Map;

Map<String, String> config = Dotenv.parse("BASIC=basic");
System.out.println(config); // {BASIC=basic}
```

</details>
<details><summary>Run</summary><br>

Use `dotenv run --` to run a command with environment variables from your `.env` file.

```bash
$ dotenv run -- java -jar app.jar
◇ injected env (2) from .env
```

Use `-f` to select one or more `.env` files.

```bash
$ dotenv run -f .env.local -f .env -- java -jar app.jar
◇ injected env (2) from .env.local, .env
```

Use `--quiet` to suppress the injected env message.

```bash
$ dotenv run --quiet -- java -jar app.jar
```

Use `--override` to overwrite existing environment variables, and `--debug` for debug logging.

```bash
$ dotenv run --override --debug -- java -jar app.jar
```

Use `--secure` or `config(new ConfigOptions().secure(true))` to decrypt via [dotenvx](https://dotenvx.com).

```bash
$ curl -sfS https://dotenvx.sh | sh
$ dotenv run --secure -- java -jar app.jar
```

```java
Dotenv.config(new ConfigOptions().secure(true));
```

Or with an environment variable:

```bash
$ DOTENV_CONFIG_SECURE=true dotenv run -- java -jar app.jar
```

Both `dotenv run --secure` and `config(new ConfigOptions().secure(true))` resolve `dotenvx` on your `PATH`.

If your `.env` contains `encrypted:` values and you run without `--secure` / `secure(true)`, dotenv warns and leaves them encrypted.

The `DOTENV_CONFIG_*` environment variables work with the CLI too. CLI flags take precedence.

```bash
$ DOTENV_CONFIG_PATH=./.env.local DOTENV_CONFIG_QUIET=true dotenv run -- java -jar app.jar
```

Use `--fast` (or `fast(true)`) for the faster character-scanner parser. Default remains the classic regex parser.

```bash
$ dotenv run --fast -- java -jar app.jar
```

```java
Dotenv.config(new ConfigOptions().fast(true));
```

Supported: `DOTENV_CONFIG_PATH`, `DOTENV_CONFIG_ENCODING`, `DOTENV_CONFIG_QUIET`, `DOTENV_CONFIG_DEBUG`, `DOTENV_CONFIG_OVERRIDE`, `DOTENV_CONFIG_SECURE`, `DOTENV_CONFIG_FAST`.

The CLI ships as an executable jar. Build it with `mvn package` and put a `dotenv` on your `PATH`:

```bash
#!/bin/sh
exec java -jar /path/to/dotenv-17.4.2.jar "$@"
```

</details>
<details><summary>Variable Expansion</summary><br>

Use [dotenvx](https://github.com/dotenvx/dotenvx) for variable expansion.

Reference and expand variables already on your machine for use in your .env file.

```ini
# .env
USERNAME="username"
DATABASE_URL="postgres://${USERNAME}@localhost/my_database"
```
```sh
$ dotenvx run --debug -- java -jar app.jar
⟐ injected env (2) from .env · dotenvx@1.59.1
DATABASE_URL postgres://username@localhost/my_database
```

</details>
<details><summary>Command Substitution</summary><br>

Use [dotenvx](https://github.com/dotenvx/dotenvx) for command substitution.

Add the output of a command to one of your variables in your .env file.

```ini
# .env
DATABASE_URL="postgres://$(whoami)@localhost/my_database"
```
```sh
$ dotenvx run --debug -- java -jar app.jar
⟐ injected env (1) from .env · dotenvx@1.59.1
DATABASE_URL postgres://yourusername@localhost/my_database
```

</details>
<details><summary>Encryption</summary><br>

Use [dotenvx](https://github.com/dotenvx/dotenvx) for encryption.

Add encryption to your `.env` files with a single command.

```
$ dotenvx set HELLO Production -f .env.production

$ DOTENV_PRIVATE_KEY_PRODUCTION="<.env.production private key>" dotenvx run -- java -jar app.jar
⟐ injected env (2) from .env.production · dotenvx@1.59.1
Hello Production
```

[learn more](https://github.com/dotenvx/dotenvx?tab=readme-ov-file#encryption)

</details>
<details><summary>Multiple Environments</summary><br>

Use [dotenvx](https://github.com/dotenvx/dotenvx) to manage multiple environments.

Run any environment locally. Create a `.env.ENVIRONMENT` file and use `-f` to load it. It's straightforward, yet flexible.

```bash
$ echo "HELLO=production" > .env.production

$ dotenvx run -f=.env.production -- java -jar app.jar
Hello production
```

or with multiple .env files

```bash
$ echo "HELLO=local" > .env.local
$ echo "HELLO=World" > .env

$ dotenvx run -f=.env.local -f=.env -- java -jar app.jar
Hello local
```

[more environment examples](https://dotenvx.com/docs/quickstart/environments?utm_source=github&utm_medium=readme&utm_campaign=motdotla-dotenv&utm_content=docs-environments)

</details>
<details><summary>Production</summary><br>

Use [dotenvx](https://github.com/dotenvx/dotenvx) for production deploys.

Create a `.env.production` file.

```sh
$ echo "HELLO=production" > .env.production
```

Encrypt it.

```sh
$ dotenvx encrypt -f .env.production
```

Set `DOTENV_PRIVATE_KEY_PRODUCTION` (found in `.env.keys`) on your server.

```
$ heroku config:set DOTENV_PRIVATE_KEY_PRODUCTION=value
```

Commit your `.env.production` file to code and deploy.

```
$ git add .env.production
$ git commit -m "encrypted .env.production"
$ git push heroku main
```

Dotenvx will decrypt and inject the secrets at runtime using `dotenvx run -- java -jar app.jar`.

</details>
<details><summary>Syncing</summary><br>

Use [dotenvx](https://github.com/dotenvx/dotenvx) to sync your .env files.

Encrypt them with `dotenvx encrypt -f .env` and safely include them in source control. Your secrets are securely synced with your git.

This still subscribes to the twelve-factor app rules by generating a decryption key separate from code.

</details>

&nbsp;

## FAQ

<details><summary>Should I commit my `.env` file?</summary><br/>

No.

Unless you encrypt it with [dotenvx](https://github.com/dotenvx/dotenvx). Then we recommend you do.

</details>
<details><summary>What about variable expansion?</summary><br/>

Use [dotenvx](https://github.com/dotenvx/dotenvx).

</details>
<details><summary>Should I have multiple `.env` files?</summary><br/>

We recommend creating one `.env` file per environment. Use `.env` for local/development, `.env.production` for production and so on. This still follows the twelve factor principles as each is attributed individually to its own environment. Avoid custom set ups that work in inheritance somehow (`.env.production` inherits values from `.env` for example). It is better to duplicate values if necessary across each `.env.environment` file.

> In a twelve-factor app, env vars are granular controls, each fully orthogonal to other env vars. They are never grouped together as “environments”, but instead are independently managed for each deploy. This is a model that scales up smoothly as the app naturally expands into more deploys over its lifetime.
>
> – [The Twelve-Factor App](http://12factor.net/config)

Additionally, we recommend using [dotenvx](https://github.com/dotenvx/dotenvx) to encrypt and manage these.

</details>
<details><summary>Why doesn't `System.getenv()` see my values?</summary><br/>

Because a JVM cannot modify its own environment. `System.getenv()` returns an unmodifiable snapshot of the real process environment, taken at startup.

Read from `Dotenv.processEnv()` instead — it is seeded from `System.getenv()` and is where dotenv writes:

```java
Dotenv.config();
Dotenv.processEnv().get("HELLO");
```

If you need the values to be real environment variables — because a library you do not control reads `System.getenv()`, for example — start your process under the CLI, which passes them to the child:

```bash
$ dotenv run -- java -jar app.jar
```

</details>
<details><summary>How do I load env before other code runs?</summary><br/>

Touch `Config` from a static initializer, before your application's own classes are initialized.

```java
import io.github.motdotla.dotenv.Config;

public class Main {
  static {
    Config.load();
  }

  public static void main(String[] args) {
    // ...
  }
}
```

You can also use the CLI to inject them before the JVM starts:

```bash
dotenv run -- java -jar app.jar
```

</details>
<details><summary>Can I customize/write plugins for dotenv?</summary><br/>

Yes! `Dotenv.config()` returns a result whose `parsed()` map represents the parsed `.env` file. This gives you everything you need to continue setting values yourself. For example:

```java
ConfigResult result = Dotenv.config(new ConfigOptions().processEnv(new LinkedHashMap<>()));
Map<String, String> expanded = myExpander(result.parsed());
Dotenv.populate(Dotenv.processEnv(), expanded);
```

</details>
<details><summary>What rules does the parsing engine follow?</summary><br/>

The parsing engine currently supports the following rules:

- `BASIC=basic` becomes `{BASIC=basic}`
- empty lines are skipped
- lines beginning with `#` are treated as comments
- `#` marks the beginning of a comment (unless when the value is wrapped in quotes)
- empty values become empty strings (`EMPTY=` becomes `{EMPTY=}`)
- inner quotes are maintained (think JSON) (`JSON={"foo": "bar"}` becomes `{JSON={"foo": "bar"}}`)
- whitespace is removed from both ends of unquoted values (`FOO=  some value  ` becomes `{FOO=some value}`)
- single and double quoted values are escaped (`SINGLE_QUOTE='quoted'` becomes `{SINGLE_QUOTE=quoted}`)
- single and double quoted values maintain whitespace from both ends (`FOO="  some value  "` becomes `{FOO=  some value  }`)
- double quoted values expand new lines (`MULTILINE="new\nline"` becomes

```
{MULTILINE=new
line}
```

- backticks are supported (`` BACKTICK_KEY=`This has 'single' and "double" quotes inside of it.` ``)

Whitespace follows JavaScript's definition rather than Java's, so a leading UTF-8 BOM is skipped ahead of the first key, as are non-breaking spaces around a value.

</details>
<details><summary>What about syncing and securing .env files?</summary><br/>

Use [dotenvx](https://github.com/dotenvx/dotenvx) to unlock syncing encrypted .env files over git.

</details>
<details><summary>What if I accidentally commit my `.env` file to code?</summary><br/>

Remove it, [remove git history](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository) and then install the [git pre-commit hook](https://github.com/dotenvx/dotenvx#pre-commit) to prevent this from ever happening again.

```
curl -sfS https://dotenvx.sh | sh
dotenvx precommit --install
```

</details>
<details><summary>What happens to environment variables that were already set?</summary><br/>

By default, we will never modify any environment variables that have already been set. In particular, if there is a variable in your `.env` file which collides with one that already exists in your environment, then that variable will be skipped.

If instead, you want to override them, use the `override` option.

```java
Dotenv.config(new ConfigOptions().override(true));
```

</details>
<details><summary>How can I prevent committing my `.env` file to a Docker build?</summary><br/>

Use the [docker prebuild hook](https://dotenvx.com/docs/features/prebuild?utm_source=github&utm_medium=readme&utm_campaign=motdotla-dotenv&utm_content=docs-prebuild).

```bash
# Dockerfile
...
RUN curl -fsS https://dotenvx.sh/ | sh
...
RUN dotenvx prebuild
CMD ["dotenvx", "run", "--", "java", "-jar", "app.jar"]
```

</details>
<details><summary>Why is the `.env` file not loading my environment variables successfully?</summary><br/>

Most likely your `.env` file is not in the correct place. dotenv looks for it in the process's working directory, which for a JVM is fixed at start-up and reported by `System.getProperty("user.dir")`.

Turn on debug mode and try again.

```java
Dotenv.config(new ConfigOptions().debug(true));
```

You will receive a helpful error outputted to your console.

</details>

&nbsp;

## Docs

Dotenv exposes three functions:

* `config`
* `parse`
* `populate`

### Config

`config` will read your `.env` file, parse the contents, assign it to
`Dotenv.processEnv()`, and return a `ConfigResult` whose `parsed()` holds the
loaded content, or whose `error()` is set if it failed.

```java
ConfigResult result = Dotenv.config();

if (result.error() != null) {
  throw result.error();
}

System.out.println(result.parsed());
```

You can additionally pass options to `config`.

#### Options

##### path

Default: the `.env` in the working directory

Specify a custom path if your file containing environment variables is located elsewhere.

```java
Dotenv.config(new ConfigOptions().path("/custom/path/to/.env"));
```

You can also pass a `URI`:

```java
Dotenv.config(new ConfigOptions().path(URI.create("file:///custom/path/to/.env")));
```

A leading `~` is expanded to your home directory.

Pass in multiple files as a list, and they will be parsed in order and combined with `Dotenv.processEnv()` (or the `processEnv` option, if set). The first value set for a variable will win, unless the `override` option is set, in which case the last value set will win. If a value already exists and `override` is NOT set, no changes will be made to that value.

```java
Dotenv.config(new ConfigOptions().path(List.of(".env.local", ".env")));
```

##### quiet

Default: `false`

Suppress runtime logging message.

```java
Dotenv.config(new ConfigOptions().quiet(true));
```

##### encoding

Default: `utf8`

Specify the encoding of your file containing environment variables. Accepts the
same names Node accepted: `utf8`, `utf16le`/`ucs2`, `latin1`/`binary`, `ascii`,
`base64`, `base64url` and `hex`.

```java
Dotenv.config(new ConfigOptions().encoding("latin1"));
```

##### debug

Default: `false`

Turn on logging to help debug why certain keys or values are not being set as you expect.

```java
Dotenv.config(new ConfigOptions().debug(true));
```

##### override

Default: `false`

Override any environment variables that have already been set with values from your .env file(s). If multiple files have been provided in `path` the override will also be used as each file is combined with the next. Without `override` being set, the first value wins. With `override` set the last value wins.

```java
Dotenv.config(new ConfigOptions().override(true));
```

##### secure

Default: `false`

Decrypt via [dotenvx](https://dotenvx.com). Requires the `dotenvx` CLI on your `PATH`.

```java
Dotenv.config(new ConfigOptions().secure(true));
```

##### fast

Default: `false`

Use the faster character-scanner parser. Default remains the classic regex parser.

```java
Dotenv.config(new ConfigOptions().fast(true));
```

##### processEnv

Default: `Dotenv.processEnv()`

Specify a map to write your environment variables to.

```java
Map<String, String> myMap = new LinkedHashMap<>();
Dotenv.config(new ConfigOptions().processEnv(myMap));

System.out.println(myMap);              // values from .env
System.out.println(Dotenv.processEnv()); // this was not changed or written to
```

### Parse

The engine which parses the contents of your file containing environment
variables is available to use. It accepts a `String` or a `byte[]` and returns
a `Map` with the parsed keys and values.

```java
Map<String, String> config = Dotenv.parse("BASIC=basic".getBytes(StandardCharsets.UTF_8));
System.out.println(config); // {BASIC=basic}
```

#### Options

##### fast

Default: `false`

Use the faster character-scanner parser.

```java
Dotenv.parse(src, new ParseOptions().fast(true));
```

### Populate

The engine which populates the contents of your .env file into a target map is
available for use. It accepts a target, a source, and options. This is useful
for power users who want to supply their own maps.

For example, customizing the source:

```java
Map<String, String> parsed = Map.of("HELLO", "world");

Dotenv.populate(Dotenv.processEnv(), parsed);

System.out.println(Dotenv.processEnv().get("HELLO")); // world
```

For example, customizing the source AND target:

```java
Map<String, String> parsed = Map.of("HELLO", "universe");
Map<String, String> target = new LinkedHashMap<>(Map.of("HELLO", "world"));

Dotenv.populate(target, parsed, new PopulateOptions().override(true).debug(true));

System.out.println(target); // {HELLO=universe}
```

#### options

##### debug

Default: `false`

Turn on logging to help debug why certain keys or values are not being populated as you expect.

##### override

Default: `false`

Override any environment variables that have already been set.

&nbsp;

## CHANGELOG

See [CHANGELOG.md](CHANGELOG.md)
