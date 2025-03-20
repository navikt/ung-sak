package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;

import java.time.LocalDateTime;

//TODO fjern?
@Dependent
public class InntektkontrollEtterlysningHåndterer implements EtterlysningHåndterer {

    private final EtterlysningRepository etterlysningRepository;

    @Inject
    public InntektkontrollEtterlysningHåndterer(EtterlysningRepository etterlysningRepository) {
        this.etterlysningRepository = etterlysningRepository;
    }

    public void håndterOpprettelse(long behandlingId) {
        final var etterlysninger = etterlysningRepository.hentOpprettetEtterlysninger(behandlingId, EtterlysningType.UTTALELSE_KONTROLL_INNTEKT);
        // Kall oppgave API
        etterlysninger.forEach(e -> e.vent(LocalDateTime.now().plusDays(14)));
        etterlysningRepository.lagre(etterlysninger);
    }
}
