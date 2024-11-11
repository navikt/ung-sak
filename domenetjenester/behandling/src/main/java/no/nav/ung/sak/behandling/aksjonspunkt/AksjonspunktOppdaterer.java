package no.nav.ung.sak.behandling.aksjonspunkt;

import java.time.LocalDate;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;

/** Interface for å oppdatere aksjonspunkter. */
public interface AksjonspunktOppdaterer<T> {

    OppdateringResultat oppdater(T dto, AksjonspunktOppdaterParameter param);

    @SuppressWarnings("unused")
    default boolean skalReinnhenteRegisteropplysninger(Behandling behandling, LocalDate forrigeSkjæringstidspunkt) {
        return false;
    }

}
