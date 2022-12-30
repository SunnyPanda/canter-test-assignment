package canter.test_assignment;

import canter.test_assignment.entity.SingleProduct;
import canter.test_assignment.entity.SingleToDo;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;

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

    private final Connector connector;

    public FetchingService(Connector connector) {
        this.connector = connector;
    }

    public void fetchProducts() {
        System.out.println("Fetch products...");
        List<SingleProduct> allProducts = connector.getProducts();
        System.out.println("Products fetched.");

        System.out.println("Creating CSV files in resources folder...");
        allProducts.stream()
                .collect(Collectors.groupingBy(SingleProduct::getCategory))
                .forEach((category, products) -> createCSVFile(products, category));
        System.out.println("CSV files created.");

        System.out.println("Download pictures into resources folder...");
        RateLimiter limiter = RateLimiter.create(10);
        allProducts.stream().parallel().forEach(product -> savePics(product, limiter));
        System.out.println("Pictures downloaded.");
    }

    public void fetchToDos() {
        List<String> toDosLinks = connector.getUsers().stream()
                .filter(user -> user.getAge() >= 40)
                .map(user -> String.format(Connector.TODOS_URI, user.getId()))
                .toList();

        connector.getToDos(toDosLinks).stream().sorted(Comparator.comparing(SingleToDo::getUserId))
                .forEach(System.out::println);
    }

    private void createCSVFile(List<SingleProduct> products, String category) {
        try (FileWriter out = new FileWriter(String.format("src/main/resources/%s.csv", category));
             CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader(Headers.class))) {
            products.stream().sorted(Comparator.comparing(SingleProduct::getTitle)).forEach(product -> {
                try {
                    printer.printRecord(product.getId(), product.getTitle(), product.getDescription(), product.getBrand());
                } catch (IOException e) {
                    System.out.printf("Couldn't create the file for category %s. Please, repeat the action%n", category);
                }
            });
        } catch (IOException e) {
            System.out.println("Something went wrong during the downloading process. Please, repeat the action");
        }
    }

    private void savePics(SingleProduct product, RateLimiter limiter) {
        product.getImages().forEach(image -> {
            limiter.acquire();
            DataBufferUtils.write(connector.getPictureData(image), createPath(image, "src/main/resources/%d_%s_%s.%s",
                            product), StandardOpenOption.CREATE).block();
        });
    }

    private Path createPath(String link, String dir, SingleProduct product) {
        int dotIndex = link.lastIndexOf(".");
        String suffix = link.substring( dotIndex + 1);
        String name = link.substring(link.lastIndexOf("/") + 1, dotIndex);
        return Paths.get(String.format(dir, product.getId(),
                product.getTitle().replace('/', '-'),
                name, suffix));
    }
}
