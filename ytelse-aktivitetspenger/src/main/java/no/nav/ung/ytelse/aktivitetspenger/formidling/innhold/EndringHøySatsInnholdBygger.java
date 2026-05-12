package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.EndringHøySatsDto;

import java.time.LocalDate;

@Dependent
public class EndringHøySatsInnholdBygger implements VedtaksbrevInnholdBygger {

    private final AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository;

    @Inject
    public EndringHøySatsInnholdBygger(AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository) {
        this.aktivitetspengerGrunnlagRepository = aktivitetspengerGrunnlagRepository;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        LocalDate satsendringsdato = DetaljertResultat.filtererTidslinje(resultatTidslinje, DetaljertResultatType.ENDRING_ØKT_SATS)
            .getMinLocalDate();

        var grunnlag = aktivitetspengerGrunnlagRepository.hentGrunnlag(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Mangler grunnlag for aktivitetspenger"));

        var satsTidslinje = grunnlag.hentAktivitetspengerSatsTidslinje();
        var nyeSatser = satsTidslinje.getSegment(new LocalDateInterval(satsendringsdato, satsendringsdato)).getValue();

        long totalBarnetillegg = nyeSatser.hentBeregnetSats().dagsatsBarnetillegg();

        return new TemplateInnholdResultat(TemplateType.AKTIVITETSPENGER_ENDRING_HØY_SATS,
            new EndringHøySatsDto(
                satsendringsdato,
                Satsberegner.beregnDagsatsInklBarnetillegg(nyeSatser),
                Sats.HØY.getFomAlder(),
                totalBarnetillegg > 0 ? totalBarnetillegg : null
            ));
    }
}
