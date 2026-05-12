package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertVilkårResultat;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBistand;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBosted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Collectors;

// Avslagsårsaker og opphørsårsaker er begge implementert vha avslagsårsaker i vilkår.
// Gjenbruker derfor funksjonalitet på tvers av avslag- og opphørsbrev.
public class AvslåttVilkårBrevinnholdHelper {

    private static final Logger LOG = LoggerFactory.getLogger(AvslåttVilkårBrevinnholdHelper.class);

    private AvslåttVilkårBrevinnholdHelper() {
    }

    public static AvslåttBosted lagAvslåttBosted(Set<DetaljertVilkårResultat> alleAvslåtteVilkår,
                                                  Behandling behandling,
                                                  LocalDateTimeline<DetaljertResultat> vurdertPeriode,
                                                  VilkårResultatRepository vilkårResultatRepository) {
        Avslagsårsak årsak = finnAvslagsårsak(alleAvslåtteVilkår, VilkårType.BOSTEDSVILKÅR);
        Vilkårene vilkårene = vilkårResultatRepository.hent(behandling.getId());
        var fritekst = hentAvslåttVilkår(vilkårene, VilkårType.BOSTEDSVILKÅR, vurdertPeriode).getFritekstVurderingBrev();
        if (fritekst != null) {
            return AvslåttBosted.medKunFritekst(fritekst);
        }

        return new AvslåttBosted(
            årsak == Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED,
            årsak == Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE,
            årsak == Avslagsårsak.YTELSE_IKKE_PÅ_ARBEIDSSTED_STUDIESTED,
            null);
    }

    public static AvslåttBistand lagAvslåttBistand(Set<DetaljertVilkårResultat> alleAvslåtteVilkår,
                                                    Behandling behandling,
                                                    LocalDateTimeline<DetaljertResultat> vurdertPeriode,
                                                    VilkårResultatRepository vilkårResultatRepository) {
        Avslagsårsak årsak = finnAvslagsårsak(alleAvslåtteVilkår, VilkårType.BISTANDSVILKÅR);
        Vilkårene vilkårene = vilkårResultatRepository.hent(behandling.getId());
        var fritekst = hentAvslåttVilkår(vilkårene, VilkårType.BISTANDSVILKÅR, vurdertPeriode).getFritekstVurderingBrev();
        if (fritekst != null) {
            return AvslåttBistand.medKunFritekst(fritekst);
        }

        return new AvslåttBistand(
            årsak == Avslagsårsak.IKKE_14A_VEDTAK,
            null
        );
    }

    public static Set<DetaljertVilkårResultat> hentAvslåtteVilkår(LocalDateTimeline<DetaljertResultat> avslagPeriode) {
        return avslagPeriode.stream()
            .flatMap(s -> s.getValue().avslåtteVilkår().stream())
            .collect(Collectors.toSet());
    }

    private static Avslagsårsak finnAvslagsårsak(Set<DetaljertVilkårResultat> avslåtteVilkår, VilkårType vilkårType) {
        return avslåtteVilkår.stream()
            .filter(v -> v.vilkårType() == vilkårType)
            .map(DetaljertVilkårResultat::avslagsårsak)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Fant ikke avslagsårsak for vilkårType " + vilkårType));
    }

    private static VilkårPeriode hentAvslåttVilkår(Vilkårene vilkårene, VilkårType vilkårType, LocalDateTimeline<DetaljertResultat> vurdertPeriode) {
        NavigableSet<LocalDateSegment<VilkårPeriode>> perioder = vilkårene.getVilkårTimeline(vilkårType)
            .intersection(vurdertPeriode.mapValue(_ -> true))
            .filterValue(vp -> vp.getGjeldendeUtfall() == Utfall.IKKE_OPPFYLT)
            .toSegments();

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

