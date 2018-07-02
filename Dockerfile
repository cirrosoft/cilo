FROM groovy:jdk8-alpine
USER root
RUN apk add --no-cache curl
RUN apk add --no-cache jq
RUN apk add --no-cache openssl
USER groovy
RUN mkdir -p /home/groovy/cilo/workspace/
RUN mkdir -p /home/groovy/cilo/bin/
RUN mkdir -p /home/groovy/cilo/tmp/
ADD cilo-scripts/ /home/groovy/cilo/bin/
ENV PATH="/home/groovy/cilo/bin/:${PATH}"
WORKDIR /home/groovy/cilo/workspace/
CMD ["sh"]
# CMD ["cilo-startup.sh"]
