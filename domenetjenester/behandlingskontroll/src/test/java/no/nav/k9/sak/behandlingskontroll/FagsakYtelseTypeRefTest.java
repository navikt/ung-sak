package no.nav.k9.sak.behandlingskontroll;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

/** Demonstrerer lookup med repeatble annotations. */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class FagsakYtelseTypeRefTest {

    @Test
    public void skal_få_duplikat_instans_av_cdi_bean() throws Exception {

        // Assert
        Assertions.assertThrows(IllegalStateException.class, () -> {

            // Act
            FagsakYtelseTypeRef.Lookup.find(Bokstav.class, FagsakYtelseType.PLEIEPENGER_SYKT_BARN);

        }, "Har flere matchende instanser");
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
