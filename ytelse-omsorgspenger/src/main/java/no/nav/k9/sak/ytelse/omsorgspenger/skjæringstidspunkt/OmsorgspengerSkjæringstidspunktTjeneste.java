package no.nav.k9.sak.ytelse.omsorgspenger.skjæringstidspunkt;

import java.time.LocalDate;
import java.time.Period;
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
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerSkjæringstidspunktTjeneste implements SkjæringstidspunktTjeneste {

    private BehandlingRepository behandlingRepository;
    private OpptjeningRepository opptjeningRepository;
    private OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private OmsorgspengerOpphørtidspunktTjeneste opphørTidspunktTjeneste;

    private Period periodeFør = Period.parse("P12M");

    OmsorgspengerSkjæringstidspunktTjeneste() {
        // CDI
    }

    @Inject
    public OmsorgspengerSkjæringstidspunktTjeneste(ÅrskvantumTjeneste årskvantumTjeneste,
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
        builder.medUtledetSkjæringstidspunkt(førsteUttaksdato);

        opptjeningRepository.finnOpptjening(behandlingId)
            .flatMap(it -> it.finnOpptjening(førsteUttaksdato))
            .map(opptjening -> opptjening.getTom().plusDays(1))
            .ifPresent(skjæringstidspunkt -> {
                builder.medUtledetSkjæringstidspunkt(skjæringstidspunkt);
            });

        return builder.build();
    }

    private LocalDate førsteUttaksdag(Long behandlingId) {
        var søknadsperioder = omsorgspengerGrunnlagRepository.hentOppgittFraværHvisEksisterer(behandlingId);
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId);

        if (søknadsperioder.isPresent()) {
            var oppgittFordeling = søknadsperioder.get();
            var førstePeriode = oppgittFordeling.getPerioder()
                .stream()
                .map(OppgittFraværPeriode::getPeriode)
                .min(DatoIntervallEntitet::compareTo);

            var førsteDagIUttaket = oppgittFordeling.getPerioder()
                .stream()
                .map(OppgittFraværPeriode::getPeriode)
                .map(DatoIntervallEntitet::getFomDato)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

            if (vilkårene.isPresent()) {
                var spesifiktVilkår = vilkårene.get()
                    .getVilkårene()
                    .stream()
                    .filter(it -> VilkårType.OPPTJENINGSVILKÅRET.equals(it.getVilkårType()))
                    .findFirst();

                if (spesifiktVilkår.isPresent() && førstePeriode.isPresent()) {
                    var vilkårPeriode = spesifiktVilkår.get().getPerioder()
                        .stream()
                        .map(VilkårPeriode::getPeriode)
                        .min(DatoIntervallEntitet::compareTo);

                    if (vilkårPeriode.isPresent()) {
                        var periode = vilkårPeriode.get();
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
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LocalDate tom = behandling.getFagsak().getPeriode().getTomDato();

        LocalDate skjæringstidspunkt = this.utledSkjæringstidspunktForRegisterInnhenting(behandlingId, ytelseType);
        return new Periode(skjæringstidspunkt.minus(periodeFør), tomDagensDato && tom.isBefore(LocalDate.now()) ? LocalDate.now() : tom);
    }
}
