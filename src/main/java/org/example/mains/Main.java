package org.example.mains;

import org.example.services.EvenementServices;
import org.example.services.InscriptionEvenementServices;
import org.example.services.AvisEvenementServices;

public class Main {
    public static void main(String[] args) {

        EvenementServices es = new EvenementServices();

        /*try {
            Evenement e = new Evenement(
                    "Forum Java",
                    "Evenement pour les développeurs Java",
                    Timestamp.valueOf("2026-04-10 09:00:00"),
                    Timestamp.valueOf("2026-04-10 17:00:00"),
                    "Palais des congres",
                    "Tunis",
                    200,
                    "ACTIF",
                    Timestamp.valueOf("2026-04-04 08:00:00"),
                    1
            );
            es.ajouter(e);
            //System.out.println(es.afficher());
            //es.supprimer(14);
            //es.modifier(13);

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }*/

        InscriptionEvenementServices is = new InscriptionEvenementServices();

        /*try {
            InscriptionEvenement i = new InscriptionEvenement(
                    Timestamp.valueOf("2026-04-04 14:00:00"),
                    "en attente",
                    1,
                    1
            );
            is.ajouter(i);
            System.out.println(is.afficher());
            is.supprimer(13);
            is.modifier(11);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }*/

        AvisEvenementServices as = new AvisEvenementServices();

        /*try {
            AvisEvenement a = new AvisEvenement(
                    4,
                    "Très bon événement",
                    Timestamp.valueOf("2026-04-04 15:00:00"),
                    1,
                    1
            );
            as.ajouter(a);
            System.out.println(as.afficher());
            as.supprimer(7);
            as.modifier(5);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }*/
    }
}