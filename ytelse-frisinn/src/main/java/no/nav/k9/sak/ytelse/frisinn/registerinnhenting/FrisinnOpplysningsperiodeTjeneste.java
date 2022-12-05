package no.nav.k9.sak.ytelse.frisinn.registerinnhenting;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.registerinnhenting.OpplysningsperiodeTjeneste;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.skjæringstidspunkt.SkattegrunnlaginnhentingTjeneste;
import no.nav.k9.sak.typer.Periode;

@FagsakYtelseTypeRef(FRISINN)
@ApplicationScoped
class FrisinnOpplysningsperiodeTjeneste implements OpplysningsperiodeTjeneste {


    private final LocalDate skjæringstidspunkt = LocalDate.of(2020, 03, 01);
    private final Period periodeFør = Period.parse("P36M");

    private UttakRepository uttakRepository;
    private BehandlingRepository behandlingRepository;

    FrisinnOpplysningsperiodeTjeneste() {
        // CDI
    }

    @Inject
    public FrisinnOpplysningsperiodeTjeneste(BehandlingRepository behandlingRepository,
                                             UttakRepository uttakRepository) {
        this.behandlingRepository = behandlingRepository;
        this.uttakRepository = uttakRepository;
    }


    @Override
    public Periode utledOpplysningsperiode(Long behandlingId, boolean tomDagensDato) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LocalDate tom = behandling.getFagsak().getPeriode().getTomDato().plus(Period.parse("P1M"));
        return new Periode(skjæringstidspunkt.minus(periodeFør), tomDagensDato && tom.isBefore(LocalDate.now()) ? LocalDate.now() : tom);
    }

    @Override
    public Periode utledOpplysningsperiodeSkattegrunnlag(Long behandlingId) {
        var fagsakperiodeTom = behandlingRepository.hentBehandling(behandlingId)
            .getFagsak()
            .getPeriode()
            .getTomDato();
        var førsteSkjæringstidspunkt = førsteUttaksdag(behandlingId);
        return SkattegrunnlaginnhentingTjeneste.utledSkattegrunnlagOpplysningsperiode(førsteSkjæringstidspunkt, fagsakperiodeTom);
    }

    private LocalDate førsteUttaksdag(Long behandlingId) {
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Mangler sønadsperiode for behandlingId=" + behandlingId));

        return søknadsperioder.getMaksPeriode().getFomDato();
    }

}
