package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderFaktaBistandStegTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);

    @Inject
    private EntityManager entityManager;

    @Inject
    private @Any Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere;

    private BehandlingRepository behandlingRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private VurderFaktaBistandSteg steg;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(entityManager);
        prosessTriggereRepository = new ProsessTriggereRepository(entityManager);

        steg = new VurderFaktaBistandSteg(behandlingRepository, prosessTriggerPeriodeUtledere);
    }

    @Test
    void skal_opprette_aksjonspunkt_nar_det_finnes_trigger_med_endret_bistandsbehov() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.ENDRET_BISTANDSBEHOV, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe())
            .containsExactly(AksjonspunktDefinisjon.VURDER_FAKTA_OM_BISTAND);
    }

    @Test
    void skal_utfores_uten_aksjonspunkt_nar_det_ikke_finnes_trigger_med_endret_bistandsbehov() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_for_annen_behandlingsarsak() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var kontekst = new BehandlingskontrollKontekst(
            behandling.getFagsakId(),
            behandling.getAktørId(),
            behandlingRepository.taSkriveLås(behandling.getId()));
        return steg.utførSteg(kontekst);
    }
}
