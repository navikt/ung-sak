package no.nav.k9.sak.ytelse.frisinn.skjæringstidspunkt;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.Skjæringstidspunkt.Builder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@FagsakYtelseTypeRef(FRISINN)
@ApplicationScoped
public class FrisinnSkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste {

    private final LocalDate skjæringstidspunkt = LocalDate.of(2020, 03, 01);
    private UttakRepository uttakRepository;

    FrisinnSkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public FrisinnSkjæringstidspunktTjenesteImpl(UttakRepository uttakRepository) {
        this.uttakRepository = uttakRepository;
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId, FagsakYtelseType ytelseType) {
        // FIXME K9 skjæringstidspunkt
        return førsteUttaksdag(behandlingId);
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        Builder builder = Skjæringstidspunkt.builder();
        builder.medUtledetSkjæringstidspunkt(skjæringstidspunkt);
        return builder.build();
    }

    @Override
    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        UttakAktivitet fastsattUttak = uttakRepository.hentFastsattUttak(ref.getBehandlingId());
        if (fastsattUttak != null && !fastsattUttak.getPerioder().isEmpty()) {
            LocalDate sisteUttaksdag = fastsattUttak.getMaksPeriode().getTomDato();
            return Optional.of(sisteUttaksdag.with(TemporalAdjusters.lastDayOfMonth()));
        }
        return Optional.empty();
    }

    private LocalDate førsteUttaksdag(Long behandlingId) {
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Mangler sønadsperiode for behandlingId=" + behandlingId));

        return søknadsperioder.getMaksPeriode().getFomDato();
    }

}
