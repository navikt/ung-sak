package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.etterlysning.kontroll.InntektkontrollEtterlysningHåndterer;

import java.util.Objects;

@ApplicationScoped
public class EtterlysningProssesseringTjeneste {

    private EtterlysningRepository etterlysningRepository;
    private InntektkontrollEtterlysningHåndterer inntektkontrollEtterlysningOppretter;
    private UngOppgaveKlient oppgaveKlient;

    public EtterlysningProssesseringTjeneste() {
        // CDI
    }

    @Inject
    public EtterlysningProssesseringTjeneste(EtterlysningRepository etterlysningRepository,
                                             InntektkontrollEtterlysningHåndterer inntektkontrollEtterlysningOppretter, UngOppgaveKlient oppgaveKlient) {
        this.etterlysningRepository = etterlysningRepository;
        this.inntektkontrollEtterlysningOppretter = inntektkontrollEtterlysningOppretter;
        this.oppgaveKlient = oppgaveKlient;
    }

    public void settTilUtløpt(Long behandlingId) {
        final var etterlysninger = etterlysningRepository.hentUtløpteEtterlysningerSomVenterPåSvar(behandlingId);
        // Kall oppgave API

        etterlysninger.forEach(Etterlysning::utløpt);
        etterlysningRepository.lagre(etterlysninger);
    }

    public void settTilAvbrutt(Long behandlingId) {
        final var etterlysninger = etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandlingId);
        // Kall oppgave API

        etterlysninger.forEach(e -> oppgaveKlient.avbrytOppgave(e.getEksternReferanse()));

        etterlysninger.forEach(Etterlysning::avbryt);
        etterlysningRepository.lagre(etterlysninger);
    }

    public void opprett(Long behandlingId, EtterlysningType etterlysningType) {
        if (Objects.requireNonNull(etterlysningType) == EtterlysningType.UTTALELSE_KONTROLL_INNTEKT) {
            inntektkontrollEtterlysningOppretter.håndterOpprettelse(behandlingId);
        } else {
            throw new IllegalArgumentException("Ukjent etterlysningstype: " + etterlysningType);
        }
    }


}
