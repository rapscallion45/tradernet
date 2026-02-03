package com.tradernet.entities;

import com.tradernet.common.SystemProperties;
import com.tradernet.common.exception.ObjectNotFoundException;
import com.tradernet.databaseservice.passwords.PBKDF2PasswordHash;
import com.tradernet.databaseservice.passwords.PasswordHash;
import com.tradernet.databaseservice.passwords.PlainTextCredentials;
import com.tradernet.jpa.dao.interceptors.PasswordMetadataInterceptor;
import com.tradernet.entities.generic.IdentifiedEntity;
import com.tradernet.enums.UserStatus;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tradernet.entities.util.RelationshipUpdateUtil.updateRelationship;

/**
 * Entity representing a system User.
 */
@Entity
@Cacheable
@Table(name = "tblUsers")
@NamedQueries({
    @NamedQuery(name = "GetUserByUsername", query = "SELECT u FROM UserEntity u WHERE LOWER(u.username) = :username", hints = {
        @QueryHint(name = "org.hibernate.cacheable", value = "true")}), // LOWER is used in conjunction with toLowerCase() on input parameter - we don't want to compare in a case-sensitive manner
    @NamedQuery(name = "GetUsersInType", query = "SELECT u FROM UserEntity u WHERE u.type = :userstatus"),
    @NamedQuery(name = "GetUsersNotInTypes", query = "SELECT u FROM UserEntity u WHERE u.type not in (:userstatuses) ORDER BY u.username")
})
public class UserEntity implements IdentifiedEntity {

    private static final Logger log = LoggerFactory.getLogger(UserEntity.class);

    private static final boolean IsExternalIdentityManagementEnabled = SystemProperties.getBooleanFlag(SystemProperties.EfsExternalIdentityManagement());
    // The user password is accessed frequently and rarely modified
    // See caching strategy in architecture.md in src/docs for implementation detail
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<PasswordEntity> passwords = new HashSet<>();
    // The user password is accessed frequently and rarely modified
    // See caching strategy in architecture.md in src/docs for implementation detail
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
    @OneToMany(mappedBy = "id.user", fetch = FetchType.EAGER, cascade = {CascadeType.ALL}, orphanRemoval = true)
    private final Set<UserPropertyEntity> userProperties = new HashSet<>();
    // The users roles are accessed frequently and rarely modified
    // See caching strategy in architecture.md in src/docs for implementation detail
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
    @ManyToMany
    @JoinTable(name = "tblUserRoles",
        joinColumns = @JoinColumn(name = "userId"),
        inverseJoinColumns = @JoinColumn(name = "roleId"))
    private final Set<RoleEntity> roles = new HashSet<>();
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
    @ManyToMany(mappedBy = "users")
    private final Set<GroupEntity> groups = new HashSet<>();
    @Id
    private long id;
    @NotNull
    @Size(max = 50)
    private String username;
    @Size(max = 255)
    @Column(name = "password_hash")
    private String passwordHash;
    @NotNull
    private int type;
    private Date accountExpiry;
    @Size(max = 255)
    private String emailAddress;
    private boolean passwordNoExpire;
    private Date lastLogin;
    @NotNull
    private int incorrectLoginAttempts;
    @NotNull
    private boolean bypassLockout;
    @NotNull
    private boolean changePasswordNextLogin;
    @Size(max = 255)
    private String fullName;
    @NotNull
    private boolean bypassDocumentSecurity = false;
    @NotNull
    private boolean isExternalIdentity = false;

    /* Transient properties ignored by persistence provider */
    @Transient
    private boolean passwordExpired = false; //todo refactor this variable into a new enum which represents state of password - normal/inWarningPeriod/expired etc

    @Transient
    private Integer passwordExpiresInDays;

    //todo - find out how to map a parameter to an sql query so jpa can do this for us!
    @Transient
    private PasswordEntity latestPassword;

    @Transient
    private boolean isLockedOut; //todo refactor this variable into a new enum which represents state of password - normal/inWarningPeriod/expired etc

    @Transient
    private boolean bypassServerLockout;

    public UserEntity() {
    }

    /**
     * Initialises the object with the given username, and {@link com.tradernet.enums.UserStatus#STANDARD}
     */
    public UserEntity(String username) {
        this.username = username;
        this.setStatus(UserStatus.STANDARD);
    }

    @PostLoad
    private void performPasswordProcessing() {
        PasswordMetadataInterceptor.populatePasswordMetadata(this);
    }

    /**
     * @return The password entity of the users' current password
     */
    public PasswordEntity getLatestPassword() {
        if (latestPassword == null)
            setupLatestPassword();
        return latestPassword;
    }

    /**
     * Lazily find the latest password for this user. Ideally this could be replaced by some sort of hibernate formula
     */
    private void setupLatestPassword() {

        if (IsExternalIdentityManagementEnabled)
            return; // don't do all this expensive work as we don't need the output

        PasswordEntity latestPassword = null;

        for (PasswordEntity password : getPasswords()) {
            if (latestPassword == null || password.getLastChanged().after(latestPassword.getLastChanged()))
                latestPassword = password;
        }

        log.debug("User[{}] Latest password: {}", getUsername(), latestPassword);
        this.latestPassword = latestPassword;
    }

    /**
     * @return Has this user expired?
     */
    public boolean isAccountExpired() {

        Date expiryDate = getAccountExpiry();
        if (expiryDate == null)
            return false;

        return new Date().after(expiryDate);
    }

    /**
     * Checks to see if the provided password is in the list of current and historic passwords for this user
     */
    public boolean hasPassword(PlainTextCredentials password) {
        return getPasswords().stream()
            .map(PasswordEntity::getPassword)
            .anyMatch(passwordHash -> passwordHash.matches(password));
    }

    public void applyPasswordRetention(int maxHistory) {
        while (getPasswords().size() > maxHistory) {
            log.debug("Password history size is configured to [{}]. Removing oldest password", maxHistory);
            removeOldestPassword();
        }
    }

    /**
     * Removes the oldest password from this users list of passwords. If the user only has one (ie current) password, it will be removed.
     */
    public void removeOldestPassword() {

        PasswordEntity oldestPassword = null;

        for (PasswordEntity password : getPasswords()) {
            if (oldestPassword == null || password.getLastChanged().before(oldestPassword.getLastChanged()))
                oldestPassword = password;
        }

        log.debug("Removing oldest password from user[{}]: {}", getUsername(), oldestPassword);

        if (oldestPassword != null)
            removePassword(oldestPassword);
    }

    public void removePassword(PasswordEntity password) {
        removePasswordReference(password);
        password.setUser(null);
    }

    /**
     * Updates the current password with a new password hash (ie if the algorithm is strengthened)
     */
    public void updatePasswordHash(PBKDF2PasswordHash passwordHash) {
        log.debug("Updating password hash for user [{}]", username);
        var currentPassword = getLatestPassword();
        var updatedPassword = new PasswordEntity(currentPassword, passwordHash);
        addPasswordReference(updatedPassword);
        removePassword(currentPassword);
    }

    /**
     * Adds a new password to this user, and sets up the bi-directional relationship.
     */
    public void setNewPassword(PBKDF2PasswordHash password) {
        log.debug("Setting new password for user#{}: {}", getPk(), password);
        addPasswordReference(new PasswordEntity(this, password));
    }

    /**
     * Adds a new password to this user, and sets up the bi-directional relationship.
     * <br/>
     * <strong>WARNING:</strong> This method allows old hash algorithms to be used - use #setNewPassword where possible
     */
    public void setPreHashedPassword(PasswordHash password) {
        log.debug("Setting pre-hashed password for user#{}: {}", getPk(), password);
        addPasswordReference(new PasswordEntity(this, password));
    }

    void addPasswordReference(PasswordEntity password) {
        passwords.add(password);
    }

    void removePasswordReference(PasswordEntity password) {
        passwords.remove(password);
    }

    /**
     * Removes this user from all GroupEntities, removing the bi-directional reference from the GroupEntity as it goes.
     */
    public void clearGroups() {
        log.debug("Clearing all GroupEntities from user#{}", getPk());
        Set.copyOf(groups).forEach(this::removeGroup);
    }

    /**
     * Adds a GroupEntity to this user, and sets up the bi-directional relationship.
     * <p>
     * As the relationship is based on Sets, calls to this function are idempotent
     */
    public void addGroup(GroupEntity group) {
        log.debug("Adding new GroupEntity to user#{}: {}", getPk(), group);
        addGroupReference(group);
        group.addUserReference(this);
    }

    /**
     * Used internally by the JPA entities to manage this side of the group/user relationship
     * Allows management from both sides of the relationship without introducing cycles
     */
    void addGroupReference(GroupEntity group) {
        groups.add(group);
    }

    /**
     * Removes a GroupEntity from this user, and removes the bi-directional relationship
     */
    public void removeGroup(GroupEntity group) {
        removeGroupReference(group);
        group.removeUserReference(this);
    }

    /**
     * Used internally by the JPA entities to manage this side of the group/user relationship
     * Allows management from both sides of the relationship without introducing cycles
     */
    void removeGroupReference(GroupEntity group) {
        groups.remove(group);
    }

    public void addProperty(UserPropertyEntity property) {
        log.debug("Adding new UserPropertyEntity to user#{}: {}", getPk(), property);
        getProperties().add(property);
    }

    /**
     * Clears all {@link PasswordEntity}s from this user.
     */
    public void clearPasswords() {
        log.debug("Clearing all PasswordEntities from user [{}]", getUsername());
        Set.copyOf(passwords).forEach(this::removePassword);
    }

    /**
     * Clears all {@link UserPropertyEntity}s from this user.
     */
    public void clearProperties() {
        log.debug("Clearing all UserPropertyEntities from user#{}", getPk());
        getProperties().clear();
    }

    /**
     * Determines whether or not the user is the system user
     */
    public boolean isSystem() {
        return UserStatus.SYSTEM == getStatus();
    }

    /**
     * Determines whether or not the user is deleted
     */
    public boolean isDeleted() {
        return UserStatus.DELETED == getStatus();
    }

    /**
     * Determines whether or not the user is disabled
     */
    public boolean isDisabled() {
        return UserStatus.DISABLED == getStatus();
    }

    /**
     * Convenience method - user must not be deleted, disabled, expired, have expired password, or locked out of the system
     */
    public boolean canLogin() {
        boolean notSystem = !isSystem();
        boolean notDeleted = !isDeleted();
        boolean notDisabled = !isDisabled();
        boolean notExpired = !isAccountExpired();
        boolean notLockedOut = !isLockedOut();
        boolean passwordNotExpired = !isPasswordExpired();
        boolean canBypassLockout = isBypassLockout();
        return notSystem && notDeleted && notDisabled && notExpired && passwordNotExpired && (notLockedOut || canBypassLockout);
    }

    public void registerSuccessfulLogin() {
        log.debug("Resetting incorrect login attempts and updating lastLogin for user: ${user.getUsername}");
        resetIncorrectLoginAttempts();
        updateLastLogin();
    }

    public void resetIncorrectLoginAttempts() {
        setIncorrectLoginAttempts(0);
    }

    /**
     * Update the users' last login to the current date/time.
     */
    public void updateLastLogin() {
        setLastLogin(new Date(System.currentTimeMillis()));
    }

    /**
     * Provides a read-only view of the roles this user has. Changes must be made through {@link #addRole(RoleEntity)} and {@link #removeRole(RoleEntity)}
     */
    public Set<RoleEntity> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public void setRoles(Set<RoleEntity> newRoles) {
        updateRelationship(this.roles, newRoles, this::addRole, this::removeRole);
    }

    public void clearRoles() {
        setRoles(Collections.emptySet());
    }

    /**
     * Add this user to the given role, updating both sides of the relationship
     */
    public void addRole(RoleEntity role) {
        addRoleReference(role);
        role.addUserReference(this);
    }

    /**
     * Used internally to manage this side of the user/role relationship
     * Allows management from both sides of the relationship without introducing cycles
     */
    void addRoleReference(RoleEntity role) {
        roles.add(role);
    }

    /**
     * Remove this user from the given role, updating both sides of the relationship
     */
    public void removeRole(RoleEntity role) {
        removeRoleReference(role);
        role.removeUserReference(this);
    }

    /**
     * Used internally to manage this side of the user/role relationship
     * Allows management from both sides of the relationship without introducing cycles
     */
    void removeRoleReference(RoleEntity role) {
        roles.remove(role);
    }

    // convenience method
    public Set<String> getRoleNames() {
        return roles.stream().map(RoleEntity::getName).collect(Collectors.toSet());
    }

    // convenience method
    public boolean hasPermission(final String permissionName) {
        return roles.stream().anyMatch(r -> permissionName.equals(r.getName()));
    }

    public boolean isBypassDocumentSecurity() {
        return bypassDocumentSecurity;
    }

    public void setBypassDocumentSecurity(boolean bypassDocumentSecurity) {
        this.bypassDocumentSecurity = bypassDocumentSecurity;
    }

    /* Simple accessors */

    public boolean isBypassServerLockout() {
        return this.bypassServerLockout;
    }

    public void setBypassServerLockout(boolean bypassServerLockout) {
        this.bypassServerLockout = bypassServerLockout;
    }

    public long getPk() {
        return id;
    }

    public void setPk(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserStatus getStatus() {
        return UserStatus.get(type);
    }

    public void setStatus(UserStatus userStatus) {
        this.type = userStatus.getId();
    }

    public Date getAccountExpiry() {
        return accountExpiry;
    }

    public void setAccountExpiry(Date accountExpiry) {
        if (accountExpiry == null) {
            this.accountExpiry = null;
        } else {
            this.accountExpiry = new Date(accountExpiry.getTime());
        }
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public boolean getPasswordNoExpire() {
        return passwordNoExpire;
    }

    public void setPasswordNoExpire(boolean passwordNoExpire) {
        this.passwordNoExpire = passwordNoExpire;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Set<UserPropertyEntity> getProperties() {
        return userProperties;
    }

    public void setProperties(Set<UserPropertyEntity> properties) {
        clearProperties();
        properties.forEach(this::addProperty);
    }

    public UserPropertyEntity getProperty(String name) {
        Set<UserPropertyEntity> userProperties = getProperties();
        for (UserPropertyEntity userProperty : userProperties) {
            if (userProperty.getPropDef().getName().equalsIgnoreCase(name)) {
                log.debug("Found user property by name: {}", userProperty);
                return userProperty;
            }
        }
        log.debug("Could not find user property '{}'", name);
        return null;
    }

    public void updateProperty(String propertyName, String propertyValue) {
        UserPropertyEntity userProperty = getProperty(propertyName);
        if (userProperty == null)
            throw new ObjectNotFoundException("Could not find user property '" + propertyName + "' against user: " + this);
        userProperty.setValue(propertyValue);
    }

    public Set<PasswordEntity> getPasswords() {
        return Collections.unmodifiableSet(passwords);
    }

    public Set<GroupEntity> getGroups() {
        return groups;
    }

    public void setGroups(Set<GroupEntity> groups) {
        clearGroups();
        groups.forEach(this::addGroup);
    }

    /**
     * Get Group membership, including group parents
     *
     * @return An empty Set if none
     */
    public Set<GroupEntity> getGroupsIncParents() {
        Set<GroupEntity> groupMembership = new HashSet<>(getGroups());
        for (GroupEntity group : getGroups()) {
            groupMembership.addAll(group.getAllParents());
        }
        return groupMembership;
    }

    public boolean isPasswordExpired() {
        log.debug("User[{}]s password has {}expired", getUsername(), (passwordExpired ? "" : "not "));
        return passwordExpired;
    }

    public void setPasswordExpired(Boolean passwordExpired) {
        this.passwordExpired = passwordExpired;
    }

    public Integer getPasswordExpiresInDays() {
        return passwordExpiresInDays;
    }

    public void setPasswordExpiresInDays(Integer passwordExpiresInDays) {
        this.passwordExpiresInDays = passwordExpiresInDays;
    }

    public boolean isLockedOut() {
        return isLockedOut;
    }

    public void setLockedOut(boolean isLockedOut) {
        this.isLockedOut = isLockedOut;
    }

    public int getIncorrectLoginAttempts() {
        return incorrectLoginAttempts;
    }

    public void setIncorrectLoginAttempts(int incorrectLoginAttempts) {
        this.incorrectLoginAttempts = incorrectLoginAttempts;
    }

    public void incrementLoginAttempts() {
        incorrectLoginAttempts++;
    }

    public boolean isBypassLockout() {
        return bypassLockout;
    }

    public void setBypassLockout(boolean bypassLockout) {
        this.bypassLockout = bypassLockout;
    }

    public boolean isChangePasswordNextLogin() {
        return changePasswordNextLogin;
    }

    public void setChangePasswordNextLogin(boolean changePasswordNextLogin) {
        this.changePasswordNextLogin = changePasswordNextLogin;
    }

    public boolean isExternalIdentity() {
        return isExternalIdentity;
    }

    public UserEntity setExternalIdentity(boolean externalIdentity) {
        isExternalIdentity = externalIdentity;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof UserEntity))
            return false;

        UserEntity user = (UserEntity) o;

        // we need a static equals/hashcode implementation as these objects are stored in (Hash)Sets
        // changing the hashcode of an object while it is in a HashSet leads to unpredictable results
        if (id != 0 && user.id != 0)
            return id == user.id;

        if (bypassLockout != user.bypassLockout)
            return false;
        if (id != user.id)
            return false;
        if (incorrectLoginAttempts != user.incorrectLoginAttempts)
            return false;
        if (passwordNoExpire != user.passwordNoExpire)
            return false;
        if (type != user.type)
            return false;
        if (!Objects.equals(accountExpiry, user.accountExpiry))
            return false;
        if (!Objects.equals(emailAddress, user.emailAddress))
            return false;
        if (!Objects.equals(lastLogin, user.lastLogin))
            return false;
        if (!Objects.equals(username, user.username))
            return false;
        if (changePasswordNextLogin != user.changePasswordNextLogin)
            return false;
        if (isExternalIdentity != user.isExternalIdentity)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        // we need a static equals/hashcode implementation as these objects are stored in (Hash)Sets
        // changing the hashcode of an object while it is in a HashSet leads to unpredictable results
        if (id != 0)
            return Objects.hashCode(id);

        int result;
        result = (int) (id ^ (id >>> 32));
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + type;
        result = 31 * result + (accountExpiry != null ? accountExpiry.hashCode() : 0);
        result = 31 * result + (emailAddress != null ? emailAddress.hashCode() : 0);
        result = 31 * result + (passwordNoExpire ? 1 : 0);
        result = 31 * result + (lastLogin != null ? lastLogin.hashCode() : 0);
        result = 31 * result + incorrectLoginAttempts;
        result = 31 * result + (bypassLockout ? 1 : 0);
        result = 31 * result + (changePasswordNextLogin ? 1 : 0);
        result = 31 * result + (isExternalIdentity ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserEntity{" +
            "id=" + id +
            ", username='" + username + '\'' +
            ", type=" + type +
            ", accountExpiry=" + accountExpiry +
            ", emailAddress='" + emailAddress + '\'' +
            ", passwordNoExpire=" + passwordNoExpire +
            ", lastLogin=" + lastLogin +
            ", incorrectLoginAttempts=" + incorrectLoginAttempts +
            ", bypassLockout=" + bypassLockout +
            ", changePasswordNextLogin=" + changePasswordNextLogin +
            ", passwordExpired=" + passwordExpired +
            ", passwordExpiresInDays=" + passwordExpiresInDays +
            ", isLockedOut=" + isLockedOut +
            ", isExternalIdentity=" + isExternalIdentity +
            '}';
    }

}
