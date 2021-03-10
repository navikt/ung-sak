package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.UtvidetRettKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.KroniskSyktBarn;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.MidlertidigAlene;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.UtvidetRett;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(UtvidetRettIverksettTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class UtvidetRettIverksettTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(UtvidetRettIverksettTask.class);
    public static final String TASKTYPE = "iverksetteVedtak.sendUtvidetRett";

    private SøknadRepository søknadRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;
    private UtvidetRettKlient utvidetRettKlient;

    protected UtvidetRettIverksettTask() {
    }

    @Inject
    public UtvidetRettIverksettTask(SøknadRepository søknadRepository,
                                    VilkårResultatRepository vilkårResultatRepository,
                                    BehandlingRepository behandlingRepository,
                                    UtvidetRettKlient utvidetRettKlient) {
        this.søknadRepository = søknadRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.utvidetRettKlient = utvidetRettKlient;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {

        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var søknad = søknadRepository.hentSøknad(behandling);
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        var iverksett = mapIverksett(behandling, søknad, vilkårene);
        var utfall = behandling.getBehandlingResultatType();

        if (utfall == BehandlingResultatType.INNVILGET || utfall == BehandlingResultatType.INNVILGET_ENDRING) {
            utvidetRettKlient.innvilget(iverksett);
        } else if (utfall == BehandlingResultatType.AVSLÅTT) {
            utvidetRettKlient.avslått(iverksett);
        } else {
            throw new IllegalStateException("Forventet ikke behandling med behandlingResultatType=" + utfall);
        }
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
        var aktørIdSøker = behandling.getAktørId();
        var aktørIdAnnenForelder = behandling.getFagsak().getRelatertPersonAktørId();
        var saksnummer = behandling.getFagsak().getSaksnummer();

        return new MidlertidigAlene()
            .setSaksnummer(saksnummer)
            .setBehandlingUuid(behandling.getUuid())
            .setTidspunkt(ZonedDateTime.now(ZoneOffset.UTC))
            .setAnnenForelder(new Person(aktørIdAnnenForelder))
            .setSøker(new Person(aktørIdSøker))
            .setPeriode(periode(vilkårene));
    }

    private KroniskSyktBarn mapKroniskSyktBarn(Behandling behandling, SøknadEntitet søknad, Vilkårene vilkårene) {
        var aktørIdSøker = behandling.getAktørId();
        var aktørIdBarn = behandling.getFagsak().getPleietrengendeAktørId();
        var saksnummer = behandling.getFagsak().getSaksnummer();

        return new KroniskSyktBarn()
            .setSaksnummer(saksnummer)
            .setBehandlingUuid(behandling.getUuid())
            .setTidspunkt(ZonedDateTime.now(ZoneOffset.UTC))
            .setBarn(new Person(aktørIdBarn))
            .setSøker(new Person(aktørIdSøker))
            .setPeriode(periode(vilkårene));
    }

    private Periode periode(Vilkårene vilkårene) {
        var vilkår = vilkårene.getVilkår(VilkårType.UTVIDETRETT);
        var perioder = vilkår.orElseThrow().getPerioder();
        var fom = perioder.stream().min(Comparator.comparing(VilkårPeriode::getFom)).map(VilkårPeriode::getFom).orElseThrow();
        var tom = perioder.stream().max(Comparator.comparing(VilkårPeriode::getTom)).map(VilkårPeriode::getTom).orElseThrow();
        return new Periode(fom, tom);
    }
}
