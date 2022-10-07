package no.nav.k9.sak.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Optional;

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

    Periode utledOpplysningsperiode(Long id, FagsakYtelseType fagsakYtelseType, boolean tomDagensDato);

    default Optional<Periode> utledOpplysningsperiodeSkattegrunnlag(Long id, FagsakYtelseType fagsakYtelseType) {
        return Optional.empty();
    };


}
