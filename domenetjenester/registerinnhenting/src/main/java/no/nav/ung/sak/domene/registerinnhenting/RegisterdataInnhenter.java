package no.nav.ung.sak.domene.registerinnhenting;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.InnhentRegisterdataRequest;
import no.nav.abakus.iaygrunnlag.request.RegisterdataType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.behandlingslager.aktør.Familierelasjon;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.domene.abakus.AbakusTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.domene.registerinnhenting.impl.SaksopplysningerFeil;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;

@ApplicationScoped
public class RegisterdataInnhenter {

    private static final Logger log = LoggerFactory.getLogger(RegisterdataInnhenter.class);

    private PersoninfoAdapter personinfoAdapter;
    private PersonopplysningRepository personopplysningRepository;
    private BehandlingRepository behandlingRepository;
    private AbakusTjeneste abakusTjeneste;
    private BehandlingLåsRepository behandlingLåsRepository;
    private Instance<InformasjonselementerUtleder> informasjonselementer;
    private Instance<YtelsesspesifikkRelasjonsFilter> relasjonsFiltre;
    private Instance<OpplysningsperiodeTjeneste> opplysningsperiodeTjeneste;

    RegisterdataInnhenter() {
        // for CDI proxy
    }

    @Inject
    public RegisterdataInnhenter(PersoninfoAdapter personinfoAdapter, // NOSONAR - krever mange parametere
                                 BehandlingRepositoryProvider repositoryProvider,
                                 AbakusTjeneste abakusTjeneste,
                                 @Any Instance<InformasjonselementerUtleder> utledInformasjonselementer,
                                 @Any Instance<YtelsesspesifikkRelasjonsFilter> relasjonsFiltre,
                                 @Any Instance<OpplysningsperiodeTjeneste> opplysningsperiodeTjeneste) {
        this.personinfoAdapter = personinfoAdapter;
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingLåsRepository = repositoryProvider.getBehandlingLåsRepository();
        this.abakusTjeneste = abakusTjeneste;
        this.informasjonselementer = utledInformasjonselementer;
        this.relasjonsFiltre = relasjonsFiltre;
        this.opplysningsperiodeTjeneste = opplysningsperiodeTjeneste;
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

    private PersonInformasjonBuilder byggPersonopplysningMedRelasjoner(Personinfo søkerPersonInfo, Behandling behandling) {

        var informasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);

        var fagsakYtelseType = behandling.getFagsakYtelseType();
        var opplysningsperioden = OpplysningsperiodeTjeneste.getTjeneste(opplysningsperiodeTjeneste, behandling.getFagsakYtelseType())
            .utledOpplysningsperiode(behandling.getId(), true);

        mapInfoMedHistorikkTilEntitet(søkerPersonInfo, informasjonBuilder, behandling);

        leggTilSøkersBarn(søkerPersonInfo, behandling, informasjonBuilder, opplysningsperioden);
        leggTilFosterbarn(søkerPersonInfo, behandling, informasjonBuilder, opplysningsperioden);

        if (fagsakYtelseType.harRelatertePersoner()) {
            leggTilEktefelle(informasjonBuilder, behandling, opplysningsperioden, søkerPersonInfo);
        }

        return informasjonBuilder;
    }

    private void leggTilSøkersBarn(Personinfo søkerPersonInfo, Behandling behandling, PersonInformasjonBuilder informasjonBuilder, no.nav.ung.sak.typer.Periode opplysningsperioden) {
        List<Personinfo> barna = hentBarnRelatertTil(søkerPersonInfo, behandling, opplysningsperioden);
        barna.forEach(barn -> {
            if (hentHistorikkForRelatertePersoner(behandling)) {
                mapInfoMedHistorikkTilEntitet(barn, informasjonBuilder, behandling);
            } else {
                mapInfoTilEntitet(barn, informasjonBuilder, behandling);
            }
            mapRelasjon(barn, søkerPersonInfo, utledRelasjonsrolleTilBarn(søkerPersonInfo, barn), informasjonBuilder);
            mapRelasjon(søkerPersonInfo, barn, Collections.singletonList(RelasjonsRolleType.BARN), informasjonBuilder);
        });
    }

    private List<RelasjonsRolleType> utledRelasjonsrolleTilBarn(Personinfo personinfo, Personinfo barn) {
        if (barn == null) {
            return Collections.emptyList();
        }
        return barn.getFamilierelasjoner().stream()
            .filter(fr -> fr.getPersonIdent().equals(personinfo.getPersonIdent()))
            .map(rel -> rel.getMinRelasjonsRolle())
            .toList();
    }

    private void leggTilFosterbarn(Personinfo søkerPersonInfo, Behandling behandling, PersonInformasjonBuilder informasjonBuilder, no.nav.ung.sak.typer.Periode opplysningsperioden) {
        List<Personinfo> barna = hentFosterbarn(behandling);
        barna.forEach(barn -> {
            if (hentHistorikkForRelatertePersoner(behandling)) {
                mapInfoMedHistorikkTilEntitet(barn, informasjonBuilder, behandling);
            } else {
                mapInfoTilEntitet(barn, informasjonBuilder, behandling);
            }
            mapRelasjon(barn, søkerPersonInfo, List.of(RelasjonsRolleType.FOSTERFORELDER), informasjonBuilder);
            mapRelasjon(søkerPersonInfo, barn, List.of(RelasjonsRolleType.FOSTERBARN), informasjonBuilder);
        });
    }


    private boolean harAktør(PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        return informasjonBuilder.harAktørId(personinfo.getAktørId());
    }

    private void mapRelasjon(Personinfo fra, Personinfo til, List<RelasjonsRolleType> roller, PersonInformasjonBuilder informasjonBuilder) {
        if (til == null) {
            return;
        }
        for (RelasjonsRolleType rolle : roller) {
            final PersonInformasjonBuilder.RelasjonBuilder builder = informasjonBuilder.getRelasjonBuilder(fra.getAktørId(), til.getAktørId(), rolle);
            informasjonBuilder.leggTil(builder);
        }
    }

    private void mapInfoMedHistorikkTilEntitet(Personinfo personinfo, PersonInformasjonBuilder informasjonBuilder, Behandling behandling) {
        if (harAktør(informasjonBuilder, personinfo)) {
            return;
        }

        mapPersonopplysning(informasjonBuilder, personinfo);
    }

    private void mapInfoTilEntitet(Personinfo personinfo, PersonInformasjonBuilder informasjonBuilder, Behandling behandling) {
        if (harAktør(informasjonBuilder, personinfo)) {
            return;
        }

        mapPersonopplysning(informasjonBuilder, personinfo);
    }

    private void mapPersonopplysning(PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        final PersonInformasjonBuilder.PersonopplysningBuilder builder = informasjonBuilder.getPersonopplysningBuilder(personinfo.getAktørId());
        builder.medFødselsdato(personinfo.getFødselsdato())
            .medNavn(personinfo.getNavn())
            .medDødsdato(personinfo.getDødsdato());
        informasjonBuilder.leggTil(builder);
    }

    private void leggTilEktefelle(PersonInformasjonBuilder informasjonBuilder, Behandling behandling, no.nav.ung.sak.typer.Periode opplysningsperioden, Personinfo søkerPersonInfo) {
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
                if (hentHistorikkForRelatertePersoner(behandling)) {
                    mapInfoMedHistorikkTilEntitet(personinfo, informasjonBuilder, behandling);
                } else {
                    mapInfoTilEntitet(personinfo, informasjonBuilder, behandling);
                }

                mapRelasjon(søkerPersonInfo, personinfo, Collections.singletonList(familierelasjon.getRelasjonsrolle()), informasjonBuilder);
                mapRelasjon(personinfo, søkerPersonInfo, Collections.singletonList(familierelasjon.getRelasjonsrolle()), informasjonBuilder);
            } else {
                log.warn("Fant ikke personinfo for familierelasjon: {}", familierelasjon.getRelasjonsrolle());
            }
        }
    }

    private List<Personinfo> hentBarnRelatertTil(Personinfo personinfo, Behandling behandling, no.nav.ung.sak.typer.Periode opplysningsperioden) {
        List<Personinfo> relaterteBarn = hentAlleRelaterteBarn(personinfo);
        var filter = YtelsesspesifikkRelasjonsFilter.finnTjeneste(relasjonsFiltre, behandling.getFagsakYtelseType());

        return filter.relasjonsFiltreringBarn(behandling, relaterteBarn, opplysningsperioden);
    }

    private List<Personinfo> hentAlleRelaterteBarn(Personinfo søkerPersonInfo) {
        return søkerPersonInfo.getFamilierelasjoner()
            .stream()
            .filter(r -> r.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(r -> personinfoAdapter.innhentSaksopplysningerForBarn(r.getPersonIdent()).orElse(null))
            .filter(Objects::nonNull)
            .toList();
    }

    private List<Personinfo> hentFosterbarn(Behandling behandling) {
        var filter = YtelsesspesifikkRelasjonsFilter.finnTjeneste(relasjonsFiltre, behandling.getFagsakYtelseType());
        return filter.hentFosterbarn(behandling).stream()
            .map(aktørId -> personinfoAdapter.hentPersoninfo(aktørId))
            .toList();
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

        var periodeTjeneste = OpplysningsperiodeTjeneste.getTjeneste(opplysningsperiodeTjeneste, fagsakYtelseType);

        var opplysningsperiode = periodeTjeneste.utledOpplysningsperiode(behandling.getId(), false);


        var ytelseType = YtelseType.fraKode(fagsakYtelseType.getKode());

        log.info("Trigger innhenting i abakus for behandling med id={} og uuid={}, saksnummer={}, opplysningsperiode={}, ytelseType={}",
            behandling.getId(), behandlingUuid, saksnummer, opplysningsperiode, ytelseType);

        var informasjonsElementer = utledBasertPå(behandlingType, fagsakYtelseType);
        var periode = new Periode(opplysningsperiode.getFom(), opplysningsperiode.getTom());
        var aktør = new AktørIdPersonident(behandling.getAktørId().getId());

        var innhentRegisterdataRequest = new InnhentRegisterdataRequest(saksnummer, behandlingUuid, ytelseType, periode, aktør, informasjonsElementer);
        innhentRegisterdataRequest.setCallbackUrl(abakusTjeneste.getCallbackUrl());
        innhentRegisterdataRequest.setCallbackScope(abakusTjeneste.getCallbackScope());
        abakusTjeneste.innhentRegisterdata(innhentRegisterdataRequest);
    }

    private Set<RegisterdataType> utledBasertPå(BehandlingType behandlingType, FagsakYtelseType ytelseType) {
        return InformasjonselementerUtleder.finnTjeneste(informasjonselementer, ytelseType, behandlingType).utled(behandlingType);
    }

    private boolean hentHistorikkForRelatertePersoner(Behandling behandling) {
        return YtelsesspesifikkRelasjonsFilter.finnTjeneste(relasjonsFiltre, behandling.getFagsakYtelseType()).hentHistorikkForRelatertePersoner();
    }
}
