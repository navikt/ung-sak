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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagBehandling;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomInnleggelser;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
public class PsbTilleggsopplysningerTjeneste implements YtelseTilleggsopplysningerTjeneste {

    private SykdomGrunnlagRepository sykdomGrunnlagRepository;

    PsbTilleggsopplysningerTjeneste() {
        // CDI
    }

    @Inject
    public PsbTilleggsopplysningerTjeneste(SykdomGrunnlagRepository sykdomGrunnlagRepository) {
        this.sykdomGrunnlagRepository = sykdomGrunnlagRepository;
    }

    @Override
    public Tilleggsopplysning generer(Behandling behandling) {

        var pleietrengendeAktørId = behandling.getFagsak().getPleietrengendeAktørId();
        var sykdomGrunnlagBehandling = sykdomGrunnlagRepository.hentGrunnlagForBehandling(behandling.getUuid());

        var innleggelsesperioder = sykdomGrunnlagBehandling.map(SykdomGrunnlagBehandling::getGrunnlag)
            .map(SykdomGrunnlag::getInnleggelser)
            .map(SykdomInnleggelser::getPerioder)
            .orElseGet(Collections::emptyList)
            .stream()
            .map(it -> new Periode(it.getFom(), it.getTom()))
            .collect(Collectors.toList());

        return new PsbTilleggsopplysninger(pleietrengendeAktørId, innleggelsesperioder);
    }
}
