package org.bonitasoft.engine.synchro.jms;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bonitasoft.engine.command.CommandService;
import org.bonitasoft.engine.command.SCommandExecutionException;
import org.bonitasoft.engine.command.TenantCommand;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.events.model.SEvent;
import org.bonitasoft.engine.events.model.SHandler;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;
import org.bonitasoft.engine.service.TenantServiceAccessor;

public class AddPerfHandlerCommand extends TenantCommand {

    private static final String PROCESSINSTANCE_STATE_UPDATED = "PROCESSINSTANCE_STATE_UPDATED";

    private static final String ACTIVITYINSTANCE_STATE_UPDATED = "ACTIVITYINSTANCE_STATE_UPDATED";

    @Override
    public Serializable execute(final Map<String, Serializable> parameters, final TenantServiceAccessor serviceAccessor)
            throws SCommandExecutionException {
        final EventService eventService = serviceAccessor.getEventService();
        final Long messageTimeout = (Long) parameters.get("timeout");

        try {
            final long tenantId = serviceAccessor.getTenantId();
            if (!containsHandler(eventService, PROCESSINSTANCE_STATE_UPDATED, ProcessInstanceFinishedHandler.class)) {
                eventService.addHandler(PROCESSINSTANCE_STATE_UPDATED, new ProcessInstanceFinishedHandler(tenantId, messageTimeout));
            }
            if (!containsHandler(eventService, ACTIVITYINSTANCE_STATE_UPDATED, TaskReadyHandler.class)) {
                eventService.addHandler(ACTIVITYINSTANCE_STATE_UPDATED, new TaskReadyHandler(tenantId, messageTimeout));
            }
            if (!containsHandler(eventService, ACTIVITYINSTANCE_STATE_UPDATED, FlowNodeReachStateHandler.class)) {
                eventService.addHandler(ACTIVITYINSTANCE_STATE_UPDATED, new FlowNodeReachStateHandler(tenantId, messageTimeout, 3));
            }
            final EntityUpdateDescriptor entityUpdateDescriptor = new EntityUpdateDescriptor();
            entityUpdateDescriptor.addField("system", true);
            final CommandService commandService = serviceAccessor.getCommandService();
            @SuppressWarnings("unchecked")
            final List<Long> commandIds = (List<Long>) parameters.get("commands");
            for (final Long commandId : commandIds) {
                commandService.update(commandService.get(commandId), entityUpdateDescriptor);
            }
        } catch (final SBonitaException e) {
            throw new SCommandExecutionException(e);
        }
        return null;
    }

    private boolean containsHandler(final EventService eventService, final String eventType, final Class<?> clazz) {
        final Set<SHandler<SEvent>> handlers = eventService.getHandlers(eventType);
        if (handlers != null) {
            for (final SHandler<SEvent> sHandler : handlers) {
                if (clazz.isInstance(sHandler)) {
                    return true;
                }
            }
        }
        return false;
    }

}
