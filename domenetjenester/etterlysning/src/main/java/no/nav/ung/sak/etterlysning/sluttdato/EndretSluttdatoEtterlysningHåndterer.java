package no.nav.ung.sak.etterlysning.sluttdato;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.startdato.EndretSluttdatoOppgaveDTO;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.EtterlysningHåndterer;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static no.nav.ung.kodeverk.uttak.Tid.TIDENES_ENDE;

@Dependent
public class EndretSluttdatoEtterlysningHåndterer implements EtterlysningHåndterer {

    private final EtterlysningRepository etterlysningRepository;
    private final BehandlingRepository behandlingRepository;
    private UngOppgaveKlient ungOppgaveKlient;
    private PersoninfoAdapter personinfoAdapter;
    private final Duration ventePeriode;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public EndretSluttdatoEtterlysningHåndterer(EtterlysningRepository etterlysningRepository,
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
        AktørId aktørId = behandling.getAktørId();
        PersonIdent deltakerIdent = personinfoAdapter.hentIdentForAktørId(aktørId).orElseThrow(() -> new IllegalStateException("Fant ikke ident for aktørId"));
        final var originalePerioder = behandling.getOriginalBehandlingId().flatMap(ungdomsprogramPeriodeRepository::hentGrunnlag).stream().map(UngdomsprogramPeriodeGrunnlag::getUngdomsprogramPerioder)
            .map(UngdomsprogramPerioder::getPerioder)
            .flatMap(Collection::stream)
            .map(UngdomsprogramPeriode::getPeriode)
            .toList();
        etterlysninger.forEach(e -> e.vent(getFrist()));
        final var oppgaveDtoer = etterlysninger.stream().map(etterlysning -> new EndretSluttdatoOppgaveDTO(
                deltakerIdent.getIdent(),
                etterlysning.getEksternReferanse(),
                etterlysning.getFrist(),
                hentSluttdato(etterlysning),
                finnOriginalSluttdato(originalePerioder)
            )
        ).toList();

        oppgaveDtoer.forEach(ungOppgaveKlient::opprettEndretSluttdatoOppgave);

        etterlysningRepository.lagre(etterlysninger);
    }

    private static LocalDate finnOriginalSluttdato(List<DatoIntervallEntitet> originalePerioder) {
        var originalSluttdato = originalePerioder.iterator().next().getTomDato();
        return originalSluttdato.equals(TIDENES_ENDE) ? null : originalSluttdato;
    }

    private LocalDate hentSluttdato(Etterlysning etterlysning) {
        return ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse())
            .orElseThrow(() -> new IllegalStateException("Forventer å finne grunnlag for etterlysning med grunnlagsreferanse: " + etterlysning.getGrunnlagsreferanse()))
            .getUngdomsprogramPerioder().getPerioder().iterator().next().getPeriode().getTomDato();
    }

    @Override
    public LocalDateTime getFrist() {
        return LocalDateTime.now().plus(ventePeriode);
    }

}
