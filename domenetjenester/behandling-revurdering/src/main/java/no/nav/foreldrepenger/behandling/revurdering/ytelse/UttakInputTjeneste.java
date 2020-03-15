package no.nav.foreldrepenger.behandling.revurdering.ytelse;

import java.util.Collection;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlag;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.uttak.input.UttakInput;
import no.nav.k9.sak.domene.uttak.input.UttakPersonInfo;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;

@ApplicationScoped
public class UttakInputTjeneste {

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private MedlemTjeneste medlemTjeneste;
    private SøknadRepository søknadRepository;
    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private UttakRepository uttakRepository;

    @Inject
    public UttakInputTjeneste(SøknadRepository søknadRepository,
                              MedisinskGrunnlagRepository medisinskGrunnlagRepository,
                              UttakRepository uttakRepository,
                              InntektArbeidYtelseTjeneste iayTjeneste,
                              BasisPersonopplysningTjeneste personopplysningTjeneste,
                              MedlemTjeneste medlemTjeneste) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.uttakRepository = uttakRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.medlemTjeneste = Objects.requireNonNull(medlemTjeneste, "medlemTjeneste");
        this.søknadRepository = Objects.requireNonNull(søknadRepository, "søknadRepository");
    }

    UttakInputTjeneste() {
        // for CDI proxys
    }

    public UttakInput lagInput(BehandlingReferanse ref) {
        var behandlingId = ref.getBehandlingId();
        var iayGrunnlag = iayTjeneste.finnGrunnlag(behandlingId).orElse(null);
        return lagInput(ref, iayGrunnlag);
    }

    private UttakInput lagInput(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        SøknadEntitet søknad = søknadRepository.hentSøknadHvisEksisterer(ref.getBehandlingId())
            .orElseThrow(() -> new IllegalStateException("Har ikke søknad for behandling " + ref));
        MedisinskGrunnlag medisinskGrunnlag = medisinskGrunnlagRepository.hentHvisEksisterer(ref.getBehandlingId())
            .orElseThrow(() -> new IllegalStateException("Har ikke Medisinsk Grunnlag for behandling " + ref));

        var statusPerioder = lagUttakAktivitetPerioder(ref);
        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);

        UttakPersonInfo søker = lagPerson(personopplysninger.getSøker());

        var pleietrengendeAktørId = medisinskGrunnlag.getPleietrengende().getAktørId();
        UttakPersonInfo pleietrengende = lagPerson(personopplysninger.getPersonopplysning(pleietrengendeAktørId));

        return new UttakInput(ref, iayGrunnlag)
            .medSøker(søker)
            .medPleietrengende(pleietrengende)
            .medUttakAktivitetPerioder(statusPerioder)
            .medSøknadMottattDato(søknad.getMottattDato());
    }

    private UttakPersonInfo lagPerson(PersonopplysningEntitet person) {
        UttakPersonInfo søker = new UttakPersonInfo(person.getAktørId(), person.getFødselsdato(), person.getDødsdato());
        return søker;
    }

    private Collection<UttakAktivitetPeriode> lagUttakAktivitetPerioder(BehandlingReferanse ref) {
        var fastsattUttak = uttakRepository.hentFastsattUttak(ref.getBehandlingId());
        return fastsattUttak.getPerioder();
    }

}
