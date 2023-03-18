package fr.choobz;

import fr.choobz.dto.AdDTO;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

// Flaky test left for discussion (non-working on certain architecture/jvm)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = RANDOM_PORT)
class AdsControllerIntegrationTest {

    private static final String ADS_ENDPOINT = "/ads";
    private static final ParameterizedTypeReference<List<AdDTO>> LIST_PARAMETERIZED_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
    };

    @Value(value = "${local.server.port}")
    private int port;

    @Value(value = "${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldAddAdsWithNoConflict() {
        final var response = restTemplate.postForEntity(getAdsUrl(), createBasicAd(UUID.randomUUID().toString()), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void shouldFailOnAddDueToValidation() {
        // Multiple assertion and test per test method left for discussion
        final var noId = new AdDTO();
        noId.setEmail("justtoignore");
        noId.setTitle("justtoignore");
        assertThat(restTemplate.postForEntity(getAdsUrl(), noId, String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // No error body left for discussion

        final var emptyId = new AdDTO();
        emptyId.setId("");
        emptyId.setEmail("justtoignore");
        emptyId.setTitle("justtoignore");
        assertThat(restTemplate.postForEntity(getAdsUrl(), emptyId, String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        final var noEmail = new AdDTO();
        noEmail.setId("justtoignore");
        noEmail.setTitle("justtoignore");
        assertThat(restTemplate.postForEntity(getAdsUrl(), noEmail, String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        final var emptyEmail = new AdDTO();
        emptyEmail.setId("justtoignore");
        emptyEmail.setEmail(" ");
        emptyEmail.setTitle("justtoignore");
        assertThat(restTemplate.postForEntity(getAdsUrl(), emptyEmail, String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        final var tooBigTitle = new AdDTO();
        tooBigTitle.setId("justtoignore");
        tooBigTitle.setEmail("justtoignore");
        tooBigTitle.setTitle(IntStream.range(0, 301).mapToObj(ignored -> "c").collect(Collectors.joining()));
        assertThat(restTemplate.postForEntity(getAdsUrl(), tooBigTitle, String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        final var noTitleOrBody = new AdDTO();
        noTitleOrBody.setId("justtoignore");
        noTitleOrBody.setEmail("justtoignore");
        assertThat(restTemplate.postForEntity(getAdsUrl(), noTitleOrBody, String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldAddAdsWithConflict() {
        final var basicAd = createBasicAd(UUID.randomUUID().toString());
        final var responseOk = restTemplate.postForEntity(getAdsUrl(), basicAd, Void.class);
        assertThat(responseOk.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        final var responseConflict = restTemplate.postForEntity(getAdsUrl(), basicAd, Void.class);
        assertThat(responseConflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldListAllAds() {
        restTemplate.postForEntity(getAdsUrl(), createBasicAd(UUID.randomUUID().toString()), Void.class);
        restTemplate.postForEntity(getAdsUrl(), createBasicAd(UUID.randomUUID().toString()), Void.class);

        final var response = restTemplate.exchange(getAdsUrl(), HttpMethod.GET, HttpEntity.EMPTY, LIST_PARAMETERIZED_TYPE_REFERENCE);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final var allAds = response.getBody();
        assertThat(allAds).isNotEmpty().hasSize(2);
    }

    @Test
    void shouldListAllAdsFilteringById() {
        restTemplate.postForEntity(getAdsUrl(), createBasicAd("idmatched"), Void.class);
        restTemplate.postForEntity(getAdsUrl(), createBasicAd("idnotmatched"), Void.class);

        final var response = restTemplate.exchange(getAdsUrl() + "?id=idmat", HttpMethod.GET, HttpEntity.EMPTY, LIST_PARAMETERIZED_TYPE_REFERENCE);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final var allAds = response.getBody();
        Assertions.assertThat(allAds).isNotEmpty().hasSize(1);
    }

    @Test
    void shouldListAllAdsFilteringByemail() {
        restTemplate.postForEntity(getAdsUrl(), createBasicAd("id", "emailmatchedButCase"), Void.class);
        restTemplate.postForEntity(getAdsUrl(), createBasicAd("id", "emailMatched"), Void.class);

        final var response = restTemplate.exchange(getAdsUrl() + "?email=ilMat", HttpMethod.GET, HttpEntity.EMPTY, LIST_PARAMETERIZED_TYPE_REFERENCE);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final var allAds = response.getBody();
        Assertions.assertThat(allAds).isNotEmpty().hasSize(1);
    }

    @Test
    void shouldCountVowels() {
        final var headers = new LinkedMultiValueMap<String, String>();
        headers.put("Accept", List.of("text/csv"));
        final var csv = restTemplate.exchange(getAdsUrl(), HttpMethod.GET, new HttpEntity<>(null, headers), InputStream.class);
        assertThat(csv.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(csv.getHeaders()).containsEntry("Content-Type", List.of("text/csv"));
        assertThat(csv.getHeaders()).containsEntry("Content-Disposition", List.of("attachement; filename=vowels.csv"));
    }

    @Test
    void shouldFailOnListAllAdsBadFiltering() {
        final var response = restTemplate.exchange(getAdsUrl() + "?car=ilMat", HttpMethod.GET, HttpEntity.EMPTY, LIST_PARAMETERIZED_TYPE_REFERENCE);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    }

    private static AdDTO createBasicAd(String id) {
        return createBasicAd(id, "emailnotformated");
    }

    private static AdDTO createBasicAd(String id, String email) {
        final var adsDTO = new AdDTO();
        adsDTO.setId(id);
        adsDTO.setEmail(email);
        adsDTO.setTitle("This is a title");
        return adsDTO;
    }

    private String getAdsUrl() {
        return getLocalServerUrl() + ADS_ENDPOINT;
    }

    private String getLocalServerUrl() {
        return "http://localhost:%d/%s".formatted(port, contextPath);
    }
}
