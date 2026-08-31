# Contributing

1. [Fork it](https://help.github.com/articles/fork-a-repo/)
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Added some feature'`)
4. Test your changes (`mvn verify`)
5. Push to the branch (`git push origin my-new-feature`)
6. [Create new Pull Request](https://help.github.com/articles/creating-a-pull-request/)

You need JDK 17 or newer. Maven downloads everything else on the first build.

## Testing

We use [JUnit 5](https://junit.org/junit5/) to write tests, with
[Mockito](https://site.mockito.org) where a test needs to stand in for the
filesystem. Run our test suite with this command:

```
mvn verify
```

A coverage report is written to `target/site/jacoco/index.html`.

There is also a parser benchmark, which is not part of the test suite because it
asserts nothing:

```
mvn -q test-compile exec:java -Dexec.classpathScope=test \
    -Dexec.mainClass=io.github.motdotla.dotenv.ParsePerf
```

## Code Style

We build with `-Xlint:all -Werror`, so the compiler enforces a good deal on its
own — a warning fails the build. Beyond that we follow the
[Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
and [editorconfig](http://editorconfig.org). Please make sure your PR builds
clean:

```
mvn -B verify
```
