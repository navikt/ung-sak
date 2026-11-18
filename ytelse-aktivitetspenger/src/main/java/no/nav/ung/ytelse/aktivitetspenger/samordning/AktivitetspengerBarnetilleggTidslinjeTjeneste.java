package no.nav.ung.ytelse.aktivitetspenger.samordning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.domene.vedtak.samordning.BarnetilleggTidslinjeTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class AktivitetspengerBarnetilleggTidslinjeTjeneste implements BarnetilleggTidslinjeTjeneste {

    private AktivitetspengerGrunnlagRepository grunnlagRepository;

    AktivitetspengerBarnetilleggTidslinjeTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AktivitetspengerBarnetilleggTidslinjeTjeneste(AktivitetspengerGrunnlagRepository grunnlagRepository) {
        this.grunnlagRepository = grunnlagRepository;
    }

    @Override
    public LocalDateTimeline<Barnetillegg> hentBarnetilleggTidslinje(Long behandlingId) {
        return grunnlagRepository.hentGrunnlag(behandlingId)
            .filter(grunnlag -> grunnlag.getSatsperioder() != null)
            .map(grunnlag -> grunnlag.hentSatsTidslinje()
                .mapValue(satser -> new Barnetillegg(satser.antallBarn(), satser.dagsatsBarnetillegg()))
                .compress())
            .orElse(LocalDateTimeline.empty());
    }
}
