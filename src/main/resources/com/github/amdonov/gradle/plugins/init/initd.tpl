#!/bin/sh
# chkconfig: ${runLevels.join("")} ${startSequence} ${stopSequence}
# description: Control Script for ${daemonName}

. /etc/rc.d/init.d/functions
### BEGIN INIT INFO
# Provides:          ${daemonName}
# Default-Start:     ${runLevels.join(" ")}
# Default-Stop:      0 1 6
# Required-Start:
# Required-Stop:
# Description: Control Script for ${daemonName}
### END INIT INFO

name=${daemonName}
pidfile="/var/run/${daemonName}.pid"
command="${command}"
user="${user}"
group="${group}"
chroot="/"
chdir="/"
nice=""

if [ -e /etc/sysconfig/${daemonName} ]; then
  . /etc/sysconfig/${daemonName}
fi

trace() {
  logger -t "/etc/init.d/${daemonName}" "$@"
}

emit() {
  trace "$@"
  echo "$@"
}

start() {


  # Setup any environmental stuff beforehand


  # Run the program!

  chroot --userspec "$user":"$group" "$chroot" sh -c "

    cd \"$chdir\"
    exec $command
  " >> /var/log/${daemonName}.log 2>> /var/log/${daemonName}.err &

  # Generate the pidfile from here. If we instead made the forked process
  # generate it there will be a race condition between the pidfile writing
  # and a process possibly asking for status.
  echo $! > $pidfile

  emit "$name started"
  return 0
}

reload() {
  # Try to kill HUP the program
  if status ; then
    pid=$(cat "$pidfile")
    trace "Killing $name (pid $pid) with SIGHUP"
    kill -HUP $pid
  fi
}

stop() {
  # Try a few times to kill TERM the program
  if status ; then
    pid=$(cat "$pidfile")
    trace "Killing $name (pid $pid) with SIGTERM"
    kill -TERM $pid
    # Wait for it to exit.
    for i in 1 2 3 4 5 ; do
      trace "Waiting $name (pid $pid) to die..."
      status || break
      sleep 1
    done
    if status ; then
      emit "$name stop failed; still running."
    else
      emit "$name stopped."
    fi
  fi
}

status() {
  if [ -f "$pidfile" ] ; then
    pid=$(cat "$pidfile")
    if ps -p $pid > /dev/null 2> /dev/null ; then
      # process by this pid is running.
      # It may not be our pid, but that's what you get with just pidfiles.
      # TODO(sissel): Check if this process seems to be the same as the one we
      # expect. It'd be nice to use flock here, but flock uses fork, not exec,
      # so it makes it quite awkward to use in this case.
      return 0
    else
      return 2 # program is dead but pid file exists
    fi
  else
    return 3 # program is not running
  fi
}

force_stop() {
  if status ; then
    stop
    status && kill -KILL $(cat "$pidfile")
  fi
}


case "$1" in
  force-start|start|stop|force-stop|restart)
    trace "Attempting '$1' on $name"
    ;;
esac

case "$1" in
  force-start)
    PRESTART=no
    exec "$0" start
    ;;
  start)
    status
    code=$?
    if [ $code -eq 0 ]; then
      emit "$name is already running"
      exit $code
    else
      start
      exit $?
    fi
    ;;
  reload) reload ;;
  stop) stop ;;
  force-stop) force_stop ;;
  status)
    status
    code=$?
    if [ $code -eq 0 ] ; then
      emit "$name is running"
    else
      emit "$name is not running"
    fi
    exit $code
    ;;
  restart)

    stop && start
    ;;
  *)
    echo "Usage: $SCRIPTNAME {start|force-start|stop|force-start|force-stop|status|restart|reload}" >&2
    exit 3
  ;;
esac

exit $?