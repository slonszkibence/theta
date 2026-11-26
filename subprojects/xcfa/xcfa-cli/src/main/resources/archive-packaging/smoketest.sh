#!/bin/bash

#
#  Copyright 2026 Budapest University of Technology and Economics
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

set -euo pipefail

cd $(dirname $0)

./theta-start.sh input.c --svcomp --portfolio STABLE --property unreach-call.prp 2>&1 | tee output.log

grep "SafetyResult Unsafe" output.log && ls witness.yml
