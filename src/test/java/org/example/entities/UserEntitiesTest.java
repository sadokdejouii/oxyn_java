package org.example.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite unitaire des entités utilisateur : {@link User} (via sous-classe de test),
 * {@link Admin}, {@link Client}, {@link Coach} (encadrant).
 */
@DisplayName("Entités utilisateur (User, Admin, Client, Encadrant)")
class UserEntitiesTest {

    /** Double minimal pour exercer le constructeur / champs protégés de {@link User}. */
    private static final class TestUser extends User {
        TestUser() {
            super();
        }

        TestUser(int id, String email, String password, String nom, String prenom, String telephone, boolean active) {
            super(id, email, password, nom, prenom, telephone, active);
        }

        @Override
        public String getRolesJsonValue() {
            return "[]";
        }
    }

    @Nested
    @DisplayName("User (classe abstraite — comportement via TestUser)")
    class UserBaseBehavior {

        @Test
        @DisplayName("constructeur remplit id, email, mot de passe, nom, prénom, téléphone et statut actif")
        void constructor_populates_all_fields() {
            TestUser u = new TestUser(42, "a@b.co", "hash", "Dupont", "Jean", "+21612345678", true);
            assertEquals(42, u.getId());
            assertEquals("a@b.co", u.getEmail());
            assertEquals("hash", u.getPassword());
            assertEquals("Dupont", u.getNom());
            assertEquals("Jean", u.getPrenom());
            assertEquals("+21612345678", u.getTelephone());
            assertTrue(u.isActive());
        }

        @Test
        @DisplayName("setters mettent à jour les champs")
        void setters_update_state() {
            TestUser u = new TestUser();
            u.setId(7);
            u.setEmail("x@y.z");
            u.setPassword("p");
            u.setNom("N");
            u.setPrenom("P");
            u.setTelephone("0612345678");
            u.setActive(false);
            assertEquals(7, u.getId());
            assertEquals("x@y.z", u.getEmail());
            assertEquals("p", u.getPassword());
            assertEquals("N", u.getNom());
            assertEquals("P", u.getPrenom());
            assertEquals("0612345678", u.getTelephone());
            assertFalse(u.isActive());
        }

        @Test
        @DisplayName("getFullName concatène nom et prénom avec un espace")
        void getFullName_joins_nom_and_prenom() {
            TestUser u = new TestUser(1, "e@e.e", "h", "Martin", "Claire", "0600", true);
            assertEquals("Martin Claire", u.getFullName().trim());
        }

        @Test
        @DisplayName("getFullName gère nom ou prénom null")
        void getFullName_handles_null_parts() {
            TestUser onlyPrenom = new TestUser(1, "e@e.e", "h", null, "Solo", "1", true);
            assertEquals(" Solo", onlyPrenom.getFullName());

            TestUser onlyNom = new TestUser(1, "e@e.e", "h", "NomSeul", null, "1", true);
            assertEquals("NomSeul ", onlyNom.getFullName());
        }

        @Test
        @DisplayName("equals / hashCode basés uniquement sur l’identifiant")
        void equals_and_hashCode_use_id_only() {
            TestUser a = new TestUser(10, "a@a.a", "h1", "A", "B", "1", true);
            TestUser b = new TestUser(10, "different@x.x", "h2", "C", "D", "2", false);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());

            TestUser c = new TestUser(11, "a@a.a", "h1", "A", "B", "1", true);
            assertNotEquals(a, c);
        }

        @Test
        @DisplayName("equals refuse null et types non-User")
        void equals_rejects_null_and_non_user() {
            TestUser u = new TestUser(1, "e@e.e", "h", "N", "P", "1", true);
            assertNotEquals(null, u);
            assertNotEquals(u, "not a user");
        }

        @Test
        @DisplayName("toString contient le nom simple de la classe et les champs principaux")
        void toString_contains_key_fields() {
            TestUser u = new TestUser(5, "t@t.t", "pw", "N", "P", "0999", false);
            String s = u.toString();
            assertTrue(s.contains("TestUser"));
            assertTrue(s.contains("id=5"));
            assertTrue(s.contains("email='t@t.t'"));
            assertTrue(s.contains("isActive=false"));
        }
    }

    @Nested
    @DisplayName("Admin")
    class AdminBehavior {

        @Test
        @DisplayName("getRolesJsonValue retourne ROLE_ADMIN")
        void roles_json_is_admin() {
            Admin admin = new Admin(1, "admin@oxyn.test", "hash", "Root", "Super", "+33123456789", true);
            assertEquals("[\"ROLE_ADMIN\"]", admin.getRolesJsonValue());
        }

        @Test
        @DisplayName("constructeur paramétré et accesseurs alignés sur User")
        void full_constructor() {
            Admin a = new Admin(2, "a@b.c", "p", "Nom", "Prénom", "0611223344", false);
            assertEquals(2, a.getId());
            assertEquals("a@b.c", a.getEmail());
            assertFalse(a.isActive());
        }

        @Test
        @DisplayName("constructeur vide + setters permettent un flux type formulaire")
        void no_arg_constructor_and_setters() {
            Admin a = new Admin();
            a.setId(99);
            a.setEmail("adm@test.fr");
            a.setPassword("secret");
            a.setNom("Admin");
            a.setPrenom("Sys");
            a.setTelephone("+21698765432");
            a.setActive(true);
            assertEquals(99, a.getId());
            assertEquals("[\"ROLE_ADMIN\"]", a.getRolesJsonValue());
            assertNotNull(a.toString());
        }
    }

    @Nested
    @DisplayName("Client")
    class ClientBehavior {

        @Test
        @DisplayName("getRolesJsonValue retourne ROLE_CLIENT")
        void roles_json_is_client() {
            Client c = new Client(3, "cli@oxyn.test", "h", "Durand", "Alice", "0622334455", true);
            assertEquals("[\"ROLE_CLIENT\"]", c.getRolesJsonValue());
        }

        @Test
        @DisplayName("hérite du comportement User (égalité par id)")
        void equality_same_as_user() {
            Client c1 = new Client(4, "a@a.a", "x", "N", "P", "1", true);
            Client c2 = new Client(4, "b@b.b", "y", "Q", "R", "2", false);
            assertEquals(c1, c2);
        }
    }

    @Nested
    @DisplayName("Encadrant (Coach)")
    class CoachEncadrantBehavior {

        @Test
        @DisplayName("getRolesJsonValue retourne ROLE_ENCADRANT")
        void roles_json_is_encadrant() {
            Coach coach = new Coach(5, "coach@oxyn.test", "h", "Bernard", "Marc", "+33611223344", true);
            assertEquals("[\"ROLE_ENCADRANT\"]", coach.getRolesJsonValue());
        }

        @Test
        @DisplayName("distinct de Client et Admin au même id si comparaison référentielle type")
        void concrete_type_distinct() {
            User coach = new Coach(6, "c@c.c", "h", "N", "P", "1", true);
            assertTrue(coach instanceof User);
            assertFalse(coach instanceof Admin);
            assertFalse(coach instanceof Client);
        }
    }

    @Nested
    @DisplayName("Polymorphisme et rôles")
    class PolymorphismAndRoles {

        @Test
        @DisplayName("trois sous-types exposent des getRolesJsonValue différents")
        void each_subtype_has_distinct_roles_json() {
            User admin = new Admin(1, "a@a.a", "h", "A", "B", "1", true);
            User client = new Client(2, "c@c.c", "h", "C", "D", "2", true);
            User coach = new Coach(3, "e@e.e", "h", "E", "F", "3", true);

            assertEquals("[\"ROLE_ADMIN\"]", admin.getRolesJsonValue());
            assertEquals("[\"ROLE_CLIENT\"]", client.getRolesJsonValue());
            assertEquals("[\"ROLE_ENCADRANT\"]", coach.getRolesJsonValue());
        }

        @Test
        @DisplayName("égalité par id : deux sous-types différents avec le même id sont égaux (contrat User)")
        void cross_subtype_equality_same_id() {
            User admin = new Admin(100, "a@a.a", "h", "A", "B", "1", true);
            User client = new Client(100, "c@c.c", "h", "C", "D", "2", false);
            assertEquals(admin, client);
            assertEquals(admin.hashCode(), client.hashCode());
        }
    }
}
