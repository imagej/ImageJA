#!/bin/sh

cd "$(dirname "$0")"

jarsigner $(cat .jarsignerrc) -signedjar signed-ij.jar ij.jar dscho
