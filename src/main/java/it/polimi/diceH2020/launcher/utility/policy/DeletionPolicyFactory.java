package it.polimi.diceH2020.launcher.utility.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "file-management")
public class DeletionPolicyFactory {
    public enum Policy {DELETE, DELETE_ON_EXIT, KEEP_FILES}

    private Policy deletionPolicy = Policy.DELETE;

    public Policy getDeletionPolicy() {
        return deletionPolicy;
    }

    public void setDeletionPolicy(Policy deletionPolicy) {
        this.deletionPolicy = deletionPolicy;
    }

    @Bean
    public DeletionPolicy create() throws RuntimeException {
        DeletionPolicy policy;
        switch (getDeletionPolicy()) {
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
