package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.weld.exceptions.UnsupportedOperationException;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
public class BehandletPeriodeTjeneste {

    private Instance<UtledBeregningSkjæringstidspunktForBehandlingTjeneste> utledStpTjenester;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenestes;

    BehandletPeriodeTjeneste() {
    }

    @Inject
    public BehandletPeriodeTjeneste(@Any Instance<UtledBeregningSkjæringstidspunktForBehandlingTjeneste> utledStpTjenester,
                                    @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenestes) {
        this.utledStpTjenester = utledStpTjenester;
        this.vilkårsPerioderTilVurderingTjenestes = vilkårsPerioderTilVurderingTjenestes;
    }

    DatoIntervallEntitet utledPeriode(BehandlingReferanse ref) {
        var fom = utledSkjæringstidspunkt(ref);
        var tom = utledPeriodeSlutt(ref);

        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    DatoIntervallEntitet utledOrginalVilkårsPeriode(BehandlingReferanse ref) {
        String ytelseTypeKode = ref.getFagsakYtelseType().getKode();
        var periodeTjeneste = FagsakYtelseTypeRef.Lookup.find(vilkårsPerioderTilVurderingTjenestes, ytelseTypeKode)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + VilkårsPerioderTilVurderingTjeneste.class.getName() + " for ytelsetype=" + ytelseTypeKode));

        return periodeTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR).stream().findFirst().orElseThrow();
    }

    private LocalDate utledPeriodeSlutt(BehandlingReferanse ref) {
        String ytelseTypeKode = ref.getFagsakYtelseType().getKode();
        var periodeTjeneste = FagsakYtelseTypeRef.Lookup.find(vilkårsPerioderTilVurderingTjenestes, ytelseTypeKode)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + VilkårsPerioderTilVurderingTjeneste.class.getName() + " for ytelsetype=" + ytelseTypeKode));

        return periodeTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .stream()
            .map(DatoIntervallEntitet::getTomDato)
            .max(LocalDate::compareTo)
            .orElseThrow();
    }

    private LocalDate utledSkjæringstidspunkt(no.nav.k9.sak.behandling.BehandlingReferanse ref) {
        String ytelseTypeKode = ref.getFagsakYtelseType().getKode();
        var mapper = FagsakYtelseTypeRef.Lookup.find(utledStpTjenester, ytelseTypeKode)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + UtledBeregningSkjæringstidspunktForBehandlingTjeneste.class.getName() + " for ytelsetype=" + ytelseTypeKode));
        return mapper.utled(ref);
    }
}
