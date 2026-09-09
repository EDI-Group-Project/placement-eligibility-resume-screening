param(
    [string]$MySqlUser = "root",
    [string]$MySqlHost = "localhost",
    [int]$MySqlPort = 3306
)

$ErrorActionPreference = "Stop"

Write-Host "== Placement Portal database setup =="

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysql) {
    throw "mysql.exe was not found on PATH. Install MySQL 8.x and add its bin folder to PATH."
}

Write-Host "Enter the MySQL password for user '$MySqlUser'."
cmd /c "mysql -h $MySqlHost -P $MySqlPort -u $MySqlUser -p < sql\01_schema.sql"
if ($LASTEXITCODE -ne 0) { throw "Schema creation failed." }

cmd /c "mysql -h $MySqlHost -P $MySqlPort -u $MySqlUser -p < sql\02_seed.sql"
if ($LASTEXITCODE -ne 0) { throw "Seed data loading failed." }

Write-Host "Database setup completed."
