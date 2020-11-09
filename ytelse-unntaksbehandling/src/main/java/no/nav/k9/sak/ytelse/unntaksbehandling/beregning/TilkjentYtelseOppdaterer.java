package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;


import java.math.BigDecimal;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.kontrakt.beregningsresultat.BekreftTilkjentYtelseDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelseAndelDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelsePeriodeDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.k9.sak.ytelse.beregning.BeregningsresultatVerifiserer;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftTilkjentYtelseDto.class, adapter = AksjonspunktOppdaterer.class)
public class TilkjentYtelseOppdaterer implements AksjonspunktOppdaterer<BekreftTilkjentYtelseDto> {

    // Konstanter som er del av beregningsresultat, men som ikke brukes som variabler av k9-oppdrag
    private static final BigDecimal STILLINGSPROSENT = BigDecimal.valueOf(100);
    private static final OpptjeningAktivitetType ARBEIDSFORHOLD_TYPE = OpptjeningAktivitetType.UDEFINERT;

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste;

    TilkjentYtelseOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public TilkjentYtelseOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                    @Any Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.beregnFeriepengerTjeneste = beregnFeriepengerTjeneste;
    }


    @Override
    public OppdateringResultat oppdater(BekreftTilkjentYtelseDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("manuell_behandling")
            .medRegelSporing("manuell_behandling")
            .build();

        for (TilkjentYtelsePeriodeDto tyPeriode : dto.getTilkjentYtelse().getPerioder()) {
            var brPeriode = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(tyPeriode.getFom(), tyPeriode.getTom())
                .build(beregningsresultat);
            for (TilkjentYtelseAndelDto tyAndel : tyPeriode.getAndeler()) {
                var tilSøker = Optional.ofNullable(tyAndel.getTilSoker()).orElse(0);
                var refusjon = Optional.ofNullable(tyAndel.getRefusjon()).orElse(0);

                // Søkers andel  - obligatorisk for Beregningsresultat
                var søkersAndel = byggBrAndel(tyAndel, tilSøker, true);
                søkersAndel.buildFor(brPeriode);

                // Arbeidsgivers andel (refusjon) - opsjonell for Beregningsresultat
                if (refusjon > 0) {
                    var arbeidsgiversAndel = byggBrAndel(tyAndel, refusjon, false);
                    arbeidsgiversAndel.buildFor(brPeriode);
                }
            }
        }
        BeregningsresultatVerifiserer.verifiserBeregningsresultat(beregningsresultat);

        // Beregn feriepenger
        var feriepengerTjeneste = FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjeneste, behandling.getFagsakYtelseType()).orElseThrow();
        feriepengerTjeneste.beregnFeriepenger(beregningsresultat);

        beregningsresultatRepository.lagreOverstyrtBeregningsresultat(behandling, beregningsresultat);

        return OppdateringResultat.utenOveropp();
    }

    private BeregningsresultatAndel.Builder byggBrAndel(TilkjentYtelseAndelDto tyAndel, Integer dagsats, Boolean erBrukerMottaker) {
        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(erBrukerMottaker)
            .medDagsats(dagsats)
            .medDagsatsFraBg(0) // Settes kun senere dersom aksjonspunkt for vurdering av tilbaketrekk
            .medUtbetalingsgrad(tyAndel.getUtbetalingsgrad())
            .medArbeidsgiver(OrgNummer.erGyldigOrgnr(tyAndel.getArbeidsgiver().getIdentifikator())
                ? Arbeidsgiver.virksomhet(tyAndel.getArbeidsgiver().getIdentifikator())
                : Arbeidsgiver.person(new AktørId(tyAndel.getArbeidsgiver().getIdentifikator()))
            )
            //.medArbeidsforholdRef() // kun nødvendig dersom flere arb.forhold samme arbeidsgiver
            .medAktivitetStatus(tyAndel.getAktivitetStatus())
            .medInntektskategori(tyAndel.getInntektskategori())
            .medArbeidsforholdType(ARBEIDSFORHOLD_TYPE)
            .medStillingsprosent(STILLINGSPROSENT);
    }
}
