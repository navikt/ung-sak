package no.nav.ung.sak.kontrakt.klage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public class PåklagdBehandlingDto {

    @JsonProperty(value = "påklagBehandlingUuid", required = true)
    @NotNull
    private UUID påklagBehandlingUuid;

    @JsonProperty(value = "påklagBehandlingVedtakDato")
    private LocalDate påklagBehandlingVedtakDato;

    @JsonProperty(value = "påklagBehandlingType")
    @Size(max = 2000)
    @Pattern(regexp = TekstValideringRegex.KODEVERK)
    private String påklagBehandlingType;

    PåklagdBehandlingDto() {
        // for CDI
    }

    @JsonCreator
    public PåklagdBehandlingDto(@JsonProperty(value = "påklagBehandlingUuid", required = true) UUID påklagBehandlingUuid,
                                @JsonProperty(value = "påklagBehandlingVedtakDato") LocalDate påklagBehandlingVedtakDato,
                                @JsonProperty(value = "påklagBehandlingType") String påklagBehandlingType) {
        this.påklagBehandlingUuid = påklagBehandlingUuid;
        this.påklagBehandlingVedtakDato = påklagBehandlingVedtakDato;
        this.påklagBehandlingType = påklagBehandlingType;
    }

    public UUID getPåklagBehandlingUuid() {
        return påklagBehandlingUuid;
    }

    public LocalDate getPåklagBehandlingVedtakDato() {
        return påklagBehandlingVedtakDato;
    }

    public String getPåklagBehandlingType() {
        return påklagBehandlingType;
    }

}
