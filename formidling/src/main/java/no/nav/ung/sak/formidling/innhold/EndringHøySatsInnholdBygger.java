package no.nav.ung.sak.formidling.innhold;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.template.dto.EndringHøySatsDto;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;

@Dependent
public class EndringHøySatsInnholdBygger implements VedtaksbrevInnholdBygger {

    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    private static final Logger LOG = LoggerFactory.getLogger(EndringHøySatsInnholdBygger.class);

    @Inject
    public EndringHøySatsInnholdBygger(
            UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }

    public EndringHøySatsInnholdBygger() {
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        return new TemplateInnholdResultat(DokumentMalType.ENDRING_DOK, TemplateType.ENDRING_HØY_SATS,
                new EndringHøySatsDto(
                        LocalDate.now(),
                        10L,
                        20,
                        BigDecimal.ONE

                ));
    }

}
