!# /bin/bash

CCG2LAMBDA_LOCATION=$1

cat ../sentences.txt | sed -f "$CCG2LAMBDA_LOCATION/en/tokenizer.sed" > sentences.tok