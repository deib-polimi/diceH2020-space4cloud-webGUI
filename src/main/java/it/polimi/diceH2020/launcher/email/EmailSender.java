package it.polimi.diceH2020.launcher.email;

import it.polimi.diceH2020.launcher.States;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

@Aspect
@Configurable(autowire = Autowire.BY_TYPE, dependencyCheck = true)
@Setter
public class EmailSender {

    private final Logger logger = Logger.getLogger(this.getClass());

    @Autowired
    private EmailSettings emailSettings;

    @Autowired
    private MailSender mailSender;

    private void doSend(String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String[] recipients = emailSettings.getRecipients();
            if (recipients.length > 0) {
                message.setTo(recipients);
                message.setSubject(emailSettings.getSubject());
                message.setText(text);
                mailSender.send(message);
            } else {
                logger.debug("No recipients configured, aborting");
            }
        } catch (RuntimeException re) {
            logger.error("Error sending email", re);
        }
    }

    @Pointcut(value = "execution(* it.polimi.diceH2020.launcher.model.SimulationsManager.setState(*)) && args(state,..)")
    private void onStateChange(States state) {}

    @After(value = "onStateChange(state)", argNames = "state")
    public void sendEmailBasedOnState(States state) {
        if (emailSettings.isEnabled()) {
            String text = emailSettings.getMessages().get(state);
            if (text != null) {
                logger.debug(String.format("Sending email for state: %s", state));
                doSend(text);
            }
        }
    }
}
