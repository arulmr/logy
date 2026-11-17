param(
    [Parameter(Mandatory = $true)][string]$InputZip,
    [Parameter(Mandatory = $true)][string]$OutputCsv
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Read-ZipCsv([System.IO.Compression.ZipArchiveEntry]$Entry) {
    $reader = [System.IO.StreamReader]::new($Entry.Open())
    try {
        $text = $reader.ReadToEnd()
        if ([string]::IsNullOrWhiteSpace($text)) { return @() }
        return @($text | ConvertFrom-Csv)
    } finally {
        $reader.Dispose()
    }
}

function To-EpochMillis([string]$Value) {
    return [DateTimeOffset]::Parse($Value, [Globalization.CultureInfo]::InvariantCulture).ToUnixTimeMilliseconds()
}

function Escape-Csv([object]$Value) {
    $text = if ($null -eq $Value) { '' } else { [string]$Value }
    if ($text.IndexOfAny([char[]]@(',', '"', "`r", "`n")) -ge 0) {
        return '"' + $text.Replace('"', '""') + '"'
    }
    return $text
}

function Csv-Row([object[]]$Values) {
    return ($Values | ForEach-Object { Escape-Csv $_ }) -join ','
}

$palette = @(4287339766L, 4293681036L, 4278237583L, 4294942531L, 4282223615L, 4288379715L)
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $InputZip))
try {
    $entriesByName = @{}
    foreach ($entry in $zip.Entries) { $entriesByName[$entry.FullName] = $entry }
    if (-not $entriesByName.ContainsKey('Events.csv')) { throw 'The ZIP does not contain Events.csv.' }
    $events = @(Read-ZipCsv $entriesByName['Events.csv'])

    $lines = [Collections.Generic.List[string]]::new()
    $lines.Add('record_type,event_id,event_name,category,category_color,event_color,event_created_at,entry_id,occurred_at,note,duration_minutes,active')

    $categoryNames = @($events | ForEach-Object { $_.Category } | Where-Object { $_ } | Sort-Object -Unique)
    foreach ($category in $categoryNames) {
        $lines.Add((Csv-Row @('category', '', '', $category, 4287339766L, '', '', '', '', '', '', '')))
    }

    foreach ($event in $events) {
        $eventId = [int]$event.Position
        $color = $palette[($eventId - 1) % $palette.Count]
        $lines.Add((Csv-Row @('event', $eventId, $event.Name, $event.Category, '', $color, (To-EpochMillis $event.Created), '', '', '', '', '')))
    }

    $entryId = 1
    $durationCount = 0
    $inactiveCount = 0
    foreach ($event in $events) {
        $eventId = [int]$event.Position
        $entryName = "$($event.Position) $($event.Name).csv"
        if (-not $entriesByName.ContainsKey($entryName)) { throw "Missing event entry file: $entryName" }
        foreach ($record in @(Read-ZipCsv $entriesByName[$entryName])) {
            if ([string]::IsNullOrWhiteSpace($record.'Date/Time')) { continue }
            $note = [string]$record.Note
            $active = $true
            if ($note -match '\s*-\s*X\s*$') {
                $active = $false
                $inactiveCount++
                $note = [regex]::Replace($note, '\s*-\s*X\s*$', '').Trim()
            }

            $duration = $null
            $durationMatch = [regex]::Match($note, '\s*-\s*(?:(?<hours>\d+)\s*h\s*)?(?<minutes>\d+)\s*m\s*$',[Text.RegularExpressions.RegexOptions]::IgnoreCase)
            if ($durationMatch.Success) {
                $hours = if ($durationMatch.Groups['hours'].Success) { [int]$durationMatch.Groups['hours'].Value } else { 0 }
                $duration = ($hours * 60) + [int]$durationMatch.Groups['minutes'].Value
                $durationCount++
                $note = $note.Substring(0, $durationMatch.Index).Trim()
            }

            $lines.Add((Csv-Row @(
                'entry', $eventId, '', '', '', '', '', $entryId,
                (To-EpochMillis $record.'Date/Time'), $note,
                $(if ($null -eq $duration) { '' } else { $duration }),
                $active.ToString().ToLowerInvariant()
            )))
            $entryId++
        }
    }

    $outputDirectory = Split-Path -Parent $OutputCsv
    if ($outputDirectory) { New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null }
    [IO.File]::WriteAllLines($OutputCsv, $lines, [Text.UTF8Encoding]::new($false))
    [pscustomobject]@{
        Events = $events.Count
        Logs = $entryId - 1
        Durations = $durationCount
        Inactive = $inactiveCount
        Output = (Resolve-Path -LiteralPath $OutputCsv).Path
    }
} finally {
    $zip.Dispose()
}
