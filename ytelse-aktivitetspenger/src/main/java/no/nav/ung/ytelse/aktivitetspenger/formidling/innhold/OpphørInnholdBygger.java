package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertVilkårResultat;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.OpphørInngangsvilkårDto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class OpphørInnholdBygger implements VedtaksbrevInnholdBygger {

    private final VilkårResultatRepository vilkårResultatRepository;
    private final LocalDate overrideDagensDatoForTest;

    @Inject
    public OpphørInnholdBygger(VilkårResultatRepository vilkårResultatRepository,
                               @KonfigVerdi(value = "BREV_DAGENS_DATO_TEST", required = false) LocalDate overrideDagensDatoForTest) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.overrideDagensDatoForTest = overrideDagensDatoForTest;
    }

    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        LocalDateTimeline<DetaljertResultat> opphørPeriode = DetaljertResultat.filtererTidslinje(detaljertResultatTidslinje, DetaljertResultatType.ENDRING_SLUTTDATO);

        var opphørStartdato = opphørPeriode.getMinLocalDate();
        var sisteUtbetalingsdato = utledFremtidigUtbetalingsdato(opphørStartdato.minusDays(1), bestemInneværendeMåned());

        Set<DetaljertVilkårResultat> alleAvslåtteVilkår = AvslåttVilkårBrevinnholdHelper.hentAvslåtteVilkår(opphørPeriode);
        Set<VilkårType> avslåtteVilkårTyper = alleAvslåtteVilkår.stream()
            .map(DetaljertVilkårResultat::vilkårType)
            .collect(Collectors.toSet());

        var avslåttBosted = avslåtteVilkårTyper.contains(VilkårType.BOSTEDSVILKÅR) ?
            AvslåttVilkårBrevinnholdHelper.lagAvslåttBosted(alleAvslåtteVilkår, behandling, opphørPeriode, vilkårResultatRepository)
            : null;

        var avslåttBistand = avslåtteVilkårTyper.contains(VilkårType.BISTANDSVILKÅR) ?
            AvslåttVilkårBrevinnholdHelper.lagAvslåttBistand(alleAvslåtteVilkår, behandling, opphørPeriode, vilkårResultatRepository)
            : null;

        return new TemplateInnholdResultat(TemplateType.AKTIVITETSPENGER_OPPHØR,
            new OpphørInngangsvilkårDto(opphørStartdato, sisteUtbetalingsdato, avslåttBosted, avslåttBistand));
    }

    private YearMonth bestemInneværendeMåned() {
        return Environment.current().isLocal() && overrideDagensDatoForTest != null ?
            YearMonth.from(overrideDagensDatoForTest)
            : YearMonth.now();
    }

    static LocalDate utledFremtidigUtbetalingsdato(LocalDate sluttdato, YearMonth denneMåneden) {
        YearMonth sluttMåned = YearMonth.from(sluttdato);
        return sluttMåned.isBefore(denneMåneden) ? null
            : sluttMåned.plusMonths(1).atDay(10);
    }
}

