package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER_AO)
public class AleneomsorgSkjæringstidspunktTjeneste implements SkjæringstidspunktTjeneste {

    private SøknadRepository søknadRepository;

    AleneomsorgSkjæringstidspunktTjeneste() {
    }

    @Inject
    AleneomsorgSkjæringstidspunktTjeneste(SøknadRepository søknadRepository) {
        this.søknadRepository = søknadRepository;
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        var søknad = søknadRepository.hentSøknadHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Har ikke søknad for behandlinId:" + behandlingId));

        return Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(søknad.getSøknadsperiode().getFomDato())
            .build();
    }

    @Override
    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        return Optional.ofNullable(ref.getFagsakPeriode().getTomDato());
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId, FagsakYtelseType ytelseType) {
        return getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
    }

}
