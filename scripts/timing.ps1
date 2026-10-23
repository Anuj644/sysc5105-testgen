# AI-assisted , reviewed by me. Re-measures the test execution time of every suite in one session.
$suites = "quadratic.CombinatorialTest","quadratic.CategoryPartitionTest","quadratic.MetamorphicTest","quadratic.ImprovementTest",
          "dates.DatesCombinatorialTest","dates.DatesCategoryPartitionTest","dates.DatesMetamorphicTest","dates.DatesImprovementTest"
$runs = 5
foreach ($s in $suites) {
  $times = @()
  for ($i = 1; $i -le $runs; $i++) {
    mvn -q test "-Dtest=$s" "-Djacoco.skip=true" | Out-Null
    $f = Get-ChildItem target\surefire-reports -Filter "TEST-$s.xml"
    $x = [xml](Get-Content $f.FullName)
    $times += [double]$x.testsuite.time
  }
  $mean = ($times | Measure-Object -Average).Average
  "{0}: mean {1:N3} s (runs: {2})" -f $s, $mean, (($times | ForEach-Object { "{0:N3}" -f $_ }) -join ", ")
}