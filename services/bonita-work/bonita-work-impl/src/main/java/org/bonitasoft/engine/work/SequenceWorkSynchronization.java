/**
 * Copyright (C) 2012 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.work;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;

/**
 * @author Charles Souillard
 * @author Baptiste Mesta
 */
public class SequenceWorkSynchronization extends AbstractWorkSynchronization {

    private final TechnicalLoggerService loggerService;

    // private final RunnableListener runnableListener;

    public SequenceWorkSynchronization(final ExecutorWorkService runnableListener, final ExecutorService executorService,
            final TechnicalLoggerService loggerService, final SessionAccessor sessionAccessor) {
        super(runnableListener, executorService, sessionAccessor);
        // this.runnableListener = runnableListener;
        this.loggerService = loggerService;
    }

    @Override
    protected void executeRunnables(final Collection<BonitaWork> works) {
        executorService.submit(new SequenceRunnableExecutor(works, getTenantId(), loggerService));
    }

}
