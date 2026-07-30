package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.ForlengetPeriodeDto;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FagsakperiodeUtleder;

import java.time.LocalDate;

@Dependent
public class ForlengetPeriodeInnholdBygger implements VedtaksbrevInnholdBygger {

    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public ForlengetPeriodeInnholdBygger(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        // Hent opprinnelig maksdato fra forrige behandling (260 virkedager).
        // Forlengelsen starter dagen etter opprinnelig maksdato, justert til neste virkedag.
        Long originalBehandlingId = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException(
                "Forventet original behandling på revurdering for forlenget periode, behandling=" + behandling.getId()));

        LocalDate originalMaksDato = ungdomsprogramPeriodeRepository.hentGrunnlag(originalBehandlingId)
            .flatMap(gr -> gr.getPeriodeMaksDato())
            .orElseThrow(() -> new IllegalStateException(
                "Forventet periodeMaksDato på original behandling=" + originalBehandlingId
                    + " ved bygging av brev for forlenget periode"));

        LocalDate forlengetPeriodeFraOgMedDato = FagsakperiodeUtleder.justerTilNesteVirkedag(originalMaksDato.plusDays(1));

        var nyttGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId())
            .orElseThrow(() -> new IllegalStateException(
                "Forventet ungdomsprogramPeriodeGrunnlag på behandling=" + behandling.getId()
                    + " ved bygging av brev for forlenget periode"));
        LocalDate nyMaksdato = nyttGrunnlag.getPeriodeMaksDato()
            .orElseThrow(() -> new IllegalStateException(
                "Forventet periodeMaksDato på behandling=" + behandling.getId()
                    + " ved bygging av brev for forlenget periode"));

        return new TemplateInnholdResultat(TemplateType.FORLENGET_PERIODE,
            new ForlengetPeriodeDto(forlengetPeriodeFraOgMedDato, nyMaksdato));
    }
}
