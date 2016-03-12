package org.opennms.minion.stests;

public class MinionSystemBuilder {

    private boolean m_skipTearDown = false;
    private boolean m_useExisting = false;

    public MinionSystemBuilder skipTearDown(boolean skipTearDown) {
        m_skipTearDown = skipTearDown;
        return this;
    }

    public MinionSystemBuilder useExisting(boolean useExisting) {
        m_useExisting = useExisting;
        return this;
    }

    public MinionSystem build() {
        if (m_useExisting) {
            return new ExistingMinionSystem();
        } else {
            return new NewMinionSystem(m_skipTearDown);
        }
    }
}
