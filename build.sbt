libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.0" % "test"
libraryDependencies += "junit" % "junit" % "4.8.1" % "test"

import Tests._
{
// A method to group a  tests
 def singleTests(tests: Seq[TestDefinition]) =
  tests map { test =>
    new Group(
      name = test.name,
      tests = Seq(test),
      runPolicy = SubProcess(javaOptions = Seq.empty[String]))
  }

 testGrouping in Test <<= definedTests in Test map singleTests
}