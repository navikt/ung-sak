package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringUnderArbeid;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBosted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SequencedCollection;

// Avslagsårsaker og opphørsårsaker er begge implementert vha avslagsårsaker i vilkår.
// Gjenbruker derfor funksjonalitet på tvers av avslag- og opphørsbrev.
public class AvslåttVilkårBrevinnholdHelper {

    private static final Logger LOG = LoggerFactory.getLogger(AvslåttVilkårBrevinnholdHelper.class);

    private AvslåttVilkårBrevinnholdHelper() {
    }

    public static AvslåttBosted lagAvslåttBosted(Vilkårene vilkårene,
                                                 VilkårsavklaringUnderArbeid vilkårsavklaring) {

        var vurdertPeriode = vilkårsavklaring.periode();
        var avslåttBosted = hentAvslåttVilkår(vilkårene, VilkårType.BOSTEDSVILKÅR, vurdertPeriode.toLocalDateInterval());
        if (avslåttBosted.getFritekstVurderingBrev() != null) {
            return AvslåttBosted.medKunFritekst(
                avslåttBosted.getFritekstVurderingBrev());
        }

        return new AvslåttBosted(
            avslåttBosted.getAvslagsårsak() == Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED,
            avslåttBosted.getAvslagsårsak() == Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE,
            avslåttBosted.getAvslagsårsak() == Avslagsårsak.YTELSE_IKKE_PÅ_ARBEIDSSTED_STUDIESTED,
            null);
    }


    private static VilkårPeriode hentAvslåttVilkår(Vilkårene vilkårene, VilkårType vilkårType, LocalDateInterval periode) {
        SequencedCollection<LocalDateSegment<VilkårPeriode>> perioder = vilkårene.getVilkårTimeline(vilkårType)
            .intersection(periode)
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

