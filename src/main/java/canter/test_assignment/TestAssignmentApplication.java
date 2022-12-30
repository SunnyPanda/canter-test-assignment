package canter.test_assignment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileWriter;
import java.util.Scanner;

@SpringBootApplication
public class TestAssignmentApplication implements CommandLineRunner {

	private static final Logger LOG = LoggerFactory
			.getLogger(TestAssignmentApplication.class);

	private final FetchingService service;

	@Autowired
	public TestAssignmentApplication(FetchingService service) {
		this.service = service;
	}

	public static void main(String[] args) {
		SpringApplication.run(TestAssignmentApplication.class, args);
	}

	@Override
	public void run(String... args) {
		LOG.info("Hi");

		Scanner in = new Scanner(System.in);

		System.out.println("Please, choose an action: 1 - download products, 2 - download 2Dos, 3 - exit");
		String action = in.nextLine();
		while (true) {
			switch (action) {
				case "1" -> service.fetchProducts();
				case "2" -> service.fetchToDos();
				case "3" -> {
					LOG.info("Bye!");
					System.exit(0);
				}
				default -> System.out.println("Wrong action, please, try another number");
			}
			System.out.println("\nPlease, choose an action: 1 - download products, 2 - download 2Dos, 3 - exit");
			action = in.nextLine();
		}
	}
}
