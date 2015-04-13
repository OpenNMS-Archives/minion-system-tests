FROM dockerfile/java:oracle-java8
MAINTAINER jesse@opennms.com

RUN wget http://apache.cs.utah.edu/karaf/2.4.0/apache-karaf-2.4.0.tar.gz; \
    mkdir /opt/karaf; \
    tar --strip-components=1 -C /opt/karaf -xzf apache-karaf-2.4.0.tar.gz; \
    rm apache-karaf-2.4.0.tar.gz; \
    mkdir /deploy; \
    sed -i 's/^\(felix\.fileinstall\.dir\s*=\s*\).*$/\1\/deploy/' /opt/karaf/etc/org.apache.felix.fileinstall-deploy.cfg

VOLUME ["/deploy"]
EXPOSE 1099 8101 44444 8202 8203 8204
ENTRYPOINT ["/opt/karaf/bin/karaf"]
