FROM ubuntu:14.04

ARG MICRO_HOME=/opt/app/crud-api
ARG BIN_HOME=$MICRO_HOME/bin

RUN apt-get update

# Install and setup java8
RUN apt-get update && apt-get install -y software-properties-common
## sudo -E is required to preserve the environment. If you remove that line, it will most like freeze at this step
RUN sudo -E add-apt-repository ppa:openjdk-r/ppa && apt-get update && apt-get install -y openjdk-8-jdk
## Setup JAVA_HOME, this is useful for docker commandline
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk--amd64
RUN export JAVA_HOME

# Build up the deployment folder structure
RUN mkdir -p $MICRO_HOME
RUN mkdir -p $MICRO_HOME/bundleconfig/etc
COPY gizmo.jar $MICRO_HOME/
RUN mkdir -p $BIN_HOME
COPY *.sh $BIN_HOME
COPY bundleconfig-local $MICRO_HOME/bundleconfig
COPY bundleconfig-local/etc/logback.xml $MICRO_HOME/bundleconfig/etc
RUN chmod 755 $BIN_HOME/*
RUN ln -s /logs $MICRO_HOME/logs

EXPOSE 9520 9520

CMD ["/opt/app/crud-api/bin/start.sh"]
