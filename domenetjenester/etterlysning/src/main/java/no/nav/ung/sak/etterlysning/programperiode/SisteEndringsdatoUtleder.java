package no.nav.ung.sak.etterlysning.programperiode;

import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SisteEndringsdatoUtleder {

    /**
     * Finner endret dato (start- eller sluttdato) ved å sammenligne gjeldende grunnlag med tidligere etterlysninger og initielt grunnlag.
     * <p>
     * Behovet for denne metoden oppstår fordi vi må finne ut om en dato har blitt endret fra det som bruker sist tok stilling til. Dersom vi har flere endringer på perioden der disse er av ulike typer (endring i startdato, endring i sluttdato...),
     * ønsker vi å kunne gi detaljert informasjon om hva som har blitt endret fra forrige etterlysning som enten ble besvart eller utløpt.
     *
     * @param gjeldendeGrunnlag       Det aktive grunnlaget
     * @param aktuelleGrunnlagSortert Alle aktuelle grunnlag for sammenligning sortert med nyeste først
     * @param aktuellDatoHenter       Funksjon for å hente aktuell dato (start- eller sluttdato)
     * @return Evt. endret dato informasjon
     */
    static Optional<EndretDato> finnSistEndretDato(UngdomsprogramPeriodeGrunnlag gjeldendeGrunnlag, List<UngdomsprogramPeriodeGrunnlag> aktuelleGrunnlagSortert, AktuellDatoHenter aktuellDatoHenter) {
        var gjeldendeDato = aktuellDatoHenter.hent(gjeldendeGrunnlag);
        if (gjeldendeDato.isEmpty()) {
            return Optional.empty();
        }
        var gjeldendeDatoOgGrunnlag = new DatoOgGrunnlag(gjeldendeDato.get(), gjeldendeGrunnlag.getGrunnlagsreferanse());
        boolean harEndringIDato = false;
        DatoOgGrunnlag forrigeDatoOgGrunnlag = null;
        for (var grunnlag : aktuelleGrunnlagSortert) {
            var datoIEtterlysning = aktuellDatoHenter.hent(grunnlag);
            harEndringIDato = datoIEtterlysning.isEmpty() || !datoIEtterlysning.get().equals(gjeldendeDatoOgGrunnlag.dato);
            if (harEndringIDato) {
                forrigeDatoOgGrunnlag = new DatoOgGrunnlag(datoIEtterlysning.orElse(null), gjeldendeDatoOgGrunnlag.grunnlagsreferanse);
                break;
            }
        }

        if (harEndringIDato) {
            return Optional.of(new EndretDato(gjeldendeDatoOgGrunnlag, forrigeDatoOgGrunnlag));
        }
        return Optional.empty();
    }


    @FunctionalInterface
    interface AktuellDatoHenter {
        Optional<LocalDate> hent(UngdomsprogramPeriodeGrunnlag grunnlag);
    }


    public record EndretDato(DatoOgGrunnlag nyDato, DatoOgGrunnlag forrigeDato) {
        @Override
        public String toString() {
            return "EndretDato{" +
                "nyDato=" + nyDato +
                ", forrigeDato=" + forrigeDato +
                '}';
        }
    }


    public record DatoOgGrunnlag(LocalDate dato, UUID grunnlagsreferanse) {
        @Override
        public String toString() {
            return "DatoOgGrunnlag{" +
                "dato=" + dato +
                ", grunnlagsreferanse=" + grunnlagsreferanse +
                '}';
        }
    }

}
