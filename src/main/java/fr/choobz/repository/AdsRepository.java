package fr.choobz.repository;

import fr.choobz.dao.AdDAO;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AdsRepository {
    private final Set<AdDAO> adsBasicRepo = new HashSet<>();

    // Use record hash stability as an identity.
    // While technically correct, shoud probably only use id as an identity and allow replacement of a previous entry
    public boolean add(AdDAO adDAO) {
        return this.adsBasicRepo.add(adDAO);
    }

    public List<AdDAO> list() {
        return new ArrayList<>(this.adsBasicRepo);
    }

    public List<AdDAO> list(@NotNull Filter filter) {
        // Not a smart way to handle filtering, should be done with a dedicated tool (RDS, ES...)
        // This should only abstract the underlying search tooling to avoid leaking (if required)
        return this.adsBasicRepo.stream()
                .filter(adDAO -> switch (filter.key()) {
                    case id -> adDAO.id().contains(filter.value);
                    case email -> adDAO.email().contains(filter.value);
                    case title -> adDAO.title().contains(filter.value);
                    case body -> adDAO.body().contains(filter.value);
                })
                .toList();
    }

    public record Filter(FilterKey key, String value) {
        public enum FilterKey {
            id("id"),
            email("email"),
            title("title"),
            body("body");
            private final String name;

            FilterKey(String name) {
                this.name = name;
            }

            public static FilterKey from(String name) {
                return Arrays.stream(values())
                        .filter(value -> value.getName().equalsIgnoreCase(name))
                        .findFirst()
                        .orElseThrow(() -> new UnsupportedOperationException("Bad filter key %s".formatted(name)));
            }

            public String getName() {
                return name;
            }
        }
    }
}
