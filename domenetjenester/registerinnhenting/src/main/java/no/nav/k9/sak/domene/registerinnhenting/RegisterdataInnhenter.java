package no.nav.k9.sak.domene.registerinnhenting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.InnhentRegisterdataRequest;
import no.nav.abakus.iaygrunnlag.request.RegisterdataType;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandlingslager.aktør.Adresseinfo;
import no.nav.k9.sak.behandlingslager.aktør.Familierelasjon;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.k9.sak.behandlingslager.aktør.historikk.PersonstatusPeriode;
import no.nav.k9.sak.behandlingslager.aktør.historikk.StatsborgerskapPeriode;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.abakus.AbakusTjeneste;
import no.nav.k9.sak.domene.medlem.MedlemTjeneste;
import no.nav.k9.sak.domene.medlem.api.Medlemskapsperiode;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.registerinnhenting.impl.SaksopplysningerFeil;
import no.nav.k9.sak.domene.registerinnhenting.personopplysninger.AlleRelasjonFilter;
import no.nav.k9.sak.domene.registerinnhenting.personopplysninger.IngenRelasjonFilter;
import no.nav.k9.sak.domene.registerinnhenting.personopplysninger.OmsorgspengerRelasjonsFilter;
import no.nav.k9.sak.domene.registerinnhenting.personopplysninger.PleietrengendeRelasjonsFilter;
import no.nav.k9.sak.domene.registerinnhenting.personopplysninger.YtelsesspesifikkRelasjonsFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
public class RegisterdataInnhenter {

    private static final Logger log = LoggerFactory.getLogger(RegisterdataInnhenter.class);

    private final Map<FagsakYtelseType, YtelsesspesifikkRelasjonsFilter> barnRelasjonsFilter = Map.of(
        FagsakYtelseType.OMSORGSPENGER_KS, new AlleRelasjonFilter(),
        FagsakYtelseType.OMSORGSPENGER_MA, new AlleRelasjonFilter(),
        FagsakYtelseType.OMSORGSPENGER_AO, new AlleRelasjonFilter(),
        FagsakYtelseType.PSB, new PleietrengendeRelasjonsFilter(),
        FagsakYtelseType.OMP, new OmsorgspengerRelasjonsFilter());

    private PersoninfoAdapter personinfoAdapter;
    private MedlemTjeneste medlemTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private BehandlingRepository behandlingRepository;
    private MedlemskapRepository medlemskapRepository;
    private AbakusTjeneste abakusTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingLåsRepository behandlingLåsRepository;
    private Instance<InformasjonselementerUtleder> informasjonselementer;

    RegisterdataInnhenter() {
        // for CDI proxy
    }

    @Inject
    public RegisterdataInnhenter(PersoninfoAdapter personinfoAdapter, // NOSONAR - krever mange parametere
                                 MedlemTjeneste medlemTjeneste,
                                 BehandlingRepositoryProvider repositoryProvider,
                                 MedlemskapRepository medlemskapRepository,
                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                 AbakusTjeneste abakusTjeneste,
                                 @Any Instance<InformasjonselementerUtleder> utledInformasjonselementer) {
        this.personinfoAdapter = personinfoAdapter;
        this.medlemTjeneste = medlemTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingLåsRepository = repositoryProvider.getBehandlingLåsRepository();
        this.medlemskapRepository = medlemskapRepository;
        this.abakusTjeneste = abakusTjeneste;
        this.informasjonselementer = utledInformasjonselementer;
    }

    public Personinfo innhentSaksopplysningerForSøker(AktørId søkerAktørId) {
        return personinfoAdapter.hentPersoninfo(søkerAktørId);
    }

    public Personinfo innhentPersonopplysninger(Behandling behandling) {
        // Innhent data fra TPS for søker
        AktørId søkerAktørId = behandling.getAktørId();
        Personinfo søkerInfo = innhentSaksopplysningerForSøker(søkerAktørId);

        if (søkerInfo == null) {
            throw SaksopplysningerFeil.FACTORY.feilVedOppslagITPS(søkerAktørId.toString())
                .toException();
        }

        // Innhent øvrige data fra TPS
        var personInformasjonBuilder = byggPersonopplysningMedRelasjoner(søkerInfo, behandling);

        // lagre alt
        behandlingLåsRepository.taLås(behandling.getId());
        personopplysningRepository.lagre(behandling.getId(), personInformasjonBuilder);

        return søkerInfo;
    }

    public void innhentMedlemskapsOpplysning(Behandling behandling) {
        Long behandlingId = behandling.getId();

        // Innhent medl for søker
        List<MedlemskapPerioderEntitet> medlemskapsperioder = innhentMedlemskapsopplysninger(behandling.getAktørId(), behandling);
        medlemskapRepository.lagreMedlemskapRegisterOpplysninger(behandlingId, medlemskapsperioder);
    }

    private PersonInformasjonBuilder byggPersonopplysningMedRelasjoner(Personinfo søkerPersonInfo,
                                                                       Behandling behandling) {

        var informasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);

        // Historikk for søker
        var fagsakYtelseType = behandling.getFagsakYtelseType();
        var opplysningsperioden = skjæringstidspunktTjeneste.utledOpplysningsperiode(behandling.getId(), fagsakYtelseType, true);
        var personhistorikkinfo = personinfoAdapter.innhentPersonopplysningerHistorikk(søkerPersonInfo.getAktørId(), opplysningsperioden);
        if (personhistorikkinfo != null) {
            mapAdresser(personhistorikkinfo.getAdressehistorikk(), informasjonBuilder, søkerPersonInfo);
            mapStatsborgerskap(personhistorikkinfo.getStatsborgerskaphistorikk(), informasjonBuilder, søkerPersonInfo);
            mapPersonstatus(personhistorikkinfo.getPersonstatushistorikk(), informasjonBuilder, søkerPersonInfo);
        }

        mapTilPersonopplysning(søkerPersonInfo, informasjonBuilder, true, false, behandling);

        if (fagsakYtelseType.harRelatertePersoner()) {
            // legg til pleietrengende
            leggTilPleietrengende(informasjonBuilder, behandling);
            // Ektefelle
            leggTilEktefelle(søkerPersonInfo, informasjonBuilder, behandling);
            // Relatert person
            leggTilRelatertPerson(informasjonBuilder, behandling);

        }

        return informasjonBuilder;
    }

    private void leggTilPleietrengende(PersonInformasjonBuilder informasjonBuilder, Behandling behandling) {
        var pleietrengende = Optional.ofNullable(behandling.getFagsak().getPleietrengendeAktørId());
        if (pleietrengende.isPresent()) {
            var aktørId = pleietrengende.get();
            var personinfo = personinfoAdapter.hentPersoninfo(aktørId);
            if (personinfo != null) {
                log.info("Fant personinfo for angitt pleietrengende fra fagsak");
                if (harAktør(informasjonBuilder, personinfo)) {
                    log.info("har allerede mappet pleietrengende");
                    return;
                }
                mapTilPersonopplysning(personinfo, informasjonBuilder, false, true, behandling);
            } else {
                throw new IllegalStateException("Finner ikke personinfo i PDL for pleietrengende aktørid");
            }
        }
    }

    private boolean harAktør(PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        return informasjonBuilder.harAktørId(personinfo.getAktørId());
    }

    private void leggTilRelatertPerson(PersonInformasjonBuilder informasjonBuilder, Behandling behandling) {
        var relatertPerson = Optional.ofNullable(behandling.getFagsak().getRelatertPersonAktørId());
        if (relatertPerson.isPresent()) {
            var aktørId = relatertPerson.get();
            var personinfo = personinfoAdapter.hentPersoninfo(aktørId);
            if (personinfo != null) {
                log.info("Fant personinfo for angitt relatert person fra fagsak");
                mapTilPersonopplysning(personinfo, informasjonBuilder, false, true, behandling);
            } else {
                throw new IllegalStateException("Finner ikke personinfo i PDL for relatert person aktørid");
            }
        }
    }

    private void mapPersonstatus(List<PersonstatusPeriode> personstatushistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        for (PersonstatusPeriode personstatus : personstatushistorikk) {
            final PersonstatusType status = personstatus.getPersonstatus();
            final DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(
                brukFødselsdatoHvisEtter(personstatus.getGyldighetsperiode().getFom(), personinfo.getFødselsdato()),
                personstatus.getGyldighetsperiode().getTom());
            final PersonInformasjonBuilder.PersonstatusBuilder builder = informasjonBuilder.getPersonstatusBuilder(personinfo.getAktørId(), periode);
            builder.medPeriode(periode)
                .medPersonstatus(status);
            informasjonBuilder.leggTil(builder);
        }
    }

    private void mapStatsborgerskap(List<StatsborgerskapPeriode> statsborgerskaphistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        for (StatsborgerskapPeriode statsborgerskap : statsborgerskaphistorikk) {
            final Landkoder landkode = Landkoder.fraKode(statsborgerskap.getStatsborgerskap().getLandkode());

            Region region = Region.finnHøyestRangertRegion(Collections.singletonList(statsborgerskap.getStatsborgerskap().getLandkode()));

            final DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(brukFødselsdatoHvisEtter(
                statsborgerskap.getGyldighetsperiode().getFom(), personinfo.getFødselsdato()), statsborgerskap.getGyldighetsperiode().getTom());
            final PersonInformasjonBuilder.StatsborgerskapBuilder builder = informasjonBuilder.getStatsborgerskapBuilder(personinfo.getAktørId(), periode,
                landkode, region);
            builder.medPeriode(periode)
                .medStatsborgerskap(landkode);
            builder.medRegion(region);

            informasjonBuilder.leggTil(builder);
        }
    }

    private void mapAdresser(List<AdressePeriode> adressehistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        AktørId aktørId = personinfo.getAktørId();
        for (AdressePeriode adresse : adressehistorikk) {
            final DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(
                brukFødselsdatoHvisEtter(adresse.getGyldighetsperiode().getFom(), personinfo.getFødselsdato()), adresse.getGyldighetsperiode().getTom());
            final PersonInformasjonBuilder.AdresseBuilder adresseBuilder = informasjonBuilder.getAdresseBuilder(aktørId, periode,
                adresse.getAdresse().getAdresseType());
            adresseBuilder.medPeriode(periode)
                .medAdresselinje1(adresse.getAdresse().getAdresselinje1())
                .medAdresselinje2(adresse.getAdresse().getAdresselinje2())
                .medAdresselinje3(adresse.getAdresse().getAdresselinje3())
                .medAdresselinje4(adresse.getAdresse().getAdresselinje4())
                .medLand(adresse.getAdresse().getLand())
                .medPostnummer(adresse.getAdresse().getPostnummer());
            informasjonBuilder.leggTil(adresseBuilder);
        }
    }

    private LocalDate brukFødselsdatoHvisEtter(LocalDate dato, LocalDate fødseldato) {
        if (dato.isBefore(fødseldato)) {
            return fødseldato;
        }
        return dato;
    }

    private void mapTilPersonopplysning(Personinfo personinfo, PersonInformasjonBuilder informasjonBuilder, boolean skalHenteBarnRelasjoner,
                                        boolean erIkkeSøker, Behandling behandling) {
        mapInfoTilEntitet(personinfo, informasjonBuilder, erIkkeSøker);

        if (skalHenteBarnRelasjoner) {
            List<Personinfo> barna = hentBarnRelatertTil(personinfo, behandling);
            barna.forEach(barn -> {
                mapInfoTilEntitet(barn, informasjonBuilder, true);
                mapRelasjon(personinfo, barn, Collections.singletonList(RelasjonsRolleType.BARN), informasjonBuilder);
                mapRelasjon(barn, personinfo, utledRelasjonsrolleTilBarn(personinfo, barn), informasjonBuilder);
            });
        }
    }

    private List<RelasjonsRolleType> utledRelasjonsrolleTilBarn(Personinfo personinfo, Personinfo barn) {
        if (barn == null) {
            return Collections.emptyList();
        }
        return barn.getFamilierelasjoner().stream()
            .filter(fr -> fr.getPersonIdent().equals(personinfo.getPersonIdent()))
            .map(rel -> utledRelasjonsrolleTilBarn(personinfo.getKjønn(), rel.getRelasjonsrolle()))
            .collect(Collectors.toList());
    }

    private void mapRelasjon(Personinfo fra, Personinfo til, List<RelasjonsRolleType> roller, PersonInformasjonBuilder informasjonBuilder) {
        if (til == null) {
            return;
        }
        for (RelasjonsRolleType rolle : roller) {
            final PersonInformasjonBuilder.RelasjonBuilder builder = informasjonBuilder.getRelasjonBuilder(fra.getAktørId(), til.getAktørId(), rolle);
            builder.harSammeBosted(utledSammeBosted(fra, til, rolle));
            informasjonBuilder.leggTil(builder);
        }
    }

    private boolean utledSammeBosted(Personinfo personinfo, Personinfo barn, RelasjonsRolleType rolle) {
        final Optional<Boolean> sammeBosted = personinfo.getFamilierelasjoner().stream()
            .filter(fr -> fr.getRelasjonsrolle().equals(rolle) && fr.getPersonIdent().equals(barn.getPersonIdent()))
            .findAny()
            .map(familierelasjon -> familierelasjon.getHarSammeBosted(personinfo, barn));
        return sammeBosted.orElse(false);
    }

    private void mapInfoTilEntitet(Personinfo personinfo, PersonInformasjonBuilder informasjonBuilder, boolean lagreIHistoriskeTabeller) {
        if (harAktør(informasjonBuilder, personinfo)) {
            return;
        }

        final DatoIntervallEntitet periode = getPeriode(personinfo.getFødselsdato(), Tid.TIDENES_ENDE);
        final PersonInformasjonBuilder.PersonopplysningBuilder builder = informasjonBuilder.getPersonopplysningBuilder(personinfo.getAktørId());
        builder.medFødselsdato(personinfo.getFødselsdato())
            .medNavn(personinfo.getNavn())
            .medDødsdato(personinfo.getDødsdato())
            .medKjønn(personinfo.getKjønn())
            .medSivilstand(personinfo.getSivilstandType())
            .medRegion(personinfo.getRegion());
        informasjonBuilder.leggTil(builder);

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttStatsborgerskapHistorikk(personinfo.getAktørId())) {
            final PersonInformasjonBuilder.StatsborgerskapBuilder statsborgerskapBuilder = informasjonBuilder.getStatsborgerskapBuilder(personinfo.getAktørId(),
                periode, personinfo.getLandkode(), personinfo.getRegion());
            informasjonBuilder.leggTil(statsborgerskapBuilder);
        }

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttAdresseHistorikk(personinfo.getAktørId())) {
            for (Adresseinfo adresse : personinfo.getAdresseInfoList()) {
                final PersonInformasjonBuilder.AdresseBuilder adresseBuilder = informasjonBuilder.getAdresseBuilder(personinfo.getAktørId(),
                    periode, adresse.getGjeldendePostadresseType());
                informasjonBuilder.leggTil(adresseBuilder
                    .medAdresselinje1(adresse.getAdresselinje1())
                    .medAdresselinje2(adresse.getAdresselinje2())
                    .medAdresselinje3(adresse.getAdresselinje3())
                    .medPostnummer(adresse.getPostNr())
                    .medLand(adresse.getLand())
                    .medAdresseType(adresse.getGjeldendePostadresseType())
                    .medPeriode(periode));
            }
        }

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttPersonstatusHistorikk(personinfo.getAktørId())) {
            final PersonInformasjonBuilder.PersonstatusBuilder personstatusBuilder = informasjonBuilder.getPersonstatusBuilder(personinfo.getAktørId(),
                periode).medPersonstatus(personinfo.getPersonstatus());
            informasjonBuilder.leggTil(personstatusBuilder);
        }
    }

    private DatoIntervallEntitet getPeriode(LocalDate fom, LocalDate tom) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom != null ? tom : Tid.TIDENES_ENDE);
    }

    private void leggTilEktefelle(Personinfo søkerPersonInfo, PersonInformasjonBuilder informasjonBuilder, Behandling behandling) {
        // Ektefelle
        final List<Familierelasjon> familierelasjoner = søkerPersonInfo.getFamilierelasjoner()
            .stream()
            .filter(f -> f.getRelasjonsrolle().equals(RelasjonsRolleType.EKTE) ||
                f.getRelasjonsrolle().equals(RelasjonsRolleType.REGISTRERT_PARTNER) ||
                f.getRelasjonsrolle().equals(RelasjonsRolleType.SAMBOER))
            .collect(Collectors.toList());
        for (Familierelasjon familierelasjon : familierelasjoner) {
            var ident = familierelasjon.getPersonIdent();
            Optional<Personinfo> ektefelleInfo = personinfoAdapter.innhentSaksopplysninger(ident);
            if (ektefelleInfo.isPresent()) {
                final Personinfo personinfo = ektefelleInfo.get();
                mapTilPersonopplysning(personinfo, informasjonBuilder, false, true, behandling);
                mapRelasjon(søkerPersonInfo, personinfo, Collections.singletonList(familierelasjon.getRelasjonsrolle()), informasjonBuilder);
                mapRelasjon(personinfo, søkerPersonInfo, Collections.singletonList(familierelasjon.getRelasjonsrolle()), informasjonBuilder);
            } else {
                log.warn("Fant ikke personinfo for familierelasjon: {}", familierelasjon.getRelasjonsrolle());
            }
        }
    }

    private List<Personinfo> hentBarnRelatertTil(Personinfo personinfo, Behandling behandling) {
        List<Personinfo> relaterteBarn = hentAlleRelaterteBarn(personinfo);
        var relasjonsFilter = barnRelasjonsFilter.getOrDefault(behandling.getFagsakYtelseType(), new IngenRelasjonFilter());

        return relaterteBarn.stream()
            .filter(it -> relasjonsFilter.relasjonsFiltrering(behandling, it))
            .collect(Collectors.toList());
    }

    private List<Personinfo> hentAlleRelaterteBarn(Personinfo søkerPersonInfo) {
        return søkerPersonInfo.getFamilierelasjoner()
            .stream()
            .filter(r -> r.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(r -> personinfoAdapter.innhentSaksopplysningerForBarn(r.getPersonIdent()).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private RelasjonsRolleType utledRelasjonsrolleTilBarn(NavBrukerKjønn kjønn, RelasjonsRolleType rolle) {
        if (kjønn.equals(NavBrukerKjønn.KVINNE) && rolle.equals(RelasjonsRolleType.FARA)) {
            return RelasjonsRolleType.MEDMOR;
        }

        return NavBrukerKjønn.KVINNE.equals(kjønn) ? RelasjonsRolleType.MORA : RelasjonsRolleType.FARA;
    }

    private List<MedlemskapPerioderEntitet> innhentMedlemskapsopplysninger(AktørId søkerInfo, Behandling behandling) {
        return innhentMedlemskapsopplysningerFor(søkerInfo, behandling);
    }

    private List<MedlemskapPerioderEntitet> innhentMedlemskapsopplysningerFor(AktørId søkerInfo, Behandling behandling) {
        var opplysningsperiode = skjæringstidspunktTjeneste.utledOpplysningsperiode(behandling.getId(), behandling.getFagsakYtelseType(), false);
        LocalDate fom = opplysningsperiode.getFom();
        LocalDate tom = opplysningsperiode.getTom();
        List<Medlemskapsperiode> medlemskapsperioder = medlemTjeneste.finnMedlemskapPerioder(søkerInfo, fom, tom);
        List<MedlemskapPerioderEntitet> resultat = new ArrayList<>();
        for (var medlemskapsperiode : medlemskapsperioder) {
            resultat.add(lagMedlemskapPeriode(medlemskapsperiode));
        }
        return resultat;
    }

    private MedlemskapPerioderEntitet lagMedlemskapPeriode(Medlemskapsperiode medlemskapsperiode) {
        return new MedlemskapPerioderBuilder()
            .medPeriode(medlemskapsperiode.getFom(), medlemskapsperiode.getTom())
            .medBeslutningsdato(medlemskapsperiode.getDatoBesluttet())
            .medErMedlem(medlemskapsperiode.isErMedlem())
            .medLovvalgLand(medlemskapsperiode.getLovvalgsland())
            .medStudieLand(medlemskapsperiode.getStudieland())
            .medDekningType(medlemskapsperiode.getTrygdedekning())
            .medKildeType(medlemskapsperiode.getKilde())
            .medMedlemskapType(medlemskapsperiode.getLovvalg())
            .medMedlId(medlemskapsperiode.getMedlId())
            .build();
    }

    public void oppdaterSistOppdatertTidspunkt(Behandling behandling) {
        behandlingRepository.oppdaterSistOppdatertTidspunkt(behandling, LocalDateTime.now());
    }

    public void innhentIAYIAbakus(Behandling behandling) {
        doInnhentIAYIAbakus(behandling, behandling.getType(), behandling.getFagsakYtelseType());
    }

    public void innhentFullIAYIAbakus(Behandling behandling) {
        doInnhentIAYIAbakus(behandling, BehandlingType.FØRSTEGANGSSØKNAD, behandling.getFagsakYtelseType());
    }

    private void doInnhentIAYIAbakus(Behandling behandling, BehandlingType behandlingType, FagsakYtelseType fagsakYtelseType) {
        var behandlingUuid = behandling.getUuid();
        var saksnummer = behandling.getFagsak().getSaksnummer().getVerdi();
        var opplysningsperiode = skjæringstidspunktTjeneste.utledOpplysningsperiode(behandling.getId(), fagsakYtelseType, false);
        var ytelseType = YtelseType.fraKode(fagsakYtelseType.getKode());

        log.info("Trigger innhenting i abakus for behandling med id={} og uuid={}, saksnummer={}, opplysningsperiode={}, ytelseType={}",
            behandling.getId(), behandlingUuid, saksnummer, opplysningsperiode, ytelseType);

        var informasjonsElementer = utledBasertPå(behandlingType, fagsakYtelseType);
        var periode = new Periode(opplysningsperiode.getFom(), opplysningsperiode.getTom());
        var aktør = new AktørIdPersonident(behandling.getAktørId().getId());

        var innhentRegisterdataRequest = new InnhentRegisterdataRequest(saksnummer, behandlingUuid, ytelseType, periode, aktør, informasjonsElementer);
        innhentRegisterdataRequest.setCallbackUrl(abakusTjeneste.getCallbackUrl());

        abakusTjeneste.innhentRegisterdata(innhentRegisterdataRequest);
    }

    private Set<RegisterdataType> utledBasertPå(BehandlingType behandlingType, FagsakYtelseType ytelseType) {
        return InformasjonselementerUtleder.finnTjeneste(informasjonselementer, ytelseType, behandlingType).utled(behandlingType);
    }
}
