[![](https://travis-ci.org/imagej/ImageJA.svg?branch=master)](https://travis-ci.org/imagej/ImageJA)

ImageJA is a project that provides a clean [Git](https://imagej.net/develop/git/)
history of the original [ImageJ](https://imagej.net/software/imagej) project,
with a proper `pom.xml` file so that it can be used with
[Maven](https://imagej.net/develop/maven) without hassles.

See the [ImageJA page](https://imagej.net/libs/imageja) for details.

## Editing this repository

Because much of the content in this repository is generated automatically, depending on the type of change you want to make your edit should go in one of three places:

* Source file content changes (e.g. bug fixes) should go to the [imagej1 repository](https://github.com/imagej/imagej1).
* Source file *location* changes (e.g. moving a file to the correct package) go in the [ij1-builds repository](https://github.com/imagej/ij1-builds).
* Changes to the build structure (e.g. `pom.xml` updates) can be done directly in this repository.
