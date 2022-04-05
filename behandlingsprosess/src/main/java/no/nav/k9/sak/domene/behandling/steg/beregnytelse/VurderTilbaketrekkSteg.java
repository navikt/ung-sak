package no.nav.k9.sak.domene.behandling.steg.beregnytelse;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_TILBAKETREKK;
import static no.nav.k9.kodeverk.behandling.BehandlingType.REVURDERING;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.ytelse.beregning.tilbaketrekk.AksjonspunktutlederTilbaketrekk;

@BehandlingStegRef(value = VURDER_TILBAKETREKK)
@BehandlingTypeRef(REVURDERING)
@FagsakYtelseTypeRef

@ApplicationScoped
public class VurderTilbaketrekkSteg implements BehandlingSteg {

    private AksjonspunktutlederTilbaketrekk aksjonspunktutlederTilbaketrekk;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private boolean disableVurderTilbaketrekk;

    VurderTilbaketrekkSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderTilbaketrekkSteg(AksjonspunktutlederTilbaketrekk aksjonspunktutlederTilbaketrekk,
                                  BehandlingRepository behandlingRepository,
                                  BeregningsresultatRepository beregningsresultatRepository,
                                  @KonfigVerdi(value = "DISABLE_VURDER_TILBAKETREKK", required = false, defaultVerdi = "true") Boolean disableVurderTilbaketrekk) {
        this.aksjonspunktutlederTilbaketrekk = aksjonspunktutlederTilbaketrekk;
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.disableVurderTilbaketrekk = disableVurderTilbaketrekk;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
        if (disableVurderTilbaketrekk) {
            if (bleLøstIForrigeBehandling(ref)) {
                // Kopierer valget som ble tatt sist og oppretter ikke aksjonspunkt
                kopierLøsningFraForrigeBehandling(ref);
            }
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        } else {
            List<AksjonspunktResultat> aksjonspunkter = aksjonspunktutlederTilbaketrekk.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref));
            return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
        }
    }

    private boolean bleLøstIForrigeBehandling(BehandlingReferanse ref) {
        var originalBeslutning = ref.getOriginalBehandlingId()
            .flatMap(oid -> beregningsresultatRepository.hentBeregningsresultatAggregat(oid))
            .flatMap(BehandlingBeregningsresultatEntitet::skalHindreTilbaketrekk);
        return originalBeslutning.isPresent();
    }

    private void kopierLøsningFraForrigeBehandling(BehandlingReferanse ref) {
        var originalBeslutning = ref.getOriginalBehandlingId()
            .flatMap(oid -> beregningsresultatRepository.hentBeregningsresultatAggregat(oid))
            .flatMap(BehandlingBeregningsresultatEntitet::skalHindreTilbaketrekk)
            .orElseThrow();
        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        beregningsresultatRepository.lagreMedTilbaketrekk(behandling, originalBeslutning);
    }

}
