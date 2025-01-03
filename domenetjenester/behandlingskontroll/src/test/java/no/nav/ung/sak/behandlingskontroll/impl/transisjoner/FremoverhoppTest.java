package no.nav.ung.sak.behandlingskontroll.impl.transisjoner;

import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus.AVBRUTT;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus.UTFØRT;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.VurderingspunktType.INN;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.VurderingspunktType.UT;
import static no.nav.ung.sak.behandlingskontroll.transisjoner.FellesTransisjoner.FREMHOPP_TIL_IVERKSETT_VEDTAK;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.ung.sak.behandlingskontroll.impl.observer.StegTransisjon;
import org.assertj.core.api.AbstractComparableAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.VurderingspunktType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg.TransisjonType;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingTransisjonEvent;
import no.nav.ung.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.ung.sak.behandlingskontroll.impl.observer.BehandlingskontrollFremoverhoppTransisjonEventObserver;
import no.nav.ung.sak.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.ung.sak.behandlingskontroll.testutilities.TestScenario;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class FremoverhoppTest {

    private static final FagsakYtelseType YTELSE_TYPE = TestScenario.DUMMY_YTELSE_TYPE;

    private List<StegTransisjon> transisjoner = new ArrayList<>();

    @Inject
    private EntityManager entityManager;

    private BehandlingRepository behandlingRepository;
    private BehandlingModellRepository behandlingModellRepository;
    private BehandlingskontrollServiceProvider serviceProvider;
    private BehandlingskontrollFremoverhoppTransisjonEventObserver observer;

    private BehandlingStegType steg1 ;
    private BehandlingStegType steg2;
    private BehandlingStegType steg3;
    private Behandling behandling;
    private BehandlingLås behandlingLås;

    @BeforeEach
    public void before() throws Exception {

        behandlingRepository = new BehandlingRepository(entityManager);
        behandlingModellRepository = new BehandlingModellRepository();
        serviceProvider = new BehandlingskontrollServiceProvider(entityManager, behandlingModellRepository, null);
        observer = new BehandlingskontrollFremoverhoppTransisjonEventObserver(serviceProvider) {
            @Override
            protected void hoppFramover(BehandlingStegModell stegModell, BehandlingTransisjonEvent transisjonEvent, BehandlingStegType sisteSteg,
                                        BehandlingStegType finalFørsteSteg) {
                transisjoner.add(new StegTransisjon(TransisjonType.HOPP_OVER_FRAMOVER,
                    stegModell.getBehandlingStegType()));
            }
        };


        var modell = behandlingModellRepository.getModell(BehandlingType.FØRSTEGANGSSØKNAD, YTELSE_TYPE);
        steg1 = BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG;
        steg2 = modell.finnNesteSteg(steg1).getBehandlingStegType();
        steg3 = modell.finnNesteSteg(steg2).getBehandlingStegType();
    }

    @Test
    public void skal_avbryte_aksjonspunkt_som_skulle_vært_håndtert_i_mellomliggende_steg() {
        assertAPAvbrytesVedFremoverhopp(fra(steg1, UT), til(steg3), medAP(steg1, UT));
        assertAPAvbrytesVedFremoverhopp(fra(steg1, UT), til(steg3), medAP(steg1, UT));
    }

    @Test
    public void skal_ikke_gjøre_noe_med_aksjonspunkt_som_oppsto_og_løstes_før_steget_det_hoppes_fra() {
        assertAPUendretVedFremoverhopp(fra(steg2, UT), til(steg3), medAP(steg1, UT));
    }

    @Test
    public void skal_ikke_gjøre_noe_med_aksjonspunkt_som_løstes_ved_inngang_til_steget_når_det_hoppes_fra_utgang_av_steget() {
        assertAPUendretVedFremoverhopp(fra(steg2, UT), til(steg3), medAP(steg1, UT));
    }

    @Test
    public void skal_avbryte_aksjonspunkt_i_utgang_av_frasteget_når_frasteget_ikke_er_ferdig() {
        assertAPAvbrytesVedFremoverhopp(fra(steg2, INN), til(steg3), medAP(steg2, UT));
        assertAPAvbrytesVedFremoverhopp(fra(steg2, UT), til(steg3), medAP(steg2, UT));
    }

    @Test
    public void skal_ikke_gjøre_noe_med_aksjonspunkt_som_skal_løses_i_steget_det_hoppes_til() {
        assertAPUendretVedFremoverhopp(fra(steg2, UT), til(steg3), medAP(steg1, UT));
        assertAPUendretVedFremoverhopp(fra(steg2, UT), til(steg3), medAP(steg1, UT));
        assertAPUendretVedFremoverhopp(fra(steg2, UT), til(steg3), medAP(steg3, UT));
        assertAPUendretVedFremoverhopp(fra(steg2, UT), til(steg3), medAP(steg3, UT));
    }

    @Test
    public void skal_kalle_transisjoner_på_steg_det_hoppes_over() throws Exception {
        assertThat(transisjonerVedFremoverhopp(fra(steg1, INN), til(steg3))).contains(
            StegTransisjon.hoppFremoverOver(steg1),
            StegTransisjon.hoppFremoverOver(steg2));
        assertThat(transisjonerVedFremoverhopp(fra(steg1, UT), til(steg3)))
            .contains(StegTransisjon.hoppFremoverOver(steg2));
        assertThat(transisjonerVedFremoverhopp(fra(steg2, INN), til(steg3)))
            .contains(StegTransisjon.hoppFremoverOver(steg2));
        assertThat(transisjonerVedFremoverhopp(fra(steg2, UT), til(steg3))).isEmpty();
    }

    private void assertAPAvbrytesVedFremoverhopp(StegPort fra, BehandlingStegType til, Aksjonspunkt ap) {
        assertAPStatusEtterHopp(fra, til, ap).isEqualTo(AVBRUTT);
    }

    private void assertAPUendretVedFremoverhopp(StegPort fra, BehandlingStegType til, Aksjonspunkt ap) {
        AksjonspunktStatus orginalStatus = ap.getStatus();
        assertAPStatusEtterHopp(fra, til, ap).isEqualTo(orginalStatus);
    }

    private List<StegTransisjon> transisjonerVedFremoverhopp(StegPort fra, BehandlingStegType til) {
        // skal ikke spille noen rolle for transisjoner hvilke aksjonspunkter som finnes
        Aksjonspunkt ap = medAP(steg1, UT);

        transisjoner.clear();
        utførFremoverhoppReturnerAksjonspunkt(fra, til, ap);
        return transisjoner;
    }

    private AbstractComparableAssert<?, AksjonspunktStatus> assertAPStatusEtterHopp(StegPort fra, BehandlingStegType til, Aksjonspunkt ap) {
        Aksjonspunkt aksjonspunkt = utførFremoverhoppReturnerAksjonspunkt(fra, til, ap);
        return Assertions.assertThat(aksjonspunkt.getStatus());
    }

    private Aksjonspunkt utførFremoverhoppReturnerAksjonspunkt(StegPort fra, BehandlingStegType til, Aksjonspunkt ap) {

        BehandlingStegStatus fraStatus;
        String fraPort = fra.getPort().getDbKode();
        if (fraPort.equals(VurderingspunktType.INN.getDbKode())) {
            fraStatus = BehandlingStegStatus.INNGANG;
        } else if (fraPort.equals(VurderingspunktType.UT.getDbKode())) {
            fraStatus = BehandlingStegStatus.UTGANG;
        } else {
            throw new IllegalStateException("BehandlingStegStatus " + fraPort + " ikke støttet i testen");
        }

        BehandlingStegTilstand fraTilstand = new BehandlingStegTilstand(fra.getSteg(), fraStatus);
        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), behandlingLås);
        BehandlingTransisjonEvent transisjonEvent = new BehandlingTransisjonEvent(kontekst, FREMHOPP_TIL_IVERKSETT_VEDTAK, fraTilstand,
            til, true);

        // act
        observer.observerBehandlingSteg(transisjonEvent);

        return ap;
    }

    private Aksjonspunkt medAP(BehandlingStegType identifisertI, VurderingspunktType type) {
        return medAP(identifisertI, AksjonspunktStatus.OPPRETTET, identifisertI.getAksjonspunktDefinisjoner(type).get(0));
    }

    private Aksjonspunkt medAP(BehandlingStegType identifisertI, AksjonspunktStatus status, AksjonspunktDefinisjon ad) {

        BehandlingStegType idSteg = BehandlingStegType.fraKode(identifisertI.getKode());

        Behandling ytelseBehandling = TestScenario.dummyScenario().lagre(serviceProvider);
        behandling = Behandling.nyBehandlingFor(ytelseBehandling.getFagsak(), BehandlingType.FØRSTEGANGSSØKNAD).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
        Aksjonspunkt ap = serviceProvider.getAksjonspunktKontrollRepository().leggTilAksjonspunkt(behandling, ad, idSteg);

        if (status.getKode().equals(UTFØRT.getKode())) {
            serviceProvider.getAksjonspunktKontrollRepository().setTilUtført(ap, "ferdig");
        } else if (status.getKode().equals(AksjonspunktStatus.OPPRETTET.getKode())) {
            // dette er default-status ved opprettelse
        } else {
            throw new IllegalArgumentException("Testen støtter ikke status " + status + " du må evt. utvide testen");
        }

        behandlingRepository.lagre(behandling, behandlingLås);

        return ap;
    }

    class TestSteg implements BehandlingSteg {

        private final BehandlingStegType behandlingStegType;

        protected TestSteg(BehandlingStegType behandlingStegType) {
            this.behandlingStegType = behandlingStegType;
        }

        @Override
        public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
            return null;
        }

        @Override
        public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType fraSteg,
                                        BehandlingStegType tilSteg) {
            transisjoner.add(new StegTransisjon(TransisjonType.HOPP_OVER_FRAMOVER, behandlingStegType));
        }

    }

    private BehandlingStegType til(BehandlingStegType steg) {
        return steg;
    }

    private StegPort fra(BehandlingStegType steg, VurderingspunktType port) {
        return new StegPort(steg, port);

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
