package no.nav.foreldrepenger.behandling.revurdering.ytelse;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.medisinsk.MedisinskGrunnlag;
import no.nav.k9.sak.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.behandlingslager.behandling.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.pleiebehov.Pleieperioder;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.medlem.MedlemTjeneste;
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
    private PleiebehovResultatRepository pleiebehovResultatRepository;
    private UttakRepository uttakRepository;

    @Inject
    public UttakInputTjeneste(SøknadRepository søknadRepository,
                              MedisinskGrunnlagRepository medisinskGrunnlagRepository,
                              PleiebehovResultatRepository pleiebehovResultatRepository,
                              UttakRepository uttakRepository,
                              InntektArbeidYtelseTjeneste iayTjeneste,
                              BasisPersonopplysningTjeneste personopplysningTjeneste,
                              MedlemTjeneste medlemTjeneste) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
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
        Long behandlingId = ref.getBehandlingId();
        SøknadEntitet søknad = søknadRepository.hentSøknadHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Har ikke søknad for behandling " + ref));
        MedisinskGrunnlag medisinskGrunnlag = medisinskGrunnlagRepository.hentHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Har ikke Medisinsk Grunnlag for behandling " + ref));
        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);

        var fastsattUttak = lagFastsattUttakAktivitetPerioder(ref);
        var søknadsperioder = lagSøknadsperioder(ref);
        var ferie = lagFerie(ref);
        var tilsynsordning = lagTilsynsordning(ref);

        Person søker = hentSøker(personopplysninger);
        Person pleietrengende = hentPleietrengende(medisinskGrunnlag, personopplysninger);
        var pleieperioder = hentPleieperioder(behandlingId);
        
        var medlemskapsperioder = hentMedlemskapsperioder(behandlingId);

        var uttakInput =  new UttakInput(ref, iayGrunnlag)
            .medSøker(søker)
            .medPleietrengende(pleietrengende)
            .medUttakAktivitetPerioder(fastsattUttak)
            .medSøknadsperioder(søknadsperioder)
            .medFerie(ferie)
            .medTilsynsordning(tilsynsordning)
            .medPleieperioder(pleieperioder)
            .medMedlemskap(medlemskapsperioder)
            .medSøknadMottattDato(søknad.getMottattDato());
        
        return uttakInput;
    }

    private VurdertMedlemskapPeriodeEntitet hentMedlemskapsperioder(Long behandlingId) {
        var medlem = medlemTjeneste.hentMedlemskap(behandlingId).flatMap(MedlemskapAggregat::getVurderingLøpendeMedlemskap).orElse(null);
        return medlem;
    }

    private Person hentSøker(PersonopplysningerAggregat personopplysninger) {
        Person søker = lagPerson(personopplysninger.getSøker());
        return søker;
    }

    private Person hentPleietrengende(MedisinskGrunnlag medisinskGrunnlag, PersonopplysningerAggregat personopplysninger) {
        var pleietrengendeAktørId = medisinskGrunnlag.getPleietrengende().getAktørId();
        Person pleietrengende = lagPerson(personopplysninger.getPersonopplysning(pleietrengendeAktørId));
        return pleietrengende;
    }

    private Pleieperioder hentPleieperioder(Long behandlingId) {
        return pleiebehovResultatRepository.hentHvisEksisterer(behandlingId).map(PleiebehovResultat::getPleieperioder).orElse(null);
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
