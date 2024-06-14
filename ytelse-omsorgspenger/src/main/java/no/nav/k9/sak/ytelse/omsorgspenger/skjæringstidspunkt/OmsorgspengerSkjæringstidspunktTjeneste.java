package no.nav.k9.sak.ytelse.omsorgspenger.skjæringstidspunkt;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

@FagsakYtelseTypeRef(OMSORGSPENGER)
@ApplicationScoped
public class OmsorgspengerSkjæringstidspunktTjeneste implements SkjæringstidspunktTjeneste {

    private BehandlingRepository behandlingRepository;
    private OpptjeningRepository opptjeningRepository;
    private OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private OmsorgspengerOpphørtidspunktTjeneste opphørTidspunktTjeneste;

    OmsorgspengerSkjæringstidspunktTjeneste() {
        // CDI
    }

    @Inject
    public OmsorgspengerSkjæringstidspunktTjeneste(ÅrskvantumTjeneste årskvantumTjeneste,
                                                   BehandlingRepository behandlingRepository,
                                                   OpptjeningRepository opptjeningRepository,
                                                   OmsorgspengerGrunnlagRepository omsorgspengerGrunnlagRepository,
                                                   OmsorgspengerOpphørtidspunktTjeneste omsorgspengerOpphørtidspunktTjeneste,
                                                   VilkårResultatRepository vilkårResultatRepository) {
        this.opphørTidspunktTjeneste = omsorgspengerOpphørtidspunktTjeneste;
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
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        Builder builder = Skjæringstidspunkt.builder();

        LocalDate førsteUttaksdato = førsteUttaksdag(behandlingId);
        builder.medUtledetSkjæringstidspunkt(førsteUttaksdato);

        opptjeningRepository.finnOpptjening(behandlingId)
            .flatMap(it -> it.finnOpptjening(førsteUttaksdato))
            .map(opptjening -> opptjening.getTom().plusDays(1))
            .ifPresent(builder::medUtledetSkjæringstidspunkt);

        return builder.build();
    }

    private LocalDate førsteUttaksdag(Long behandlingId) {
        var søknadsperioder = omsorgspengerGrunnlagRepository.hentSammenslåtteFraværPerioder(behandlingId);
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(behandlingId);

        if (!søknadsperioder.isEmpty()) {
            var førstePeriode = søknadsperioder
                .stream()
                .map(OppgittFraværPeriode::getPeriode)
                .min(DatoIntervallEntitet::compareTo);

            var førsteDagIUttaket = søknadsperioder
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

}
