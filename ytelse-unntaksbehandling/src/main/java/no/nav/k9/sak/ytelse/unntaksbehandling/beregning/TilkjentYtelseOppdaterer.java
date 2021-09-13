package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;

import static java.lang.String.format;
import static java.util.Map.entry;
import static java.util.Optional.ofNullable;
import static no.nav.k9.felles.feil.LogLevel.INFO;
import static no.nav.k9.sak.ytelse.unntaksbehandling.beregning.TilkjentYtelseOppdaterer.InntektskategoriTilAktivitetstatusMapper.aktivitetStatusFor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.FunksjonellFeil;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsgiverDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.BekreftTilkjentYtelseDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelseAndelDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelsePeriodeDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
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
    private VilkårResultatRepository vilkårResultatRepository;
    private ArbeidsgiverValidator arbeidsgiverValidator;
    private HistorikkTjenesteAdapter historikkAdapter;

    TilkjentYtelseOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public TilkjentYtelseOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                    @Any Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste,
                                    ArbeidsgiverValidator arbeidsgiverValidator, HistorikkTjenesteAdapter historikkAdapter) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.beregnFeriepengerTjeneste = beregnFeriepengerTjeneste;
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.arbeidsgiverValidator = arbeidsgiverValidator;
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat oppdater(BekreftTilkjentYtelseDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        validerDto(dto, behandling);

        var gammeltBeregningsresultat = beregningsresultatRepository.hentBgBeregningsresultat(behandling.getId()).orElse(null);
        var nyttBeregningsresultat = BeregningsresultatEntitet.builder().medRegelInput("unntaksbehandling")
            .medRegelSporing("unntaksbehandling").build();
        for (TilkjentYtelsePeriodeDto nyPeriode : dto.getTilkjentYtelse().getPerioder()) {
            byggAndelForSøkerOgArbeidsgiver(nyttBeregningsresultat, nyPeriode);
        }

        BeregningsresultatVerifiserer.verifiserBeregningsresultat(nyttBeregningsresultat);

        getFeriepengerTjeneste(behandling).ifPresent(tjeneste -> tjeneste.beregnFeriepenger(nyttBeregningsresultat));

        beregningsresultatRepository.lagre(behandling, nyttBeregningsresultat);

        opprettHistorikkinnslag(behandling, gammeltBeregningsresultat, nyttBeregningsresultat);

        return OppdateringResultat.utenOveropp();
    }

    private void byggAndelForSøkerOgArbeidsgiver(BeregningsresultatEntitet beregningsresultat, TilkjentYtelsePeriodeDto tyPeriode) {
        var brPeriode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(tyPeriode.getFom(), tyPeriode.getTom())
            .build(beregningsresultat);

        for (TilkjentYtelseAndelDto tyAndel : tyPeriode.getAndeler()) {
            var tilSøker = Optional.ofNullable(tyAndel.getTilSoker()).orElse(0);
            var refusjon = Optional.ofNullable(tyAndel.getRefusjon()).orElse(0);

            // Søkers andel - obligatorisk for Beregningsresultat
            var søkersAndel = byggAndel(tyAndel, tilSøker, true);
            søkersAndel.buildFor(brPeriode);

            // Arbeidsgivers andel (refusjon) - opsjonell for Beregningsresultat
            if (refusjon > 0) {
                var arbeidsgiversAndel = byggAndel(tyAndel, refusjon, false);
                arbeidsgiversAndel.buildFor(brPeriode);
            }
        }
    }

    private BeregningsresultatAndel.Builder byggAndel(TilkjentYtelseAndelDto tyAndel, Integer dagsats, Boolean erBrukerMottaker) {
        Arbeidsgiver arbeidsgiver = hentArbeidsgiver(tyAndel, erBrukerMottaker).orElse(null);

        return BeregningsresultatAndel.builder()
            .medBrukerErMottaker(erBrukerMottaker)
            .medDagsats(dagsats)
            .medDagsatsFraBg(0) // Settes kun senere dersom aksjonspunkt for vurdering av tilbaketrekk
            .medUtbetalingsgrad(tyAndel.getUtbetalingsgrad())
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(InternArbeidsforholdRef.ref(tyAndel.getArbeidsforholdRef()))
            .medAktivitetStatus(aktivitetStatusFor(tyAndel.getInntektskategori()))
            .medInntektskategori(tyAndel.getInntektskategori())
            .medArbeidsforholdType(ARBEIDSFORHOLD_TYPE)
            .medStillingsprosent(STILLINGSPROSENT);
    }

    private Optional<Arbeidsgiver> hentArbeidsgiver(TilkjentYtelseAndelDto tyAndel, Boolean erBrukerMottaker) {
        var arbeidsgiverIdentikator = Optional.ofNullable(tyAndel.getArbeidsgiver()).map(ArbeidsgiverDto::getIdentifikator);
        if (arbeidsgiverIdentikator.isEmpty() && !erBrukerMottaker) {
            throw new IllegalArgumentException("Må opggi arbeidstaker dersom andel er refusjon");
        }
        return arbeidsgiverIdentikator
            .map(identitikator -> OrgNummer.erGyldigOrgnr(identitikator)
                ? Arbeidsgiver.virksomhet(identitikator)
                : Arbeidsgiver.person(new AktørId(identitikator)));
    }

    private void validerDto(BekreftTilkjentYtelseDto dto, Behandling behandling) {
        var k9Vilkåret = vilkårResultatRepository.hent(behandling.getId())
            .getVilkår(VilkårType.K9_VILKÅRET).orElseThrow();
        TilkjentYtelsePerioderValidator.valider(dto.getTilkjentYtelse().getPerioder(), k9Vilkåret);

        arbeidsgiverValidator.valider(dto.getTilkjentYtelse().getPerioder(), behandling.getAktørId());
    }

    private void opprettHistorikkinnslag(Behandling behandling, BeregningsresultatEntitet beregningsresultatFør, BeregningsresultatEntitet beregningsresultatEtter) {
        KalkulatorForBeregningsresultat kalkulatorForBeregningsresultat = new KalkulatorForBeregningsresultat(behandling.getFagsakYtelseType());
        var sumFør = beregningsresultatFør != null ? kalkulatorForBeregningsresultat.beregnTotalsum(beregningsresultatFør) : 0L;
        var sumEtter = kalkulatorForBeregningsresultat.beregnTotalsum(beregningsresultatEtter);

        var historikkInnslagTekstBuilder = historikkAdapter.tekstBuilder();
        historikkInnslagTekstBuilder.medEndretFelt(HistorikkEndretFeltType.TILKJENT_YTELSE, sumFør, sumEtter);
        historikkAdapter.opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.FAKTA_ENDRET);
    }

    private Optional<BeregnFeriepengerTjeneste> getFeriepengerTjeneste(Behandling behandling) {
        FagsakYtelseType ytelseType = behandling.getFagsakYtelseType();
        if (ytelseType == FagsakYtelseType.FRISINN) {
            return Optional.empty();
        }
        return Optional.of(FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjeneste, behandling.getFagsakYtelseType()).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + BeregnFeriepengerTjeneste.class.getSimpleName() + " for ytelse=" + ytelseType)));
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
            entry(Inntektskategori.ARBEIDSTAKER, /*                  */ AktivitetStatus.ARBEIDSTAKER),
            entry(Inntektskategori.FRILANSER, /*                     */ AktivitetStatus.FRILANSER),
            entry(Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, /*   */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE),
            entry(Inntektskategori.DAGPENGER, /*                     */ AktivitetStatus.DAGPENGER),
            entry(Inntektskategori.ARBEIDSAVKLARINGSPENGER, /*       */ AktivitetStatus.ARBEIDSAVKLARINGSPENGER),
            entry(Inntektskategori.SJØMANN, /*                       */ AktivitetStatus.ARBEIDSTAKER),
            entry(Inntektskategori.DAGMAMMA, /*                      */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE),
            entry(Inntektskategori.JORDBRUKER, /*                    */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE),
            entry(Inntektskategori.FISKER, /*                        */ AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE),
            entry(Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER, /* */ AktivitetStatus.ARBEIDSTAKER));

        static AktivitetStatus aktivitetStatusFor(Inntektskategori inntektskategori) {
            return ofNullable(INNTEKTSKATEGORI_AKTIVITET_STATUS_MAP.get(inntektskategori))
                .orElseThrow(() -> new IllegalArgumentException(format("Mangler mapping for inntektskategori: %s", inntektskategori)));
        }
    }
}
