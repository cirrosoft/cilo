FROM groovy:jdk8-alpine
RUN mkdir -p /home/groovy/cilo/workspace/
RUN mkdir -p /home/groovy/cilo/bin/
RUN mkdir -p /home/groovy/cilo/tmp/
ADD cilo-scripts/ /home/groovy/cilo/bin/
ENV PATH="/home/groovy/cilo/bin/:${PATH}"
WORKDIR /home/groovy/cilo/workspace/
# CMD ["sh"]
CMD ["cilo-startup.sh"]
