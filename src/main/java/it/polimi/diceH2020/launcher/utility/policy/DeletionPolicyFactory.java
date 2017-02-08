/*
Copyright 2016-2017 Eugenio Gianniti

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

import it.polimi.diceH2020.launcher.utility.FileManagementSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class DeletionPolicyFactory {

    @Autowired
    private FileManagementSettings settings;

    @Bean
    public DeletionPolicy create() throws RuntimeException {
        DeletionPolicy policy;
        switch (settings.getDeletionPolicy()) {
            case DELETE:
                policy = new Delete();
                break;
            case DELETE_ON_EXIT:
                policy = new DeleteOnExit();
                break;
            case KEEP_FILES:
                policy = new KeepFiles();
                break;
            default:
                throw new RuntimeException("Misconfigured deletion policy");
        }
        return policy;
    }
}
