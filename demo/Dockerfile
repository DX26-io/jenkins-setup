# Useful:
#   http://www.catosplace.net/blog/2015/02/11/running-jenkins-in-docker-containers/
#   https://github.com/jenkinsci/docker#preinstalling-plugins
#   https://engineering.riotgames.com/news/jenkins-docker-proxies-and-compose
FROM jenkins/jenkins:2.238-jdk11


LABEL maintainer "admin-vizcentric <admin@vizcentric.com>"

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

ENV DEBIAN_FRONTEND=noninteractive
ENV ANSIBLE_VERSION 2.6.3
ENV JAVA_OPTS -Djenkins.install.runSetupWizard=false \
              -Djava.awt.headless=true \
              -Dhudson.model.ParametersAction.keepUndefinedParameters=true \
              -Dpermissive-script-security.enabled=no_security

COPY seed/init.groovy /usr/share/jenkins/ref/init.groovy
COPY seed/jenkins_pipeline.groovy /usr/share/jenkins/jenkins_pipeline.groovy
COPY seed/settings.xml /usr/share/jenkins/settings.xml
COPY plugins.txt /usr/share/jenkins/plugins.txt

USER root

# Install tools needed by the master worker for building apps
RUN apt-get update && \
    apt-get install -y --no-install-recommends ruby curl jq apt-transport-https ca-certificates gnupg-agent software-properties-common && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install kubectl
RUN KUBERNETES_VERSION="$( curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt )" && \
    curl -o /usr/local/bin/kubectl -LO "https://storage.googleapis.com/kubernetes-release/release/${KUBERNETES_VERSION}/bin/linux/amd64/kubectl" && \
    chmod 755 /usr/local/bin/kubectl

# Install helm
RUN curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 && \
	chmod 700 get_helm.sh && \
	./get_helm.sh && \
	rm -f .get_helm.sh


# Install Ansible
RUN apt-get update && \
     apt-get install -y --no-install-recommends python-dev python-pip python-setuptools && \
     pip install --no-cache-dir ansible==${ANSIBLE_VERSION} && \
     apt-get clean && \
     rm -rf /var/lib/apt/lists/*

# Install google cloud sdk
RUN echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" | \
	tee -a /etc/apt/sources.list.d/google-cloud-sdk.list && \
	apt-get install -y --no-install-recommends apt-transport-https ca-certificates gnupg && \
	curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key --keyring /usr/share/keyrings/cloud.google.gpg add - && \
	apt-get -y update && apt-get install -y --no-install-recommends google-cloud-sdk



# Making docker in docker possible
RUN apt-get update &&\
	curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add - && \
	add-apt-repository \
	"deb [arch=amd64] https://download.docker.com/linux/debian \
	$(lsb_release -cs) \
	stable" && \
	apt-get update && \
	apt-get -y install docker-ce docker-ce-cli containerd.io && \
	echo "jenkins ALL=NOPASSWD: /usr/bin/docker" >> /etc/sudoers && \
	echo "jenkins ALL=NOPASSWD: /usr/local/bin/docker-compose" >> /etc/sudoers && \
	echo 'Defaults  env_keep += "HOME"' >> /etc/sudoers && \
	apt-get clean && \
	rm -rf /var/lib/apt/lists/* && \
	curl -L "https://github.com/docker/compose/releases/download/1.23.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose \
	&& chmod +x /usr/local/bin/docker-compose

RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/plugins.txt
