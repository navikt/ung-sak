package no.nav.ung.sak.kabal.kontrakt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KabalRequestv4(

    @JsonIgnore
    UUID behandlingUuid,
    String type,
    OversendtKlager klager,
    OversendtSakenGjelder sakenGjelder,
    OversendtProsessfullmektig prosessfullmektig,
    OversendtSak fagsak,
    String kildeReferanse,
    List<String> hjemler,
    String forrigeBehandlendeEnhet,
    List<String> tilknyttedeJournalposter,
    LocalDate brukersKlageMottattVedtaksinstans,
    LocalDate frist,
    String ytelse,
    String kommentar        // Kommentarer fra saksbehandler i førsteinstans som ikke er med i oversendelsesbrevet klager mottar
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
            ", avsenderEnhet='" + forrigeBehandlendeEnhet + '\'' +
            ", frist='" + frist + '\'' +
            ", hjemler=" + hjemler + '\'' +
            ", kildeReferanse='" + kildeReferanse + '\'' +
            ", tilknyttedeJournalposter=" + tilknyttedeJournalposter + '\'' +
            ", type='" + type + '\'' +
            ", ytelse='" + ytelse + '\'' +
            ", fagsak=" + fagsak +
            '}';
    }
}
