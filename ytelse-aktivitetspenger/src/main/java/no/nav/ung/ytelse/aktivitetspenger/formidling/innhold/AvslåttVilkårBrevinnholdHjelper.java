package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBistand;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBosted;

// Beskrivelser av avslag og opphør er begge implementert vha ikkeOppfylteÅrsaker fra vilkårsvurdering.
// Gjenbruker derfor funksjonalitet på tvers av avslag- og opphørsbrev.
public class AvslåttVilkårBrevinnholdHjelper {

    private AvslåttVilkårBrevinnholdHjelper() {
    }

    public static AvslåttBosted lagAvslåttBosted(VilkårsvurderingResultat vurdering) {
        if (vurdering.fritekstVurderingBrev() != null) {
            return AvslåttBosted.medKunFritekst(
                vurdering.fritekstVurderingBrev()
            );
        }

        return new AvslåttBosted(
            vurdering.ikkeOppfyltÅrsak() == BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM,
            vurdering.ikkeOppfyltÅrsak() == BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM,
            vurdering.ikkeOppfyltÅrsak() == BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM,
            vurdering.fritekstVurderingBrev());
    }

    public static AvslåttBistand lagAvslåttBistand(VilkårsvurderingResultat vurdering) {
        if (vurdering.fritekstVurderingBrev() != null) {
            return AvslåttBistand.medKunFritekst(
                vurdering.fritekstVurderingBrev()
            );
        }

        var vilkårsvurderingBistand = vurdering.ikkeOppfyltÅrsak();
        return new AvslåttBistand(
            vilkårsvurderingBistand == BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK,
            vurdering.fritekstVurderingBrev()
        );
    }
}

