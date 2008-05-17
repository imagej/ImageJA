#!/usr/bin/perl

if($ARGV[0] eq "--add-last-newline") {
	shift(@ARGV);
	$add_newline=1;
} else {
	$add_newline=0;
}

sub inittransform () {
	$orig="";
	$nonorig="";
	$charcount=0;
	$nonasciicount=0;
	$passthru=0;
}

sub dotransform (*$) {
	my $file = shift;
	$_ = shift;
	if ($passthru) {
PASSTHRU:
		print $file $_;
		goto CONTINUE;
	}
	if ($charcount<256) {
		foreach $c (unpack("c*",$_)) {
			$charcount++;
			if ($c>127 || ($c<32 && $c!=10 && $c!=13 && $c!=9)) {
				$nonasciicount++;
			}
		}
		if ($charcount<256) {
			$orig.=$_;
		} elsif ($nonasciicount>10) {
			$passthru=1;
			print $file $orig;
			goto PASSTHRU;
		} else {
			print $file $nonorig;
		}
	}
	s/\r\n?/\n/g;
	if($add_newline && !/\n$/) {
		$_.="\n";
	}
	if ($charcount>=256) {
		print $file $_;
	} else {
		$nonorig.=$_;
	}
CONTINUE:
}

sub tinitransform (*) {
	my $file = shift;
	if ($charcount<256) {
		if ($nonasciicount<0.1*$charcount) {
			print $file $nonorig;
		} else {
			print $file $orig;
		}
	}
}

if($#ARGV>=0) {
	foreach $i (@ARGV) {
		inittransform();
		open IN, "<".$i || die "could not open ".$i;
		open OUT, "> ".$i.".tmpp" || die "could not open ".$i.".tmpp";
		while(<IN>) {
			dotransform(\*OUT,$_);
		}
		tinitransform(\*OUT);
		close IN;
		close OUT;
		unlink $i || die "could not remove ".$i;
		rename $i.".tmpp", $i;
	}
} else {
	inittransform();
	while(<>) {
		dotransform(\*STDOUT,$_);
	}
	tinitransform(\*STDOUT);
}

