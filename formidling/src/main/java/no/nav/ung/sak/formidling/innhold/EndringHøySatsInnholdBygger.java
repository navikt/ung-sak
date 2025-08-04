package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.formidling.template.dto.EndringHøySatsDto;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

@Dependent
public class EndringHøySatsInnholdBygger implements VedtaksbrevInnholdBygger {

    private final UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    private static final Logger LOG = LoggerFactory.getLogger(EndringHøySatsInnholdBygger.class);

    @Inject
    public EndringHøySatsInnholdBygger(
        UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }


    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {

        // Min. dato i resultattidslinjen er da deltager blir 25 år utledet av prosessTrigger
        // via DetaljertResultatUtleder
        LocalDate satsendringsdato = resultatTidslinje.getMinLocalDate();

        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Mangler grunnlag"));

        var nyeSatser = ungdomsytelseGrunnlag.getSatsTidslinje().getSegment(new LocalDateInterval(satsendringsdato, satsendringsdato)).getValue();

        long totalBarnetillegg = nyeSatser.dagsatsBarnetillegg();

        return new TemplateInnholdResultat(DokumentMalType.ENDRING_HØY_SATS, TemplateType.ENDRING_HØY_SATS,
            new EndringHøySatsDto(
                satsendringsdato,
                Satsberegner.beregnDagsatsInklBarnetillegg(nyeSatser),
                Sats.HØY.getFomAlder(),
                totalBarnetillegg > 0 ? totalBarnetillegg : null
            ));
    }

}
