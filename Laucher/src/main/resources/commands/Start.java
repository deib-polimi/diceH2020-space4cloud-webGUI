package commands;

import java.io.IOException;

import org.crsh.cli.Command;
import org.crsh.cli.Usage;
import org.crsh.command.BaseCommand;
import org.crsh.command.InvocationContext;
import org.crsh.text.Color;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.launcher.Experiment;
import it.polimi.diceH2020.launcher.Runner;

public class Start extends BaseCommand  {

  @Command
  @Usage("Start the experiment")
  public String main(InvocationContext context) throws IOException{
	  BeanFactory beans = (BeanFactory) context.getAttributes().get("spring.beanfactory");
	  Runner run = 	beans.getBean(Runner.class);
	  out.println("This can take a while, for this reason the the experiment in background", Color.red);
	  Experiment exp = 	beans.getBean(Experiment.class);
	  exp.setStop(false);
	  run.run();	  
    return "";
  }
}