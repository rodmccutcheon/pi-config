docker run -it --rm \
       -v "$(PWD)":/opt/maven \
       -w /opt/maven \
       maven:3.6.0-jdk-8-alpine \
       mvn clean install