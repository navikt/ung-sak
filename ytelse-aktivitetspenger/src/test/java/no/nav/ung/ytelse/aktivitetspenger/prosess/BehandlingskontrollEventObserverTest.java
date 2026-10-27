package no.nav.ung.ytelse.aktivitetspenger.prosess;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.hendelse.EventHendelse;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.hendelse.produksjonsstyring.PubliserEventTask;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.kontrakt.behandling.BehandlingProsessHendelse;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@CdiDbAwareTest
class BehandlingskontrollEventObserverTest {

    private static final BehandlingStegType NAV_KONTOR_STEG = BehandlingStegType.NAV_KONTOR_BESLUTTER_VILKÅR;
    private static final BehandlingStegType SENTRALT_STEG = BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR;
    private static final Periode PERIODE = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));

    @Inject
    private EntityManager entityManager;

    @Inject
    private BehandlingskontrollTjeneste kontrollTjeneste;

    @Inject
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Test
    void publiserer_eventtask_ved_overgang_fra_navkontor_til_sentral_del() {
        var behandling = nyBehandling(NAV_KONTOR_STEG);
        assertThat(taskerFor(behandling)).isEmpty();

        kontrollTjeneste.prosesserBehandling(kontrollTjeneste.initBehandlingskontroll(behandling.getId()));

        behandling = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(SENTRALT_STEG);
        assertThat(behandling.getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.UTGANG);
        assertThat(behandling.getÅpneAksjonspunkter())
            .extracting(aksjonspunkt -> aksjonspunkt.getAksjonspunktDefinisjon())
            .containsExactly(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAP);
        assertThat(overgangTaskerFor(behandling)).singleElement().satisfies(task -> {
            var hendelse = JsonObjectMapper.fromJson(task.getPayloadAsString(), BehandlingProsessHendelse.class);
            assertThat(hendelse.getEventHendelse()).isEqualTo(EventHendelse.AKSJONSPUNKT_UTFØRT);
            assertThat(hendelse.getBehandlingSteg()).isEqualTo(SENTRALT_STEG.getKode());
            assertThat(hendelse.getBehandlingenErHosNavKontor()).isFalse();
            assertThat(hendelse.getAksjonspunktTilstand()).isEmpty();
        });
    }

    @Test
    void publiserer_eventtask_ved_tilbakehopp_fra_sentral_del_til_navkontor() {
        var behandling = nyBehandling(SENTRALT_STEG);
        assertThat(taskerFor(behandling)).isEmpty();
        var kontekst = kontrollTjeneste.initBehandlingskontroll(behandling.getId());

        kontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, NAV_KONTOR_STEG);

        behandling = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(behandling.getAktivtBehandlingSteg()).isEqualTo(NAV_KONTOR_STEG);
        assertThat(behandling.getBehandlingStegStatus()).isEqualTo(BehandlingStegStatus.INNGANG);
        assertThat(overgangTaskerFor(behandling)).singleElement().satisfies(task -> {
            var hendelse = JsonObjectMapper.fromJson(task.getPayloadAsString(), BehandlingProsessHendelse.class);
            assertThat(hendelse.getEventHendelse()).isEqualTo(EventHendelse.AKSJONSPUNKT_TILBAKEFØR);
            assertThat(hendelse.getBehandlingSteg()).isEqualTo(NAV_KONTOR_STEG.getKode());
            assertThat(hendelse.getBehandlingenErHosNavKontor()).isTrue();
        });
    }

    private Behandling nyBehandling(BehandlingStegType startSteg) {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medBehandlingStegStart(startSteg)
            .medVirkningstidspunkt(PERIODE.getFom())
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_VURDERT, PERIODE)
            .lagre(entityManager);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE,
            DatoIntervallEntitet.fraOgMedTilOgMed(PERIODE.getFom(), PERIODE.getTom()))));
        return behandling;
    }

    private List<ProsessTaskData> taskerFor(Behandling behandling) {
        return prosessTaskTjeneste.finnAlle(PubliserEventTask.TASKTYPE, ProsessTaskStatus.KLAR).stream()
            .filter(task -> behandling.getId().toString().equals(task.getPropertyValue(PubliserEventTask.PROPERTY_KEY)))
            .toList();
    }

    private List<ProsessTaskData> overgangTaskerFor(Behandling behandling) {
        return taskerFor(behandling).stream()
            .filter(task -> {
                var hendelse = JsonObjectMapper.fromJson(task.getPayloadAsString(), BehandlingProsessHendelse.class).getEventHendelse();
                return hendelse == EventHendelse.AKSJONSPUNKT_UTFØRT || hendelse == EventHendelse.AKSJONSPUNKT_TILBAKEFØR;
            })
            .toList();
    }
}
