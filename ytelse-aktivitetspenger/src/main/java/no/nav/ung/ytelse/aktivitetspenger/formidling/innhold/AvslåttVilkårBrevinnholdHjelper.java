package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBistand;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBosted;

import java.util.EnumSet;

// Beskrivelser av avslag og opphør er begge implementert vha ikkeOppfylteÅrsaker fra vilkårsvurdering.
// Gjenbruker derfor funksjonalitet på tvers av avslag- og opphørsbrev.
public class AvslåttVilkårBrevinnholdHjelper {

    private AvslåttVilkårBrevinnholdHjelper() {
    }

    public static AvslåttBosted lagAvslåttBosted(VilkårsvurderingResultat vurdering) {
        var ikkeOppfyltÅrsak = vurdering.ikkeOppfyltÅrsak();
        if (!(ikkeOppfyltÅrsak instanceof BostedsvilkårIkkeOppfyltÅrsak bostedsÅrsak)) {
            throw new IllegalStateException("Ukjent ikkeOppfyltÅrsak for bostedsvilkår: " + ikkeOppfyltÅrsak);
        }

        if (vurdering.fritekstVurderingBrev() != null) {
            return AvslåttBosted.medKunFritekst(
                vurdering.fritekstVurderingBrev()
            );
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
            null);
    }

    public static AvslåttBistand lagAvslåttBistand(VilkårsvurderingResultat vurdering) {
        var ikkeOppfyltÅrsak = vurdering.ikkeOppfyltÅrsak();
        if (!(ikkeOppfyltÅrsak instanceof BistandsvilkårIkkeOppfyltÅrsak bistandsÅrsak)) {
            throw new IllegalStateException("Ukjent ikkeOppfyltÅrsak for bistandsvilkår: " + ikkeOppfyltÅrsak);
        }

        if (vurdering.fritekstVurderingBrev() != null) {
            return AvslåttBistand.medKunFritekst(
                vurdering.fritekstVurderingBrev()
            );
        }
        var støttedeÅrsaker = EnumSet.of(
            BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK
        );
        if (!støttedeÅrsaker.contains(bistandsÅrsak)) {
            throw new IllegalStateException("Ukjent ikkeOppfyltÅrsak for bistandsvilkår: " + bistandsÅrsak);
        }

        return new AvslåttBistand(
            bistandsÅrsak == BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK,
            null
        );
    }
}

