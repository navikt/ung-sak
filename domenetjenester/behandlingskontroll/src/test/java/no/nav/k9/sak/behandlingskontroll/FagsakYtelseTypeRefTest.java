package no.nav.k9.sak.behandlingskontroll;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

/** Demonstrerer lookup med repeatble annotations. */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class FagsakYtelseTypeRefTest {

    @Test
    public void skal_få_duplikat_instans_av_cdi_bean() throws Exception {

        // Assert
        Assertions.assertThrows(IllegalStateException.class, () -> {

            // Act
            FagsakYtelseTypeRef.Lookup.find(Bokstav.class, PLEIEPENGER_SYKT_BARN);

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
    @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
    public static class A implements Bokstav {

    }

    @ApplicationScoped
    @FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
    public static class B implements Bokstav {

    }
}
