#!/bin/sh

BASEDIR="/opt/app/crud-api/"
AJSC_HOME="$BASEDIR"
AJSC_CONF_HOME="$AJSC_HOME/bundleconfig/"

if [ -z "$CONFIG_HOME" ]; then
	echo "CONFIG_HOME must be set in order to start up process"
	exit 1
fi

if [ -z "$KEY_STORE_PASSWORD" ]; then
	echo "KEY_STORE_PASSWORD must be set in order to start up process"
	exit 1
else
	echo -e "KEY_STORE_PASSWORD=$KEY_STORE_PASSWORD\n" >> $AJSC_CONF_HOME/etc/sysprops/sys-props.properties
fi

if [ -z "$KEY_MANAGER_PASSWORD" ]; then
	echo "KEY_MANAGER_PASSWORD must be set in order to start up process"
	exit 1
else
	echo -e "KEY_MANAGER_PASSWORD=$KEY_MANAGER_PASSWORD\n" >> $AJSC_CONF_HOME/etc/sysprops/sys-props.properties
fi

# Add any spring bean configuration files to the Gizmo deployment
if [ -n "$SERVICE_BEANS" ]; then
        echo "Adding the following dynamic service beans to the deployment: "
        mkdir -p /tmp/crud-api/v1/conf
        for f in `ls $SERVICE_BEANS`
        do
                cp $SERVICE_BEANS/$f /tmp/crud-api/v1/conf
                echo "Adding dynamic service bean $SERVICE_BEANS/$f"
        done
        jar uf /opt/app/crud-api/services/crud-api_v1.zip* -C /tmp/ crud-api
        rm -rf /tmp/crud-api
fi

CLASSPATH="$AJSC_HOME/lib/*"
CLASSPATH="$CLASSPATH:$AJSC_HOME/extJars/"
CLASSPATH="$CLASSPATH:$AJSC_HOME/etc/"
PROPS="-DAJSC_HOME=$AJSC_HOME"
PROPS="$PROPS -DAJSC_CONF_HOME=$BASEDIR/bundleconfig/"
PROPS="$PROPS -Dlogback.configurationFile=$BASEDIR/bundleconfig/etc/logback.xml"
PROPS="$PROPS -DAJSC_SHARED_CONFIG=$AJSC_CONF_HOME"
PROPS="$PROPS -DAJSC_SERVICE_NAMESPACE=crud-api"
PROPS="$PROPS -DAJSC_SERVICE_VERSION=v1"
PROPS="$PROPS -Dserver.port=9520"
PROPS="$PROPS -DCONFIG_HOME=$CONFIG_HOME"
JVM_MAX_HEAP=${MAX_HEAP:-1024}

echo $CLASSPATH

exec java -Xmx${JVM_MAX_HEAP}m $PROPS -classpath $CLASSPATH com.att.ajsc.runner.Runner context=// sslport=9520
