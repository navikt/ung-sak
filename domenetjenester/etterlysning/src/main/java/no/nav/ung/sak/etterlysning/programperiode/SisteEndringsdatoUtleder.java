package no.nav.ung.sak.etterlysning.programperiode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class SisteEndringsdatoUtleder {

    /**
     * Finner endret dato (start- eller sluttdato) ved å sammenligne gjeldende snapshot med tidligere snapshots.
     * <p>
     * Behovet for denne metoden oppstår fordi vi må finne ut om en dato har blitt endret fra det som bruker sist tok stilling til. Dersom vi har flere endringer på perioden der disse er av ulike typer (endring i startdato, endring i sluttdato...),
     * ønsker vi å kunne gi detaljert informasjon om hva som har blitt endret fra forrige etterlysning som enten ble besvart eller utløpt.
     *
     * @param gjeldendeSnapshot       Snapshot av det aktive grunnlaget
     * @param aktuelleSnapshotsSortert Alle aktuelle snapshots for sammenligning sortert med nyeste først
     * @param aktuellDatoHenter       Funksjon for å hente aktuell dato (start- eller sluttdato)
     * @return Evt. endret dato informasjon
     */
    static Optional<EndretDato> finnSistEndretDato(PeriodeSnapshot gjeldendeSnapshot, List<PeriodeSnapshot> aktuelleSnapshotsSortert, AktuellDatoHenter aktuellDatoHenter) {
        var gjeldendeDato = aktuellDatoHenter.hent(gjeldendeSnapshot);
        if (gjeldendeDato.isEmpty()) {
            return Optional.empty();
        }
        var gjeldendeDatoOgGrunnlag = new DatoOgBeskrivelse(gjeldendeDato.get(), gjeldendeSnapshot.beskrivelse());
        boolean harEndringIDato = false;
        DatoOgBeskrivelse forrigeDatoOgBeskrivelse = null;
        for (var snapshot : aktuelleSnapshotsSortert) {
            var datoISnapshot = aktuellDatoHenter.hent(snapshot);
            harEndringIDato = datoISnapshot.isEmpty() || !datoISnapshot.get().equals(gjeldendeDatoOgGrunnlag.dato);
            if (harEndringIDato) {
                forrigeDatoOgBeskrivelse = new DatoOgBeskrivelse(datoISnapshot.orElse(null), gjeldendeDatoOgGrunnlag.beskrivelse);
                break;
            }
        }

        if (harEndringIDato) {
            return Optional.of(new EndretDato(gjeldendeDatoOgGrunnlag, forrigeDatoOgBeskrivelse));
        }
        return Optional.empty();
    }


    @FunctionalInterface
    interface AktuellDatoHenter {
        Optional<LocalDate> hent(PeriodeSnapshot snapshot);
    }


    public record EndretDato(DatoOgBeskrivelse nyDatoOgBeskrivelse, DatoOgBeskrivelse forrigeDatoOgBeskrivelse) {
        @Override
        public String toString() {
            return "EndretDato{" +
                "nyDatoOgBeskrivelse=" + nyDatoOgBeskrivelse +
                ", forrigeDatoOgBeskrivelse=" + forrigeDatoOgBeskrivelse +
                '}';
        }
    }


    public record DatoOgBeskrivelse(LocalDate dato, String beskrivelse) {
        @Override
        public String toString() {
            return "DatoOgGrunnlag{" +
                "dato=" + dato +
                ", beskrivelse=" + beskrivelse +
                '}';
        }
    }

}
