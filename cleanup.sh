#!/bin/sh

git-ls-files --others -z | xargs -0 rm

