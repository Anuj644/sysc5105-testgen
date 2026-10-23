# AI-assisted , reviewed by me. Scoped measurement helpers for the dates case study.
$global:ex = "getToday,getTomorrow,getTodayWithTime,getDaysBetweenTwoDate,getHoursBetweenTwoDate,getMinutesBetweenTwoDates,getDateAndTime,getTimeOnly,getDateFromDays"

function Get-DatesJacoco {
  $t = (Get-Content target\site\jacoco\jacoco.xml -Raw) -replace '<!DOCTYPE[^>]*>',''
  $x = [xml]$t
  $scope = "convert","parseDate","getDesiredFormat","getDateOnly","parseAnyDate","getDateFormat"
  $ms = foreach ($c in $x.report.package.class) { if ($c.name -like "dates/DateHelper*") { foreach ($m in $c.method) { if ($scope -contains $m.name) { $m } } } }
  foreach ($type in "LINE","BRANCH","INSTRUCTION") {
    $cov = 0; $mis = 0
    foreach ($m in $ms) { foreach ($k in $m.counter) { if ($k.type -eq $type) { $cov += [int]$k.covered; $mis += [int]$k.missed } } }
    "$type covered $cov missed $mis"
  }
}

function Get-DatesPit {
  $f = Get-ChildItem target\pit-reports -Recurse -Filter mutations.csv | Sort-Object LastWriteTime -Descending | Select-Object -First 1
  $rows = Import-Csv $f.FullName -Header file,class,mutator,method,line,status,tests
  $d = $rows | Where-Object { $_.class -like "dates.*" }
  "dates mutants: " + @($d).Count + "  killed: " + @($d | Where-Object { $_.status -in "KILLED","TIMED_OUT" }).Count
  $d | Where-Object { $_.status -eq "SURVIVED" } | ForEach-Object { "survived line $($_.line) $($_.method) $($_.mutator.Split('.')[-1])" }
  $d | Where-Object { $_.status -eq "NO_COVERAGE" } | ForEach-Object { "nocov line $($_.line) $($_.method)" }
}