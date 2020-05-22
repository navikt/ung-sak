package no.nav.k9.sak.ytelse.omsorgspenger.filter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregnytelse.BeregneYtelseSteg;
import no.nav.k9.sak.domene.iay.modell.AktørYtelse;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.inngangsvilkår.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.konfig.KonfigVerdi;

@FagsakYtelseTypeRef("OMP")
@BehandlingStegRef(kode = "VARIANT_FILTER")
@BehandlingTypeRef
@ApplicationScoped
public class FiltrerUtVariantSomIkkeStøttesSteg implements BeregneYtelseSteg {

    private Boolean filterAktivert;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private VilkårsPerioderTilVurderingTjeneste vilkårsPeriodeTjeneste;

    protected FiltrerUtVariantSomIkkeStøttesSteg() {
        // for proxy
    }

    @Inject
    public FiltrerUtVariantSomIkkeStøttesSteg(@KonfigVerdi(value = "OMP_VARIANT_FILTER_AKTIVERT", defaultVerdi = "true") Boolean filterAktivert,
                                              InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                              @FagsakYtelseTypeRef("OMP") VilkårsPerioderTilVurderingTjeneste vilkårsPeriodeTjeneste) {
        this.filterAktivert = filterAktivert;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.vilkårsPeriodeTjeneste = vilkårsPeriodeTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (!filterAktivert) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        Long behandlingId = kontekst.getBehandlingId();
        AktørId aktørId = kontekst.getAktørId();
        var aktørYtelse = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId)
            .getAktørYtelseFraRegister(aktørId);

        var vilkårPerioder = vilkårsPeriodeTjeneste.utled(behandlingId, VilkårType.OPPTJENINGSVILKÅRET);

        if (aktørYtelse.isPresent() && !vilkårPerioder.isEmpty()) {
            var opptjeningSegmenter = vilkårPerioder.stream().sorted().map(v -> new LocalDateSegment<>(v.getFomDato(), v.getTomDato(), Boolean.TRUE)).collect(Collectors.toList());
            var opptjeningVilkårTimeline = new LocalDateTimeline<>(opptjeningSegmenter);
            return filtrerBehandlinger(opptjeningVilkårTimeline, aktørYtelse.get());
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
    }

    BehandleStegResultat filtrerBehandlinger(LocalDateTimeline<Boolean> vilkårTimeline, AktørYtelse aktørYtelse) {
        Map<FagsakYtelseType, NavigableSet<LocalDateInterval>> overlapp = new TreeMap<>();
        if (!vilkårTimeline.isEmpty()) {
            LocalDate minDato = vilkårTimeline.getMinLocalDate();
            LocalDate maksDato = vilkårTimeline.getMaxLocalDate();
            var ytelseFilter = new YtelseFilter(aktørYtelse).filter(yt -> !FagsakYtelseType.OMSORGSPENGER.equals(yt.getYtelseType())); // ser bort fra omsorgspenger
            var vilkårPeriode = new LocalDateInterval(minDato, maksDato);
            for (var yt : ytelseFilter.getFiltrertYtelser()) {
                var ytp = yt.getPeriode();
                var overlappPeriode = vilkårPeriode.overlap(new LocalDateInterval(ytp.getFomDato(), ytp.getTomDato()));
                if (overlappPeriode.isPresent()) {
                    if (yt.getYtelseAnvist().isEmpty()) {
                        // er under behandling. flagger hele perioden med overlapp
                        overlapp.put(yt.getYtelseType(), new TreeSet<>(Set.of(overlappPeriode.get())));
                    } else {
                        var anvistSegmenter = yt.getYtelseAnvist().stream()
                            .map(ya -> new LocalDateSegment<>(ya.getAnvistFOM(), ya.getAnvistTOM(), yt.getYtelseType()))
                            .sorted()
                            .collect(Collectors.toList());

                        var anvistTimeline = new LocalDateTimeline<>(anvistSegmenter);
                        var intersection = anvistTimeline.intersection(vilkårTimeline);
                        if (!intersection.isEmpty()) {
                            overlapp.put(yt.getYtelseType(), intersection.getDatoIntervaller());
                        }
                    }
                }
            }

        }

        var venteårsak = Venteårsak.MANGLENDE_FUNKSJONALITET;
        if (!overlapp.isEmpty()) {
            return settPåVent(venteårsak, overlapp);
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
    }

    private BehandleStegResultat settPåVent(Venteårsak venteårsak, Map<FagsakYtelseType, NavigableSet<LocalDateInterval>> overlapp) {
        String venteårsakVariant = String.valueOf(overlapp); // tar den som ren string, (ikke json el.) for at ingen skal bli avhengig av denne.

        var aksjonspunktResultat = AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_VENT_FILTER_MANGLENDE_FUNKSJONALITET,
            venteårsak, venteårsakVariant,
            LocalDateTime.now().plusDays(14));

        return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(aksjonspunktResultat));
    }

}
