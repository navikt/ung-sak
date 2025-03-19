package no.nav.ung.sak.etterlysning;

import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

//TODO fjern?
@Dependent
public class InntektkontrollEtterlysningOppretter implements EtterlysningOppretter {

    private final InntektArbeidYtelseTjeneste iayTjeneste;
    private final EtterlysningRepository etterlysningRepository;

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
            eksternReferanse,
            periode,
            EtterlysningType.UTTALELSE_KONTROLL_INNTEKT, null );


        etterlysningRepository.lagre(etterlysning);




    }
}
