package no.nav.ung.sak.etterlysning.ungdomsprogramperiode;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.periodeendring.EndretProgamperiodeOppgaveDTO;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.periodeendring.ProgramperiodeDTO;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.etterlysning.EtterlysningHåndterer;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;

@Dependent
public class EndretUngdomsprogramperiodeEtterlysningHåndterer implements EtterlysningHåndterer {

    private final EtterlysningRepository etterlysningRepository;
    private final BehandlingRepository behandlingRepository;
    private UngOppgaveKlient ungOppgaveKlient;
    private PersoninfoAdapter personinfoAdapter;
    private final Duration ventePeriode;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public EndretUngdomsprogramperiodeEtterlysningHåndterer(EtterlysningRepository etterlysningRepository,
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

    public void håndterOpprettelse(long behandlingId, EtterlysningType etterlysningType) {
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        final var etterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandlingId, etterlysningType);
        AktørId aktørId = behandling.getAktørId();
        PersonIdent deltakerIdent = personinfoAdapter.hentIdentForAktørId(aktørId).orElseThrow(() -> new IllegalStateException("Fant ikke ident for aktørId"));
        final var originalePerioder = behandling.getOriginalBehandlingId().flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag).stream().map(UngdomsprogramPeriodeGrunnlag::getUngdomsprogramPerioder)
            .map(UngdomsprogramPerioder::getPerioder)
            .flatMap(Collection::stream)
            .map(UngdomsprogramPeriode::getPeriode)
            .toList();
        etterlysninger.forEach(e -> e.vent(getFrist()));
        final var oppgaveDtoer = etterlysninger.stream().map(etterlysning -> new EndretProgamperiodeOppgaveDTO(
                deltakerIdent.getIdent(),
                etterlysning.getEksternReferanse(),
                etterlysning.getFrist(),
                new ProgramperiodeDTO(etterlysning.getPeriode().getFomDato(), etterlysning.getPeriode().getTomDato()),
                originalePerioder.stream().filter(p -> p.overlapper(etterlysning.getPeriode())).findFirst().map(it -> new ProgramperiodeDTO(it.getFomDato(), it.getTomDato())).orElse(null)
            )
        ).toList();

        oppgaveDtoer.forEach(ungOppgaveKlient::opprettEndretSluttdatoOppgave);

        etterlysningRepository.lagre(etterlysninger);
    }

    @Override
    public LocalDateTime getFrist() {
        return LocalDateTime.now().plus(ventePeriode);
    }

}
