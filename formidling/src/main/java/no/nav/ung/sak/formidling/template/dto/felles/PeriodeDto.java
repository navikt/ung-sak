package no.nav.ung.sak.formidling.template.dto.felles;


import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PeriodeDto(
    LocalDate fom,
    LocalDate tom
) {
    @JsonProperty
    public boolean harFlereDager() {
        if (fom == null || tom == null) {
            return true;
        }
        return ChronoUnit.DAYS.between(fom, tom) > 0L;
    }
}
