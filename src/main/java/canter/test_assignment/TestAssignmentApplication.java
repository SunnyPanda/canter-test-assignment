package canter.test_assignment;

import canter.test_assignment.entity.Product;
import canter.test_assignment.entity.Products;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SpringBootApplication
public class TestAssignmentApplication implements CommandLineRunner {

	WebClient client = WebClient.create("https://dummyjson.com");

	public static void main(String[] args) {
		SpringApplication.run(TestAssignmentApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
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

		System.out.println("Bye!");
	}

	private Mono<Products> getProducts(String request) {
		return client.get()
				.uri(request)
				.retrieve()
				.bodyToMono(Products.class);
	}


}
