package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.alenemidlertidig;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_MA;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.Periode;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER_MA)
public class MidlertidigAleneSkjæringstidspunktTjeneste implements SkjæringstidspunktTjeneste {

    private BehandlingRepository behandlingRepository;
    private SøknadRepository søknadRepository;

    private Period periodeFør = Period.parse("P12M");

    MidlertidigAleneSkjæringstidspunktTjeneste() {
    }

    @Inject
    MidlertidigAleneSkjæringstidspunktTjeneste(BehandlingRepository behandlingRepository,
                                               SøknadRepository søknadRepository) {
        this.behandlingRepository = behandlingRepository;
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

    @Override
    public Periode utledOpplysningsperiode(Long behandlingId, FagsakYtelseType ytelseType, boolean tomDagensDato) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LocalDate tom = behandling.getFagsak().getPeriode().getTomDato();

        LocalDate skjæringstidspunkt = this.utledSkjæringstidspunktForRegisterInnhenting(behandlingId, ytelseType);
        return new Periode(skjæringstidspunkt.minus(periodeFør), tomDagensDato && tom.isBefore(LocalDate.now()) ? LocalDate.now() : tom);
    }

}
