package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.util.Collection;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;

class OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt {

    private OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt() {
    }

    //TODO(OJR) burde kanskje innfører en egenskap som tilsier at MEDLEMSKAPSVILKÅRET_LØPENDE ikke er et inngangsvilkår?
    public static boolean vurder(List<Vilkår> vilkårene) {
        final ChronoLocalDate chronoLocalDate = LocalDate.now();
        return vilkårene.stream()
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .filter(it -> it.getPeriode().inkluderer(chronoLocalDate))
            .anyMatch(v -> !Utfall.OPPFYLT.equals(v.getGjeldendeUtfall()));
    }

    public static Behandlingsresultat fastsett(Behandling revurdering, List<Vilkår> vilkårene) {
        boolean skalBeregnesIInfotrygd = harIngenBeregningsreglerILøsningen(vilkårene);
        return SettOpphørOgIkkeRett.fastsett(revurdering, skalBeregnesIInfotrygd ? Vedtaksbrev.INGEN : Vedtaksbrev.AUTOMATISK);
    }

    private static boolean harIngenBeregningsreglerILøsningen(List<Vilkår> vilkårene) {
        return vilkårene.stream()
            .filter(vilkår -> VilkårType.BEREGNINGSGRUNNLAGVILKÅR.equals(vilkår.getVilkårType()))
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .anyMatch(periode -> Avslagsårsak.INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN.equals(periode.getAvslagsårsak())
                && Utfall.IKKE_OPPFYLT.equals(periode.getGjeldendeUtfall()));
    }
}
