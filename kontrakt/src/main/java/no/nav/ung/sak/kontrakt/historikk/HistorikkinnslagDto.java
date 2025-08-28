package no.nav.ung.sak.kontrakt.historikk;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;


public record HistorikkinnslagDto(@NotNull UUID historikkinnslagUuid,
                                  @NotNull UUID behandlingUuid,
                                  @NotNull HistorikkAktørDto aktør,
                                  SkjermlenkeType skjermlenke,
                                  @NotNull LocalDateTime opprettetTidspunkt,
                                  @NotNull List<HistorikkInnslagDokumentLinkDto> dokumenter,
                                  String tittel,
                                  @NotNull List<Linje> linjer) implements Comparable<HistorikkinnslagDto> {

    public HistorikkinnslagDto {
        Objects.requireNonNull(historikkinnslagUuid);
        Objects.requireNonNull(behandlingUuid);
        Objects.requireNonNull(aktør);
        Objects.requireNonNull(opprettetTidspunkt);
        if(dokumenter == null) {
            dokumenter = List.of();
        }
        if(linjer == null) {
            linjer = List.of();
        }
    }

    public record HistorikkAktørDto(HistorikkAktør type, String ident) {

        public static HistorikkAktørDto fra(HistorikkAktør aktør, String opprettetAv) {
            if (Set.of(HistorikkAktør.SAKSBEHANDLER, HistorikkAktør.BESLUTTER).contains(aktør)) {
                return new HistorikkAktørDto(aktør, opprettetAv);
            }
            return new HistorikkAktørDto(aktør, null);
        }
    }

    @Override
    public int compareTo(HistorikkinnslagDto o) {
        return this.opprettetTidspunkt.compareTo(o.opprettetTidspunkt);
    }

    public record Linje(Type type, String tekst) {

        public static Linje tekstlinje(String tekst) {
            return new Linje(Type.TEKST, tekst);
        }

        public static Linje linjeskift() {
            return new Linje(Type.LINJESKIFT, null);
        }


        public enum Type {
            TEKST,
            LINJESKIFT;
        }
    }
}
