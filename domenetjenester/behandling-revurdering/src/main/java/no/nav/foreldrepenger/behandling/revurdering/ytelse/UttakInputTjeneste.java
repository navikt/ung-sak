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

import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
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
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class UttakInputTjeneste {

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private MedlemTjeneste medlemTjeneste;
    private BehandlingRepository behandlingRepository;
    private HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private SøknadRepository søknadRepository;
    private PersonopplysningRepository personopplysningRepository;

    @Inject
    public UttakInputTjeneste(BehandlingRepositoryProvider repositoryProvider,
                              HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                              InntektArbeidYtelseTjeneste iayTjeneste,
                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                              MedlemTjeneste medlemTjeneste) {
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.skjæringstidspunktTjeneste = Objects.requireNonNull(skjæringstidspunktTjeneste, "skjæringstidspunktTjeneste");
        this.medlemTjeneste = Objects.requireNonNull(medlemTjeneste, "medlemTjeneste");
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
    }

    UttakInputTjeneste() {
        // for CDI proxy
    }

    public UttakInput lagInput(Behandling behandling) {
        var behandlingId = behandling.getId();
        var iayGrunnlag = iayTjeneste.finnGrunnlag(behandlingId).orElse(null);
        var medlemskapOpphørsdato = medlemTjeneste.hentOpphørsdatoHvisEksisterer(behandling);
        return lagInput(behandling, iayGrunnlag, medlemskapOpphørsdato.orElse(null));
    }

    public UttakInput lagInput(Behandling behandling, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate medlemskapOpphørsdato) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return lagInput(ref, iayGrunnlag, medlemskapOpphørsdato);
    }

    public UttakInput lagInput(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate medlemskapOpphørsdato) {
        var mottattDato = søknadRepository.hentSøknadHvisEksisterer(ref.getBehandlingId()).map(SøknadEntitet::getMottattDato).orElse(null);
        var årsaker = finnÅrsaker(ref);
        var statusPerioder = lagBeregningsgrunnlagStatusPerioder(ref);
        return new UttakInput(ref, iayGrunnlag)
            .medMedlemskapOpphørsdato(medlemskapOpphørsdato)
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

    public UttakInput lagInput(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return lagInput(behandling);
    }

    private Collection<BeregningsgrunnlagStatusPeriode> lagBeregningsgrunnlagStatusPerioder(BehandlingReferanse ref) {
        var beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagForBehandling(ref.getBehandlingId());
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
