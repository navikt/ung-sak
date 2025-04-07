package no.nav.ung.sak.etterlysning.kontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.deltakelseopplyser.kontrakt.oppgave.registerinntekt.RegisterInntektOppgaveDTO;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.etterlysning.EtterlysningHåndterer;
import no.nav.ung.sak.etterlysning.UngOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

//TODO fjern?
@Dependent
public class InntektkontrollEtterlysningHåndterer implements EtterlysningHåndterer {

    private final EtterlysningRepository etterlysningRepository;
    private final BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private UngOppgaveKlient ungOppgaveKlient;
    private PersoninfoAdapter personinfoAdapter;

    @Inject
    public InntektkontrollEtterlysningHåndterer(EtterlysningRepository etterlysningRepository,
                                                BehandlingRepository behandlingRepository,
                                                InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                UngOppgaveKlient ungOppgaveKlient, PersoninfoAdapter personinfoAdapter) {
        this.etterlysningRepository = etterlysningRepository;
        this.behandlingRepository = behandlingRepository;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.ungOppgaveKlient = ungOppgaveKlient;
        this.personinfoAdapter = personinfoAdapter;
    }

    public void håndterOpprettelse(long behandlingId, EtterlysningType etterlysningType) {
        if (etterlysningType != EtterlysningType.UTTALELSE_KONTROLL_INNTEKT) {
            throw new IllegalArgumentException("Ikke støttet etterlysningstype: " + etterlysningType);
        }

        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        final var etterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandlingId, etterlysningType);
        AktørId aktørId = behandling.getAktørId();
        PersonIdent deltakerIdent = personinfoAdapter.hentIdentForAktørId(aktørId).orElseThrow(() -> new IllegalStateException("Fant ikke ident for aktørId"));

        etterlysninger.forEach(e -> e.vent(getFrist()));
        final var oppgaveDtoer = etterlysninger.stream().map(etterlysning -> {
            final var grunnlag = inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingId, etterlysning.getGrunnlagsreferanse());
            return new RegisterInntektOppgaveDTO(deltakerIdent.getIdent(),
                etterlysning.getEksternReferanse(),
                etterlysning.getFrist(),
                etterlysning.getPeriode().getFomDato(),
                etterlysning.getPeriode().getTomDato(),
                InntektKontrollOppgaveMapper.mapTilRegisterInntekter(grunnlag, etterlysning.getPeriode()));
        }).collect(Collectors.toList());

        oppgaveDtoer.forEach(ungOppgaveKlient::opprettKontrollerRegisterInntektOppgave);

        etterlysningRepository.lagre(etterlysninger);
    }
}
