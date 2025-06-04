package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;

import java.util.List;

@ApplicationScoped
public class EtterlysningProssesseringTjeneste {

    private EtterlysningRepository etterlysningRepository;
    private OpprettOppgaveTjeneste opprettOppgaveTjeneste;

    private UngOppgaveKlient oppgaveKlient;

    public EtterlysningProssesseringTjeneste() {
        // CDI
    }

    @Inject
    public EtterlysningProssesseringTjeneste(EtterlysningRepository etterlysningRepository,
                                             OpprettOppgaveTjeneste opprettOppgaveTjeneste,
                                             UngOppgaveKlient oppgaveKlient) {
        this.etterlysningRepository = etterlysningRepository;
        this.opprettOppgaveTjeneste = opprettOppgaveTjeneste;
        this.oppgaveKlient = oppgaveKlient;
    }

    public void settTilUtløpt(Long behandlingId) {
        final var etterlysninger = etterlysningRepository.hentUtløpteEtterlysningerSomVenterPåSvar(behandlingId);

        settEttelysningerUtløpt(etterlysninger);
    }

    public void settEttelysningerUtløpt(List<Etterlysning> etterlysninger) {
        etterlysninger.forEach(e -> {
            oppgaveKlient.oppgaveUtløpt(e.getEksternReferanse());
            e.utløpt();
        });

        etterlysningRepository.lagre(etterlysninger);
    }

    public void settTilAvbrutt(Long behandlingId) {
        final var etterlysninger = etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandlingId);

        etterlysninger.forEach(e -> {
            oppgaveKlient.avbrytOppgave(e.getEksternReferanse());
            e.avbryt();
        });

        etterlysningRepository.lagre(etterlysninger);
    }

    public void opprett(Long behandlingId, EtterlysningType etterlysningType) {
        opprettOppgaveTjeneste.opprett(behandlingId, etterlysningType);
    }
}
