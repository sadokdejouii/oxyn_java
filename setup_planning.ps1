# ═══════════════════════════════════════════════════════════════
#   OXYN - Script PowerShell pour créer les données de planning
#   ═══════════════════════════════════════════════════════════════

Write-Host "🎯 OXYN - Création des données de planning" -ForegroundColor Cyan
Write-Host "══════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# Vérifier si MySQL est disponible
try {
    $mysqlVersion = mysql --version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ MySQL trouvé: $mysqlVersion" -ForegroundColor Green
    } else {
        Write-Host "❌ MySQL non trouvé. Veuillez installer MySQL ou l'ajouter au PATH." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Erreur: MySQL non disponible" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Demander les identifiants MySQL
$dbUser = Read-Host "👤 Utilisateur MySQL (Enter pour 'root')"
if ([string]::IsNullOrEmpty($dbUser)) {
    $dbUser = "root"
}

$dbPassword = Read-Host "🔑 Mot de passe MySQL (Enter si vide)"
if ([string]::IsNullOrEmpty($dbPassword)) {
    $dbPassword = ""
}

Write-Host ""
Write-Host "🚀 Création des données de planning pour California Gymm..." -ForegroundColor Yellow
Write-Host ""

# Lire le fichier SQL
$sqlFile = "quick_setup.sql"
if (-not (Test-Path $sqlFile)) {
    Write-Host "❌ Fichier SQL non trouvé: $sqlFile" -ForegroundColor Red
    exit 1
}

try {
    # Lire le contenu SQL
    $sqlContent = Get-Content $sqlFile -Raw
    
    # Exécuter le SQL
    if ($dbPassword -eq "") {
        $result = mysql -u $dbUser oxyn -e $sqlContent
    } else {
        $result = mysql -u $dbUser -p$dbPassword oxyn -e $sqlContent
    }
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Données de planning créées avec succès !" -ForegroundColor Green
        Write-Host ""
        
        # Afficher le résumé des sessions créées
        Write-Host "📊 Résumé des sessions créées:" -ForegroundColor Cyan
        Write-Host ""
        
        $query = "SELECT title, DATE_FORMAT(start_at, '%W %d/%m %H:%i') as jour_heure, TIME_FORMAT(start_at, '%H:%i') as debut, TIME_FORMAT(end_at, '%H:%i') as fin, capacity, price FROM training_sessions WHERE gymnasium_id = 1 AND is_active = 1 ORDER BY start_at;"
        
        if ($dbPassword -eq "") {
            mysql -u $dbUser oxyn -e $query
        } else {
            mysql -u $dbUser -p$dbPassword oxyn -e $query
        }
        
        Write-Host ""
        Write-Host "🎯 Planning disponible dans l'application !" -ForegroundColor Green
        Write-Host "   Lancez l'application et cliquez sur '📅 Voir le planning'" -ForegroundColor White
        Write-Host ""
        
    } else {
        Write-Host "❌ Erreur lors de la création des données" -ForegroundColor Red
        Write-Host "💡 Vérifiez:" -ForegroundColor Yellow
        Write-Host "   - La base de données 'oxyn' existe" -ForegroundColor White
        Write-Host "   - Les identifiants MySQL sont corrects" -ForegroundColor White
        Write-Host "   - Les tables training_sessions et gymnasia existent" -ForegroundColor White
        Write-Host ""
    }
    
} catch {
    Write-Host "❌ Erreur: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Appuyez sur une touche pour continuer..." -ForegroundColor Cyan
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
