/*
Copyright 2016 Eugenio Gianniti

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package it.polimi.diceH2020.launcher.utility.policy;

import java.io.File;

public interface DeletionPolicy {
    /**
     * Delegate file deletion
     * @param file: the file to delete
     * @return {@code true} if the file was actually deleted
     */
    boolean delete(File file);

    void markForDeletion(File file);
}
