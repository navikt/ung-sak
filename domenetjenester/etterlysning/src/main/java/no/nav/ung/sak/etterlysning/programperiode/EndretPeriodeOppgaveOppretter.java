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
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

        // Dette med å finne diff kan potensielt forenkles dersom vi ikkje trenger å vise kva startdato og sluttdato var før endringen.
        List<UngdomsprogramPeriodeGrunnlag> grunnlagslisteForSammenligning = finnSortertGrunnlagslisteForSammenligning(etterlysning, initieltPeriodeGrunnlag);

        Optional<UngdomsprogramPeriodeTjeneste.EndretDato> endretStartDato = SisteEndringsdatoUtleder.finnSistEndretDato(
            gjeldendeGrunnlag,
            grunnlagslisteForSammenligning,
            EndretPeriodeOppgaveOppretter::getStartdato);

        Optional<UngdomsprogramPeriodeTjeneste.EndretDato> endretSluttDato = SisteEndringsdatoUtleder.finnSistEndretDato(
            gjeldendeGrunnlag,
            grunnlagslisteForSammenligning,
            EndretPeriodeOppgaveOppretter::getSluttdato);

        if (endretStartDato.isPresent() && endretSluttDato.isEmpty()) {
            // ENDRING AV STARTDATO
            var oppgaveDto = mapTilStartdatoOppgaveDto(etterlysning, deltakerIdent, endretStartDato.get().nyDato(), endretStartDato.get().forrigeDato());
            ungOppgaveKlient.opprettEndretStartdatoOppgave(oppgaveDto);
        } else if (endretStartDato.isEmpty() && endretSluttDato.isPresent()) {
            // ENDRING AV SLUTTDATO
            var oppgaveDto = mapTilSluttdatoOppgaveDto(etterlysning, deltakerIdent, endretSluttDato.get().nyDato(), endretSluttDato.get().forrigeDato());
            ungOppgaveKlient.opprettEndretSluttdatoOppgave(oppgaveDto);
        } else if (gjeldendeGrunnlag.hentForEksaktEnPeriodeDersomFinnes().isEmpty()) {
            // FJERNET PERIODE
            PeriodeDTO forrigePeriode = hentPeriodeFraGrunnlag(initieltPeriodeGrunnlag);
            var endringer = Set.of(PeriodeEndringType.FJERNET_PERIODE);
            var oppgaveDto = mapTilEndretPeriodeOppgaveDto(etterlysning, deltakerIdent, null, forrigePeriode, endringer);
            ungOppgaveKlient.opprettEndretPeriodeOppgave(oppgaveDto);
        } else {
            PeriodeDTO nyPeriode = hentPeriodeFraGrunnlag(gjeldendeGrunnlag);
            PeriodeDTO forrigePeriode = hentPeriodeFraGrunnlag(initieltPeriodeGrunnlag);
            var endringer = Set.of(PeriodeEndringType.ENDRET_STARTDATO, PeriodeEndringType.ENDRET_SLUTTDATO);
            var oppgaveDto = mapTilEndretPeriodeOppgaveDto(etterlysning, deltakerIdent, nyPeriode, forrigePeriode, endringer);
            ungOppgaveKlient.opprettEndretPeriodeOppgave(oppgaveDto);
        }

    }

    private List<UngdomsprogramPeriodeGrunnlag> finnSortertGrunnlagslisteForSammenligning(Etterlysning etterlysning, UngdomsprogramPeriodeGrunnlag initieltPeriodeGrunnlag) {
        List<Etterlysning> sorterteEtterlysninger = etterlysningRepository.hentEtterlysningerMedSisteFørst(etterlysning.getId(), EtterlysningType.UTTALELSE_ENDRET_PERIODE);

        // Dersom vi treffer en etterlysning som er mottatt svar eller utløpt, betyr det at bruker har tatt stilling til alle endringer før denne. Det er derfor ikke nødvendig å sjekke flere grunnlag.
        List<Etterlysning> tidligereEtterlysningerSomBleAvbruttSortert = sorterteEtterlysninger.stream().filter(it -> EtterlysningStatus.OPPRETTET != it.getStatus())
            .takeWhile(it -> it.getStatus() != EtterlysningStatus.MOTTATT_SVAR && it.getStatus() != EtterlysningStatus.UTLØPT)
            .filter(it -> it.getStatus() == EtterlysningStatus.AVBRUTT).toList();

        // Henter alle aktuelle grunnlag. Beholder rekkefølge fra etterlysningene
        List<UngdomsprogramPeriodeGrunnlag> aktuelleGrunnlagSortert = new ArrayList<>(ungdomsprogramPeriodeRepository.hentGrunnlagFraReferanser(
            tidligereEtterlysningerSomBleAvbruttSortert.stream().map(Etterlysning::getGrunnlagsreferanse).toList()
        ));
        aktuelleGrunnlagSortert.add(initieltPeriodeGrunnlag); // Legger til initielt grunnlag sist for sjekk
        return aktuelleGrunnlagSortert;
    }

    private static Optional<LocalDate> getStartdato(UngdomsprogramPeriodeGrunnlag grunnlag) {
        return grunnlag.hentForEksaktEnPeriodeDersomFinnes().map(DatoIntervallEntitet::getFomDato);
    }

    private static Optional<LocalDate> getSluttdato(UngdomsprogramPeriodeGrunnlag grunnlag) {
        return grunnlag.hentForEksaktEnPeriodeDersomFinnes().filter(it -> !it.getFomDato().equals(TIDENES_ENDE)).map(DatoIntervallEntitet::getTomDato);
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
