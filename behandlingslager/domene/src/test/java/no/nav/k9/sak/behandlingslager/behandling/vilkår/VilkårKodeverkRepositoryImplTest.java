package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;

public class VilkårKodeverkRepositoryImplTest {

    @Test
    public void test_finn_vilkårtype_fra_avslagårsak() {
        assertThat(Avslagsårsak.fraKode(Avslagsårsak.SØKER_HAR_IKKE_OPPHOLDSRETT.getKode()).getVilkårTyper()).isNotEmpty();
    }

    @Test
    public void skal_hente_alle_avslagsårsaker_gruppert_på_vilkårstype() {
        Map<VilkårType, Set<Avslagsårsak>> map = VilkårType.finnAvslagårsakerGruppertPåVilkårType();
        assertThat(map.get(VilkårType.SØKERSOPPLYSNINGSPLIKT)).containsOnly(Avslagsårsak.MANGLENDE_DOKUMENTASJON);
        assertThat(map.get(VilkårType.OPPTJENINGSVILKÅRET)).isNotEmpty();
    }
}
