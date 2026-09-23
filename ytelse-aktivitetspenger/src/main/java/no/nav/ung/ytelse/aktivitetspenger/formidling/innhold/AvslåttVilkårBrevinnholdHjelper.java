package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertVilkårResultat;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttAndreLivsoppholdsytelser;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBistand;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBosted;

import java.util.EnumSet;

// Beskrivelser av avslag og opphør er begge implementert vha ikkeOppfylteÅrsaker fra vilkårsvurdering.
// Gjenbruker derfor funksjonalitet på tvers av avslag- og opphørsbrev.
public class AvslåttVilkårBrevinnholdHjelper {

    private AvslåttVilkårBrevinnholdHjelper() {
    }

    public static boolean erFunksjoneltAvslag(DetaljertVilkårResultat vilkår) {
        return vilkår.avslagsårsak() != null && vilkår.avslagsårsak() != Avslagsårsak.AVKORTET;
    }

    public static AvslåttBosted lagAvslåttBosted(VilkårsvurderingResultat vurdering) {
        if (vurdering == null) {
            return null;
        }

        var ikkeOppfyltÅrsak = vurdering.ikkeOppfyltÅrsak();
        if (!(ikkeOppfyltÅrsak instanceof BostedsvilkårIkkeOppfyltÅrsak bostedsÅrsak)) {
            throw new IllegalStateException("Ukjent ikkeOppfyltÅrsak for bostedsvilkår: " + ikkeOppfyltÅrsak);
        }

        if (bostedsÅrsak.kreverFritekst()) {
            if (vurdering.fritekstVurderingBrev() == null) {
                throw new IllegalStateException("Fritekst i brev mangler for ikkeOppfyltÅrsak som krever det: " + bostedsÅrsak);
            }
            return AvslåttBosted.medKunFritekst(vurdering.fritekstVurderingBrev());
        }

        var støttedeÅrsaker = EnumSet.of(
            BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM,
            BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM,
            BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM
        );
        if (!støttedeÅrsaker.contains(bostedsÅrsak)) {
            throw new IllegalStateException("Ukjent ikkeOppfyltÅrsak for bostedsvilkår: " + bostedsÅrsak);
        }

        return new AvslåttBosted(
            bostedsÅrsak == BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM,
            bostedsÅrsak == BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM,
            bostedsÅrsak == BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM,
            vurdering.fritekstVurderingBrev());
    }

    public static AvslåttBistand lagAvslåttBistand(VilkårsvurderingResultat vurdering) {
        if (vurdering == null) {
            return null;
        }

        var ikkeOppfyltÅrsak = vurdering.ikkeOppfyltÅrsak();
        if (!(ikkeOppfyltÅrsak instanceof BistandsvilkårIkkeOppfyltÅrsak bistandsÅrsak)) {
            throw new IllegalStateException("Ukjent ikkeOppfyltÅrsak for bistandsvilkår: " + ikkeOppfyltÅrsak);
        }

        if (bistandsÅrsak.kreverFritekst()) {
            if (vurdering.fritekstVurderingBrev() == null) {
                throw new IllegalStateException("Fritekst i brev mangler for ikkeOppfyltÅrsak som krever det: " + bistandsÅrsak);
            }
            return AvslåttBistand.medKunFritekst(vurdering.fritekstVurderingBrev());
        }

        var støttedeÅrsaker = EnumSet.of(
            BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK
        );
        if (!støttedeÅrsaker.contains(bistandsÅrsak)) {
            throw new IllegalStateException("Ukjent ikkeOppfyltÅrsak for bistandsvilkår: " + bistandsÅrsak);
        }

        return new AvslåttBistand(
            bistandsÅrsak == BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK,
            vurdering.fritekstVurderingBrev()
        );
    }

    public static AvslåttAndreLivsoppholdsytelser lagAvslåttAndreLivsoppholdsytelser(VilkårsvurderingResultat vurdering) {
        if (vurdering == null) {
            return null;
        }

        var ikkeOppfyltÅrsak = vurdering.ikkeOppfyltÅrsak();
        if (!(ikkeOppfyltÅrsak instanceof AndreLivsoppholdsytelserIkkeOppfyltÅrsak livsoppholdsÅrsak)) {
            throw new IllegalStateException("Ukjent ikkeOppfyltÅrsak for vilkåret om andre livsoppholdsytelser: " + ikkeOppfyltÅrsak);
        }

        // MOTTAR_ANNEN_YTELSE navngir ikke ytelsen. Fritekst er kun påkrevd på varselet, så årsaken kan nå brevet uten.
        if (livsoppholdsÅrsak == AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_ANNEN_YTELSE) {
            return new AvslåttAndreLivsoppholdsytelser(null, true, vurdering.fritekstVurderingBrev());
        }

        if (livsoppholdsÅrsak.kreverFritekst()) {
            if (vurdering.fritekstVurderingBrev() == null) {
                throw new IllegalStateException("Fritekst i brev mangler for ikkeOppfyltÅrsak som krever det: " + livsoppholdsÅrsak);
            }
            return AvslåttAndreLivsoppholdsytelser.medKunFritekst(vurdering.fritekstVurderingBrev());
        }

        return new AvslåttAndreLivsoppholdsytelser(navnFor(livsoppholdsÅrsak), false, vurdering.fritekstVurderingBrev());
    }

    // Bøyd ytelsesnavn slik det leses i setningen "du får {{ytelse}}". Samme ordvalg som varselet til deltakeren.
    private static String navnFor(AndreLivsoppholdsytelserIkkeOppfyltÅrsak årsak) {
        return switch (årsak) {
            case MOTTAR_ARBEIDSAVKLARINGSPENGER -> "arbeidsavklaringspenger";
            case MOTTAR_TILTAKSPENGER -> "tiltakspenger";
            case MOTTAR_KVALIFISERINGSSTØNAD -> "kvalifiseringsstønad";
            case MOTTAR_DAGPENGER -> "dagpenger";
            case MOTTAR_FORELDREPENGER -> "foreldrepenger";
            case MOTTAR_SVANGERSKAPSPENGER -> "svangerskapspenger";
            case MOTTAR_UFØRETRYGD -> "uføretrygd";
            case MOTTAR_INTRODUKSJONSSTØNAD -> "introduksjonsstønad";
            case MOTTAR_BARNEPENSJON -> "barnepensjon";
            // AVKORTET er ikke et avslag brevet begrunner, og UDEFINERT er ingen årsak.
            case MOTTAR_ANNEN_YTELSE, AVKORTET, UDEFINERT ->
                throw new IllegalStateException("Vedtaksbrev har intet ytelsesnavn for ikkeOppfyltÅrsak " + årsak);
        };
    }
}
