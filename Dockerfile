FROM gradle:5.1.1-jdk11

USER root

RUN apt-get update && \
	apt-get -y install \
		bash \
		git \
		tar \
		zip \
		curl \
		ruby \
		wget \
		unzip \
		python \
		jq && \
    apt-get install -y --no-install-recommends ruby curl jq apt-transport-https ca-certificates && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get update && \
    apt-get -y install apt-transport-https \
         ca-certificates \
         curl \
         gnupg2 \
         software-properties-common && \
    curl -fsSL https://download.docker.com/linux/$(. /etc/os-release; echo "$ID")/gpg > /tmp/dkey; apt-key add /tmp/dkey && \
    add-apt-repository \
       "deb [arch=amd64] https://download.docker.com/linux/$(. /etc/os-release; echo "$ID") \
       $(lsb_release -cs) \
       stable" && \
    apt-get update && \
    apt-get -y install docker-ce


COPY . project
WORKDIR project

VOLUME project

