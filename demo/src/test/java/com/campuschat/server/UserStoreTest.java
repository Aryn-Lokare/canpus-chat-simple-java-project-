package com.campuschat.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserStoreTest {

    @AfterEach
    void cleanUp() {
        UserStore.removeUser("david");
        UserStore.removeUser("eve");
        UserStore.setSimulateDatabaseError(false);
    }

    @Test
    void knownUserExists() {
        assertTrue(UserStore.exists("alice"));
        assertTrue(UserStore.exists("  ALICE "));      // trim + lower-case
    }

    @Test
    void unknownUserDoesNotExist() {
        assertFalse(UserStore.exists("zed"));
        assertFalse(UserStore.exists(null));
    }

    @Test
    void addUserWorks() {
        UserStore.addUser("david");
        assertTrue(UserStore.exists("david"));
    }

    @Test
    void addingSameUserTwiceGivesOneEntry() {
        UserStore.addUser("eve");
        UserStore.addUser("Eve");
        long count = UserStore.allUsernames().stream()
                .filter(n -> n.equals("eve")).count();
        assertEquals(1, count);
    }

    @Test
    void databaseErrorIsThrown() {
        UserStore.setSimulateDatabaseError(true);
        assertThrows(RuntimeException.class, () -> UserStore.exists("alice"));
    }
}
