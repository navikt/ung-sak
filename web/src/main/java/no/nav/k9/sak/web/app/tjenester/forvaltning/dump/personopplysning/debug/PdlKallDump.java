package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.personopplysning.debug;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.aktør.Adresseinfo;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.k9.sak.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.k9.sak.domene.registerinnhenting.YtelsesspesifikkRelasjonsFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DumpOutput;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.ContainerContextRunner;
import no.nav.k9.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.OMSORGSPENGER)
public class PdlKallDump implements DebugDumpBehandling {

    static final ObjectMapper OM = new ObjectMapper();

    static {
        OM.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        OM.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        OM.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OM.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
        OM.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OM.registerModule(new JavaTimeModule());
    }

    static String toJson(Object object) {
        try {
            return OM.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private DebugPersoninfoAdapter personinfoAdapter;

    private Instance<YtelsesspesifikkRelasjonsFilter> relasjonsFiltre;

    public static final String path = "pdlkall";

    public PdlKallDump() {
    }

    @Inject
    public PdlKallDump(SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                       DebugPersoninfoAdapter personinfoAdapter,
                       @Any Instance<YtelsesspesifikkRelasjonsFilter> relasjonsFiltre) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.relasjonsFiltre = relasjonsFiltre;
    }

    @Override
    public List<DumpOutput> dump(Behandling behandling) {
        return ContainerContextRunner.doRun(behandling, () -> doDump(behandling));
    }

    @Nullable
    private List<DumpOutput> doDump(Behandling behandling) {
        List<String> dumpinnhold = new ArrayList<>();
        AktørId søkerAktørId = behandling.getAktørId();
        Personinfo søkerInfo = personinfoAdapter.hentPersoninfo(dumpinnhold, søkerAktørId);

        byggPersonopplysningMedRelasjoner(dumpinnhold, søkerInfo, behandling);
        return List.of(new DumpOutput(path, String.join("\n", dumpinnhold)));
    }

    private PersonInformasjonBuilder byggPersonopplysningMedRelasjoner(List<String> dumpinnhold, Personinfo søkerPersonInfo, Behandling behandling) {
        var informasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);

        var fagsakYtelseType = behandling.getFagsakYtelseType();
        var opplysningsperioden = skjæringstidspunktTjeneste.utledOpplysningsperiode(behandling.getId(), fagsakYtelseType, true);
        dumpinnhold.add(opplysningsperioden.toString());

        Personhistorikkinfo personhistorikkinfo = personinfoAdapter.innhentPersonopplysningerHistorikk(dumpinnhold, søkerPersonInfo.getAktørId(), opplysningsperioden);
        mapInfoMedHistorikkTilEntitet(dumpinnhold, søkerPersonInfo, personhistorikkinfo, informasjonBuilder, behandling);

        leggTilSøkersBarn(søkerPersonInfo, behandling, informasjonBuilder, opplysningsperioden, dumpinnhold);
        leggTilFosterbarn(behandling, informasjonBuilder, opplysningsperioden, dumpinnhold);

        return informasjonBuilder;
    }

    private void mapInfoMedHistorikkTilEntitet(List<String> dumpinnhold, Personinfo personinfo, Personhistorikkinfo personhistorikkinfo, PersonInformasjonBuilder informasjonBuilder, Behandling behandling) {
        if (harAktør(informasjonBuilder, personinfo)) {
            dumpinnhold.add("ignorerte info i mapping fordi det fantes fra før for " + personinfo.getAktørId().getAktørId());
            return;
        }
        mapAdresser(dumpinnhold, personhistorikkinfo.getAdressehistorikk(), informasjonBuilder, personinfo);
    }


    private void leggTilSøkersBarn(Personinfo søkerPersonInfo, Behandling behandling, PersonInformasjonBuilder informasjonBuilder, Periode opplysningsperioden, List<String> dumpinnhold) {
        List<Personinfo> barna = hentBarnRelatertTil(søkerPersonInfo, behandling, opplysningsperioden, dumpinnhold);
        barna.forEach(barn -> {
            if (hentHistorikkForRelatertePersoner(behandling)) {
                Personhistorikkinfo personhistorikkinfo = personinfoAdapter.innhentPersonopplysningerHistorikk(dumpinnhold, barn.getAktørId(), opplysningsperioden);
                mapInfoMedHistorikkTilEntitet(dumpinnhold, barn, personhistorikkinfo, informasjonBuilder, behandling);
            } else {
                mapInfoTilEntitet(dumpinnhold, barn, informasjonBuilder, behandling);
            }
        });
    }

    private List<Personinfo> hentBarnRelatertTil(Personinfo personinfo, Behandling behandling, Periode opplysningsperioden, List<String> dumpinnhold) {
        List<Personinfo> relaterteBarn = hentAlleRelaterteBarn(personinfo, dumpinnhold);
        var filter = YtelsesspesifikkRelasjonsFilter.finnTjeneste(relasjonsFiltre, behandling.getFagsakYtelseType());

        return filter.relasjonsFiltreringBarn(behandling, relaterteBarn, opplysningsperioden);
    }

    private List<Personinfo> hentAlleRelaterteBarn(Personinfo søkerPersonInfo, List<String> dumpinnhold) {
        return søkerPersonInfo.getFamilierelasjoner()
            .stream()
            .filter(r -> r.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(r -> personinfoAdapter.innhentSaksopplysningerForBarn(r.getPersonIdent(), dumpinnhold).orElse(null))
            .filter(Objects::nonNull)
            .toList();
    }

    private void leggTilFosterbarn(Behandling behandling, PersonInformasjonBuilder informasjonBuilder, Periode opplysningsperioden, List<String> dumpinnhold) {
        List<Personinfo> barna = hentFosterbarn(behandling, dumpinnhold);
        barna.forEach(barn -> {
            if (hentHistorikkForRelatertePersoner(behandling)) {
                Personhistorikkinfo personhistorikkinfo = personinfoAdapter.innhentPersonopplysningerHistorikk(dumpinnhold, barn.getAktørId(), opplysningsperioden);
                mapInfoMedHistorikkTilEntitet(dumpinnhold, barn, personhistorikkinfo, informasjonBuilder, behandling);
            } else {
                mapInfoTilEntitet(dumpinnhold, barn, informasjonBuilder, behandling);
            }
        });
    }

    private List<Personinfo> hentFosterbarn(Behandling behandling, List<String> dumpinnhold) {
        var filter = YtelsesspesifikkRelasjonsFilter.finnTjeneste(relasjonsFiltre, behandling.getFagsakYtelseType());
        return filter.hentFosterbarn(behandling).stream()
            .map(aktørId -> personinfoAdapter.hentPersoninfo(dumpinnhold, aktørId))
            .toList();
    }


    private void mapInfoTilEntitet(List<String> dumpinnhold, Personinfo personinfo, PersonInformasjonBuilder informasjonBuilder, Behandling behandling) {
        if (harAktør(informasjonBuilder, personinfo)) {
            dumpinnhold.add("ignorerte aktør fordi fantes fra før : " + personinfo.getAktørId());
            return;
        }

        //map opplysninger som kan være periodisert, men som ikke er hentet inn periodisert for denne saken
        //setter da periode som angitt under
        DatoIntervallEntitet periode = getPeriode(personinfo.getFødselsdato(), Tid.TIDENES_ENDE);
        if (informasjonBuilder.harIkkeFåttStatsborgerskapHistorikk(personinfo.getAktørId())) {
            PersonInformasjonBuilder.StatsborgerskapBuilder statsborgerskapBuilder = informasjonBuilder.getStatsborgerskapBuilder(personinfo.getAktørId(), periode, personinfo.getLandkode(), personinfo.getRegion());
            informasjonBuilder.leggTil(statsborgerskapBuilder);
        }

        if (informasjonBuilder.harIkkeFåttAdresseHistorikk(personinfo.getAktørId())) {
            for (Adresseinfo adresse : personinfo.getAdresseInfoList()) {
                PersonInformasjonBuilder.AdresseBuilder adresseBuilder = informasjonBuilder.getAdresseBuilder(personinfo.getAktørId(), periode, adresse.getGjeldendePostadresseType());
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

        if (informasjonBuilder.harIkkeFåttPersonstatusHistorikk(personinfo.getAktørId())) {
            PersonInformasjonBuilder.PersonstatusBuilder personstatusBuilder = informasjonBuilder.getPersonstatusBuilder(personinfo.getAktørId(), periode)
                .medPersonstatus(personinfo.getPersonstatus());
            informasjonBuilder.leggTil(personstatusBuilder);
        }
    }

    private void mapAdresser(List<String> dumpinnhold, List<AdressePeriode> adressehistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
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
        dumpinnhold.add("innhold i informasjonsbuilder: " + PdlKallDump.toJson(informasjonBuilder));
    }

    private LocalDate brukFødselsdatoHvisEtter(LocalDate dato, LocalDate fødseldato) {
        if (dato.isBefore(fødseldato)) {
            return fødseldato;
        }
        return dato;
    }

    private boolean harAktør(PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        return informasjonBuilder.harAktørId(personinfo.getAktørId());
    }

    private boolean hentHistorikkForRelatertePersoner(Behandling behandling) {
        return YtelsesspesifikkRelasjonsFilter.finnTjeneste(relasjonsFiltre, behandling.getFagsakYtelseType()).hentHistorikkForRelatertePersoner();
    }

    private DatoIntervallEntitet getPeriode(LocalDate fom, LocalDate tom) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom != null ? tom : Tid.TIDENES_ENDE);
    }

}
