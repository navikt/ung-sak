package no.nav.ung.sak.etterlysning.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.etterlysning.EtterlysningHåndterer;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

//TODO fjern?
@Dependent
public class InntektkontrollEtterlysningHåndterer implements EtterlysningHåndterer {

    private final EtterlysningRepository etterlysningRepository;
    private final BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private UngOppgaveKlient ungOppgaveKlient;

    @Inject
    public InntektkontrollEtterlysningHåndterer(EtterlysningRepository etterlysningRepository,
                                                BehandlingRepository behandlingRepository,
                                                InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                UngOppgaveKlient ungOppgaveKlient) {
        this.etterlysningRepository = etterlysningRepository;
        this.behandlingRepository = behandlingRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.ungOppgaveKlient = ungOppgaveKlient;
    }

    public void håndterOpprettelse(long behandlingId) {
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        final var etterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandlingId, EtterlysningType.UTTALELSE_KONTROLL_INNTEKT);
        etterlysninger.forEach(e -> e.vent(getFrist()));
        final var oppgaveDtoer = etterlysninger.stream().map(etterlysning -> {
            final var grunnlag = inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingId, etterlysning.getGrunnlagsreferanse());
            return new RegisterInntektOppgaveDTO(behandling.getAktørId().getAktørId(),
                etterlysning.getEksternReferanse(),
                etterlysning.getFrist(),
                etterlysning.getPeriode().getFomDato(),
                etterlysning.getPeriode().getTomDato(),
                InntektKontrollOppgaveMapper.mapTilRegisterInntekter(grunnlag, etterlysning.getPeriode()));
        }).collect(Collectors.toList());

        oppgaveDtoer.forEach(ungOppgaveKlient::opprettOppgave);

        // Kall oppgave API
        etterlysningRepository.lagre(etterlysninger);
    }

    private static LocalDateTime getFrist() {
        return LocalDateTime.now().plusDays(14);
    }

}
