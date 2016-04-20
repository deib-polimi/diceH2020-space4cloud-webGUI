package it.polimi.diceH2020.launcher.email;

import it.polimi.diceH2020.launcher.States;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;

@Component
@ConfigurationProperties(prefix = "email")
@Data
class EmailSettings {
    private boolean enabled;
    private String[] recipients;
    private String subject;
    private Map<States, String> messages = new TreeMap<>();
}
