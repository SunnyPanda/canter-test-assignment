package canter.test_assignment.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Products {

    List<Product> products;
    int total;
    int skip;
    int limit;

    @Override
    public String toString() {
        return products.stream().map(Product::toString).reduce((s1, s2) -> String.format("%s\n%s\n", s1, s2)).toString();
    }
}
