package no.nav.k9.sak.ytelse.frisinn.skjæringstidspunkt;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.Skjæringstidspunkt.Builder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.skjæringstidspunkt.SkattegrunnlaginnhentingTjeneste;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.Periode;

@FagsakYtelseTypeRef(FRISINN)
@ApplicationScoped
public class FrisinnSkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste {

    private final LocalDate skjæringstidspunkt = LocalDate.of(2020, 03, 01);
    private final Period periodeFør = Period.parse("P36M");

    private UttakRepository uttakRepository;
    private BehandlingRepository behandlingRepository;

    FrisinnSkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public FrisinnSkjæringstidspunktTjenesteImpl(BehandlingRepository behandlingRepository,
                                                 UttakRepository uttakRepository) {
        this.behandlingRepository = behandlingRepository;
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

    @Override
    public Periode utledOpplysningsperiode(Long behandlingId, FagsakYtelseType ytelseType, boolean tomDagensDato) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LocalDate tom = behandling.getFagsak().getPeriode().getTomDato().plus(Period.parse("P1M"));
        return new Periode(skjæringstidspunkt.minus(periodeFør), tomDagensDato && tom.isBefore(LocalDate.now()) ? LocalDate.now() : tom);
    }

    @Override
    public Optional<Periode> utledOpplysningsperiodeSkattegrunnlag(Long behandlingId, FagsakYtelseType ytelseType) {
        var fagsakperiodeTom = behandlingRepository.hentBehandling(behandlingId)
            .getFagsak()
            .getPeriode()
            .getTomDato();
        var førsteSkjæringstidspunkt = this.utledSkjæringstidspunktForRegisterInnhenting(behandlingId, ytelseType);
        return Optional.of(SkattegrunnlaginnhentingTjeneste.utledSkattegrunnlagOpplysningsperiode(førsteSkjæringstidspunkt, fagsakperiodeTom));
    }

}
