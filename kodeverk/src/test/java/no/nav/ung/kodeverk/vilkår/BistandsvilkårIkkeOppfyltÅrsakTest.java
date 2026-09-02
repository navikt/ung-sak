package no.nav.ung.kodeverk.vilkår;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BistandsvilkårIkkeOppfyltÅrsakTest {

    @Test
    void ikke_14a_vedtak_gir_automatisk_avslag() {
        var årsak = BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK;
        assertThat(årsak.avslagsårsak()).contains(Avslagsårsak.IKKE_14A_VEDTAK);
        assertThat(årsak.krevesFritekst()).isFalse();
        assertThat(årsak.erGyldigAvklaringsårsak()).isTrue();
    }

    @Test
    void annet_krever_fritekst_men_er_gyldig_avklaring() {
        var årsak = BistandsvilkårIkkeOppfyltÅrsak.ANNET;
        assertThat(årsak.avslagsårsak()).contains(Avslagsårsak.IKKE_14A_VEDTAK);
        assertThat(årsak.krevesFritekst()).isTrue();
        assertThat(årsak.erGyldigAvklaringsårsak()).isTrue();
    }

    @Test
    void avkortet_er_ikke_en_gyldig_avklaringsårsak() {
        var årsak = BistandsvilkårIkkeOppfyltÅrsak.AVKORTET;
        assertThat(årsak.avslagsårsak()).contains(Avslagsårsak.AVKORTET);
        assertThat(årsak.krevesFritekst()).isFalse();
        assertThat(årsak.erGyldigAvklaringsårsak()).isFalse();
    }

    @Test
    void udefinert_mangler_avslagsårsak_og_er_ikke_gyldig_avklaring() {
        var årsak = BistandsvilkårIkkeOppfyltÅrsak.UDEFINERT;
        assertThat(årsak.avslagsårsak()).isEmpty();
        assertThat(årsak.krevesFritekst()).isTrue();
        assertThat(årsak.erGyldigAvklaringsårsak()).isFalse();
    }

    @Test
    void alle_verdier_svarer_uten_å_kaste() {
        for (var årsak : BistandsvilkårIkkeOppfyltÅrsak.values()) {
            assertThat(årsak.avslagsårsak()).as("avslagsårsak() for %s", årsak).isNotNull();
            årsak.krevesFritekst();
            årsak.erGyldigAvklaringsårsak();
        }
    }
}
