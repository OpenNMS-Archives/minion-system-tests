package org.opennms.minion.stests.utils;

import java.net.InetSocketAddress;

import javax.management.openmbean.CompositeData;

import org.apache.karaf.features.management.FeaturesServiceMBean;

/**
 * Utilities that make using Karaf's MBeans easier.
 *
 * @author jwhite
 */
public class KarafMBeanHelper {

    public static boolean isFeatureInstalled(FeaturesServiceMBean mbean, final String featureName) throws Exception {
        // Iterate through all of the available features and search for the feature in question
        // We do this instead of using infoFeature(), since the later throws an NPE if
        // the requested feature name doesn't exist
        for (final Object composite : mbean.getFeatures().values()) {
            final CompositeData feature = (CompositeData)composite;
            
            final String name = (String) feature.get(FeaturesServiceMBean.FEATURE_NAME);
            final Boolean installed = (Boolean) feature.get(FeaturesServiceMBean.FEATURE_INSTALLED);

            if (featureName.equals(name)) {
                return installed;
            }
        }

        return false;
    }

    public static void main(String[] argv) throws Exception {
        InetSocketAddress jmxAddr = new InetSocketAddress("127.0.0.1", 49312);
        InetSocketAddress rmiAddr = new InetSocketAddress("127.0.0.1", 49311);
        KarafMBeanProxyHelper helper = new KarafMBeanProxyHelper(jmxAddr, rmiAddr, "admin", "admin");
        FeaturesServiceMBean featuresService = helper.getFeaturesService("opennms");

        System.err.println(isFeatureInstalled(featuresService, "opennms-activemq"));
        System.err.println(isFeatureInstalled(featuresService, "opennms-act!ivemq"));
    }
}
