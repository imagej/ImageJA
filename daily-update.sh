#!/bin/sh

cd $(dirname $0)/..
perl tools/wsync.perl
if [ -f .wsync-add ]; then
	cat .wsync-add | tr "\n" "\0" | xargs -0r perl ~/my/tools/mac2unix.pl
	cat .wsync-add | tr "\n" "\0" | xargs -0r git-update-index --add
fi
if [ -f .wsync-remove ]; then
	cat .wsync-remove | tr "\n" "\0" | xargs git-ls-files | xargs -0r rm
	cat .wsync-remove | tr "\n" "\0" | xargs git-ls-files | xargs -0r git-update-index --remove
fi
GIT_AUTHOR_NAME="Wayne Rasband" GIT_AUTHOR_EMAIL="wsr@nih.gov" git commit -m "updated $(date +"%d.%m.%Y %H:%M")" && git push itas
