package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.formidling.template.dto.EndringBarnetilleggDto;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

@Dependent
public class EndringBarnetilleggInnholdBygger implements VedtaksbrevInnholdBygger {

    private final UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    private static final Logger LOG = LoggerFactory.getLogger(EndringBarnetilleggInnholdBygger.class);

    @Inject
    public EndringBarnetilleggInnholdBygger(
            UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }


    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {

        // Min. dato i resultattidslinjen er da nytt barn ble født utledet av prosessTrigger
        // via DetaljertResultatUtleder
        LocalDate satsendringsdato = resultatTidslinje.getMinLocalDate();

        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandling.getId())
                .orElseThrow(() -> new IllegalStateException("Mangler grunnlag"));

        var nyeSatser = ungdomsytelseGrunnlag.getSatsTidslinje().getSegment(new LocalDateInterval(satsendringsdato, satsendringsdato)).getValue();

        if (nyeSatser.antallBarn() == 0) {
            throw new IllegalStateException("Ingen barn på fom=" + satsendringsdato);
        }

        var sats = Satsberegner.beregnBarnetilleggSats(nyeSatser);

        return new TemplateInnholdResultat(DokumentMalType.ENDRING_BARNETILLEGG, TemplateType.ENDRING_BARNETILLEGG,
                new EndringBarnetilleggDto(satsendringsdato, nyeSatser.dagsatsBarnetillegg(), sats));

    }

}
