FROM maven:3.6.0-jdk-8-alpine

#'-v $HOME/.m2:/root/.m2'
COPY pom.xml ./

RUN mvn clean install -Dskiptests

COPY ./ .

ENTRYPOINT ["java", "ConfigApplication"]

#CMD ["java", "-jar", "./target/eureka-0.0.1-SNAPSHOT.jar"]