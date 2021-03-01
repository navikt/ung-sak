package no.nav.k9.sak.ytelse.pleiepengerbarn.skjæringstidspunkt;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.Skjæringstidspunkt.Builder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PleiepengerbarnSkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste {

    private BehandlingRepository behandlingRepository;

    private Period periodeEtter = Period.parse("P3M");
    private Period periodeFør = Period.parse("P17M");

    PleiepengerbarnSkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public PleiepengerbarnSkjæringstidspunktTjenesteImpl(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId, FagsakYtelseType ytelseType) {
        // FIXME K9 skjæringstidspunkt
        return førsteUttaksdag(behandlingId);
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        Builder builder = Skjæringstidspunkt.builder();

        LocalDate førsteUttaksdato = førsteUttaksdag(behandlingId);
        builder.medUtledetSkjæringstidspunkt(førsteUttaksdato);
        return builder.build();
    }

    @Override
    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        return Optional.empty();
    }

    private LocalDate førsteUttaksdag(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return behandling.getFagsak().getPeriode().getFomDato();
    }

    @Override
    public Periode utledOpplysningsperiode(Long behandlingId, FagsakYtelseType ytelseType, boolean tomDagensDato) {
        var skjæringstidspunkt = this.utledSkjæringstidspunktForRegisterInnhenting(behandlingId, ytelseType);

        var tom = behandlingRepository.hentBehandling(behandlingId)
            .getFagsak()
            .getPeriode()
            .getTomDato()
            .plus(periodeEtter);

        return new Periode(skjæringstidspunkt.minus(periodeFør), tomDagensDato && tom.isBefore(LocalDate.now()) ? LocalDate.now() : tom);
    }
}
