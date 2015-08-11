FROM java:8
MAINTAINER jesse@opennms.com

ENV OPENNMS_RELEASE stable
RUN wget -O - http://debian.opennms.org/OPENNMS-GPG-KEY | apt-key add -
RUN echo "deb http://debian.opennms.org ${OPENNMS_RELEASE} main" > /etc/apt/sources.list.d/opennms.list
RUN echo "deb-src http://debian.opennms.org ${OPENNMS_RELEASE} main" >> /etc/apt/sources.list.d/opennms.list

RUN apt-get update && apt-get install -y jicmp jicmp6 jrrd rrdtool

# Webapp
EXPOSE 8980
# JMX
EXPOSE 18980
# Karaf RMI
EXPOSE 1099
# Karaf SSH
EXPOSE 8101
# MQ
EXPOSE 61616

CMD ["/opt/provisioning/bootstrap.sh"]
