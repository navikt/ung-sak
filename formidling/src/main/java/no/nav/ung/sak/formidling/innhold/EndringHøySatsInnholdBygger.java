package no.nav.ung.sak.formidling.innhold;

import java.math.RoundingMode;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.template.dto.EndringHøySatsDto;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;

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

        return new TemplateInnholdResultat(DokumentMalType.ENDRING_DOK, TemplateType.ENDRING_HØY_SATS,
                new EndringHøySatsDto(
                        satsendringsdato,
                        nyeSatser.dagsats().setScale(0, RoundingMode.HALF_UP).longValue(),
                        Sats.HØY.getFomAlder(),
                        nyeSatser.grunnbeløpFaktor().setScale(2, RoundingMode.HALF_UP)
                ));
    }

}
