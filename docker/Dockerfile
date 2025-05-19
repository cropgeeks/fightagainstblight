FROM node:22.11-alpine3.20 AS builder

LABEL org.opencontainers.image.authors="sebastian.raubach@hutton.ac.uk;iain.milne@hutton.ac.uk;linda.milne@hutton.ac.uk"

ARG BRANCH=master

# Force docker to not cache the next line
ADD https://api.github.com/repos/cropgeeks/fightagainstblight/git/refs/heads/main version.json
# Clone the FAB server code and client code
RUN echo "Pulling GitHub branch: $BRANCH"
RUN apk add --no-cache git && \
    if [ "$BRANCH" = "master" ] ; then git clone -b "main" --single-branch --depth 1 https://github.com/cropgeeks/fightagainstblight.git /opt/fab ; else git clone -b "$BRANCH" --single-branch --depth 1 https://github.com/cropgeeks/fightagainstblight.git /opt/fab ; fi

# Build the client code
WORKDIR /opt/fab/client
RUN rm -f .env && \
    echo "VUE_APP_BASE_URL=./api/" > .env && \
    apk add --no-cache python3 build-base gcc wget && \
    npm i && \
    npm run build

# Download Gradle and build the server code
WORKDIR /opt/fab/server
RUN apk add --no-cache openjdk21 && \
    wget https://dlcdn.apache.org//ant/binaries/apache-ant-1.10.15-bin.zip -P /tmp/ && \
    unzip /tmp/apache-ant-1.10.15-bin.zip -d /opt/ && \
    echo "project.dist=../client/dist" > /opt/fab/server/build.properties && \
    /opt/apache-ant-1.10.15/bin/ant -f /opt/fab/server/build.xml war


FROM tomcat:10.1-jdk21

LABEL org.opencontainers.image.authors="sebastian.raubach@hutton.ac.uk;iain.milne@hutton.ac.uk;linda.milne@hutton.ac.uk"

RUN apt-get update && \
    apt-get --yes --force-yes install unzip zip && \
    # Obscuring server info
    cd ${CATALINA_HOME}/lib && \
    mkdir -p org/apache/catalina/util/ && \
    unzip -j catalina.jar org/apache/catalina/util/ServerInfo.properties -d org/apache/catalina/util/ && \
    sed -i 's/server.info=.*/server.info=Apache Tomcat/g' org/apache/catalina/util/ServerInfo.properties && \
    zip -ur catalina.jar org/apache/catalina/util/ServerInfo.properties && \
    rm -rf org && cd ${CATALINA_HOME} && \
    # Add a default error page mapping to hide the exception message
    sed -i 's/<\/web-app>/    <error-page>\n        <exception-type>java.lang.Throwable<\/exception-type>\n        <location>\/dev\/null<\/location>\n    <\/error-page>\n<\/web-app>/g' conf/web.xml && \
    sed -i 's/<\/Host>/  <Valve className="org.apache.catalina.valves.ErrorReportValve" showReport="false" showServerInfo="false" \/>\n      <\/Host>/g' conf/server.xml

RUN mkdir -p /usr/local/tomcat/webapps && \
    rm -rf /usr/local/tomcat/webapps/ROOT

COPY --from=builder /opt/fab/server/fab.war /usr/local/tomcat/webapps/ROOT.war

WORKDIR /usr/local/tomcat/