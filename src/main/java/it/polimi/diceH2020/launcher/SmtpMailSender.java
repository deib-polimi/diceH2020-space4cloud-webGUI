package it.polimi.diceH2020.launcher;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpMailSender {
	
	@Autowired
	private JavaMailSender javaMailSender; ;
	
	public void send(String to, String subject, String body)
		throws MessagingException {
		MimeMessage message = javaMailSender.createMimeMessage();
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setFrom("michele.ciavotta@gmail.com");
		msg.setTo(to);
		msg.setSubject(subject);
		msg.setText(body);
		MimeMessageHelper helper;
		// SSL Certhificate.
		helper = new MimeMessageHelper(message, true);
		// Multipart messages.
		helper.setSubject(subject);
		helper.setTo(to);
		helper.setText(body, true);
		 SimpleMailMessage mail = new SimpleMailMessage();
			mail.setTo(to);
			mail.setFrom("danvega@gmail.com");
			mail.setSubject("Spring Boot is awesome!");
			mail.setText("Why aren't you using Spring Boot?");
			javaMailSender.send(mail);
		//javaMailSender.send(msg);
    }
}