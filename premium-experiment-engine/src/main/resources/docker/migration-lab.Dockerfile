FROM eclipse-temurin:21-jdk

ENV MAVEN_VERSION=3.9.6
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar -xz -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/local/bin/mvn

ENV GRADLE_VERSION=8.7
RUN curl -fsSL https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -o /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    ln -s /opt/gradle-${GRADLE_VERSION} /opt/gradle && \
    ln -s /opt/gradle/bin/gradle /usr/local/bin/gradle && \
    rm /tmp/gradle.zip

ENV REWRITE_VERSION=8.10.0
RUN curl -fsSL https://repo1.maven.org/maven2/org/openrewrite/rewrite-cli/${REWRITE_VERSION}/rewrite-cli-${REWRITE_VERSION}.jar -o /opt/rewrite-cli.jar && \
    ln -s /opt/rewrite-cli.jar /usr/local/bin/rewrite-cli

RUN curl -fsSL https://repo1.maven.org/maven2/org/eclipse/transformer/org.eclipse.transformer.cli/1.2.5/org.eclipse.transformer.cli-1.2.5.jar -o /opt/eclipse-transformer.jar && \
    ln -s /opt/eclipse-transformer.jar /usr/local/bin/eclipse-transformer

RUN apt-get update && apt-get install -y --no-install-recommends \
    git zip unzip curl jq sed findutils \
    && rm -rf /var/lib/apt/lists/*

ENV MAVEN_OPTS="-Xmx2g"
WORKDIR /workspace
