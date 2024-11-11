package no.nav.ung.sak.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandling.Skjæringstidspunkt;

public interface SkjæringstidspunktTjeneste {

    Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId);

    Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref);

    /**
     * Skjæringstidspunkt som benyttes for registerinnhenting
     */
    LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId, FagsakYtelseType ytelseType);



}
