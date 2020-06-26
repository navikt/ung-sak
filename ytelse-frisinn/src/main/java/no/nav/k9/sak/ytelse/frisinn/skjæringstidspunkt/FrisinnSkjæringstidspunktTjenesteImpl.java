package no.nav.k9.sak.ytelse.frisinn.skjæringstidspunkt;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.Skjæringstidspunkt.Builder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.Periode;

@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class FrisinnSkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste {

    private final LocalDate skjæringstidspunkt = LocalDate.of(2020, 03, 01);
    private final Period periodeFør = Period.parse("P36M");

    private UttakRepository uttakRepository;
    private OpphørUttakTjeneste opphørUttakTjeneste;
    private BehandlingRepository behandlingRepository;

    FrisinnSkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public FrisinnSkjæringstidspunktTjenesteImpl(BehandlingRepository behandlingRepository,
                                                 UttakRepository uttakRepository,
                                                 UttakTjeneste uttakTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.uttakRepository = uttakRepository;
        this.opphørUttakTjeneste = new OpphørUttakTjeneste(uttakTjeneste);
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId, FagsakYtelseType ytelseType) {
        // FIXME K9 skjæringstidspunkt
        return førsteUttaksdag(behandlingId);
    }

    @Override
    public LocalDate hentSkjæringstidspunkterForPeriode(DatoIntervallEntitet vilkårsperiode) {
        return skjæringstidspunkt;
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        Builder builder = Skjæringstidspunkt.builder();

        LocalDate førsteUttaksdato = førsteUttaksdag(behandlingId);
        builder.medFørsteUttaksdato(førsteUttaksdato);
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
    public boolean harAvslåttPeriode(UUID behandlingUuid) {
        return opphørUttakTjeneste.harAvslåttUttakPeriode(behandlingUuid);
    }

    @Override
    public Periode utledOpplysningsperiode(Long behandlingId, FagsakYtelseType ytelseType, boolean tomDagensDato) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LocalDate tom = behandling.getFagsak().getPeriode().getTomDato().plus(Period.parse("P1M"));
        return new Periode(skjæringstidspunkt.minus(periodeFør), tomDagensDato && tom.isBefore(LocalDate.now()) ? LocalDate.now() : tom);
    }
}
