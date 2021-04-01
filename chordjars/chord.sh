#!/bin/sh

for index in {1..5}
do
	echo "Main-Class: no.hvl.dat110.node.peers.Process$index" > Manifest.txt
	G:/Programming/Java/jdk1.8.0_251/bin/jar cfm process$index.jar Manifest.txt -C ../tasks/bin/ .
done