package no.nav.ung.kodeverk.vilkår;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.hjemmel.AktivitetspengerForskrift;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AktivitetspengerLovreferanseTest {

    private static final FagsakYtelseType YTELSE = FagsakYtelseType.AKTIVITETSPENGER;

    @Test
    void alle_aktivitetspengervilkår_har_lovreferanse() {
        var aktivitetspengerVilkår = Arrays.stream(VilkårType.values())
            .filter(v -> v.getLovReferanse(YTELSE) != null)
            // Opplysningsplikt er ikke i bruk for aktivitetspenger, og hjemmel er ikke avklart
            .filter(v -> v != VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .toList();

        assertThat(aktivitetspengerVilkår).contains(
            VilkårType.ALDERSVILKÅR, VilkårType.SØKNADSFRIST, VilkårType.BOSTEDSVILKÅR,
            VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, VilkårType.BISTANDSVILKÅR,
            VilkårType.AKTIVITETSVILKÅR, VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET);
        assertThat(aktivitetspengerVilkår)
            .allSatisfy(v -> assertThat(v.getLovReferanse(YTELSE)).doesNotContain("TODO").isNotBlank());
    }

    @Test
    void avslagsårsaker_under_aktivitetspengervilkår_har_lovreferanse() {
        var vilkårMedAvslagsårsaker = Arrays.stream(VilkårType.values())
            .filter(v -> v.getAvslagsårsaker() != null)
            .filter(v -> v.getLovReferanse(YTELSE) != null)
            .filter(v -> v != VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .toList();

        for (var vilkår : vilkårMedAvslagsårsaker) {
            for (var årsak : vilkår.getAvslagsårsaker()) {
                assertThat(årsak.getLovHjemmelData(YTELSE))
                    .as("%s under %s", årsak, vilkår)
                    .doesNotContain("TODO", "mangler");
            }
        }
    }

    @Test
    void bostedsvilkår_viser_paragraf_2_i_forskriften() {
        assertThat(VilkårType.BOSTEDSVILKÅR.getLovReferanse(YTELSE))
            .isEqualTo(AktivitetspengerForskrift.NAVN + " § 2");
    }

    @Test
    void ingen_avslagsårsak_for_aktivitetspenger_har_todo() {
        assertThat(Arrays.stream(Avslagsårsak.values())
            .map(a -> a.getLovHjemmelData(YTELSE))
            .filter(h -> h.contains("TODO AKT")))
            .isEmpty();
    }
}
