package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.template.dto.EndringProgramPeriodeDto;
import no.nav.ung.sak.formidling.template.dto.endring.programperiode.EndretDatoDto;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Dependent
public class EndringProgramPeriodeInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = LoggerFactory.getLogger(EndringProgramPeriodeInnholdBygger.class);

    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public EndringProgramPeriodeInnholdBygger(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }


    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        var denneProgramPerioder = hentProgramperiodeTidslinje(behandling.getId());
        if (denneProgramPerioder.size() > 1) {
            LOG.warn("Fant flere enn 1 programperiode={} i denne behandlingen. Bruker den siste", denneProgramPerioder);
        }

        var forrigeProgramPerioder = hentProgramperiodeTidslinje(behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Trenger forrige behandling ved endring av programperiode")));

        if (forrigeProgramPerioder.size() > 1) {
            LOG.warn("Fant flere enn 1 programperiode={} i forrige behandling. Bruker den siste", forrigeProgramPerioder);
        }

        var denneProgramperiode = denneProgramPerioder.toSegments().last();
        var forrigeProgramperiode = forrigeProgramPerioder.toSegments().last();

        var endretStartdato = !denneProgramperiode.getFom().equals(forrigeProgramperiode.getFom()) ?
            new EndretDatoDto(denneProgramperiode.getFom(), forrigeProgramperiode.getFom()) : null;

        var endretSluttdato = !denneProgramperiode.getTom().equals(forrigeProgramperiode.getTom()) ?
            new EndretDatoDto(denneProgramperiode.getTom().plusDays(1), forrigeProgramperiode.getTom()) : null;


        return new TemplateInnholdResultat(DokumentMalType.ENDRING_DOK, TemplateType.ENDRING_PROGRAMPERIODE,
            new EndringProgramPeriodeDto(
                endretSluttdato,
                endretStartdato,
                false
            ));
    }

    private LocalDateTimeline<Boolean> hentProgramperiodeTidslinje(Long behandlingid) {
        return new LocalDateTimeline<>(ungdomsprogramPeriodeRepository
            .hentGrunnlag(behandlingid)
            .orElseThrow(() -> new IllegalStateException("Finner ikke grunnlag for behandling"))
            .getUngdomsprogramPerioder()
            .getPerioder().stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), true))
            .toList()
        );
    }

}
