#!/usr/bin/env python

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

import csv
import json
import sys

headers = ["Instance", "Query", "Provider", "VM Type", "VM Number",
           "m", "v", "Containers", "Cost", "Penalty",
           "Response Time", "Deadline", "Users", "Feasible",
           "INIT_SOLUTION", "EVALUATION", "OPTIMIZATION",
           "Scenario", "Error"]

rows = []

for line in sys.stdin:
    filename = line.strip ()

    with open (filename) as infile:
        data = json.load (infile)

    out = {}
    out["Instance"] = data["id"]
    out["Provider"] = data["provider"]
    out["Cost"] = data["cost"]
    out["Penalty"] = data["penalty"]
    out["Scenario"] = data["scenario"]

    for phase in data["lstPhases"]:
        out[phase["id"]] = phase["duration"]

    for job in data["lstSolutions"]:
        out["Query"] = job["id"]
        out["Feasible"] = job["feasible"]
        out["VM Number"] = job["numberVM"]
        out["Containers"] = job["numberContainers"]
        out["Response Time"] = job["duration"]
        out["Error"] = job["error"]
        out["VM Type"] = job["typeVMselected"]["id"]
        out["Users"] = job["numberUsers"]

        parameters = job["job"]
        out["m"] = parameters["m"]
        out["v"] = parameters["v"]
        out["Deadline"] = parameters["d"]

    rows.append (out)

writer = csv.DictWriter (sys.stdout, fieldnames = headers)
writer.writeheader ()
writer.writerows (rows)
