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

## -*- texinfo -*-
## @deftypefn {Function File} {@var{results} =} read_results (@var{filename})
##
## Read results from the input CSV file named @var{filename}
## and return its content in @var{results}.
##
## @end deftypefn

function results = read_results (filename)

if (! ischar (filename))
  error ("read_results: FILENAME should be a string");
else
  [results.instance, results.query, results.provider, ...
   results.type, results.vm_number, ...
   results.m, results.v, results.containers, ...
   results.cost, results.penalty, ...
   results.R, results.D, results.users, results.feasible, ...
   results.init_time, results.eval_time, results.opt_time, ...
   results.scenario, results.error] = ...
    textread (filename,
              "%s %s %s %s %d %f %f %d %f %f %f %f %d %s %d %d %d %s %s",
              "delimiter", ",", "headerLines", 1);

  results.error = strcmp (results.error, "True");
  results.feasible = strcmp (results.feasible, "True");
endif

endfunction
