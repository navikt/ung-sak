package no.nav.k9.sak.domene.medlem;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.Periode;

@FagsakYtelseTypeRef
@RequestScoped
public class DummySkjæringstidspunktTjeneste implements SkjæringstidspunktTjeneste {

    private LocalDate utledetSkjæringstidspunkt;

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        return Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(getUtledetSkjæringstidspunkt()).build();
    }

    @Override
    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        return null;
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId, FagsakYtelseType ytelseType) {
        return null;
    }

    public LocalDate getUtledetSkjæringstidspunkt() {
        return utledetSkjæringstidspunkt;
    }

    public void setUtledetSkjæringstidspunkt(LocalDate utledetSkjæringstidspunkt) {
        this.utledetSkjæringstidspunkt = utledetSkjæringstidspunkt;
    }

    @Override
    public Periode utledOpplysningsperiode(Long id, FagsakYtelseType fagsakYtelseType, boolean tomDagensDato) {
        return null;
    }

    @Override
    public Optional<Periode> utledOpplysningsperiodeSkattegrunnlag(Long id, FagsakYtelseType fagsakYtelseType) {
        return Optional.empty();
    }

}
