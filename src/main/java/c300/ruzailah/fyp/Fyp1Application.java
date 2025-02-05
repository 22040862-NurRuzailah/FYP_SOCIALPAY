package c300.ruzailah.fyp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;



@SpringBootApplication
public class Fyp1Application {

	@Value("${PORT:8080}")
	private String port;
	
	public static void main(String[] args) {
		SpringApplication.run(Fyp1Application.class, args);
	}

}
