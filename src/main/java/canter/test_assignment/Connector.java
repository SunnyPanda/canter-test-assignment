package canter.test_assignment;

import canter.test_assignment.entity.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Component
public class Connector {

    public static final String TODOS_URI = "https://dummyjson.com/users/%d/todos";
    private static final String PRODUCTS_URI = "https://dummyjson.com/products?limit=%d&skip=%d";
    private static final String USERS_URI = "https://dummyjson.com/users?limit=%d&skip=%d&select=age";
    WebClient client = WebClient.create();
    public List<SingleProduct> getProducts() {
        return Objects.requireNonNull(Flux.fromIterable(createUri(PRODUCTS_URI, 10, 10, 100))
                        .flatMap(this::getProducts)
                        .collectList()
                        .block())
                .stream()
                .flatMap(products -> products.getProducts().stream())
                .toList();
    }

    public List<SingleUser> getUsers() {
        return Objects.requireNonNull(Flux.fromIterable(createUri(USERS_URI, 20, 20, 5))
                        .flatMap(this::getUsers)
                        .collectList()
                        .block())
                .stream()
                .flatMap(users -> users.getUsers().stream())
                .toList();
    }

    public List<SingleToDo> getToDos(List<String> links) {
        return Objects.requireNonNull(Flux.fromIterable(links)
                        .flatMap(this::getTodos)
                        .collectList()
                        .block())
                .stream()
                .flatMap(toDos -> toDos.getTodos().stream())
                .toList();
    }

    private List<String> createUri(String uri, int limit, int skip, int max) {
        return IntStream.iterate(0, i -> i + skip).limit(max)
                .mapToObj(num -> String.format(uri, limit, num))
                .toList();
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
    private Mono<Products> getProducts(String request) {
        return client.get()
                .uri(request)
                .retrieve()
                .bodyToMono(Products.class);
    }
    // 1
}
