/**
 * Copyright (C) 2013 BonitaSoft S.A.
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
 ** 
 * @since 6.0
 */
package org.bonitasoft.engine.api.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.api.impl.transaction.profile.CreateProfileMember;
import org.bonitasoft.engine.api.impl.transaction.profile.DeleteProfileMember;
import org.bonitasoft.engine.api.impl.transaction.profile.GetNumberOfProfileMembers;
import org.bonitasoft.engine.api.impl.transaction.profile.GetProfile;
import org.bonitasoft.engine.api.impl.transaction.profile.GetProfileEntry;
import org.bonitasoft.engine.api.impl.transaction.profile.GetProfilesForUser;
import org.bonitasoft.engine.api.impl.transaction.profile.ProfileMemberUtils;
import org.bonitasoft.engine.command.SGroupProfileMemberAlreadyExistsException;
import org.bonitasoft.engine.command.SRoleProfileMemberAlreadyExistsException;
import org.bonitasoft.engine.command.SUserMembershipProfileMemberAlreadyExistsException;
import org.bonitasoft.engine.command.SUserProfileMemberAlreadyExistsException;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaRuntimeException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.RetrieveException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.identity.IdentityService;
import org.bonitasoft.engine.identity.MemberType;
import org.bonitasoft.engine.persistence.SBonitaSearchException;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.engine.profile.ProfileEntry;
import org.bonitasoft.engine.profile.ProfileEntryNotFoundException;
import org.bonitasoft.engine.profile.ProfileMember;
import org.bonitasoft.engine.profile.ProfileMemberCreator;
import org.bonitasoft.engine.profile.ProfileMemberSearchDescriptor;
import org.bonitasoft.engine.profile.ProfileNotFoundException;
import org.bonitasoft.engine.profile.ProfileService;
import org.bonitasoft.engine.profile.exception.profile.SProfileNotFoundException;
import org.bonitasoft.engine.profile.model.SProfile;
import org.bonitasoft.engine.profile.model.SProfileMember;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.search.descriptor.SearchEntitiesDescriptor;
import org.bonitasoft.engine.search.descriptor.SearchEntityDescriptor;
import org.bonitasoft.engine.search.profile.SearchProfileEntries;
import org.bonitasoft.engine.search.profile.SearchProfileMembersForProfile;
import org.bonitasoft.engine.search.profile.SearchProfiles;
import org.bonitasoft.engine.service.ModelConvertor;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceSingleton;
import org.bonitasoft.engine.service.impl.ServiceAccessorFactory;
import org.bonitasoft.engine.sessionaccessor.SessionAccessor;

/**
 * @author Celine Souchet
 * @author Matthieu Chaffotte
 */
public class ProfileAPIImpl implements ProfileAPI {

    protected TenantServiceAccessor getTenantAccessor() {
        try {
            final SessionAccessor sessionAccessor = ServiceAccessorFactory.getInstance().createSessionAccessor();
            final long tenantId = sessionAccessor.getTenantId();
            return TenantServiceSingleton.getInstance(tenantId);
        } catch (final Exception e) {
            throw new BonitaRuntimeException(e);
        }
    }

    @Override
    public Profile getProfile(final long id) throws ProfileNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProfileService profileService = tenantAccessor.getProfileService();

        SProfile profile;

        final GetProfile getProfileTransaction = new GetProfile(profileService, id);
        try {
            getProfileTransaction.execute();
            profile = getProfileTransaction.getResult();
        } catch (final SProfileNotFoundException e) {
            throw new ProfileNotFoundException(e);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }

        return ModelConvertor.toProfile(profile);
    }

    @Override
    public List<Profile> getProfilesForUser(final long userId) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProfileService profileService = tenantAccessor.getProfileService();
        final GetProfilesForUser getProfilesForUser = new GetProfilesForUser(profileService, userId);
        try {
            getProfilesForUser.execute();
            final List<SProfile> profiles = getProfilesForUser.getResult();
            return ModelConvertor.toProfiles(profiles);
        } catch (final SBonitaException e) {
            throw new RetrieveException(e);
        }
    }

    @Override
    public SearchResult<Profile> searchProfiles(final SearchOptions options) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProfileService profileService = tenantAccessor.getProfileService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchProfiles searchProfileTransaction = new SearchProfiles(profileService, searchEntitiesDescriptor.getProfileDescriptor(), options);
        try {
            searchProfileTransaction.execute();
            return searchProfileTransaction.getResult();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
    }

    protected String getQuerySuffix(final MemberType memberType) {
        String suffix = null;
        switch (memberType) {
            case USER:
                suffix = "ForUser";
                break;
            case GROUP:
                suffix = "ForGroup";
                break;
            case ROLE:
                suffix = "ForRole";
                break;
            case MEMBERSHIP:
                suffix = "ForRoleAndGroup";
                break;
        }
        return suffix;
    }

    @Override
    public Map<Long, Long> getNumberOfProfileMembers(final List<Long> profileIds) {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProfileService profileService = tenantAccessor.getProfileService();
        final GetNumberOfProfileMembers numberOfProfileMembers = new GetNumberOfProfileMembers(profileIds, profileService);
        try {
            numberOfProfileMembers.execute();
            final List<SProfileMember> listOfProfileMembers = numberOfProfileMembers.getResult();
            final Map<Long, SProfileMember> profileMembers = new HashMap<Long, SProfileMember>();
            final Map<Long, Long> result = new HashMap<Long, Long>();
            for (final SProfileMember p : listOfProfileMembers) {
                profileMembers.put(p.getProfileId(), p);
            }

            for (final Long obj : profileMembers.keySet()) {
                long number = 0;
                for (final SProfileMember listOfProfileMember : listOfProfileMembers) {
                    final long tempProfileId = listOfProfileMember.getProfileId();
                    if (obj == tempProfileId) {
                        number++;
                    }
                }
                result.put(obj, number);
            }
            return result;
        } catch (final SBonitaException sbe) {
            throw new RetrieveException(sbe);
        }
    }

    @Override
    public SearchResult<ProfileMember> searchProfileMembers(final String memberType, final SearchOptions options) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProfileService profileService = tenantAccessor.getProfileService();
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        try {
            final SearchProfileMembersForProfile searchProfileMembersForProfile = new SearchProfileMembersForProfile(getProfileMemberQuerySuffix(memberType),
                    profileService, getProfileMemberDescriptor(searchEntitiesDescriptor, memberType), options);
            searchProfileMembersForProfile.execute();
            return searchProfileMembersForProfile.getResult();
        } catch (final SBonitaException sbe) {
            throw new SearchException(sbe);
        }
    }

    private SearchEntityDescriptor getProfileMemberDescriptor(final SearchEntitiesDescriptor searchEntitiesDescriptor, final String memberType)
            throws SBonitaSearchException {
        if (ProfileMemberUtils.USER_TYPE.equals(memberType)) {
            return searchEntitiesDescriptor.getProfileMemberUserDescriptor();
        } else if (ProfileMemberUtils.GROUP_TYPE.equals(memberType)) {
            return searchEntitiesDescriptor.getProfileMemberGroupDescriptor();
        } else if (ProfileMemberUtils.ROLE_TYPE.equals(memberType)) {
            return searchEntitiesDescriptor.getProfileMemberRoleDescriptor();
        } else if (ProfileMemberUtils.ROLE_AND_GROUP_TYPE.equals(memberType)) {
            return searchEntitiesDescriptor.getProfileMemberRoleAndGroupDescriptor();
        } else {
            throw new SBonitaSearchException("Member type must be equalse to user,group,role or roleAndGroup.");
        }
    }

    private String getProfileMemberQuerySuffix(final String memberType) throws SBonitaSearchException {
        if (ProfileMemberUtils.USER_TYPE.equals(memberType)) {
            return ProfileMemberUtils.USER_SUFFIX;
        } else if (ProfileMemberUtils.GROUP_TYPE.equals(memberType)) {
            return ProfileMemberUtils.GROUP_SUFFIX;
        } else if (ProfileMemberUtils.ROLE_TYPE.equals(memberType)) {
            return ProfileMemberUtils.ROLE_SUFFIX;
        } else if (ProfileMemberUtils.ROLE_AND_GROUP_TYPE.equals(memberType)) {
            return ProfileMemberUtils.ROLE_AND_GROUP_SUFFIX;
        } else {
            throw new SBonitaSearchException("Member type must be equalse to user,group,role or roleAndGroup.");
        }
    }

    @Override
    public SearchResult<ProfileEntry> searchProfileEntries(final SearchOptions options) throws SearchException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProfileService profileService = tenantAccessor.getProfileService();

        final SearchProfileEntries searchProfileEntries = new SearchProfileEntries(profileService, tenantAccessor.getSearchEntitiesDescriptor()
                .getProfileEntryDescriptor(), options);
        try {
            searchProfileEntries.execute();
            return searchProfileEntries.getResult();
        } catch (final SBonitaException e) {
            throw new SearchException(e);
        }
    }

    @Override
    public ProfileEntry getProfileEntry(final long id) throws ProfileEntryNotFoundException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProfileService profileService = tenantAccessor.getProfileService();

        final GetProfileEntry getProfileEntryTransaction = new GetProfileEntry(profileService, id);
        try {
            getProfileEntryTransaction.execute();
        } catch (final SBonitaException e) {
            throw new ProfileEntryNotFoundException(e);
        }

        return ModelConvertor.toProfileEntry(getProfileEntryTransaction.getResult());
    }

    @Override
    public ProfileMember createProfileMember(final Long profileId, final Long userId, final Long groupId, final Long roleId) throws CreationException,
            AlreadyExistsException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProfileService profileService = tenantAccessor.getProfileService();
        final IdentityService identityService = tenantAccessor.getIdentityService();

        final MemberType memberType = getMemberType(userId, groupId, roleId);
        final CreateProfileMember createProfileMember = new CreateProfileMember(profileService, identityService, profileId, userId, groupId, roleId, memberType);
        try {
            checkIfProfileMemberExists(tenantAccessor, profileService, profileId, userId, groupId, roleId, memberType);
        } catch (final SBonitaException e1) {
            throw new AlreadyExistsException(e1);
        }
        try {
            createProfileMember.execute();
            ProfileMember profileMember = ModelConvertor.toProfileMember(createProfileMember.getResult());
            return profileMember;
        } catch (final SBonitaException e) {
            throw new CreationException(e);
        }
    }

    @Override
    public ProfileMember createProfileMember(final ProfileMemberCreator creator) throws CreationException, AlreadyExistsException {
        if (creator == null) {
            throw new CreationException("Unable to create a profile member with a null creator!");
        }

        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final SProfileMember sProfileMember = ModelConvertor.constructSProfileMember(creator);
        return createProfileMember(sProfileMember.getProfileId(), sProfileMember.getUserId(), sProfileMember.getGroupId(), sProfileMember.getRoleId());
    }

    protected void checkIfProfileMemberExists(final TenantServiceAccessor tenantAccessor, final ProfileService profileService, final Long profileId,
            final Long userId, final Long groupId, final Long roleId, final MemberType memberType) throws SBonitaException {
        final SearchEntitiesDescriptor searchEntitiesDescriptor = tenantAccessor.getSearchEntitiesDescriptor();
        final SearchEntityDescriptor searchDescriptor = searchEntitiesDescriptor.getProfileMemberUserDescriptor();
        final SearchOptionsBuilder searchOptionsBuilder = new SearchOptionsBuilder(0, 1);
        searchOptionsBuilder.filter(ProfileMemberSearchDescriptor.PROFILE_ID, profileId);

        SearchProfileMembersForProfile searchProfileMembersForProfile;

        switch (memberType) {
            case USER:
                searchProfileMembersForProfile = new SearchProfileMembersForProfile("ForUser", profileService, searchDescriptor, searchOptionsBuilder.done());
                searchOptionsBuilder.filter(ProfileMemberSearchDescriptor.USER_ID, userId);
                searchProfileMembersForProfile.execute();
                if (searchProfileMembersForProfile.getResult().getCount() != 0) {
                    throw new SUserProfileMemberAlreadyExistsException("A profileMember with userId \"" + userId + "\" already exists");
                }
                break;
            case GROUP:
                searchProfileMembersForProfile = new SearchProfileMembersForProfile("ForGroup", profileService, searchDescriptor, searchOptionsBuilder.done());
                searchOptionsBuilder.filter(ProfileMemberSearchDescriptor.GROUP_ID, groupId);
                searchProfileMembersForProfile.execute();

                if (searchProfileMembersForProfile.getResult().getCount() != 0) {
                    throw new SGroupProfileMemberAlreadyExistsException("A profileMember with groupId \"" + groupId + "\" already exists");
                }
                break;
            case ROLE:
                searchProfileMembersForProfile = new SearchProfileMembersForProfile("ForRole", profileService, searchDescriptor, searchOptionsBuilder.done());
                searchOptionsBuilder.filter(ProfileMemberSearchDescriptor.ROLE_ID, roleId);
                searchProfileMembersForProfile.execute();

                if (searchProfileMembersForProfile.getResult().getCount() != 0) {
                    throw new SRoleProfileMemberAlreadyExistsException("A profileMember with roleId \"" + roleId + "\" already exists");
                }
                break;
            default:
                searchProfileMembersForProfile = new SearchProfileMembersForProfile("ForRoleAndGroup", profileService, searchDescriptor,
                        searchOptionsBuilder.done());
                searchOptionsBuilder.filter(ProfileMemberSearchDescriptor.ROLE_ID, roleId);
                searchOptionsBuilder.filter(ProfileMemberSearchDescriptor.GROUP_ID, groupId);
                searchProfileMembersForProfile.execute();

                if (searchProfileMembersForProfile.getResult().getCount() != 0) {
                    throw new SUserMembershipProfileMemberAlreadyExistsException("A profileMember with groupId \"" + groupId + "\" and roleId \"" + roleId
                            + "\" already exists");
                }
                break;
        }
    }

    public MemberType getMemberType(final Long userId, final Long groupId, final Long roleId) throws CreationException {
        MemberType memberType = null;
        if (userId != null) {
            memberType = MemberType.USER;
        } else if (groupId != null && roleId == null) {
            memberType = MemberType.GROUP;
        } else if (roleId != null && groupId == null) {
            memberType = MemberType.ROLE;
        } else if (roleId != null && groupId != null) {
            memberType = MemberType.MEMBERSHIP;
        } else {
            final StringBuilder stb = new StringBuilder("Parameters map must contain at least one of entries: ");
            stb.append(ProfileMemberUtils.USER_ID);
            stb.append(", ");
            stb.append(ProfileMemberUtils.GROUP_ID);
            stb.append(", ");
            stb.append(ProfileMemberUtils.ROLE_ID);
            throw new CreationException(stb.toString());
        }
        return memberType;
    }

    @Override
    public void deleteProfileMember(final Long profilMemberId) throws DeletionException {
        final TenantServiceAccessor tenantAccessor = getTenantAccessor();
        final ProfileService profileService = tenantAccessor.getProfileService();

        final DeleteProfileMember deleteUserProfileTransaction = new DeleteProfileMember(profileService, profilMemberId);

        try {
            deleteUserProfileTransaction.execute();
        } catch (final SBonitaException e) {
            throw new DeletionException(e);
        }
    }

}
