package org.example.dao;



import org.example.entities.FicheSanteRow;

import org.example.model.planning.FicheSanteFormData;

import org.example.repository.planning.FicheSanteRepository;



import java.sql.SQLException;

import java.util.Optional;



/**

 * JDBC — table {@code fiche_sante} (délégation vers {@link FicheSanteRepository}).

 */

public final class FicheSanteDao {



    private final FicheSanteRepository repository = new FicheSanteRepository();



    public Optional<FicheSanteRow> findByUserId(int userId) throws SQLException {

        return repository.findByUserId(userId);

    }



    public int insert(int userId, FicheSanteFormData d) throws SQLException {

        return repository.insert(userId, d);

    }



    public void update(int ficheId, int userId, FicheSanteFormData d) throws SQLException {

        repository.update(ficheId, userId, d);

    }

}

