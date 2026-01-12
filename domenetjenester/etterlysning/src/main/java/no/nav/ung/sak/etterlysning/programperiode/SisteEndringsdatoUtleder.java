package no.nav.ung.sak.etterlysning.programperiode;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Dependent
public class SisteEndringsdatoUtleder {

    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public SisteEndringsdatoUtleder(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    /** Finner endret dato (start- eller sluttdato) ved å sammenligne gjeldende grunnlag med tidligere etterlysninger og initielt grunnlag.
     * <p>
     * Behovet for denne metoden oppstår fordi vi må finne ut om en dato har blitt endret fra det som bruker sist tok stilling til. Dersom vi har flere endringer på perioden der disse er av ulike typer (endring i startdato, endring i sluttdato...),
     * ønsker vi å kunne gi detaljert informasjon om hva som har blitt endret fra forrige etterlysning som enten ble besvart eller utløpt.
     * @param gjeldendeGrunnlag Det aktive grunnlaget
     * @param aktuelleGrunnlagSortert Alle aktuelle grunnlag for sammenligning sortert med nyeste først
     * @param aktuellDatoHenter Funksjon for å hente aktuell dato (start- eller sluttdato)
     * @return Evt. endret dato informasjon
     */
    static Optional<UngdomsprogramPeriodeTjeneste.EndretDato> finnSistEndretDato(UngdomsprogramPeriodeGrunnlag gjeldendeGrunnlag, List<UngdomsprogramPeriodeGrunnlag> aktuelleGrunnlagSortert, AktuellDatoHenter aktuellDatoHenter) {
        LocalDate gjeldendeDato = aktuellDatoHenter.hent(gjeldendeGrunnlag);
        boolean harEndringIDato = false;
        LocalDate forrigeDato = null;
        for (var grunnlag : aktuelleGrunnlagSortert) {
            LocalDate datoIEtterlysning = aktuellDatoHenter.hent(grunnlag);
            harEndringIDato = !datoIEtterlysning.equals(gjeldendeDato);
            if (harEndringIDato) {
                forrigeDato = datoIEtterlysning;
                break;
            }
        }

        if (harEndringIDato) {
            return Optional.of(new UngdomsprogramPeriodeTjeneste.EndretDato(gjeldendeDato, forrigeDato));
        }
        return Optional.empty();
    }


    @FunctionalInterface
    interface AktuellDatoHenter {
        LocalDate hent(UngdomsprogramPeriodeGrunnlag grunnlag);
    }

}
