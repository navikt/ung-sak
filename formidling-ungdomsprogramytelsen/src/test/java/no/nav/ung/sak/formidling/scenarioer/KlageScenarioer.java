package no.nav.ung.sak.formidling.scenarioer;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.klage.KlageMedholdÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageFormkravAdapter;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageVurderingAdapter;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngKlageTestScenario;

import java.util.List;

public class KlageScenarioer {

    /**
     * Klage Avvist
     *
     * @param originalScenario
     * @return
     */
    public static UngKlageTestScenario klageAvvist(TestScenarioBuilder originalScenario) {
        var klageUtredning = new KlageUtredningEntitet.Builder()
            .medFormkrav(new KlageFormkravAdapter(
                true,
                false,
                true,
                true,
                true,
                "klage avvist"
            ))
            .medOpprinneligBehandlendeEnhet("4806");

        var klageVurdering = KlageVurderingAdapter.Templates.AVVIST_VURDERING_VEDTAKSINSTANS;

        return new UngKlageTestScenario(klageUtredning, klageVurdering, originalScenario, List.of(AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_VEDTAKSINSTANS));
    }

    /**
     * Klage oversendt
     *
     * @param originalScenario
     * @return
     */
    public static UngKlageTestScenario klageOversendt(TestScenarioBuilder originalScenario) {
        var klageUtredning = new KlageUtredningEntitet.Builder()
            .medFormkrav(lagGodkjentFormkrav())
            .medOpprinneligBehandlendeEnhet("4806");

        var klageVurdering = new KlageVurderingAdapter(
            KlageVurderingType.STADFESTE_YTELSESVEDTAK,
            null,
            null,
            "Fritekstbeskrivelse av klagevurdering",
            "FRITEKST I BREV",
            null,
            null,
            KlageVurdertAv.VEDTAKSINSTANS
        );

        return new UngKlageTestScenario(klageUtredning, klageVurdering, originalScenario, List.of(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_VEDTAKSINSTANS));
    }
    /**
     * Klage medhold
     *
     * @param originalScenario
     * @return
     */
    public static UngKlageTestScenario klageMedhold(TestScenarioBuilder originalScenario) {
        var klageUtredning = new KlageUtredningEntitet.Builder()
            .medFormkrav(lagGodkjentFormkrav())
            .medOpprinneligBehandlendeEnhet("4806");

        var klageVurdering = new KlageVurderingAdapter(
            KlageVurderingType.MEDHOLD_I_KLAGE,
            KlageMedholdÅrsak.ULIK_VURDERING,
            KlageVurderingOmgjør.GUNST_MEDHOLD_I_KLAGE,
            "Fritekstbeskrivelse av klagevurdering",
            "FRITEKST I BREV",
            null,
            null,
            KlageVurdertAv.VEDTAKSINSTANS
        );

        return new UngKlageTestScenario(klageUtredning, klageVurdering, originalScenario, List.of(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_KLAGE_VEDTAKSINSTANS));
    }


    public static Behandling lagKlageBehandling(UngTestRepositories ungTestRepositories, UngKlageTestScenario klageScenario) {
        var fagsakTestScenario = klageScenario.originalBehandlingScenario();
        fagsakTestScenario
            .medBehandlingType(BehandlingType.KLAGE)
            .medBehandlingsresultat(BehandlingResultatType.INNVILGET)
            .medKlageGrunnlag(klageScenario);

        klageScenario.utførteAksjonspunkter().forEach(it ->
            fagsakTestScenario.leggTilAksjonspunkt(it, it.getBehandlingSteg()));

        Behandling klageBehandling = fagsakTestScenario.buildOgLagreKlage(ungTestRepositories);

        AksjonspunktTestSupport aksjonspunktTestSupport = new AksjonspunktTestSupport();
        klageBehandling.getÅpneAksjonspunkter().forEach(
            it -> {
                it.setAnsvarligSaksbehandler(BrevScenarioerUtils.SAKSBEHANDLER1_IDENT);
                aksjonspunktTestSupport.setTilUtført(it, "utført");
            }
        );

        klageBehandling.setAnsvarligSaksbehandler(BrevScenarioerUtils.SAKSBEHANDLER1_IDENT);

        BehandlingRepository behandlingRepository = ungTestRepositories.repositoryProvider().getBehandlingRepository();
        behandlingRepository.lagre(klageBehandling, behandlingRepository.taSkriveLås(klageBehandling));

        return klageBehandling;
    }

    private static KlageFormkravAdapter lagGodkjentFormkrav() {
        return new KlageFormkravAdapter(
            true,
            true,
            true,
            true,
            true,
            "Alt ser bra ut"
        );
    }
}
