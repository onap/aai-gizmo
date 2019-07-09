FROM @aai.docker.namespace@/aai-common-@aai.base.image@:@aai.base.image.version@

ARG MICRO_HOME=/opt/app/crud-api
ARG BIN_HOME=$MICRO_HOME/bin
ARG USERS_HOME=/opt/aaihome

# AAI-2177: Change aai gizmo container processes to run as non-root on the host

#Note:The group id and user id used below (492382 & 341790 respectively) are chosen arbitarily based on assumption that
# these are not used elsewhere. Please see  https://jira.onap.org/browse/AAI-2172 for more background on this.

RUN mkdir -p $MICRO_HOME $USERS_HOME /logs \
    && groupadd -g 492382 aaiadmin \
    && useradd -r -u 341790  -g 492382 -ms /bin/sh -d $USERS_HOME/aaiadmin aaiadmin
WORKDIR $MICRO_HOME
COPY /maven/gizmo/ .
RUN chmod 755 $BIN_HOME/* \
    && ln -snf /logs $MICRO_HOME/logs \
    && chown -R aaiadmin:aaiadmin $MICRO_HOME /logs

USER aaiadmin

EXPOSE 9520 9520

CMD ["/opt/app/crud-api/bin/start.sh"]
