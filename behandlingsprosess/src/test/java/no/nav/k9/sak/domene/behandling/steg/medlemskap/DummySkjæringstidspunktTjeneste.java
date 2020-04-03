package no.nav.k9.sak.domene.behandling.steg.medlemskap;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

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

    @Override
    public boolean harAvslåttPeriode(UUID behandlingUuid) {
        return false;
    }

    public LocalDate getUtledetSkjæringstidspunkt() {
        return utledetSkjæringstidspunkt;
    }

    public void setUtledetSkjæringstidspunkt(LocalDate utledetSkjæringstidspunkt) {
        this.utledetSkjæringstidspunkt = utledetSkjæringstidspunkt;
    }

}
