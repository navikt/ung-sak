package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;

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
@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
public class KroniskSykSkjæringstidspunktTjeneste implements SkjæringstidspunktTjeneste {

    private SøknadRepository søknadRepository;


    KroniskSykSkjæringstidspunktTjeneste() {
    }

    @Inject
    KroniskSykSkjæringstidspunktTjeneste(SøknadRepository søknadRepository) {
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
