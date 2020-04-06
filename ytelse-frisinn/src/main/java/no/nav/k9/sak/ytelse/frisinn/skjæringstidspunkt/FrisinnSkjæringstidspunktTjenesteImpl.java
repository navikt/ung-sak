package no.nav.k9.sak.ytelse.frisinn.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.Skjæringstidspunkt.Builder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class FrisinnSkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste {

    private BehandlingRepository behandlingRepository;
    private UttakRepository uttakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private OpphørUttakTjeneste opphørUttakTjeneste;

    FrisinnSkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public FrisinnSkjæringstidspunktTjenesteImpl(BehandlingRepository behandlingRepository, 
                                                 UttakRepository uttakRepository, 
                                                 UttakTjeneste uttakTjeneste,
                                                 VilkårResultatRepository vilkårResultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.uttakRepository = uttakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.opphørUttakTjeneste = new OpphørUttakTjeneste(uttakTjeneste);
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
        builder.medFørsteUttaksdato(førsteUttaksdato);
        builder.medUtledetSkjæringstidspunkt(førsteUttaksdato);
        return builder.build();
    }
    
    @Override
    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        return null;
    }

    private LocalDate førsteUttaksdag(Long behandlingId) {
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingId);
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId);

        if (søknadsperioder.isPresent()) {
            final var oppgittFordeling = søknadsperioder.get();
            final var førstePeriode = oppgittFordeling.getPerioder()
                .stream()
                .map(Søknadsperiode::getPeriode)
                .min(DatoIntervallEntitet::compareTo);
            final var førsteDagIUttaket = oppgittFordeling.getPerioder()
                .stream()
                .map(Søknadsperiode::getPeriode)
                .map(DatoIntervallEntitet::getFomDato)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

            if (vilkårene.isPresent()) {
                final var spesifiktVilkår = vilkårene.get().getVilkårene().stream().filter(it -> VilkårType.OPPTJENINGSVILKÅRET.equals(it.getVilkårType())).findFirst();
                if (spesifiktVilkår.isPresent() && førstePeriode.isPresent()) {
                    final var vilkårPeriode = spesifiktVilkår.get().getPerioder()
                        .stream()
                        .filter(it -> it.getPeriode().hengerSammen(førstePeriode.get()))
                        .map(VilkårPeriode::getPeriode)
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
    public boolean harAvslåttPeriode(UUID behandlingUuid) {
        return opphørUttakTjeneste.harAvslåttUttakPeriode(behandlingUuid);
    }
}
