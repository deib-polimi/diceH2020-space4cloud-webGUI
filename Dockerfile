# Copyright 2018 <Marco Lattuada>
FROM ubuntu:16.04
MAINTAINER marco.lattuada@polimi.it

ENV DICE_USER dice
ENV DICE_PASSWORD password
ENV DICE_HOME /home/${DICE_USER}

# Install environment dependencies
RUN apt update
RUN apt -y dist-upgrade
RUN apt -y install git maven openjdk-8-jdk

RUN useradd -ms /bin/bash ${DICE_USER}
RUN echo "${DICE_USER}:${DICE_PASSWORD}" | chpasswd

USER ${DICE_USER}
WORKDIR ${DICE_HOME}

ENV DICE_SHARED_REPO https://github.com/lattuada/diceH2020-space4clouds_shared.git
RUN git clone ${DICE_SHARED_REPO}
RUN cd diceH2020-space4clouds_shared && mvn initialize compile package install

ENV DICE_FRONTEND_REPO http://github.com/lattuada/diceH2020-space4cloud-webgui.git
RUN git clone ${DICE_FRONTEND_REPO}
RUN cd diceH2020-space4cloud-webgui && mvn initialize compile package

COPY src/main/resources/application-docker.properties ${DICE_HOME}/application-docker.properties
EXPOSE 8000
ENTRYPOINT java -jar diceH2020-space4cloud-webgui/target/D-SPACE4Cloud-webGUI-0.3.1-SNAPSHOT.jar --spring.config.location=file:/home/dice/application-docker.properties
