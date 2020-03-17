package no.nav.foreldrepenger.behandling.revurdering.ytelse;

import java.util.Collection;
import java.util.Collections;
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
import no.nav.k9.sak.domene.uttak.repo.Ferie;
import no.nav.k9.sak.domene.uttak.repo.OppgittTilsynsordning;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.Person;

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

        var fastsattUttak = lagFastsattUttakAktivitetPerioder(ref);
        var søknadsperioder = lagSøknadsperioder(ref);
        var ferie = lagFerie(ref);
        var tilsynsordning = lagTilsynsordning(ref);
        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);
        Person søker = lagPerson(personopplysninger.getSøker());

        var pleietrengendeAktørId = medisinskGrunnlag.getPleietrengende().getAktørId();
        Person pleietrengende = lagPerson(personopplysninger.getPersonopplysning(pleietrengendeAktørId));
        
        var tilsynbehov = medisinskGrunnlag.getKontinuerligTilsyn();
        
        return new UttakInput(ref, iayGrunnlag)
            .medSøker(søker)
            .medPleietrengende(pleietrengende)
            .medUttakAktivitetPerioder(fastsattUttak)
            .medSøknadsperioder(søknadsperioder)
            .medFerie(ferie)
            .medTilsynsordning(tilsynsordning)
            .medTilsynbehov(tilsynbehov)
            .medSøknadMottattDato(søknad.getMottattDato());
    }

    private Person lagPerson(PersonopplysningEntitet pe) {
        return new Person(pe.getAktørId() == null ? null : pe.getAktørId().getId(),
            pe.getFødselsdato(),
            pe.getDødsdato());
    }

    private Collection<UttakAktivitetPeriode> lagFastsattUttakAktivitetPerioder(BehandlingReferanse ref) {
        // FIXME K9: etabler alltid fastsatt uttak i fakta om uttak steg i sted fallback
        Long behandlingId = ref.getBehandlingId();
        var res = uttakRepository.hentFastsattUttakHvisEksisterer(behandlingId).orElse(null);
        return res == null ? Collections.emptyList() : res.getPerioder();
    }

    private Søknadsperioder lagSøknadsperioder(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        var res = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingId).orElse(null);
        if (res.getPerioder() == null || res.getPerioder().isEmpty()) {
            throw new IllegalStateException("Mangler søkndadsperioder for behandling: " + ref);
        }
        return res;
    }

    private Ferie lagFerie(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        Ferie res = uttakRepository.hentOppgittFerieHvisEksisterer(behandlingId).orElse(null);
        return res;
    }

    private OppgittTilsynsordning lagTilsynsordning(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        var res = uttakRepository.hentOppgittTilsynsordningHvisEksisterer(behandlingId).orElse(null);
        return res;
    }

}
