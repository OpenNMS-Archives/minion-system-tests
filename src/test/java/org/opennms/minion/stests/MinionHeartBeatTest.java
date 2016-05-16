/*******************************************************************************
 * This file is part of OpenNMS(R).
 * <p>
 * Copyright (C) 2016 The OpenNMS Group, Inc. OpenNMS(R) is Copyright (C)
 * 1999-2016 The OpenNMS Group, Inc.
 * <p>
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 * <p>
 * OpenNMS(R) is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * <p>
 * OpenNMS(R) is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R). If not, see: http://www.gnu.org/licenses/
 * <p>
 * For more information contact: OpenNMS(R) Licensing
 * <license@opennms.org> http://www.opennms.org/ http://www.opennms.com/
 *******************************************************************************/

package org.opennms.minion.stests;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.greaterThan;

import java.net.InetSocketAddress;
import java.util.Date;

import org.junit.ClassRule;
import org.junit.Test;
import org.opennms.core.criteria.Criteria;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.minion.stests.NewMinionSystem.ContainerAlias;
import org.opennms.minion.stests.utils.DaoUtils;
import org.opennms.minion.stests.utils.HibernateDaoFactory;
import org.opennms.netmgt.dao.api.MinionDao;
import org.opennms.netmgt.dao.hibernate.MinionDaoHibernate;
import org.opennms.netmgt.model.minion.OnmsMinion;

public class MinionHeartBeatTest {

    @ClassRule
    public static MinionSystem minionSystem = MinionSystem.builder().build();

    @Test
    public void minionAcknowledgeTest() {

        Date startOfTest = new Date();
        InetSocketAddress pgsql = minionSystem.getServiceAddress(ContainerAlias.POSTGRES,
                                                                 5432);
        HibernateDaoFactory daoFactory = new HibernateDaoFactory(pgsql);
        MinionDao minionDao = daoFactory.getDao(MinionDaoHibernate.class);

        Criteria criteria = new CriteriaBuilder(OnmsMinion.class).ge("lastCheckedIn",
                                                                     startOfTest).toCriteria();

        await().atMost(1,
                       MINUTES).pollInterval(5,
                                             SECONDS).until(DaoUtils.countMatchingCallable(minionDao,
                                                                                           criteria),
                                                            greaterThan(0));

    }
}
