package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultatPeriode;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBistand;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBosted;

// Beskrivelser av avslag og opphør er begge implementert vha ikkeOppfylteÅrsaker fra vilkårsvurdering.
// Gjenbruker derfor funksjonalitet på tvers av avslag- og opphørsbrev.
public class AvslåttVilkårBrevinnholdHjelper {

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
}

