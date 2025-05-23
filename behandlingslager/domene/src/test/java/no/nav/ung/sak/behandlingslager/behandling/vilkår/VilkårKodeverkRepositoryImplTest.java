package no.nav.ung.sak.behandlingslager.behandling.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;

public class VilkårKodeverkRepositoryImplTest {

    @Test
    public void test_finn_vilkårtype_fra_avslagårsak() {
        assertThat(Avslagsårsak.fraKode(Avslagsårsak.OPPHØRT_UNGDOMSPROGRAM.getKode()).getVilkårTyper()).isNotEmpty();
    }

    @Test
    public void skal_hente_alle_avslagsårsaker_gruppert_på_vilkårstype() {
        Map<VilkårType, Set<Avslagsårsak>> map = VilkårType.finnAvslagårsakerGruppertPåVilkårType();
        assertThat(map.get(VilkårType.SØKERSOPPLYSNINGSPLIKT)).containsOnly(Avslagsårsak.MANGLENDE_DOKUMENTASJON);
        assertThat(map.get(VilkårType.UNGDOMSPROGRAMVILKÅRET)).isNotEmpty();
    }
}
