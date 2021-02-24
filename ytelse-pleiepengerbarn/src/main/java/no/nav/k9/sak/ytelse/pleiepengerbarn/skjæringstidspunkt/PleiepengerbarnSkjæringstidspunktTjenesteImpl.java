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
    private OpptjeningRepository opptjeningRepository;
    private SøknadsperiodeRepository uttakRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    private Period periodeEtter = Period.parse("P4Y");
    private Period periodeFør = Period.parse("P17M");

    PleiepengerbarnSkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public PleiepengerbarnSkjæringstidspunktTjenesteImpl(BehandlingRepository behandlingRepository,
                                                         OpptjeningRepository opptjeningRepository,
                                                         SøknadsperiodeRepository uttakRepository,
                                                         VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.opptjeningRepository = opptjeningRepository;
        this.uttakRepository = uttakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
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

        opptjeningRepository.finnOpptjening(behandlingId)
            .flatMap(it -> it.finnOpptjening(førsteUttaksdato)) // TODO: Dette er neppe optimalt ...
            .map(opptjening -> opptjening.getTom().plusDays(1))
            .ifPresent(skjæringstidspunkt -> {
                builder.medUtledetSkjæringstidspunkt(skjæringstidspunkt);
            });

        return builder.build();
    }

    @Override
    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        return Optional.empty();
    }

    private LocalDate førsteUttaksdag(Long behandlingId) {
        var søknadsperioder = uttakRepository.hentGrunnlag(behandlingId);
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId);

        if (søknadsperioder.isPresent()) {
            final var oppgittFordeling = søknadsperioder.get();
            final var førstePeriode = oppgittFordeling.getOppgitteSøknadsperioder()
                .getPerioder()
                .stream()
                .map(Søknadsperioder::getPerioder)
                .flatMap(Collection::stream)
                .map(Søknadsperiode::getPeriode)
                .min(DatoIntervallEntitet::compareTo);
            final var førsteDagIUttaket = oppgittFordeling.getOppgitteSøknadsperioder()
                .getPerioder()
                .stream()
                .map(Søknadsperioder::getPerioder)
                .flatMap(Collection::stream)
                .map(Søknadsperiode::getPeriode)
                .map(DatoIntervallEntitet::getFomDato)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

            if (vilkårene.isPresent()) {
                final var spesifiktVilkår = vilkårene.get().getVilkårene().stream().filter(it -> VilkårType.OPPTJENINGSVILKÅRET.equals(it.getVilkårType())).findFirst();
                if (spesifiktVilkår.isPresent() && førstePeriode.isPresent()) {
                    final var vilkårPeriode = spesifiktVilkår.get().getPerioder()
                        .stream()
                        .map(VilkårPeriode::getPeriode)
                        .filter(periode -> periode.hengerSammen(førstePeriode.get()))
                        .min(DatoIntervallEntitet::compareTo);

                    if (vilkårPeriode.isEmpty()) {
                        throw new IllegalStateException("Utvikler feil: Fant ingen gyldig vilkårsperiode for dato=" + førsteDagIUttaket + ". Skal ikke forekomme!");
                    } else {
                        final var periode = vilkårPeriode.get();
                        if (periode.getFomDato().isBefore(førsteDagIUttaket)) {
                            return periode.getFomDato();
                        }
                    }
                }
            }

            return førsteDagIUttaket;
        }
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return behandling.getOpprettetDato().toLocalDate();
    }

    @Override
    public Periode utledOpplysningsperiode(Long behandlingId, FagsakYtelseType ytelseType, boolean tomDagensDato) {
        LocalDate skjæringstidspunkt = this.utledSkjæringstidspunktForRegisterInnhenting(behandlingId, ytelseType);
        LocalDate tom = skjæringstidspunkt.plus(periodeEtter);
        return new Periode(skjæringstidspunkt.minus(periodeFør), tomDagensDato && tom.isBefore(LocalDate.now()) ? LocalDate.now() : tom);
    }
}
