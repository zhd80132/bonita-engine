package org.bonitasoft.engine.bpm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bonitasoft.engine.actor.mapping.ActorMappingService;
import org.bonitasoft.engine.actor.mapping.SActorNotFoundException;
import org.bonitasoft.engine.actor.mapping.model.SActor;
import org.bonitasoft.engine.actor.mapping.model.SActorBuilderFactory;
import org.bonitasoft.engine.actor.mapping.model.SActorMember;
import org.bonitasoft.engine.api.impl.IdentityAPIImpl;
import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.identity.Group;
import org.bonitasoft.engine.identity.Role;
import org.bonitasoft.engine.transaction.STransactionException;
import org.bonitasoft.engine.transaction.TransactionService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ActorMappingServiceTest extends CommonBPMServicesTest {

    private final BPMServicesBuilder servicesBuilder;

    private final TransactionService transactionService;

    private final ActorMappingService actorMappingService;

    private final SActorBuilderFactory sActorBuilderFactory;

    private Group mainGroup;

    private Group parentGroup;

    private Group childGroup;

    @Before
    public void setup() throws AlreadyExistsException, CreationException {
        mainGroup = createGroup("main");
        parentGroup = createGroup("parent");
        childGroup = createGroup("child", "/parent");
    }

    @After
    public void tearDown() throws DeletionException {
        try {
            transactionService.begin();
            new IdentityAPIImpl().deleteGroups(Arrays.asList(childGroup.getId(), parentGroup.getId(), mainGroup.getId()));
            transactionService.complete();
        } catch (STransactionException e) {
            throw new DeletionException(e);
        }
    }

    public ActorMappingServiceTest() {
        super();
        servicesBuilder = getServicesBuilder();
        transactionService = servicesBuilder.getTransactionService();
        actorMappingService = servicesBuilder.getActorMappingService();
        sActorBuilderFactory = BuilderFactory.getInstance().get(SActorBuilderFactory.class);
    }

    @Test(expected = SActorNotFoundException.class)
    public void cannotGetAnUnknownActor() throws SBonitaException {
        transactionService.begin();
        try {
            actorMappingService.getActor(0);
        } finally {
            transactionService.complete();
        }
    }

    @Test
    public void getAnActor() throws SBonitaException {
        final Set<SActor> actors = new HashSet<SActor>();
        final String manager = "Manager";
        final long scopeId = 12;
        final SActor actor = sActorBuilderFactory.create(manager, scopeId, false).getActor();
        actors.add(actor);
        transactionService.begin();
        try {
            actorMappingService.addActors(actors);
        } finally {
            transactionService.complete();
        }
        transactionService.begin();
        try {
            final SActor sActor = actorMappingService.getActor(manager, scopeId);
            assertNotNull(sActor);
            assertEquals(manager, sActor.getName());
            assertEquals(scopeId, sActor.getScopeId());
            actorMappingService.deleteActors(scopeId);
        } finally {
            transactionService.complete();
        }
    }

    @Test
    public void getActorsFromActorIds() throws SBonitaException {
        final Set<SActor> actors = new HashSet<SActor>();

        final String manager = "Manager";
        final long scopeId = 12;
        final SActor actor = sActorBuilderFactory.create(manager, scopeId, false).getActor();
        actors.add(actor);

        final String manager2 = "Leader";
        final long scopeId2 = 12;
        final SActor actor2 = sActorBuilderFactory.create(manager2, scopeId2, false).getActor();
        actors.add(actor2);

        transactionService.begin();
        try {
            actorMappingService.addActors(actors);
        } finally {
            transactionService.complete();
        }
        transactionService.begin();
        long actor1id, actor2id;
        try {
            final SActor sActor1 = actorMappingService.getActor(manager, scopeId);
            assertNotNull(sActor1);
            assertEquals(manager, sActor1.getName());
            assertEquals(scopeId, sActor1.getScopeId());
            actor1id = sActor1.getId();

            final SActor sActor2 = actorMappingService.getActor(manager2, scopeId2);
            assertNotNull(sActor2);
            assertEquals(manager2, sActor2.getName());
            assertEquals(scopeId2, sActor2.getScopeId());
            actor2id = sActor2.getId();
        } finally {
            transactionService.complete();
        }

        transactionService.begin();
        try {
            final List<Long> actorIds = Arrays.asList(actor1id, actor2id);
            final List<SActor> actorsRes = actorMappingService.getActors(actorIds);

            boolean isHasManager = false;
            boolean isHasManager2 = false;
            assertEquals(2, actorsRes.size());
            for (final SActor sActor : actorsRes) {
                if (manager.equals(sActor.getName())) {
                    isHasManager = true;
                } else {
                    if (manager2.equals(sActor.getName())) {
                        isHasManager2 = true;
                    }
                }
            }
            assertTrue(isHasManager && isHasManager2);

            actorMappingService.deleteActors(scopeId2);
        } finally {
            transactionService.complete();
        }
    }

    @Test
    public void addAndRemoveAUserOfAnActor() throws Exception {
        Set<SActor> actors = new HashSet<SActor>();
        final SActor actor = sActorBuilderFactory.create("Manager", 12, false).getActor();
        actors.add(actor);

        transactionService.begin();
        actors = actorMappingService.addActors(actors);
        final long actorId = actors.iterator().next().getId();

        List<SActorMember> actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
        Assert.assertEquals(0, actorMembers.size());

        actorMappingService.addUserToActor(actorId, 1);
        transactionService.complete();

        transactionService.begin();
        try {
            actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
            Assert.assertEquals(1, actorMembers.size());
            checkActorMember(actorMembers.get(0), 1, -1, -1);

            actorMappingService.removeActorMember(actorMembers.get(0).getId());
            actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
            Assert.assertEquals(0, actorMembers.size());
            actorMappingService.deleteActors(12);
        } finally {
            transactionService.complete();
        }
    }

    private void checkActorMember(final SActorMember sActorMember, final long userId, final long groupId, final long roleId) {
        assertEquals(userId, sActorMember.getUserId());
        assertEquals(groupId, sActorMember.getGroupId());
        assertEquals(roleId, sActorMember.getRoleId());
    }

    @Test
    public void addAndRemoveARoleOfAnActor() throws Exception {
        Set<SActor> actors = new HashSet<SActor>();
        final SActor actor = sActorBuilderFactory.create("Manager", 12, false).getActor();
        actors.add(actor);

        transactionService.begin();
        actors = actorMappingService.addActors(actors);
        final long actorId = actors.iterator().next().getId();

        List<SActorMember> actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
        Assert.assertEquals(0, actorMembers.size());

        actorMappingService.addRoleToActor(actorId, 1);
        transactionService.complete();

        transactionService.begin();
        try {
            actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
            Assert.assertEquals(1, actorMembers.size());
            checkActorMember(actorMembers.get(0), -1, -1, 1);

            actorMappingService.removeActorMember(actorMembers.get(0).getId());
            actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
            Assert.assertEquals(0, actorMembers.size());
            actorMappingService.deleteActors(12);
        } finally {
            transactionService.complete();
        }
    }

    @Test
    public void addAndRemoveAGroupOfAnActor() throws Exception {
        Set<SActor> actors = new HashSet<SActor>();
        final SActor actor = sActorBuilderFactory.create("Manager", 12, false).getActor();
        actors.add(actor);

        transactionService.begin();
        actors = actorMappingService.addActors(actors);
        final long actorId = actors.iterator().next().getId();

        List<SActorMember> actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
        Assert.assertEquals(0, actorMembers.size());

        actorMappingService.addGroupToActor(actorId, mainGroup.getId());
        transactionService.complete();

        transactionService.begin();
        try {
            actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
            Assert.assertEquals(1, actorMembers.size());
            checkActorMember(actorMembers.get(0), -1, mainGroup.getId(), -1);

            actorMappingService.removeActorMember(actorMembers.get(0).getId());
            actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
            Assert.assertEquals(0, actorMembers.size());
            actorMappingService.deleteActors(12);
        } finally {
            transactionService.complete();
        }
    }

    @Test
    public void addAndRemoveAGroupWithSubGroupsOfAnActor() throws Exception {
        Set<SActor> actors = new HashSet<SActor>();
        final SActor actor = sActorBuilderFactory.create("Manager", 12, false).getActor();
        actors.add(actor);

        transactionService.begin();
        actors = actorMappingService.addActors(actors);
        final long actorId = actors.iterator().next().getId();

        List<SActorMember> actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
        Assert.assertEquals(0, actorMembers.size());

        actorMappingService.addGroupToActor(actorId, parentGroup.getId());
        transactionService.complete();

        transactionService.begin();
        try {
            actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
            Assert.assertEquals(2, actorMembers.size());
            checkActorMember(actorMembers.get(0), -1, parentGroup.getId(), -1);
            checkActorMember(actorMembers.get(1), -1, childGroup.getId(), -1);

            actorMappingService.removeActorMember(actorMembers.get(0).getId());
            actorMappingService.removeActorMember(actorMembers.get(1).getId());
            actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
            Assert.assertEquals(0, actorMembers.size());
            actorMappingService.deleteActors(12);
        } finally {
            transactionService.complete();
        }
    }

    @Test
    public void addAndRemoveAMembershipOfAnActor() throws Exception {
        Set<SActor> actors = new HashSet<SActor>();
        final SActor actor = sActorBuilderFactory.create("Manager", 12, false).getActor();
        actors.add(actor);

        transactionService.begin();
        actors = actorMappingService.addActors(actors);
        final long actorId = actors.iterator().next().getId();

        List<SActorMember> actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
        Assert.assertEquals(0, actorMembers.size());

        actorMappingService.addRoleAndGroupToActor(actorId, 1, mainGroup.getId());
        transactionService.complete();

        transactionService.begin();
        try {
            actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
            Assert.assertEquals(1, actorMembers.size());
            checkActorMember(actorMembers.get(0), -1, mainGroup.getId(), 1);

            actorMappingService.removeActorMember(actorMembers.get(0).getId());
            actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
            Assert.assertEquals(0, actorMembers.size());
            actorMappingService.deleteActors(12);
        } finally {
            transactionService.complete();
        }
    }

    @Test
    public void addAndRemoveAMembershipOfAnActorWithSubGroups() throws Exception {
        Set<SActor> actors = new HashSet<SActor>();
        final SActor actor = sActorBuilderFactory.create("Manager", 12, false).getActor();
        actors.add(actor);

        transactionService.begin();
        actors = actorMappingService.addActors(actors);
        final long actorId = actors.iterator().next().getId();

        List<SActorMember> actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
        Assert.assertEquals(0, actorMembers.size());

        actorMappingService.addRoleAndGroupToActor(actorId, 1, parentGroup.getId());
        transactionService.complete();

        transactionService.begin();
        try {
            actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
            Assert.assertEquals(2, actorMembers.size());
            checkActorMember(actorMembers.get(0), -1, parentGroup.getId(), 1);
            checkActorMember(actorMembers.get(1), -1, childGroup.getId(), 1);

            actorMappingService.removeActorMember(actorMembers.get(0).getId());
            actorMappingService.removeActorMember(actorMembers.get(1).getId());
            actorMembers = actorMappingService.getActorMembers(actorId, 0, 10);
            Assert.assertEquals(0, actorMembers.size());
            actorMappingService.deleteActors(12);
        } finally {
            transactionService.complete();
        }
    }

    @Test
    public void countActorMembers() throws Exception {
        final long scopId = 12;
        SActor actor = sActorBuilderFactory.create("Manager", scopId, false).getActor();

        transactionService.begin();
        actor = actorMappingService.addActor(actor);

        final List<SActorMember> actorMembers = actorMappingService.getActorMembers(actor.getId(), 0, 10);
        Assert.assertEquals(0, actorMembers.size());

        actorMappingService.addRoleToActor(actor.getId(), 41);
        actorMappingService.addUserToActor(actor.getId(), 7);
        actorMappingService.addGroupToActor(actor.getId(), mainGroup.getId());
        actorMappingService.addGroupToActor(actor.getId(), childGroup.getId());
        actorMappingService.addUserToActor(actor.getId(), 83);

        final long numberOfActorMembers = actorMappingService.getNumberOfActorMembers(actor.getId());
        assertEquals(5L, numberOfActorMembers);
        actorMappingService.deleteActors(12);

        transactionService.complete();
    }

    @Test
    public void getNumberOfRolesOfActorShouldNotCountMemberships() throws Exception {
        final long scopId = 12;
        transactionService.begin();
        final IdentityAPIImpl identityAPI = new IdentityAPIImpl();
        final Role role1 = identityAPI.createRole("roletest");
        final Role role2 = identityAPI.createRole("role2test");
        transactionService.complete();

        SActor actor = sActorBuilderFactory.create("ActorRoleTest", scopId, false).getActor();
        transactionService.begin();
        actor = actorMappingService.addActor(actor);

        final List<SActorMember> actorMembers = actorMappingService.getActorMembers(actor.getId(), 0, 10);
        Assert.assertEquals(0, actorMembers.size());

        actorMappingService.addRoleAndGroupToActor(actor.getId(), role1.getId(), mainGroup.getId());
        actorMappingService.addRoleToActor(actor.getId(), role2.getId());

        final long numberOfActorMembers = actorMappingService.getNumberOfRolesOfActor(actor.getId());
        transactionService.complete();

        assertEquals(1L, numberOfActorMembers);

        // clean-up:
        transactionService.begin();
        actorMappingService.deleteActors(12);
        identityAPI.deleteRole(role1.getId());
        identityAPI.deleteRole(role2.getId());
        transactionService.complete();
    }

    @Test
    public void getNumberOfGroupsOfActorShouldNotCountMemberships() throws Exception {
        final long scopId = 12;
        SActor actor = sActorBuilderFactory.create("ActorGroupTest", scopId, false).getActor();

        transactionService.begin();
        actor = actorMappingService.addActor(actor);

        final List<SActorMember> actorMembers = actorMappingService.getActorMembers(actor.getId(), 0, 10);
        Assert.assertEquals(0, actorMembers.size());

        actorMappingService.addRoleAndGroupToActor(actor.getId(), 21, mainGroup.getId());
        actorMappingService.addGroupToActor(actor.getId(), mainGroup.getId());

        final long numberOfActorMembers = actorMappingService.getNumberOfGroupsOfActor(actor.getId());
        assertEquals(1L, numberOfActorMembers);

        actorMappingService.deleteActors(12);
        transactionService.complete();
    }
}
