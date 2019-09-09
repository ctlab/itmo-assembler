#!/bin/bash

res=".:bin/java"

for f in `find lib/ -iname '*.jar'`
do
    res="$res:$f"
done

echo "$res"

