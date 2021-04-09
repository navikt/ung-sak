package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.Person;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
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

    private static final Set<LocalDate> UGYLDIGE_DATOER_FOR_PERIODE = Set.of(
        Tid.TIDENES_BEGYNNELSE,
        Tid.TIDENES_ENDE
    );
    public static final String TASKTYPE = "iverksetteVedtak.sendUtvidetRett";

    private SøknadRepository søknadRepository;
    private VilkårTjeneste vilkårTjeneste;
    private BehandlingRepository behandlingRepository;
    private UtvidetRettKlient utvidetRettKlient;

    protected UtvidetRettIverksettTask() {
    }

    @Inject
    public UtvidetRettIverksettTask(SøknadRepository søknadRepository,
                                    VilkårTjeneste vilkårTjeneste,
                                    BehandlingRepository behandlingRepository,
                                    UtvidetRettKlient utvidetRettKlient) {
        this.søknadRepository = søknadRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.utvidetRettKlient = utvidetRettKlient;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {

        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var søknad = søknadRepository.hentSøknad(behandling);
        var samletVilkårsresultat = vilkårTjeneste.samletVilkårsresultat(behandlingId);

        var utfall = behandling.getBehandlingResultatType();

        if (utfall == BehandlingResultatType.INNVILGET || utfall == BehandlingResultatType.INNVILGET_ENDRING) {
            var iverksett = mapIverksett(behandling, søknad, periodeForVedtak(samletVilkårsresultat, true));
            utvidetRettKlient.innvilget(iverksett);
        } else if (utfall == BehandlingResultatType.AVSLÅTT) {
            var iverksett = mapIverksett(behandling, søknad, periodeForVedtak(samletVilkårsresultat, false));
            utvidetRettKlient.avslått(iverksett);
        } else {
            throw new IllegalStateException("Forventet ikke behandling med behandlingResultatType=[" + utfall + "]");
        }
    }

    private UtvidetRett mapIverksett(Behandling behandling, SøknadEntitet søknad, Periode periode) {
        var ytelseType = behandling.getFagsakYtelseType();

        switch (ytelseType) {
            case OMSORGSPENGER_KS:
                return mapKroniskSyktBarn(behandling, søknad, periode);
            case OMSORGSPENGER_MA:
                return mapMidlertidigAlene(behandling, søknad, periode);
            default:
                throw new UnsupportedOperationException("Støtter ikke ytelseType=[" + ytelseType + "]");
        }
    }

    private UtvidetRett mapMidlertidigAlene(Behandling behandling, SøknadEntitet søknad, Periode periode) {
        var aktørIdSøker = behandling.getAktørId();
        var aktørIdAnnenForelder = behandling.getFagsak().getRelatertPersonAktørId();
        var saksnummer = behandling.getFagsak().getSaksnummer();

        return new MidlertidigAlene()
            .setSaksnummer(saksnummer)
            .setBehandlingUuid(behandling.getUuid())
            .setTidspunkt(ZonedDateTime.now(ZoneOffset.UTC))
            .setAnnenForelder(new Person(aktørIdAnnenForelder))
            .setSøker(new Person(aktørIdSøker))
            .setPeriode(periode);
    }

    private KroniskSyktBarn mapKroniskSyktBarn(Behandling behandling, SøknadEntitet søknad, Periode periode) {
        var aktørIdSøker = behandling.getAktørId();
        var aktørIdBarn = behandling.getFagsak().getPleietrengendeAktørId();
        var saksnummer = behandling.getFagsak().getSaksnummer();

        return new KroniskSyktBarn()
            .setSaksnummer(saksnummer)
            .setBehandlingUuid(behandling.getUuid())
            .setTidspunkt(ZonedDateTime.now(ZoneOffset.UTC))
            .setBarn(new Person(aktørIdBarn))
            .setSøker(new Person(aktørIdSøker))
            .setPeriode(periode);
    }

    static Periode periodeForVedtak(LocalDateTimeline<VilkårUtfallSamlet> samletVilkårsresultat, Boolean innvilget) {
        var oppfyltePerioder = perioderMedUtfall(samletVilkårsresultat, Utfall.OPPFYLT);
        var ikkeOppfyltePerioder = perioderMedUtfall(samletVilkårsresultat, Utfall.IKKE_OPPFYLT);

        if (innvilget && oppfyltePerioder.size() == 1) {
            return oppfyltePerioder.get(0);
        } else if (!innvilget && ikkeOppfyltePerioder.size() == 1 && oppfyltePerioder.isEmpty()) {
            return ikkeOppfyltePerioder.get(0);
        } else {
            throw new IllegalStateException(String.format("Uventet samlet vilkårsresultat. Innvilget=[%s], OppfyltePerioder=%s, IkkeOppfyltePerioder=%s", innvilget, oppfyltePerioder, ikkeOppfyltePerioder));
        }
    }

    private static List<Periode> perioderMedUtfall(LocalDateTimeline<VilkårUtfallSamlet> samletVilkårsresultat, Utfall utfall) {
        return samletVilkårsresultat
            .stream()
            .filter(sv -> sv.getValue().getSamletUtfall() == utfall)
            .map(seg -> gyldigPeriode(seg.getFom(), seg.getTom()))
            .collect(Collectors.toList());
    }

    private static Periode gyldigPeriode(LocalDate fom, LocalDate tom) {
        var periode = new Periode(fom, tom);
        if (UGYLDIGE_DATOER_FOR_PERIODE.contains(fom) || UGYLDIGE_DATOER_FOR_PERIODE.contains(tom)) {
            throw new IllegalStateException("Ugyldig periode for vedtak om utvidet rett " + periode.toString());
        }
        return periode;
    }
}
