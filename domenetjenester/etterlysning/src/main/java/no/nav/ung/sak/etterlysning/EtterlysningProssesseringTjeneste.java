package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.etterlysning.publisering.PubliserEtterlysningTask;

import java.util.List;

@ApplicationScoped
public class EtterlysningProssesseringTjeneste {

    private EtterlysningRepository etterlysningRepository;
    private OpprettOppgaveTjeneste opprettOppgaveTjeneste;

    private UngOppgaveKlient oppgaveKlient;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    public EtterlysningProssesseringTjeneste() {
        // CDI
    }

    @Inject
    public EtterlysningProssesseringTjeneste(EtterlysningRepository etterlysningRepository,
                                             OpprettOppgaveTjeneste opprettOppgaveTjeneste,
                                             UngOppgaveKlient oppgaveKlient, ProsessTaskTjeneste prosessTaskTjeneste) {
        this.etterlysningRepository = etterlysningRepository;
        this.opprettOppgaveTjeneste = opprettOppgaveTjeneste;
        this.oppgaveKlient = oppgaveKlient;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public void settTilUtløpt(Behandling behandling) {
        final var etterlysninger = etterlysningRepository.hentUtløpteEtterlysningerSomVenterPåSvar(behandling.getId());

        settEttelysningerUtløpt(etterlysninger);
        publiser(etterlysninger);
    }

    public void settEttelysningerUtløpt(List<Etterlysning> etterlysninger) {
        etterlysninger.forEach(e -> {
            oppgaveKlient.oppgaveUtløpt(e.getEksternReferanse());
            e.utløpt();
        });

        etterlysningRepository.lagre(etterlysninger);
    }

    public void settTilAvbrutt(Behandling behandling) {
        final var etterlysninger = etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId());

        etterlysninger.forEach(e -> {
            oppgaveKlient.avbrytOppgave(e.getEksternReferanse());
            e.avbryt();
        });

        etterlysningRepository.lagre(etterlysninger);
        publiser(etterlysninger);
    }

    public void opprett(Behandling behandling, EtterlysningType etterlysningType) {
        var opprettet = opprettOppgaveTjeneste.opprett(behandling, etterlysningType);
        publiser(opprettet);
        opprettet.forEach(e -> {
            var prosessTaskData = ProsessTaskData.forProsessTask(SettEtterlysningTilUtløptDersomVenterTask.class);
            prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId());
            prosessTaskData.setNesteKjøringEtter(e.getFrist());
            prosessTaskTjeneste.lagre(prosessTaskData);
        });
    }

    private void publiser(List<Etterlysning> etterlysninger) {
        var gruppe = new ProsessTaskGruppe();
        etterlysninger.forEach(e -> {
                var prosessTaskData = ProsessTaskData.forProsessTask(PubliserEtterlysningTask.class);
                prosessTaskData.setProperty(PubliserEtterlysningTask.ETTERLYSNING_ID, e.getId().toString());
                gruppe.addNesteSekvensiell(prosessTaskData);
            }
        );
        prosessTaskTjeneste.lagre(gruppe);
    }
}
