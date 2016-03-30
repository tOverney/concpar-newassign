#!/bin/bash

assignment=${1:?"Error: expected assignment tag as 1st argument."}
repo=${2:?"Error: expected git repository name as second argument."}
rev=${3:?"Error: expected revision hash as third argument."}
sbt="/home/www-data/bin/sbt"

echo Grading $assignment from $repo. Commit $rev:

uri="git@larasrv03.epfl.ch:${repo}"
repodirname="student-repos/${assignment}-${repo}-${rev}"
referencedir="/home/www-data/reference/concpar-newassign"
basedir=`pwd`

#scalasafe="${scala} -Djava.security.manager -Djava.security.policy=./${policyfile}"

# Now we define everything in terms of methods and then have a small snippet at the end
# which invokes the methods. So you can probably scroll down all the way.

#
# Clone the repository for the assignment *** SAME FOR ALL ASSIGNMENTS ***
#
clone_repo() {
  echo "*** Repo clone ***"

  cd ${basedir}
  rm -rf ${repodirname}

  git clone ${uri} ${repodirname} &> /dev/null
  if [ $? -ne 0 ]
  then
    echo Could not clone repo: ${uri}
    exit 1
  fi
  cd ${repodirname}
  git checkout ${rev} &> /dev/null
  if [ $? -ne 0 ]
  then
    echo Could not get hash: ${rev}
    exit 1
  fi

  chmod u+rw "${basedir}/${repodirname}" -R
  if [ $? -ne 0 ]
  then
    echo Could not get set rights to files.
    exit 1
  fi

  echo Repository ${uri} cloned at ${rev}.
  date > date.txt
  cd ..
  echo ""
}

#
# Override the build scripts and the tests with our own verified versions 
#
override_repo() {
  cd ${basedir}/${repodirname}
  echo "*** Restore build scripts and test suite ***"
  # We need to override the sbt build script and test directory here:
  # build.sbt
  # project/Build.scala
  # project/...

  #rm -rf src/test
  rm -rf project
  rm -rf target
  rm -rf bin
  rm -rf *.sbt
  # and now copying safe files
  cp -r ${referencedir}/* ${basedir}/${repodirname}/

  echo "Successful."
  echo ""
}

#
# Compile the code for the current assignment
#
compile() {
  cd ${basedir}/${repodirname}
  if [ "$assignment" == "assignment5" ]
  then
    compilecommand="$sbt clean compile test:compile"
    sourcefiles=`find src/ -type f -name '*.scala' | xargs echo`

    echo "*** Sources ***"
    for src in ${sourcefiles}; do
      echo "  ${src}"
    done
    echo ""

    echo "*** Compilation ***"
    $compilecommand 2>&1 > sbt.compile.out
    error=$?
    cat sbt.compile.out | sed "s|${basedir}/${repodirname}|<repo>|" | grep "Compiling" | 
	sed -r "s/\x1B\[([0-9]{1,3}((;[0-9]{1,3})*)?)?[m|K]//g" # to remove special characters
  fi

  if [ $error -ne 0 ]
  then
    echo "  Compilation failed. All subsequent tests will inevitably fail."
    exit 1
  else
    echo "  Compilation successful."
  fi

  echo ""
}

ttest() {
  cd ${basedir}/${repodirname}
  if [ "$assignment" == "assignment5" ]
  then
    testsources=`find src/test/ -type f -name '*.scala' | xargs echo`

    echo "*** Tests ***"
    for src in ${testsources}; do
      echo "  ${src}"
    done
    echo ""

    echo "*** Test execution ***"
    bash -c "$sbt test" 2>&1 > sbt.test.out    
    echo Tests: 
    cat sbt.test.out | sed "s|${basedir}/${repodirname}|<repo>|" | grep -v ".scala" |
	sed -r "s/\x1B\[([0-9]{1,3}((;[0-9]{1,3})*)?)?[m|K]//g" # to remove special characters
    echo ""
  fi
  
  echo ""
  failure=`grep "TESTS FAILED" sbt.test.out`
  if [ "$failure" == "" ]
  then
    echo "Test execution successful, your submission passed all automated tests!"
    echo "Note: your code may be manually inspected, and the grades could be revised if any wrong doing is identified."    
    exit 0
  else    
    echo "Some tests failed."    
    exit 1
  fi  
}

if [ "$assignment" == "assignment5" ] #-o "$assignment" == "assignment6" 
then
    clone_repo
    override_repo
    compile
    ttest
else
  echo "Assignment checking scripts for $assignment are not ready yet. Please check back later... Sorry :("
  exit 1
fi
