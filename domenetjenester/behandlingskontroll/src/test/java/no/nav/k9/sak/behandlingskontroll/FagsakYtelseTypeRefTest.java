package no.nav.k9.sak.behandlingskontroll;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.ApplicationScoped;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

/** Demonstrerer lookup med repeatble annotations. */
@RunWith(CdiRunner.class)
public class FagsakYtelseTypeRefTest {

    @Test
    public void skal_få_duplikat_instans_av_cdi_bean() throws Exception {
        Assert.assertThrows("Har flere matchende instanser", IllegalStateException.class, () -> {
            FagsakYtelseTypeRef.Lookup.find(Bokstav.class, FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        });
    }

    @Test
    public void skal_få_unik_instans_av_cdi_bean() throws Exception {
        var instans = FagsakYtelseTypeRef.Lookup.find(Bokstav.class, FagsakYtelseType.SVANGERSKAPSPENGER);
        assertThat(instans).isNotNull();
        assertThat(instans).isEmpty();
    }

    public interface Bokstav {
    }

    @ApplicationScoped
    @FagsakYtelseTypeRef("PSB")
    public static class A implements Bokstav {

    }

    @ApplicationScoped
    @FagsakYtelseTypeRef("PSB")
    public static class B implements Bokstav {

    }
}
