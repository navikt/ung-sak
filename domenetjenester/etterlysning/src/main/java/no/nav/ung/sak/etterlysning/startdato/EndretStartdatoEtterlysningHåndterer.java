package no.nav.ung.sak.etterlysning.startdato;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretStartdatoOppgaveDTO;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.EtterlysningHåndterer;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Dependent
public class EndretStartdatoEtterlysningHåndterer implements EtterlysningHåndterer {

    private final EtterlysningRepository etterlysningRepository;
    private final BehandlingRepository behandlingRepository;
    private UngOppgaveKlient ungOppgaveKlient;
    private PersoninfoAdapter personinfoAdapter;
    private final Duration ventePeriode;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public EndretStartdatoEtterlysningHåndterer(EtterlysningRepository etterlysningRepository,
                                                BehandlingRepository behandlingRepository,
                                                UngOppgaveKlient ungOppgaveKlient, PersoninfoAdapter personinfoAdapter,
                                                @KonfigVerdi(value = "VENTEFRIST_UTTALELSE", defaultVerdi = "P14D") String ventePeriode,
                                                UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.etterlysningRepository = etterlysningRepository;
        this.behandlingRepository = behandlingRepository;
        this.ungOppgaveKlient = ungOppgaveKlient;
        this.personinfoAdapter = personinfoAdapter;
        this.ventePeriode = Duration.parse(ventePeriode);
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    @Override
    public void håndterOpprettelse(long behandlingId, EtterlysningType etterlysningType) {
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        final var etterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandlingId, etterlysningType);
        PersonIdent deltakerIdent = personinfoAdapter.hentIdentForAktørId(behandling.getAktørId()).orElseThrow(() -> new IllegalStateException("Fant ikke ident for aktørId"));
        final var originalPeriode = behandling.getOriginalBehandlingId().flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag).map(UngdomsprogramPeriodeGrunnlag::hentForEksaktEnPeriode);
        etterlysninger.forEach(e -> e.vent(getFrist()));
        final var oppgaveDtoer = etterlysninger.stream().map(etterlysning -> new EndretStartdatoOppgaveDTO(
                deltakerIdent.getIdent(),
                etterlysning.getEksternReferanse(),
                etterlysning.getFrist(),
                hentStartdato(etterlysning),
                originalPeriode.map(DatoIntervallEntitet::getFomDato).orElseThrow((() -> new IllegalStateException("Forventer å finne original startdato")))
            )
        ).toList();

        oppgaveDtoer.forEach(ungOppgaveKlient::opprettEndretStartdatoOppgave);

        etterlysningRepository.lagre(etterlysninger);
    }

    private LocalDate hentStartdato(Etterlysning etterlysning) {
        return ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse())
            .orElseThrow(() -> new IllegalStateException("Forventer å finne startdato for etterlysning med grunnlagsreferanse: " + etterlysning.getGrunnlagsreferanse()))
            .getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getFomDato();
    }

    @Override
    public LocalDateTime getFrist() {
        return LocalDateTime.now().plus(ventePeriode);
    }

}
