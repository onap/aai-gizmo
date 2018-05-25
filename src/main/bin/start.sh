#!/bin/bash

APP_HOME="/opt/app/crud-api"

if [ -z "$CONFIG_HOME" ]; then
	echo "CONFIG_HOME must be set in order to start up process"
	exit 1
fi

if [ -z "$SERVICE_BEANS" ]; then
	echo "SERVICE_BEANS must be set in order to start up process"
	exit 1
fi

if [ -z "$KEY_STORE_PASSWORD" ]; then
	echo "KEY_STORE_PASSWORD must be set in order to start up process"
	exit 1
fi

PROPS="-DAPP_HOME=$APP_HOME"
PROPS="$PROPS -DCONFIG_HOME=$CONFIG_HOME"
PROPS="$PROPS -Dlogging.config=$APP_HOME/bundleconfig/etc/logback.xml"
PROPS="$PROPS -DKEY_STORE_PASSWORD=$KEY_STORE_PASSWORD"
JVM_MAX_HEAP=${MAX_HEAP:-1024}

set -x
exec java -Xmx${JVM_MAX_HEAP}m $PROPS -jar ${APP_HOME}/gizmo.jar
