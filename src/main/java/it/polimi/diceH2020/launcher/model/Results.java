package it.polimi.diceH2020.launcher.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.launcher.utility.Compressor;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.io.IOException;

@Entity
@Data
public class Results {
    @Id
    private long id;
    private int iteration;
    private String instanceName;

    @Column(length = 1000)
    @NotNull
    private String solution="";

    public void setSol(Solution sol){
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.solution = Compressor.compress(mapper.writeValueAsString(sol));
        } catch (IOException e) {
            this.solution = "Error";
        }
    }
    public Solution getSol(){
        ObjectMapper mapper = new ObjectMapper();
        try {
            return this.solution.equals("") || this.solution.equals("Error")? null :
                    mapper.readValue(Compressor.decompress(this.solution), Solution.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
