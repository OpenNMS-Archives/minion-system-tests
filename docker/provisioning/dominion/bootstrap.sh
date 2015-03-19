#! /bin/bash
OPENNMS_HOME=/opt/opennms

echo "OPENNMS HOME: "${OPENNMS_HOME}

# We need an opennms.tar.gz file
if [ ! -f /opt/provisioning/opennms.tar.gz ]; then
    echo "There is no opennms.tar.gz file located in /opt/provisioning."
    exit 1
fi

# We need a smnnepo.war file
if [ ! -f /opt/provisioning/smnnepo.war ]; then
    echo "There is no smnnepo.war file located in /opt/provisioning."
    exit 1
fi

# Configure OpenNMS
mkdir -p ${OPENNMS_HOME}
tar xvf /opt/provisioning/opennms.tar.gz -C ${OPENNMS_HOME}

# Copy the smnnepo server components to opennms
cp /opt/provisioning/smnnepo.war ${OPENNMS_HOME}/jetty-webapps

# Overwrite some default config parameters
cp /opt/provisioning/etc/* ${OPENNMS_HOME}/etc/

# Point PostgreSQL to the linked container
sed -i 's/\$POSTGRES_TCP_PORT\$/'"${POSTGRES_PORT_5432_TCP_PORT}"'/g' "${OPENNMS_HOME}/etc/opennms-datasources.xml"

# Expose the Karaf shell
sed -i s/sshHost.*/sshHost=0.0.0.0/g "${OPENNMS_HOME}/etc/org.apache.karaf.shell.cfg"

# Start OpenNMS
${OPENNMS_HOME}/bin/runjava -s
${OPENNMS_HOME}/bin/install -dis
"${OPENNMS_HOME}/bin/opennms" -f start
