@echo off
echo ═══════════════════════════════════════════════════════════════
echo    OXYN - Installation des données de planning
echo ═══════════════════════════════════════════════════════════════
echo.

echo 📋 Ce script va créer des sessions de planning pour vos salles
echo    avec des données réalistes et variées.
echo.

echo 🔍 Vérification de la connexion MySQL...
mysql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ MySQL n'est pas installé ou pas dans le PATH
    echo 💡 Veuillez installer MySQL ou ajouter au PATH
    pause
    exit /b 1
)

echo ✅ MySQL trouvé
echo.

echo 📝 Configuration de la base de données:
echo    - Base: oxyn
echo    - Utilisateur: root (par défaut)
echo    - Mot de passe: (sera demandé)
echo.

set /p db_user="👤 Utilisateur MySQL (root): "
if "%db_user%"=="" set db_user=root

set /p db_password="🔑 Mot de passe MySQL: "

echo.
echo 🚀 Exécution du script de création des données...
echo.

mysql -u %db_user% -p%db_password% oxyn < create_planning_data.sql

if %errorlevel% equ 0 (
    echo.
    echo ✅ Données de planning créées avec succès !
    echo.
    echo 📊 Résumé des sessions créées:
    mysql -u %db_user% -p%db_password% oxyn -e "
        SELECT 
            g.name as salle_nom,
            COUNT(*) as nombre_sessions,
            MIN(ts.price) as prix_min,
            MAX(ts.price) as prix_max
        FROM training_sessions ts
        JOIN gymnasia g ON ts.gymnasium_id = g.id
        WHERE ts.gymnasium_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        GROUP BY g.name
        ORDER BY g.name;
    "
    echo.
    echo 🎯 Planning disponible dans l'application !
    echo    Lancez l'application et cliquez sur "📅 Voir le planning"
    echo.
) else (
    echo.
    echo ❌ Erreur lors de la création des données
    echo 💡 Vérifiez:
    echo    - La base de données "oxyn" existe
    echo    - Les identifiants MySQL sont corrects
    echo    - Les tables training_sessions et gymnasia existent
    echo.
)

pause
