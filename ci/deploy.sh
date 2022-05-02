#!/usr/bin/env bash
#
# Copyright 2015 Octavian Hasna
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

COMMIT_MESSAGE="$(git log --format=%s --max-count 1)"

if [ "$TRAVIS_PULL_REQUEST" == "false" ] && [[ "$COMMIT_MESSAGE" != *"[no-deploy]"* ]]; then
  gpg --version
  gpg --batch --generate-key ci/gpg.txt
  gpg --list-secret-keys
  mvn deploy --batch-mode -DskipTests=true --settings ci/maven-settings.xml -P release
else
  echo "Skip deploy"
fi
