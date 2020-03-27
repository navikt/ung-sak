package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.person.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.k9.sak.domene.uttak.input.UttakInput;
import no.nav.k9.sak.domene.uttak.repo.Ferie;
import no.nav.k9.sak.domene.uttak.repo.OppgittTilsynsordning;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperioder;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.domene.uttak.repo.pleiebehov.PleiebehovResultat;
import no.nav.k9.sak.domene.uttak.repo.pleiebehov.PleiebehovResultatRepository;
import no.nav.k9.sak.domene.uttak.repo.pleiebehov.Pleieperioder;
import no.nav.k9.sak.domene.uttak.uttaksplan.input.Person;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
public class UttakInputTjeneste {

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SøknadRepository søknadRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private PleiebehovResultatRepository pleiebehovResultatRepository;
    private UttakRepository uttakRepository;

    @Inject
    public UttakInputTjeneste(SøknadRepository søknadRepository,
                              PleiebehovResultatRepository pleiebehovResultatRepository,
                              UttakRepository uttakRepository,
                              InntektArbeidYtelseTjeneste iayTjeneste,
                              BasisPersonopplysningTjeneste personopplysningTjeneste,
                              VilkårResultatRepository vilkårResultatRepository) {
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
        this.uttakRepository = uttakRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.søknadRepository = Objects.requireNonNull(søknadRepository, "søknadRepository");
        this.vilkårResultatRepository = vilkårResultatRepository;
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
        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);

        var fastsattUttak = lagFastsattUttakAktivitetPerioder(ref);
        var søknadsperioder = lagSøknadsperioder(ref);
        var ferie = lagFerie(ref);
        var tilsynsordning = lagTilsynsordning(ref);

        Person søker = hentSøker(personopplysninger);
        Person pleietrengende = hentPleietrengende(ref.getPleietrengendeAktørId(), personopplysninger);
        var pleieperioder = hentPleieperioder(behandlingId);

        var medlemskapsperioder = hentMedlemskapsperioder(behandlingId);

        var uttakInput = new UttakInput(ref, iayGrunnlag)
            .medSøker(søker)
            .medPleietrengende(pleietrengende)
            .medUttakAktivitetPerioder(fastsattUttak)
            .medSøknadsperioder(søknadsperioder)
            .medFerie(ferie)
            .medTilsynsordning(tilsynsordning)
            .medPleieperioder(pleieperioder)
            .medMedlemskapVilkår(medlemskapsperioder)
            .medSøknadMottattDato(søknad.getMottattDato());

        return uttakInput;
    }

    private Vilkår hentMedlemskapsperioder(Long behandlingId) {
        var medlemskapsVilkår = vilkårResultatRepository.hent(behandlingId).getVilkår(VilkårType.MEDLEMSKAPSVILKÅRET).orElse(null);
        return medlemskapsVilkår;
    }

    private Person hentSøker(PersonopplysningerAggregat personopplysninger) {
        Person søker = lagPerson(personopplysninger.getSøker());
        return søker;
    }

    private Person hentPleietrengende(AktørId pleietrengendeAktørId, PersonopplysningerAggregat personopplysninger) {
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
