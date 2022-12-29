package canter.test_assignment;

import canter.test_assignment.entity.Product;
import canter.test_assignment.entity.Products;
import canter.test_assignment.entity.ToDos;
import canter.test_assignment.entity.Users;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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

	WebClient client = WebClient.create("https://dummyjson.com");
	WebClient webClient = WebClient.create();
	RateLimiter limiter = RateLimiter.create(10);

	public static void main(String[] args) {
		SpringApplication.run(TestAssignmentApplication.class, args);
	}

	@Override
	public void run(String... args) {
		System.out.println("Hi!");

		List<String> requests = new ArrayList<>();
		for (int i = 0; i < 100; i += 10) {
			requests.add(String.format("/products?limit=%d&skip=%d", 10, i));
		}

		List<Product> products = Objects.requireNonNull(Flux.fromIterable(requests)
						.flatMap(this::getProducts)
						.collectList()
						.block())
				.stream()
				.flatMap(products1 -> products1.getProducts().stream())
				.toList();

		Map<String, List<Product>> map = products.stream().collect(Collectors.groupingBy(Product::getCategory));
		map.forEach((category, products2) -> {
			try {
				createCSVFile(products2, category);
			} catch (IOException e) {
				throw new RuntimeException("MISTAAAAAAKE!1");
			}
		});

		products.stream().parallel().forEach(this::savePics);

		fetchToDos();

		System.out.println("Bye!");
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
				.filter(toDos2 -> toDos2.getTotal() > 0)
				.flatMap(toDos2 -> toDos2.getTodos().stream())
				.forEach(System.out::println);
	}

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
	private Mono<Products> getProducts(String request) {
		return client.get()
				.uri(request)
				.retrieve()
				.bodyToMono(Products.class);
	}

	public void createCSVFile(List<Product> products, String category) throws IOException {
		FileWriter out = new FileWriter(String.format("../csv/%s.csv", category));
		try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader("ID", "TITLE", "DESCRIPTION", "BRAND"))) {
			products.stream().sorted(Comparator.comparing(Product::getTitle)).forEach(product -> {
				try {
					printer.printRecord(product.getId(), product.getTitle(), product.getDescription(), product.getBrand());
				} catch (IOException e) {
					throw new RuntimeException("MISTAAAAAAKE!2");
				}
			});
		}
	}

	private void savePics(Product product) {

		product.getImages().stream().filter(image -> !image.endsWith("thumbnail")).forEach(image -> {
			String suffix = image.substring(image.lastIndexOf(".") + 1);
			String name = image.substring(image.lastIndexOf("/") + 1, image.lastIndexOf("."));
			Path path = Paths.get(String.format("../pics/%d_%s_%s.%s", product.getId(),
					product.getTitle().replace('/', '-'),
					name, suffix));

			limiter.acquire();
			Flux<DataBuffer> dataBufferFlux = webClient.get().uri(image).retrieve().bodyToFlux(DataBuffer.class);
			DataBufferUtils.write(dataBufferFlux, path, StandardOpenOption.CREATE).block();
		});
	}
}
