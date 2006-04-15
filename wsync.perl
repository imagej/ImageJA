#!/usr/bin/perl

use Time::Local;

@stack = ("");
$baseURL = "http://rsb.info.nih.gov/ij/source/";

unlink '.wsync-all';
unlink '.wsync-add';
unlink '.wsync-remove';

%months = ("Jan" => 0, "Feb" => 1, "Mar" => 2, "Apr" => 3, "May" => 4,
"Jun" => 5, "Jul" => 6, "Aug" => 7, "Sep" => 8, "Oct" => 9, "Nov" => 10,
"Dec" => 11);

sub parseDate {
	my $year = $_[0];
	my $month = $_[1];
	my $day = $_[2];
	my $hour = $_[3];
	my $minute = $_[4];

#print "date:$minute:$hour:$day:$month:$year\n";
	return timegm(0, $minute, $hour, $day, $months{$month}, $year);
}

sub append {
	my $path = $_[0];
	my $line = $_[1];

	open my $f, ">>" . $path;
	print $f $line . "\n";
	close $f;
}

sub getFile {
	my $path = $_[0];
	my $date = $_[1];

	my @list = stat($path);
	if ($#list > 0 && $list[9] >= $date) {
		return;
	}
	`wget -O $path $baseURL$path`;
	utime $date, $date, $path;
	append('.wsync-add', $path);
}

sub handleDir {
	my $path = $_[0];
	my $path1 = $path;
	my $path2 = $path;
	if ($path eq "") {
		$path1 = ".";
	} else {
		$path2 .= "/";
	}

	`wget -O .index.html $baseURL$path`;

	my %exists = ("." => 1, ".." => 1);
	open my $f, "<.index.html";
	while (<$f>) {
		if (/A HREF="([^"]*)".*(\d\d)-([^-]*)-(\d\d\d\d) *(\d\d):(\d\d)/) {
			my $p = $1;
			my $date = parseDate($4, $3, $2, $5, $6);
			if ($p =~ /\//) {
				if ($p =~ /^[^\/]/) {
					append('.wsync-all', $path2 . $p);
					$p =~ s/\/$//;
					my $p1 = $path2 . $p;
					if (! -d $p1) {
						mkdir $p1;
					}
					push @stack, $p1;
				}
			} else {
				getFile($path2 . $p, $date);
				append('.wsync-all', $path2 . $p);
			}
			$exists{$p} = 1;
		}
	}
	close $f;

	opendir(my $dir, $path1);
	my $d;
	while (($d = readdir($dir))) {
		if (!$exists{$d}) {
			#`rm -rf $path2$d`;
			append('.wsync-remove', $path2 . $d);
		}
	}
	closedir $dir;
}

while ($#stack >= 0) {
	handleDir(pop @stack);
}

