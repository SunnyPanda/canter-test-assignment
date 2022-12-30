package canter.test_assignment;

import canter.test_assignment.entity.SingleProduct;
import canter.test_assignment.entity.Products;
import canter.test_assignment.entity.ToDos;
import canter.test_assignment.entity.Users;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
public class TestAssignmentApplication implements CommandLineRunner {

	private WebClient client = WebClient.create("https://dummyjson.com");
	private WebClient webClient = WebClient.create();
	private RateLimiter limiter = RateLimiter.create(5);

	private final Connector connector;

	@Autowired
	public TestAssignmentApplication(Connector connector) {
		this.connector = connector;
	}

	public static void main(String[] args) {
		SpringApplication.run(TestAssignmentApplication.class, args);
	}

	@Override
	public void run(String... args) {
		System.out.println("Hi!");

		fetchProducts();
		fetchToDos();

		System.out.println("Bye!");
	}

	private void fetchProducts() {
		List<SingleProduct> allProducts = connector.getProducts();
		allProducts.stream()
				.collect(Collectors.groupingBy(SingleProduct::getCategory))
				.forEach((category, products) -> createCSVFile(products, category));

		allProducts.stream().parallel().forEach(this::savePics);
	}

	private void fetchToDos() {
		List<String> requests = new ArrayList<>();
		for (int i = 0; i < 100; i += 20) {
			requests.add(String.format("/users?limit=%d&skip=%d&select=age", 20, i));
		}

		List<String> users = Objects.requireNonNull(Flux.fromIterable(requests)
						.flatMap(this::getUsers)
						.collectList()
						.block())
				.stream()
				.flatMap(users1 -> users1.getUsers().stream())
				.filter(user -> user.getAge() >= 40)
				.map(user -> String.format("/users/%d/todos", user.getId()))
				.toList();

		Objects.requireNonNull(Flux.fromIterable(users)
						.flatMap(this::getTodos)
						.collectList()
						.block())
				.stream()
				.flatMap(toDos2 -> toDos2.getTodos().stream())
				.forEach(System.out::println);
	}

	// 1
	private Mono<ToDos> getTodos(String request) {
		return client.get()
				.uri(request)
				.retrieve()
				.bodyToMono(ToDos.class);
	}

	private Mono<Users> getUsers(String request) {
		return client.get()
				.uri(request)
				.retrieve()
				.bodyToMono(Users.class);
	}
	// 1

	public void createCSVFile(List<SingleProduct> singleProducts, String category) {
		try (FileWriter out = new FileWriter(String.format("../csv/%s.csv", category));
			 CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader("ID", "TITLE", "DESCRIPTION", "BRAND"))) {
			singleProducts.stream().sorted(Comparator.comparing(SingleProduct::getTitle)).forEach(singleProduct -> {
				try {
					printer.printRecord(singleProduct.getId(), singleProduct.getTitle(), singleProduct.getDescription(), singleProduct.getBrand());
				} catch (IOException e) {
					throw new RuntimeException("MISTAAAAAAKE!2");
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void savePics(SingleProduct singleProduct) {

		singleProduct.getImages().forEach(image -> {
			String suffix = image.substring(image.lastIndexOf(".") + 1);
			String name = image.substring(image.lastIndexOf("/") + 1, image.lastIndexOf("."));
			Path path = Paths.get(String.format("../pics/%d_%s_%s.%s", singleProduct.getId(),
					singleProduct.getTitle().replace('/', '-'),
					name, suffix));

			limiter.acquire();
			Flux<DataBuffer> dataBufferFlux = webClient.get().uri(image).retrieve().bodyToFlux(DataBuffer.class);
			DataBufferUtils.write(dataBufferFlux, path, StandardOpenOption.CREATE).block();
		});
	}
}
