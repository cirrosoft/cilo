FROM groovy:jdk8-alpine
USER root
RUN apk add --no-cache bash
RUN apk add --no-cache openssh
RUN apk add --no-cache curl
RUN apk add --no-cache jq
RUN apk add --no-cache openssl
RUN apk add --no-cache emacs
RUN addgroup -S cilogroup && adduser -S cilo -G cilogroup
USER cilo
RUN echo 'export PS1="\u:\w$ "' >> ~/.bashrc
RUN mkdir -p /home/cilo/workspace/
RUN mkdir -p /home/cilo/bin/
RUN mkdir -p /home/cilo/tmp/
ADD cilo-scripts/ /home/cilo/bin/
ENV PATH="/home/cilo/bin/:${PATH}"
WORKDIR /home/cilo/workspace/
ENTRYPOINT ["bash"]
CMD ["cilo-run-pipeline"]
