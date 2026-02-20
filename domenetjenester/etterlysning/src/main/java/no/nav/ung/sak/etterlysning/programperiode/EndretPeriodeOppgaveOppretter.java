package no.nav.ung.sak.etterlysning.programperiode;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.etterlysning.MidlertidigOppgaveDelegeringTjeneste;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.OpprettEndretPeriodeOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.PeriodeDTO;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretperiode.PeriodeEndringType;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretsluttdato.OpprettEndretSluttdatoOppgaveDto;
import no.nav.ung.sak.kontrakt.oppgaver.typer.endretstartdato.OpprettEndretStartdatoOppgaveDto;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static no.nav.ung.sak.tid.AbstractLocalDateInterval.TIDENES_ENDE;

@Dependent
public class EndretPeriodeOppgaveOppretter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EndretPeriodeOppgaveOppretter.class);

    private final MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private final EtterlysningRepository etterlysningRepository;

    @Inject
    public EndretPeriodeOppgaveOppretter(MidlertidigOppgaveDelegeringTjeneste delegeringTjeneste,
                                         UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                         EtterlysningRepository etterlysningRepository) {
        this.delegeringTjeneste = delegeringTjeneste;
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

        log.info("Utleder endringer fra grunnlag med referanse {} basert på følgende grunnlag for sammenligning: {}",
            gjeldendeGrunnlag.getGrunnlagsreferanse(),
            grunnlagslisteForSammenligning.stream().map(UngdomsprogramPeriodeGrunnlag::getGrunnlagsreferanse).toList());

        Optional<SisteEndringsdatoUtleder.EndretDato> endretStartDato = SisteEndringsdatoUtleder.finnSistEndretDato(
            gjeldendeGrunnlag,
            grunnlagslisteForSammenligning,
            EndretPeriodeOppgaveOppretter::getStartdato);

        Optional<SisteEndringsdatoUtleder.EndretDato> endretSluttDato = SisteEndringsdatoUtleder.finnSistEndretDato(
            gjeldendeGrunnlag,
            grunnlagslisteForSammenligning,
            EndretPeriodeOppgaveOppretter::getSluttdato);

        if (endretStartDato.isPresent() && endretSluttDato.isEmpty()) {
            // ENDRING AV STARTDATO
            log.info("Fant kun endring i startdato for etterlysning {}. Ny startdato og grunnlag: {}, forrige startdato og grunnlag: {}",
                etterlysning.getEksternReferanse(),
                endretStartDato.get().nyDatoOgGrunnlag(),
                endretStartDato.get().forrigeDatoOgGrunnlag());
            var oppgaveDto = mapTilStartdatoOppgaveDto(etterlysning, deltakerIdent, endretStartDato.get().nyDatoOgGrunnlag().dato(), endretStartDato.get().forrigeDatoOgGrunnlag().dato());
            delegeringTjeneste.opprettEndretStartdatoOppgave(oppgaveDto);
        } else if (endretStartDato.isEmpty() && endretSluttDato.isPresent()) {
            // ENDRING AV SLUTTDATO
            log.info("Fant kun endring i sluttdato for etterlysning {}. Ny sluttdato og grunnlag: {}, forrige sluttdato og grunnlag: {}",
                etterlysning.getEksternReferanse(),
                endretSluttDato.get().nyDatoOgGrunnlag(),
                endretSluttDato.get().forrigeDatoOgGrunnlag());
            var oppgaveDto = mapTilSluttdatoOppgaveDto(etterlysning, deltakerIdent, endretSluttDato.get().nyDatoOgGrunnlag().dato(), endretSluttDato.get().forrigeDatoOgGrunnlag().dato());
            delegeringTjeneste.opprettEndretSluttdatoOppgave(oppgaveDto);
        } else if (gjeldendeGrunnlag.hentForEksaktEnPeriodeDersomFinnes().isEmpty()) {
            // FJERNET PERIODE
            PeriodeDTO forrigePeriode = hentPeriodeFraGrunnlag(initieltPeriodeGrunnlag);
            var endringer = Set.of(PeriodeEndringType.FJERNET_PERIODE);
            var oppgaveDto = mapTilEndretPeriodeOppgaveDto(etterlysning, deltakerIdent, null, forrigePeriode, endringer);
            delegeringTjeneste.opprettEndretPeriodeOppgave(oppgaveDto);
        } else if (endretStartDato.isPresent() && endretSluttDato.isPresent()) {
            log.info("Fant endring i både start og slutt for etterlysning {}. Ny sluttdato og grunnlag: {}, forrige sluttdato og grunnlag: {}. Ny startdato og grunnlag: {}, forrige startdato og grunnlag: {}.",
                etterlysning.getEksternReferanse(),
                endretSluttDato.get().nyDatoOgGrunnlag(),
            endretSluttDato.get().forrigeDatoOgGrunnlag(),
            endretStartDato.get().nyDatoOgGrunnlag(),
            endretStartDato.get().forrigeDatoOgGrunnlag());
            PeriodeDTO nyPeriode = hentPeriodeFraGrunnlag(gjeldendeGrunnlag);
            PeriodeDTO forrigePeriode = hentPeriodeFraGrunnlag(initieltPeriodeGrunnlag);
            var endringer = Set.of(PeriodeEndringType.ENDRET_STARTDATO, PeriodeEndringType.ENDRET_SLUTTDATO);
            var oppgaveDto = mapTilEndretPeriodeOppgaveDto(etterlysning, deltakerIdent, nyPeriode, forrigePeriode, endringer);
            delegeringTjeneste.opprettEndretPeriodeOppgave(oppgaveDto);
        } else {
            throw new IllegalStateException("Fant ingen endringer som kunne mappes til oppgave for etterlysning " + etterlysning.getEksternReferanse());
        }

    }

    private List<UngdomsprogramPeriodeGrunnlag> finnSortertGrunnlagslisteForSammenligning(Etterlysning etterlysning, UngdomsprogramPeriodeGrunnlag initieltPeriodeGrunnlag) {
        List<Etterlysning> sorterteEtterlysninger = etterlysningRepository.hentEtterlysningerMedSisteFørst(etterlysning.getId(), EtterlysningType.UTTALELSE_ENDRET_PERIODE);

        // Dersom vi treffer en etterlysning som er mottatt svar eller utløpt, betyr det at bruker har tatt stilling til alle endringer før denne. Det er derfor ikke nødvendig å sjekke flere grunnlag.
        List<Etterlysning> tidligereEtterlysningerSomBleAvbruttSortert = sorterteEtterlysninger.stream()
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
        return grunnlag.hentForEksaktEnPeriodeDersomFinnes().filter(it -> !it.getTomDato().equals(TIDENES_ENDE)).map(DatoIntervallEntitet::getTomDato);
    }

    private OpprettEndretPeriodeOppgaveDto mapTilEndretPeriodeOppgaveDto(Etterlysning etterlysning, PersonIdent deltakerIdent, PeriodeDTO nyPeriode, PeriodeDTO forrigePeriode, Set<PeriodeEndringType> endringer) {
        return new OpprettEndretPeriodeOppgaveDto(
            deltakerIdent.getIdent(),
            etterlysning.getEksternReferanse(),
            nyPeriode,
            forrigePeriode,
            endringer,
            etterlysning.getFrist()
        );
    }

    private PeriodeDTO hentPeriodeFraGrunnlag(UngdomsprogramPeriodeGrunnlag periodeGrunnlag) {
        LocalDate fomDato = periodeGrunnlag.getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getFomDato();
        LocalDate tomDato = periodeGrunnlag.getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getTomDato();
        return new PeriodeDTO(fomDato, tomDato);
    }

    private OpprettEndretStartdatoOppgaveDto mapTilStartdatoOppgaveDto(Etterlysning etterlysning, PersonIdent deltakerIdent, LocalDate nyStartDato, LocalDate forrigeStartDato) {
        return new OpprettEndretStartdatoOppgaveDto(
            deltakerIdent.getIdent(),
            etterlysning.getEksternReferanse(),
            nyStartDato,
            forrigeStartDato,
            etterlysning.getFrist()
        );
    }

    private OpprettEndretSluttdatoOppgaveDto mapTilSluttdatoOppgaveDto(Etterlysning etterlysning, PersonIdent deltakerIdent, LocalDate nySluttDato, LocalDate forrigeSluttDato) {
        return new OpprettEndretSluttdatoOppgaveDto(
            deltakerIdent.getIdent(),
            etterlysning.getEksternReferanse(),
            nySluttDato,
            forrigeSluttDato.equals(TIDENES_ENDE) ? null : forrigeSluttDato,
            etterlysning.getFrist()
        );
    }

}
