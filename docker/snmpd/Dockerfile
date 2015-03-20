FROM centos:centos7
MAINTAINER jesse@opennms.com

RUN \
    yum update -y && \
    yum install -y net-snmp && \
    yum clean all

COPY container-files /

ENV LOCATION docker

EXPOSE 161/udp

ENTRYPOINT ["/bootstrap.sh"]
