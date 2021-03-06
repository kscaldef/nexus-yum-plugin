/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.yum.internal.support;

import static org.sonatype.scheduling.TaskState.RUNNING;

import java.util.List;
import java.util.Map.Entry;
import javax.inject.Inject;

import org.junit.After;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.scheduling.ScheduledTask;
import com.google.code.tempusfugit.temporal.Condition;

public abstract class SchedulerYumNexusTestSupport
    extends YumNexusTestSupport
{

    @Inject
    private NexusScheduler nexusScheduler;

    @After
    public void waitForThreadPool()
        throws Exception
    {
        waitFor( new Condition()
        {
            @Override
            public boolean isSatisfied()
            {
                int running = 0;
                for ( Entry<String, List<ScheduledTask<?>>> entry : nexusScheduler.getActiveTasks().entrySet() )
                {
                    for ( ScheduledTask<?> task : entry.getValue() )
                    {
                        if ( RUNNING.equals( task.getTaskState() ) )
                        {
                            running++;
                        }
                    }
                }
                return running == 0;
            }
        } );
    }

}
