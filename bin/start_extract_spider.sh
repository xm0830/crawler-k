#!/usr/bin/env bash

DIR=$(cd $(dirname $0); pwd)
HOME=$(cd $(dirname $DIR); pwd)

cd $HOME

if [ -f "$HOME/work/extract.pid" ]; then
  TARGET_PID=$(cat "$HOME/work/extract.pid")
  if kill -0 $TARGET_PID > /dev/null 2>&1; then
    echo process already started!
    exit 0
  fi
fi

export SPIDER_HOME=$HOME

nohup java -cp $CLASSPATH:$HOME/lib/* com.rainbow.main.ExactSpiderStarter >/dev/null 2>&1 &

echo $! > "$HOME/work/extract.pid"