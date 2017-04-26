#!/bin/sh

## Copyright 2017 Eugenio Gianniti
##
## Licensed under the Apache License, Version 2.0 (the "License");
## you may not use this file except in compliance with the License.
## You may obtain a copy of the License at
##
##     http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.

script="$(basename "$0")"

error_aux ()
{
    lineno="$1"
    shift
    echo "$script": "$lineno": "$@" >&2
    exit 1
}
alias error='error_aux $LINENO '

while getopts :s:u: opt; do
    case "$opt" in
        s)
            input_scenario="$OPTARG"
            ;;
        u)
            input_url="$OPTARG"
            ;;
        \?)
            error unrecognized option "-$OPTARG"
            ;;
        :)
            error "-$OPTARG" option requires an argument
            ;;
    esac
done
shift $(( $OPTIND - 1 ))

scenario="${input_scenario-PublicAvgWorkLoad}"
url="${input_url-localhost:8000}"

if [ $# -lt 1 ]; then
    error missing instance directory
else
    directory="$1"
fi

tmpfile="$(mktemp "/tmp/$script.XXX")"
trap "rm -f '$tmpfile'; exit 130" INT TERM

find "$directory" -type f | while IFS= read -r filename; do
    echo "-F 'file[]=@$filename'"
done > "$tmpfile"

files="$(cat "$tmpfile")"
eval curl -i -F "scenario=$scenario" $files "$url/files/upload" | tee "$tmpfile"
echo

endpoint="$(grep '"submit"' "$tmpfile" | tr '\{\}' '\n' | grep '"href"' \
    | cut -d \" -f 4)"
rm "$tmpfile"

curl -i -X POST "$endpoint"
