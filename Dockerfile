FROM ubuntu:14.04.4
MAINTAINER Dmitry Mozzherin
ENV LAST_FULL_REBUILD 2016-06-29

ENV SBT_VERSION 0.13.11
ENV SBT_HOME /usr/local/sbt
ENV PATH ${PATH}:${SBT_HOME}/bin

RUN apt-get update \
    && apt-get install -y apt-transport-https apt-utils \
       software-properties-common \
    && apt-add-repository ppa:brightbox/ruby-ng \
    && apt-add-repository ppa:openjdk-r/ppa \
    && apt-get update \
    && apt-get install -y ruby2.3 ruby2.3-dev ruby-switch \
       libxslt-dev supervisor build-essential nodejs supervisor \
       zlib1g-dev libssl-dev libreadline-dev libyaml-dev \
       libxml2-dev libxslt-dev nodejs libpq-dev liblzma-dev \
       openjdk-8-jdk curl postgresql-client \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Install sbt
RUN curl -sL "http://dl.bintray.com/sbt/native-packages/sbt/$SBT_VERSION/sbt-$SBT_VERSION.tgz" | gunzip | tar -x -C /usr/local && \
    echo -ne "- with sbt $SBT_VERSION\n" >> /root/.built

RUN locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

RUN ruby-switch --set ruby2.3
RUN echo 'gem: --no-rdoc --no-ri' | tee -a  $HOME/.gemrc
RUN gem install bundle

RUN mkdir /app
WORKDIR /app

COPY ./project/*.sbt ./project/build.properties /app/project/
COPY scalastyle-config.xml build.sbt /app/
RUN sbt test:compile

COPY ./db-migration/Gemfile /app/db-migration/
WORKDIR /app/db-migration
RUN bundle
COPY . /app
WORKDIR /app

CMD ["/app/bin/docker-startup.sh"]