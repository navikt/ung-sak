package no.nav.ung.sak.domene.vedtak.samordning;

import java.math.BigDecimal;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.samordning.VedtakYtelse;
import no.nav.ung.sak.behandlingslager.samordning.YtelseAnvist;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.Virkedager;
import no.nav.ung.sak.domene.vedtak.samordning.BarnetilleggTidslinjeTjeneste.Barnetillegg;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.ytelseperioder.KvalifiserteYtelsesperioderTjeneste;

/**
 * Utleder vedtaket som lagres til samordning med andre ytelser.
 * <p>
 * Utledningen er tilstandsløs: den ser kun på behandlingen den får inn, aldri på hva som er lagret fra før.
 * Hvilket vedtak som er gjeldende avgjøres av {@link no.nav.ung.sak.behandlingslager.samordning.VedtakYtelseRepository}.
 */
@Dependent
public class VedtakYtelseUtleder {

    private BehandlingVedtakRepository behandlingVedtakRepository;
    private VilkårTjeneste vilkårTjeneste;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private Instance<BarnetilleggTidslinjeTjeneste> barnetilleggTjenester;
    private Instance<KvalifiserteYtelsesperioderTjeneste> kvalifiserteYtelsesperioderTjenester;

    VedtakYtelseUtleder() {
        // for CDI proxy
    }

    @Inject
    public VedtakYtelseUtleder(BehandlingVedtakRepository behandlingVedtakRepository,
                               VilkårTjeneste vilkårTjeneste,
                               TilkjentYtelseRepository tilkjentYtelseRepository,
                               @Any Instance<BarnetilleggTidslinjeTjeneste> barnetilleggTjenester,
                               @Any Instance<KvalifiserteYtelsesperioderTjeneste> kvalifiserteYtelsesperioderTjenester) {
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.vilkårTjeneste = vilkårTjeneste;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.barnetilleggTjenester = barnetilleggTjenester;
        this.kvalifiserteYtelsesperioderTjenester = kvalifiserteYtelsesperioderTjenester;
    }

    public VedtakYtelse utled(Behandling behandling) {
        var vedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Fant ikke vedtak for behandling " + behandling.getId()));

        var builder = VedtakYtelse.builder()
            .medAktørId(behandling.getAktørId())
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medYtelseType(behandling.getFagsakYtelseType())
            .medVedtakReferanse(behandling.getUuid())
            .medVedtattTidspunkt(vedtak.getVedtakstidspunkt());

        return leggTilPeriodeOgAnvisninger(builder, behandling, vedtak).build();
    }

    /**
     * Innvilget tidslinje avgjør alene hvilke perioder som tas med: samlet vilkårsutfall {@code OPPFYLT}, snittet med
     * ytelsens kvalifiserte ytelsesperioder. For UPY fjerner snittet perioder som er avslått i uttak (etter dødsfall og
     * etter at maks antall dager er brukt opp), siden disse ikke ligger i vilkårsresultatet.
     * Tilkjent ytelse og barnetillegg brukes kun til å berike periodene. Perioder som ikke er beregnet ennå får
     * anvisning uten dagsats, og måneder med 0 kr utbetalt tas med. Uten innvilget periode lagres vedtaket med
     * vedtaksdato som periode og ingen anvisninger, slik at et tidligere innvilget vedtak nulles ut.
     */
    private VedtakYtelse.Builder leggTilPeriodeOgAnvisninger(VedtakYtelse.Builder builder, Behandling behandling, BehandlingVedtak vedtak) {
        var behandlingId = behandling.getId();
        var ytelseType = behandling.getFagsakYtelseType();
        var innvilget = vilkårTjeneste.samletVilkårsresultat(behandlingId)
            .filterValue(utfall -> utfall.getSamletUtfall() == Utfall.OPPFYLT)
            .mapValue(utfall -> Anvisningsverdi.IKKE_BEREGNET);
        if (!innvilget.isEmpty()) {
            innvilget = innvilget.intersection(KvalifiserteYtelsesperioderTjeneste.finnTjeneste(ytelseType, kvalifiserteYtelsesperioderTjenester)
                .finnPeriodeTidslinje(behandlingId));
        }

        if (innvilget.isEmpty()) {
            var vedtaksdato = vedtak.getVedtaksdato();
            return builder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(vedtaksdato, vedtaksdato));
        }

        var barnetillegg = BarnetilleggTidslinjeTjeneste.finnTjeneste(barnetilleggTjenester, ytelseType)
            .hentBarnetilleggTidslinje(behandlingId);

        innvilget
            .combine(tilkjentYtelseRepository.hentTidslinje(behandlingId), VedtakYtelseUtleder::medTilkjentYtelse, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .combine(barnetillegg, VedtakYtelseUtleder::medBarnetillegg, LocalDateTimeline.JoinStyle.LEFT_JOIN)
            .compress()
            .toSegments()
            .forEach(segment -> builder.leggTilAnvisning(tilAnvisning(segment)));

        return builder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(innvilget.getMinLocalDate(), innvilget.getMaxLocalDate()));
    }

    private static LocalDateSegment<Anvisningsverdi> medTilkjentYtelse(LocalDateInterval intervall,
                                                                      LocalDateSegment<Anvisningsverdi> innvilget,
                                                                      LocalDateSegment<TilkjentYtelseVerdi> tilkjentYtelse) {
        if (tilkjentYtelse == null) {
            return new LocalDateSegment<>(intervall, innvilget.getValue());
        }
        var verdi = tilkjentYtelse.getValue();
        return new LocalDateSegment<>(intervall, innvilget.getValue().medTilkjentYtelse(verdi.dagsats(), verdi.utbetalingsgrad()));
    }

    private static LocalDateSegment<Anvisningsverdi> medBarnetillegg(LocalDateInterval intervall,
                                                                    LocalDateSegment<Anvisningsverdi> anvisning,
                                                                    LocalDateSegment<Barnetillegg> barnetillegg) {
        if (barnetillegg == null) {
            return new LocalDateSegment<>(intervall, anvisning.getValue());
        }
        var verdi = barnetillegg.getValue();
        return new LocalDateSegment<>(intervall, anvisning.getValue().medBarnetillegg(verdi.antallBarn(), verdi.dagsatsBarnetillegg()));
    }

    /**
     * Beløp utledes per anvisning som dagsats × virkedager (samme definisjon som tilkjent beløp i tilkjent ytelse),
     * slik at beløpet blir riktig også når en måned splittes på barnetillegg eller like måneder slås sammen.
     */
    private static YtelseAnvist tilAnvisning(LocalDateSegment<Anvisningsverdi> segment) {
        var verdi = segment.getValue();
        var beløp = verdi.dagsats() == null
            ? null
            : verdi.dagsats().multiply(BigDecimal.valueOf(Virkedager.beregnAntallVirkedager(segment.getFom(), segment.getTom())));
        return YtelseAnvist.builder(DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom()))
            .medDagsats(verdi.dagsats())
            .medBeløp(beløp)
            .medUtbetalingsgrad(verdi.utbetalingsgrad())
            .medAntallBarn(verdi.antallBarn())
            .medDagsatsBarnetillegg(verdi.dagsatsBarnetillegg())
            .build();
    }

    private record Anvisningsverdi(BigDecimal dagsats, BigDecimal utbetalingsgrad, Integer antallBarn, Integer dagsatsBarnetillegg) {

        static final Anvisningsverdi IKKE_BEREGNET = new Anvisningsverdi(null, null, null, null);

        Anvisningsverdi medTilkjentYtelse(BigDecimal dagsats, BigDecimal utbetalingsgrad) {
            return new Anvisningsverdi(dagsats, utbetalingsgrad, antallBarn, dagsatsBarnetillegg);
        }

        Anvisningsverdi medBarnetillegg(Integer antallBarn, Integer dagsatsBarnetillegg) {
            return new Anvisningsverdi(dagsats, utbetalingsgrad, antallBarn, dagsatsBarnetillegg);
        }
    }
}
