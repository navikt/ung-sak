package no.nav.k9.sak.ytelse.pleiepengerbarn.vedtak;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Collections;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.vedtak.observer.Tilleggsopplysning;
import no.nav.k9.sak.domene.vedtak.observer.YtelseTilleggsopplysningerTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagsdata;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk.MedisinskGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomInnleggelser;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
public class PsbTilleggsopplysningerTjeneste implements YtelseTilleggsopplysningerTjeneste {

    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;

    PsbTilleggsopplysningerTjeneste() {
        // CDI
    }

    @Inject
    public PsbTilleggsopplysningerTjeneste(MedisinskGrunnlagRepository medisinskGrunnlagRepository) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
    }

    @Override
    public Tilleggsopplysning generer(Behandling behandling) {

        var pleietrengendeAktørId = behandling.getFagsak().getPleietrengendeAktørId();
        var sykdomGrunnlagBehandling = medisinskGrunnlagRepository.hentGrunnlagForBehandling(behandling.getUuid());

        var innleggelsesperioder = sykdomGrunnlagBehandling.map(MedisinskGrunnlag::getGrunnlagsdata)
            .map(MedisinskGrunnlagsdata::getInnleggelser)
            .map(PleietrengendeSykdomInnleggelser::getPerioder)
            .orElseGet(Collections::emptyList)
            .stream()
            .map(it -> new Periode(it.getFom(), it.getTom()))
            .collect(Collectors.toList());

        return new PsbTilleggsopplysninger(pleietrengendeAktørId, innleggelsesperioder);
    }
}
