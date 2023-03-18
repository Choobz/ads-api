package fr.choobz.web;

import fr.choobz.dto.AdDTO;
import fr.choobz.services.AdsServices;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ads")
public class AdsController {
    private final AdsServices adsServices;

    public AdsController(AdsServices adsServices) {
        this.adsServices = adsServices;
    }

    @PostMapping
    public ResponseEntity<Void> addAds(@Valid @RequestBody AdDTO adDTO) {
        // Jakarta bean validation does not work cross field
        // Implementing it is an expensive task and maintaining it even more so.
        // Need to be carefully considered before being developed
        if(!StringUtils.hasText(adDTO.getTitle()) && !StringUtils.hasText(adDTO.getBody())){
            return ResponseEntity.badRequest().build();
        }

        return this.adsServices.add(adDTO) ?
                ResponseEntity.status(HttpStatus.CREATED).build() :
                ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    // No paging  considering that the dataset will contain a large number of entries might end up being a bad idea
    // As is handling data using deserialization/serialisation, to discuss
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AdDTO>> listAds(@RequestParam Map<String, String> filterParams) {
        if (filterParams.size() > 1) {
            return ResponseEntity.badRequest().build();
        }
        final var filter = !filterParams.isEmpty() ? filterParams.entrySet().iterator().next() : null;

        try {
            final var ads = this.adsServices.list(filter);
            return ResponseEntity.ok(ads);
        } catch (UnsupportedOperationException e) {
            // Can also be set up as a @RestControlerAdvice for a single error policy
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        }
    }

    // While technically true since the Rest specification does not specify it
    // using the Accept header as a service discriminator is not really in the spirit of the spec.
    // Also, this does not work too well with openapi (if used)
    // But since the exercise asked for 2 endpoints with 3 services, that also somewhat count :)
    @GetMapping(produces = "text/csv")
    public ResponseEntity<InputStreamResource> vowelsPerAd(){
        final var fileStream = this.adsServices.vowelsPerAdd();
        return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachement; filename=vowels.csv")
                .body(new InputStreamResource(fileStream));


    }

}
