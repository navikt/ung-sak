package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.kronisksyk;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.Periode;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP_KS")
public class KroniskSykSkjæringstidspunktTjeneste implements SkjæringstidspunktTjeneste {

    private BehandlingRepository behandlingRepository;
    private SøknadRepository søknadRepository;

    private Period periodeFør = Period.parse("P12M");

    KroniskSykSkjæringstidspunktTjeneste() {
    }

    @Inject
    KroniskSykSkjæringstidspunktTjeneste(BehandlingRepository behandlingRepository,
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
    public boolean harAvslåttPeriode(UUID behandlingUuid) {
        throw new UnsupportedOperationException("ikke implementert, skal ikke være i bruk");
    }

    @Override
    public Periode utledOpplysningsperiode(Long behandlingId, FagsakYtelseType ytelseType, boolean tomDagensDato) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LocalDate tom = behandling.getFagsak().getPeriode().getTomDato();

        LocalDate skjæringstidspunkt = this.utledSkjæringstidspunktForRegisterInnhenting(behandlingId, ytelseType);
        return new Periode(skjæringstidspunkt.minus(periodeFør), tomDagensDato && tom.isBefore(LocalDate.now()) ? LocalDate.now() : tom);
    }

}
