package no.nav.ung.sak.domene.behandling.steg.medlemskap;

import java.time.LocalDate;
import java.util.Optional;
import jakarta.enterprise.context.RequestScoped;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandling.Skjæringstidspunkt;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

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

}
