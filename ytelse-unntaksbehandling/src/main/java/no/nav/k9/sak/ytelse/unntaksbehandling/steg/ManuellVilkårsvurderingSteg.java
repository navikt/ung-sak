package no.nav.k9.sak.ytelse.unntaksbehandling.steg;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

@FagsakYtelseTypeRef
@BehandlingStegRef(kode = "MANUELL_VILKÅRSVURDERING")
@BehandlingTypeRef("BT-010")
@ApplicationScoped
public class ManuellVilkårsvurderingSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    public ManuellVilkårsvurderingSteg() {
        // CDO
    }

    @Inject
    public ManuellVilkårsvurderingSteg(BehandlingRepository behandlingRepository, BeregningsresultatRepository beregningsresultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        if (BehandlingResultatType.AVSLÅTT.equals(behandling.getBehandlingResultatType())) {
            nullstillTilkjentYtelse(behandling, kontekst);
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.MANUELL_TILKJENT_YTELSE));
    }

    private void nullstillTilkjentYtelse(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        var origBehandlingId = behandling.getOriginalBehandlingId();

        if (!origBehandlingId.isPresent()) {
            beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), kontekst.getSkriveLås());
            return;
        }

        // Reverter til beregningsresultat fra forrige behandling
        beregningsresultatRepository.hentBeregningsresultatAggregat(origBehandlingId.get())
            .ifPresent(origAggregat -> {
                if (origAggregat.getBgBeregningsresultat() != null) {
                    beregningsresultatRepository.lagre(behandling, origAggregat.getBgBeregningsresultat());
                }
                if (origAggregat.getOverstyrtBeregningsresultat() != null) {
                    beregningsresultatRepository.lagre(behandling, origAggregat.getOverstyrtBeregningsresultat());
                }
            });
    }

}
