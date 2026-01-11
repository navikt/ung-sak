package no.nav.ung.sak.etterlysning.programperiode;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.endretperiode.EndretPeriodeOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.endretperiode.PeriodeEndringType;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.felles.PeriodeDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.ung.kodeverk.uttak.Tid.TIDENES_ENDE;

@Dependent
public class EndretPeriodeOppgaveOppretter {

    private final UngOppgaveKlient ungOppgaveKlient;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private final EtterlysningRepository etterlysningRepository;

    @Inject
    public EndretPeriodeOppgaveOppretter(UngOppgaveKlient ungOppgaveKlient,
                                         UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                         EtterlysningRepository etterlysningRepository) {
        this.ungOppgaveKlient = ungOppgaveKlient;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.etterlysningRepository = etterlysningRepository;
    }


    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, PersonIdent deltakerIdent) {
        if (etterlysninger.isEmpty()) {
            return;
        }
        UngdomsprogramPeriodeGrunnlag initieltPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentInitiell(behandling.getId()).orElseThrow(() ->
            new IllegalStateException("Klarte ikke å innhentete originalt ungdomsprogram periodegrunnlag for behandling " + behandling.getId())
        );
        if (etterlysninger.size() > 1) {
            throw new IllegalStateException("Fant flere etterlysninger for behandling " + behandling.getId());
        }
        Etterlysning etterlysning = etterlysninger.getFirst();
        UngdomsprogramPeriodeGrunnlag gjeldendeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse());
        List<Etterlysning> sorterteEtterlysninger = etterlysningRepository.hentEtterlysningerMedSisteFørst(etterlysning.getId(), EtterlysningType.UTTALELSE_ENDRET_PERIODE);

        Optional<UngdomsprogramPeriodeTjeneste.EndretDato> endretStartDato = finnEndretDato(
            gjeldendeGrunnlag,
            sorterteEtterlysninger,
            initieltPeriodeGrunnlag,
            EndretPeriodeOppgaveOppretter::getStartdato);

        Optional<UngdomsprogramPeriodeTjeneste.EndretDato> endretSluttDato = finnEndretDato(
            gjeldendeGrunnlag,
            sorterteEtterlysninger,
            initieltPeriodeGrunnlag,
            EndretPeriodeOppgaveOppretter::getSluttdato);

        if (endretStartDato.isPresent() && endretSluttDato.isEmpty()) {
            var oppgaveDto = mapTilStartdatoOppgaveDto(etterlysning, deltakerIdent, endretStartDato.get().nyDato(), endretStartDato.get().forrigeDato());
            ungOppgaveKlient.opprettEndretStartdatoOppgave(oppgaveDto);
        } else if (endretStartDato.isEmpty() && endretSluttDato.isPresent()) {
            var oppgaveDto = mapTilSluttdatoOppgaveDto(etterlysning, deltakerIdent, endretSluttDato.get().nyDato(), endretSluttDato.get().forrigeDato());
            ungOppgaveKlient.opprettEndretSluttdatoOppgave(oppgaveDto);
        } else {
            PeriodeDTO nyPeriode = hentPeriodeFraGrunnlag(gjeldendeGrunnlag);
            PeriodeDTO forrigePeriode = hentPeriodeFraGrunnlag(initieltPeriodeGrunnlag);
            var endringer = Set.of(PeriodeEndringType.ENDRET_STARTDATO, PeriodeEndringType.ENDRET_SLUTTDATO);
            var oppgaveDto = mapTilEndretPeriodeOppgaveDto(etterlysning, deltakerIdent, nyPeriode, forrigePeriode, endringer);
            ungOppgaveKlient.opprettEndretPeriodeOppgave(oppgaveDto);
        }

    }

    /** Finner endret dato (start- eller sluttdato) ved å sammenligne gjeldende grunnlag med tidligere etterlysninger og initielt grunnlag.
     * <p>
     * Behovet for denne metoden oppstår fordi vi må finne ut om en dato har blitt endret fra det som bruker sist tok stilling til. Dersom vi har flere endringer på perioden der disse er av ulike typer (endring i startdato, endring i sluttdato...),
     * ønsker vi å kunne gi detaljert informasjon om hva som har blitt endret fra forrige etterlysning som enten ble besvart eller utløpt.
     * @param gjeldendeGrunnlag Det aktive grunnlaget
     * @param sorterteEtterlysninger Alle tidligere opprettede etterlysninger for perioden, sortert med nyeste først
     * @param initieltPeriodeGrunnlag Initielt grunnlag
     * @param aktuellDatoHenter Funksjon for å hente aktuell dato (start- eller sluttdato)
     * @return Evt. endret dato informasjon
     */
    private Optional<UngdomsprogramPeriodeTjeneste.EndretDato> finnEndretDato(UngdomsprogramPeriodeGrunnlag gjeldendeGrunnlag,
                                                                              List<Etterlysning> sorterteEtterlysninger,
                                                                              UngdomsprogramPeriodeGrunnlag initieltPeriodeGrunnlag,
                                                                              AktuellDatoHenter aktuellDatoHenter) {
        LocalDate gjeldendeDato = aktuellDatoHenter.hent(gjeldendeGrunnlag);
        boolean harEndringIDato = false;
        LocalDate forrigeDato = null;
        Optional<UngdomsprogramPeriodeTjeneste.EndretDato> endretStartDato = Optional.empty();
        // Dersom vi treffer en etterlysning som er mottatt svar eller utløpt, betyr det at bruker har tatt stilling til alle endringer før denne. Det er derfor ikke nødvendig å sjekke flere grunnlag.
        List<Etterlysning> tidligereEtterlysningerSomBleAvbruttSortert = sorterteEtterlysninger.stream().filter(it -> EtterlysningStatus.OPPRETTET != it.getStatus())
            .takeWhile(it -> it.getStatus() != EtterlysningStatus.MOTTATT_SVAR && it.getStatus() != EtterlysningStatus.UTLØPT)
            .filter(it -> it.getStatus() == EtterlysningStatus.AVBRUTT).toList();
        // Henter alle aktuelle grunnlag
        List<UngdomsprogramPeriodeGrunnlag> aktuelleGrunnlagSortert = new ArrayList<>(ungdomsprogramPeriodeRepository.hentGrunnlagFraReferanser(
            tidligereEtterlysningerSomBleAvbruttSortert.stream().map(Etterlysning::getGrunnlagsreferanse).toList()
        ));
        aktuelleGrunnlagSortert.add(initieltPeriodeGrunnlag); // Legger til initielt grunnlag sist for sjekk

        for (var grunnlag : aktuelleGrunnlagSortert) {
            LocalDate datoIEtterlysning = aktuellDatoHenter.hent(grunnlag);
            harEndringIDato = !datoIEtterlysning.equals(gjeldendeDato);
            if (harEndringIDato) {
                forrigeDato = datoIEtterlysning;
                break;
            }
        }

        if (harEndringIDato) {
            endretStartDato = Optional.of(new UngdomsprogramPeriodeTjeneste.EndretDato(gjeldendeDato, forrigeDato));
        }
        return endretStartDato;
    }

    private static LocalDate getStartdato(UngdomsprogramPeriodeGrunnlag grunnlag) {
        return grunnlag.hentForEksaktEnPeriode().getFomDato();
    }

    private static LocalDate getSluttdato(UngdomsprogramPeriodeGrunnlag grunnlag) {
        return grunnlag.hentForEksaktEnPeriode().getTomDato();
    }

    @FunctionalInterface
    private interface AktuellDatoHenter {
        LocalDate hent(UngdomsprogramPeriodeGrunnlag grunnlag);
    }

    private EndretPeriodeOppgaveDTO mapTilEndretPeriodeOppgaveDto(Etterlysning etterlysning, PersonIdent deltakerIdent, PeriodeDTO nyPeriode, PeriodeDTO forrigePeriode, Set<PeriodeEndringType> endringer) {

        return new EndretPeriodeOppgaveDTO(
            deltakerIdent.getIdent(),
            etterlysning.getEksternReferanse(),
            etterlysning.getFrist(),
            nyPeriode,
            forrigePeriode,
            endringer
        );
    }

    private PeriodeDTO hentPeriodeFraGrunnlag(UngdomsprogramPeriodeGrunnlag periodeGrunnlag) {
        LocalDate fomDato = periodeGrunnlag.getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getFomDato();
        LocalDate tomDato = periodeGrunnlag.getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getTomDato();
        return new PeriodeDTO(fomDato, tomDato);
    }

    private EndretStartdatoOppgaveDTO mapTilStartdatoOppgaveDto(Etterlysning etterlysning, PersonIdent deltakerIdent, LocalDate nyStartDato, LocalDate forrigeStartDato) {
        return new EndretStartdatoOppgaveDTO(
            deltakerIdent.getIdent(),
            etterlysning.getEksternReferanse(),
            etterlysning.getFrist(),
            nyStartDato,
            forrigeStartDato
        );
    }

    private EndretSluttdatoOppgaveDTO mapTilSluttdatoOppgaveDto(Etterlysning etterlysning, PersonIdent deltakerIdent, LocalDate nySluttDato, LocalDate forrigeSluttDato) {
        return new EndretSluttdatoOppgaveDTO(
            deltakerIdent.getIdent(),
            etterlysning.getEksternReferanse(),
            etterlysning.getFrist(),
            nySluttDato,
            forrigeSluttDato.equals(TIDENES_ENDE) ? null : forrigeSluttDato
        );
    }

}
