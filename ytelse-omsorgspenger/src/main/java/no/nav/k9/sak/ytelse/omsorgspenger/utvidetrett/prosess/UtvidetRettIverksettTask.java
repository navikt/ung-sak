package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.UtvidetRettRestKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.AnnenForelder;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.Barn;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.KroniskSyktBarn;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.MidlertidigAlene;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.Søker;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.UtvidetRett;
import no.nav.k9.søknad.felles.type.NorskIdentitetsnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(UtvidetRettIverksettTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class UtvidetRettIverksettTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.sendUtvidetRett";

    private SøknadRepository søknadRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;

    private UtvidetRettRestKlient restKlient;

    private PersoninfoAdapter personinfoAdapter;

    private PersonopplysningTjeneste personopplysningTjeneste;

    protected UtvidetRettIverksettTask() {
    }

    @Inject
    public UtvidetRettIverksettTask(SøknadRepository søknadRepository,
                                    VilkårResultatRepository vilkårResultatRepository,
                                    BehandlingRepository behandlingRepository,
                                    PersoninfoAdapter personinfoAdapter,
                                    PersonopplysningTjeneste personopplysningTjeneste,
                                    UtvidetRettRestKlient restKlient) {
        this.søknadRepository = søknadRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.restKlient = restKlient;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {

        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var søknad = søknadRepository.hentSøknad(behandling);
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        var iverksett = mapIverksett(behandling, søknad, vilkårene);

        restKlient.innvilget(behandling.getFagsakYtelseType(), behandling.getUuid(), iverksett);
    }

    private UtvidetRett mapIverksett(Behandling behandling, SøknadEntitet søknad, Vilkårene vilkårene) {
        var ytelseType = behandling.getFagsakYtelseType();

        switch (ytelseType) {
            case OMSORGSPENGER_KS:
                return mapKroniskSyktBarn(behandling, søknad, vilkårene);
            case OMSORGSPENGER_MA:
                return mapMidlertidigAlene(behandling, søknad, vilkårene);
            default:
                throw new UnsupportedOperationException("Støtter ikke ytelsetype=" + ytelseType);
        }
    }

    private UtvidetRett mapMidlertidigAlene(Behandling behandling, SøknadEntitet søknad, Vilkårene vilkårene) {
        var saksnummer = behandling.getFagsak().getSaksnummer();
        var søkerIdent = personinfoAdapter.hentIdentForAktørId(behandling.getAktørId());

        var personopplysninger = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(behandling.getId(), behandling.getAktørId(), søknad.getMottattDato());

        // TODO : annenforelder
        /*
         * var relasjon = personopplysninger.getSøkersRelasjoner().stream().filter(r -> r.getAktørId().equals(annenForelderAktørId)).findFirst()
         * .orElseThrow(() -> new IllegalStateException("Har ikke relasjon fra søker til annenForelder: " + annenForelderAktørId));
         */
        return new MidlertidigAlene()
            .setSaksnummer(saksnummer)
            .setBehandlingUuid(behandling.getUuid())
            .setSøknadMottatt(søknad.getMottattDato().atStartOfDay(ZoneId.systemDefault()))
            .setTidspunkt(ZonedDateTime.now())
// .setAnnenForelder(new AnnenForelder(NorskIdentitetsnummer.of(søkerIdent.get().getIdent())))
            .setSøker(new Søker(NorskIdentitetsnummer.of(søkerIdent.get().getIdent())));
    }

    private KroniskSyktBarn mapKroniskSyktBarn(Behandling behandling, SøknadEntitet søknad, Vilkårene vilkårene) {
        AktørId pleietrengendeAktørId = behandling.getFagsak().getPleietrengendeAktørId();
        var saksnummer = behandling.getFagsak().getSaksnummer();
        var søkerIdent = personinfoAdapter.hentIdentForAktørId(behandling.getAktørId());

        var barnIdent = personinfoAdapter.hentIdentForAktørId(pleietrengendeAktørId);
        var barnInfo = personinfoAdapter.hentKjerneinformasjon(pleietrengendeAktørId);

        var personopplysninger = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(behandling.getId(), behandling.getAktørId(), søknad.getMottattDato());

        var relasjon = personopplysninger.getSøkersRelasjoner().stream().filter(r -> r.getAktørId().equals(pleietrengendeAktørId)).findFirst()
            .orElseThrow(() -> new IllegalStateException("Har ikke relasjon fra søker til barn: " + pleietrengendeAktørId));

        return new KroniskSyktBarn()
            .setSaksnummer(saksnummer)
            .setBehandlingUuid(behandling.getUuid())
            .setSøknadMottatt(søknad.getMottattDato().atStartOfDay(ZoneId.systemDefault()))
            .setTidspunkt(ZonedDateTime.now())
            .setBarn(new Barn(NorskIdentitetsnummer.of(barnIdent.get().getIdent()), barnInfo.getFødselsdato(), relasjon.getHarSammeBosted()))
            .setSøker(new Søker(NorskIdentitetsnummer.of(søkerIdent.get().getIdent())));

    }

}
