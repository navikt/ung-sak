package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.*;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultatPeriode;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBistand;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBosted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SequencedCollection;
import java.util.stream.Collectors;

// Avslagsårsaker og opphørsårsaker er begge implementert vha avslagsårsaker i vilkår.
// Gjenbruker derfor funksjonalitet på tvers av avslag- og opphørsbrev.
public class AvslåttVilkårBrevinnholdHjelper {

    private static final Logger LOG = LoggerFactory.getLogger(AvslåttVilkårBrevinnholdHjelper.class);

    private AvslåttVilkårBrevinnholdHjelper() {
    }

    public static AvslåttBosted lagAvslåttBosted(VilkårsvurderingResultatPeriode vurdering) {
        if (vurdering.getFritekstVurderingBrev() != null) {
            return AvslåttBosted.medKunFritekst(
                vurdering.getFritekstVurderingBrev()
            );
        }

        return new AvslåttBosted(
            vurdering.getIkkeOppfyltÅrsak() == BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM,
            vurdering.getIkkeOppfyltÅrsak() == BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM,
            vurdering.getIkkeOppfyltÅrsak() == BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM,
            vurdering.getFritekstVurderingBrev());
    }

    public static AvslåttBistand lagAvslåttBistand(VilkårsvurderingResultatPeriode vurdering) {
        if (vurdering.getFritekstVurderingBrev() != null) {
            return AvslåttBistand.medKunFritekst(
                vurdering.getFritekstVurderingBrev()
            );
        }

        var vilkårsvurderingBistand = vurdering.getIkkeOppfyltÅrsak();
        return new AvslåttBistand(
            vilkårsvurderingBistand == BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK,
            vurdering.getFritekstVurderingBrev()
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

