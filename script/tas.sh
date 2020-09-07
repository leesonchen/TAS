#!/bin/bash
MAIN_CLASS=com.leeson.TAS
CMD="java -Djava.ext.dirs=lib -cp conf:`ls *.jar` $MAIN_CLASS"

case $1 in
'start')
    nohup $CMD &
    echo "$0 has started"
    ;;

'stop')
    kill -9 `ps -ef|grep $MAIN_CLASS|grep -v grep|awk '{print $2}'`
    echo "$0 has stopped"
    ;;

'status')
    if [ `ps -ef|grep -c $MAIN_CLASS` -ge "1" ]; then
        echo "$0 has started"
    else
        echo "$0 has stopped"
    fi
    ;;

'restart')

    kill -9 `ps -ef|grep $MAIN_CLASS|grep -v grep|awk '{print $2}'`
    nohup $CMD &
    ;;

*)
    echo "usage:$0{start|stop|restart|status}"
    ;;
esac
