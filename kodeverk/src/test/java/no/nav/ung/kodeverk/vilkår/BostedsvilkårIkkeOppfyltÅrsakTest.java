package no.nav.ung.kodeverk.vilkår;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BostedsvilkårIkkeOppfyltÅrsakTest {

    @Test
    void ikke_bosattadresse_i_trondheim_gir_automatisk_avslag() {
        var årsak = BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM;
        assertThat(årsak.avslagsårsak()).contains(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE);
        assertThat(årsak.krevesFritekst()).isFalse();
        assertThat(årsak.erGyldigAvklaringsårsak()).isTrue();
    }

    @Test
    void annet_krever_fritekst_men_er_gyldig_avklaring() {
        var årsak = BostedsvilkårIkkeOppfyltÅrsak.ANNET;
        assertThat(årsak.avslagsårsak()).isPresent();
        assertThat(årsak.krevesFritekst()).isTrue();
        assertThat(årsak.erGyldigAvklaringsårsak()).isTrue();
    }

    @Test
    void avkortet_er_ikke_en_gyldig_avklaringsårsak() {
        var årsak = BostedsvilkårIkkeOppfyltÅrsak.AVKORTET;
        assertThat(årsak.avslagsårsak()).contains(Avslagsårsak.AVKORTET);
        assertThat(årsak.krevesFritekst()).isFalse();
        assertThat(årsak.erGyldigAvklaringsårsak()).isFalse();
    }

    @Test
    void udefinert_mangler_avslagsårsak_og_er_ikke_gyldig_avklaring() {
        var årsak = BostedsvilkårIkkeOppfyltÅrsak.UDEFINERT;
        assertThat(årsak.avslagsårsak()).isEmpty();
        assertThat(årsak.krevesFritekst()).isTrue();
        assertThat(årsak.erGyldigAvklaringsårsak()).isFalse();
    }

    @Test
    void alle_verdier_svarer_uten_å_kaste() {
        for (var årsak : BostedsvilkårIkkeOppfyltÅrsak.values()) {
            assertThat(årsak.avslagsårsak()).as("avslagsårsak() for %s", årsak).isNotNull();
            årsak.krevesFritekst();
            årsak.erGyldigAvklaringsårsak();
        }
    }
}
