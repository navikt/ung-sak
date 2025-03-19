package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;

import java.time.LocalDateTime;

//TODO fjern?
@Dependent
public class InntektkontrollEtterlysningHåndterer implements EtterlysningHåndterer {

    private final InntektArbeidYtelseTjeneste iayTjeneste;
    private final EtterlysningRepository etterlysningRepository;

    @Inject
    public InntektkontrollEtterlysningHåndterer(InntektArbeidYtelseTjeneste iayTjeneste,
                                                EtterlysningRepository etterlysningRepository) {
        this.iayTjeneste = iayTjeneste;
        this.etterlysningRepository = etterlysningRepository;
    }

    @Override
    public void hånterEtterlysning(long behandlingId) {
        final var grunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        final var etterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandlingId, EtterlysningType.UTTALELSE_KONTROLL_INNTEKT);
        // Kall oppgave API
        etterlysninger.forEach(e -> e.vent(LocalDateTime.now().plusDays(14)));
        etterlysningRepository.lagre(etterlysninger);
    }
}
