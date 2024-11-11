package no.nav.ung.sak.ytelse.ung.beregnytelse;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.UtfallType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatMedUtbetaltePeriodeDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatPeriodeAndelDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.BeregningsresultatPeriodeDto;
import no.nav.ung.sak.kontrakt.beregningsresultat.UttakDto;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.ytelse.beregning.BeregningsresultatMapper;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@BehandlingTypeRef
public class UngdomsytelseBeregningsresultatMapper implements BeregningsresultatMapper {


    @Override
    public BeregningsresultatDto map(Behandling behandling,
                                     BehandlingBeregningsresultatEntitet beregningsresultatAggregat) {
        return BeregningsresultatDto.build()
            .medPerioder(lagPerioder(beregningsresultatAggregat.getBgBeregningsresultat()))
            .medSkalHindreTilbaketrekk(beregningsresultatAggregat.skalHindreTilbaketrekk().orElse(null))
            .create();
    }

    @Override
    public BeregningsresultatMedUtbetaltePeriodeDto mapMedUtbetaltePerioder(Behandling behandling, BehandlingBeregningsresultatEntitet beregningsresultatAggregat) {
        var beregningsresultatEntitet = Optional.ofNullable(beregningsresultatAggregat.getBgBeregningsresultat())
            .orElse(beregningsresultatAggregat.getBgBeregningsresultat());
        var perioder = lagPerioder(beregningsresultatEntitet);
        var utbetaltePerioder = lagPerioder(beregningsresultatAggregat.getUtbetBeregningsresultat());

        return BeregningsresultatMedUtbetaltePeriodeDto.build()
            .medPerioder(perioder)
            .medUtbetaltePerioder(utbetaltePerioder)
            .medSkalHindreTilbaketrekk(beregningsresultatAggregat.skalHindreTilbaketrekk().orElse(null))
            .create();
    }

    public List<BeregningsresultatPeriodeDto> lagPerioder(BeregningsresultatEntitet beregningsresultat) {
        if (beregningsresultat == null) {
            return List.of();
        }
        var beregningsresultatPerioder = beregningsresultat.getBeregningsresultatPerioder();
        var sisteUtbetalingsdato = finnSisteUtbetalingdato(beregningsresultatPerioder);

        var brpTimline = beregningsresultat.getBeregningsresultatAndelTimeline();

        var dtoer = brpTimline.toSegments().stream()
            .map(seg -> BeregningsresultatPeriodeDto.build(seg.getFom(), seg.getTom())
                .medDagsats(seg.getValue().stream().map(BeregningsresultatAndel::getDagsats).reduce(Integer::sum).orElse(0))
                .medAndeler(lagAndel(seg.getValue(), sisteUtbetalingsdato))
                .create())
            .collect(Collectors.toList());
        var resultatTimeline = toTimeline(dtoer);

        return resultatTimeline.toSegments().stream().map(LocalDateSegment::getValue).collect(Collectors.toList());
    }

    private LocalDateTimeline<BeregningsresultatPeriodeDto> toTimeline(List<BeregningsresultatPeriodeDto> resultatDtoListe) {
        var segments = resultatDtoListe.stream().map(v -> new LocalDateSegment<>(v.getFom(), v.getTom(), v)).collect(Collectors.toList());
        return new LocalDateTimeline<>(segments);
    }

    List<BeregningsresultatPeriodeAndelDto> lagAndel(List<BeregningsresultatAndel> andeler,
                                                     Optional<LocalDate> sisteUtbetalingsdato) {


        if (andeler.size() != 1) {
            throw new IllegalStateException("Forventet nøyaktig en andel, fant " + andeler.size());
        }

        BeregningsresultatAndel brukersAndel = andeler.get(0);
        BeregningsresultatPeriodeAndelDto.Builder dtoBuilder = BeregningsresultatPeriodeAndelDto.build()
            .medTilSøker(brukersAndel.getDagsats())
            .medRefusjon(0)
            .medSisteUtbetalingsdato(sisteUtbetalingsdato.orElse(null))
            .medInntektskategori(brukersAndel.getInntektskategori())
            .medStillingsprosent(brukersAndel.getStillingsprosent())
            .medUtbetalingsgrad(brukersAndel.getUtbetalingsgrad())
            .medUtbetalingsgradOppdragForBruker(brukersAndel.getUtbetalingsgradOppdrag());
        var uttakDto = new UttakDto(new Periode(brukersAndel.getFom(), brukersAndel.getTom()), UtfallType.INNVILGET, brukersAndel.getUtbetalingsgrad());
        dtoBuilder.medUttak(List.of(uttakDto));

        return List.of(dtoBuilder.create());
    }


    private Optional<LocalDate> finnSisteUtbetalingdato(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        return beregningsresultatPerioder.stream()
            .filter(p -> p.getBeregningsresultatAndelList().stream().anyMatch(a -> a.getDagsats() > 0))
            .map(BeregningsresultatPeriode::getPeriode)
            .map(DatoIntervallEntitet::getTomDato)
            .max(Comparator.naturalOrder());
    }

}
