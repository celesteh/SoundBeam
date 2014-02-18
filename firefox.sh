#! /bin/bash

/usr/bin/nodejs /home/celesteh/Documents/code/osc-web/bridge.js &
pid=$!
/usr/bin/firefox
kill $pid

