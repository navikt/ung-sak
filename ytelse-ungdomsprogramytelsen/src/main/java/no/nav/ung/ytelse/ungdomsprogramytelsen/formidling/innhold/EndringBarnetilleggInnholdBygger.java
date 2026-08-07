package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.EndringBarnetilleggDto;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
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
    public TemplateInnholdResultat bygg(Behandling behandling, DetaljertResultatTidslinje tidslinje) {

        LocalDate satsendringsdato = tidslinje.filtrerPåÅrsak(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
            .getMinLocalDate();

        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandling.getId())
                .orElseThrow(() -> new IllegalStateException("Mangler grunnlag"));

        var nyeSatser = ungdomsytelseGrunnlag.getSatsTidslinje().getSegment(new LocalDateInterval(satsendringsdato, satsendringsdato)).getValue();

        if (nyeSatser.antallBarn() == 0) {
            throw new IllegalStateException("Ingen barn på fom=" + satsendringsdato);
        }

        var sats = Satsberegner.beregnBarnetilleggSats(nyeSatser);

        return new TemplateInnholdResultat(TemplateType.ENDRING_BARNETILLEGG,
                new EndringBarnetilleggDto(satsendringsdato, nyeSatser.dagsatsBarnetillegg(), sats));

    }

}
