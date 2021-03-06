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
package org.bonitasoft.engine.api.impl.transaction.actor;

import java.util.Set;

import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.transaction.TransactionContentWithResult;
import org.bonitasoft.engine.core.process.definition.ProcessDefinitionService;
import org.bonitasoft.engine.core.process.definition.model.SActorDefinition;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;

/**
 * @author Yanyan Liu
 * @author Emmanuel Duchastenier
 */
public class GetNumberOfActors implements TransactionContentWithResult<Integer> {

    private final ProcessDefinitionService processDefinitionService;

    private final long processDefinitionId;

    private int numberOfActors;

    public GetNumberOfActors(final ProcessDefinitionService processDefinitionService, final long processDefinitionId) {
        this.processDefinitionService = processDefinitionService;
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public void execute() throws SBonitaException {
        final SProcessDefinition definition = processDefinitionService.getProcessDefinition(processDefinitionId);
        SActorDefinition actorInitiator = definition.getActorInitiator();
        Set<SActorDefinition> actors = definition.getActors();
        numberOfActors = actors.size();
        if ((actorInitiator != null) && !actors.contains(actorInitiator)) {
            numberOfActors++;
        }
    }

    @Override
    public Integer getResult() {
        return numberOfActors;
    }

}
