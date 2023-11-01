package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.weld.exceptions.IllegalStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.vilkår.VilkårTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.UtvidetRettKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.AleneOmsorg;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.KroniskSyktBarn;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.MidlertidigAlene;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.Person;
import no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell.UtvidetRett;

@ApplicationScoped
@ProsessTask(UtvidetRettIverksettTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class UtvidetRettIverksettTask extends BehandlingProsessTask {
    private static final Logger log = LoggerFactory.getLogger(UtvidetRettIverksettTask.class);
    private static final Set<LocalDate> UGYLDIGE_DATOER_FOR_PERIODE = Set.of(
        Tid.TIDENES_BEGYNNELSE,
        Tid.TIDENES_ENDE);
    public static final String TASKTYPE = "iverksetteVedtak.sendUtvidetRett";

    private VilkårTjeneste vilkårTjeneste;
    private BehandlingRepository behandlingRepository;
    private UtvidetRettKlient utvidetRettKlient;
    private PeriodisertUtvidetRettIverksettTjeneste periodisertUtvidetRettIverksettTjeneste;
    private boolean brukPeriodisertRammevedtak;


    protected UtvidetRettIverksettTask() {
    }

    @Inject
    public UtvidetRettIverksettTask(VilkårTjeneste vilkårTjeneste,
                                    BehandlingRepository behandlingRepository,
                                    UtvidetRettKlient utvidetRettKlient,
                                    PeriodisertUtvidetRettIverksettTjeneste periodisertUtvidetRettIverksettTjeneste,
                                    @KonfigVerdi(value = "PERIODISERT_RAMMEVEDTAK", defaultVerdi = "false") boolean brukPeriodisertRammevedtak) {
        this.vilkårTjeneste = vilkårTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.utvidetRettKlient = utvidetRettKlient;
        this.periodisertUtvidetRettIverksettTjeneste = periodisertUtvidetRettIverksettTjeneste;
        this.brukPeriodisertRammevedtak = brukPeriodisertRammevedtak;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        if (brukPeriodisertRammevedtak) {
            håndterAktuellOgTilpassTidligerePerioder(prosessTaskData);
        } else {
            håndterAktuellPeriode(prosessTaskData);
        }
    }

    private void håndterAktuellOgTilpassTidligerePerioder(ProsessTaskData prosessTaskData) {
        var behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingId());
        LocalDateTimeline<Utfall> resultat = periodisertUtvidetRettIverksettTjeneste.utfallSomErEndret(behandling);
        if (resultat.size() > 1) {
            //begrensningen kan fjernes dersom omsorgsdager får støtte for å ta imot flere perioder for samme behandling
            throw new IllegalStateException("Kan ikke sende mer enn en periode ved iverksetting av rammevedtak, siden omsorgsdager p.t. ikke støtter det. Har perioder: " + resultat);
        }
        resultat.forEach(segment -> {
            Periode vedtakperiode = new Periode(segment.getFom(), segment.getTom());
            var iverksett = mapIverksett(behandling, vedtakperiode);
            switch (segment.getValue()) {
                case OPPFYLT -> {
                    log.info("Iverksetter innvilget rammevedtak for periode: {}", vedtakperiode);
                    utvidetRettKlient.innvilget(iverksett);
                }
                case IKKE_OPPFYLT -> {
                    log.info("Iverksetter avslått rammevedtak for periode: {}", vedtakperiode);
                    utvidetRettKlient.avslått(iverksett);
                }
                default -> throw new IllegalArgumentException("Ikke-støttet verdi: " + segment.getValue() + " for " + segment.getLocalDateInterval());
            }
        });
    }

    private void håndterAktuellPeriode(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        logContext(behandling);

        var samletVilkårsresultat = vilkårTjeneste.samletVilkårsresultat(behandlingId);

        var utfall = behandling.getBehandlingResultatType();

        if (utfall == BehandlingResultatType.INNVILGET || utfall == BehandlingResultatType.INNVILGET_ENDRING) {
            Periode vedtakperiode = periodeForVedtak(samletVilkårsresultat, true);
            log.info("Innvilger rammevedtak for periode: {}", vedtakperiode);
            var iverksett = mapIverksett(behandling, vedtakperiode);
            utvidetRettKlient.innvilget(iverksett);
        } else if (utfall == BehandlingResultatType.AVSLÅTT) {
            Periode avslagperiode = periodeForVedtak(samletVilkårsresultat, false);
            log.info("Avslår rammevedtak for periode: {}", avslagperiode);
            var iverksett = mapIverksett(behandling, avslagperiode);
            utvidetRettKlient.avslått(iverksett);
        } else {
            throw new IllegalStateException("Forventet ikke behandling med behandlingResultatType=[" + utfall + "]");
        }
    }

    private UtvidetRett mapIverksett(Behandling behandling, Periode periode) {
        var ytelseType = behandling.getFagsakYtelseType();

        return switch (ytelseType) {
            case OMSORGSPENGER_KS -> mapKroniskSyktBarn(behandling, periode);
            case OMSORGSPENGER_MA -> mapMidlertidigAlene(behandling, periode);
            case OMSORGSPENGER_AO -> mapAleneOmsorg(behandling, periode);
            default -> throw new UnsupportedOperationException("Støtter ikke ytelseType=[" + ytelseType + "]");
        };
    }

    private UtvidetRett mapAleneOmsorg(Behandling behandling, Periode periode) {
        var aktørIdSøker = behandling.getAktørId();
        var aktørIdBarn = behandling.getFagsak().getPleietrengendeAktørId();
        var saksnummer = behandling.getFagsak().getSaksnummer();

        return new AleneOmsorg()
            .setSaksnummer(saksnummer)
            .setBehandlingUuid(behandling.getUuid())
            .setTidspunkt(ZonedDateTime.now(ZoneOffset.UTC))
            .setBarn(new Person(aktørIdBarn))
            .setSøker(new Person(aktørIdSøker))
            .setPeriode(periode);
    }

    private UtvidetRett mapMidlertidigAlene(Behandling behandling, Periode periode) {
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

    private KroniskSyktBarn mapKroniskSyktBarn(Behandling behandling, Periode periode) {
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

        List<Periode> oppfyltKomprimert = komprimer(oppfyltePerioder);
        List<Periode> ikkeOppfyltKomprimert = komprimer(ikkeOppfyltePerioder);
        if (innvilget && oppfyltKomprimert.size() == 1) {
            return oppfyltKomprimert.get(0);
        } else if (!innvilget && ikkeOppfyltKomprimert.size() == 1 && oppfyltePerioder.isEmpty()) {
            return ikkeOppfyltKomprimert.get(0);
        } else {
            throw new IllegalStateException(
                String.format("Uventet samlet vilkårsresultat. Innvilget=[%s], %sOppfyltePerioder=%s, %sIkkeOppfyltePerioder=%s; samletVilkårResultat=%s",
                    innvilget,
                    oppfyltKomprimert.size() != oppfyltePerioder.size() && !oppfyltKomprimert.isEmpty() ? "OppfyltePerioder/KOMPRIMERT=" + oppfyltKomprimert + ", " : "",
                    oppfyltePerioder,
                    ikkeOppfyltKomprimert.size() != ikkeOppfyltePerioder.size() && !ikkeOppfyltKomprimert.isEmpty() ? "IkkeOppfyltePerioder/KOMPRIMERT=" + ikkeOppfyltKomprimert + ", " : "",
                    ikkeOppfyltePerioder,
                    samletVilkårsresultat));
        }
    }

    private static List<Periode> komprimer(List<Periode> perioder) {

        var segmenter = perioder.stream().map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), Boolean.TRUE)).collect(Collectors.toList());
        var komprimertSegmenter = new LocalDateTimeline<Boolean>(segmenter).compress().toSegments();
        return komprimertSegmenter.stream().map(s -> new Periode(s.getFom(), s.getTom())).collect(Collectors.toList());

    }

    private static List<Periode> perioderMedUtfall(LocalDateTimeline<VilkårUtfallSamlet> samletVilkårsresultat, Utfall utfall) {
        List<Periode> perioder = samletVilkårsresultat
            .stream()
            .filter(sv -> sv.getValue().getSamletUtfall() == utfall)
            .map(seg -> gyldigPeriode(seg.getFom(), seg.getTom()))
            .collect(Collectors.toList());
        return perioder;
    }

    private static Periode gyldigPeriode(LocalDate fom, LocalDate tom) {
        if (fom == null || UGYLDIGE_DATOER_FOR_PERIODE.contains(fom)) {
            throw new IllegalStateException("Ugyldig periode for vedtak om utvidet rett. fom=" + fom + ", tom=" + tom);
        }
        return new Periode(fom, tom);
    }
}
