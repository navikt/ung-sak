package no.nav.ung.sak.web.app.tjenester.forvaltning.dump.personopplysning.debug;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.domene.registerinnhenting.OpplysningsperiodeTjeneste;
import no.nav.ung.sak.domene.registerinnhenting.YtelsesspesifikkRelasjonsFilter;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.ContainerContextRunner;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DebugDumpBehandling;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.DumpMottaker;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.OMSORGSPENGER)
@FagsakYtelseTypeRef(FagsakYtelseType.OMSORGSPENGER_AO)
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

    private DebugPersoninfoAdapter personinfoAdapter;

    private Instance<YtelsesspesifikkRelasjonsFilter> relasjonsFiltre;
    private Instance<OpplysningsperiodeTjeneste> opplysningsperiodeTjeneste;

    public static final String path = "pdlkall";

    public PdlKallDump() {
    }

    @Inject
    public PdlKallDump(DebugPersoninfoAdapter personinfoAdapter,
                       @Any Instance<YtelsesspesifikkRelasjonsFilter> relasjonsFiltre,
                       @Any Instance<OpplysningsperiodeTjeneste> opplysningsperiodeTjeneste) {
        this.personinfoAdapter = personinfoAdapter;
        this.relasjonsFiltre = relasjonsFiltre;
        this.opplysningsperiodeTjeneste = opplysningsperiodeTjeneste;
    }

    @Override
    public void dump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        ContainerContextRunner.doRun(behandling, () -> doDump(dumpMottaker, behandling, basePath));
    }

    private int doDump(DumpMottaker dumpMottaker, Behandling behandling, String basePath) {
        List<String> dumpinnhold = new ArrayList<>();
        AktørId søkerAktørId = behandling.getAktørId();
        Personinfo søkerInfo = personinfoAdapter.hentPersoninfo(dumpinnhold, søkerAktørId);

        byggPersonopplysningMedRelasjoner(dumpinnhold, søkerInfo, behandling);
        dumpMottaker.newFile(basePath + "/" + path);
        dumpMottaker.write(String.join("\n", dumpinnhold));
        return 1;
    }

    private PersonInformasjonBuilder byggPersonopplysningMedRelasjoner(List<String> dumpinnhold, Personinfo søkerPersonInfo, Behandling behandling) {
        var informasjonBuilder = new PersonInformasjonBuilder(PersonopplysningVersjonType.REGISTRERT);

        var fagsakYtelseType = behandling.getFagsakYtelseType();
        var opplysningsperioden = OpplysningsperiodeTjeneste.getTjeneste(opplysningsperiodeTjeneste, behandling.getFagsakYtelseType())
            .utledOpplysningsperiode(behandling.getId(), true);
        dumpinnhold.add(opplysningsperioden.toString());

        mapInfoMedHistorikkTilEntitet(dumpinnhold, søkerPersonInfo, informasjonBuilder, behandling);

        leggTilSøkersBarn(søkerPersonInfo, behandling, informasjonBuilder, opplysningsperioden, dumpinnhold);
        leggTilFosterbarn(behandling, informasjonBuilder, opplysningsperioden, dumpinnhold);

        return informasjonBuilder;
    }

    private void mapInfoMedHistorikkTilEntitet(List<String> dumpinnhold, Personinfo personinfo, PersonInformasjonBuilder informasjonBuilder, Behandling behandling) {
        if (harAktør(informasjonBuilder, personinfo)) {
            dumpinnhold.add("ignorerte info i mapping fordi det fantes fra før for " + personinfo.getAktørId().getAktørId());
        }
    }


    private void leggTilSøkersBarn(Personinfo søkerPersonInfo, Behandling behandling, PersonInformasjonBuilder informasjonBuilder, Periode opplysningsperioden, List<String> dumpinnhold) {
        List<Personinfo> barna = hentBarnRelatertTil(søkerPersonInfo, behandling, opplysningsperioden, dumpinnhold);
        barna.forEach(barn -> {
            if (hentHistorikkForRelatertePersoner(behandling)) {
                mapInfoMedHistorikkTilEntitet(dumpinnhold, barn, informasjonBuilder, behandling);
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
                mapInfoMedHistorikkTilEntitet(dumpinnhold, barn, informasjonBuilder, behandling);
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
        }
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
