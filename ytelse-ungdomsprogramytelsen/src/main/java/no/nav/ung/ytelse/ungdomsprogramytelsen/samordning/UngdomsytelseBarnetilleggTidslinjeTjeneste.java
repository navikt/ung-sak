package no.nav.ung.ytelse.ungdomsprogramytelsen.samordning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.domene.vedtak.samordning.BarnetilleggTidslinjeTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public class UngdomsytelseBarnetilleggTidslinjeTjeneste implements BarnetilleggTidslinjeTjeneste {

    private UngdomsytelseGrunnlagRepository grunnlagRepository;

    UngdomsytelseBarnetilleggTidslinjeTjeneste() {
        // for CDI proxy
    }

    @Inject
    public UngdomsytelseBarnetilleggTidslinjeTjeneste(UngdomsytelseGrunnlagRepository grunnlagRepository) {
        this.grunnlagRepository = grunnlagRepository;
    }

    @Override
    public LocalDateTimeline<Barnetillegg> hentBarnetilleggTidslinje(Long behandlingId) {
        return grunnlagRepository.hentGrunnlag(behandlingId)
            .filter(grunnlag -> grunnlag.getSatsPerioder() != null)
            .map(grunnlag -> grunnlag.getSatsTidslinje()
                .mapValue(satser -> new Barnetillegg(satser.antallBarn(), satser.dagsatsBarnetillegg()))
                .compress())
            .orElse(LocalDateTimeline.empty());
    }
}
