#!/bin/bash
echo `date` $@ 
cd /home/www-data/ 
./grade.sh assignment5 $@ 
