package no.nav.k9.sak.ytelse.pleiepengerbarn.registerdata;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.registerinnhenting.OpplysningsperiodeTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.skjæringstidspunkt.SkattegrunnlaginnhentingTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@ApplicationScoped
public class PleiepengerOgOpplæringspengerOpplysningsperiodeTjeneste implements OpplysningsperiodeTjeneste {

    private BehandlingRepository behandlingRepository;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;

    private final Period periodeEtter = Period.parse("P3M");
    private final Period periodeFør = Period.parse("P17M");

    PleiepengerOgOpplæringspengerOpplysningsperiodeTjeneste() {
        // CDI
    }

    @Inject
    public PleiepengerOgOpplæringspengerOpplysningsperiodeTjeneste(BehandlingRepository behandlingRepository, SøknadsperiodeTjeneste søknadsperiodeTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
    }

    @Override
    public Periode utledOpplysningsperiode(Long behandlingId, boolean tomDagensDato) {
        var skjæringstidspunkt = førsteUttaksdag(behandlingId);

        var tom = behandlingRepository.hentBehandling(behandlingId)
            .getFagsak()
            .getPeriode()
            .getTomDato()
            .plus(periodeEtter);

        return new Periode(skjæringstidspunkt.minus(periodeFør), tomDagensDato && tom.isBefore(LocalDate.now()) ? LocalDate.now() : tom);
    }

    @Override
    public Periode utledOpplysningsperiodeSkattegrunnlag(Long behandlingId) {
        var fagsakperiodeTom = behandlingRepository.hentBehandling(behandlingId)
            .getFagsak()
            .getPeriode()
            .getTomDato();
        var førsteSkjæringstidspunkt = førsteUttaksdag(behandlingId);
        return SkattegrunnlaginnhentingTjeneste.utledSkattegrunnlagOpplysningsperiode(førsteSkjæringstidspunkt, fagsakperiodeTom, LocalDate.now());
    }

    private LocalDate førsteUttaksdag(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var kravperioder = søknadsperiodeTjeneste.utledPeriode(behandlingId);
        var førsteKravdato = kravperioder.stream().map(DatoIntervallEntitet::getFomDato).min(Comparator.naturalOrder());
        return førsteKravdato.orElse(behandling.getFagsak().getPeriode().getFomDato());
    }

}
