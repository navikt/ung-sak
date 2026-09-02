package no.nav.ung.kodeverk.vilkår;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AktivitetsvilkåretIkkeOppfyltÅrsakTest {

    @Test
    void annet_krever_fritekst_men_er_gyldig_avklaring() {
        var årsak = AktivitetsvilkåretIkkeOppfyltÅrsak.ANNET;
        assertThat(årsak.avslagsårsak()).contains(Avslagsårsak.AKTIVITETSVILKÅR_GENERELL_AVSLAGSÅRSAK);
        assertThat(årsak.krevesFritekst()).isTrue();
        assertThat(årsak.erGyldigAvklaringsårsak()).isTrue();
    }

    @Test
    void avkortet_er_ikke_en_gyldig_avklaringsårsak() {
        var årsak = AktivitetsvilkåretIkkeOppfyltÅrsak.AVKORTET;
        assertThat(årsak.avslagsårsak()).contains(Avslagsårsak.AVKORTET);
        assertThat(årsak.krevesFritekst()).isFalse();
        assertThat(årsak.erGyldigAvklaringsårsak()).isFalse();
    }

    @Test
    void udefinert_mangler_avslagsårsak_og_er_ikke_gyldig_avklaring() {
        var årsak = AktivitetsvilkåretIkkeOppfyltÅrsak.UDEFINERT;
        assertThat(årsak.avslagsårsak()).isEmpty();
        assertThat(årsak.krevesFritekst()).isTrue();
        assertThat(årsak.erGyldigAvklaringsårsak()).isFalse();
    }

    @Test
    void alle_verdier_svarer_uten_å_kaste() {
        for (var årsak : AktivitetsvilkåretIkkeOppfyltÅrsak.values()) {
            assertThat(årsak.avslagsårsak()).as("avslagsårsak() for %s", årsak).isNotNull();
            årsak.krevesFritekst();
            årsak.erGyldigAvklaringsårsak();
        }
    }
}
