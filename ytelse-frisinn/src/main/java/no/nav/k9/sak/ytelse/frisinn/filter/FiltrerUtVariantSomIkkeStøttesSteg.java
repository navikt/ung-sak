package no.nav.k9.sak.ytelse.frisinn.filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregnytelse.BeregneYtelseSteg;
import no.nav.k9.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakGrunnlag;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.vedtak.konfig.KonfigVerdi;

@FagsakYtelseTypeRef("FRISINN")
@BehandlingStegRef(kode = "VARIANT_FILTER")
@BehandlingTypeRef
@ApplicationScoped
public class FiltrerUtVariantSomIkkeStøttesSteg implements BeregneYtelseSteg {

    public static final DatoIntervallEntitet NÆRINGS_PERIODE = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31));
    // https://github.com/navikt/frisinn/releases/tag/1.2
    private static final LocalDate HENSYNTATT_OPPHØRTE_PERSONLIGE_FORETAK_ETTER = LocalDate.parse("2020-05-20");
    private static final LocalDate MANUELT_KLARERTE_BEHANDLINGER_TOM = LocalDate.parse("2020-05-14");

    private Boolean filterAktivert;
    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private UttakRepository uttakRepository;
    private SøknadRepository søknadRepository;

    protected FiltrerUtVariantSomIkkeStøttesSteg() {
        // for proxy
    }

    @Inject
    public FiltrerUtVariantSomIkkeStøttesSteg(@KonfigVerdi(value = "FRISINN_VARIANT_FILTER_AKTIVERT", defaultVerdi = "true") Boolean filterAktivert,
                                              InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                              UttakRepository uttakRepository,
                                              BehandlingRepositoryProvider provider) {
        this.filterAktivert = filterAktivert;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.uttakRepository = uttakRepository;
        this.søknadRepository = provider.getSøknadRepository();
        this.behandlingRepository = provider.getBehandlingRepository();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (!filterAktivert) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        if (erBehandlingInnenforManueltKlarertPeriode(kontekst.getBehandlingId())) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        Long behandlingId = kontekst.getBehandlingId();
        var oppgittOpptjening = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId)
            .getOppgittOpptjening()
            .orElseThrow();
        var uttakGrunnlag = uttakRepository.hentGrunnlag(behandlingId);

        var søknad = søknadRepository.hentSøknadHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Fant ikke søknad for behandlingId " + behandlingId));


        return filtrerBehandlinger(uttakGrunnlag, oppgittOpptjening, søknad);
    }

    BehandleStegResultat filtrerBehandlinger(Optional<UttakGrunnlag> uttakGrunnlag, OppgittOpptjening oppgittOpptjening, SøknadEntitet søknad) {
        var frilans = oppgittOpptjening.getFrilans();
        var næring = oppgittOpptjening.getEgenNæring();
        var harFrilansInntekter = harFrilansInntekter(frilans);
        var søkerKompensasjonForFrilans = harSøktKompensasjonForFrilans(uttakGrunnlag);
        var harNæringsInntekt = harNæringsinntekt(næring);
        var harNæringsinntektIHele2019 = harNæringsinntektIHele2019(næring);
        var søknadMottattEtterOpphørtePersonligeForetakBleHensyntatt = søknad.getMottattDato().isAfter(HENSYNTATT_OPPHØRTE_PERSONLIGE_FORETAK_ETTER);
        var næringStartdato = harNæringsinntektIHele2019 ? NÆRINGS_PERIODE.getFomDato() : næringStartdato(næring);
        var søkerKompensasjonForNæring = harSøktKompensasjonForNæring(uttakGrunnlag);

        /*
         Frisinnsøknad.inntekter har  frilanser og selvstendig.
        i førte omgang skal vi kun ta inn *enten*
        *  frilanser!=null && frilanser.søkerKompensasjon==true && frilanser.erNyEtablert==false && !frilanser.inntekterSøknadsperiode.isEmpty()
        eller
        *  selvstendig!=null && selvstendig.søkerKompensasjon== true && !selvstendig.inntekterSøknadsperiode.isEmpty()
        for future reference:
        frilanser.erNyEtablert er som antagelig vil slippes opp først
         */

        if (søkerKompensasjonForFrilans && harFrilansInntekter && (søkerKompensasjonForNæring || !harNæringsInntekt)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        } else if ((søkerKompensasjonForFrilans || !harFrilansInntekter) && søkerKompensasjonForNæring && harNæringsInntekt && (harNæringsinntektIHele2019 || søknadMottattEtterOpphørtePersonligeForetakBleHensyntatt)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var venteårsak = utledVenteÅrsak(harFrilansInntekter, søkerKompensasjonForFrilans, harNæringsInntekt, søkerKompensasjonForNæring, næringStartdato);

        var resultater = List.of(AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_VENT_FRISINN_MANGLENDE_FUNKSJONALITET, venteårsak, LocalDateTime.now().plusDays(3)));
        return BehandleStegResultat.utførtMedAksjonspunktResultater(resultater);
    }

    private Venteårsak utledVenteÅrsak(boolean harFrilansInntekter,
                                       boolean søkerKompensasjonForFrilans,
                                       boolean harNæringsInntekt,
                                       boolean søkerKompensasjonForNæring,
                                       LocalDate startDatoNæring) {
        String kode = "FRISINN_VARIANT";

        if (søkerKompensasjonForFrilans && søkerKompensasjonForNæring) {
            kode += "_KOMBINERT";
        } else {
            if (søkerKompensasjonForFrilans && harNæringsInntekt) {
                kode += "_FL_MED_SN_INNTEKT";
            }
            if (søkerKompensasjonForNæring && harFrilansInntekter) {
                kode += "_SN_MED_FL_INNTEKT";
            }
        }
        if (startDatoNæring != null && !startDatoNæring.equals(NÆRINGS_PERIODE.getFomDato())) {
            kode += "_NY_SN_" + startDatoNæring.getYear(); // Forventer 2019 & 2020 her
        }

        return Venteårsak.fraKode(kode);
    }

    private LocalDate næringStartdato(List<OppgittEgenNæring> næring) {
        return næring.stream()
            .filter(it -> BigDecimal.ZERO.compareTo(it.getBruttoInntekt()) != 0)
            .filter(it -> Objects.nonNull(it.getPeriode()))
            .map(OppgittEgenNæring::getFraOgMed)
            .min(LocalDate::compareTo).orElse(null);
    }

    private boolean harNæringsinntekt(List<OppgittEgenNæring> næring) {
        return næring.stream()
            .anyMatch(it -> BigDecimal.ZERO.compareTo(it.getBruttoInntekt()) != 0);
    }

    private boolean harNæringsinntektIHele2019(List<OppgittEgenNæring> næring) {
        return næring.stream()
            .filter(it -> NÆRINGS_PERIODE.equals(it.getPeriode()))
            .anyMatch(it -> BigDecimal.ZERO.compareTo(it.getBruttoInntekt()) != 0);
    }

    private boolean harSøktKompensasjonForNæring(Optional<UttakGrunnlag> uttakGrunnlag) {
        return uttakGrunnlag.map(UttakGrunnlag::getOppgittUttak)
            .map(UttakAktivitet::getPerioder)
            .orElse(Set.of())
            .stream()
            .anyMatch(it -> UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE.equals(it.getAktivitetType()));
    }

    private boolean harFrilansInntekter(Optional<OppgittFrilans> frilans) {
        return frilans.isPresent() && harInntekterSomFrilans(frilans.get());
    }

    private boolean harInntekterSomFrilans(OppgittFrilans frilans) {
        return !frilans.getFrilansoppdrag().isEmpty();
    }

    private boolean harSøktKompensasjonForFrilans(Optional<UttakGrunnlag> uttakGrunnlag) {
        return uttakGrunnlag.map(UttakGrunnlag::getOppgittUttak)
            .map(UttakAktivitet::getPerioder)
            .orElse(Set.of())
            .stream()
            .anyMatch(it -> UttakArbeidType.FRILANSER.equals(it.getAktivitetType()));
    }

    private boolean erBehandlingInnenforManueltKlarertPeriode(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return behandling.getOpprettetDato().toLocalDate().isBefore(MANUELT_KLARERTE_BEHANDLINGER_TOM);
    }

}
