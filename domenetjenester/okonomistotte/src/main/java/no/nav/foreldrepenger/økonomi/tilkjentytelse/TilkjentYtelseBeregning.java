package no.nav.foreldrepenger.økonomi.tilkjentytelse;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelsePeriodeV1;

@FagsakYtelseTypeRef
@ApplicationScoped
public class TilkjentYtelseBeregning implements YtelseTypeTilkjentYtelseTjeneste {

    private BeregningsresultatRepository beregningsresultatRepository;
    private UttakTjeneste uttakTjeneste;

    TilkjentYtelseBeregning() {
        // for CDI proxy
    }

    @Inject
    public TilkjentYtelseBeregning(BeregningsresultatRepository beregningsresultatRepository, UttakTjeneste uttakTjeneste) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.uttakTjeneste = uttakTjeneste;
    }

    @Override
    public List<TilkjentYtelsePeriodeV1> hentTilkjentYtelsePerioder(Long behandlingId) {
        Optional<BeregningsresultatEntitet> resultatOpt = hentResultat(behandlingId);
        if (!resultatOpt.isPresent()) {
            return Collections.emptyList();
        }
        BeregningsresultatEntitet resultat = resultatOpt.get();
        return MapperForTilkjentYtelse.mapTilkjentYtelse(resultat);
    }

    @Override
    public boolean erOpphør(BehandlingReferanse ref) {
        return ref.getBehandlingResultat().isBehandlingsresultatOpphørt();
    }

    @Override
    public Boolean erOpphørEtterSkjæringstidspunkt(BehandlingReferanse ref) {
        if (!ref.getBehandlingResultat().isBehandlingsresultatOpphørt()) {
            return null; // ikke relevant //NOSONAR
        }

        var uttakOpt = uttakTjeneste.hentUttaksplanHvisEksisterer(ref.getBehandlingUuid());
        return uttakOpt.map(uttaksplan -> uttaksplan.harInnvilgetPerioder()).orElse(false);
    }

    @Override
    public LocalDate hentEndringstidspunkt(Long behandlingId) {
        return hentResultat(behandlingId)
            .flatMap(BeregningsresultatEntitet::getEndringsdato)
            .orElse(null);
    }

    private Optional<BeregningsresultatEntitet> hentResultat(Long behandlingId) {
        return beregningsresultatRepository.hentBeregningsresultat(behandlingId);
    }
}
