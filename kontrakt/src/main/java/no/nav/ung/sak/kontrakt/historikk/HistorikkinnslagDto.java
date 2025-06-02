package no.nav.ung.sak.kontrakt.historikk;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public record HistorikkinnslagDto(UUID behandlingUuid,
                                  HistorikkAktørDto aktør,
                                  SkjermlenkeType skjermlenke,
                                  LocalDateTime opprettetTidspunkt,
                                  List<HistorikkInnslagDokumentLinkDto> dokumenter,
                                  String tittel,
                                  List<Linje> linjer) implements Comparable<HistorikkinnslagDto> {

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
