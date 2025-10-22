package no.nav.ung.sak.kabal.kontrakt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KabalRequestv4(

    @JsonIgnore
    UUID behandlingUuid,

    @NotNull
    String type,

    OversendtKlager klager,

    @NotNull
    OversendtSakenGjelder sakenGjelder,

    @NotNull
    OversendtSak fagsak,

    @NotNull
    String kildeReferanse,

    @NotNull
    List<String> hjemler,

    @NotNull
    String forrigeBehandlendeEnhet,

    @NotNull
    List<String> tilknyttedeJournalposter,

    @NotNull    // Valgfri i kontrakt, men påkrevd for klager
    LocalDate brukersKlageMottattVedtaksinstans,

    @NotNull
    String ytelse
) {

    public record OversendtKlager(OversendtPartId id) { }

    public record OversendtPartId(
        String type,
        String verdi
    ) {
    }

    public record OversendtProsessfullmektig(
        OversendtPartId id,
        boolean skalKlagerMottaKopi
    ) {
    }

    public record OversendtSakenGjelder(
        OversendtPartId id
    ) { }

    public record OversendtSak(
        String fagsakId,
        String fagsystem
    ) { }

    @Override
    public String toString() {
        return "KabalRequest{" +
            "forrigeBehandlendeEnhet='" + forrigeBehandlendeEnhet + '\'' +
            ", hjemler=" + hjemler + '\'' +
            ", kildeReferanse='" + kildeReferanse + '\'' +
            ", tilknyttedeJournalposter=" + tilknyttedeJournalposter + '\'' +
            ", type='" + type + '\'' +
            ", brukersKlageMottattVedtaksinstans='" + brukersKlageMottattVedtaksinstans + '\'' +
            ", ytelse='" + ytelse + '\'' +
            ", fagsak=" + fagsak +
            '}';
    }
}
