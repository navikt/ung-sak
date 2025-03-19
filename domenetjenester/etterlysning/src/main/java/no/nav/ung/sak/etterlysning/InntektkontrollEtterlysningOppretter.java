package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.UUID;

@Dependent
public class InntektkontrollEtterlysningOppretter implements EtterlysningOppretter {

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private EtterlysningRepository etterlysningRepository;

    @Inject
    public InntektkontrollEtterlysningOppretter(InntektArbeidYtelseTjeneste iayTjeneste,
                                                EtterlysningRepository etterlysningRepository) {
        this.iayTjeneste = iayTjeneste;
        this.etterlysningRepository = etterlysningRepository;
    }

    @Override
    public void opprettEtterlysning(long behandlingId, DatoIntervallEntitet periode) {
        final var grunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        final var grunnlagReferanse = grunnlag.getEksternReferanse();

        final var eksternReferanse = UUID.randomUUID();
        final var etterlysning = new Etterlysning(
            behandlingId,
            grunnlagReferanse,
            eksternReferanse, periode,
            EtterlysningType.UTTALELSE_KONTROLL_INNTEKT);


        etterlysningRepository.lagre(etterlysning);




    }
}
