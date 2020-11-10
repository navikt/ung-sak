package no.nav.k9.sak.behandlingskontroll.impl.observer;

import static no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderingspunktType.INN;
import static no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderingspunktType.UT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.assertj.core.api.AbstractComparableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderingspunktType;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg.TransisjonType;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.events.BehandlingStegOvergangEvent;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.k9.sak.behandlingskontroll.testutilities.TestScenario;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class TilbakehoppTest {

    private static final FagsakYtelseType YTELSE_TYPE = TestScenario.DUMMY_YTELSE_TYPE;
    private final BehandlingModellRepository behandlingModellRepository = new BehandlingModellRepository();
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingStegType steg1;
    private BehandlingStegType steg2;
    private BehandlingStegType steg3;
    private BehandlingStegType steg4;
    private BehandlingStegType steg5;
    private List<StegTransisjon> transisjoner = new ArrayList<>();
    private EntityManager em = repoRule.getEntityManager();
    private BehandlingskontrollServiceProvider serviceProvider = new BehandlingskontrollServiceProvider(em, behandlingModellRepository, null);
    private final BehandlingRepository behandlingRepository = serviceProvider.getBehandlingRepository();

    private BehandlingskontrollTransisjonTilbakeføringEventObserver observer = new BehandlingskontrollTransisjonTilbakeføringEventObserver(serviceProvider) {
        @Override
        protected void hoppBakover(BehandlingStegModell s,
                                   BehandlingStegOvergangEvent.BehandlingStegTilbakeføringEvent event,
                                   BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
            transisjoner.add(new StegTransisjon(TransisjonType.HOPP_OVER_BAKOVER, s.getBehandlingStegType()));
        }
    };

    private Behandling behandling;
    private BehandlingLås behandlingLås;
    private BehandlingModell modell;

    @BeforeEach
    public void before() throws Exception {
        modell = behandlingModellRepository.getModell(BehandlingType.FØRSTEGANGSSØKNAD, YTELSE_TYPE);
        steg1 = BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT;

        // siden konfig er statisk definert p.t. må vi lete fram noen passende steg til å hoppe mellom
        steg2 = modell.finnNesteSteg(steg1).getBehandlingStegType();
        steg3 = modell.finnNesteSteg(steg2).getBehandlingStegType();
        steg4 = modell.finnNesteSteg(steg3).getBehandlingStegType();
        steg5 = modell.finnNesteSteg(steg4).getBehandlingStegType();
    }

    public void skal_ikke_røre_utførte_aksjonspunkt_som_oppsto_i_steget_det_hoppes_tilbake_til() {
        assertAPUendretVedTilbakehopp(fra(steg3, INN), til(steg1), medUtførtAP(identifisertI(steg1), løsesI(steg2, INN)));
        assertAPUendretVedTilbakehopp(fra(steg3, INN), til(steg1), medUtførtAP(identifisertI(steg1), løsesI(steg2, UT)));
        assertAPUendretVedTilbakehopp(fra(steg3, INN), til(steg1), medUtførtAP(identifisertI(steg1), løsesI(steg3, INN)));
    }

    public void skal_avbryte_åpent_aksjonspunkt_som_oppsto_i_steget_det_hoppes_tilbake_til_inngang() {
        assertAPAvbrytesVedTilbakehopp(fra(steg2, UT), til(steg2, INN), medAP(identifisertI(steg2), løsesI(steg2, UT), AksjonspunktStatus.OPPRETTET, false));
        assertAPAvbrytesVedTilbakehopp(fra(steg3, INN), til(steg2, INN), medAP(identifisertI(steg2), løsesI(steg3, INN), AksjonspunktStatus.OPPRETTET, false));
        assertAPUendretVedTilbakehopp(fra(steg3, INN), til(steg2, UT), medAP(identifisertI(steg2), løsesI(steg3, UT), AksjonspunktStatus.OPPRETTET, false));
    }

    @Disabled("TODO: sjekk test oppsett og avbrudd")
    @Test
    public void skal_avbryte_aksjonspunkter_som_oppsto_etter_tilsteget() {
        assertAPAvbrytesVedTilbakehopp(fra(steg3, INN), til(steg1), medUtførtAP(identifisertI(steg2), løsesI(steg2, UT)));
        assertAPAvbrytesVedTilbakehopp(fra(steg3, INN), til(steg1), medUtførtAP(identifisertI(steg2), løsesI(steg3, INN)));
    }

    @Disabled("TODO: sjekk test oppsett og avbrudd")
    @Test
    public void skal_ikke_endre_utførte_aksjonspunkter_som_oppsto_i_steget_det_hoppes_til() {
        assertAPUendretVedTilbakehopp(fra(steg3, INN), til(steg2, INN), medUtførtAP(identifisertI(steg2), løsesI(steg2, UT)));
        assertAPUendretVedTilbakehopp(fra(steg3, INN), til(steg2, UT), medUtførtAP(identifisertI(steg2), løsesI(steg3, INN)));
        assertAPUendretVedTilbakehopp(fra(steg3, INN), til(steg2, UT), medUtførtAP(identifisertI(steg2), løsesI(steg2, UT)));
    }

    @Disabled("TODO: sjekk test oppsett og avbrudd")
    @Test
    public void skal_ikke_endre_aksjonspunkter_som_oppsto_før_til_steget_og_som_skulle_utføres_i_eller_etter_til_steget() {
        assertAPUendretVedTilbakehopp(fra(steg3, INN), til(steg2), medUtførtAP(identifisertI(steg1), løsesI(steg2, UT)));
        assertAPUendretVedTilbakehopp(fra(steg3, INN), til(steg2), medUtførtAP(identifisertI(steg1), løsesI(steg3, INN)));
    }

    @Test
    public void skal_ikke_gjøre_noe_med_aksjonspunkt_som_oppsto_og_løstes_før_steget_det_hoppes_til() {
        assertAPUendretVedTilbakehopp(fra(steg3, INN), til(steg2), medUtførtAP(identifisertI(steg1), løsesI(steg1, UT)));
    }

    @Test
    public void skal_ikke_gjøre_noe_med_aksjonspunkt_som_oppsto_før_steget_det_hoppes_til_og_som_løses_etter_punktet_det_hoppes_fra() {
        assertAPUendretVedTilbakehopp(fra(steg5, INN), til(steg4),
            medAP(identifisertI(steg3), løsesI(steg5, UT), medStatus(AksjonspunktStatus.OPPRETTET)));
    }

    @Test
    public void skal_gjenopprette_et_overstyrings_aksjonspunkt_når_det_hoppes() {
        assertAPGjenåpnesVedTilbakehopp(fra(steg3, INN), til(steg1), medUtførtOverstyringAP(identifisertI(steg1), løsesI(steg2, UT)));
        assertAPGjenåpnesVedTilbakehopp(fra(steg3, INN), til(steg1), medUtførtOverstyringAP(identifisertI(steg1), løsesI(steg2, UT)));
        assertAPGjenåpnesVedTilbakehopp(fra(steg3, INN), til(steg1), medUtførtOverstyringAP(identifisertI(steg2), løsesI(steg2, UT)));
        assertAPGjenåpnesVedTilbakehopp(fra(steg3, INN), til(steg2), medUtførtOverstyringAP(identifisertI(steg1), løsesI(steg2, UT)));
        assertAPGjenåpnesVedTilbakehopp(fra(steg3, INN), til(steg2), medUtførtOverstyringAP(identifisertI(steg2), løsesI(steg2, UT)));
    }

    @Test
    public void skal_kalle_transisjoner_på_steg_det_hoppes_over() {
        assertThat(transisjonerVedTilbakehopp(fra(steg3, INN), til(steg1))).containsOnly(StegTransisjon.hoppTilbakeOver(steg1),
            StegTransisjon.hoppTilbakeOver(steg2), StegTransisjon.hoppTilbakeOver(steg3));
        assertThat(transisjonerVedTilbakehopp(fra(steg3, INN), til(steg2))).containsOnly(StegTransisjon.hoppTilbakeOver(steg2),
            StegTransisjon.hoppTilbakeOver(steg3));
        assertThat(transisjonerVedTilbakehopp(fra(steg2, UT), til(steg2))).containsOnly(StegTransisjon.hoppTilbakeOver(steg2));
    }

    @Test
    public void skal_ta_med_transisjon_på_steg_det_hoppes_fra_for_overstyring() {
        assertThat(transisjonerVedOverstyrTilbakehopp(fra(steg3, INN), til(steg1))).containsOnly(StegTransisjon.hoppTilbakeOver(steg1),
            StegTransisjon.hoppTilbakeOver(steg2), StegTransisjon.hoppTilbakeOver(steg3));
        assertThat(transisjonerVedOverstyrTilbakehopp(fra(steg3, INN), til(steg2))).containsOnly(StegTransisjon.hoppTilbakeOver(steg2),
            StegTransisjon.hoppTilbakeOver(steg3));
        assertThat(transisjonerVedOverstyrTilbakehopp(fra(steg2, UT), til(steg2))).containsOnly(StegTransisjon.hoppTilbakeOver(steg2));
    }

    private void assertAPGjenåpnesVedTilbakehopp(StegPort fra, StegPort til, Aksjonspunkt ap) {
        assertAPStatusEtterHopp(fra, til, ap).isEqualTo(AksjonspunktStatus.OPPRETTET);
    }

    private void assertAPAvbrytesVedTilbakehopp(StegPort fra, StegPort til, Aksjonspunkt ap) {
        assertAPStatusEtterHopp(fra, til, ap).isEqualTo(AksjonspunktStatus.AVBRUTT);
    }

    private void assertAPUendretVedTilbakehopp(StegPort fra, StegPort til, Aksjonspunkt ap) {
        if (ap != null) {
            AksjonspunktStatus orginalStatus = ap.getStatus();
            assertAPStatusEtterHopp(fra, til, ap).isEqualTo(orginalStatus);
        }
    }

    private AbstractComparableAssert<?, AksjonspunktStatus> assertAPStatusEtterHopp(StegPort fra, StegPort til, Aksjonspunkt ap) {
        Aksjonspunkt aksjonspunkt = utførTilbakehoppReturnerAksjonspunkt(fra, til, ap);
        return assertThat(aksjonspunkt.getStatus());
    }

    private List<StegTransisjon> transisjonerVedTilbakehopp(StegPort fra, StegPort til) {
        // skal ikke spille noen rolle for transisjoner hvilke aksjonspunkter som finnes
        Aksjonspunkt ap = medUtførtAP(identifisertI(steg1), løsesI(steg2, INN));

        transisjoner.clear();
        utførTilbakehoppReturnerAksjonspunkt(fra, til, ap);
        return transisjoner;
    }

    private List<StegTransisjon> transisjonerVedOverstyrTilbakehopp(StegPort fra, StegPort til) {
        // skal ikke spille noen rolle for transisjoner hvilke aksjonspunkter som finnes
        Aksjonspunkt ap = medUtførtOverstyringAP(identifisertI(steg1), løsesI(steg2, UT));

        transisjoner.clear();
        utførOverstyringTilbakehoppReturnerAksjonspunkt(fra, til, ap);
        return transisjoner;
    }

    private Aksjonspunkt utførTilbakehoppReturnerAksjonspunkt(StegPort fra, StegPort til, Aksjonspunkt ap) {
        BehandlingStegTilstandSnapshot fraTilstand = new BehandlingStegTilstandSnapshot(1L, fra.getSteg(), getBehandlingStegStatus(fra));
        BehandlingStegTilstandSnapshot tilTilstand = new BehandlingStegTilstandSnapshot(2L, til.getSteg(), getBehandlingStegStatus(til));
        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        BehandlingStegOvergangEvent.BehandlingStegTilbakeføringEvent event = new BehandlingStegOvergangEvent.BehandlingStegTilbakeføringEvent(kontekst,
            fraTilstand, tilTilstand);

        // act
        observer.observerBehandlingSteg(event);

        return ap;
    }

    private Aksjonspunkt utførOverstyringTilbakehoppReturnerAksjonspunkt(StegPort fra, StegPort til, Aksjonspunkt ap) {
        BehandlingStegTilstandSnapshot fraTilstand = new BehandlingStegTilstandSnapshot(1L, fra.getSteg(), getBehandlingStegStatus(fra));
        BehandlingStegTilstandSnapshot tilTilstand = new BehandlingStegTilstandSnapshot(2L, til.getSteg(), getBehandlingStegStatus(til));

        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        BehandlingStegOvergangEvent.BehandlingStegTilbakeføringEvent event = new BehandlingStegOvergangEvent.BehandlingStegTilbakeføringEvent(kontekst,
            fraTilstand, tilTilstand);

        // act
        observer.observerBehandlingSteg(event);

        return ap;
    }

    private BehandlingStegStatus getBehandlingStegStatus(StegPort fra) {
        BehandlingStegStatus fraStatus;
        String fraPort = fra.getPort().getDbKode();
        if (fraPort.equals(VurderingspunktType.INN.getDbKode())) {
            fraStatus = BehandlingStegStatus.INNGANG;
        } else if (fraPort.equals(VurderingspunktType.UT.getDbKode())) {
            fraStatus = BehandlingStegStatus.UTGANG;
        } else {
            throw new IllegalStateException("BehandlingStegStatus " + fraPort + " ikke støttet i testen");
        }
        return fraStatus;
    }

    private Aksjonspunkt medUtførtOverstyringAP(BehandlingStegType identifisertI, StegPort port) {
        return medAP(identifisertI, port, AksjonspunktStatus.UTFØRT, true);
    }

    private Aksjonspunkt medUtførtAP(BehandlingStegType identifisertI, StegPort port) {
        return medAP(identifisertI, port, AksjonspunktStatus.UTFØRT, false);
    }

    private Aksjonspunkt medAP(BehandlingStegType identifisertI, StegPort port, AksjonspunktStatus status) {
        return medAP(identifisertI, port, status, false);
    }

    private Aksjonspunkt medAP(BehandlingStegType identifisertISteg, StegPort port, AksjonspunktStatus status, boolean manueltOpprettet) {
        clearTransisjoner();
        AksjonspunktDefinisjon ad = finnAksjonspunkt(port, manueltOpprettet);
        BehandlingStegType idSteg = BehandlingStegType.fraKode(identifisertISteg.getKode());

        Behandling ytelseBehandling = TestScenario.dummyScenario().lagre(serviceProvider);
        behandling = Behandling.nyBehandlingFor(ytelseBehandling.getFagsak(), BehandlingType.FØRSTEGANGSSØKNAD).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        Aksjonspunkt ap = null;
        if (ad != null) {
            ap = serviceProvider.getAksjonspunktKontrollRepository().leggTilAksjonspunkt(behandling, ad, idSteg);
        }
        if (status.getKode().equals(AksjonspunktStatus.UTFØRT.getKode()) && ap != null) {
            serviceProvider.getAksjonspunktKontrollRepository().setTilUtført(ap, "ferdig");
        } else if (status.getKode().equals(AksjonspunktStatus.OPPRETTET.getKode())) {
            // dette er default-status ved opprettelse
        } else {
            throw new IllegalArgumentException("Testen støtter ikke status " + status + " du må evt. utvide testen");
        }

        behandlingRepository.lagre(behandling, behandlingLås);

        return ap;
    }

    private AksjonspunktDefinisjon finnAksjonspunkt(StegPort port, boolean manueltOpprettet) {
        var defs = AksjonspunktDefinisjon.finnAksjonspunktDefinisjoner(port.getSteg(), port.getPort());
        var filtered = defs.stream()
            .filter(ad -> !manueltOpprettet || ad.getAksjonspunktType().erOverstyringpunkt())
            .findFirst();
        return filtered.orElse(null);
    }

    private void clearTransisjoner() {
        transisjoner.clear();
    }

    private StegPort til(BehandlingStegType steg) {
        return new StegPort(steg, INN);
    }

    private StegPort til(BehandlingStegType steg, VurderingspunktType port) {
        return new StegPort(steg, port);
    }

    private StegPort fra(BehandlingStegType steg, VurderingspunktType port) {
        return new StegPort(steg, port);
    }

    private StegPort løsesI(BehandlingStegType steg, VurderingspunktType port) {
        return new StegPort(steg, port);
    }

    private BehandlingStegType identifisertI(BehandlingStegType steg) {
        return steg;

    }

    private AksjonspunktStatus medStatus(AksjonspunktStatus status) {
        return status;
    }

    static class StegPort {

        private final BehandlingStegType steg;

        private final VurderingspunktType port;

        public StegPort(BehandlingStegType steg, VurderingspunktType port) {
            this.steg = steg;
            this.port = port;
        }

        public BehandlingStegType getSteg() {
            return steg;
        }

        public VurderingspunktType getPort() {
            return port;
        }

    }

}
