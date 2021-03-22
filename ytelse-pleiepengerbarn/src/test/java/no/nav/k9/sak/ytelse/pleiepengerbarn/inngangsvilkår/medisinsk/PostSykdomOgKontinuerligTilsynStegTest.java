package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import java.util.TreeSet;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;

class PostSykdomOgKontinuerligTilsynStegTest {

    private PostSykdomOgKontinuerligTilsynSteg steg = new PostSykdomOgKontinuerligTilsynSteg();

    @Disabled
    @Test
    void name() {
        steg.justerVilkårsperioderEtterSykdom(Vilkårene.builder().build(), new TreeSet<>(), new TreeSet<>());
    }
}
