package commands;

import java.io.IOException;

import org.crsh.cli.Command;
import org.crsh.cli.Usage;
import org.crsh.command.BaseCommand;
import org.crsh.command.InvocationContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.launcher.Runner;

public class Hello extends BaseCommand  {

  @Command
  @Usage("Say Hello")
  public String main(InvocationContext context) throws IOException{
	  BeanFactory beans = (BeanFactory) context.getAttributes().get("spring.beanfactory");
	  Runner run = 	beans.getBean(Runner.class);
	  run.exec();
    return "HELLO";
  }
}