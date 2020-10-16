package no.nav.k9.sak.web.app.tjenester.behandling.beregningsresultat;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsgiverDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelseAndelDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelsePeriodeDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.OrgNummer;

class MapperForTilkjentYtelse {

    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    MapperForTilkjentYtelse(ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    List<TilkjentYtelsePeriodeDto> mapTilkjentYtelse(BeregningsresultatEntitet beregningsresultat) {
        if (beregningsresultat == null) {
            return Collections.emptyList();
        }
        return beregningsresultat.getBeregningsresultatPerioder()
            .stream()
            .map(this::mapPeriode)
            //.filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private TilkjentYtelsePeriodeDto mapPeriode(BeregningsresultatPeriode periode) {
        List<TilkjentYtelseAndelDto> andeler = periode.getBeregningsresultatAndelList()
            .stream()
            .map(this::mapAndel)
            //.filter(andel -> andel.getDagsats() != 0)
            .collect(Collectors.toList());

        return TilkjentYtelsePeriodeDto
            .build(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom())
            .medAndeler(andeler)
            .create();
    }

    private TilkjentYtelseAndelDto mapAndel(BeregningsresultatAndel andel) {
        TilkjentYtelseAndelDto andelDto = mapAndelUtenFeriepenger(andel);
        andelDto.setUtbetalingsgrad(andel.getUtbetalingsgrad());
        // TODO: Ta hensyn til feriepenger
        return andelDto;
    }

    private TilkjentYtelseAndelDto mapAndelUtenFeriepenger(BeregningsresultatAndel andel) {
        var dtoAndelBuilder = TilkjentYtelseAndelDto.build()
            .medErBrukerMottaker(andel.erBrukerMottaker())
            .medDagsats(andel.getDagsats())
            .medInntektskategori(andel.getInntektskategori());

        andel.getArbeidsgiver()
            .map(it -> arbeidsgiverTjeneste.hent(it))
            .ifPresent(oppl ->
                dtoAndelBuilder.medArbeidsgiver(new ArbeidsgiverDto(oppl.getIdentifikator(), oppl.getIdentifikatorGUI(), oppl.getNavn())));

        Optional<Arbeidsgiver> arbeidsgiverOpt = andel.getArbeidsgiver();
        if (!andel.erBrukerMottaker() && arbeidsgiverOpt.isPresent()) {
            if (andel.erArbeidsgiverPrivatperson()) {
                dtoAndelBuilder.medAktørId(new AktørId(arbeidsgiverOpt.get().getIdentifikator()));
            } else {
                dtoAndelBuilder.medArbeidsgiverOrgnr(new OrgNummer(arbeidsgiverOpt.get().getIdentifikator()));
            }
        }

        return dtoAndelBuilder.create();
    }
}
