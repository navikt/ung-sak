package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.util.NavigableSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
@BehandlingStegRef(kode = "BEKREFT_UTTAK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class BekreftUttakSteg implements BehandlingSteg {

    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    BekreftUttakSteg() {
        // CDI
    }

    @Inject
    private BekreftUttakSteg(VilkårResultatRepository vilkårResultatRepository, @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());

        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(kontekst.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        var beregningsgrunnlagsvilkåret = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        if (harNoenPerioderTilVurderingBlittAvslåttIBeregning(perioderTilVurdering, beregningsgrunnlagsvilkåret)) {
            // TDOO: Kall på uttaket for å "ajurholde"
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean harNoenPerioderTilVurderingBlittAvslåttIBeregning(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Vilkår beregningsgrunnlagsvilkåret) {
        return beregningsgrunnlagsvilkåret.getPerioder().stream().anyMatch(it -> Utfall.IKKE_OPPFYLT.equals(it.getUtfall()) && perioderTilVurdering.stream().anyMatch(at -> at.overlapper(it.getPeriode())));
    }
}
