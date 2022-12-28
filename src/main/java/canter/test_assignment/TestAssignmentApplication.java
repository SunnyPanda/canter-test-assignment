package canter.test_assignment;

import canter.test_assignment.entity.Product;
import canter.test_assignment.entity.Products;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
public class TestAssignmentApplication implements CommandLineRunner {

	WebClient client = WebClient.create("https://dummyjson.com");

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

		System.out.println("Bye!");
	}

	private Mono<Products> getProducts(String request) {
		return client.get()
				.uri(request)
				.retrieve()
				.bodyToMono(Products.class);
	}

	public void createCSVFile(List<Product> products, String category) throws IOException {
		FileWriter out = new FileWriter(String.format("../csv/%s.csv", category));
		try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL.withHeader("ID", "TITLE", "DESCRIPTION", "BRAND"))) {
			products.stream().sorted(Comparator.comparing(Product::getTitle)).forEach(product -> {
				try {
					printer.printRecord(product.getId(), product.getTitle(), product.getDescription(), product.getBrand());
				} catch (IOException e) {
					throw new RuntimeException("MISTAAAAAAKE!2");
				}
			});
		}
	}
}
