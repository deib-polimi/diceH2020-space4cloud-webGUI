package it.polimi.diceH2020.launcher.email;

import org.aspectj.lang.Aspects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailSenderConfiguration {
    @Bean(name = "EmailSender")
    public EmailSender createEmailSender() {
        return Aspects.aspectOf(EmailSender.class);
    }
}
