package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.EndringBarnetilleggDto;

import java.time.LocalDate;

@Dependent
public class EndringBarnetilleggInnholdBygger implements VedtaksbrevInnholdBygger {

    private final AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository;

    @Inject
    public EndringBarnetilleggInnholdBygger(AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository) {
        this.aktivitetspengerGrunnlagRepository = aktivitetspengerGrunnlagRepository;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        LocalDate satsendringsdato = DetaljertResultat
            .filtererTidslinje(resultatTidslinje, DetaljertResultatType.ENDRING_BARN_FØDSEL)
            .getMinLocalDate();

        var grunnlag = aktivitetspengerGrunnlagRepository.hentGrunnlag(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Mangler grunnlag for aktivitetspenger"));

        var satsTidslinje = grunnlag.hentAktivitetspengerSatsTidslinje();
        var nyeSatser = satsTidslinje.getSegment(new LocalDateInterval(satsendringsdato, satsendringsdato)).getValue();

        if (nyeSatser.satsGrunnlag().antallBarn() == 0) {
            throw new IllegalStateException("Ingen barn på fom=" + satsendringsdato);
        }

        var sats = Satsberegner.beregnBarnetilleggSats(nyeSatser.satsGrunnlag());

        return new TemplateInnholdResultat(TemplateType.AKTIVITETSPENGER_ENDRING_BARNETILLEGG,
            new EndringBarnetilleggDto(satsendringsdato, nyeSatser.hentBeregnetSats().dagsatsBarnetillegg(), sats));
    }
}

