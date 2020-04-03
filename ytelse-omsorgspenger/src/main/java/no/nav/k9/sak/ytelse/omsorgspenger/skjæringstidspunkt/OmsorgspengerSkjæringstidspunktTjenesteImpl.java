package no.nav.k9.sak.ytelse.omsorgspenger.skjæringstidspunkt;

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
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerSkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste {

    private BehandlingRepository behandlingRepository;
    private OpptjeningRepository opptjeningRepository;
    private OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private OmsorgspengerOpphørtidspunktTjeneste opphørTidspunktTjeneste;

    OmsorgspengerSkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public OmsorgspengerSkjæringstidspunktTjenesteImpl(ÅrskvantumTjeneste årskvantumTjeneste, 
                                                       BehandlingRepository behandlingRepository, 
                                                       OpptjeningRepository opptjeningRepository,
                                                       OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository, 
                                                       VilkårResultatRepository vilkårResultatRepository) {
        this.opphørTidspunktTjeneste = new OmsorgspengerOpphørtidspunktTjeneste(årskvantumTjeneste);
        this.behandlingRepository = behandlingRepository;
        this.opptjeningRepository = opptjeningRepository;
        this.omsorgspengerGrunnlagRepository = omsorgspengerGrunnlagRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        return opphørTidspunktTjeneste.getOpphørsdato(ref);
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId, FagsakYtelseType ytelseType) {
        // FIXME K9 skjæringstidspunkt
        return førsteUttaksdag(behandlingId);
    }
    
    @Override
    public boolean harAvslåttPeriode(UUID behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid);
        var ref = BehandlingReferanse.fra(behandling);
        return opphørTidspunktTjeneste.harAvslåttPeriode(ref);
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        Builder builder = Skjæringstidspunkt.builder();

        LocalDate førsteUttaksdato = førsteUttaksdag(behandlingId);
        builder.medFørsteUttaksdato(førsteUttaksdato);
        builder.medUtledetSkjæringstidspunkt(førsteUttaksdato);

        opptjeningRepository.finnOpptjening(behandlingId)
            .map(opptjening -> opptjening.getTom().plusDays(1))
            .ifPresent(skjæringstidspunkt -> {
                builder.medSkjæringstidspunktOpptjening(skjæringstidspunkt);
                builder.medUtledetSkjæringstidspunkt(skjæringstidspunkt);
            });

        return builder.build();
    }

    private LocalDate førsteUttaksdag(Long behandlingId) {
        var søknadsperioder = omsorgspengerGrunnlagRepository.hentOppgittFraværHvisEksisterer(behandlingId);
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId);

        if (søknadsperioder.isPresent()) {
            final var oppgittFordeling = søknadsperioder.get();
            final var førstePeriode = oppgittFordeling.getPerioder()
                .stream()
                .map(OppgittFraværPeriode::getPeriode)
                .min(DatoIntervallEntitet::compareTo);
            final var førsteDagIUttaket = oppgittFordeling.getPerioder()
                .stream()
                .map(OppgittFraværPeriode::getPeriode)
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
}
