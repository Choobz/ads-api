package fr.choobz.services;

import fr.choobz.dao.AdDAO;
import fr.choobz.dto.AdDTO;
import fr.choobz.repository.AdsRepository;
import fr.choobz.repository.AdsRepository.Filter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static fr.choobz.repository.AdsRepository.Filter.FilterKey;

@Service
public class AdsServices {
    private static final String NOT_VOWELS = "[^aeiouAEIOU]";
    private final AdsRepository adsRepository;

    public AdsServices(AdsRepository adsRepository) {
        this.adsRepository = adsRepository;
    }

    public boolean add(AdDTO adDTO) {
        final var adsDAO = new AdDAO(adDTO.getId(), adDTO.getEmail(), adDTO.getTitle(), adDTO.getBody());
        return this.adsRepository.add(adsDAO);
    }

    public List<AdDTO> list(Map.Entry<String, String> filter) {
        List<AdDAO> adds;
        if (filter != null) {
            final var filterKey = FilterKey.from(filter.getKey());
            adds = this.adsRepository.list(new Filter(filterKey, filter.getValue()));
        } else {
            adds = this.adsRepository.list();
        }

        return adds.stream()
                .map(AdsServices::map)
                .toList();
    }


    private static AdDTO map(AdDAO adDAO) {
        final var adsDTO = new AdDTO();
        adsDTO.setId(adDAO.id());
        adsDTO.setEmail(adDAO.email());
        adsDTO.setTitle(adDAO.title());
        adsDTO.setBody(adDAO.body());
        return adsDTO;
    }

    public InputStream vowelsPerAdd() {
        final var sw = new StringWriter();

        final var csvFormat = CSVFormat.DEFAULT.builder()
                // No headers (wasn't asked)
                .build();

        try (final var printer = new CSVPrinter(sw, csvFormat)) {
            this.adsRepository.list().stream()
                    .filter(adDAO -> StringUtils.hasText(adDAO.body()))
                    .map(adDAO -> new CsvPair(adDAO.id(), countVowels(adDAO.body())))
                    .forEach(printRecord(printer));
        } catch (IOException e) {
            throw new CsvException("Broken IO on csv creation (can't happen on a string writer)", e);
        }
        // If needed, the writer should be transformed into a FileWriter onto the local FS and
        // the InputStream handling should be left to spring web File handling (but that's another can of worms)
        return new ByteArrayInputStream(sw.toString().trim().getBytes(StandardCharsets.UTF_8));
    }

    private Consumer<? super CsvPair> printRecord(CSVPrinter printer) {
        return csvPair -> {
            try {
                printer.printRecord(csvPair.id, csvPair.nbvowel);
            } catch (IOException e) {
                throw new CsvException("Broken IO on csv creation (can't happen on a string writer)", e);
            }
        };
    }

    private int countVowels(String body) {
        return body.replaceAll(NOT_VOWELS,"").length();
    }

    public static class CsvException extends RuntimeException {
        public CsvException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record CsvPair(String id, int nbvowel) {
    }
}
