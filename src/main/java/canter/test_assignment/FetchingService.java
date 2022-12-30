package canter.test_assignment;

import canter.test_assignment.entity.SingleProduct;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FetchingService {

    private WebClient webClient = WebClient.create();
    private RateLimiter limiter = RateLimiter.create(5);

    private final Connector connector;

    public FetchingService(Connector connector) {
        this.connector = connector;
    }

    public void fetchProducts() {
        List<SingleProduct> allProducts = connector.getProducts();
        allProducts.stream()
                .collect(Collectors.groupingBy(SingleProduct::getCategory))
                .forEach((category, products) -> createCSVFile(products, category));

        allProducts.stream().parallel().forEach(this::savePics);
    }

    public void fetchToDos() {
        List<String> toDosLinks = connector.getUsers().stream()
                .filter(user -> user.getAge() >= 40)
                .map(user -> String.format(Connector.TODOS_URI, user.getId()))
                .toList();

        connector.getToDos(toDosLinks).forEach(System.out::println);
    }

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
