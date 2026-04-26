package org.example.services;

import org.example.entities.AvisEvenement;
import org.example.entities.InscriptionEvenement;
import org.example.utils.MyDataBase;
import org.example.utils.SqlDateReaders;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AvisEvenementServices implements ICrud<AvisEvenement> {

    Connection con;
    private final InscriptionEvenementServices inscriptionEvenementServices;

    public AvisEvenementServices() {
        con = MyDataBase.getInstance().getConnection();
        inscriptionEvenementServices = new InscriptionEvenementServices();
    }

    @Override
    public void ajouter(AvisEvenement a) throws SQLException {
        validateUserRegisteredForEvent(a);
        sanitizeAvisComment(a);

        String sql = "INSERT INTO avis_evenement (" +
                "note_avis_evenement, " +
                "commentaire_avis_evenement, " +
                "created_at_avis_evenement, " +
                "id_evenement_avis_evenement_id, " +
                "id_user_avis_evenement_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, a.getNote());
        ps.setString(2, a.getCommentaire());
        ps.setTimestamp(3, new Timestamp(a.getCreatedAt().getTime()));
        ps.setInt(4, a.getIdEvenement());
        ps.setInt(5, a.getIdUser());

        ps.executeUpdate();
        System.out.println("Avis ajouté avec succès !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM avis_evenement WHERE id_note_avis_evenement = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();

        System.out.println("Avis supprimé avec succès !");
    }

    @Override
    public List<AvisEvenement> afficher() throws SQLException {
        List<AvisEvenement> avisList = new ArrayList<>();

        String sql = "SELECT * FROM avis_evenement";
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            AvisEvenement a = new AvisEvenement();
            a.setId(rs.getInt("id_note_avis_evenement"));
            a.setNote(rs.getInt("note_avis_evenement"));
            a.setCommentaire(rs.getString("commentaire_avis_evenement"));
            a.setCreatedAt(SqlDateReaders.readTimestampOrNull(rs, "created_at_avis_evenement"));
            a.setIdEvenement(rs.getInt("id_evenement_avis_evenement_id"));
            a.setIdUser(rs.getInt("id_user_avis_evenement_id"));

            avisList.add(a);
        }

        return avisList;
    }

    @Override
    public void modifier(int id) throws SQLException {
        // overload kept for interface compatibility
    }

    public void modifier(AvisEvenement a) throws SQLException {
        validateUserRegisteredForEvent(a);
        sanitizeAvisComment(a);

        String sql = "UPDATE avis_evenement SET " +
                "note_avis_evenement = ?, " +
                "commentaire_avis_evenement = ?, " +
                "created_at_avis_evenement = ?, " +
                "id_evenement_avis_evenement_id = ?, " +
                "id_user_avis_evenement_id = ? " +
                "WHERE id_note_avis_evenement = ?";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, a.getNote());
        ps.setString(2, a.getCommentaire());
        ps.setTimestamp(3, new Timestamp(a.getCreatedAt().getTime()));
        ps.setInt(4, a.getIdEvenement());
        ps.setInt(5, a.getIdUser());
        ps.setInt(6, a.getId());
        ps.executeUpdate();

        System.out.println("Avis modifié avec succès !");
    }

    private void sanitizeAvisComment(AvisEvenement avis) {
        if (avis == null) {
            throw new IllegalArgumentException("L'avis ne peut pas etre null.");
        }

        String normalizedComment = AvisAiModerationService.normalizeAcceptedComment(avis.getCommentaire());
        AvisAiModerationService.validateCommentOrThrow(normalizedComment);
        avis.setCommentaire(normalizedComment);
    }

    private void validateUserRegisteredForEvent(AvisEvenement avis) throws SQLException {
        if (avis == null) {
            throw new IllegalArgumentException("L'avis ne peut pas etre null.");
        }

        boolean hasRegistration = inscriptionEvenementServices.afficher().stream()
                .anyMatch(inscription -> isMatchingRegistration(inscription, avis));

        if (hasRegistration) {
            return;
        }

        throw new SQLException("Vous devez être inscrit à cet événement avant de publier un avis.");
    }

    private boolean isMatchingRegistration(InscriptionEvenement inscription, AvisEvenement avis) {
        return inscription != null
                && inscription.getIdEvenement() == avis.getIdEvenement()
                && inscription.getIdUser() == avis.getIdUser();
    }
}