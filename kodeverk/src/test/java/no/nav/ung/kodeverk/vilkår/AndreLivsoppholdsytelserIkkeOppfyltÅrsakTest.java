package no.nav.ung.kodeverk.vilkår;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AndreLivsoppholdsytelserIkkeOppfyltÅrsakTest {

    @Test
    void har_annen_livsoppholdsytelse_gir_automatisk_avslag() {
        var årsak = AndreLivsoppholdsytelserIkkeOppfyltÅrsak.HAR_ANNEN_LIVSOPPHOLDSYTELSE;
        assertThat(årsak.avslagsårsak()).contains(Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE);
        assertThat(årsak.krevesFritekst()).isFalse();
        assertThat(årsak.erGyldigAvklaringsårsak()).isTrue();
    }

    @Test
    void avkortet_er_ikke_en_gyldig_avklaringsårsak() {
        var årsak = AndreLivsoppholdsytelserIkkeOppfyltÅrsak.AVKORTET;
        assertThat(årsak.avslagsårsak()).contains(Avslagsårsak.AVKORTET);
        assertThat(årsak.krevesFritekst()).isFalse();
        assertThat(årsak.erGyldigAvklaringsårsak()).isFalse();
    }

    @Test
    void udefinert_mangler_avslagsårsak_og_er_ikke_gyldig_avklaring() {
        var årsak = AndreLivsoppholdsytelserIkkeOppfyltÅrsak.UDEFINERT;
        assertThat(årsak.avslagsårsak()).isEmpty();
        assertThat(årsak.krevesFritekst()).isTrue();
        assertThat(årsak.erGyldigAvklaringsårsak()).isFalse();
    }

    @Test
    void alle_verdier_svarer_uten_å_kaste() {
        for (var årsak : AndreLivsoppholdsytelserIkkeOppfyltÅrsak.values()) {
            assertThat(årsak.avslagsårsak()).as("avslagsårsak() for %s", årsak).isNotNull();
            årsak.krevesFritekst();
            årsak.erGyldigAvklaringsårsak();
        }
    }
}
