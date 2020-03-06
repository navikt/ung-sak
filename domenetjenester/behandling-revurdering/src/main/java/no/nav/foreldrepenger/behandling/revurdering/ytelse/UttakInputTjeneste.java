package no.nav.foreldrepenger.behandling.revurdering.ytelse;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningGrunnlagDiff;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatusPeriode;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;

@ApplicationScoped
public class UttakInputTjeneste {

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private MedlemTjeneste medlemTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningTjeneste kalkulusTjeneste;
    private SøknadRepository søknadRepository;
    private PersonopplysningRepository personopplysningRepository;

    @Inject
    public UttakInputTjeneste(BehandlingRepositoryProvider repositoryProvider,
                              BeregningTjeneste kalkulusTjeneste,
                              InntektArbeidYtelseTjeneste iayTjeneste,
                              MedlemTjeneste medlemTjeneste) {
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.medlemTjeneste = Objects.requireNonNull(medlemTjeneste, "medlemTjeneste");
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
    }

    UttakInputTjeneste() {
        // for CDI proxys
    }

    public UttakInput lagInput(BehandlingReferanse ref) {
        var behandlingId = ref.getBehandlingId();
        var iayGrunnlag = iayTjeneste.finnGrunnlag(behandlingId).orElse(null);
        var medlemskapOpphørsdato = medlemTjeneste.hentOpphørsdatoHvisEksisterer(behandlingId);
        return lagInput(ref, iayGrunnlag, medlemskapOpphørsdato.orElse(null));
    }

    private UttakInput lagInput(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate medlemskapOpphørsdato) {
        var mottattDato = søknadRepository.hentSøknadHvisEksisterer(ref.getBehandlingId()).map(SøknadEntitet::getMottattDato).orElse(null);
        var årsaker = finnÅrsaker(ref);
        var statusPerioder = lagBeregningsgrunnlagStatusPerioder(ref);
        return new UttakInput(ref, iayGrunnlag)
            .medBeregningsgrunnlagPerioder(statusPerioder)
            .medSøknadMottattDato(mottattDato)
            .medBehandlingÅrsaker(map(årsaker))
            .medBehandlingManueltOpprettet(erManueltOpprettet(årsaker))
            .medErOpplysningerOmDødEndret(erOpplysningerOmDødEndret(ref));
    }

    private Set<BehandlingÅrsakType> map(Set<BehandlingÅrsak> årsaker) {
        return årsaker.stream().map(BehandlingÅrsak::getBehandlingÅrsakType).collect(Collectors.toSet());
    }

    private boolean erManueltOpprettet(Set<BehandlingÅrsak> årsaker) {
        return årsaker.stream().anyMatch(BehandlingÅrsak::erManueltOpprettet);
    }

    private Set<BehandlingÅrsak> finnÅrsaker(BehandlingReferanse ref) {
        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        return new HashSet<>(behandling.getBehandlingÅrsaker());
    }

    private Collection<BeregningsgrunnlagStatusPeriode> lagBeregningsgrunnlagStatusPerioder(BehandlingReferanse ref) {
        var beregningsgrunnlag = kalkulusTjeneste.hentFastsatt(ref.getBehandlingId());
        if (beregningsgrunnlag.isPresent()) {
            var andeler = beregningsgrunnlag.get().getBeregningsgrunnlagPerioder().stream()
                .flatMap(beregningsgrunnlagPeriode -> beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
                .collect(Collectors.toList());
            return andeler.stream().map(this::mapAndel).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private BeregningsgrunnlagStatusPeriode mapAndel(BeregningsgrunnlagPrStatusOgAndel a) {
        var arbeidsforholdRef = a.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef).orElse(null);
        var arbeidsgiver = a.getArbeidsgiver().orElse(null);
        LocalDate fom = a.getBeregningsperiodeFom();
        LocalDate tom = a.getBeregningsperiodeTom();
        return new BeregningsgrunnlagStatusPeriode(a.getAktivitetStatus(), fom, tom, arbeidsgiver, arbeidsforholdRef);
    }

    private boolean erOpplysningerOmDødEndret(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        PersonopplysningGrunnlagEntitet originaltGrunnlag = personopplysningRepository.hentFørsteVersjonAvPersonopplysninger(behandlingId);
        PersonopplysningGrunnlagEntitet nåværendeGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);
        PersonopplysningGrunnlagDiff poDiff = new PersonopplysningGrunnlagDiff(ref.getAktørId(), nåværendeGrunnlag, originaltGrunnlag);

        boolean barnDødt = poDiff.erBarnDødsdatoEndret();
        boolean foreldreDød = poDiff.erForeldreDødsdatoEndret();

        return barnDødt || foreldreDød;
    }
}
