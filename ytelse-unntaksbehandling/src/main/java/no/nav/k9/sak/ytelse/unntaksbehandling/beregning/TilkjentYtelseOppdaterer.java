package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;


import static java.lang.String.format;
import static java.util.Map.entry;
import static java.util.Optional.ofNullable;
import static no.nav.k9.sak.ytelse.unntaksbehandling.beregning.TilkjentYtelseOppdaterer.InntektskategoriTilAktivitetstatusMapper.aktivitetStatusFor;
import static no.nav.vedtak.feil.LogLevel.INFO;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
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
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.kontrakt.beregningsresultat.BekreftTilkjentYtelseDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelseAndelDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelsePeriodeDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.k9.sak.ytelse.beregning.BeregningsresultatVerifiserer;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.FunksjonellFeil;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftTilkjentYtelseDto.class, adapter = AksjonspunktOppdaterer.class)
public class TilkjentYtelseOppdaterer implements AksjonspunktOppdaterer<BekreftTilkjentYtelseDto> {
    // Konstanter som er del av beregningsresultat, men som ikke brukes som variabler av k9-oppdrag
    private static final BigDecimal STILLINGSPROSENT = BigDecimal.valueOf(100);
    private static final OpptjeningAktivitetType ARBEIDSFORHOLD_TYPE = OpptjeningAktivitetType.UDEFINERT;

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    @SuppressWarnings("unused")
    TilkjentYtelseOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public TilkjentYtelseOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                    @Any Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.beregnFeriepengerTjeneste = beregnFeriepengerTjeneste;
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
    }

    @Override
    public OppdateringResultat oppdater(BekreftTilkjentYtelseDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput("manuell_behandling")
            .medRegelSporing("manuell_behandling")
            .build();

        Vilkårene vilkårene = vilkårResultatRepository.hent(behandlingId);
        var vilkår = vilkårene.getVilkår(VilkårType.K9_VILKÅRET).orElseThrow();

        TilkjentYtelsePerioderValidator.valider(dto.getTilkjentYtelse().getPerioder(), vilkår);

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
            .medArbeidsforholdRef(InternArbeidsforholdRef.ref(tyAndel.getArbeidsforholdRef()))
            .medAktivitetStatus(aktivitetStatusFor(tyAndel.getInntektskategori()))
            .medInntektskategori(tyAndel.getInntektskategori())
            .medArbeidsforholdType(ARBEIDSFORHOLD_TYPE)
            .medStillingsprosent(STILLINGSPROSENT);
    }

    interface TilkjentYtelseOppdatererFeil extends DeklarerteFeil {
        TilkjentYtelseOppdatererFeil FACTORY = FeilFactory.create(TilkjentYtelseOppdatererFeil.class); // NOSONAR

        @FunksjonellFeil(feilkode = "K9-951877", feilmelding = "Det er angitt overlappende perioder med tilkjent ytelse: %s", løsningsforslag = "", logLevel = INFO)
        Feil overlappendeTilkjentYtelsePerioder(String feilmelding);

        @FunksjonellFeil(feilkode = "K9-951878", feilmelding = "Periode med tilkjent ytelse er ikke innenfor vilkåret", løsningsforslag = "", logLevel = INFO)
        Feil tilkjentYtelseIkkeInnenforVilkår();
    }

    static class InntektskategoriTilAktivitetstatusMapper {
        private static final Map<Inntektskategori, AktivitetStatus> INNTEKTSKATEGORI_AKTIVITET_STATUS_MAP = Map.ofEntries(
            entry(Inntektskategori.ARBEIDSTAKER,/*                  */ AktivitetStatus.ARBEIDSTAKER),
            entry(Inntektskategori.FRILANSER,/*                     */ AktivitetStatus.FRILANSER),
            entry(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE,/*   */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE),
            entry(Inntektskategori.DAGPENGER,/*                     */ AktivitetStatus.DAGPENGER),
            entry(Inntektskategori.ARBEIDSAVKLARINGSPENGER,/*       */ AktivitetStatus.ARBEIDSAVKLARINGSPENGER),
            entry(Inntektskategori.SJØMANN,/*                       */ AktivitetStatus.ARBEIDSTAKER),
            entry(Inntektskategori.DAGMAMMA,/*                      */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE),
            entry(Inntektskategori.JORDBRUKER,/*                    */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE),
            entry(Inntektskategori.FISKER,/*                        */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE),
            entry(Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER,/* */ AktivitetStatus.ARBEIDSTAKER)
        );

        static AktivitetStatus aktivitetStatusFor(Inntektskategori inntektskategori) {
            return ofNullable(INNTEKTSKATEGORI_AKTIVITET_STATUS_MAP.get(inntektskategori))
                .orElseThrow(() -> new IllegalArgumentException(format("Mangler mapping for inntektskategori: %s", inntektskategori)));
        }
    }
}
