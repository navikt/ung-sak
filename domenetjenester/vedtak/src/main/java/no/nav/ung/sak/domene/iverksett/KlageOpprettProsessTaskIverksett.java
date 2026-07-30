package no.nav.ung.sak.domene.iverksett;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingAnsvarlig;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Optional;

import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

@ApplicationScoped
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@BehandlingTypeRef(BehandlingType.KLAGE)
public class KlageOpprettProsessTaskIverksett implements OpprettProsessTaskIverksett {

    private static final String BESKRIVELSESTEKST = "Vedtaket er opphevet eller omgjort. Opprett en ny behandling.";
    private static final Logger LOGGER = LoggerFactory.getLogger(KlageOpprettProsessTaskIverksett.class);

    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private KlageRepository klageRepository;

    KlageOpprettProsessTaskIverksett() {
        // for CDI proxy
    }

    @Inject
    public KlageOpprettProsessTaskIverksett(FagsakProsessTaskRepository fagsakProsessTaskRepository, BehandlingAnsvarligRepository behandlingAnsvarligRepository,
                                            KlageRepository klageRepository,
                                            OppgaveTjeneste oppgaveTjeneste) {
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
        this.klageRepository = klageRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    public void opprettIverksettingstasker(Behandling behandling) {
        opprettIverksettingstasker(behandling, Optional.empty());
    }

    @Override
    public void opprettIverksettingstasker(Behandling behandling, Optional<String> initiellTaskNavn) {
        ProsessTaskData avsluttBehandling = ProsessTaskData.forProsessTask(AvsluttBehandlingTask.class);
        var taskerFørAvsluttBehandling = new ArrayList<ProsessTaskData>(2);

        Optional<ProsessTaskData> avsluttOppgave = oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling);
        avsluttOppgave.ifPresent(taskerFørAvsluttBehandling::add);

        ProsessTaskGruppe taskGruppe = new ProsessTaskGruppe();
        taskGruppe.addNesteParallell(taskerFørAvsluttBehandling);
        taskGruppe.addNesteSekvensiell(avsluttBehandling);

        OrganisasjonsEnhet enhet = behandlingAnsvarligRepository.hentBehandlingAnsvarlig(behandling.getId())
            .map(BehandlingAnsvarlig::getBehandlendeOrganisasjonsEnhet)
            .orElse(null);

        opprettTaskDataForKlage(behandling, enhet).ifPresent(taskGruppe::addNesteSekvensiell);

        taskGruppe.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        taskGruppe.setCallIdFraEksisterende();

        var fagsakId = behandling.getFagsakId();
        var behandlingId = behandling.getId();
        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId.toString(), taskGruppe);
    }

    private Optional<ProsessTaskData> opprettTaskDataForKlage(Behandling behandling, OrganisasjonsEnhet enhet) {
        KlageUtredningEntitet klageUtredning = klageRepository.hentKlageUtredning(behandling.getId());

        return klageUtredning.hentKlageVurderingType(KlageVurdertAv.KLAGEINSTANS)
            .map((vurderingTypeKabal) -> {
                String kabalVurdering = "Klagen er ferdigbehandlet av klageinstansen med vurdering: " + vurderingTypeKabal.getNavn();
                return opprettVKYoppgave(enhet, kabalVurdering);
            }).or(() -> {
                var klageVurderingType = klageUtredning.hentGjeldendeKlagevurderingType();
                if (klageVurderingType == KlageVurderingType.MEDHOLD_I_KLAGE) {
                    return Optional.of(opprettVKYoppgave(enhet, BESKRIVELSESTEKST));
                } else {
                    return Optional.empty();
                }
            });
    }

    private ProsessTaskData opprettVKYoppgave(OrganisasjonsEnhet enhet, String vurderingTekst) {
        ProsessTaskData opprettOppgave = ProsessTaskData.forProsessTask(OpprettOppgaveVurderKonsekvensTask.class);
        opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BEHANDLENDE_ENHET, enhet.getEnhetId());
        opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_PRIORITET, OpprettOppgaveVurderKonsekvensTask.PRIORITET_HØY);
        opprettOppgave.setProperty(OpprettOppgaveVurderKonsekvensTask.KEY_BESKRIVELSE, vurderingTekst);
        return opprettOppgave;
    }
}
