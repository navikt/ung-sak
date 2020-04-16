package no.nav.k9.sak.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.typer.Periode;

public interface SkjæringstidspunktTjeneste {

    Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId);

    Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref);
    
    /**
     * Skjæringstidspunkt som benyttes for registerinnhenting
     */
    LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId, FagsakYtelseType ytelseType);

    boolean harAvslåttPeriode(UUID behandlingUuid);

    Periode utledOpplysningsperiode(Long id, FagsakYtelseType fagsakYtelseType, boolean tomDagensDato);

}
