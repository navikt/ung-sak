package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBistand;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBosted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SequencedCollection;
import java.util.stream.Collectors;

// Avslagsårsaker og opphørsårsaker er begge implementert vha avslagsårsaker i vilkår.
// Gjenbruker derfor funksjonalitet på tvers av avslag- og opphørsbrev.
public class AvslåttVilkårBrevinnholdHelper {

    private static final Logger LOG = LoggerFactory.getLogger(AvslåttVilkårBrevinnholdHelper.class);

    private AvslåttVilkårBrevinnholdHelper() {
    }

    public static AvslåttBosted lagAvslåttBosted(Vilkårene vilkårene,
                                                 AktivitetspengerInngangsvilkårResultatGrunnlag vilkårVurdering,
                                                 LocalDateTimeline<Boolean> vurdertPeriode) {
        var avslåttBosted = hentAvslåttVilkår(vilkårene, VilkårType.BOSTEDSVILKÅR, vurdertPeriode);
        if (avslåttBosted.getFritekstVurderingBrev() != null) {
            return AvslåttBosted.medKunFritekst(
                avslåttBosted.getFritekstVurderingBrev());
        }

        var vilkårsvurderingBosted = vilkårVurdering.hentBostedTidslinje().intersection(vurdertPeriode)
            .segmenter().stream()
            .map(it -> it.getValue().getIkkeOppfyltÅrsak())
            .collect(Collectors.toSet());

        return new AvslåttBosted(
            vilkårsvurderingBosted.contains(BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM),
            vilkårsvurderingBosted.contains(BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM),
            vilkårsvurderingBosted.contains(BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM),
            null);
    }

    public static AvslåttBistand lagAvslåttBistand(Vilkårene vilkårene,
                                                   AktivitetspengerInngangsvilkårResultatGrunnlag vilkårVurdering,
                                                   LocalDateTimeline<Boolean> vurdertPeriode) {
        var avslåttBistand = hentAvslåttVilkår(vilkårene, VilkårType.BISTANDSVILKÅR, vurdertPeriode);
        if (avslåttBistand.getFritekstVurderingBrev() != null) {
            return AvslåttBistand.medKunFritekst(
                avslåttBistand.getFritekstVurderingBrev());
        }

        var vilkårsvurderingBistand = vilkårVurdering.hentBistandTidslinje().intersection(vurdertPeriode)
            .segmenter().stream()
            .map(it -> it.getValue().getIkkeOppfyltÅrsak())
            .collect(Collectors.toSet());

        return new AvslåttBistand(
            vilkårsvurderingBistand.contains(BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK),
            null
        );
    }



    private static VilkårPeriode hentAvslåttVilkår(Vilkårene vilkårene, VilkårType vilkårType, LocalDateTimeline<Boolean> vurdertPeriode) {
        SequencedCollection<LocalDateSegment<VilkårPeriode>> perioder = vilkårene.getVilkårTimeline(vilkårType)
            .intersection(vurdertPeriode)
            .filterValue(vp -> vp.getGjeldendeUtfall() == Utfall.IKKE_OPPFYLT)
            .segmenter();

        long antallDistinkte = perioder.stream()
            .map(LocalDateSegment::getValue)
            .map(vp -> vp.getAvslagsårsak() + "|" + vp.getFritekstVurderingBrev())
            .distinct()
            .count();
        if (antallDistinkte > 1) {
            LOG.warn("Fant {} ulike kombinasjoner av avslagsårsak og fritekst for {}, bruker første periode", antallDistinkte, vilkårType);
        }
        return perioder.getFirst().getValue();
    }
}

