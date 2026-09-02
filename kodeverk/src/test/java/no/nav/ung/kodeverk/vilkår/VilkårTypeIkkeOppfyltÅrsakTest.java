package no.nav.ung.kodeverk.vilkår;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VilkårTypeIkkeOppfyltÅrsakTest {

    @Test
    void bostedsvilkår_slår_opp_i_riktig_enum() {
        assertThat(VilkårType.BOSTEDSVILKÅR.ikkeOppfyltÅrsak("ANNET")).isEqualTo(BostedsvilkårIkkeOppfyltÅrsak.ANNET);
    }

    @Test
    void bistandsvilkår_slår_opp_i_riktig_enum() {
        assertThat(VilkårType.BISTANDSVILKÅR.ikkeOppfyltÅrsak("IKKE_14A_VEDTAK")).isEqualTo(BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK);
    }

    @Test
    void andre_livsoppholdsytelser_vilkår_slår_opp_i_riktig_enum() {
        assertThat(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR.ikkeOppfyltÅrsak("HAR_ANNEN_LIVSOPPHOLDSYTELSE"))
            .isEqualTo(AndreLivsoppholdsytelserIkkeOppfyltÅrsak.HAR_ANNEN_LIVSOPPHOLDSYTELSE);
    }

    @Test
    void aktivitetsvilkår_slår_opp_i_riktig_enum() {
        assertThat(VilkårType.AKTIVITETSVILKÅR.ikkeOppfyltÅrsak("ANNET")).isEqualTo(AktivitetsvilkåretIkkeOppfyltÅrsak.ANNET);
    }

    @Test
    void vilkår_uten_registrert_resolver_kaster() {
        assertThatThrownBy(() -> VilkårType.ALDERSVILKÅR.ikkeOppfyltÅrsak("ANNET"))
            .isInstanceOf(IllegalStateException.class);
    }
}
