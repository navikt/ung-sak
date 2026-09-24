package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.kodeverk.vilkår.VilkårsavklaringÅrsaker;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringMedVurdering;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttAndreLivsoppholdsytelser;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttAndreLivsoppholdsytelser.Livsoppholdsårsak;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBistand;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBistand.Bistandsårsak;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBosted;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBosted.Bostedsårsak;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

// Beskrivelser av avslag og opphør er begge implementert vha ikkeOppfylteÅrsaker fra vilkårsvurdering.
// Gjenbruker derfor funksjonalitet på tvers av avslag- og opphørsbrev.
public class AvslåttVilkårBrevinnholdHjelper {

    public static final Set<VilkårType> VILKÅR_I_MALEN = EnumSet.of(
        VilkårType.BOSTEDSVILKÅR,
        VilkårType.BISTANDSVILKÅR,
        VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR,
        VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET  // Ikke i malen, men feiler i verdikjedetest uten
        );

    private AvslåttVilkårBrevinnholdHjelper() {
    }

    /**
     * Periodene der vilkåret både er avklart i behandlingen og avslått, per vilkårtype.
     * Vilkår uten slike perioder er utelatt.
     */
    public static Map<VilkårType, LocalDateTimeline<VilkårsavklaringMedVurdering>> avklarteAvslag(
        LocalDateTimeline<Map<VilkårType, VilkårsavklaringMedVurdering>> avklaringOgVurderingTidslinje,
        DetaljertResultatTidslinje resultatTidslinje) {

        Map<VilkårType, LocalDateTimeline<VilkårsavklaringMedVurdering>> avklarteAvslag = new EnumMap<>(VilkårType.class);
        for (var vilkårType : VilkårsavklaringÅrsaker.alle().keySet()) {
            var avklartOgAvslått = avklaringOgVurderingTidslinje
                .mapValue(it -> it.get(vilkårType))
                .filterValue(it -> it != null && it.harVilkårsAvklaring())
                .intersection(resultatTidslinje.avslåttPeriode(vilkårType))
                .compress();
            if (!avklartOgAvslått.isEmpty()) {
                avklarteAvslag.put(vilkårType, avklartOgAvslått);
            }
        }
        return avklarteAvslag;
    }

    public static AvslåttBosted lagAvslåttBosted(VilkårsvurderingResultat vurdering) {
        var årsak = årsakFra(vurdering, BostedsvilkårIkkeOppfyltÅrsak.class);
        var brevårsak = switch (årsak) {
            case IKKE_BOSATTADRESSE_I_TRONDHEIM -> Bostedsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED;
            case IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM -> Bostedsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE;
            case STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM -> Bostedsårsak.YTELSE_IKKE_PÅ_ARBEIDSSTED_STUDIESTED;
            case ANNET -> Bostedsårsak.ANNEN_ÅRSAK;
            case AVKORTET, UDEFINERT -> throw utenBrevtekst(årsak);
        };
        return new AvslåttBosted(brevårsak, fritekstFra(vurdering, årsak));
    }

    public static AvslåttBistand lagAvslåttBistand(VilkårsvurderingResultat vurdering) {
        var årsak = årsakFra(vurdering, BistandsvilkårIkkeOppfyltÅrsak.class);
        var brevårsak = switch (årsak) {
            case IKKE_14A_VEDTAK -> Bistandsårsak.HAR_IKKE_14A_VEDTAK;
            case AVKORTET, UDEFINERT -> throw utenBrevtekst(årsak);
        };
        return new AvslåttBistand(brevårsak, fritekstFra(vurdering, årsak));
    }

    public static AvslåttAndreLivsoppholdsytelser lagAvslåttAndreLivsoppholdsytelser(VilkårsvurderingResultat vurdering) {
        var årsak = årsakFra(vurdering, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.class);
        var brevårsak = switch (årsak) {
            case MOTTAR_ARBEIDSAVKLARINGSPENGER -> Livsoppholdsårsak.MOTTAR_ARBEIDSAVKLARINGSPENGER;
            case MOTTAR_TILTAKSPENGER -> Livsoppholdsårsak.MOTTAR_TILTAKSPENGER;
            case MOTTAR_KVALIFISERINGSSTØNAD -> Livsoppholdsårsak.MOTTAR_KVALIFISERINGSSTØNAD;
            case MOTTAR_DAGPENGER -> Livsoppholdsårsak.MOTTAR_DAGPENGER;
            case MOTTAR_FORELDREPENGER -> Livsoppholdsårsak.MOTTAR_FORELDREPENGER;
            case MOTTAR_SVANGERSKAPSPENGER -> Livsoppholdsårsak.MOTTAR_SVANGERSKAPSPENGER;
            case MOTTAR_UFØRETRYGD -> Livsoppholdsårsak.MOTTAR_UFØRETRYGD;
            case MOTTAR_INTRODUKSJONSSTØNAD -> Livsoppholdsårsak.MOTTAR_INTRODUKSJONSSTØNAD;
            case MOTTAR_BARNEPENSJON -> Livsoppholdsårsak.MOTTAR_BARNEPENSJON;
            case MOTTAR_ANNEN_YTELSE -> Livsoppholdsårsak.MOTTAR_ANNEN_YTELSE;
            case AVKORTET, UDEFINERT -> throw utenBrevtekst(årsak);
        };
        return AvslåttAndreLivsoppholdsytelser.av(brevårsak, fritekstFra(vurdering, årsak));
    }

    private static <T extends IkkeOppfyltDetaljertÅrsak> T årsakFra(VilkårsvurderingResultat vurdering, Class<T> årsakstype) {
        var årsak = vurdering.ikkeOppfyltÅrsak();
        if (!årsakstype.isInstance(årsak)) {
            throw new IllegalStateException("Ukjent ikkeOppfyltÅrsak for " + vurdering.vilkårType() + ": " + årsak);
        }
        return årsakstype.cast(årsak);
    }

    private static String fritekstFra(VilkårsvurderingResultat vurdering, IkkeOppfyltDetaljertÅrsak årsak) {
        var fritekst = vurdering.fritekstVurderingBrev();
        if (fritekst == null && årsak.kreverFritekst()) {
            throw new IllegalStateException("Fritekst i brev mangler for ikkeOppfyltÅrsak som krever det: " + årsak);
        }
        return fritekst;
    }

    private static IllegalStateException utenBrevtekst(IkkeOppfyltDetaljertÅrsak årsak) {
        return new IllegalStateException("Vedtaksbrev har ingen tekst for ikkeOppfyltÅrsak " + årsak);
    }
}
