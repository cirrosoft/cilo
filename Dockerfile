FROM groovy:jdk8-alpine
USER root
RUN apk add --no-cache bash &&\
    apk add --no-cache openssh &&\
    apk add --no-cache curl &&\
    apk add --no-cache jq &&\
    apk add --no-cache openssl &&\
    apk add --no-cache emacs &&\
    addgroup -S cilogroup && adduser -S cilo -G cilogroup &&\
    # docker
    apk add --no-cache docker &&\
    apk add --no-cache expect

USER cilo
RUN echo 'export PS1="\u:\w$ "' >> ~/.bashrc &&\
    echo 'docker() { unbuffer -p docker -H "$CILO_DOCKER_ADDRESS" $@ ; }' >> ~/.bashrc &&\
    mkdir -p /home/cilo/bin/ &&\
    mkdir -p /home/cilo/secret/local/
ADD cilo-scripts/ /home/cilo/bin/
ENV PATH="/home/cilo/bin/:${PATH}"
WORKDIR /home/cilo/workspace/
ENTRYPOINT ["bash"]
