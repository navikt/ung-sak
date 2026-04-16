package no.nav.ung.sak.etterlysning.programperiode;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OppgaveYtelsetype;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.endretperiode.EndretPeriodeDataDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.endretperiode.PeriodeDTO;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.endretperiode.PeriodeEndringType;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.endretsluttdato.EndretSluttdatoDataDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.endretstartdato.EndretStartdatoDataDto;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.etterlysning.OppgaveYtelsetypeMapper;
import no.nav.ung.sak.etterlysning.UngBrukerdialogOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

@Dependent
public class EndretPeriodeOppgaveOppretter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EndretPeriodeOppgaveOppretter.class);

    private final UngBrukerdialogOppgaveKlient oppgaveKlient;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private final EtterlysningRepository etterlysningRepository;
    private final UngdomsytelseStartdatoRepository startdatoRepository;

    @Inject
    public EndretPeriodeOppgaveOppretter(UngBrukerdialogOppgaveKlient oppgaveKlient,
                                         UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                         EtterlysningRepository etterlysningRepository,
                                         UngdomsytelseStartdatoRepository startdatoRepository) {
        this.oppgaveKlient = oppgaveKlient;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.etterlysningRepository = etterlysningRepository;
        this.startdatoRepository = startdatoRepository;
    }


    public void opprettOppgave(Behandling behandling, List<Etterlysning> etterlysninger, AktørId aktørId) {
        if (etterlysninger.isEmpty()) {
            return;
        }
        OppgaveYtelsetype ytelsetype = OppgaveYtelsetypeMapper.mapTilOppgaveYtelsetype(behandling.getFagsak().getYtelseType());
        UngdomsprogramPeriodeGrunnlag initieltPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentInitiell(behandling.getId()).orElseThrow(() ->
            new IllegalStateException("Klarte ikke å innhentete originalt ungdomsprogram periodegrunnlag for behandling " + behandling.getId())
        );
        if (etterlysninger.size() > 1) {
            throw new IllegalStateException("Fant flere etterlysninger for behandling " + behandling.getId());
        }
        Etterlysning etterlysning = etterlysninger.getFirst();
        UngdomsprogramPeriodeGrunnlag gjeldendeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse());
        Optional<UngdomsytelseStartdatoGrunnlag> startdatoGrunnlag = startdatoRepository.hentGrunnlag(behandling.getId());

        // Dette med å finne diff kan potensielt forenkles dersom vi ikkje trenger å vise kva startdato og sluttdato var før endringen.
        List<PeriodeSnapshot> snapshotsForSammenligning = finnSortertSnapshotlisteForSammenligning(etterlysning, initieltPeriodeGrunnlag, startdatoGrunnlag);

        PeriodeSnapshot gjeldendeSnapshot = PeriodeSnapshot.fraGrunnlag(gjeldendeGrunnlag);

        log.info("Utleder endringer fra grunnlag med referanse {} basert på følgende snapshots for sammenligning: {}",
            gjeldendeGrunnlag.getGrunnlagsreferanse(),
            snapshotsForSammenligning.stream().map(PeriodeSnapshot::grunnlagsreferanse).toList());

        Optional<SisteEndringsdatoUtleder.EndretDato> endretStartDato = SisteEndringsdatoUtleder.finnSistEndretDato(
            gjeldendeSnapshot,
            snapshotsForSammenligning,
            PeriodeSnapshot::fomDato);

        Optional<SisteEndringsdatoUtleder.EndretDato> endretSluttDato = SisteEndringsdatoUtleder.finnSistEndretDato(
            gjeldendeSnapshot,
            snapshotsForSammenligning,
            s -> s.tomDato().filter(d -> !d.equals(TIDENES_ENDE)));

        if (endretStartDato.isPresent() && endretSluttDato.isEmpty()) {
            // ENDRING AV STARTDATO
            log.info("Fant kun endring i startdato for etterlysning {}. Ny startdato og grunnlag: {}, forrige startdato og grunnlag: {}",
                etterlysning.getEksternReferanse(),
                endretStartDato.get().nyDatoOgGrunnlag(),
                endretStartDato.get().forrigeDatoOgGrunnlag());
            var oppgaveDto = mapTilStartdatoOppgaveDto(etterlysning, aktørId, ytelsetype, endretStartDato.get().nyDatoOgGrunnlag().dato(), endretStartDato.get().forrigeDatoOgGrunnlag().dato());
            oppgaveKlient.opprettOppgave(oppgaveDto);
        } else if (endretStartDato.isEmpty() && endretSluttDato.isPresent()) {
            // ENDRING AV SLUTTDATO
            log.info("Fant kun endring i sluttdato for etterlysning {}. Ny sluttdato og grunnlag: {}, forrige sluttdato og grunnlag: {}",
                etterlysning.getEksternReferanse(),
                endretSluttDato.get().nyDatoOgGrunnlag(),
                endretSluttDato.get().forrigeDatoOgGrunnlag());
            var oppgaveDto = mapTilSluttdatoOppgaveDto(etterlysning, aktørId, ytelsetype, endretSluttDato.get().nyDatoOgGrunnlag().dato(), endretSluttDato.get().forrigeDatoOgGrunnlag().dato());
            oppgaveKlient.opprettOppgave(oppgaveDto);
        } else if (gjeldendeGrunnlag.hentForEksaktEnPeriodeDersomFinnes().isEmpty()) {
            // FJERNET PERIODE
            PeriodeDTO forrigePeriode = hentPeriodeFraGrunnlag(initieltPeriodeGrunnlag);
            var oppgaveDto = mapTilFjernetPeriodeOppgaveDto(etterlysning, aktørId, ytelsetype, forrigePeriode);
            oppgaveKlient.opprettOppgave(oppgaveDto);
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
            var oppgaveDto = mapTilEndretPeriodeOppgaveDto(etterlysning, aktørId, ytelsetype, nyPeriode, forrigePeriode, endringer);
            oppgaveKlient.opprettOppgave(oppgaveDto);
        } else {
            throw new IllegalStateException("Fant ingen endringer som kunne mappes til oppgave for etterlysning " + etterlysning.getEksternReferanse());
        }

    }

    private List<PeriodeSnapshot> finnSortertSnapshotlisteForSammenligning(
        Etterlysning etterlysning,
        UngdomsprogramPeriodeGrunnlag initieltPeriodeGrunnlag,
        Optional<UngdomsytelseStartdatoGrunnlag> startdatoGrunnlag) {
        List<Etterlysning> sorterteEtterlysninger = etterlysningRepository.hentEtterlysningerMedSisteFørst(etterlysning.getId(), EtterlysningType.UTTALELSE_ENDRET_PERIODE);

        // Dersom vi treffer en etterlysning som er mottatt svar eller utløpt, betyr det at bruker har tatt stilling til alle endringer før denne. Det er derfor ikke nødvendig å sjekke flere grunnlag.
        List<Etterlysning> tidligereEtterlysningerSomBleAvbruttSortert = sorterteEtterlysninger.stream()
            .takeWhile(it -> it.getStatus() != EtterlysningStatus.MOTTATT_SVAR && it.getStatus() != EtterlysningStatus.UTLØPT)
            .filter(it -> it.getStatus() == EtterlysningStatus.AVBRUTT).toList();

        // Henter alle aktuelle grunnlag og konverterer til snapshots. Beholder rekkefølge fra etterlysningene.
        List<PeriodeSnapshot> snapshotsSortert = new ArrayList<>(ungdomsprogramPeriodeRepository.hentGrunnlagFraReferanser(
            tidligereEtterlysningerSomBleAvbruttSortert.stream().map(Etterlysning::getGrunnlagsreferanse).toList()
        ).stream().map(PeriodeSnapshot::fraGrunnlag).toList());

        snapshotsSortert.add(PeriodeSnapshot.fraGrunnlag(initieltPeriodeGrunnlag)); // Legger til initielt grunnlag for sjekk

        // Oppgitt startdato (fra søknaden) legges til sist.
        // Dette håndterer caset der perioden endres mellom søknadstidspunkt og innhenting: kun ett grunnlag finnes, men startdato er endret.
        startdatoGrunnlag
            .stream()
            .map(UngdomsytelseStartdatoGrunnlag::getOppgitteStartdatoer)
            .map(UngdomsytelseStartdatoer::getStartdatoer)
            .flatMap(Collection::stream)
            .map(UngdomsytelseSøktStartdato::getStartdato)
            .min(Comparator.comparing(d -> BigDecimal.valueOf(Period.between(d, etterlysning.getPeriode().getFomDato()).getDays()).abs())) // Finner startdato nærmest aktuell periode
            .map(PeriodeSnapshot::fraOppgittStartdato)
            .ifPresent(snapshotsSortert::add);

        return snapshotsSortert;
    }

    private OpprettOppgaveDto mapTilEndretPeriodeOppgaveDto(Etterlysning etterlysning, AktørId aktørId, OppgaveYtelsetype ytelsetype, PeriodeDTO nyPeriode, PeriodeDTO forrigePeriode, Set<PeriodeEndringType> endringer) {
        return new OpprettOppgaveDto(
            new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
            ytelsetype,
            etterlysning.getEksternReferanse(),
            new EndretPeriodeDataDto(nyPeriode, forrigePeriode, endringer),
            etterlysning.getFrist()
        );
    }

    private OpprettOppgaveDto mapTilFjernetPeriodeOppgaveDto(Etterlysning etterlysning, AktørId aktørId, OppgaveYtelsetype ytelsetype, PeriodeDTO forrigePeriode) {
        return new OpprettOppgaveDto(
            new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
            ytelsetype,
            etterlysning.getEksternReferanse(),
            new EndretPeriodeDataDto(null, forrigePeriode, Set.of(PeriodeEndringType.FJERNET_PERIODE)),
            etterlysning.getFrist()
        );
    }

    private PeriodeDTO hentPeriodeFraGrunnlag(UngdomsprogramPeriodeGrunnlag periodeGrunnlag) {
        LocalDate fomDato = periodeGrunnlag.getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getFomDato();
        LocalDate tomDato = periodeGrunnlag.getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getTomDato();
        return new PeriodeDTO(fomDato, tomDato);
    }

    private OpprettOppgaveDto mapTilStartdatoOppgaveDto(Etterlysning etterlysning, AktørId aktørId, OppgaveYtelsetype ytelsetype, LocalDate nyStartDato, LocalDate forrigeStartDato) {
        return new OpprettOppgaveDto(
            new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
            ytelsetype,
            etterlysning.getEksternReferanse(),
            new EndretStartdatoDataDto(nyStartDato, forrigeStartDato),
            etterlysning.getFrist()
        );
    }

    private OpprettOppgaveDto mapTilSluttdatoOppgaveDto(Etterlysning etterlysning, AktørId aktørId, OppgaveYtelsetype ytelsetype, LocalDate nySluttDato, LocalDate forrigeSluttDato) {
        return new OpprettOppgaveDto(
            new no.nav.ung.brukerdialog.typer.AktørId(aktørId.getAktørId()),
            ytelsetype,
            etterlysning.getEksternReferanse(),
            new EndretSluttdatoDataDto(nySluttDato, TIDENES_ENDE.equals(forrigeSluttDato) ? null : forrigeSluttDato),
            etterlysning.getFrist()
        );
    }

}
