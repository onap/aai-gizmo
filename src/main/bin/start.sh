#!/bin/bash

BASEDIR="/opt/app/crud-api/"
AJSC_HOME="$BASEDIR"
AJSC_CONF_HOME="$AJSC_HOME/bundleconfig/"

# List of ajsc properties which are exposed for modification at deploy time
declare -a MODIFY_PROP_LIST=("KEY_STORE_PASSWORD"
                             "KEY_MANAGER_PASSWORD"
                             "AJSC_JETTY_ThreadCount_MIN" 
                             "AJSC_JETTY_ThreadCount_MAX"
                             "AJSC_JETTY_BLOCKING_QUEUE_SIZE")
PROP_LIST_LENGTH=${#MODIFY_PROP_LIST[@]}  

for (( i=1; i<${PROP_LIST_LENGTH}+1; i++ ));
do
   PROP_NAME=${MODIFY_PROP_LIST[$i-1]}
   PROP_VALUE=${!PROP_NAME}
   if [ ! -z "$PROP_VALUE" ]; then
      sed -i "s/$PROP_NAME.*$/$PROP_NAME=$PROP_VALUE/g" $AJSC_CONF_HOME/etc/sysprops/sys-props.properties
   fi
done

if [ -z "$CONFIG_HOME" ]; then
	echo "CONFIG_HOME must be set in order to start up process"
	exit 1
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
