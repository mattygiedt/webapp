FROM ubuntu:20.04

# Install OpenJDK-17
RUN apt-get update && \
    apt-get install -y openjdk-17-jre-headless && \
    apt-get install -y build-essential && \
    apt-get install -y wget unzip curl && \
    apt-get install -y clang-format && \
    apt-get install -y protobuf-compiler && \
    apt-get clean;

# Fix certificate issues
RUN apt-get update && \
    apt-get install ca-certificates-java && \
    apt-get clean && \
    update-ca-certificates -f;

# Setup gradle
WORKDIR /gradle
RUN curl -L https://services.gradle.org/distributions/gradle-7.6-bin.zip -o gradle-7.6-bin.zip
RUN unzip gradle-7.6-bin.zip

# Setup GRADLE_HOME and PATH
RUN echo 'export GRADLE_HOME=/gradle/gradle-7.6\n \
          export PATH=${GRADLE_HOME}/bin:${PATH}' >> /etc/profile.d/gradle.sh

# Setup JAVA_HOME -- useful for docker commandline
ENV JAVA_HOME /usr/lib/jvm/java-17-openjdk-amd64/
