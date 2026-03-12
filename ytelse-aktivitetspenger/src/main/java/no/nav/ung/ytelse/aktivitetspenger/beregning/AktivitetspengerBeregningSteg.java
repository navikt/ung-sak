package no.nav.ung.ytelse.aktivitetspenger.beregning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.BeregningStegTjeneste;

@ApplicationScoped
@BehandlingStegRef(BehandlingStegType.AKTIVITETSPENGER_BEREGNING)
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
@BehandlingTypeRef
public class AktivitetspengerBeregningSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private AktivitetspengerBeregningsgrunnlagRepository aktivitetspengerBeregningsgrunnlagRepository;
    private VilkårTjeneste vilkårTjeneste;
    private BeregningStegTjeneste beregningStegTjeneste;

    AktivitetspengerBeregningSteg() {
    }

    @Inject
    public AktivitetspengerBeregningSteg(BehandlingRepository behandlingRepository,
                                         AktivitetspengerBeregningsgrunnlagRepository aktivitetspengerBeregningsgrunnlagRepository,
                                         VilkårTjeneste vilkårTjeneste,
                                         BeregningStegTjeneste beregningStegTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.aktivitetspengerBeregningsgrunnlagRepository = aktivitetspengerBeregningsgrunnlagRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.beregningStegTjeneste = beregningStegTjeneste;
    }


    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var samletResultat = vilkårTjeneste.samletVilkårsresultat(kontekst.getBehandlingId());
        validerKunVurdertePerioder(samletResultat);
        var oppfyltVilkårTidslinje = samletResultat.filterValue(v -> v.getSamletUtfall().equals(Utfall.OPPFYLT)).mapValue(it -> true);
        if (oppfyltVilkårTidslinje.isEmpty()) {
            aktivitetspengerBeregningsgrunnlagRepository.deaktiverGrunnlag(kontekst.getBehandlingId());
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        var skjæringstidspunkt = oppfyltVilkårTidslinje.getMinLocalDate();
        beregningStegTjeneste.utførBesteberegning(behandling.getId(), skjæringstidspunkt);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    /**
         * Validerer at alle perioder i tidslinjen er vurdert.
         * Kaster IllegalStateException hvis noen periode ikke er vurdert.
         *
         * @param samletResultat Tidslinje med vurderte vilkår
         */
    private static void validerKunVurdertePerioder(LocalDateTimeline<VilkårUtfallSamlet> samletResultat) {
        var ikkeVurdertTidslinje = samletResultat.filterValue(v -> v.getSamletUtfall().equals(Utfall.IKKE_VURDERT)).mapValue(it -> true);

        if (!ikkeVurdertTidslinje.isEmpty()){
            throw new IllegalStateException("Fant segmenter som ikke var vurdert: " + ikkeVurdertTidslinje.getLocalDateIntervals());
        }
    }

}
