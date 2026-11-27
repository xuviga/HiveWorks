# Generates the HiveWorks resource pack (textures, models, item definitions) and zips it.
# Requires Windows PowerShell 5.1+ with System.Drawing (built in).

param(
    [string]$Output = "$PSScriptRoot\..\src\main\resources\pack\hiveworks-pack.zip"
)

Add-Type -AssemblyName System.Drawing

$build = Join-Path $env:TEMP "hiveworks-pack-build"
if (Test-Path $build) { Remove-Item -Recurse -Force $build }
$assets = Join-Path $build "assets\hiveworks"
$textures = Join-Path $assets "textures\item"
$models = Join-Path $assets "models\item"
$items = Join-Path $assets "items"
$legacyDir = Join-Path $build "assets\minecraft\models\item"
New-Item -ItemType Directory -Force -Path $textures, $models, $items, $legacyDir | Out-Null

function Rgb([int]$r, [int]$g, [int]$b, [int]$a = 255) {
    return [System.Drawing.Color]::FromArgb($a, $r, $g, $b)
}

function Fill($g, $color, [int]$x, [int]$y, [int]$w, [int]$h) {
    $brush = New-Object System.Drawing.SolidBrush($color)
    $g.FillRectangle($brush, $x, $y, $w, $h)
    $brush.Dispose()
}

function Rect($g, $color, [int]$x, [int]$y, [int]$w, [int]$h) {
    $pen = New-Object System.Drawing.Pen($color, 1)
    $g.DrawRectangle($pen, $x, $y, $w, $h)
    $pen.Dispose()
}

function Oval($g, $color, [int]$x, [int]$y, [int]$w, [int]$h) {
    $brush = New-Object System.Drawing.SolidBrush($color)
    $g.FillEllipse($brush, $x, $y, $w, $h)
    $brush.Dispose()
}

function Save-Png([string]$name, [scriptblock]$draw) {
    $bitmap = New-Object System.Drawing.Bitmap 32, 32
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $graphics.Clear([System.Drawing.Color]::Transparent)
    & $draw $graphics
    $bitmap.Save((Join-Path $textures "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $bitmap.Dispose()
}

function Write-Json([string]$path, $object) {
    $json = $object | ConvertTo-Json -Depth 20
    [System.IO.File]::WriteAllText($path, $json, (New-Object System.Text.UTF8Encoding($false)))
}

function New-ItemModel([string]$name) {
    Write-Json (Join-Path $models "$name.json") @{
        parent = "item/generated"
        textures = @{ layer0 = "hiveworks:item/$name" }
    }
    Write-Json (Join-Path $items "$name.json") @{
        model = @{ type = "minecraft:model"; model = "hiveworks:item/$name" }
    }
}

function New-HiveModel([string]$name) {
    $faces = @{
        down = @{ texture = "#bottom" }
        up = @{ texture = "#top" }
        north = @{ texture = "#front" }
        south = @{ texture = "#side" }
        west = @{ texture = "#side" }
        east = @{ texture = "#side" }
    }
    Write-Json (Join-Path $models "$name.json") @{
        parent = "block/block"
        textures = @{
            particle = "hiveworks:item/${name}_side"
            side = "hiveworks:item/${name}_side"
            front = "hiveworks:item/${name}_front"
            top = "hiveworks:item/${name}_top"
            bottom = "hiveworks:item/${name}_bottom"
        }
        elements = @(
            @{ from = @(0, 0, 0); to = @(16, 16, 16); faces = $faces }
        )
        display = @{
            gui = @{ rotation = @(30, 225, 0); translation = @(0, 0, 0); scale = @(0.625, 0.625, 0.625) }
            "none" = @{ rotation = @(0, 0, 0); translation = @(0, 0, 0); scale = @(1.03, 1.03, 1.03) }
            fixed = @{ rotation = @(0, 0, 0); translation = @(0, 0, 0); scale = @(1.03, 1.03, 1.03) }
        }
    }
    Write-Json (Join-Path $items "$name.json") @{
        model = @{ type = "minecraft:model"; model = "hiveworks:item/$name" }
    }
}

function Write-Legacy([string]$material, [string]$parent, $entries) {
    $overrides = @()
    foreach ($entry in $entries) {
        $overrides += @{
            predicate = @{ custom_model_data = $entry.cmd }
            model = "hiveworks:item/$($entry.model)"
        }
    }
    Write-Json (Join-Path $legacyDir "$material.json") @{
        parent = $parent
        textures = @{ layer0 = "minecraft:item/$material" }
        overrides = $overrides
    }
}

# ---------------------------------------------------------------- jars

$bees = @(
    @{ id = "forest";  r = 90;  g = 140; b = 70;  cmd = 1001 },
    @{ id = "meadow";  r = 215; g = 195; b = 75;  cmd = 1002 },
    @{ id = "taiga";   r = 65;  g = 110; b = 85;  cmd = 1003 },
    @{ id = "swamp";   r = 85;  g = 120; b = 75;  cmd = 1004 },
    @{ id = "jungle";  r = 55;  g = 155; b = 75;  cmd = 1005 },
    @{ id = "savanna"; r = 190; g = 150; b = 80;  cmd = 1006 },
    @{ id = "desert";  r = 215; g = 180; b = 100; cmd = 1007 },
    @{ id = "snowy";   r = 160; g = 210; b = 230; cmd = 1008 },
    @{ id = "mushroom";r = 180; g = 115; b = 160; cmd = 1009 },
    @{ id = "flower";  r = 230; g = 120; b = 180; cmd = 1010 },
    @{ id = "amethyst";r = 150; g = 90;  b = 205; cmd = 1011 },
    @{ id = "nether";  r = 175; g = 60;  b = 60;  cmd = 1012 },
    @{ id = "pale";    r = 180; g = 180; b = 180; cmd = 1013 }
)
$legacyGlass = @()
foreach ($bee in $bees) {
    $beeColor = Rgb $bee.r $bee.g $bee.b
    $name = "bee_jar_$($bee.id)"
    Save-Png $name {
        param($g)
        Fill $g (Rgb 195 220 235 180) 9 8 14 18
        Rect $g (Rgb 240 250 255 230) 8 7 16 20
        Fill $g (Rgb 120 85 50) 10 4 12 4
        Fill $g (Rgb 80 55 30) 10 8 12 1
        Fill $g (Rgb 240 250 255 210) 9 9 3 16
        Fill $g $beeColor 13 15 7 8
        Fill $g (Rgb 30 30 30) 13 17 7 2
        Fill $g (Rgb 30 30 30) 13 20 7 2
        Fill $g (Rgb 245 245 255 200) 10 13 4 3
        Fill $g (Rgb 245 245 255 200) 18 13 4 3
        Fill $g (Rgb 255 255 255 140) 11 10 2 12
    }
    New-ItemModel $name
    $legacyGlass += @{ cmd = $bee.cmd; model = $name }
}

# ---------------------------------------------------------------- honey

$honeys = @(
    @{ id = "ordinary";  r = 205; g = 150; b = 50;  cmd = 2000 },
    @{ id = "good";      r = 140; g = 200; b = 80;  cmd = 2001 },
    @{ id = "elite";     r = 80;  g = 200; b = 220; cmd = 2002 },
    @{ id = "legendary"; r = 240; g = 190; b = 60;  cmd = 2003 }
)
$legacyHoney = @()
foreach ($honey in $honeys) {
    $fillColor = Rgb $honey.r $honey.g $honey.b
    $name = "honey_$($honey.id)"
    Save-Png $name {
        param($g)
        Fill $g (Rgb 230 240 245 200) 11 9 10 17
        Rect $g (Rgb 250 255 255 220) 10 8 12 19
        Fill $g (Rgb 140 100 60) 12 5 8 3
        Fill $g $fillColor 12 15 8 10
        Fill $g (Rgb 255 255 255 150) 12 10 2 13
        Fill $g (Rgb 255 255 255 220) 14 18 3 3
    }
    New-ItemModel $name
    $legacyHoney += @{ cmd = $honey.cmd; model = $name }
}

# ---------------------------------------------------------------- mead

$meads = @(
    @{ id = "meadow";  cmd = 2111; r = 235; g = 205; b = 90 },
    @{ id = "forest";  cmd = 2112; r = 110; g = 175; b = 80 },
    @{ id = "elite";   cmd = 2113; r = 245; g = 180; b = 55 }
)
$legacyMead = @()
foreach ($mead in $meads) {
    $fillColor = Rgb $mead.r $mead.g $mead.b
    $name = "mead_$($mead.id)"
    Save-Png $name {
        param($g)
        Fill $g (Rgb 230 240 245 200) 11 9 10 17
        Rect $g (Rgb 250 255 255 220) 10 8 12 19
        Fill $g (Rgb 120 85 50) 12 5 8 3
        Fill $g $fillColor 12 15 8 10
        Fill $g (Rgb 255 255 255 150) 12 10 2 13
        Fill $g (Rgb 255 240 170) 14 18 3 3
        Fill $g (Rgb 255 255 255 120) 13 20 5 1
    }
    New-ItemModel $name
    $legacyMead += @{ cmd = $mead.cmd; model = $name }
}

# ---------------------------------------------------------------- frames

$frames = @(
    @{ id = "wax";      cmd = 1101; r = 230; g = 190; b = 80 },
    @{ id = "honey";    cmd = 1102; r = 210; g = 150; b = 50 },
    @{ id = "royal";    cmd = 1103; r = 180; g = 120; b = 220 },
    @{ id = "propolis"; cmd = 1104; r = 90;  g = 130; b = 80 },
    @{ id = "amethyst"; cmd = 1105; r = 150; g = 90;  b = 205 }
)
$legacyComb = @()
foreach ($frame in $frames) {
    $inner = Rgb $frame.r $frame.g $frame.b
    $name = "frame_$($frame.id)"
    Save-Png $name {
        param($g)
        Fill $g (Rgb 120 80 45) 4 4 24 24
        Fill $g (Rgb 165 115 65) 6 6 20 20
        Fill $g $inner 9 9 14 14
        for ($i = 0; $i -lt 14; $i += 3) {
            Fill $g (Rgb 245 240 220 110) (9 + $i) 9 1 14
            Fill $g (Rgb 245 240 220 110) 9 (9 + $i) 14 1
        }
        Fill $g (Rgb 80 50 25) 4 4 24 1
    }
    New-ItemModel $name
    $legacyComb += @{ cmd = $frame.cmd; model = $name }
}

# ---------------------------------------------------------------- products

Save-Png "product_comb" {
    param($g)
    Fill $g (Rgb 240 190 70) 7 7 18 18
    for ($y = 8; $y -le 22; $y += 6) {
        for ($x = 8; $x -le 22; $x += 6) {
            Fill $g (Rgb 200 140 40) $x $y 4 4
            Fill $g (Rgb 255 220 120) ($x + 1) ($y + 1) 2 2
        }
    }
}
New-ItemModel "product_comb"

Save-Png "product_wax" {
    param($g)
    Fill $g (Rgb 210 165 55) 7 7 18 18
    Fill $g (Rgb 245 205 90) 8 8 16 6
    Fill $g (Rgb 170 125 40) 8 22 16 2
    Rect $g (Rgb 140 100 30) 7 7 18 18
}
New-ItemModel "product_wax"

Save-Png "product_propolis" {
    param($g)
    Oval $g (Rgb 70 110 65) 7 10 18 14
    Oval $g (Rgb 100 145 90) 10 12 9 7
    Fill $g (Rgb 45 75 45) 12 23 8 2
}
New-ItemModel "product_propolis"

Save-Png "product_royal_jelly" {
    param($g)
    Fill $g (Rgb 245 240 250 230) 10 9 12 17
    Rect $g (Rgb 255 255 255 240) 9 8 14 19
    Fill $g (Rgb 160 110 200) 11 5 10 3
    Fill $g (Rgb 255 250 255 220) 12 12 3 11
    Fill $g (Rgb 220 200 240) 14 18 5 4
}
New-ItemModel "product_royal_jelly"

Save-Png "product_royal_cell" {
    param($g)
    Fill $g (Rgb 205 165 70) 8 9 16 15
    Rect $g (Rgb 150 110 40) 8 9 16 15
    Oval $g (Rgb 245 240 255 230) 12 11 8 11
    Oval $g (Rgb 240 220 255 220) 14 13 4 6
    Fill $g (Rgb 255 255 255 160) 12 12 2 8
}
New-ItemModel "product_royal_cell"

Save-Png "product_pollen" {
    param($g)
    Fill $g (Rgb 180 140 30) 8 20 16 4
    Oval $g (Rgb 240 205 70) 9 12 14 10
    Oval $g (Rgb 255 230 120) 12 14 7 5
    Oval $g (Rgb 250 215 90) 8 9 4 4
    Oval $g (Rgb 250 215 90) 20 10 4 4
}
New-ItemModel "product_pollen"

# ---------------------------------------------------------------- tools

Save-Png "net" {
    param($g)
    for ($i = 0; $i -lt 16; $i++) {
        Fill $g (Rgb 120 80 40) (8 + $i) (25 - $i) 2 2
    }
    for ($i = 0; $i -lt 14; $i += 3) {
        Fill $g (Rgb 245 250 255 220) (9 + $i) 5 1 10
        Fill $g (Rgb 245 250 255 220) 9 (5 + $i) 12 1
    }
    Rect $g (Rgb 225 235 245 220) 9 5 12 10
}
New-ItemModel "net"

Save-Png "smoker" {
    param($g)
    Fill $g (Rgb 120 125 130) 10 14 12 12
    Fill $g (Rgb 155 160 165) 10 14 12 3
    Fill $g (Rgb 90 95 100) 12 26 8 2
    Fill $g (Rgb 80 85 90) 16 10 7 4
    Fill $g (Rgb 60 60 60) 21 9 3 5
    Oval $g (Rgb 210 210 210 140) 22 5 5 5
    Oval $g (Rgb 190 190 190 110) 25 2 4 4
}
New-ItemModel "smoker"

Save-Png "potion_regeneration" {
    param($g)
    Fill $g (Rgb 220 235 245 200) 12 8 8 18
    Rect $g (Rgb 245 255 255 220) 11 7 10 20
    Fill $g (Rgb 150 100 190) 13 5 6 3
    Fill $g (Rgb 230 130 190 220) 13 15 6 9
    Fill $g (Rgb 255 255 255 180) 13 10 2 12
    Fill $g (Rgb 255 220 240) 15 18 3 3
}
New-ItemModel "potion_regeneration"

# ---------------------------------------------------------------- hives

$hives = @(
    @{ name = "hive_t1"; wood = @(200, 160, 100); accent = @(255, 210, 80);  cmd = 1201 },
    @{ name = "hive_t2"; wood = @(150, 110, 60);  accent = @(245, 190, 70);  cmd = 1202 },
    @{ name = "hive_t3"; wood = @(110, 80, 50);   accent = @(235, 180, 60);  cmd = 1203 },
    @{ name = "hive_t4"; wood = @(150, 110, 200); accent = @(220, 170, 255); cmd = 1204 }
)
$legacyPaper = @()
foreach ($hive in $hives) {
    $wood = Rgb $hive.wood[0] $hive.wood[1] $hive.wood[2]
    $accent = Rgb $hive.accent[0] $hive.accent[1] $hive.accent[2]
    $name = $hive.name
    Save-Png "${name}_side" {
        param($g)
        Fill $g $wood 0 0 32 32
        for ($y = 0; $y -lt 32; $y += 5) {
            Fill $g (Rgb 90 60 35 100) 0 $y 32 1
            Fill $g (Rgb 255 235 190 55) 0 ($y + 1) 32 1
        }
        for ($row = 0; $row -lt 2; $row++) {
            for ($col = 0; $col -lt 4; $col++) {
                $x = 3 + $col * 7
                $y = 7 + $row * 7
                Fill $g $accent $x $y 5 5
                Fill $g (Rgb 255 245 190) ($x + 1) ($y + 1) 3 3
            }
        }
        Fill $g (Rgb 90 60 35 90) 15 0 1 32
    }
    Save-Png "${name}_front" {
        param($g)
        Fill $g $wood 0 0 32 32
        for ($y = 0; $y -lt 32; $y += 5) {
            Fill $g (Rgb 90 60 35 100) 0 $y 32 1
            Fill $g (Rgb 255 235 190 55) 0 ($y + 1) 32 1
        }
        for ($row = 0; $row -lt 2; $row++) {
            for ($col = 0; $col -lt 4; $col++) {
                $x = 3 + $col * 7
                $y = 7 + $row * 7
                Fill $g $accent $x $y 5 5
                Fill $g (Rgb 255 245 190) ($x + 1) ($y + 1) 3 3
            }
        }
        Fill $g (Rgb 55 35 20) 12 22 8 9
        Fill $g (Rgb 20 12 6) 14 25 4 4
        Fill $g (Rgb 255 230 140) 11 21 10 2
    }
    Save-Png "${name}_top" {
        param($g)
        Fill $g (Rgb 100 70 40) 0 0 32 32
        Fill $g $wood 2 2 28 28
        Fill $g (Rgb 100 70 40) 6 6 20 20
        Fill $g $wood 8 8 16 16
        Fill $g $accent 13 13 6 6
        Fill $g (Rgb 90 60 35) 15 15 2 2
        for ($i = 0; $i -lt 32; $i += 8) {
            Fill $g (Rgb 90 60 35 80) $i 0 1 32
        }
    }
    Save-Png "${name}_bottom" {
        param($g)
        Fill $g (Rgb 70 50 30) 0 0 32 32
        for ($y = 0; $y -lt 32; $y += 6) {
            Fill $g (Rgb 50 35 20 120) 0 $y 32 1
        }
        Fill $g (Rgb 40 25 10) 0 0 32 2
    }
    New-HiveModel $name
    $legacyPaper += @{ cmd = $hive.cmd; model = $name }
}

# ---------------------------------------------------------------- legacy overrides

Write-Legacy "glass_bottle" "minecraft:item/generated" $legacyGlass
$legacyHoneyAll = @()
$legacyHoneyAll += $legacyHoney
$legacyHoneyAll += @{ cmd = 2013; model = "product_royal_jelly" }
$legacyHoneyAll += $legacyMead
Write-Legacy "honey_bottle" "minecraft:item/generated" $legacyHoneyAll
Write-Legacy "honeycomb" "minecraft:item/generated" ($legacyComb + @(
    @{ cmd = 2010; model = "product_comb" },
    @{ cmd = 2011; model = "product_wax" },
    @{ cmd = 2015; model = "product_royal_cell" }
))
Write-Legacy "green_dye" "minecraft:item/generated" @(@{ cmd = 2012; model = "product_propolis" })
Write-Legacy "yellow_dye" "minecraft:item/generated" @(@{ cmd = 2014; model = "product_pollen" })
Write-Legacy "brush" "minecraft:item/handheld" @(@{ cmd = 2101; model = "net" })
Write-Legacy "blaze_rod" "minecraft:item/handheld" @(@{ cmd = 2102; model = "smoker" })
Write-Legacy "potion" "minecraft:item/generated" @(@{ cmd = 2120; model = "potion_regeneration" })
Write-Legacy "paper" "minecraft:item/generated" $legacyPaper

# ---------------------------------------------------------------- pack meta + zip

Write-Json (Join-Path $build "pack.mcmeta") @{
    pack = @{
        pack_format = 34
        supported_formats = @{ min_inclusive = 34; max_inclusive = 99 }
        min_format = 34
        max_format = 99
        description = "HiveWorks 0.2 resource pack"
    }
}

$outputDir = Split-Path $Output -Parent
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
if (Test-Path $Output) { Remove-Item $Output -Force }

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zipStream = [System.IO.File]::Create($Output)
$archive = New-Object System.IO.Compression.ZipArchive($zipStream, [System.IO.Compression.ZipArchiveMode]::Create)
Get-ChildItem $build -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($build.Length + 1).Replace('\', '/')
    $entry = $archive.CreateEntry($relative, [System.IO.Compression.CompressionLevel]::Optimal)
    $entryStream = $entry.Open()
    $fileStream = [System.IO.File]::OpenRead($_.FullName)
    $fileStream.CopyTo($entryStream)
    $fileStream.Dispose()
    $entryStream.Dispose()
}
$archive.Dispose()
$zipStream.Dispose()

$textureCount = (Get-ChildItem $textures -Filter *.png).Count
$modelCount = (Get-ChildItem $models -Filter *.json).Count
$itemCount = (Get-ChildItem $items -Filter *.json).Count
$size = (Get-Item $Output).Length
"pack=$Output textures=$textureCount models=$modelCount itemDefinitions=$itemCount size=$size"
