#!/bin/bash

BASE=$(cd $(dirname $0) && pwd)
cd ${BASE}
GEN_SQL="${BASE}/../utils/cloudos-lib/gen-sql.sh"

outfile=${BASE}/target/histori.sql

SILENT="${1}"

if [ ! -z "${SILENT}" ] ; then
  ${GEN_SQL} histori_test ${outfile} 1> /dev/null 2> /dev/null
else
  ${GEN_SQL} histori_test ${outfile}
fi