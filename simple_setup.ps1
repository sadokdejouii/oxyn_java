# Script simple pour créer les données de planning
Write-Host "🎯 OXYN - Création des données de planning" -ForegroundColor Cyan

# Configuration MySQL
$dbUser = "root"
$dbPassword = ""

# Lire et exécuter le SQL
$sqlContent = Get-Content "quick_setup.sql" -Raw

Write-Host "🚀 Exécution du script SQL..." -ForegroundColor Yellow

# Exécuter avec MySQL
if ($dbPassword -eq "") {
    mysql -u $dbUser oxyn -e $sqlContent
} else {
    mysql -u $dbUser -p$dbPassword oxyn -e $sqlContent
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Données créées avec succès !" -ForegroundColor Green
    
    # Afficher les sessions
    Write-Host "📊 Sessions créées:" -ForegroundColor Cyan
    mysql -u $dbUser oxyn -e "SELECT title, DATE_FORMAT(start_at, '%W %d/%m %H:%i') as jour_heure, capacity, price FROM training_sessions WHERE gymnasium_id = 1 AND is_active = 1 ORDER BY start_at;"
    
    Write-Host "🎯 Lancez l'application et testez le planning !" -ForegroundColor Green
} else {
    Write-Host "❌ Erreur lors de la création" -ForegroundColor Red
}

Write-Host "Appuyez sur Entrée pour continuer..."
Read-Host
